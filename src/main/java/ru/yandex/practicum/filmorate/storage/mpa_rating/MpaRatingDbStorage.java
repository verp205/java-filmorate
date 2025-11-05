package ru.yandex.practicum.filmorate.storage.mpa_rating;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.model.Mpa;

import java.util.List;

@Component
@Primary
@Qualifier("dbMpaRatingStorage")
public class MpaRatingDbStorage implements MpaRatingStorage {

    private final JdbcTemplate jdbcTemplate;

    public MpaRatingDbStorage(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private final RowMapper<Mpa> mpaRowMapper = (rs, rowNum) ->
            new Mpa(rs.getLong("rating_id"), rs.getString("rating_name"));

    @Override
    public List<Mpa> getAllMpa() {
        String sql = "SELECT * FROM mpa_ratings ORDER BY rating_id";
        return jdbcTemplate.query(sql, mpaRowMapper);
    }

    @Override
    public Mpa getMpaById(long id) {
        String sql = "SELECT * FROM mpa_ratings WHERE rating_id = ?";
        return jdbcTemplate.query(sql, mpaRowMapper, id)
                .stream().findFirst().orElse(null);
    }
}