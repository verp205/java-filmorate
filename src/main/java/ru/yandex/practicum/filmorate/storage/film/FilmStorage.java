package ru.yandex.practicum.filmorate.storage.film;

import ru.yandex.practicum.filmorate.model.Film;

import java.util.Map;

public interface FilmStorage {
    Film addFilm(long id);

    Film deleteFilm(long id);

    Film updateFilm(Film film);

    Map<Long, Film> getAllFilms();

    Film createFilm(Film film);
}
