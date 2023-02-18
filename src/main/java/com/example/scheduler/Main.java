package com.example.scheduler;

import com.example.scheduler.service.CustomSchedulerService;

import java.util.concurrent.TimeUnit;

public class Main {

    public static void main(String[] args) {
        CustomSchedulerService schedulerService = new CustomSchedulerService(10);

        Runnable task1 = getRunnableTask("Task1");
        schedulerService.schedule(task1, 1, TimeUnit.SECONDS);

        Runnable task2 = getRunnableTask("Task2");
        schedulerService.scheduledAtFixedRate(task2, 1, 2, TimeUnit.SECONDS);

        Runnable task3 = getRunnableTask("Task3");
        schedulerService.scheduledWithFixedDelay(task3, 1, 2, TimeUnit.SECONDS);

        Runnable task4 = getRunnableTask("Task4");
        schedulerService.scheduledAtFixedRate(task4, 1, 2, TimeUnit.SECONDS);

        schedulerService.start();
    }

    private static Runnable getRunnableTask(String s) {
        return () -> {
            System.out.println("Task " + s + " started at " + System.currentTimeMillis() / 1000);
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ex) {
                ex.printStackTrace();
            }

            System.out.println("Task " + s + " ended at " + System.currentTimeMillis() / 1000);
        };
    }
}
