package ru.yandex.practicum.filmorate.controller.DBtests;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.Genre;
import ru.yandex.practicum.filmorate.storage.film.FilmDbStorage;
import ru.yandex.practicum.filmorate.storage.genre.GenreDbStorage;
import ru.yandex.practicum.filmorate.storage.mpa_rating.MpaRatingDbStorage;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

@JdbcTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@RequiredArgsConstructor(onConstructor_ = @Autowired)
@Import({FilmDbStorage.class, MpaRatingDbStorage.class, GenreDbStorage.class})
public class FilmDbStorageTest {

    private final FilmDbStorage filmDbStorage;
    private final MpaRatingDbStorage mpaRatingDbStorage;
    private final GenreDbStorage genreDbStorage;
    private final JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setup() {
        jdbcTemplate.execute("DELETE FROM film_genres");
        jdbcTemplate.execute("DELETE FROM likes");
        jdbcTemplate.execute("DELETE FROM films");

        // Восстанавливаем базовые данные MPA и жанров для H2
        jdbcTemplate.execute("MERGE INTO mpa_ratings (rating_id, rating_name) KEY (rating_id) VALUES (1, 'G')");
        jdbcTemplate.execute("MERGE INTO mpa_ratings (rating_id, rating_name) KEY (rating_id) VALUES (2, 'PG')");
        jdbcTemplate.execute("MERGE INTO mpa_ratings (rating_id, rating_name) KEY (rating_id) VALUES (3, 'PG-13')");
        jdbcTemplate.execute("MERGE INTO mpa_ratings (rating_id, rating_name) KEY (rating_id) VALUES (4, 'R')");
        jdbcTemplate.execute("MERGE INTO mpa_ratings (rating_id, rating_name) KEY (rating_id) VALUES (5, 'NC-17')");

        jdbcTemplate.execute("MERGE INTO genres (genre_id, genre_name) KEY (genre_id) VALUES (1, 'Комедия')");
        jdbcTemplate.execute("MERGE INTO genres (genre_id, genre_name) KEY (genre_id) VALUES (2, 'Драма')");
        jdbcTemplate.execute("MERGE INTO genres (genre_id, genre_name) KEY (genre_id) VALUES (3, 'Мультфильм')");
        jdbcTemplate.execute("MERGE INTO genres (genre_id, genre_name) KEY (genre_id) VALUES (4, 'Триллер')");
        jdbcTemplate.execute("MERGE INTO genres (genre_id, genre_name) KEY (genre_id) VALUES (5, 'Документальный')");
        jdbcTemplate.execute("MERGE INTO genres (genre_id, genre_name) KEY (genre_id) VALUES (6, 'Боевик')");
    }

    @Test
    void testAddFilm() {
        Film film = new Film();
        film.setName("Test Film");
        film.setDescription("Description");
        film.setReleaseDate(LocalDate.of(2020, 1, 1));
        film.setDuration(120);
        film.setMpa(mpaRatingDbStorage.getMpaById(1L));
        film.setGenres(Arrays.asList(
                genreDbStorage.getGenreById(1L),
                genreDbStorage.getGenreById(2L)
        ));

        Film addedFilm = filmDbStorage.addFilm(film);
        assertNotNull(addedFilm.getId());

        Film retrievedFilm = filmDbStorage.getFilmById(addedFilm.getId());
        assertEquals("Test Film", retrievedFilm.getName());
        assertNotNull(retrievedFilm.getMpa());
        assertEquals(1L, retrievedFilm.getMpa().getId());
        assertEquals("G", retrievedFilm.getMpa().getName());

        assertEquals(2, retrievedFilm.getGenres().size());
        assertEquals(Arrays.asList(1L, 2L),
                retrievedFilm.getGenres().stream().map(Genre::getId).collect(Collectors.toList()));
    }

    @Test
    void testAddFilmWithoutMpaAndGenres() {
        Film film = new Film();
        film.setName("Test Film No MPA");
        film.setDescription("Description");
        film.setReleaseDate(LocalDate.of(2020, 1, 1));
        film.setDuration(120);

        Film addedFilm = filmDbStorage.addFilm(film);
        assertNotNull(addedFilm.getId());

        Film retrievedFilm = filmDbStorage.getFilmById(addedFilm.getId());
        assertEquals("Test Film No MPA", retrievedFilm.getName());
        assertNull(retrievedFilm.getMpa());
        assertTrue(retrievedFilm.getGenres().isEmpty());
    }

    @Test
    void testGetAllFilms() {
        Film film1 = new Film();
        film1.setName("Film 1");
        film1.setDescription("Desc 1");
        film1.setReleaseDate(LocalDate.of(2021, 1, 1));
        film1.setDuration(100);
        film1.setMpa(mpaRatingDbStorage.getMpaById(1L));
        film1.setGenres(Arrays.asList(genreDbStorage.getGenreById(1L)));

        Film film2 = new Film();
        film2.setName("Film 2");
        film2.setDescription("Desc 2");
        film2.setReleaseDate(LocalDate.of(2022, 1, 1));
        film2.setDuration(120);
        film2.setMpa(mpaRatingDbStorage.getMpaById(2L));
        film2.setGenres(Arrays.asList(genreDbStorage.getGenreById(2L), genreDbStorage.getGenreById(3L)));

        filmDbStorage.addFilm(film1);
        filmDbStorage.addFilm(film2);

        List<Film> films = filmDbStorage.getAllFilms();
        assertEquals(2, films.size());

        Film firstFilm = films.get(0);
        assertEquals("Film 1", firstFilm.getName());
        assertNotNull(firstFilm.getMpa());
        assertEquals(1L, firstFilm.getMpa().getId());
        assertEquals(1, firstFilm.getGenres().size());
        assertEquals(1L, firstFilm.getGenres().get(0).getId());

        Film secondFilm = films.get(1);
        assertEquals("Film 2", secondFilm.getName());
        assertNotNull(secondFilm.getMpa());
        assertEquals(2L, secondFilm.getMpa().getId());
        assertEquals(2, secondFilm.getGenres().size());
        assertEquals(Arrays.asList(2L, 3L),
                secondFilm.getGenres().stream().map(Genre::getId).collect(Collectors.toList()));
    }

    @Test
    void testUpdateFilm() {
        Film film = new Film();
        film.setName("Old Name");
        film.setDescription("Old Desc");
        film.setReleaseDate(LocalDate.of(2019, 5, 5));
        film.setDuration(90);
        film.setMpa(mpaRatingDbStorage.getMpaById(1L));
        film.setGenres(Arrays.asList(genreDbStorage.getGenreById(1L)));

        Film addedFilm = filmDbStorage.addFilm(film);

        addedFilm.setName("New Name");
        addedFilm.setDuration(95);
        addedFilm.setMpa(mpaRatingDbStorage.getMpaById(4L));
        addedFilm.setGenres(Arrays.asList(genreDbStorage.getGenreById(4L), genreDbStorage.getGenreById(5L)));
        filmDbStorage.updateFilm(addedFilm);

        Film updatedFilm = filmDbStorage.getFilmById(addedFilm.getId());
        assertEquals("New Name", updatedFilm.getName());
        assertEquals(95, updatedFilm.getDuration());
        assertNotNull(updatedFilm.getMpa());
        assertEquals(4L, updatedFilm.getMpa().getId());
        assertEquals(2, updatedFilm.getGenres().size());
        assertEquals(Arrays.asList(4L, 5L),
                updatedFilm.getGenres().stream().map(Genre::getId).collect(Collectors.toList()));
    }

    @Test
    void testUpdateFilmRemoveGenres() {
        Film film = new Film();
        film.setName("Film with Genres");
        film.setDescription("Desc");
        film.setReleaseDate(LocalDate.of(2019, 5, 5));
        film.setDuration(90);
        film.setMpa(mpaRatingDbStorage.getMpaById(1L));
        film.setGenres(Arrays.asList(
                genreDbStorage.getGenreById(1L),
                genreDbStorage.getGenreById(2L),
                genreDbStorage.getGenreById(3L)
        ));

        Film addedFilm = filmDbStorage.addFilm(film);

        addedFilm.setGenres(null);
        filmDbStorage.updateFilm(addedFilm);

        Film updatedFilm = filmDbStorage.getFilmById(addedFilm.getId());
        assertTrue(updatedFilm.getGenres().isEmpty());
    }

    @Test
    void testUpdateFilmRemoveMpa() {
        Film film = new Film();
        film.setName("Film with MPA");
        film.setDescription("Desc");
        film.setReleaseDate(LocalDate.of(2019, 5, 5));
        film.setDuration(90);
        film.setMpa(mpaRatingDbStorage.getMpaById(1L));
        film.setGenres(Arrays.asList(genreDbStorage.getGenreById(1L)));

        Film addedFilm = filmDbStorage.addFilm(film);

        addedFilm.setMpa(null);
        filmDbStorage.updateFilm(addedFilm);

        Film updatedFilm = filmDbStorage.getFilmById(addedFilm.getId());
        assertNull(updatedFilm.getMpa());
    }

    @Test
    void testDeleteFilm() {
        Film film = new Film();
        film.setName("Delete Film");
        film.setDescription("Desc");
        film.setReleaseDate(LocalDate.of(2018, 3, 3));
        film.setDuration(110);
        film.setMpa(mpaRatingDbStorage.getMpaById(4L));
        film.setGenres(Arrays.asList(genreDbStorage.getGenreById(1L), genreDbStorage.getGenreById(2L)));

        Film addedFilm = filmDbStorage.addFilm(film);
        Film deletedFilm = filmDbStorage.deleteFilm(addedFilm.getId());

        assertNotNull(deletedFilm);
        assertNull(filmDbStorage.getFilmById(addedFilm.getId()));
    }

    @Test
    void testFilmWithLikes() {
        Film film = new Film();
        film.setName("Film with Likes");
        film.setDescription("Desc");
        film.setReleaseDate(LocalDate.of(2020, 1, 1));
        film.setDuration(120);
        film.setMpa(mpaRatingDbStorage.getMpaById(1L));
        film.setGenres(Arrays.asList(genreDbStorage.getGenreById(1L)));

        Film addedFilm = filmDbStorage.addFilm(film);

        jdbcTemplate.update("INSERT INTO likes (film_id, user_id) VALUES (?, ?)", addedFilm.getId(), 1L);
        jdbcTemplate.update("INSERT INTO likes (film_id, user_id) VALUES (?, ?)", addedFilm.getId(), 2L);

        Film filmWithLikes = filmDbStorage.getFilmById(addedFilm.getId());
        assertEquals(2, filmWithLikes.getLikes().size());
        assertTrue(filmWithLikes.getLikes().contains(1L));
        assertTrue(filmWithLikes.getLikes().contains(2L));
    }

    @Test
    void testGetNonExistentFilm() {
        Film film = filmDbStorage.getFilmById(9999L);
        assertNull(film);
    }
}