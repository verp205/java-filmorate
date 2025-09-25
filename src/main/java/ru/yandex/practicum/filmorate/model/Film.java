package ru.yandex.practicum.filmorate.model;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

@Data
@EqualsAndHashCode(of = "id")
public class Film {
    protected long id;
    protected String name;
    protected String description;
    protected LocalDate releaseDate;
    protected Integer duration;
}
