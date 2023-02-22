package com.wts.dag.scheduler;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @Package com.wts.dag.scheduler
 * @author: xuchao（xuchao.xxc@ncarzone.com）
 * @date: 2023/2/16 下午4:12
 */
public class ConcurrentArrayListAddSolver {

    public static void main(String[] args) throws InterruptedException {

        ArrayList<Integer> list = new ArrayList<>();
        AtomicInteger counter = new AtomicInteger();

        List<Thread> threadList = new ArrayList<>();
        CountDownLatch countDownLatch = new CountDownLatch(100);
        for (int i = 0; i < 100; i++) {
            threadList.add(new Thread() {
                @Override
                public void run() {
                    for (int j = 0; j < 100; j++) {
                        list.add(counter.getAndIncrement());
                    }
                    countDownLatch.countDown();
                }
            });
        }
        threadList.forEach(Thread::start);

        countDownLatch.await();
        System.out.println(list);
        System.out.println(list.size());
        System.out.println(counter);
    }
}
