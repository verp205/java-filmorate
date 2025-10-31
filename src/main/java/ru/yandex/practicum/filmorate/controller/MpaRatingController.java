package ru.yandex.practicum.filmorate.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.filmorate.model.Mpa;
import ru.yandex.practicum.filmorate.service.MpaRatingService;

import java.util.List;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/mpa")
public class MpaRatingController {
    private final MpaRatingService mpaService;

    @GetMapping
    public List<Mpa> getAllMpa() {
        log.info("Запрос на получение всех рейтингов");
        return mpaService.getAllMpa();
    }

    @GetMapping("/{id}")
    public Mpa getMpaById(@PathVariable long id) {
        log.info("Запрос на получение рейтинга с ID {}", id);
        return mpaService.getMpaById(id);
    }
}