package ru.yandex.practicum.filmorate.storage.user;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.model.User;

import java.sql.PreparedStatement;
import java.util.*;

@Slf4j
@Component
@Qualifier("dbUserStorage")
public class UserDbStorage implements UserStorage {

    private final JdbcTemplate jdbcTemplate;

    public UserDbStorage(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private final RowMapper<User> userRowMapper = (rs, rowNum) -> {
        User user = new User();
        user.setId(rs.getLong("user_id"));
        user.setName(rs.getString("name"));
        user.setEmail(rs.getString("email"));
        user.setLogin(rs.getString("login"));
        if (rs.getDate("birthday") != null) {
            user.setBirthday(rs.getDate("birthday").toLocalDate());
        }
        return user;
    };

    public void addFriend(long userId, long friendId) {
        String sql = "INSERT INTO friends (user_id, friend_id) VALUES (?, ?)";
        try {
            jdbcTemplate.update(sql, userId, friendId);
            log.info("Добавлен друг: {} -> {}", userId, friendId);
        } catch (DataAccessException e) {
            log.debug("Друг уже существует: {} -> {}", userId, friendId);
        }
    }

    public void removeFriend(long userId, long friendId) {
        String sql = "DELETE FROM friends WHERE user_id = ? AND friend_id = ?";
        jdbcTemplate.update(sql, userId, friendId);
        log.info("Удален друг: {} -> {}", userId, friendId);
    }

    public List<User> getFriends(long userId) {
        String sql = "SELECT u.* FROM users u " +
                "JOIN friends f ON u.user_id = f.friend_id " +
                "WHERE f.user_id = ?";
        return jdbcTemplate.query(sql, userRowMapper, userId);
    }

    public List<User> getCommonFriends(long userId, long otherUserId) {
        String sql = "SELECT u.* FROM users u " +
                "JOIN friends f1 ON u.user_id = f1.friend_id " +
                "JOIN friends f2 ON u.user_id = f2.friend_id " +
                "WHERE f1.user_id = ? AND f2.user_id = ?";
        return jdbcTemplate.query(sql, userRowMapper, userId, otherUserId);
    }

    @Override
    public User addUser(User user) {
        String sql = "INSERT INTO users (name, email, login, birthday) VALUES (?, ?, ?, ?)";
        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS);
            ps.setString(1, user.getName());
            ps.setString(2, user.getEmail());
            ps.setString(3, user.getLogin());
            ps.setObject(4, user.getBirthday());
            return ps;
        }, keyHolder);

        user.setId(keyHolder.getKey().longValue());
        return user;
    }

    @Override
    public User updateUser(User user) {
        String sql = "UPDATE users SET name = ?, email = ?, login = ?, birthday = ? WHERE user_id = ?";
        jdbcTemplate.update(sql, user.getName(), user.getEmail(), user.getLogin(), user.getBirthday(), user.getId());
        return user;
    }

    @Override
    public User deleteUser(long id) {
        User user = getUserById(id);
        if (user != null) {
            jdbcTemplate.update("DELETE FROM users WHERE user_id = ?", id);
        }
        return user;
    }

    @Override
    public List<User> getAllUsers() {
        return jdbcTemplate.query("SELECT * FROM users", userRowMapper);
    }

    @Override
    public User getUserById(long id) {
        return jdbcTemplate.query("SELECT * FROM users WHERE user_id = ?", userRowMapper, id)
                .stream().findFirst().orElse(null);
    }

    public Optional<User> findUserById(long id) {
        return Optional.ofNullable(getUserById(id));
    }
}
