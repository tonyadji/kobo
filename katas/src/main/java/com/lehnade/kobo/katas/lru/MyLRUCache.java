package com.lehnade.kobo.katas.lru;

import java.util.HashMap;

public class MyLRUCache {
    private DoublyLinkedList cache;
    private HashMap<Integer, Node> map;
    private int capacity;

    public MyLRUCache(int capacity) {
        if (capacity <= 0) throw new IllegalArgumentException("Capacity must be positive");
        this.capacity = capacity;
        this.cache = new DoublyLinkedList();
        this.map = new HashMap<>();
    }

    public int get(int key) {
        Node node = map.get(key);
        if (node == null) return -1;
        cache.remove(node);
        cache.addFirst(node);
        return node.value;
    }


    public void put(int key, int value) {
        Node node = map.get(key);
        if(node != null) {
            node.value = value;
            cache.remove(node);
            cache.addFirst(node);
        } else {
            if (map.size() == capacity) {
                map.remove(cache.getLast().key);
                cache.removeLast();
            }
            Node newNode = new Node(key, value);
            map.put(key, newNode);
            cache.addFirst(newNode);
        }
    }

    static class Node {
        Node prev;
        Node next;
        int key;
        int value;

        public Node() {}

        public Node(int key, int value) {
            this.key = key;
            this.value = value;
        }
    }

    static class DoublyLinkedList {
        final Node head;
        final Node tail;
        int size;
        public DoublyLinkedList() {
            head = new Node();
            tail = new Node();
            head.next = tail;
            tail.prev = head;
            size = 0;
        }

        public void addFirst(Node node) {
            node.prev = head;
            node.next = head.next;
            head.next.prev = node;
            head.next = node;
            size++;
        }

        public void removeLast() {
            remove(tail.prev);
            size--;
        }

        public void remove(Node node) {
            Node prev = node.prev;
            Node next = node.next;
            prev.next = next;
            next.prev = prev;
            size--;
        }

        public Node getLast() {
            return tail.prev;
        }
    }
}
