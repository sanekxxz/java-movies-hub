package ru.practicum.moviehub.api;

import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;


public class ErrorResponse {

    public ErrorResponse() {

    }

    public void error(int code, String error, HttpExchange exchange) throws IOException {

        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");

        byte[] responseBytes = error.getBytes(StandardCharsets.UTF_8);

        exchange.sendResponseHeaders(code, responseBytes.length);

        try (OutputStream outputStream = exchange.getResponseBody()) {
            outputStream.write(responseBytes);
        }
    }
}