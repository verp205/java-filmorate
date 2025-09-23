package ru.yandex.practicum.filmorate.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.User;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class UserControllerTest {

    private UserController userController;

    @BeforeEach
    void setUp() {
        userController = new UserController();
    }

    @Test
    void createUser_ValidUser_ShouldCreateSuccessfully() {
        User validUser = createValidUser();

        User result = userController.createUser(validUser);

        assertNotNull(result.getId());
        assertEquals("user", result.getLogin());
        assertEquals("user@example.com", result.getEmail());
    }

    @Test
    void createUser_NullEmail_ShouldThrowException() {
        User user = createValidUser();
        user.setEmail(null);

        ValidationException exception = assertThrows(
                ValidationException.class,
                () -> userController.createUser(user)
        );
        assertEquals("Электронная почта не может быть пустой!", exception.getMessage());
    }

    @Test
    void createUser_EmptyEmail_ShouldThrowException() {
        User user = createValidUser();
        user.setEmail("   ");

        ValidationException exception = assertThrows(
                ValidationException.class,
                () -> userController.createUser(user)
        );
        assertEquals("Электронная почта не может быть пустой!", exception.getMessage());
    }

    @Test
    void createUser_EmailWithoutAtSymbol_ShouldThrowException() {
        User user = createValidUser();
        user.setEmail("invalid-email");

        ValidationException exception = assertThrows(
                ValidationException.class,
                () -> userController.createUser(user)
        );
        assertEquals("Электронная почта должна содержать символ @", exception.getMessage());
    }

    @Test
    void createUser_NullLogin_ShouldThrowException() {
        User user = createValidUser();
        user.setLogin(null);

        ValidationException exception = assertThrows(
                ValidationException.class,
                () -> userController.createUser(user)
        );
        assertEquals("Логин не может быть пустым!", exception.getMessage());
    }

    @Test
    void createUser_EmptyLogin_ShouldThrowException() {
        User user = createValidUser();
        user.setLogin("   ");

        ValidationException exception = assertThrows(
                ValidationException.class,
                () -> userController.createUser(user)
        );
        assertEquals("Логин не может быть пустым!", exception.getMessage());
    }

    @Test
    void createUser_LoginWithSpaces_ShouldThrowException() {
        User user = createValidUser();
        user.setLogin("user with spaces");

        ValidationException exception = assertThrows(
                ValidationException.class,
                () -> userController.createUser(user)
        );
        assertEquals("Логин не может содержать пробелы!", exception.getMessage());
    }

    @Test
    void createUser_FutureBirthday_ShouldThrowException() {
        User user = createValidUser();
        user.setBirthday(LocalDate.now().plusDays(1));

        ValidationException exception = assertThrows(
                ValidationException.class,
                () -> userController.createUser(user)
        );
        assertEquals("Дата рождения не может быть в будущем!", exception.getMessage());
    }

    @Test
    void createUser_NullName_ShouldUseLoginAsName() {
        User user = createValidUser();
        user.setName(null);

        User result = userController.createUser(user);

        assertEquals(user.getLogin(), result.getName());
    }

    @Test
    void createUser_EmptyName_ShouldUseLoginAsName() {
        User user = createValidUser();
        user.setName("   ");

        User result = userController.createUser(user);

        assertEquals(user.getLogin(), result.getName());
    }

    @Test
    void createUser_TodayBirthday_ShouldCreateSuccessfully() {
        User user = createValidUser();
        user.setBirthday(LocalDate.now());

        User result = userController.createUser(user);

        assertEquals(LocalDate.now(), result.getBirthday());
    }

    @Test
    void createUser_PastBirthday_ShouldCreateSuccessfully() {
        User user = createValidUser();
        user.setBirthday(LocalDate.of(1990, 1, 1));

        User result = userController.createUser(user);

        assertEquals(LocalDate.of(1990, 1, 1), result.getBirthday());
    }

    @Test
    void createUser_EmailWithMultipleAtSymbols_ShouldCreateSuccessfully() {
        User user = createValidUser();
        user.setEmail("user@company@example.com");

        User result = userController.createUser(user);

        assertEquals("user@company@example.com", result.getEmail());
    }

    @Test
    void updateUser_NonExistentId_ShouldThrowException() {
        User user = createValidUser();
        user.setId(999L);

        NotFoundException exception = assertThrows(
                NotFoundException.class,
                () -> userController.updateUser(user)
        );
        assertEquals("Пользователь не найден", exception.getMessage());
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