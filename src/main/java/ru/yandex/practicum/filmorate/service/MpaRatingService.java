package ru.yandex.practicum.filmorate.service;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.Mpa;
import ru.yandex.practicum.filmorate.storage.mpa_rating.MpaRatingStorage;

import java.util.List;

@Service
public class MpaRatingService {
    private final MpaRatingStorage mpaRatingStorage;

    public MpaRatingService(@Qualifier("mpaRatingDbStorage") MpaRatingStorage mpaRatingStorage) {
        this.mpaRatingStorage = mpaRatingStorage;
    }

    public List<Mpa> getAllMpa() {
        return mpaRatingStorage.getAllMpa();
    }

    public Mpa getMpaById(long id) {
        Mpa mpa = mpaRatingStorage.getMpaById(id);
        if (mpa == null) {
            throw new NotFoundException("Рейтинг MPA с ID " + id + " не найден");
        }
        return mpa;
    }
}