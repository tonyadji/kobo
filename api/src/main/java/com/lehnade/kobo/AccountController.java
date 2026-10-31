package com.lehnade.kobo;

import io.javalin.http.Context;

public class AccountController {

    private final AccountService accountService;

    public  AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    public void get(Context ctx) {
        long id = ctx.pathParamAsClass("id", Long.class).get();
        var account = accountService.getAccount(id);
        ctx.status(200).json(account);
    }
}
