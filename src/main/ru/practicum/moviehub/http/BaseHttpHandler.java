package ru.practicum.moviehub.http;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import ru.practicum.moviehub.api.ErrorResponse;
import ru.practicum.moviehub.model.Movie;
import ru.practicum.moviehub.store.MoviesStore;

import java.io.*;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class BaseHttpHandler implements HttpHandler {

	private final MoviesStore moviesStore;
	private final Gson gson = new Gson();
	private Map<String, String> params;

	public BaseHttpHandler(MoviesStore moviesStore) {
		this.moviesStore = moviesStore;
	}

	@Override
	public void handle(HttpExchange httpExchange) throws IOException {
		String method = httpExchange.getRequestMethod();
		switch (method) {
			case "GET":
				getMovies(httpExchange);
				break;
			case "POST":
				postMovies(httpExchange);
				break;
			case "DELETE":
				deleteMovies(httpExchange);
				break;
		}
	}

	private void deleteMovies(HttpExchange exchange) throws IOException {
		String path = exchange.getRequestURI().getPath();
		String[] parts = path.split("/");
		if (parts.length == 3 && parts[1].equals("movies")) {
			if (parts[2].matches("\\d+")) {
				int id = Integer.parseInt(parts[2]);
				if (moviesStore.getMovie().containsKey(id)) {
					moviesStore.getMovie().remove(id);
					sendResponse(204, "", exchange);
				} else {
					sendJson(exchange,404,new ErrorResponse("Фильм не найден", List.of()));
				}
			} else {
				sendJson(exchange,400,new ErrorResponse("Некорректный ID", List.of()));
			}
			return;
		}
		sendJson(exchange,400,new ErrorResponse("Некорректный запрос", List.of()));
	}

	private void getMovies(HttpExchange exchange) throws IOException {

		params = getQueryParams(exchange.getRequestURI());

		String title = params.get("title");
		String yearString = params.get("year");
		if (title == null && yearString != null && !yearString.isBlank()) {
			try {
				int year = Integer.parseInt(yearString);
				boolean isValid = year >= 1888 && year <= 2026;

				if (isValid) {
					String response = gson.toJson(moviesStore.getMovie().values().stream()
							.filter(movie -> movie.getYear() == year)
							.collect(Collectors.toSet()));
					sendResponse(200, response, exchange);
					return;
				} else {
					sendJson(exchange,400,new ErrorResponse("Некорректный параметр запроса — 'year'", List.of()));
					return;
				}
			} catch (NumberFormatException e) {
				sendJson(exchange,400,new ErrorResponse("Некорректный запрос", List.of()));
				return;
			}
		}

		String path = exchange.getRequestURI().getPath();
		String[] parts = path.split("/");
		if (parts.length == 3 && parts[1].equals("movies")) {
			if (parts[2].matches("\\d+")) {
				int id = Integer.parseInt(parts[2]);
				if (moviesStore.getMovie().containsKey(id)) {
					String response = gson.toJson(moviesStore.getMovie().get(id));
					sendResponse(200, response, exchange);
				} else {
					sendJson(exchange,404,new ErrorResponse("Фильм не найден", List.of()));
					return;
				}
			} else {
				sendJson(exchange,400,new ErrorResponse("Некорректный ID", List.of()));
				return;
			}
		} else if (parts.length == 2 && parts[1].equals("movies")) {
			String response = gson.toJson(moviesStore.getMovie().values());
			sendResponse(200, response, exchange);
			return;
		}
		sendJson(exchange,400,new ErrorResponse("Некорректный запрос", List.of()));
	}

	private void postMovies(HttpExchange exchange) throws IOException {
		String contentType = exchange.getRequestHeaders().getFirst("Content-Type");
		boolean content = contentType != null && !contentType.startsWith("application/json");
		if (content) {
			sendJson(exchange,415,new ErrorResponse("Не верный Content-Type", List.of()));
			return;
		}

		Movie movie;
		InputStream inputStream = exchange.getRequestBody();
		try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream))) {
			movie = gson.fromJson(bufferedReader, Movie.class);
		} catch (JsonSyntaxException e) {
			sendJson(exchange,400,new ErrorResponse("Не верный JSON", List.of()));
			return;
		}

		exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
		if (isValidMovies(movie)) {
			moviesStore.addMovie(movie.getTitle(), movie.getYear());
			String response = gson.toJson(movie);
			sendResponse(201, response, exchange);
		} else {
			List<String> details = List.of(
					"Название не должно быть пустым",
					"Год должен быть от 1888 до текущего года плюс один"
			);

			sendJson(exchange,422,new ErrorResponse("Ошибка валидации", details));
		}
	}

	private void sendResponse(final int code, final String response, final HttpExchange exchange) throws IOException {
		if (code == 204) {
			exchange.sendResponseHeaders(code, -1);
			return;
		}
		exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
		byte[] responseBytes = response.getBytes(StandardCharsets.UTF_8);
		exchange.sendResponseHeaders(code, responseBytes.length);
		try (OutputStream outputStream = exchange.getResponseBody()) {
			outputStream.write(responseBytes);
		}
	}

	private Map<String, String> getQueryParams(URI uri) {
		Map<String, String> params = new HashMap<>();
		String query = uri.getQuery();
		if (query == null || query.isBlank()) {
			return params;
		}
		String[] pairs = query.split("&");
		for (String pair : pairs) {
			String[] keyValue = pair.split("=", 2);
			String key = keyValue[0];
			String value = keyValue.length > 1
					? keyValue[1]
					: "";
			params.put(key, value);
		}
		return params;
	}

	private boolean isValidMovies(final Movie movie) {
		return movie != null && movie.getTitle() != null
				&& !movie.getTitle().isBlank()
				&& movie.getTitle().length() <= 100
				&& movie.getYear() >= 1888
				&& movie.getYear() <= 2026;
	}

	private void sendJson(HttpExchange exchange, int statusCode, ErrorResponse errorResponse) throws IOException {
		String json = gson.toJson(errorResponse);

		byte[] responseBytes = json.getBytes(StandardCharsets.UTF_8);

		exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");

		exchange.sendResponseHeaders(statusCode, responseBytes.length);

		try (OutputStream outputStream = exchange.getResponseBody()) {
			outputStream.write(responseBytes);
		}
	}
}