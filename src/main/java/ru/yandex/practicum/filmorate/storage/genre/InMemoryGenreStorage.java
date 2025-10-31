package ru.yandex.practicum.filmorate.storage.genre;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.model.Genre;

import java.util.*;

@Slf4j
@Component
public class InMemoryGenreStorage implements GenreStorage {

    private final Map<Long, Genre> genres = new HashMap<>();

    @Override
    public List<Genre> getAllGenres() {
        return new ArrayList<>(genres.values());
    }

    @Override
    public Genre getGenreById(long id) {
        return genres.get(id);
    }
}
