package ru.yandex.practicum.filmorate.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.storage.user.UserDbStorage;
import ru.yandex.practicum.filmorate.storage.user.UserStorage;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class UserService {
    private final UserStorage userStorage;

    @Autowired
    public UserService(@Qualifier("dbUserStorage") UserStorage userStorage) {
        this.userStorage = userStorage;
    }

    public void addFriend(long userId, long friendId) {
        getUserById(userId);
        getUserById(friendId);

        ((UserDbStorage) userStorage).addFriend(userId, friendId);

        log.info("Односторонняя дружба: {} → {}", userId, friendId);
    }

    public void deleteFriend(long userId, long friendId) {
        getUserById(userId);
        getUserById(friendId);

        ((UserDbStorage) userStorage).removeFriend(userId, friendId);

        log.info("Пользователь {} удалил из друзей {}", userId, friendId);
    }

    public List<User> getFriends(long userId) {
        getUserById(userId);

        return ((UserDbStorage) userStorage).getFriends(userId);
    }

    public List<User> getCommonFriends(long userId, long otherUserId) {
        getUserById(userId);
        getUserById(otherUserId);

        return ((UserDbStorage) userStorage).getCommonFriends(userId, otherUserId);
    }

    private User getUserById(long userId) {
        User user = userStorage.getUserById(userId);
        if (user == null) {
            throw new NotFoundException("Пользователь с ID " + userId + " не найден");
        }
        return user;
    }

    public User getUserByIdPublic(long userId) {
        return getUserById(userId);
    }

    public User addUser(User user) {
        return userStorage.addUser(user);
    }

    public User updateUser(User user) {
        if (userStorage.getUserById(user.getId()) == null) {
            throw new NotFoundException("Пользователь с ID " + user.getId() + " не найден");
        }
        return userStorage.updateUser(user);
    }

    public List<User> getAllUsers() {
        return new ArrayList<>(userStorage.getAllUsers());
    }
}