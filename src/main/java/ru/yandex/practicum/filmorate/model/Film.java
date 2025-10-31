package ru.yandex.practicum.filmorate.model;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Data
@EqualsAndHashCode(of = "id")
public class Film {
    private long id;
    private String name;
    private String description;
    private LocalDate releaseDate;
    private Integer duration;
    private Set<Long> likes = new HashSet<>();
    private Mpa mpa;
    private List<Genre> genres;

    public void addLike(long likeId) {
        likes.add(likeId);
    }

    public void removeLike(long likeId) {
        likes.remove(likeId);
    }
}
