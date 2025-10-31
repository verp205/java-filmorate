package ru.yandex.practicum.filmorate.controller.DBtests;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.storage.user.UserDbStorage;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@JdbcTest
@AutoConfigureTestDatabase
@RequiredArgsConstructor(onConstructor_ = @Autowired)
@Import(UserDbStorage.class)
class UserDbStorageTest {

    private final UserDbStorage userStorage;
    private final JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        jdbcTemplate.execute("DELETE FROM friends");
        jdbcTemplate.execute("DELETE FROM users");

        User user1 = new User();
        user1.setName("Alice");
        user1.setEmail("alice@example.com");
        user1.setLogin("alice123");
        user1.setBirthday(LocalDate.of(1990, 1, 1));
        userStorage.addUser(user1);

        User user2 = new User();
        user2.setName("Bob");
        user2.setEmail("bob@example.com");
        user2.setLogin("bob123");
        user2.setBirthday(LocalDate.of(1992, 2, 2));
        userStorage.addUser(user2);
    }

    @Test
    void testAddUser() {
        User user = new User();
        user.setName("Charlie");
        user.setEmail("charlie@example.com");
        user.setLogin("charlie123");
        user.setBirthday(LocalDate.of(1995, 5, 5));

        User saved = userStorage.addUser(user);
        assertThat(saved.getId()).isPositive();

        Optional<User> fetched = userStorage.findUserById(saved.getId());
        assertThat(fetched).isPresent().hasValueSatisfying(u -> {
            assertThat(u.getName()).isEqualTo("Charlie");
            assertThat(u.getEmail()).isEqualTo("charlie@example.com");
            assertThat(u.getLogin()).isEqualTo("charlie123");
            assertThat(u.getBirthday()).isEqualTo(LocalDate.of(1995, 5, 5));
        });
    }

    @Test
    void testAddUserWithEmptyName() {
        User user = new User();
        user.setName("");
        user.setEmail("noname@example.com");
        user.setLogin("noname123");
        user.setBirthday(LocalDate.of(1990, 1, 1));

        User saved = userStorage.addUser(user);
        assertThat(saved.getId()).isPositive();
        assertThat(saved.getName()).isEmpty();
    }

    @Test
    void testGetAllUsers() {
        List<User> users = userStorage.getAllUsers();
        assertThat(users).hasSize(2);

        User firstUser = users.get(0);
        assertThat(firstUser.getName()).isIn("Alice", "Bob");
        assertThat(firstUser.getEmail()).contains("@example.com");
        assertThat(firstUser.getLogin()).isNotEmpty();
        assertThat(firstUser.getBirthday()).isNotNull();
    }

    @Test
    void testUpdateUser() {
        User user = userStorage.getAllUsers().get(0);
        user.setName("UpdatedName");
        user.setEmail("updated@example.com");
        user.setLogin("updated123");
        user.setBirthday(LocalDate.of(2000, 1, 1));

        userStorage.updateUser(user);

        Optional<User> updated = userStorage.findUserById(user.getId());
        assertThat(updated).isPresent().hasValueSatisfying(u -> {
            assertThat(u.getName()).isEqualTo("UpdatedName");
            assertThat(u.getEmail()).isEqualTo("updated@example.com");
            assertThat(u.getLogin()).isEqualTo("updated123");
            assertThat(u.getBirthday()).isEqualTo(LocalDate.of(2000, 1, 1));
        });
    }

    @Test
    void testDeleteUser() {
        User user = userStorage.getAllUsers().get(0);
        User deleted = userStorage.deleteUser(user.getId());
        assertThat(deleted).isNotNull();
        assertThat(deleted.getName()).isEqualTo(user.getName());

        Optional<User> fetched = userStorage.findUserById(user.getId());
        assertThat(fetched).isEmpty();

        List<User> users = userStorage.getAllUsers();
        assertThat(users).hasSize(1);
    }

    @Test
    void testDeleteNonExistentUser() {
        User deleted = userStorage.deleteUser(9999L);
        assertNull(deleted);
    }

    @Test
    void testFindUserById() {
        User existingUser = userStorage.getAllUsers().get(0);
        Optional<User> userOptional = userStorage.findUserById(existingUser.getId());

        assertThat(userOptional).isPresent();
        assertThat(userOptional.get().getId()).isEqualTo(existingUser.getId());
        assertThat(userOptional.get().getName()).isEqualTo(existingUser.getName());
    }

    @Test
    void testFindUserByIdNonExistent() {
        Optional<User> userOptional = userStorage.findUserById(9999L);
        assertThat(userOptional).isEmpty();
    }

    @Test
    void testGetUserById() {
        User existingUser = userStorage.getAllUsers().get(0);
        User user = userStorage.getUserById(existingUser.getId());

        assertThat(user).isNotNull();
        assertThat(user.getId()).isEqualTo(existingUser.getId());
        assertThat(user.getName()).isEqualTo(existingUser.getName());
        assertThat(user.getEmail()).isEqualTo(existingUser.getEmail());
        assertThat(user.getLogin()).isEqualTo(existingUser.getLogin());
        assertThat(user.getBirthday()).isEqualTo(existingUser.getBirthday());
    }

    @Test
    void testGetUserByIdNonExistent() {
        User user = userStorage.getUserById(9999L);
        assertNull(user);
    }

    @Test
    void testUserWithoutFriends() {
        User user = userStorage.getAllUsers().get(0);
        assertThat(user.getFriends()).isEmpty();
    }

    @Test
    void testUpdateUserRemoveAllFriends() {
        User user1 = userStorage.getAllUsers().get(0);
        User user2 = userStorage.getAllUsers().get(1);

        jdbcTemplate.update("INSERT INTO friends (user_id, friend_id) VALUES (?, ?)",
                user1.getId(), user2.getId());

        user1.getFriends().clear();
        userStorage.updateUser(user1);

        User finalUser = userStorage.getUserById(user1.getId());
        assertThat(finalUser.getFriends()).isEmpty();
    }

    @Test
    void testUserEquality() {
        User user1 = userStorage.getAllUsers().get(0);
        User user2 = userStorage.getUserById(user1.getId());

        assertEquals(user1, user2);
        assertEquals(user1.hashCode(), user2.hashCode());
    }

    @Test
    void testUserWithEmptyFriends() {
        User user = new User();
        user.setName("Empty Friends");
        user.setEmail("empty@example.com");
        user.setLogin("empty123");
        user.setBirthday(LocalDate.of(1990, 1, 1));

        User saved = userStorage.addUser(user);
        assertThat(saved.getFriends()).isEmpty();
    }
}