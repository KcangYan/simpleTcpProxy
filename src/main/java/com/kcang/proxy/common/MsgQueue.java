package com.kcang.proxy.common;

import com.kcang.proxy.AppStart;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class MsgQueue<E> {
    private static Logger myLogger = LoggerFactory.getLogger(MsgQueue.class);

    private final ReentrantLock lock;
    private final Condition notEmpty;
    private final Condition notFull;
    private final Condition emptySize;
    private final Object[] items;
    private int count;
    private int putIndex;
    private int takeIndex;
    private boolean isDestroy = false;
    public MsgQueue(int capacity, boolean fair){
        if(capacity <= 0) throw new IllegalArgumentException();
        this.items = new Object[capacity];
        this.lock = new ReentrantLock(fair);
        this.notEmpty = this.lock.newCondition();
        this.notFull = this.lock.newCondition();
        this.emptySize = this.lock.newCondition();
    }
    public MsgQueue(int capacity){
        this(capacity, false);
    }
    public MsgQueue(){
        this(4396);
    }
    private void checkNull(Object o){
        if(o == null) throw  new NullPointerException("队列不接受null");
    }
    private void enqueue(E x){
        //final Object[] items = this.items;
        items[putIndex] = x;
        if(++putIndex == items.length) putIndex = 0;
        count++;
        notEmpty.signal();
        emptySize.signalAll();
    }
    private E dequeue(){
        E x = (E) items[takeIndex];
        items[takeIndex] = null;
        if(++takeIndex == items.length) takeIndex = 0;
        count--;
        notFull.signal();
        return x;
    }
    public void put(E e) throws InterruptedException{
        checkNull(e);
        lock.lock();
        try {
            while (count == items.length && !isDestroy) notFull.await();
            if(isDestroy) return;
            enqueue(e);
        }finally {
            lock.unlock();
        }
    }
    public E take() throws InterruptedException{
        lock.lock();
        try {
            while (count == 0 && !isDestroy) notEmpty.await();
            if(isDestroy) return null;
            return dequeue();
        }finally {
            lock.unlock();
        }
    }
    public E poll(){
        lock.lock();
        try {
            return (count == 0) ? null:dequeue();
        }finally {
            lock.unlock();
        }
    }
    public int size() throws InterruptedException{
        lock.lock();
        try {
            while (count == 0 && !isDestroy) emptySize.await();
            return count;
        }finally {
            lock.unlock();
        }
    }
    public int size(long timeout, TimeUnit unit) throws InterruptedException{
        long nanos = unit.toNanos(timeout);
        lock.lock();
        try {
            if(count == 0) emptySize.awaitNanos(nanos);
            return count;
        }finally {
            lock.unlock();
        }
    }

    /**
     * 释放所有阻塞线程
     */
    public void destroy(){
        lock.lock();
        try{
            isDestroy = true;
            emptySize.signalAll();
            notFull.signalAll();
            notEmpty.signalAll();
        }finally {
            lock.unlock();
        }
    }
}
