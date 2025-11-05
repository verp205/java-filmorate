package ru.yandex.practicum.filmorate.storage.genre;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.Genre;

import java.util.*;
import java.util.stream.Collectors;

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

    private Set<Genre> getFilmGenres(Long filmId) {
        String sql = "SELECT g.genre_id, g.genre_name " +
                "FROM film_genres fg " +
                "JOIN genres g ON fg.genre_id = g.genre_id " +
                "WHERE fg.film_id = ? " +
                "ORDER BY g.genre_id";
        List<Genre> genres = jdbcTemplate.query(sql, genreRowMapper, filmId);
        return new LinkedHashSet<>(genres);
    }

    @Override
    public Map<Long, Set<Genre>> getGenresForFilms(List<Long> filmIds) {
        if (filmIds.isEmpty()) {
            return new HashMap<>();
        }

        String inClause = filmIds.stream()
                .map(id -> "?")
                .collect(Collectors.joining(","));

        String sql = "SELECT fg.film_id, g.genre_id, g.genre_name " +
                "FROM film_genres fg " +
                "JOIN genres g ON fg.genre_id = g.genre_id " +
                "WHERE fg.film_id IN (" + inClause + ") " +
                "ORDER BY fg.film_id, g.genre_id";

        return jdbcTemplate.query(sql, filmIds.toArray(), rs -> {
            Map<Long, Set<Genre>> result = new HashMap<>();
            while (rs.next()) {
                Long filmId = rs.getLong("film_id");
                long genreId = rs.getLong("genre_id");
                String genreName = rs.getString("genre_name");
                Genre genre = new Genre(genreId, genreName);
                result.computeIfAbsent(filmId, k -> new LinkedHashSet<>()).add(genre);
            }
            return result;
        });
    }
}