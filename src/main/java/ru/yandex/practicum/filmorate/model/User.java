package ru.yandex.practicum.filmorate.model;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

@Data
@EqualsAndHashCode(of = "id")
public class User {
    protected long id;
    protected String name;
    protected String email;
    protected String login;
    protected LocalDate birthday;
}
