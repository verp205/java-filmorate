package ru.yandex.practicum.filmorate.storage.user;

import ru.yandex.practicum.filmorate.model.User;

import java.util.List;
import java.util.Map;

public interface UserStorage {

    User addUser(User user);

    User updateUser(User user);

    User deleteUser(long id);

    List<User> getAllUsers();

    User getUserById(long id);
}
