package com.lehnade.kobo;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Table;
import org.jooq.impl.DSL;

import java.util.List;
import java.util.Map;

public class TransferService {

    private static final Table<?> ACCOUNTS = DSL.table("accounts");
    private static final Table<?> TRANSFERS = DSL.table("transfers");
    private static final Table<?> IDEMPOTENCY_KEYS = DSL.table("transfers_idempotency_keys");
    private static final Field<Long> ID = DSL.field("id", Long.class);
    private static final Field<Long> BALANCE_MINOR = DSL.field("balance_minor", Long.class);
    private static final Field<Long> AMOUNT_MINOR = DSL.field("amount_minor", Long.class);
    private static final Field<Long> FROM_ACCOUNT = DSL.field("from_account_id", Long.class);
    private static final Field<Long> TO_ACCOUNT = DSL.field("to_account_id", Long.class);
    private static final Field<String> KEY = DSL.field("key", String.class);
    private static final Field<String> REQUEST_HASH = DSL.field("request_hash", String.class);
    private static final Field<String> RESPONSE_BODY = DSL.field("response_body", String.class);
    private static final Field<String> STATUS = DSL.field("status", String.class);
    private static final Field<Integer> HTTP_STATUS = DSL.field("http_status", Integer.class);
    private static final ObjectMapper mapper = new ObjectMapper();
    private final DSLContext ctx;

    public TransferService(DSLContext ctx) {
        this.ctx = ctx;
    }

    public long transfer(long from, long to, long amount) {
        if (from == to) { throw new InvalidTransferException("Self transfer is not allowed") ; }
        if (amount <= 0) { throw new InvalidTransferException("Amount should be greater than 0") ; }

        long transferId = ctx.transactionResult(configuration -> {
            var txCtx = DSL.using(configuration);
            if (from < to) {
                debit(from, amount, txCtx);
                credit(to, amount, txCtx);
            } else {
                credit(to, amount, txCtx);
                debit(from, amount, txCtx);
            }

            return transfer(from, to, amount, txCtx);
        });
        return transferId;
    }

    public TransferResponseDto.StoredResponse transfer(long from, long to, long amount, IdempotencyKey key) {
        if (from == to) {
            throw new InvalidTransferException("Self transfer is not allowed");
        }
        if (amount <= 0) {
            throw new InvalidTransferException("Amount should be greater than 0");
        }
        TransferResponseDto.StoredResponse transactionResponse = ctx.transactionResult(configuration -> {
            var outerTx = DSL.using(configuration);
            int insertedRows = insertIdempotencyKey(key, outerTx);
            if (insertedRows == 0) {
                return handleExistingIdempotencyKey(key, outerTx);
            }
            try {
                return outerTx.transactionResult(innerConfig -> {
                    var innerTx = DSL.using(innerConfig);
                    if (from < to) {
                        debit(from, amount, innerTx);
                        credit(to, amount, innerTx);
                    } else {
                        credit(to, amount, innerTx);
                        debit(from, amount, innerTx);
                    }
                    long transferIdentifier = transfer(from, to, amount, innerTx);
                    updateIdempotencyKey(key.key(), payloadOf(transferIdentifier), "SUCCESS", 201, innerTx);
                    var body = mapper.writeValueAsString(new TransferResponseDto(transferIdentifier));
                    return new TransferResponseDto.StoredResponse(201, body);
                });
            } catch (UnknownAccountException e) {
                updateIdempotencyKey(key.key(), payloadOf(e), "FAILED", 404, outerTx);
                return new TransferResponseDto.StoredResponse(404, payloadOf(e));
            } catch (InsufficientFundException e) {
                updateIdempotencyKey(key.key(), payloadOf(e), "FAILED", 422, outerTx);
                return new TransferResponseDto.StoredResponse(422, payloadOf(e));
            }
        });
        return transactionResponse;
    }

    private long updateIdempotencyKey(String key, String payload, String status, int httpStatus, DSLContext ctx) {
        return ctx.update(IDEMPOTENCY_KEYS)
                .set(RESPONSE_BODY, payload)
                .set(STATUS, status)
                .set(HTTP_STATUS, httpStatus)
                .where(KEY.eq(key))
                .execute();
    }

    private String payloadOf(long from, long to, long amount) throws JsonProcessingException {
        Map<String, Object> payload = Map.of("from", from, "to", to, "amount", amount);
        return new ObjectMapper().writeValueAsString(payload);
    }

    private String payloadOf(long id) throws JsonProcessingException {
        Map<String, Object> payload = Map.of("id", id);
        return mapper.writeValueAsString(payload);
    }

    private String payloadOf(KoboException e) throws JsonProcessingException {
        Map<String, Object> payload = Map.of("error", e.getError());
        return new ObjectMapper().writeValueAsString(payload);
    }

    private void requireAccountExists(long account, DSLContext ctx) {
        boolean accountExists = ctx.fetchExists(
                ctx.selectOne().from(ACCOUNTS).where(ID.eq(account)));

        if (!accountExists) throw new UnknownAccountException("Unknown account " + account);
    }

    private void debit(long from, long amount, DSLContext ctx) {
        int updatedRow = ctx.update(ACCOUNTS)
                .set(BALANCE_MINOR, BALANCE_MINOR.minus(amount))
                .where(ID.eq(from))
                .and(BALANCE_MINOR.ge(amount))
                .execute();
        if (updatedRow == 0) {
            requireAccountExists(from, ctx);
            throw new InsufficientFundException("Unsufficient balance");
        }
    }

    private void credit(long to, long amount, DSLContext ctx) {
        int updatedRow = ctx.update(ACCOUNTS)
                .set(BALANCE_MINOR, BALANCE_MINOR.plus(amount))
                .where(ID.eq(to))
                .execute();
        if (updatedRow == 0) {
            throw new UnknownAccountException("Unknown account " + to);
        }
    }

    private long transfer(long from, long to, long amount, DSLContext ctx) {
        return ctx.insertInto(TRANSFERS, FROM_ACCOUNT, TO_ACCOUNT, AMOUNT_MINOR)
                .values(from, to, amount)
                .returning(ID)
                .fetchOne(ID);
    }

    private int insertIdempotencyKey(IdempotencyKey key, DSLContext ctx) {
        return ctx.insertInto(IDEMPOTENCY_KEYS, KEY, REQUEST_HASH, STATUS)
                .values(key.key(), key.requestHash(), key.status())
                .onConflict(List.of(KEY))
                .doNothing()
                .execute();
    }

    private IdempotencyKey getKey(String key, DSLContext ctx) {
        return ctx.select(KEY, REQUEST_HASH, RESPONSE_BODY, STATUS, HTTP_STATUS).from(IDEMPOTENCY_KEYS)
                .where(KEY.eq(key))
                .fetchOneInto(IdempotencyKey.class);
    }

    private TransferResponseDto.StoredResponse handleExistingIdempotencyKey(IdempotencyKey key, DSLContext ctx) throws JsonProcessingException {
        IdempotencyKey existingKey = getKey(key.key(), ctx);
        if (existingKey.status().equals("PROCESSING")) {
            return new TransferResponseDto.StoredResponse(409, existingKey.responseBody());
        }
        if (!existingKey.requestHash().equals(key.requestHash())) {
            return new TransferResponseDto.StoredResponse(422, payloadOf(new IdempotencyKeyException("")));
        }
        return new TransferResponseDto.StoredResponse(existingKey.httpStatus(), existingKey.responseBody());
    }
}
