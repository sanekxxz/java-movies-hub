package ru.practicum.moviehub.http;

import com.google.gson.Gson;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

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

    private final static MoviesServer moviesServer = new MoviesServer(new MoviesStore(), 8080);
    private static HttpClient httpClient;
    private final static String uri = "http://localhost:8080/";
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

        assertEquals(2, movies.size());

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

        String movieJson = """
                    {
                      "title": "Mus",
                      "year": 2006
                    }
                    """;

        HttpResponse<String> correctData = postMovies(movieJson, type);


        assertEquals(201, correctData.statusCode());
    }

    @Test
    void postMovies_adds_filmsDataEmptyTitle() throws Exception {

        String response = """
                    {
                      "error": "Ошибка валидации",
                      "details": [
                        "Название не должно быть пустым",
                        "Год должен быть между 1888 и 2026"
                      ]
                    }
                """;

        String movieJson = """
                    {
                      "title": "",
                      "year": 2006
                    }
                    """;

        HttpResponse<String> isEmptyTitle = postMovies(movieJson, type);

        assertEquals(422, isEmptyTitle.statusCode());

        assertEquals(response, isEmptyTitle.body());
    }

    @Test
    void postMovies_returnErrorLengthMore100() throws Exception {

        String response = """
                    {
                      "error": "Ошибка валидации",
                      "details": [
                        "Название не должно быть пустым",
                        "Год должен быть между 1888 и 2026"
                      ]
                    }
                """;

        String movieJson = """
                    {
                      "title": "asddaasdasdasdasssssssssssssssssssssssswaqwqweqgfdgdfgdfgaewfdsvfswesdasfsdvewrvfdvvevrerggegsfsafsfsa",
                      "year": 2006
                    }
                    """;
        HttpResponse<String> lineTitle = postMovies(movieJson, type);


        assertEquals(422, lineTitle.statusCode());

        assertEquals(response, lineTitle.body());
    }

    @Test
    void postMovies_returnErrorIfIncorrectYear() throws Exception {

        String response = """
                    {
                      "error": "Ошибка валидации",
                      "details": [
                        "Название не должно быть пустым",
                        "Год должен быть между 1888 и 2026"
                      ]
                    }
                """;

        String movieJson = """
                    {
                      "title": "test",
                      "year": 1887
                    }
                    """;

        HttpResponse<String> lineYear = postMovies(movieJson, type);


        assertEquals(422, lineYear.statusCode());

        assertEquals(response, lineYear.body());
    }

    @Test
    void postMovies_returnErrorIfIncorrectContentType() throws Exception {
        String movieJson = """
                    {
                      "title": "test",
                      "year": 1999
                    }
                    """;

        HttpResponse<String> typeTest = postMovies(movieJson, "text/plain");


        assertEquals(415, typeTest.statusCode());

        assertEquals("{\"message\":\"Не верный Content-Type\"}", typeTest.body());
    }

    @Test
    void postMovies_returnErrorIfIncorrectJson() throws Exception {
        String invalidJson = """
            {
              "title": "Mus",
              "year": 2006
            """;

        HttpResponse<String> response = postMovies(invalidJson, "application/json");


        assertEquals(400, response.statusCode());

        assertEquals("{\"message\":\"Не верный JSON\"}", response.body());
    }


    @Test
    void getMovies_returnFilmsExistingId() throws Exception {
            int id = 1;
            HttpResponse<String> resp = getMovieById(id);
            assertEquals("application/json; charset=UTF-8", resp.headers().firstValue("Content-Type").orElse(null));

            Movie movies = gson.fromJson(resp.body(), Movie.class);

            assertTrue(movies.equals(moviesServer.getMoviesStore().getMovie().get(id)));

    }

    @Test
    void getMovies_returnErrorIfNotExisting() throws Exception {

        int id = 3;
        HttpResponse<String> resp = getMovieById(id);

        assertEquals(404, resp.statusCode());

        assertEquals("{\"message\":\"Фильм не найден\"}", resp.body());
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

        assertEquals("{\"message\":\"Некорректный ID\"}", resp.body());
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
        int id = 5;

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(uri + "movies/" + id))
                .header("Content-Type", "application/json; charset=UTF-8")
                .DELETE()
                .build();

        HttpResponse<String> resp = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));


        assertEquals(404, resp.statusCode());

        assertEquals("{\"message\":\"Фильм не найден\"}", resp.body());
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

        assertEquals("{\"message\":\"Некорректный ID\"}", resp.body());
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

        assertEquals("{\"message\":\"Некорректный запроса\"}", resp.body());
    }
}