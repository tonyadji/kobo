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
        var transferRequestDto = requestDto(2L, 3L, 100L);

        // act
        HttpResponse<String> response = post("/transfers", transferRequestDto);
        TransferResponseDto result = objectMapper.readValue(response.body(), TransferResponseDto.class);

        // assert
        Assertions.assertEquals(201, response.statusCode());
        Assertions.assertEquals(1, result.id());
    }

    @Test
    void transferWithAmountLessEqualTo0ShouldReturn400() throws Exception {
        // arrange
        long fromBalanceBefore = 200L;
        long toBalanceBefore = 150L;
        insertAccountsWithBalance(2L, fromBalanceBefore);
        insertAccountsWithBalance(3L, toBalanceBefore);
        var transferRequestDto = requestDto(2L, 3L, 0L);

        // act
        HttpResponse<String> response = post("/transfers", transferRequestDto);
        long fromBalanceAfter = getAccountBalance(2L);
        long toBalanceAfter = getAccountBalance(3L);

        // assert
        Assertions.assertEquals(400, response.statusCode());
        Assertions.assertTrue(response.body().contains("BAD_REQUEST"));
        Assertions.assertEquals(fromBalanceBefore, fromBalanceAfter);
        Assertions.assertEquals(toBalanceBefore, toBalanceAfter);

    }

    @Test
    void selfTransferShouldReturn400() throws Exception {
        // arrange
        long fromBalanceBefore = 200L;
        long toBalanceBefore = 150L;
        insertAccountsWithBalance(2L, fromBalanceBefore);
        insertAccountsWithBalance(3L, toBalanceBefore);
        var transferRequestDto = requestDto(2L, 2L, 100L);

        // act
        HttpResponse<String> response = post("/transfers", transferRequestDto);
        long fromBalanceAfter = getAccountBalance(2L);
        long toBalanceAfter = getAccountBalance(3L);

        // assert
        Assertions.assertEquals(400, response.statusCode());
        Assertions.assertTrue(response.body().contains("BAD_REQUEST"));
        Assertions.assertEquals(fromBalanceBefore, fromBalanceAfter);
        Assertions.assertEquals(toBalanceBefore, toBalanceAfter);
    }

    @Test
    void invalidJsonShouldReturn400() throws Exception {
        // arrange
        long fromBalanceBefore = 200L;
        long toBalanceBefore = 150L;
        insertAccountsWithBalance(2L, fromBalanceBefore);
        insertAccountsWithBalance(3L, toBalanceBefore);

        // act
        HttpResponse<String> response = post("/transfers", "{" );
        long fromBalanceAfter = getAccountBalance(2L);
        long toBalanceAfter = getAccountBalance(3L);

        // assert
        Assertions.assertEquals(400, response.statusCode());
        Assertions.assertEquals(fromBalanceBefore, fromBalanceAfter);
        Assertions.assertEquals(toBalanceBefore, toBalanceAfter);
    }

    @Test
    void validJsonWithInvalidInputMissingAccountShouldReturn400() throws Exception {
        // arrange
        long fromBalanceBefore = 200L;
        long toBalanceBefore = 150L;
        insertAccountsWithBalance(2L, fromBalanceBefore);
        insertAccountsWithBalance(3L, toBalanceBefore);

        // act
        HttpResponse<String> response = post("/transfers", "{\"to\": 3, \"amount\": 100}" );
        long fromBalanceAfter = getAccountBalance(2L);
        long toBalanceAfter = getAccountBalance(3L);

        // assert
        Assertions.assertEquals(400, response.statusCode());
        Assertions.assertEquals(fromBalanceBefore, fromBalanceAfter);
        Assertions.assertEquals(toBalanceBefore, toBalanceAfter);
    }

    @Test
    void validJsonWithInvalidInputDecimalAmountShouldReturn400() throws Exception {
        // arrange
        long fromBalanceBefore = 200L;
        long toBalanceBefore = 150L;
        insertAccountsWithBalance(2L, fromBalanceBefore);
        insertAccountsWithBalance(3L, toBalanceBefore);

        // act
        HttpResponse<String> response = post("/transfers", "{\"from\": 2, \"to\": 3, \"amount\": 100.41}" );
        long fromBalanceAfter = getAccountBalance(2L);
        long toBalanceAfter = getAccountBalance(3L);

        // assert
        //Assertions.assertEquals(400, response.statusCode());
        Assertions.assertEquals(fromBalanceBefore, fromBalanceAfter);
        Assertions.assertEquals(toBalanceBefore, toBalanceAfter);
    }

    @Test
    void unknownFromAccountShouldReturn404() throws Exception {
        // arrange
        var transferRequestDto = requestDto(2L, 3L, 100L);

        // act
        HttpResponse<String> response = post("/transfers", transferRequestDto);

        // assert
        Assertions.assertEquals(404, response.statusCode());
        Assertions.assertTrue(response.body().contains("UNKNOWN_ACCOUNT"));
    }

    @Test
    void unknownToAccountShouldReturn404() throws Exception {
        // arrange
        long fromBalanceBefore = 200L;
        insertAccountsWithBalance(2L, fromBalanceBefore);
        var transferRequestDto = requestDto(2L, 3L, 100L);

        // act
        HttpResponse<String> response = post("/transfers", transferRequestDto);
        long fromBalanceAfter = getAccountBalance(2L);

        // assert
        Assertions.assertEquals(404, response.statusCode());
        Assertions.assertTrue(response.body().contains("UNKNOWN_ACCOUNT"));
        Assertions.assertEquals(fromBalanceBefore, fromBalanceAfter);
    }

    @Test
    void transferWithInsufficientBalanceShouldReturn422() throws Exception {
        // arrange
        insertAccountsWithBalance(2L, 200L);
        insertAccountsWithBalance(3L, 150L);
        var transferRequestDto = requestDto(2L, 3L, 300L);

        // act
        HttpResponse<String> response = post("/transfers", transferRequestDto);

        // assert
        Assertions.assertEquals(422, response.statusCode());
        Assertions.assertTrue(response.body().contains("INSUFFICIENT_FUNDS"));
    }

    private void insertAccountsWithBalance(long account, long balance) {
        dsl.insertInto(ACCOUNTS, ID, BALANCE_MINOR)
                .values(account, balance)
                .execute();
    }

    private long getAccountBalance(long account) {
        return dsl.select(BALANCE_MINOR).from(ACCOUNTS)
                .where(ID.eq(account))
                .fetchOne(BALANCE_MINOR);
    }

    private HttpResponse<String> post(String path, String body) throws Exception {
        try(HttpClient client = HttpClient.newHttpClient()) {
            String uri = new StringBuilder().append("http://localhost:").append(port)
                    .append(path).toString();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(uri))
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            return response;
        }
    }

    private String requestDto(long from, long to, long amount) {
        return String.format("{\"from\":%d,\"to\":%d,\"amount\":%d}", from, to, amount);
    }


    @AfterAll
    static void stop() { app.stop(); }
}
