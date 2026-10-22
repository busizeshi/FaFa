package com.fafa.biz.pet.infrastructure;

import com.fafa.biz.pet.domain.BreedRepository;
import com.fafa.common.rpc.AdminBusinessRpc.Breed;
import com.fafa.common.error.BusinessError;
import com.fafa.common.error.FafaException;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcBreedRepository implements BreedRepository {
    private final JdbcTemplate jdbc;
    public JdbcBreedRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }
    public Breed find(String id) {
        var rows = jdbc.query("SELECT * FROM pet_breed WHERE id=?", this::map, id);
        return rows.isEmpty() ? null : rows.getFirst();
    }
    public List<Breed> list(String species, String query, String after, int limit, boolean inactive) {
        return jdbc.query("SELECT * FROM pet_breed WHERE (? IS NULL OR species=?) AND (?=TRUE OR status='ACTIVE') "
                + "AND name LIKE ? ESCAPE '!' AND id>? ORDER BY id LIMIT ?", this::map, species, species, inactive,
                "%" + query.replace("!", "!!").replace("%", "!%").replace("_", "!_") + "%", after, limit);
    }
    public Breed save(String id, String species, String name, String status, Long version) {
        if (version == null) {
            jdbc.update("INSERT INTO pet_breed(id,species,name,status,version) VALUES(?,?,?,?,1)", id, species, name, status);
        } else if (jdbc.update("UPDATE pet_breed SET name=?,status=?,version=version+1 WHERE id=? AND species=? AND version=?",
                name, status, id, species, version) != 1) throw new FafaException(BusinessError.VERSION_CONFLICT);
        return find(id);
    }
    private Breed map(java.sql.ResultSet rs, int n) throws java.sql.SQLException {
        return new Breed(rs.getString("id"), rs.getString("species"), rs.getString("name"), rs.getString("status"), rs.getLong("version"));
    }
}
