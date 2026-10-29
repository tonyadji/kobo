package com.lehnade.kobo;

import io.javalin.Javalin;
import org.jooq.DSLContext;

import static io.javalin.apibuilder.ApiBuilder.post;

public class KoboApi {

    private static Javalin API;

    public static Javalin create(DSLContext dsl) {
        TransferController transferController = new TransferController(new TransferService(dsl));
        API = Javalin.create(
                config ->
                    config.router.apiBuilder(() ->
                        post("transfers",transferController::transfer)
                )
        );
        return API;
    }
}
