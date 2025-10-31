package ru.yandex.practicum.filmorate.model;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

@Data
@EqualsAndHashCode(of = "id")
public class User {
    private long id;
    private String name;
    private String email;
    private String login;
    private LocalDate birthday;
    private Set<Long> friends = new HashSet<>();
    private String friendStatus;

    public void addFriend(long friendId) {
        friends.add(friendId);
    }

    public void removeFriend(long friendId) {
        friends.remove(friendId);
    }

    public Set<Long> getFriends() {
        return new HashSet<>(friends);
    }
}
