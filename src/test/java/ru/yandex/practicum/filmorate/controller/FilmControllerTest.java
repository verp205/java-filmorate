package ru.yandex.practicum.filmorate.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.service.FilmService;
import ru.yandex.practicum.filmorate.service.UserService;
import ru.yandex.practicum.filmorate.storage.film.InMemoryFilmStorage;
import ru.yandex.practicum.filmorate.storage.user.InMemoryUserStorage;

import java.time.LocalDate;
import java.time.Month;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class FilmControllerTest {

    private FilmController filmController;

    @BeforeEach
    void setUp() {
        InMemoryFilmStorage filmStorage = new InMemoryFilmStorage();
        InMemoryUserStorage userStorage = new InMemoryUserStorage();
        FilmService filmService = new FilmService(filmStorage, userStorage);
        UserService userService = new UserService(userStorage);
        filmController = new FilmController(filmService, userService);
    }

    @Test
    void addFilm_ValidFilm_ShouldAddSuccessfully() {
        Film validFilm = createValidFilm();

        Film result = filmController.addFilm(validFilm);

        assertNotNull(result.getId());
        assertEquals("Новый фильм", result.getName());
        assertEquals("Описание", result.getDescription());
    }

    @Test
    void addFilm_NullName_ShouldThrowException() {
        Film film = createValidFilm();
        film.setName(null);

        assertThrows(ValidationException.class, () -> filmController.addFilm(film));
    }

    @Test
    void addFilm_EmptyName_ShouldThrowException() {
        Film film = createValidFilm();
        film.setName("   ");

        assertThrows(ValidationException.class, () -> filmController.addFilm(film));
    }

    @Test
    void addFilm_ReleaseDateBefore1895_ShouldThrowException() {
        Film film = createValidFilm();
        film.setReleaseDate(LocalDate.of(1800, Month.JANUARY, 1));

        assertThrows(ValidationException.class, () -> filmController.addFilm(film));
    }

    @Test
    void addFilm_DescriptionTooLong_ShouldThrowException() {
        Film film = createValidFilm();
        film.setDescription("A".repeat(201));

        assertThrows(ValidationException.class, () -> filmController.addFilm(film));
    }

    @Test
    void addFilm_ZeroDuration_ShouldThrowException() {
        Film film = createValidFilm();
        film.setDuration(0);

        assertThrows(ValidationException.class, () -> filmController.addFilm(film));
    }

    @Test
    void addFilm_NegativeDuration_ShouldThrowException() {
        Film film = createValidFilm();
        film.setDuration(-10);

        assertThrows(ValidationException.class, () -> filmController.addFilm(film));
    }

    @Test
    void updateFilm_NonExistentId_ShouldThrowException() {
        Film film = createValidFilm();
        film.setId(999L);

        assertThrows(NotFoundException.class, () -> filmController.updateFilm(film));
    }

    @Test
    void addFilm_ValidBoundaryReleaseDate_ShouldAddSuccessfully() {
        Film film = createValidFilm();
        film.setReleaseDate(LocalDate.of(1895, Month.DECEMBER, 28));

        Film result = filmController.addFilm(film);

        assertEquals(LocalDate.of(1895, Month.DECEMBER, 28), result.getReleaseDate());
    }

    private Film createValidFilm() {
        Film film = new Film();
        film.setName("Новый фильм");
        film.setDescription("Описание");
        film.setReleaseDate(LocalDate.of(2000, Month.JANUARY, 1));
        film.setDuration(120);
        return film;
    }
}
