package ru.practicum.moviehub.http;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import ru.practicum.moviehub.store.MoviesStore;

import java.io.IOException;
import java.net.InetSocketAddress;

public class MoviesServer {

    private final HttpServer server;
    private final MoviesStore moviesStore;

    public MoviesServer(MoviesStore moviesStore, int port) {
        this.moviesStore = moviesStore;

        try {
            server = HttpServer.create(new InetSocketAddress(port), 0);
            server.createContext("/movies", new BaseHttpHandler(moviesStore));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public MoviesStore getMoviesStore() {
        return moviesStore;
    }

    public void stop() {
        server.stop(0);
        System.out.println("Сервер остановлен");
    }

    public void start() {
        server.start();
        System.out.println("Сервер запущен");
    }
}