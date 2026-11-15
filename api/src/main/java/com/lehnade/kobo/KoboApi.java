package com.lehnade.kobo;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lehnade.kobo.application.IdempotentTransfer;
import com.lehnade.kobo.application.Transactions;
import com.lehnade.kobo.application.TransferMoney;
import com.lehnade.kobo.domain.DomainException;
import com.lehnade.kobo.infrastructure.JooqTransactions;
import io.javalin.Javalin;
import io.javalin.json.JavalinJackson;
import org.jooq.DSLContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static io.javalin.apibuilder.ApiBuilder.get;
import static io.javalin.apibuilder.ApiBuilder.post;

public class KoboApi {

    private static final Logger log = LoggerFactory.getLogger(KoboApi.class);

    public static Javalin create(DSLContext dsl) {

        Transactions transactions = new JooqTransactions(dsl);
        TransferMoney transferMoney = new TransferMoney();
        IdempotentTransfer idempotentTransfer = new IdempotentTransfer(transferMoney, transactions);
        TransferController transferController = new TransferController(idempotentTransfer);
        AccountController accountController = new AccountController(new AccountService(dsl));

        ObjectMapper mapper = new ObjectMapper();
        mapper.enable(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES);
        mapper.disable(DeserializationFeature.ACCEPT_FLOAT_AS_INT);
        Javalin app = Javalin.create(
                config -> {
                    config.jsonMapper(new JavalinJackson(mapper, false));
                    config.router.apiBuilder(
                            () -> {
                                post("/transfers", transferController::transfer);
                                get("/accounts/{id}", accountController::get);
                            }
                    );
                }

        );
        app.exception(DomainException.class, (e, ctx) -> ctx.status(400).json(APIError.BAD_REQUEST));
        app.exception(InvalidTransferException.class,
                (e, ctx) -> ctx.status(400).json(APIError.BAD_REQUEST));
        app.exception(UnknownAccountException.class,
                (e, ctx) -> ctx.status(404).json(APIError.UNKNOWN_ACCOUNT));
        app.exception(InsufficientFundException.class,
                (e, ctx) -> ctx.status(422).json(APIError.INSUFFICIENT_FUNDS));
        app.exception(RuntimeException.class,
                (e, ctx) -> {
                    log.error(e.getMessage(), e);
                    ctx.status(500).json(APIError.INTERNAL_SERVER_ERROR);
                });

        return app;
    }

}
