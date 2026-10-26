package com.lehnade.kobo.katas.lru;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class LRUCacheTest {

    @Test
    void evictLeastRecentlyUsedWhenCapacityIsFull() {
        // arrange
        MyLRUCache cache = new MyLRUCache(2);
        cache.put(1, 100);
        cache.put(2, 22);

        // act
        cache.put(3, 7);
        int oneIsEvicted = cache.get(1);

        // assert
        Assertions.assertEquals(-1, oneIsEvicted);
    }

    @Test
    void getRefreshesTheKey() {
        // arrange
        MyLRUCache cache = new MyLRUCache(2);
        cache.put(1, 9);
        cache.put(2, 10);

        // act
        cache.get(1);
        cache.put(3, 63);
        int twoIsEvicted = cache.get(2);

        // assertions
        Assertions.assertEquals(-1, twoIsEvicted);

    }

    @Test
    void putOnExistingKeyUpdatesAndRefreshes() {
        // arrange
        MyLRUCache cache = new MyLRUCache(2);
        cache.put(1, 91);
        cache.put(2, 32);

        // act
        cache.put(1, 34);
        cache.put(3, 45);
        int twoIsEvicted = cache.get(2);
        int valueOfKeyOne = cache.get(1);

        // assertions
        Assertions.assertEquals(-1, twoIsEvicted);
        Assertions.assertEquals(34, valueOfKeyOne);
    }

    @Test
    void capacityOne() {
        // arrange
        MyLRUCache cache = new MyLRUCache(1);
        cache.put(1, 8);
        cache.put(2, 28);

        // act
        int oneIsEvicted = cache.get(1);

        // assertions
        Assertions.assertEquals(-1, oneIsEvicted);
    }
}
