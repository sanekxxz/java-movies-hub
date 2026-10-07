package ru.practicum.moviehub.model;

import java.util.Objects;

public class Movie {
    private final int id;
    private final String title;
    private final int year;

    public Movie(int id, String title, int year) {
        this.id = id;
        this.title = title;
        this.year = year;
    }

    public int getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public int getYear() {
        return year;
    }

    @Override
    public boolean equals(Object object) {
        if (object == null || getClass() != object.getClass()) return false;
        Movie movie = (Movie) object;
        return id == movie.id && year == movie.year && Objects.equals(title, movie.title);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, title, year);
    }

    @Override
    public String toString() {
        return "Movie{" +
                "ID=" + id +
                ", Название='" + title + '\'' +
                ", Год=" + year +
                '}';
    }
}