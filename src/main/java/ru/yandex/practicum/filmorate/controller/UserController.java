package ru.yandex.practicum.filmorate.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.User;

import java.time.LocalDate;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/users")
public class UserController {
    public Map<Long, User> users = new HashMap<>();
    private static final Logger log = LoggerFactory.getLogger(UserController.class);

    @GetMapping
    public Collection<User> getUsers() {
        return users.values();
    }

    @PostMapping
    public User createUser(@RequestBody User user) {
        log.info("Попытка создания пользователя: login={}, email={}", user.getLogin(), user.getEmail());

        user.setId(getNextId());
        log.debug("Юзеру присвоен ID: {}", user.getId());

        if (user.getEmail() == null || user.getEmail().isBlank()) {
            log.error("Попытка создания пользователя с пустым email. Login: {}", user.getLogin());
            throw new ValidationException("Электронная почта не может быть пустой!");
        }

        if (!user.getEmail().contains("@")) {
            log.error("Некорректный email: {}. Login: {}", user.getEmail(), user.getLogin());
            throw new ValidationException("Электронная почта должна содержать символ @");
        }

        if (user.getLogin() == null || user.getLogin().isBlank()) {
            log.error("Попытка создания пользователя с пустым логином. Email: {}", user.getEmail());
            throw new ValidationException("Логин не может быть пустым!");
        }

        if (user.getLogin().contains(" ")) {
            log.error("Логин содержит пробелы: '{}'. Email: {}", user.getLogin(), user.getEmail());
            throw new ValidationException("Логин не может содержать пробелы!");
        }

        if (user.getBirthday() != null && user.getBirthday().isAfter(LocalDate.now())) {
            log.error("Дата рождения в будущем: {}. Login: {}", user.getBirthday(), user.getLogin());
            throw new ValidationException("Дата рождения не может быть в будущем!");
        }

        if (user.getName() == null || user.getName().isBlank()) {
            log.info("Имя пользователя '{}' заменено на логин: {}", user.getName(), user.getLogin());
            user.setName(user.getLogin());
        }

        users.put(user.getId(), user);
        log.info("Пользователь создан успешно. ID: {}, login: {}, email: {}",
                user.getId(), user.getLogin(), user.getEmail());
        return user;
    }

    @PutMapping
    public User updateUser(@RequestBody User user) {
        log.info("Попытка обновления пользователя. login: {}", user.getLogin());

        User existingUser = users.get(user.getId());
        if (existingUser == null) {
            log.error("Пользователь с ID {} не найден", user.getId());
            throw new NotFoundException("Пользователь не найден");
        }

        if (user.getEmail() != null) {
            if (user.getEmail().isBlank() || !user.getEmail().contains("@")) {
                log.error("Некорректный email при обновлении: {}", user.getEmail());
                throw new ValidationException("Некорректный email");
            }
        }

        if (user.getLogin() != null) {
            if (user.getLogin().isBlank() || user.getLogin().contains(" ")) {
                log.error("Некорректный логин при обновлении: {}", user.getLogin());
                throw new ValidationException("Некорректный логин");
            }
        }

        if (user.getEmail() != null) {
            existingUser.setEmail(user.getEmail());
        }
        if (user.getName() != null) {
            existingUser.setName(user.getName());
        }
        if (user.getLogin() != null) {
            existingUser.setLogin(user.getLogin());
        }
        if (user.getBirthday() != null) {
            existingUser.setBirthday(user.getBirthday());
        }

        log.info("Пользователь обновлен. login: {}", user.getLogin());
        return existingUser;
    }

    private long getNextId() {
        long currentMaxId = users.keySet()
                .stream()
                .mapToLong(id -> id)
                .max()
                .orElse(0);
        return ++currentMaxId;
    }
}
