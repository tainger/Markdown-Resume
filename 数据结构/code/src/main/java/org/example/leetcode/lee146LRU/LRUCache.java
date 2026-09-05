package org.example.leetcode.lee146LRU;

import org.w3c.dom.Node;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * @author jiazhiyuan
 * @date 2026/9/5 16:32
 */
public class LRUCache {


    private static class Node{
        Integer key;
        Integer value;
        Node pre;
        Node next;

        Node(int key, int value) {
            this.key = key;
            this.value = value;
        }
    }

    private final  int capacity;
    private final  Map<Integer, Node> cache;

    private final Node head;

    private final Node tail;


    public LRUCache(int capacity) {
        this.capacity = capacity;
        this.cache = new HashMap<>(capacity);
        this.head = new Node(0, 0);
        this.tail = new Node(0, 0);
        this.head.next = tail;
        this.tail.pre = head;
    }

    public int get(int key) {
        Node value = this.cache.get(key);
        if(value == null) {
            return  -1;
        }
        moveToHead(value);
        return value.value;
    }

    public void put(int key, int value) {
        //插入放在最新
        Node nodeValue = this.cache.get(key);
        if(null != nodeValue) {
            nodeValue.value = value;
            moveToHead(nodeValue);
        }else {
            Node newNode  = new Node(key, value);
            this.cache.put(key, newNode);
            addToHead(newNode);

            if(this.cache.size() > capacity) {
                Node tailNode = removeTail();
                this.cache.remove(tailNode.key);
            }

        }



    }

    private Node removeTail() {
       Node node = this.tail.pre;
       Node preNode = node.pre;
       preNode.next = this.tail;
       this.tail.pre = preNode;
       return node;
    }

    private void addToHead(Node newNode) {

        newNode.next = this.head.next;

        newNode.next.pre = newNode;

        newNode.pre = this.head;
        this.head.next = newNode;


    }

    private void moveToHead(Node nodeValue) {
        removeNode(nodeValue);
        addToHead(nodeValue);
    }

    private void removeNode(Node node) {
        node.pre.next = node.next;
        node.next.pre = node.pre;
    }


}



    
