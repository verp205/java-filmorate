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
    private final Map<Long, Set<Long>> filmLikes = new HashMap<>(); // filmId -> Set of userIds

    @Autowired
    public FilmService(FilmStorage filmStorage, UserStorage userStorage) {
        this.filmStorage = filmStorage;
        this.userStorage = userStorage;
    }

    public void addLike(long filmId, long userId) {
        Film film = getFilmById(filmId);

        User user = userStorage.addUser(userId);
        if (user == null) {
            throw new NotFoundException("Пользователь с ID " + userId + " не найден");
        }

        if (!filmLikes.containsKey(filmId)) {
            filmLikes.put(filmId, new HashSet<>());
        }

        Set<Long> likes = filmLikes.get(filmId);

        if (likes.contains(userId)) {
            throw new ValidationException("Пользователь уже поставил лайк этому фильму");
        }

        likes.add(userId);
    }

    public void deleteLike(long filmId, long userId) {
        Film film = getFilmById(filmId);

        if (!filmLikes.containsKey(filmId)) {
            throw new NotFoundException("Лайк не найден");
        }

        Set<Long> likes = filmLikes.get(filmId);

        if (!likes.contains(userId)) {
            throw new NotFoundException("Лайк не найден");
        }

        likes.remove(userId);
    }

    public List<Film> getPopularFilms(int count) {
        List<Film> allFilms = getAllFilms();

        allFilms.sort((film1, film2) -> {
            int likes1 = getLikesCount(film1.getId());
            int likes2 = getLikesCount(film2.getId());
            return Integer.compare(likes2, likes1);
        });

        return allFilms.stream().limit(count).collect(Collectors.toList());
    }

    public int getLikesCount(long filmId) {
        if (!filmLikes.containsKey(filmId)) {
            return 0;
        }
        return filmLikes.get(filmId).size();
    }

    private Film getFilmById(long filmId) {
        Film film = filmStorage.addFilm(filmId);
        if (film == null) {
            throw new NotFoundException("Фильм с ID " + filmId + " не найден");
        }
        return film;
    }

    private User getUserById(long userId) {
        return userStorage.addUser(userId);
    }

    public List<Film> getAllFilms() {
        return new ArrayList<>(filmStorage.getAllFilms().values());
    }
}