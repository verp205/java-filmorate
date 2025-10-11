package ru.yandex.practicum.filmorate.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.service.UserService;
import ru.yandex.practicum.filmorate.storage.user.InMemoryUserStorage;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class UserControllerTest {

    private UserController userController;

    @BeforeEach
    void setUp() {
        InMemoryUserStorage userStorage = new InMemoryUserStorage();
        UserService userService = new UserService(userStorage);
        userController = new UserController(userService);
    }

    @Test
    void createUser_ValidUser_ShouldCreateSuccessfully() {
        User validUser = createValidUser();

        User result = userController.addUser(validUser);

        assertNotNull(result.getId());
        assertEquals("user", result.getLogin());
        assertEquals("user@example.com", result.getEmail());
    }

    @Test
    void createUser_NullEmail_ShouldThrowException() {
        User user = createValidUser();
        user.setEmail(null);

        assertThrows(ValidationException.class, () -> userController.addUser(user));
    }

    @Test
    void createUser_EmailWithoutAtSymbol_ShouldThrowException() {
        User user = createValidUser();
        user.setEmail("invalidemail");

        assertThrows(ValidationException.class, () -> userController.addUser(user));
    }

    @Test
    void createUser_NullLogin_ShouldThrowException() {
        User user = createValidUser();
        user.setLogin(null);

        assertThrows(ValidationException.class, () -> userController.addUser(user));
    }

    @Test
    void createUser_EmptyLogin_ShouldThrowException() {
        User user = createValidUser();
        user.setLogin("  ");

        assertThrows(ValidationException.class, () -> userController.addUser(user));
    }

    @Test
    void createUser_LoginWithSpaces_ShouldThrowException() {
        User user = createValidUser();
        user.setLogin("user with spaces");

        assertThrows(ValidationException.class, () -> userController.addUser(user));
    }

    @Test
    void createUser_FutureBirthday_ShouldThrowException() {
        User user = createValidUser();
        user.setBirthday(LocalDate.now().plusDays(1));

        assertThrows(ValidationException.class, () -> userController.addUser(user));
    }

    @Test
    void createUser_NullName_ShouldUseLoginAsName() {
        User user = createValidUser();
        user.setName(null);

        User result = userController.addUser(user);
        assertEquals(user.getLogin(), result.getName());
    }

    @Test
    void updateUser_NonExistentId_ShouldThrowException() {
        User user = createValidUser();
        user.setId(999L);

        assertThrows(NotFoundException.class, () -> userController.updateUser(user));
    }

    private User createValidUser() {
        User user = new User();
        user.setEmail("user@example.com");
        user.setLogin("user");
        user.setName("User");
        user.setBirthday(LocalDate.of(1990, 1, 1));
        return user;
    }
}
