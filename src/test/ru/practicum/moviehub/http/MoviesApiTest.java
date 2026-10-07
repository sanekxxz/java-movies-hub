package ru.practicum.moviehub.http;

import com.google.gson.Gson;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import ru.practicum.moviehub.api.ErrorResponse;
import ru.practicum.moviehub.model.Movie;
import ru.practicum.moviehub.store.MoviesStore;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;

public class MoviesApiTest {

    private static final MoviesServer moviesServer = new MoviesServer(new MoviesStore(), 8080);
    private static HttpClient httpClient;
    private static final String uri = "http://localhost:8080/";
    private static Gson gson = new Gson();
    private static String type = "application/json; charset=UTF-8";

    private HttpResponse<String> getMovies() throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(uri + "movies"))
                .header("Content-Type", "application/json; charset=UTF-8")
                .GET()
                .build();

        return httpClient.send(
                request,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)
        );
    }

    private HttpResponse<String> getMovieById(int id) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(uri + "movies/" + id))
                .header("Content-Type", "application/json; charset=UTF-8")
                .GET()
                .build();

        return httpClient.send(
                request,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)
        );
    }

    private HttpResponse<String> postMovies(String movieJson, String contentType)
            throws IOException, InterruptedException {

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(uri + "movies"))
                .header("Content-Type", contentType)
                .POST(HttpRequest.BodyPublishers.ofString(
                        movieJson,
                        StandardCharsets.UTF_8
                ))
                .build();

        return httpClient.send(
                request,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)
        );
    }

    @BeforeAll
    static void beforeAll() {
        moviesServer.start();
        httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build();
    }

    @BeforeEach
    void beforeEach() {
        moviesServer.getMoviesStore().clear();
        moviesServer.getMoviesStore().addMovie("Tormentor", 2000);
        moviesServer.getMoviesStore().addMovie("Isis", 1999);
        moviesServer.getMoviesStore().addMovie("Gladiator", 2000);
        moviesServer.getMoviesStore().addMovie("Interstellar", 2014);
        moviesServer.getMoviesStore().addMovie("Inception", 2010);
        moviesServer.getMoviesStore().addMovie("Avatar", 2009);
        moviesServer.getMoviesStore().addMovie("Titanic", 1997);
        moviesServer.getMoviesStore().addMovie("Matrix", 1999);
        moviesServer.getMoviesStore().addMovie("Alien", 1979);
        moviesServer.getMoviesStore().addMovie("Joker", 2000);
    }

    @AfterAll
    static void afterAll() {
        moviesServer.stop();
    }

    @Test
    void getMovies_returns_a_list_with_previously_added_movies() throws Exception {

        HttpResponse<String> resp = getMovies();

        assertEquals(200, resp.statusCode());

        assertEquals("application/json; charset=UTF-8", resp.headers().firstValue("Content-Type").orElse(null));

        List<Movie> movies = gson.fromJson(
                resp.body(),
                new ListOfMoviesTypeToken().getType()
        );

        assertEquals(10, movies.size());

    }

    @Test
    void getMovies_whenEmpty_returnsEmptyArray() throws Exception {
        moviesServer.getMoviesStore().clear();

        HttpResponse<String> resp = getMovies();

        List<Movie> movies = gson.fromJson(
                resp.body(),
                new ListOfMoviesTypeToken().getType()
        );

        assertEquals("application/json; charset=UTF-8", resp.headers().firstValue("Content-Type").orElse(null));

        assertEquals(200, resp.statusCode(), "GET /movies должен вернуть 200");


        assertEquals(0, movies.size(), "GET /movies вернёт пустой массив");


    }

    @Test
    void postMovies_adds_filmsDataIsCorrect() throws Exception {

        moviesServer.getMoviesStore().clear();

        String movieJson = "{\"title\":\"Mus\",\"year\":2006}";

        HttpResponse<String> correctData = postMovies(movieJson, type);


        assertEquals(201, correctData.statusCode());
    }

    @Test
    void postMovies_adds_filmsDataEmptyTitle() throws Exception {


        String movieJson = "{\"title\":\"\",\"year\":2006}";

        HttpResponse<String> isEmptyTitle = postMovies(movieJson, type);

        assertEquals(422, isEmptyTitle.statusCode());

        ErrorResponse error = gson.fromJson(
                isEmptyTitle.body(),
                ErrorResponse.class
        );

        assertEquals("Ошибка валидации", error.getError());
        assertTrue(error.getDetails().contains("Название не должно быть пустым"));
    }

    @Test
    void postMovies_returnErrorLengthMore100() throws Exception {

        String movieJson = "{\"title\":\"asddaasdasdasdasssssssssssssssssssssssswaqwqweqgfdgdfgdfgaewfdsvfswesdasfsdvewrvfdvvevrerggegsfsafsfsa\",\"year\":2006}";
        HttpResponse<String> lineTitle = postMovies(movieJson, type);


        assertEquals(422, lineTitle.statusCode());

        ErrorResponse error = gson.fromJson(
                lineTitle.body(),
                ErrorResponse.class
        );

        assertEquals("Ошибка валидации", error.getError());
        assertTrue(error.getDetails().contains("Название не должно быть пустым"));
    }

    @Test
    void postMovies_returnErrorIfIncorrectYear() throws Exception {


        String movieJson = "{\"title\":\"test\",\"year\":1887}";

        HttpResponse<String> lineYear = postMovies(movieJson, type);


        assertEquals(422, lineYear.statusCode());

        ErrorResponse error = gson.fromJson(
                lineYear.body(),
                ErrorResponse.class
        );

        assertEquals("Ошибка валидации", error.getError());
        assertTrue(error.getDetails().contains("Название не должно быть пустым"));
    }

    @Test
    void postMovies_returnErrorIfIncorrectContentType() throws Exception {

        String movieJson = "{\"title\":\"test\",\"year\":1999}";

        HttpResponse<String> typeTest = postMovies(movieJson, "text/plain");


        assertEquals(415, typeTest.statusCode());

        ErrorResponse error = gson.fromJson(
                typeTest.body(),
                ErrorResponse.class
        );

        assertEquals("Не верный Content-Type", error.getError());
    }

    @Test
    void postMovies_returnErrorIfIncorrectJson() throws Exception {

        String invalidJson = "{\"title\":\"test\",\"year\":1999";

        HttpResponse<String> response = postMovies(invalidJson, "application/json");


        assertEquals(400, response.statusCode());

        ErrorResponse error = gson.fromJson(
                response.body(),
                ErrorResponse.class
        );

        assertEquals("Не верный JSON", error.getError());
    }


    @Test
    void getMovies_returnFilmsExistingId() throws Exception {
            int id = 1;
            HttpResponse<String> resp = getMovieById(id);
            assertEquals("application/json; charset=UTF-8", resp.headers().firstValue("Content-Type").orElse(null));

        Movie movie = gson.fromJson(resp.body(), Movie.class);
        Movie expected = moviesServer.getMoviesStore().getMovie().get(id);

        assertEquals(200, resp.statusCode());

        assertEquals(expected.getId(), movie.getId());
        assertEquals(expected.getTitle(), movie.getTitle());
        assertEquals(expected.getYear(), movie.getYear());

    }

    @Test
    void getMovies_returnErrorIfNotExisting() throws Exception {

        int id = 15;
        HttpResponse<String> resp = getMovieById(id);

        assertEquals(404, resp.statusCode());


        ErrorResponse error = gson.fromJson(
                resp.body(),
                ErrorResponse.class
        );

        assertEquals("Фильм не найден", error.getError());
    }

    @Test
    void getMovies_returnErrorIfID_not_number() throws Exception {

        String id = "a";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(uri + "movies/" + id))
                .header("Content-Type", "application/json; charset=UTF-8")
                .GET()
                .build();

        HttpResponse<String> resp = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(400, resp.statusCode());

        ErrorResponse error = gson.fromJson(
                resp.body(),
                ErrorResponse.class
        );

        assertEquals("Некорректный ID", error.getError());
    }

    @Test
    void delete_Movies_ExistingID() throws Exception {
        int id = 1;

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(uri + "movies/" + id))
                .header("Content-Type", "application/json; charset=UTF-8")
                .DELETE()
                .build();

        HttpResponse<String> resp = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));


        assertEquals(204, resp.statusCode());
    }

    @Test
    void delete_return_Error_IfFilms_not_found() throws Exception {
        int id = 23;

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(uri + "movies/" + id))
                .header("Content-Type", "application/json; charset=UTF-8")
                .DELETE()
                .build();

        HttpResponse<String> resp = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));


        assertEquals(404, resp.statusCode());

        ErrorResponse error = gson.fromJson(
                resp.body(),
                ErrorResponse.class
        );

        assertEquals("Фильм не найден", error.getError());
    }

    @Test
    void delete_return_Error_IfId_Not_Number() throws Exception {
        String id = "a";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(uri + "movies/" + id))
                .header("Content-Type", "application/json; charset=UTF-8")
                .DELETE()
                .build();

        HttpResponse<String> resp = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));


        assertEquals(400, resp.statusCode());

        ErrorResponse error = gson.fromJson(
                resp.body(),
                ErrorResponse.class
        );

        assertEquals("Некорректный ID", error.getError());
    }

    @Test
    void getMovies_return_films_year() throws Exception {
        int year = 1999;

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(uri + "movies?year=" + year))
                .header("Content-Type", "application/json; charset=UTF-8")
                .GET()
                .build();

        HttpResponse<String> resp = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(200, resp.statusCode());

        List<Movie> movies = gson.fromJson(resp.body(), new ListOfMoviesTypeToken().getType());

        assertFalse(movies.isEmpty());

        assertTrue(movies.stream().allMatch(movie -> movie.getYear() == year));
    }

    @Test
    void getMovies_return_emptyList() throws Exception {
        int year = 1888;

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(uri + "movies?year=" + year))
                .header("Content-Type", "application/json; charset=UTF-8")
                .GET()
                .build();

        HttpResponse<String> resp = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(200, resp.statusCode());

        List<Movie> movies = gson.fromJson(resp.body(), new ListOfMoviesTypeToken().getType());

        assertTrue(movies.isEmpty());
    }

    @Test
    void getMovies_return_error_ifYear_not_number() throws Exception {
        String year = "abs";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(uri + "movies?year=" + year))
                .header("Content-Type", "application/json; charset=UTF-8")
                .GET()
                .build();

        HttpResponse<String> resp = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        ErrorResponse error = gson.fromJson(
                resp.body(),
                ErrorResponse.class
        );

        assertEquals("Некорректный запрос", error.getError());


    }
}