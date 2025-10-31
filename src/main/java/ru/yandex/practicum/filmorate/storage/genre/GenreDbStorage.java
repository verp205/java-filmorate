package ru.yandex.practicum.filmorate.storage.genre;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.Genre;

import java.util.List;

@Slf4j
@Component
@Primary
@Qualifier("dbGenreStorage")
public class GenreDbStorage implements GenreStorage {

    private final JdbcTemplate jdbcTemplate;

    public GenreDbStorage(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private final RowMapper<Genre> genreRowMapper = (rs, rowNum) ->
            new Genre(rs.getLong("genre_id"), rs.getString("genre_name"));

    @Override
    public List<Genre> getAllGenres() {
        String sql = "SELECT * FROM genres ORDER BY genre_id";
        List<Genre> genres = jdbcTemplate.query(sql, genreRowMapper);

        log.info("Загружено жанров: {}", genres.size());
        for (Genre genre : genres) {
            log.info("Жанр: id={}, name={}", genre.getId(), genre.getName());
        }

        return genres;
    }

    @Override
    public Genre getGenreById(long id) {
        String sql = "SELECT * FROM genres WHERE genre_id = ?";
        List<Genre> genres = jdbcTemplate.query(sql, genreRowMapper, id);

        if (genres.isEmpty()) {
            throw new NotFoundException("Жанр с ID " + id + " не найден");
        }

        return genres.get(0);
    }
}