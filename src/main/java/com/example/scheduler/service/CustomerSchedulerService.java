package com.example.scheduler.service;

import com.example.scheduler.model.ScheduledTask;

import java.util.Comparator;
import java.util.PriorityQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class CustomerSchedulerService {

    private final PriorityQueue<ScheduledTask> taskQueue;

    private final Lock lock = new ReentrantLock();

    private final Condition newTaskAdded = lock.newCondition();

    private final ThreadPoolExecutor workerExecutor;

    public CustomerSchedulerService(int workerThreadSize) {
        this.taskQueue = new PriorityQueue<>(Comparator.comparingLong(ScheduledTask::getScheduledTime));
        this.workerExecutor = (ThreadPoolExecutor) Executors.newFixedThreadPool(workerThreadSize);
    }

    public void start() {
        long timeToSleep = 0;
        while (true) {
            lock.lock();

            try {
                while (taskQueue.isEmpty()) {
                    // Wait until task is scheduled
                    newTaskAdded.await();
                }

                while (!taskQueue.isEmpty()) {
                    timeToSleep = taskQueue.peek().getScheduledTime() - System.currentTimeMillis();
                    if (timeToSleep <= 0) {
                        break; // It's time to run the scheduled task
                    }

                    newTaskAdded.await(timeToSleep, TimeUnit.MILLISECONDS);
                }

                ScheduledTask task = taskQueue.poll();
                long newScheduledTime = 0; // Applicable for recurring tasks only (Type 2 and Type 3)

                switch(task.getTaskType()) {
                    case 1:
                        // Task will be executed only once
                        workerExecutor.submit(task.getRunnable());
                        break;

                    case 2:
                        // Task will be executed at fixed intervals
                        newScheduledTime = System.currentTimeMillis() + task.getUnit().toMillis(task.getPeriod());
                        workerExecutor.submit(task.getRunnable());
                        task.setScheduledTime(newScheduledTime);
                        taskQueue.add(task); // Add task again for subsequent occurrence
                        break;

                    case 3:
                        // Task will be executed after cooldown period once this task gets completed
                        Future<?> future = workerExecutor.submit(task.getRunnable());
                        future.get(); // Wait until this task finishes
                        newScheduledTime = System.currentTimeMillis() + task.getUnit().toMillis(task.getDelay());
                        task.setScheduledTime(newScheduledTime);
                        taskQueue.add(task); // Add task again for subsequent occurrence
                        break;
                }

            } catch (Exception ex) {
                System.out.println("Exception occurred while starting a task");
                ex.printStackTrace();
            } finally {
                lock.unlock();
            }
        }
    }

    /**
     * Creates and executes one shot action that becomes enabled after the given initial delay
     */
    public void schedule(Runnable command, long delay, TimeUnit unit) {
        lock.lock();

        try {
            long scheduledTime = System.currentTimeMillis() + unit.toMillis(delay);
            ScheduledTask task = new ScheduledTask(command, scheduledTime, null, null, 1, unit);
            taskQueue.add(task);
            newTaskAdded.signalAll();
        } catch (Exception ex) {
            System.out.println("Exception occurred while scheduling one time task (Type 1). Error details " + ex);
        } finally {
            lock.unlock();
        }
    }

    /**
     * Creates and executes a periodic action that becomes enabled first after the given initial delay
     * and subsequently with the given period; That is executions will commence after initialDelay then
     * initialDelay + period then initialDelay + 2 * period and so on
     */

    public void scheduledAtFixedRate(Runnable command, long initialDelay, long period, TimeUnit unit) {
        lock.lock();

        try {
            long scheduledTime = System.currentTimeMillis() + unit.toMillis(initialDelay);
            ScheduledTask task = new ScheduledTask(command, scheduledTime, period, null, 2, unit);
            taskQueue.add(task);
            newTaskAdded.signalAll();
        } catch (Exception ex) {
            System.out.println("Exception occurred while scheduling task that runs at fixed interval (Type 2). Error details " + ex);
        } finally {
            lock.unlock();
        }
    }

    /**
     * Creates and executes a periodic action that becomes enabled first after the initial delay and subsequently
     * with the given delay between the termination of one execution and the commencement of the next.
     */
    public void scheduledWithFixedDelay(Runnable command, long initialDelay, long delay, TimeUnit unit) {
        lock.lock();

        try {
            long scheduledTime = System.currentTimeMillis() + unit.toMillis(initialDelay);
            ScheduledTask task = new ScheduledTask(command, scheduledTime, null, delay, 3, unit);
            taskQueue.add(task);
            newTaskAdded.signalAll();
        } catch (Exception ex) {
            System.out.println("Exception occurred while scheduling task that runs with fixed delay (Type 2). Error details " + ex);
        } finally {
            lock.unlock();
        }
    }
}
