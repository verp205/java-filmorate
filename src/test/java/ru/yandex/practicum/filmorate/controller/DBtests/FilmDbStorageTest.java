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
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.storage.film.FilmDbStorage;
import ru.yandex.practicum.filmorate.storage.genre.GenreDbStorage;
import ru.yandex.practicum.filmorate.storage.mpa_rating.MpaRatingDbStorage;
import ru.yandex.practicum.filmorate.storage.user.UserDbStorage;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

@JdbcTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@RequiredArgsConstructor(onConstructor_ = @Autowired)
@Import({FilmDbStorage.class, MpaRatingDbStorage.class, GenreDbStorage.class, UserDbStorage.class})
public class FilmDbStorageTest {

    private final FilmDbStorage filmDbStorage;
    private final MpaRatingDbStorage mpaRatingDbStorage;
    private final GenreDbStorage genreDbStorage;
    private final UserDbStorage userDbStorage;
    private final JdbcTemplate jdbcTemplate;

    private User testUser1;
    private User testUser2;

    @BeforeEach
    void setup() {
        // Очистка в правильном порядке (учитывая foreign keys)
        jdbcTemplate.execute("DELETE FROM likes");
        jdbcTemplate.execute("DELETE FROM film_genres");
        jdbcTemplate.execute("DELETE FROM friends");
        jdbcTemplate.execute("DELETE FROM films");
        jdbcTemplate.execute("DELETE FROM users");

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

        // Создаем тестовых пользователей для лайков
        testUser1 = createTestUser("Test User 1", "user1@test.com", "user1", LocalDate.of(1990, 1, 1));
        testUser2 = createTestUser("Test User 2", "user2@test.com", "user2", LocalDate.of(1991, 2, 2));
    }

    private User createTestUser(String name, String email, String login, LocalDate birthday) {
        User user = new User();
        user.setName(name);
        user.setEmail(email);
        user.setLogin(login);
        user.setBirthday(birthday);
        return userDbStorage.addUser(user);
    }

    private Film createTestFilm(String name, String description, LocalDate releaseDate, int duration, Long mpaId, List<Long> genreIds) {
        Film film = new Film();
        film.setName(name);
        film.setDescription(description);
        film.setReleaseDate(releaseDate);
        film.setDuration(duration);

        if (mpaId != null) {
            film.setMpa(mpaRatingDbStorage.getMpaById(mpaId));
        }

        if (genreIds != null && !genreIds.isEmpty()) {
            Set<Genre> genres = genreIds.stream()
                    .map(genreDbStorage::getGenreById)
                    .collect(Collectors.toSet()); // Исправлено: collect to Set
            film.setGenres(genres);
        }

        return filmDbStorage.addFilm(film);
    }

    @Test
    void testAddFilm() {
        Film film = createTestFilm(
                "Test Film",
                "Description",
                LocalDate.of(2020, 1, 1),
                120,
                1L,
                Arrays.asList(1L, 2L)
        );

        assertNotNull(film.getId());

        Film retrievedFilm = filmDbStorage.getFilmById(film.getId());
        assertEquals("Test Film", retrievedFilm.getName());
        assertNotNull(retrievedFilm.getMpa());
        assertEquals(1L, retrievedFilm.getMpa().getId());
        assertEquals("G", retrievedFilm.getMpa().getName());

        assertEquals(2, retrievedFilm.getGenres().size());

        // Исправлено: проверка через stream
        Set<Long> genreIds = retrievedFilm.getGenres().stream()
                .map(Genre::getId)
                .collect(Collectors.toSet());
        assertTrue(genreIds.contains(1L));
        assertTrue(genreIds.contains(2L));
    }

    @Test
    void testAddFilmWithoutMpaAndGenres() {
        Film film = createTestFilm(
                "Test Film No MPA",
                "Description",
                LocalDate.of(2020, 1, 1),
                120,
                null,
                null
        );

        assertNotNull(film.getId());

        Film retrievedFilm = filmDbStorage.getFilmById(film.getId());
        assertEquals("Test Film No MPA", retrievedFilm.getName());
        assertNull(retrievedFilm.getMpa());
        assertTrue(retrievedFilm.getGenres().isEmpty());
    }

    @Test
    void testGetAllFilms() {
        Film film1 = createTestFilm(
                "Film 1",
                "Desc 1",
                LocalDate.of(2021, 1, 1),
                100,
                1L,
                Arrays.asList(1L)
        );

        Film film2 = createTestFilm(
                "Film 2",
                "Desc 2",
                LocalDate.of(2022, 1, 1),
                120,
                2L,
                Arrays.asList(2L, 3L)
        );

        List<Film> films = filmDbStorage.getAllFilms();
        assertEquals(2, films.size());

        // Исправлено: поиск фильмов по имени, так как порядок в Set не гарантирован
        Film firstFilm = films.stream()
                .filter(f -> f.getName().equals("Film 1"))
                .findFirst()
                .orElseThrow();
        assertEquals("Film 1", firstFilm.getName());
        assertNotNull(firstFilm.getMpa());
        assertEquals(1L, firstFilm.getMpa().getId());
        assertEquals(1, firstFilm.getGenres().size());

        // Исправлено: получение первого элемента из Set
        Genre firstGenre = firstFilm.getGenres().iterator().next();
        assertEquals(1L, firstGenre.getId());

        Film secondFilm = films.stream()
                .filter(f -> f.getName().equals("Film 2"))
                .findFirst()
                .orElseThrow();
        assertEquals("Film 2", secondFilm.getName());
        assertNotNull(secondFilm.getMpa());
        assertEquals(2L, secondFilm.getMpa().getId());
        assertEquals(2, secondFilm.getGenres().size());

        Set<Long> secondFilmGenreIds = secondFilm.getGenres().stream()
                .map(Genre::getId)
                .collect(Collectors.toSet());
        assertTrue(secondFilmGenreIds.contains(2L));
        assertTrue(secondFilmGenreIds.contains(3L));
    }

    @Test
    void testUpdateFilm() {
        Film film = createTestFilm(
                "Old Name",
                "Old Desc",
                LocalDate.of(2019, 5, 5),
                90,
                1L,
                Arrays.asList(1L)
        );

        film.setName("New Name");
        film.setDuration(95);
        film.setMpa(mpaRatingDbStorage.getMpaById(4L));

        // Исправлено: создание Set вместо List
        film.setGenres(new HashSet<>(Arrays.asList(
                genreDbStorage.getGenreById(4L),
                genreDbStorage.getGenreById(5L)
        )));
        filmDbStorage.updateFilm(film);

        Film updatedFilm = filmDbStorage.getFilmById(film.getId());
        assertEquals("New Name", updatedFilm.getName());
        assertEquals(95, updatedFilm.getDuration());
        assertNotNull(updatedFilm.getMpa());
        assertEquals(4L, updatedFilm.getMpa().getId());
        assertEquals(2, updatedFilm.getGenres().size());

        Set<Long> updatedGenreIds = updatedFilm.getGenres().stream()
                .map(Genre::getId)
                .collect(Collectors.toSet());
        assertTrue(updatedGenreIds.contains(4L));
        assertTrue(updatedGenreIds.contains(5L));
    }

    @Test
    void testUpdateFilmRemoveGenres() {
        Film film = createTestFilm(
                "Film with Genres",
                "Desc",
                LocalDate.of(2019, 5, 5),
                90,
                1L,
                Arrays.asList(1L, 2L, 3L)
        );

        film.setGenres(null);
        filmDbStorage.updateFilm(film);

        Film updatedFilm = filmDbStorage.getFilmById(film.getId());
        assertTrue(updatedFilm.getGenres().isEmpty());
    }

    @Test
    void testUpdateFilmRemoveMpa() {
        Film film = createTestFilm(
                "Film with MPA",
                "Desc",
                LocalDate.of(2019, 5, 5),
                90,
                1L,
                Arrays.asList(1L)
        );

        film.setMpa(null);
        filmDbStorage.updateFilm(film);

        Film updatedFilm = filmDbStorage.getFilmById(film.getId());
        assertNull(updatedFilm.getMpa());
    }

    @Test
    void testDeleteFilm() {
        Film film = createTestFilm(
                "Delete Film",
                "Desc",
                LocalDate.of(2018, 3, 3),
                110,
                4L,
                Arrays.asList(1L, 2L)
        );

        Film deletedFilm = filmDbStorage.deleteFilm(film.getId());

        assertNotNull(deletedFilm);
        assertNull(filmDbStorage.getFilmById(film.getId()));
    }

    @Test
    void testFilmWithLikes() {
        Film film = createTestFilm(
                "Film with Likes",
                "Desc",
                LocalDate.of(2020, 1, 1),
                120,
                1L,
                Arrays.asList(1L)
        );

        // Добавляем лайки от реальных пользователей
        filmDbStorage.addLike(film.getId(), testUser1.getId());
        filmDbStorage.addLike(film.getId(), testUser2.getId());

        // Проверяем, что лайки добавились в таблицу likes
        Integer likeCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM likes WHERE film_id = ?",
                Integer.class, film.getId());
        assertEquals(2, likeCount);

        // Проверяем конкретные лайки
        List<Long> userIds = jdbcTemplate.queryForList(
                "SELECT user_id FROM likes WHERE film_id = ? ORDER BY user_id",
                Long.class, film.getId());
        assertEquals(2, userIds.size());
        assertTrue(userIds.contains(testUser1.getId()));
        assertTrue(userIds.contains(testUser2.getId()));
    }

    @Test
    void testGetNonExistentFilm() {
        Film film = filmDbStorage.getFilmById(9999L);
        assertNull(film);
    }

    @Test
    void testAddAndRemoveLike() {
        Film film = createTestFilm(
                "Like Test Film",
                "Desc",
                LocalDate.of(2020, 1, 1),
                120,
                1L,
                Arrays.asList(1L)
        );

        // Добавляем лайк
        filmDbStorage.addLike(film.getId(), testUser1.getId());

        // Проверяем, что лайк добавился в таблицу
        Integer likeCountAfterAdd = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM likes WHERE film_id = ? AND user_id = ?",
                Integer.class, film.getId(), testUser1.getId());
        assertEquals(1, likeCountAfterAdd);

        // Удаляем лайк
        filmDbStorage.removeLike(film.getId(), testUser1.getId());

        // Проверяем, что лайк удалился из таблицы
        Integer likeCountAfterRemove = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM likes WHERE film_id = ? AND user_id = ?",
                Integer.class, film.getId(), testUser1.getId());
        assertEquals(0, likeCountAfterRemove);
    }
}