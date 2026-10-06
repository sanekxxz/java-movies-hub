package ru.practicum.moviehub.store;

import ru.practicum.moviehub.model.Movie;

import java.util.HashMap;
import java.util.Map;

public class MoviesStore {

    private final Map<Integer, Movie> movies = new HashMap<>();
    private int nextID = 1;

    public MoviesStore() {

    }

    public Movie addMovie(String title, int year) {
        Movie movie = new Movie(nextID, title, year);
        movies.put(nextID, movie);
        nextID++;

        return movie;
    }

    public Map<Integer, Movie> getMovie() {
        return movies;
    }

    public void clear() {
        movies.clear();
        this.nextID = 1;
    }
}