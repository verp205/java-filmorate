package ru.yandex.practicum.filmorate.storage.user;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.model.User;

import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.*;

@Component
@Primary
@Qualifier("dbUserStorage")
public class UserDbStorage implements UserStorage {

    private final JdbcTemplate jdbcTemplate;
    private final RowMapper<User> userRowMapper;

    public UserDbStorage(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        this.userRowMapper = createUserRowMapper();
    }

    private RowMapper<User> createUserRowMapper() {
        return (rs, rowNum) -> {
            User user = new User();
            user.setId(rs.getLong("user_id"));
            user.setName(rs.getString("name"));
            user.setEmail(rs.getString("email"));
            user.setLogin(rs.getString("login"));

            Date birthday = rs.getDate("birthday");
            if (birthday != null) {
                user.setBirthday(birthday.toLocalDate());
            }

            // Загружаем друзей
            Set<Long> friends = getFriendsIds(user.getId());
            user.setFriends(friends);

            return user;
        };
    }

    private Set<Long> getFriendsIds(long userId) {
        String sql = "SELECT friend_id FROM friends WHERE user_id = ?";
        List<Long> friendIds = jdbcTemplate.queryForList(sql, Long.class, userId);
        return new HashSet<>(friendIds);
    }

    private void saveFriends(long userId, Set<Long> friends) {
        jdbcTemplate.update("DELETE FROM friends WHERE user_id = ?", userId);
        if (friends == null || friends.isEmpty()) return;

        String insertFriendSql = "INSERT INTO friends (user_id, friend_id) VALUES (?, ?)";
        for (Long friendId : friends) {
            jdbcTemplate.update(insertFriendSql, userId, friendId);
        }
    }

    @Override
    public User addUser(User user) {
        String sql = "INSERT INTO users (name, email, login, birthday) VALUES (?, ?, ?, ?)";
        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, user.getName());
            ps.setString(2, user.getEmail());
            ps.setString(3, user.getLogin());
            ps.setDate(4, user.getBirthday() != null ? Date.valueOf(user.getBirthday()) : null);
            return ps;
        }, keyHolder);

        long generatedId = keyHolder.getKey().longValue();
        user.setId(generatedId);

        saveFriends(generatedId, user.getFriends());

        return getUserById(generatedId);
    }

    @Override
    public User updateUser(User user) {
        String sql = "UPDATE users SET name = ?, email = ?, login = ?, birthday = ? WHERE user_id = ?";

        jdbcTemplate.update(sql,
                user.getName(),
                user.getEmail(),
                user.getLogin(),
                user.getBirthday() != null ? Date.valueOf(user.getBirthday()) : null,
                user.getId());

        saveFriends(user.getId(), user.getFriends());

        return getUserById(user.getId());
    }

    @Override
    public User deleteUser(long id) {
        User user = getUserById(id);
        if (user != null) {
            jdbcTemplate.update("DELETE FROM friends WHERE user_id = ? OR friend_id = ?", id, id);
            jdbcTemplate.update("DELETE FROM likes WHERE user_id = ?", id);
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

    @Override
    public Optional<User> findUserById(long id) {
        return Optional.ofNullable(getUserById(id));
    }

    @Override
    public void addFriend(long userId, long friendId) {
        String sql = "INSERT INTO friends (user_id, friend_id) VALUES (?, ?)";
        jdbcTemplate.update(sql, userId, friendId);
    }

    @Override
    public void removeFriend(long userId, long friendId) {
        String sql = "DELETE FROM friends WHERE user_id = ? AND friend_id = ?";
        jdbcTemplate.update(sql, userId, friendId);
    }

    @Override
    public List<User> getFriends(long userId) {
        String sql = "SELECT u.* FROM users u " +
                "JOIN friends f ON u.user_id = f.friend_id " +
                "WHERE f.user_id = ?";
        return jdbcTemplate.query(sql, userRowMapper, userId);
    }

    @Override
    public List<User> getCommonFriends(long userId1, long userId2) {
        String sql = "SELECT u.* FROM users u " +
                "JOIN friends f1 ON u.user_id = f1.friend_id " +
                "JOIN friends f2 ON u.user_id = f2.friend_id " +
                "WHERE f1.user_id = ? AND f2.user_id = ?";
        return jdbcTemplate.query(sql, userRowMapper, userId1, userId2);
    }
}