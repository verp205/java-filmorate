package ru.yandex.practicum.filmorate.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.storage.film.FilmStorage;
import ru.yandex.practicum.filmorate.storage.user.UserStorage;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class FilmService {
    private final FilmStorage filmStorage;
    private final UserStorage userStorage;

    @Autowired
    public FilmService(FilmStorage filmStorage, UserStorage userStorage) {
        this.filmStorage = filmStorage;
        this.userStorage = userStorage;
    }

    public void addLike(long filmId, long userId) {
        Film film = getFilmById(filmId);
        User user = getUserById(userId);

        if (film.getLikes().contains(user.getId())) {
            throw new ValidationException("Пользователь уже поставил лайк этому фильму");
        }

        film.addLike(user.getId());
    }

    public void deleteLike(long filmId, long userId) {
        Film film = getFilmById(filmId);
        User user = getUserById(userId);

        if (!film.getLikes().contains(user.getId())) {
            throw new NotFoundException("Лайк не найден");
        }

        film.removeLike(user.getId());
    }

    public List<Film> getPopularFilms(int count) {
        return filmStorage.getAllFilms().values().stream()
                .sorted((f1, f2) -> Integer.compare(f2.getLikes().size(), f1.getLikes().size()))
                .limit(count)
                .collect(Collectors.toList());
    }

    private Film getFilmById(long filmId) {
        Film film = filmStorage.getAllFilms().get(filmId);
        if (film == null) {
            throw new NotFoundException("Фильм с ID " + filmId + " не найден");
        }
        return film;
    }

    private User getUserById(long userId) {
        User user = userStorage.getAllUsers().get(userId);
        if (user == null) {
            throw new NotFoundException("Пользователь с ID " + userId + " не найден");
        }
        return user;
    }

    public Film getFilmByIdPublic(long filmId) {
        return getFilmById(filmId);
    }

    public Film addFilm(Film film) {
        return filmStorage.addFilm(film);
    }

    public Film updateFilm(Film film) {
        return filmStorage.updateFilm(film);
    }

    public List<Film> getAllFilms() {
        return new ArrayList<>(filmStorage.getAllFilms().values());
    }
}