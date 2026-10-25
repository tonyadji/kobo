package com.lehnade.kobo.engine;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class AtomicEngineTest {

    @Test
    void transferToUnknownAccountMustNotDestroyMoney() {
        // arrange
        var engine = new AtomicEngine(10, 5000);
        long before = engine.total();

        // act & assert
        IllegalArgumentException unknowAccount = Assertions.assertThrows(IllegalArgumentException.class,
                () -> engine.transfer(0, 10, 100));
        Assertions.assertEquals(before, engine.total());
        Assertions.assertEquals("Unknown account: 10", unknowAccount.getMessage());
    }
}
