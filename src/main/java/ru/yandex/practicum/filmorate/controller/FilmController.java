package ru.yandex.practicum.filmorate.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.Film;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.time.LocalDate;
import java.time.Month;

@RestController
@RequestMapping("/films")
public class FilmController {
    public Map<Long, Film> films = new HashMap<>();
    private static final int MAX_DESCRIPTION_LENGTH = 200;
    private static final LocalDate MIN_DATE = LocalDate.of(1895, Month.DECEMBER, 28);
    private static final int MIN_TIME = 1;
    private static final Logger log = LoggerFactory.getLogger(FilmController.class);

    @GetMapping
    public Collection<Film> getAllFilms() {
        return films.values();
    }

    @PostMapping
    public Film addFilm(@RequestBody Film film) {
        log.info("Попытка добавления фильма: {}", film.getName());

        if (film.getReleaseDate().isBefore(MIN_DATE)) {
            log.error("Дата релиза фильма '{}' раньше допустимой: {} < {}",
                    film.getName(), film.getReleaseDate(), MIN_DATE);
            throw new ValidationException("Дата релиза не может быть раньше 28 декабря 1895 года!");
        }

        if (film.getName() == null || film.getName().isBlank()) {
            log.error("Попытка добавления фильма с пустым названием");
            throw new ValidationException("Название не может быть пустым!");
        }

        if (film.getDescription().length() > MAX_DESCRIPTION_LENGTH) {
            log.error("Превышена длина описания у фильма '{}': {} символов > {}",
                    film.getName(), film.getDescription().length(), MAX_DESCRIPTION_LENGTH);
            throw new ValidationException("Максимальная длина описания — 200 символов!");
        }

        if (film.getDuration() < MIN_TIME) {
            log.error("Некорректная продолжительность фильма '{}': {}",
                    film.getName(), film.getDuration());
            throw new ValidationException("Продолжительность фильма должна быть положительным числом!");
        }

        film.setId(getNextId());
        log.debug("Фильму присвоен ID: {}", film.getId());

        films.put(film.getId(), film);
        log.info("Фильм успешно добавлен: ID {}, название '{}'", film.getId(), film.getName());
        return film;
    }

    @PutMapping
    public Film updateFilm(@RequestBody Film film) {
        log.info("Попытка обновления фильма: {}", film.getName());

        Film existingFilm = films.get(film.getId());
        if (existingFilm == null) {
            log.error("Фильм с ID {} не найден", film.getId());
            throw new NotFoundException("Фильм не найден");
        }

        if (film.getName() != null) {
            if (film.getName().isBlank()) {
                log.error("Пустое название при обновлении фильма ID: {}", film.getId());
                throw new ValidationException("Название не может быть пустым");
            }
            existingFilm.setName(film.getName());
        }

        if (film.getDescription() != null) {
            if (film.getDescription().length() > MAX_DESCRIPTION_LENGTH) {
                log.error("Слишком длинное описание при обновлении");
                throw new ValidationException("Описание слишком длинное");
            }
            existingFilm.setDescription(film.getDescription());
        }

        if (film.getReleaseDate() != null) {
            if (film.getReleaseDate().isBefore(MIN_DATE)) {
                log.error("Некорректная дата релиза при обновлении");
                throw new ValidationException("Дата релиза некорректна");
            }
            existingFilm.setReleaseDate(film.getReleaseDate());
        }

        if (film.getDuration() != null) {
            if (film.getDuration() < MIN_TIME) {
                log.error("Некорректная продолжительность при обновлении");
                throw new ValidationException("Продолжительность некорректна");
            }
            existingFilm.setDuration(film.getDuration());
        }

        films.put(existingFilm.getId(), existingFilm);
        log.info("Фильм обновлен: {}", film.getName());
        return existingFilm;
    }

    private long getNextId() {
        long currentMaxId = films.keySet()
                .stream()
                .mapToLong(id -> id)
                .max()
                .orElse(0);
        return ++currentMaxId;
    }
}
