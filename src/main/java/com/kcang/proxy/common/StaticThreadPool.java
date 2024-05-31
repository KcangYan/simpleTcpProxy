package com.kcang.proxy.common;

import java.lang.reflect.Method;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class StaticThreadPool {
    static class DefaultThreadFactory implements ThreadFactory {
        private static final AtomicInteger poolNumber = new AtomicInteger(1);
        private final ThreadGroup group;
        private final AtomicInteger threadNumber = new AtomicInteger(1);
        private final String namePrefix;

        DefaultThreadFactory(String name) {
            SecurityManager s = System.getSecurityManager();
            group = (s != null) ? s.getThreadGroup() :
                    Thread.currentThread().getThreadGroup();
            namePrefix = name+"-" +
                    poolNumber.getAndIncrement() +
                    "-pool-";
        }

        public Thread newThread(Runnable r) {
            Thread t = new Thread(group, r,
                    namePrefix + threadNumber.getAndIncrement(),
                    0);
            if (t.isDaemon())
                t.setDaemon(false);
            if (t.getPriority() != Thread.NORM_PRIORITY)
                t.setPriority(Thread.NORM_PRIORITY);
            return t;
        }
    }

    private static ThreadPoolExecutor SynchronousPool = null;

    public static void setTask(Runnable task){
        if(SynchronousPool == null){
            SynchronousPool = new ThreadPoolExecutor(
                    1, Parameters.maxThread, 180, TimeUnit.SECONDS,
                    new SynchronousQueue<Runnable>(),
                    new DefaultThreadFactory("Synchronous"),
                    new ThreadPoolExecutor.CallerRunsPolicy()
            );
        }
        SynchronousPool.execute(task);
    }
    public static void setTask(Object object) throws NoSuchMethodException {
        if(SynchronousPool == null){
            SynchronousPool = new ThreadPoolExecutor(
                    1, Parameters.maxThread, 180, TimeUnit.SECONDS,
                    new SynchronousQueue<Runnable>(),
                    new DefaultThreadFactory("Synchronous"),
                    new ThreadPoolExecutor.CallerRunsPolicy()
            );
        }
        Method method = object.getClass().getMethod("run");
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                try {
                    method.invoke(object);
                }catch (Exception e){
                    e.printStackTrace();
                }
            }
        };
        SynchronousPool.execute(runnable);
    }

}
