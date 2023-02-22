package com.wts.dag.scheduler;

/**
 * @Package com.wts.dag.scheduler
 * @author: xuchao（xuchao.xxc@ncarzone.com）
 * @date: 2023/1/17 下午5:11
 */
public class WithoutMonitorLockRule {

    private static boolean stop = false;

    public static void main(String[] args) {

        Thread updater = new Thread(() -> {

            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            stop = true;

            System.out.println("updater set stop true");

        }, "updater");

        Thread getter = new Thread(() -> {

            while (true) {
                if (stop) {

                    System.out.println("getter stopped");

                    break;

                }

            }

        }, "getter");
        updater.start();
        getter.start();
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println(stop);

    }

}
