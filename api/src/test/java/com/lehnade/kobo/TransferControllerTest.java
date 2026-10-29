package com.lehnade.kobo;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import io.javalin.Javalin;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Table;
import org.jooq.impl.DSL;
import org.junit.jupiter.api.*;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class TransferControllerTest {

    private static final Table<?> ACCOUNTS = DSL.table("accounts");
    private static final Field<Long> ID = DSL.field("id", Long.class);
    private static final Field<Long> BALANCE_MINOR = DSL.field("balance_minor", Long.class);
    private static DSLContext dsl;
    private static int port;
    private static Javalin app;
    private static ObjectMapper objectMapper;

    @BeforeAll
    static void init() {
        dsl = DSL.using(Postgres.CONTAINER.getJdbcUrl(),
                Postgres.CONTAINER.getUsername(),
                Postgres.CONTAINER.getPassword());
        app = KoboApi.create(dsl).start(0);
        port = app.port();
        objectMapper = new ObjectMapper();
        objectMapper.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);
    }


    @BeforeEach
    void cleanDatabase() {
        Postgres.reset(dsl);
    }

    @Test
    void successfulTransferShouldReturn201AndTheTransferId() throws Exception {
        // arrange
        insertAccountsWithBalance(2L, 200L);
        insertAccountsWithBalance(3L, 150L);
        var transferRequestDto = new TransferRequestDto(2L, 3L, 100L);

        // act
        HttpResponse<String> response = post("/transfers", transferRequestDto);
        TransferResponseDto result = objectMapper.readValue(response.body(), TransferResponseDto.class);

        // assert
        Assertions.assertEquals(201, response.statusCode());
        Assertions.assertEquals(1, result.id());
    }

    private void insertAccountsWithBalance(long from, long balance) {
        dsl.insertInto(ACCOUNTS, ID, BALANCE_MINOR)
                .values(from, balance)
                .execute();
    }

    private HttpResponse<String> post(String path, TransferRequestDto body) throws Exception {
        try(HttpClient client = HttpClient.newHttpClient()) {
            String uri = new StringBuilder().append("http://localhost:").append(port)
                    .append(path).toString();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(uri))
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            return response;
        }
    }

    @AfterAll
    static void stop() { app.stop(); }
}
