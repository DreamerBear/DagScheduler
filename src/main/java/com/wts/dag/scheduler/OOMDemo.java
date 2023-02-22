package com.wts.dag.scheduler;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * @Package com.wts.dag.scheduler
 * @author: xuchao（xuchao.xxc@ncarzone.com）
 * @date: 2023/1/29 下午4:48
 */
public class OOMDemo {
    public static void main(String[] args) throws InterruptedException {
        try {
            //返回Java虚拟机中的堆内存总量
            long xmsMemory = Runtime.getRuntime().totalMemory() / 1024 / 1024;
            //返回Java虚拟机中使用的最大堆内存
            long xmxMemory = Runtime.getRuntime().maxMemory() / 1024 / 1024;
            System.out.println("-Xms:" + xmsMemory + "M");
            System.out.println("-Xmx:" + xmxMemory + "M");
            System.out.println("start");

            Thread thread = new Thread(() -> {
                List<Long[]> list = new ArrayList<>();
                for (int i = 0; i < 800 * 1024; i++) {
                    list.add(new Long[1 * 1024]);
                }
                System.out.println("initialized");
                while (true) {
                    try {
                        TimeUnit.SECONDS.sleep(10);
                        System.out.println("waiting");
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            });

            thread.start();
            thread.join();
        } finally {
            System.out.println("end");
        }
    }
}
