package ru.yandex.practicum.filmorate.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.Film;

import java.time.LocalDate;
import java.time.Month;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class FilmControllerTest {

    private FilmController filmController;

    @BeforeEach
    void setUp() {
        filmController = new FilmController();
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

        ValidationException exception = assertThrows(
                ValidationException.class,
                () -> filmController.addFilm(film)
        );
        assertEquals("Название не может быть пустым!", exception.getMessage());
    }

    @Test
    void addFilm_EmptyName_ShouldThrowException() {
        Film film = createValidFilm();
        film.setName("   ");

        ValidationException exception = assertThrows(
                ValidationException.class,
                () -> filmController.addFilm(film)
        );
        assertEquals("Название не может быть пустым!", exception.getMessage());
    }

    @Test
    void addFilm_ReleaseDateBefore1895_ShouldThrowException() {
        Film film = createValidFilm();
        film.setReleaseDate(LocalDate.of(1890, Month.JANUARY, 1));

        ValidationException exception = assertThrows(
                ValidationException.class,
                () -> filmController.addFilm(film)
        );
        assertEquals("Дата релиза не может быть раньше 28 декабря 1895 года!", exception.getMessage());
    }

    @Test
    void addFilm_DescriptionTooLong_ShouldThrowException() {
        Film film = createValidFilm();
        String longDescription = "A".repeat(201); // 201 символов
        film.setDescription(longDescription);

        ValidationException exception = assertThrows(
                ValidationException.class,
                () -> filmController.addFilm(film)
        );
        assertEquals("Максимальная длина описания — 200 символов!", exception.getMessage());
    }

    @Test
    void addFilm_DescriptionExactly200Chars_ShouldAddSuccessfully() {
        Film film = createValidFilm();
        String exactLengthDescription = "A".repeat(200); // 200 символов
        film.setDescription(exactLengthDescription);

        Film result = filmController.addFilm(film);

        assertEquals(exactLengthDescription, result.getDescription());
    }

    @Test
    void addFilm_ZeroDuration_ShouldThrowException() {
        Film film = createValidFilm();
        film.setDuration(0); // 0 минут

        ValidationException exception = assertThrows(
                ValidationException.class,
                () -> filmController.addFilm(film)
        );
        assertEquals("Продолжительность фильма должна быть положительным числом!", exception.getMessage());
    }

    @Test
    void addFilm_NegativeDuration_ShouldThrowException() {
        Film film = createValidFilm();
        film.setDuration(0);

        ValidationException exception = assertThrows(
                ValidationException.class,
                () -> filmController.addFilm(film)
        );
        assertEquals("Продолжительность фильма должна быть положительным числом!", exception.getMessage());
    }

    @Test
    void addFilm_MinimumValidDuration_ShouldAddSuccessfully() {
        Film film = createValidFilm();
        film.setDuration(1);

        Film result = filmController.addFilm(film);

        assertEquals(1, result.getDuration());
    }

    @Test
    void updateFilm_NonExistentId_ShouldThrowException() {
        Film film = createValidFilm();
        film.setId(999L);

        NotFoundException exception = assertThrows(
                NotFoundException.class,
                () -> filmController.updateFilm(film)
        );
        assertEquals("Фильм не найден", exception.getMessage());
    }

    @Test
    void addFilm_BoundaryReleaseDate_ShouldAddSuccessfully() {
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
        film.setDuration(2);
        return film;
    }
}