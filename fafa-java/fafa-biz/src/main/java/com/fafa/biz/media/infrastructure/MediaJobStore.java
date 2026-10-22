package com.fafa.biz.media.infrastructure;

import com.fafa.biz.media.domain.LibraryModels.JobView;
import java.time.*;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** 处理作业、投递事件与消费收据均持久化在 Biz 库。 */
@Repository
public class MediaJobStore {
    public record Job(String id,String owner,String state,int attempts,String token,Instant leaseUntil,Instant retainUntil,String errorCode) {
        public JobView view() { return new JobView(id,state,attempts,errorCode,retainUntil); }
    }
    public record Event(String id,String jobId) { }
    private final JdbcTemplate jdbc;
    public MediaJobStore(JdbcTemplate jdbc) { this.jdbc=jdbc; }
    public Job find(String id) {
        var rows=jdbc.query("SELECT * FROM media_processing_job WHERE id=?",(r,n)->new Job(r.getString("id"),r.getString("owner_user_id"),
            r.getString("state"),r.getInt("attempts"),r.getString("lease_token"),instant(r.getObject("lease_until",LocalDateTime.class)),
            instant(r.getObject("retain_until",LocalDateTime.class)),r.getString("error_code")),id);
        return rows.isEmpty()?null:rows.getFirst();
    }
    public void create(String id,String owner,Instant retain) {
        jdbc.update("INSERT INTO media_processing_job(id,owner_user_id,state,retain_until) VALUES(?,?,'QUEUED',?)",id,owner,time(retain));
    }
    public void queue(String id,Instant due,Instant now) {
        jdbc.update("UPDATE media_processing_job SET state='QUEUED',lease_token=NULL,lease_until=NULL,error_code=NULL WHERE id=?",id);
        jdbc.update("INSERT INTO outbox_event(id,aggregate_id,event_type,available_at,created_at) VALUES(?,?,'MEDIA_PROCESS',?,?)",
            UUID.randomUUID().toString(),id,time(due),time(now));
    }
    public boolean matches(String event,String id) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM outbox_event WHERE id=? AND aggregate_id=? AND event_type='MEDIA_PROCESS'",Integer.class,event,id)>0;
    }
    public boolean seen(String event) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM consumer_inbox WHERE consumer_name='MEDIA_PROCESS' AND event_id=?",Integer.class,event)>0;
    }
    public void receipt(String event,Instant now) {
        jdbc.update("INSERT INTO consumer_inbox(consumer_name,event_id,processed_at) VALUES('MEDIA_PROCESS',?,?)",event,time(now));
    }
    public void claim(String id,String token,Instant until) {
        jdbc.update("UPDATE media_processing_job SET state='RUNNING',attempts=attempts+1,lease_token=?,lease_until=? WHERE id=?",token,time(until),id);
    }
    public void state(String id,String state,String error) {
        jdbc.update("UPDATE media_processing_job SET state=?,error_code=?,lease_token=NULL,lease_until=NULL WHERE id=?",state,error,id);
    }
    public List<Event> pending(Instant now) {
        return jdbc.query("SELECT id,aggregate_id FROM outbox_event WHERE event_type='MEDIA_PROCESS' AND state='PENDING' AND available_at<=? ORDER BY available_at,id LIMIT 50",
            (r,n)->new Event(r.getString(1),r.getString(2)),time(now));
    }
    public void sent(String id,Instant now) {
        jdbc.update("UPDATE outbox_event SET state='SENT',sent_at=? WHERE id=? AND state='PENDING'",time(now),id);
    }
    public List<String> expired(Instant now) {
        return jdbc.query("SELECT id FROM media_processing_job WHERE (state='RUNNING' AND lease_until<=?) OR (state='QUEUED' AND (retain_until<=? OR (             NOT EXISTS(SELECT 1 FROM outbox_event e WHERE e.aggregate_id=media_processing_job.id AND e.state='PENDING')             AND EXISTS(SELECT 1 FROM outbox_event e WHERE e.aggregate_id=media_processing_job.id AND e.state='SENT')             AND NOT EXISTS(SELECT 1 FROM outbox_event e WHERE e.aggregate_id=media_processing_job.id AND e.sent_at>?))))             ORDER BY id LIMIT 50",
            (r,n)->r.getString(1),time(now),time(now),time(now.minusSeconds(60)));
    }
    public boolean resendNeeded(String id,Instant now) {
        return expired(now).contains(id);
    }
    private static LocalDateTime time(Instant value) { return LocalDateTime.ofInstant(value,ZoneOffset.UTC); }
    private static Instant instant(LocalDateTime value) { return value==null?null:value.toInstant(ZoneOffset.UTC); }
}
