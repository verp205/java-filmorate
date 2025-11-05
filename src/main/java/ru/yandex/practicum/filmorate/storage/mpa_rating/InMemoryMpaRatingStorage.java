package ru.yandex.practicum.filmorate.storage.mpa_rating;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.model.Mpa;

import java.util.*;

@Slf4j
@Component
public class InMemoryMpaRatingStorage implements MpaRatingStorage {

    private final Map<Long, Mpa> mpaRatings = new HashMap<>();

    @Override
    public List<Mpa> getAllMpa() {
        return new ArrayList<>(mpaRatings.values());
    }

    @Override
    public Mpa getMpaById(long id) {
        return mpaRatings.get(id);
    }
}
