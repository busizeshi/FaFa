package com.fafa.biz.pet.infrastructure;

import com.fafa.biz.pet.domain.PetModels.*;
import com.fafa.biz.pet.domain.PetRepository;
import com.fafa.common.error.BusinessError;
import com.fafa.common.error.FafaException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Date;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcPetRepository implements PetRepository {
    private final JdbcTemplate jdbc;
    private final Clock clock;
    public JdbcPetRepository(JdbcTemplate jdbc, Clock clock) { this.jdbc = jdbc; this.clock = clock; }
    public Pet find(String id) {
        var pets = jdbc.query("SELECT * FROM pet WHERE id=?", this::map, id);
        return pets.isEmpty() ? null : pets.getFirst();
    }
    public List<Pet> list(String owner, Status status, String after, int limit) {
        return jdbc.query("SELECT * FROM pet WHERE owner_user_id=? AND status=? AND id>? ORDER BY id LIMIT ?",
                this::map, owner, status.name(), after, limit);
    }
    public void insert(Pet pet) {
        var p = pet.profile();
        var now = LocalDateTime.now(clock);
        jdbc.update("INSERT INTO pet(id,owner_user_id,created_by,nickname,species,breed_id,sex,neutered_status,"
                + "birth_date,estimated_birth_month,arrived_on,status,version,created_at,updated_at) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
                pet.id(), pet.ownerUserId(), pet.createdBy(), p.nickname(), p.species().name(), p.breedId(), p.sex().name(),
                p.neuteredStatus().name(), p.birthDate(), p.estimatedBirthMonth(), p.arrivedOn(), pet.status().name(), 1, now, now);
    }
    public void update(Pet pet, long expectedVersion) {
        var p = pet.profile();
        int changed = jdbc.update("UPDATE pet SET nickname=?,species=?,breed_id=?,sex=?,neutered_status=?,birth_date=?,"
                + "estimated_birth_month=?,arrived_on=?,status=?,version=version+1,updated_at=? WHERE id=? AND version=?",
                p.nickname(), p.species().name(), p.breedId(), p.sex().name(), p.neuteredStatus().name(), p.birthDate(),
                p.estimatedBirthMonth(), p.arrivedOn(), pet.status().name(), LocalDateTime.now(clock), pet.id(), expectedVersion);
        if (changed != 1) throw new FafaException(BusinessError.VERSION_CONFLICT);
    }
    public Preferences preferences(String owner) {
        var rows = jdbc.query("SELECT * FROM user_preferences WHERE user_id=?", (rs, n) -> new Preferences(
                rs.getString("current_pet_id"), rs.getString("default_pet_id"), rs.getLong("version")), owner);
        return rows.isEmpty() ? new Preferences(null, null, 1) : rows.getFirst();
    }
    public void savePreferences(String owner, Preferences p) {
        if (jdbc.update("UPDATE user_preferences SET current_pet_id=?,default_pet_id=?,version=? WHERE user_id=?",
                p.currentPetId(), p.defaultPetId(), p.version(), owner) == 0) {
            jdbc.update("INSERT INTO user_preferences(user_id,current_pet_id,default_pet_id,version) VALUES(?,?,?,?)",
                    owner, p.currentPetId(), p.defaultPetId(), p.version());
        }
    }
    public void clearSelection(String owner, String petId) {
        var p = preferences(owner);
        if (petId.equals(p.currentPetId()) || petId.equals(p.defaultPetId())) savePreferences(owner,
                new Preferences(petId.equals(p.currentPetId()) ? null : p.currentPetId(),
                        petId.equals(p.defaultPetId()) ? null : p.defaultPetId(), p.version() + 1));
    }
    public void requestDeletion(String id, Pet pet, Instant deadline) {
        var now = LocalDateTime.now(clock);
        jdbc.update("INSERT INTO pet_deletion_request(id,pet_id,actor_id,previous_status,state,delete_after,created_at,updated_at) "
                + "VALUES(?,?,?,?,?,?,?,?)", id, pet.id(), pet.ownerUserId(), pet.status().name(), "PENDING",
                LocalDateTime.ofInstant(deadline, ZoneOffset.UTC), now, now);
    }
    public Instant deletionDeadline(String petId) {
        var rows = jdbc.query("SELECT delete_after FROM pet_deletion_request WHERE pet_id=? AND state='PENDING'",
                (rs, n) -> rs.getTimestamp(1).toInstant(), petId);
        return rows.isEmpty() ? null : rows.getFirst();
    }
    public Status withdrawDeletion(String petId, Instant now) {
        var rows = jdbc.query("SELECT id,previous_status FROM pet_deletion_request WHERE pet_id=? AND state='PENDING' FOR UPDATE",
                (rs, n) -> new String[]{rs.getString(1), rs.getString(2)}, petId);
        if (rows.isEmpty()) throw new FafaException(BusinessError.PET_STATE_CONFLICT);
        var row = rows.getFirst();
        if (jdbc.update("UPDATE pet_deletion_request SET state='WITHDRAWN',updated_at=? "
                + "WHERE id=? AND state='PENDING' AND delete_after>?", LocalDateTime.ofInstant(now, ZoneOffset.UTC),
                row[0], LocalDateTime.ofInstant(now, ZoneOffset.UTC)) != 1) throw new FafaException(BusinessError.PET_STATE_CONFLICT);
        return Status.valueOf(row[1]);
    }
    private Pet map(ResultSet rs, int n) throws SQLException {
        var profile = new Profile(rs.getString("nickname"), Species.valueOf(rs.getString("species")),
                rs.getString("breed_id"), Sex.valueOf(rs.getString("sex")), Neutered.valueOf(rs.getString("neutered_status")),
                localDate(rs.getDate("birth_date")), rs.getString("estimated_birth_month"), localDate(rs.getDate("arrived_on")));
        return new Pet(rs.getString("id"), rs.getString("owner_user_id"), rs.getString("created_by"), profile,
                Status.valueOf(rs.getString("status")), rs.getLong("version"));
    }
    private java.time.LocalDate localDate(Date date) { return date == null ? null : date.toLocalDate(); }
}
