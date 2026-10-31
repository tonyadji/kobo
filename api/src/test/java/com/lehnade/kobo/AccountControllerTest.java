package com.lehnade.kobo;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.javalin.Javalin;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Table;
import org.jooq.impl.DSL;
import org.junit.jupiter.api.*;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class AccountControllerTest {

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
    }

    @BeforeEach
    void cleanDatabase() {
        Postgres.reset(dsl);
    }

    @Test
    void getAccountWithExistingIdShouldReturnAccount() throws Exception {
        // arrange
        long id = 2L;
        long balanceMinor = 200L;
        insertAccountsWithBalance(id, balanceMinor);

        // act
        var result = getAccount(String.valueOf(id));

        // assert
        Assertions.assertEquals(200, result.statusCode());
        Account account = objectMapper.readValue(result.body(), Account.class);
        Assertions.assertEquals(id, account.id());
        Assertions.assertEquals(balanceMinor, account.balance());
    }

    @Test
    void getAccountWithNonExistingIdShouldReturn404() throws Exception {
        // arrange
        long id = 2L;

        // act
        var result = getAccount(String.valueOf(id));

        // assert
        Assertions.assertEquals(404, result.statusCode());
    }

    @Test
    void getAccountWithInvalidIdShouldReturn400() throws Exception {
        // arrange
        String id = "invalid_id";

        // act
        var result = getAccount(id);

        // assert
        Assertions.assertEquals(400, result.statusCode());
    }

    private void insertAccountsWithBalance(long account, long balance) {
        dsl.insertInto(ACCOUNTS, ID, BALANCE_MINOR)
                .values(account, balance)
                .execute();
    }

    private HttpResponse<String> getAccount(String id) throws Exception {
        try(HttpClient client = HttpClient.newHttpClient()) {
            String uri = new StringBuilder().append("http://localhost:").append(port)
                    .append("/accounts/").append(id).toString();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(uri))
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            return response;
        }
    }

    @AfterAll
    static void stop() { app.stop(); }
}
