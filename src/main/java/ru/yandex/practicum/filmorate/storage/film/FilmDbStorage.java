package ru.yandex.practicum.filmorate.storage.film;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Primary;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.Genre;
import ru.yandex.practicum.filmorate.model.Mpa;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;
import java.util.stream.Collectors;

@Component
@Primary
@Qualifier("dbFilmStorage")
public class FilmDbStorage implements FilmStorage {

    private final JdbcTemplate jdbcTemplate;
    private final RowMapper<Film> filmRowMapper;
    private final RowMapper<Genre> genreRowMapper;

    public FilmDbStorage(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        this.filmRowMapper = createFilmRowMapper();
        this.genreRowMapper = createGenreRowMapper();
    }

    private RowMapper<Film> createFilmRowMapper() {
        return (rs, rowNum) -> {
            Film film = new Film();
            film.setId(rs.getLong("film_id"));
            film.setName(rs.getString("name"));
            film.setDescription(rs.getString("description"));

            java.sql.Date sqlDate = rs.getDate("release_date");
            if (sqlDate != null) {
                film.setReleaseDate(sqlDate.toLocalDate());
            }

            film.setDuration(rs.getInt("duration"));

            Long mpaId = rs.getObject("mpa_rating_id", Long.class);
            String mpaName = rs.getString("rating_name");
            if (mpaId != null && mpaName != null) {
                film.setMpa(new Mpa(mpaId, mpaName));
            }

            film.setGenres(new LinkedHashSet<>());
            return film;
        };
    }

    private RowMapper<Genre> createGenreRowMapper() {
        return (rs, rowNum) -> new Genre(rs.getLong("genre_id"), rs.getString("genre_name"));
    }

    @Override
    public List<Film> getAllFilms() {
        String filmsSql = "SELECT f.*, m.rating_name FROM films f " +
                "LEFT JOIN mpa_ratings m ON f.mpa_rating_id = m.rating_id";
        List<Film> films = jdbcTemplate.query(filmsSql, filmRowMapper);

        if (!films.isEmpty()) {
            loadGenresForFilms(films);
        }

        return films;
    }

    @Override
    public Film getFilmById(long id) {
        String filmSql = "SELECT f.*, m.rating_name FROM films f " +
                "LEFT JOIN mpa_ratings m ON f.mpa_rating_id = m.rating_id " +
                "WHERE f.film_id = ?";
        Film film = jdbcTemplate.query(filmSql, filmRowMapper, id)
                .stream().findFirst().orElse(null);

        if (film != null) {
            loadGenresForFilms(Collections.singletonList(film));
        }

        return film;
    }

    @Override
    public List<Film> getPopularFilms(int count) {
        String filmsSql = "SELECT f.*, m.rating_name, " +
                "COUNT(l.user_id) as likes_count " +
                "FROM films f " +
                "LEFT JOIN likes l ON f.film_id = l.film_id " +
                "LEFT JOIN mpa_ratings m ON f.mpa_rating_id = m.rating_id " +
                "GROUP BY f.film_id, m.rating_name " +
                "ORDER BY likes_count DESC " +
                "LIMIT ?";

        List<Film> films = jdbcTemplate.query(filmsSql, filmRowMapper, count);

        if (!films.isEmpty()) {
            loadGenresForFilms(films);
        }

        return films;
    }

    private void loadGenresForFilms(List<Film> films) {
        List<Long> filmIds = films.stream()
                .map(Film::getId)
                .collect(Collectors.toList());

        if (filmIds.isEmpty()) {
            return;
        }

        String inClause = filmIds.stream()
                .map(id -> "?")
                .collect(Collectors.joining(","));

        String genresSql = "SELECT fg.film_id, g.genre_id, g.genre_name " +
                "FROM film_genres fg " +
                "JOIN genres g ON fg.genre_id = g.genre_id " +
                "WHERE fg.film_id IN (" + inClause + ") " +
                "ORDER BY fg.film_id, g.genre_id";

        Map<Long, Set<Genre>> genresByFilmId = jdbcTemplate.query(genresSql, filmIds.toArray(), rs -> {
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

        for (Film film : films) {
            Set<Genre> genres = genresByFilmId.getOrDefault(film.getId(), new LinkedHashSet<>());
            film.setGenres(genres);
        }
    }

    private void saveFilmGenres(long filmId, Set<Genre> genres) {
        jdbcTemplate.update("DELETE FROM film_genres WHERE film_id = ?", filmId);

        if (genres == null || genres.isEmpty()) {
            return;
        }

        List<Genre> sortedGenres = genres.stream()
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(Genre::getId))
                .toList();

        String insertGenreSql = "INSERT INTO film_genres (film_id, genre_id) VALUES (?, ?)";

        jdbcTemplate.batchUpdate(insertGenreSql, new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                Genre genre = sortedGenres.get(i);
                ps.setLong(1, filmId);
                ps.setLong(2, genre.getId());
            }

            @Override
            public int getBatchSize() {
                return sortedGenres.size();
            }
        });
    }

    private void saveFilmLikes(long filmId, Set<Long> likes) {
        jdbcTemplate.update("DELETE FROM likes WHERE film_id = ?", filmId);
        if (likes == null || likes.isEmpty()) return;

        String insertLikeSql = "INSERT INTO likes (film_id, user_id) VALUES (?, ?)";

        List<Long> likesList = new ArrayList<>(likes);
        jdbcTemplate.batchUpdate(insertLikeSql, new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                Long userId = likesList.get(i);
                ps.setLong(1, filmId);
                ps.setLong(2, userId);
            }

            @Override
            public int getBatchSize() {
                return likesList.size();
            }
        });
    }

    @Override
    public Film addFilm(Film film) {
        String sql = "INSERT INTO films (name, description, release_date, duration, mpa_rating_id) " +
                "VALUES (?, ?, ?, ?, ?)";
        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, film.getName());
            ps.setString(2, film.getDescription());
            ps.setObject(3, film.getReleaseDate());
            ps.setInt(4, film.getDuration());
            ps.setObject(5, film.getMpa() != null ? film.getMpa().getId() : null);
            return ps;
        }, keyHolder);

        long generatedId = keyHolder.getKey().longValue();
        film.setId(generatedId);

        saveFilmGenres(generatedId, film.getGenres());

        return getFilmById(generatedId);
    }

    @Override
    public Film updateFilm(Film film) {
        String sql = "UPDATE films SET name = ?, description = ?, release_date = ?, duration = ?, mpa_rating_id = ? " +
                "WHERE film_id = ?";

        int updated = jdbcTemplate.update(sql,
                film.getName(),
                film.getDescription(),
                film.getReleaseDate(),
                film.getDuration(),
                film.getMpa() != null ? film.getMpa().getId() : null,
                film.getId());

        if (updated == 0) {
            throw new NotFoundException("Фильм с id " + film.getId() + " не найден");
        }

        saveFilmGenres(film.getId(), film.getGenres());

        return getFilmById(film.getId());
    }

    @Override
    public Film deleteFilm(long id) {
        Film film = getFilmById(id);
        if (film != null) {
            jdbcTemplate.update("DELETE FROM likes WHERE film_id = ?", id);
            jdbcTemplate.update("DELETE FROM film_genres WHERE film_id = ?", id);
            jdbcTemplate.update("DELETE FROM films WHERE film_id = ?", id);
        }
        return film;
    }

    @Override
    public void addLike(long filmId, long userId) {
        String sql = "INSERT INTO likes (film_id, user_id) VALUES (?, ?)";
        try {
            jdbcTemplate.update(sql, filmId, userId);
        } catch (DataIntegrityViolationException e) {
            throw new ValidationException("Пользователь уже поставил лайк этому фильму");
        }
    }

    @Override
    public void removeLike(long filmId, long userId) {
        String sql = "DELETE FROM likes WHERE film_id = ? AND user_id = ?";
        int rowsAffected = jdbcTemplate.update(sql, filmId, userId);
        if (rowsAffected == 0) {
            throw new NotFoundException("Лайк не найден");
        }
    }
}