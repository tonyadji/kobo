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
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

public class TransferControllerTest {

    private static final Table<?> ACCOUNTS = DSL.table("accounts");
    private static final Table<?> TRANSFERS = DSL.table("transfers");
    private static final Table<?> IDEMPOTENCY_KEYS = DSL.table("transfers_idempotency_keys");
    private static final Field<Long> ID = DSL.field("id", Long.class);
    private static final Field<Long> BALANCE_MINOR = DSL.field("balance_minor", Long.class);
    private static final Field<String> KEY = DSL.field("key", String.class);
    private static final Field<String> RESPONSE_BODY = DSL.field("response_body", String.class);
    private static final Field<String> REQUEST_HASH = DSL.field("request_hash", String.class);
    private static final Field<String> STATUS = DSL.field("status", String.class);
    private static final Field<Integer> HTTP_STATUS = DSL.field("http_status", Integer.class);
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
        var key = UUID.randomUUID().toString();

        // act
        HttpResponse<String> response = post("/transfers", transferRequestDto, key);
        System.out.println(response.body());
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
        var key = UUID.randomUUID().toString();

        // act
        HttpResponse<String> response = post("/transfers", transferRequestDto, key);
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
        var key = UUID.randomUUID().toString();

        // act
        HttpResponse<String> response = post("/transfers", transferRequestDto, key);
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
        var key = UUID.randomUUID().toString();

        // act
        HttpResponse<String> response = post("/transfers", "{", key);
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
        var key = UUID.randomUUID().toString();

        // act
        HttpResponse<String> response = post("/transfers", "{\"to\": 3, \"amount\": 100}", key);
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
        var key = UUID.randomUUID().toString();

        // act
        HttpResponse<String> response = post("/transfers", "{\"from\": 2, \"to\": 3, \"amount\": 100.41}", key);
        long fromBalanceAfter = getAccountBalance(2L);
        long toBalanceAfter = getAccountBalance(3L);

        // assert
        Assertions.assertEquals(400, response.statusCode());
        Assertions.assertEquals(fromBalanceBefore, fromBalanceAfter);
        Assertions.assertEquals(toBalanceBefore, toBalanceAfter);
    }

    @Test
    void unknownFromAccountShouldReturn404() throws Exception {
        // arrange
        var transferRequestDto = requestDto(2L, 3L, 100L);
        var key = UUID.randomUUID().toString();

        // act
        HttpResponse<String> response = post("/transfers", transferRequestDto, key);

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
        var key = UUID.randomUUID().toString();

        // act
        HttpResponse<String> response = post("/transfers", transferRequestDto, key);
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
        var key = UUID.randomUUID().toString();

        // act
        HttpResponse<String> response = post("/transfers", transferRequestDto, key);

        // assert
        Assertions.assertEquals(422, response.statusCode());
        Assertions.assertTrue(response.body().contains("INSUFFICIENT_FUNDS"));
    }


    @Test
    void theSameKeyWithTheSameBodyReturnsTheStoredResponseAndDebitIsAppliedOnce() throws Exception {
        long from = 2L;
        long to = 3L;
        long amount = 300;
        insertAccountsWithBalance(from, 1000L);
        insertAccountsWithBalance(to, 0L);
        var  transferRequestDto = requestDto(from, to, amount);
        String key = UUID.randomUUID().toString();

        // act
        var initialResult = post("/transfers", transferRequestDto, key);
        var retryResult = post("/transfers", transferRequestDto, key);
        var sender = objectMapper.readValue(getAccount(String.valueOf(from)).body(), Account.class);
        var receiver = objectMapper.readValue(getAccount(String.valueOf(to)).body(), Account.class);


        // assert
        Assertions.assertEquals(700, sender.balance());
        Assertions.assertEquals(300, receiver.balance());
        Assertions.assertEquals(retryResult.body(), initialResult.body());
    }

    @Test
    void transferWithoutIdempotencyKeyReturns400() throws Exception {
        long from = 2L;
        long to = 3L;
        long amount = 300;
        insertAccountsWithBalance(from, 1000L);
        insertAccountsWithBalance(to, 0L);
        var  transferRequestDto = requestDto(from, to, amount);

        // act
        var response = post("/transfers", transferRequestDto);
        Assertions.assertEquals(400, response.statusCode());
    }

    @Test
    void transferToUnknownAccountReplayedWithTheSameKeyShouldReturn404AndSamePayload() throws Exception {
        // arrange
        long from = 2L;
        long to = 3L;
        long amount = 100;
        insertAccountsWithBalance(2L, 200L);
        var  transferRequestDto = requestDto(from, to, amount);
        String key = UUID.randomUUID().toString();

        // act
        var response = post("/transfers", transferRequestDto, key);
        var firstCallKey = getByKey(key);
        var retryResponse = post("/transfers", transferRequestDto, key);
        var retryKey = getByKey(key);

        // assertions
        Assertions.assertEquals(404, response.statusCode());
        Assertions.assertEquals(404, retryResponse.statusCode());
        Assertions.assertEquals(retryKey.responseBody(), firstCallKey.responseBody());
        Assertions.assertEquals(retryKey.status(), firstCallKey.status());
    }

    @Test
    void theSameKeyWithDifferentBodyShouldReturn422AndNothingIsDebited() throws Exception {
        // arrange
        long from = 2L;
        long to = 3L;
        long amount = 400;
        insertAccountsWithBalance(2L, 1000L);
        insertAccountsWithBalance(3L, 200L);
        var  initialRequest = requestDto(from, to, amount);
        var  retryRequest = requestDto(from, to, 500);
        String key = UUID.randomUUID().toString();

        // act
        var response = post("/transfers", initialRequest, key);
        var firstCallKey = getByKey(key);
        var retryResponse = post("/transfers", retryRequest, key);
        var retryKey = getByKey(key);
        var balanceAfter = getAccountBalance(from);
        // assertions
        Assertions.assertEquals(201, response.statusCode());
        Assertions.assertEquals(422, retryResponse.statusCode());
        Assertions.assertEquals(600, balanceAfter);
    }

    @Test
    void rejectedTransferInsufficientReplayedWithTheSameKeyReturnsTheSameRejection() throws Exception {
        // arrange
        long from = 2L;
        long to = 3L;
        long amount = 400;
        insertAccountsWithBalance(2L, 200L);
        insertAccountsWithBalance(3L, 200L);
        var  initialRequest = requestDto(from, to, amount);
        String key = UUID.randomUUID().toString();

        // act
        var response = post("/transfers", initialRequest, key);
        var firstCallKey = getByKey(key);
        updateBalance(from, 1000);
        var retryResponse = post("/transfers", initialRequest, key);
        var retryKey = getByKey(key);

        // assertions
        Assertions.assertEquals(422, response.statusCode());
        Assertions.assertEquals(422, retryResponse.statusCode());
        Assertions.assertEquals(retryResponse.body(), response.body());
        Assertions.assertEquals(retryKey.responseBody(), firstCallKey.responseBody());
        Assertions.assertEquals(retryKey.status(), firstCallKey.status());
    }

    @Disabled("one shared JDBC connection: 201 for a rolled-back transfer, fixed by a DataSource")
    @RepeatedTest(5)
    void concurrentTransferShouldDebitOnce() throws InterruptedException {
        // arrange
        long from = 2L;
        long to = 3L;
        long amount = 60;
        insertAccountsWithBalance(2L, 200L);
        insertAccountsWithBalance(3L, 200L);
        var  initialRequest = requestDto(from, to, amount);
        String key = UUID.randomUUID().toString();
        var numberOfThreads = 10;
        var threads = new Thread[numberOfThreads];
        var latch = new CountDownLatch(1);
        Map<Integer, AtomicInteger> responses = new ConcurrentHashMap<>();
        Set<String> createdBodies = ConcurrentHashMap.newKeySet();
        var errors = new CopyOnWriteArrayList<Exception>();

        for (int i = 0; i < numberOfThreads; i++) {
            threads[i] = new Thread(() -> {
                try {
                    latch.await();
                    var response = post("/transfers", initialRequest, key);
                    responses.computeIfAbsent(response.statusCode(), k -> new AtomicInteger(0))
                            .incrementAndGet();
                    if (response.statusCode() == 201) createdBodies.add(response.body());
                } catch (Exception e) {
                    errors.add(e);
                }
            });
        }

        for (Thread thread : threads) thread.start();
        latch.countDown();

        for (Thread thread : threads)  thread.join();


        long fromBalanceAfter = getAccountBalance(from);
        long transferCount = countTransfers();
        Map<Integer, Integer> statuses = new TreeMap<>();
        responses.forEach((status, count) -> statuses.put(status, count.get()));

        // assert
        Assertions.assertAll(
                () -> Assertions.assertTrue(errors.isEmpty(), "client errors: " + errors),
                () -> Assertions.assertEquals(Map.of(201, numberOfThreads), statuses, "status codes"),
                () -> Assertions.assertEquals(1, createdBodies.size(), "distinct 201 bodies: " + createdBodies),
                () -> Assertions.assertEquals(1, transferCount, "transfers in DB"),
                () -> Assertions.assertEquals(140, fromBalanceAfter, "sender balance"));
    }

    private long countTransfers() {
        return dsl.selectCount().from(TRANSFERS).fetchOne(0, Long.class);
    }

    private IdempotencyKey getByKey(String key) {
        return dsl.select(KEY, REQUEST_HASH, RESPONSE_BODY, STATUS, HTTP_STATUS).from(IDEMPOTENCY_KEYS)
                .where(KEY.eq(key))
                .fetchOneInto(IdempotencyKey.class);
    }

    private void insertAccountsWithBalance(long account, long balance) {
        dsl.insertInto(ACCOUNTS, ID, BALANCE_MINOR)
                .values(account, balance)
                .execute();
    }

    private void updateBalance(long account, long balance) {
        dsl.update(ACCOUNTS)
                .set(BALANCE_MINOR, balance)
                .where(ID.eq(account))
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

    private HttpResponse<String> post(String path, String body, String idempotencyKey) throws Exception {
        try(HttpClient client = HttpClient.newHttpClient()) {
            String uri = new StringBuilder().append("http://localhost:").append(port)
                    .append(path).toString();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(uri))
                    .header("Idempotency-Key", idempotencyKey)
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            return response;
        }
    }

    private String requestDto(long from, long to, long amount) {
        return String.format("{\"from\":%d,\"to\":%d,\"amount\":%d}", from, to, amount);
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
