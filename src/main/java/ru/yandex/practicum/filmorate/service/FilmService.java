package ru.yandex.practicum.filmorate.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.Genre;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.storage.film.FilmStorage;
import ru.yandex.practicum.filmorate.storage.genre.GenreStorage;
import ru.yandex.practicum.filmorate.storage.mpa_rating.MpaRatingStorage;
import ru.yandex.practicum.filmorate.storage.user.UserStorage;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class FilmService {

    private final FilmStorage filmStorage;
    private final UserStorage userStorage;
    private final GenreStorage genreStorage;
    private final MpaRatingStorage mpaRatingStorage;

    @Autowired
    public FilmService(@Qualifier("dbFilmStorage") FilmStorage filmStorage,
                       @Qualifier("dbUserStorage") UserStorage userStorage,
                       GenreStorage genreStorage,
                       MpaRatingStorage mpaRatingStorage) {
        this.filmStorage = filmStorage;
        this.userStorage = userStorage;
        this.genreStorage = genreStorage;
        this.mpaRatingStorage = mpaRatingStorage;
    }

    public Film addFilm(Film film) {
        validateFilmBasic(film);
        validateMpaAndGenres(film); // Проверяем существование MPA и жанров
        return filmStorage.addFilm(film);
    }

    public Film updateFilm(Film film) {
        Film existingFilm = filmStorage.getFilmById(film.getId());
        if (existingFilm == null) {
            throw new RuntimeException("Фильм с ID " + film.getId() + " не найден");
        }
        validateFilmBasic(film);
        validateMpaAndGenres(film);
        return filmStorage.updateFilm(film);
    }

    public List<Film> getAllFilms() {
        return filmStorage.getAllFilms();
    }

    public Film getFilmByIdPublic(long filmId) {
        Film film = filmStorage.getFilmById(filmId);
        if (film == null) throw new NotFoundException("Фильм с ID " + filmId + " не найден");
        return film;
    }

    public void addLike(long filmId, long userId) {
        Film film = getFilmByIdPublic(filmId);
        User user = getUserById(userId);

        if (film.getLikes().contains(userId)) {
            throw new ValidationException("Пользователь уже поставил лайк этому фильму");
        }

        film.addLike(userId);
        filmStorage.updateFilm(film);
    }

    public void deleteLike(long filmId, long userId) {
        Film film = getFilmByIdPublic(filmId);
        User user = getUserById(userId);

        Set<Long> likes = film.getLikes();
        if (!likes.remove(user.getId())) {
            throw new NotFoundException("Лайк не найден");
        }
        filmStorage.updateFilm(film);
    }

    public List<Film> getPopularFilms(int count) {
        return filmStorage.getAllFilms().stream()
                .sorted((f1, f2) -> Integer.compare(f2.getLikes().size(), f1.getLikes().size()))
                .limit(count)
                .collect(Collectors.toList());
    }

    private void validateFilmBasic(Film film) {
        if (film.getName() == null || film.getName().isBlank()) {
            throw new ValidationException("Название не может быть пустым!");
        }
        if (film.getDescription() != null && film.getDescription().length() > 200) {
            throw new ValidationException("Описание не может быть длиннее 200 символов!");
        }
        if (film.getDuration() == null || film.getDuration() <= 0) {
            throw new ValidationException("Продолжительность фильма должна быть положительной!");
        }
        if (film.getReleaseDate() == null) {
            throw new ValidationException("Дата релиза не может быть пустой!");
        }
        // ДОБАВИТЬ ПРОВЕРКУ ДАТЫ РЕЛИЗА
        if (film.getReleaseDate().isBefore(java.time.LocalDate.of(1895, 12, 28))) {
            throw new ValidationException("Дата релиза не может быть раньше 28 декабря 1895 года");
        }
    }

    private void validateMpaAndGenres(Film film) {
        if (film.getMpa() != null) {
            if (mpaRatingStorage.getMpaById(film.getMpa().getId()) == null) {
                throw new NotFoundException("Рейтинг MPA с ID " + film.getMpa().getId() + " не найден");
            }
        }

        if (film.getGenres() != null) {
            for (Genre genre : film.getGenres()) {
                if (genreStorage.getGenreById(genre.getId()) == null) {
                    throw new NotFoundException("Жанр с ID " + genre.getId() + " не найден");
                }
            }
        }
    }

    private User getUserById(long userId) {
        User user = userStorage.getUserById(userId);
        if (user == null) throw new NotFoundException("Пользователь с ID " + userId + " не найден");
        return user;
    }
}