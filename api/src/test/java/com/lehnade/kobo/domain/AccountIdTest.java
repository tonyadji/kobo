package com.lehnade.kobo.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AccountIdTest {
    @Test
    void accountIdWithNegativeShouldThrowException() {
        // arrange act
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> new AccountId(-1));

        // assert
        assertEquals("Invalid id", exception.getMessage());
    }

    @Test
    void accountIdWith0ShouldThrowException() {
        // arrange act
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> new AccountId(0));

        // assert
        assertEquals("Invalid id", exception.getMessage());
    }

    @Test
    void validIdShouldCreateInstanceAndReturnCorrectValue() {
        // arrange act
        AccountId id = new AccountId(10);

        // assert
        assertNotNull(id);
        assertEquals(10, id.value());
    }
}