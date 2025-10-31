-- ========================
-- Таблица MPA рейтингов
-- ========================
MERGE INTO mpa_ratings (rating_id, rating_name) KEY(rating_id) VALUES
    (1, 'G'),
    (2, 'PG'),
    (3, 'PG-13'),
    (4, 'R'),
    (5, 'NC-17');

-- ========================
-- Таблица жанров (6 жанров как требуется в тестах)
-- ========================
MERGE INTO genres (genre_id, genre_name) KEY(genre_id) VALUES
    (1, 'Комедия'),
    (2, 'Драма'),
    (3, 'Мультфильм'),
    (4, 'Триллер'),
    (5, 'Документальный'),
    (6, 'Боевик');

-- ========================
-- Таблица пользователей (очищаем ID для автоинкремента)
-- ========================
MERGE INTO users (user_id, name, email, login, birthday) KEY(user_id) VALUES
    (1, 'User One', 'user1@example.com', 'user1', '1990-01-01'),
    (2, 'User Two', 'user2@example.com', 'user2', '1991-02-02'),
    (3, 'User Three', 'user3@example.com', 'user3', '1992-03-03');

-- ========================
-- Таблица фильмов (очищаем ID для автоинкремента)
-- ========================
MERGE INTO films (film_id, name, description, release_date, duration, mpa_rating_id) KEY(film_id) VALUES
    (1, 'Film One', 'First film description', '2020-01-01', 120, 1),
    (2, 'Film Two', 'Second film description', '2021-02-15', 90, 2),
    (3, 'Film Three', 'Third film description', '2022-03-10', 110, 3);

-- ========================
-- Таблица film_genres (связь фильмов и жанров)
-- ========================
MERGE INTO film_genres (film_id, genre_id) KEY(film_id, genre_id) VALUES
    (1, 1),  -- Film One - Комедия
    (1, 2),  -- Film One - Драма
    (2, 3),  -- Film Two - Мультфильм
    (3, 4);  -- Film Three - Триллер

-- ========================
-- Таблица лайков (упрощенная структура без like_id)
-- ========================
MERGE INTO likes (user_id, film_id) KEY(user_id, film_id) VALUES
    (1, 1),  -- User 1 лайкнул Film 1
    (2, 1),  -- User 2 лайкнул Film 1
    (1, 2);  -- User 1 лайкнул Film 2

-- ========================
-- Таблица друзей (односторонняя дружба без статусов)
-- ========================
MERGE INTO friends (user_id, friend_id) KEY(user_id, friend_id) VALUES
    (1, 2),  -- User 1 добавил User 2 в друзья
    (1, 3),  -- User 1 добавил User 3 в друзья
    (2, 3);  -- User 2 добавил User 3 в друзья