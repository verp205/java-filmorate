package ru.yandex.practicum.filmorate.storage.film;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.Genre;
import ru.yandex.practicum.filmorate.storage.genre.GenreStorage;
import ru.yandex.practicum.filmorate.storage.mpa_rating.MpaRatingStorage;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
@Primary
@Qualifier("dbFilmStorage")
public class FilmDbStorage implements FilmStorage {

    private final JdbcTemplate jdbcTemplate;
    private final MpaRatingStorage mpaRatingStorage;
    private final GenreStorage genreStorage;
    private final RowMapper<Film> filmRowMapper;

    public FilmDbStorage(JdbcTemplate jdbcTemplate,
                         @Qualifier("dbMpaRatingStorage") MpaRatingStorage mpaRatingStorage,
                         @Qualifier("dbGenreStorage") GenreStorage genreStorage) {
        this.jdbcTemplate = jdbcTemplate;
        this.mpaRatingStorage = mpaRatingStorage;
        this.genreStorage = genreStorage;

        this.filmRowMapper = createFilmRowMapper();
    }

    private RowMapper<Film> createFilmRowMapper() {
        return (rs, rowNum) -> {
            Film film = new Film();
            film.setId(rs.getLong("film_id"));
            film.setName(rs.getString("name"));
            film.setDescription(rs.getString("description"));
            java.sql.Date date = rs.getDate("release_date");
            if (date != null) film.setReleaseDate(date.toLocalDate());
            film.setDuration(rs.getInt("duration"));

            Long mpaId = rs.getObject("mpa_rating_id", Long.class);
            if (mpaId != null) {
                film.setMpa(mpaRatingStorage.getMpaById(mpaId));
            }

            film.setGenres(getFilmGenres(film.getId()));

            List<Long> likesList = getFilmLikesIds(film.getId());
            film.setLikes(new HashSet<>(likesList));

            return film;
        };
    }

    private final RowMapper<Genre> genreRowMapper = (rs, rowNum) ->
            new Genre(rs.getLong("genre_id"), rs.getString("genre_name"));

    private List<Genre> getFilmGenres(Long filmId) {
        String sql = "SELECT g.genre_id, g.genre_name " +
                "FROM film_genres fg " +
                "JOIN genres g ON fg.genre_id = g.genre_id " +
                "WHERE fg.film_id = ? " +
                "GROUP BY g.genre_id, g.genre_name " +
                "ORDER BY g.genre_id";
        return jdbcTemplate.query(sql, genreRowMapper, filmId);
    }

    private List<Long> getFilmLikesIds(long filmId) {
        return jdbcTemplate.queryForList(
                "SELECT user_id FROM likes WHERE film_id = ?",
                Long.class, filmId);
    }

    private void saveFilmGenres(long filmId, List<Genre> genres) {
        jdbcTemplate.update("DELETE FROM film_genres WHERE film_id = ?", filmId);
        if (genres == null || genres.isEmpty()) return;

        Set<Long> uniqueGenreIds = new HashSet<>();
        List<Genre> uniqueGenres = new ArrayList<>();

        for (Genre genre : genres) {
            if (uniqueGenreIds.add(genre.getId())) {
                uniqueGenres.add(genre);
            }
        }

        String insertGenreSql = "INSERT INTO film_genres (film_id, genre_id) VALUES (?, ?)";
        for (Genre genre : uniqueGenres) {
            jdbcTemplate.update(insertGenreSql, filmId, genre.getId());
        }
    }

    private void saveFilmLikes(long filmId, Set<Long> likes) {
        jdbcTemplate.update("DELETE FROM likes WHERE film_id = ?", filmId);
        if (likes == null || likes.isEmpty()) return;

        String insertLikeSql = "INSERT INTO likes (film_id, user_id) VALUES (?, ?)";
        for (Long userId : likes) {
            jdbcTemplate.update(insertLikeSql, filmId, userId);
        }
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
        saveFilmLikes(generatedId, film.getLikes());

        return getFilmById(generatedId);
    }

    @Override
    public Film updateFilm(Film film) {
        String sql = "UPDATE films SET name = ?, description = ?, release_date = ?, duration = ?, mpa_rating_id = ? " +
                "WHERE film_id = ?";

        jdbcTemplate.update(sql,
                film.getName(),
                film.getDescription(),
                film.getReleaseDate(),
                film.getDuration(),
                film.getMpa() != null ? film.getMpa().getId() : null,
                film.getId());

        saveFilmGenres(film.getId(), film.getGenres());
        saveFilmLikes(film.getId(), film.getLikes());

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
    public List<Film> getAllFilms() {
        return jdbcTemplate.query("SELECT * FROM films", filmRowMapper);
    }

    @Override
    public Film getFilmById(long id) {
        return jdbcTemplate.query("SELECT * FROM films WHERE film_id = ?", filmRowMapper, id)
                .stream().findFirst().orElse(null);
    }

    @Override
    public void addLike(long filmId, long userId) {
        String sql = "INSERT INTO likes (film_id, user_id) VALUES (?, ?)";
        jdbcTemplate.update(sql, filmId, userId);
    }

    @Override
    public void removeLike(long filmId, long userId) {
        String sql = "DELETE FROM likes WHERE film_id = ? AND user_id = ?";
        jdbcTemplate.update(sql, filmId, userId);
    }
}