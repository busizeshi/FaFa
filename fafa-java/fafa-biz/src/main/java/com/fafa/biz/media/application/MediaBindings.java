package com.fafa.biz.media.application;

import com.fafa.biz.identity.application.AccountAdministration;
import com.fafa.biz.pet.application.PetAccess;
import com.fafa.biz.media.domain.MediaModels.*;
import com.fafa.biz.media.infrastructure.MediaStore;
import com.fafa.common.error.*;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;

/** 供照护/笔记/成长事实用例调用的真实绑定端口；不回调事实仓储，不暴露任意对象键。 */
@Service
public class MediaBindings {
    public enum BusinessType { RECORD, NOTE, MOMENT }
    private final AccountAdministration users;private final PetAccess pets;private final MediaStore store;private final JdbcTemplate jdbc;
    public MediaBindings(AccountAdministration users,PetAccess pets,MediaStore store,JdbcTemplate jdbc) {
        this.users=users;this.pets=pets;this.store=store;this.jdbc=jdbc;
    }
    @Transactional(propagation=Propagation.MANDATORY)
    public void attach(String owner,String petId,BusinessType type,String businessId,List<String> ids) {
        users.lock(owner);pets.active(owner,petId);
        if(ids.size()>20) throw new FafaException(CommonErrorCode.INVALID_ARGUMENT);
        UUID.fromString(businessId);
        for(String id:ids) assertBindable(owner,petId,id);
        // 替换引用仅影响本事实；调用方须在同一事务保存真实业务对象。
        detach(owner,type,businessId);
        ids.stream().distinct().forEach(id->jdbc.update("INSERT INTO media_reference(media_id,business_type,business_id) VALUES(?,?,?)",id,type.name(),businessId));
    }
    public void assertBindable(String owner,String petId,String id) {
        var session=store.find(id);
        if(session==null||!session.owner().equals(owner)||!session.petId().equals(petId)) throw new FafaException(CommonErrorCode.NOT_FOUND);
        if(session.state()!=State.READY) throw new FafaException(CommonErrorCode.CONFLICT);
    }
    @Transactional(propagation=Propagation.MANDATORY)
    public void detach(String owner,BusinessType type,String businessId) {
        users.lock(owner);
        jdbc.update("DELETE FROM media_reference WHERE business_type=? AND business_id=? AND media_id IN(SELECT id FROM media_asset WHERE owner_user_id=?)",
            type.name(),businessId,owner);
    }
    public List<String> ids(String owner,BusinessType type,String businessId) {
        return jdbc.query("SELECT r.media_id FROM media_reference r JOIN media_asset a ON a.id=r.media_id WHERE a.owner_user_id=? AND r.business_type=? AND r.business_id=? AND a.state='READY' ORDER BY r.media_id",
            (r,n)->r.getString(1),owner,type.name(),businessId);
    }
}
