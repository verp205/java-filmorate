package ru.yandex.practicum.filmorate.storage.film;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.Film;

import java.time.LocalDate;
import java.time.Month;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Component
public class InMemoryFilmStorage implements FilmStorage {
    private Map<Long, Film> films = new HashMap<>();
    private Map<Long, Set<Long>> likes = new HashMap<>();
    private static final int MAX_DESCRIPTION_LENGTH = 200;
    private static final LocalDate MIN_DATE = LocalDate.of(1895, Month.DECEMBER, 28);
    private static final int MIN_TIME = 1;

    @Override
    public Film addFilm(Film film) {
        log.info("Попытка добавления фильма: {}", film.getName());

        // Валидация названия
        if (film.getName() == null || film.getName().isBlank()) {
            log.error("Попытка добавления фильма с пустым названием");
            throw new ValidationException("Название не может быть пустым!");
        }

        // Валидация даты релиза
        if (film.getReleaseDate() == null) {
            log.error("Дата фильма = null");
            throw new ValidationException("Дата релиза должна быть заполнена");
        }

        // Валидация продолжительности
        if (film.getDuration() == null) {
            log.error("Продолжительность = null");
            throw new ValidationException("Должна быть указана продолжительность фильма");
        }

        // Валидация даты релиза (не раньше 28.12.1895)
        if (film.getReleaseDate().isBefore(MIN_DATE)) {
            log.error("Дата релиза фильма '{}' раньше допустимой: {} < {}",
                    film.getName(), film.getReleaseDate(), MIN_DATE);
            throw new ValidationException("Дата релиза не может быть раньше 28 декабря 1895 года!");
        }

        // Валидация длины описания
        if (film.getDescription().length() > MAX_DESCRIPTION_LENGTH) {
            log.error("Превышена длина описания у фильма '{}': {} символов > {}",
                    film.getName(), film.getDescription().length(), MAX_DESCRIPTION_LENGTH);
            throw new ValidationException("Максимальная длина описания — 200 символов!");
        }

        // Валидация продолжительности (положительное число)
        if (film.getDuration() < MIN_TIME) {
            log.error("Некорректная продолжительность фильма '{}': {}",
                    film.getName(), film.getDuration());
            throw new ValidationException("Продолжительность фильма должна быть положительным числом!");
        }

        // Генерация ID и сохранение
        film.setId(getNextId());
        log.debug("Фильму присвоен ID: {}", film.getId());

        films.put(film.getId(), film);
        log.info("Фильм успешно добавлен: ID {}, название '{}'", film.getId(), film.getName());
        return film;
    }

    @Override
    public Film updateFilm(Film film) {
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

    @Override
    public Film deleteFilm(long id) {
        Film deletedFilm = films.remove(id);
        if (deletedFilm == null) {
            log.error("Фильм с ID {} не найден для удаления", id);
            throw new NotFoundException("Фильм не найден");
        }
        log.info("Фильм удален: ID {}, название '{}'", id, deletedFilm.getName());
        return deletedFilm;
    }

    @Override
    public List<Film> getAllFilms() {
        return new ArrayList<>(films.values());
    }

    @Override
    public Film getFilmById(long id) {
        Film film = films.get(id);
        if (film == null) {
            throw new NotFoundException("Фильм с ID " + id + " не найден");
        }
        return film;
    }

    @Override
    public void addLike(long filmId, long userId) {
        log.info("Попытка добавления лайка фильму ID {} от пользователя ID {}", filmId, userId);

        Film film = getFilmById(filmId);
        film.getLikes().add(userId);

        log.info("Лайк добавлен фильму ID {} от пользователя ID {}", filmId, userId);
    }

    @Override
    public void removeLike(long filmId, long userId) {
        log.info("Попытка удаления лайка фильму ID {} от пользователя ID {}", filmId, userId);

        Film film = getFilmById(filmId);
        if (!film.getLikes().remove(userId)) {
            log.warn("Лайк от пользователя ID {} не найден у фильма ID {}", userId, filmId);
            throw new NotFoundException("Лайк не найден");
        }

        log.info("Лайк удален фильму ID {} от пользователя ID {}", filmId, userId);
    }

    @Override
    public List<Film> getPopularFilms(int count) {
        return films.values().stream()
                .sorted((f1, f2) -> {
                    int likes1 = likes.getOrDefault(f1.getId(), Collections.emptySet()).size();
                    int likes2 = likes.getOrDefault(f2.getId(), Collections.emptySet()).size();
                    return Integer.compare(likes2, likes1);
                })
                .limit(count)
                .collect(Collectors.toList());
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
