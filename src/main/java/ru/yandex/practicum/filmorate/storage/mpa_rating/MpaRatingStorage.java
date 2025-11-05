package ru.yandex.practicum.filmorate.storage.mpa_rating;

import ru.yandex.practicum.filmorate.model.Mpa;
import java.util.List;

public interface MpaRatingStorage {

    List<Mpa> getAllMpa();

    Mpa getMpaById(long id);
}