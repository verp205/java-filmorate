CREATE TABLE IF NOT EXISTS users (
    user_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(150) NOT NULL,
    email VARCHAR(255) NOT NULL,
    login VARCHAR(100) NOT NULL,
    birthday DATE,
    CONSTRAINT uq_users_email UNIQUE (email)
    );

CREATE TABLE IF NOT EXISTS mpa_ratings (
    rating_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    rating_name VARCHAR(50) NOT NULL
    );

CREATE TABLE IF NOT EXISTS films (
    film_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    description VARCHAR(1000),
    release_date DATE NOT NULL,
    duration INT NOT NULL,
    mpa_rating_id BIGINT,
    CONSTRAINT fk_films_mpa FOREIGN KEY (mpa_rating_id) REFERENCES mpa_ratings(rating_id)
    );

CREATE TABLE IF NOT EXISTS genres (
    genre_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    genre_name VARCHAR(100) NOT NULL
    );

CREATE TABLE IF NOT EXISTS film_genres (
    film_id BIGINT NOT NULL,
    genre_id BIGINT NOT NULL,
    PRIMARY KEY (film_id, genre_id),
    CONSTRAINT fk_fg_film FOREIGN KEY (film_id) REFERENCES films(film_id) ON DELETE CASCADE,
    CONSTRAINT fk_fg_genre FOREIGN KEY (genre_id) REFERENCES genres(genre_id) ON DELETE CASCADE
    );

CREATE TABLE IF NOT EXISTS likes (
    like_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    film_id BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_likes_user FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    CONSTRAINT fk_likes_film FOREIGN KEY (film_id) REFERENCES films(film_id) ON DELETE CASCADE,
    CONSTRAINT uq_likes_user_film UNIQUE (user_id, film_id)
    );

CREATE TABLE IF NOT EXISTS friends (
    friendship_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    friend_id BIGINT NOT NULL,
    friend_status VARCHAR(50),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_friends_user FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    CONSTRAINT fk_friends_friend FOREIGN KEY (friend_id) REFERENCES users(user_id) ON DELETE CASCADE,
    CONSTRAINT uq_friends_pair UNIQUE (user_id, friend_id)
    );