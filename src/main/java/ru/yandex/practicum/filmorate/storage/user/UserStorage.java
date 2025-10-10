package ru.yandex.practicum.filmorate.storage.user;

import ru.yandex.practicum.filmorate.model.User;

import java.util.Map;

public interface UserStorage {
    User addUser(long id);
    User deletUser(long id);
    User updateUser(User user);
    Map<Long, User> getAllUsers();
    User createUser(User user);
}
