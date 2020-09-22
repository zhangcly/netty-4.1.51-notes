/*
 * Copyright 2013 The Netty Project
 *
 * The Netty Project licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package io.netty.util.concurrent;

import io.netty.util.internal.DefaultPriorityQueue;
import io.netty.util.internal.PriorityQueueNode;

import java.util.concurrent.Callable;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;

/**
 * 定时执行的任务
 * 两个核心的属性 {@link #deadlineNanos}和 {@link #periodNanos} 核心方法 {@link #run()}
 * ScheduledFutureTask其实是对一个 {@link Runnable}对象的封装 这个{@link Runnable}对象是实际执行的过程任务 即 {@link #task} 在构造方法中被初始化
 * @param <V> 任务执行的返回值的类型
 */
@SuppressWarnings("ComparableImplementedButEqualsNotOverridden")
final class ScheduledFutureTask<V> extends PromiseTask<V> implements ScheduledFuture<V>, PriorityQueueNode {
    private static final long START_TIME = System.nanoTime();

    static long nanoTime() {
        return System.nanoTime() - START_TIME;
    }

    static long deadlineNanos(long delay) {
        long deadlineNanos = nanoTime() + delay;
        // Guard against overflow
        return deadlineNanos < 0 ? Long.MAX_VALUE : deadlineNanos;
    }

    static long initialNanoTime() {
        return START_TIME;
    }

    /**
     * 任务id
     */
    // set once when added to priority queue
    private long id;

    /**
     * task的截止时间（纳秒）
     */
    private long deadlineNanos;
    /* 0 - no repeat, >0 - repeat at fixed rate, <0 - repeat with fixed delay */
    /**
     * 任务循环的时间段（纳秒），如果等于0则不循环即在 {@link #periodNanos}时间之后执行一次，
     * 如果大于0则按照固定的频率循环，如果小于0则按照固定的延迟循环
     * （固定频率是 下一次执行的时间是执行前的时间加上时间段的时长，固定延迟是执行完之后的时间加上时间段的时长）
     */
    private final long periodNanos;

    private int queueIndex = INDEX_NOT_IN_QUEUE;

    ScheduledFutureTask(AbstractScheduledEventExecutor executor,
            Runnable runnable, long nanoTime) {

        super(executor, runnable);
        deadlineNanos = nanoTime;
        periodNanos = 0;
    }

    ScheduledFutureTask(AbstractScheduledEventExecutor executor,
            Runnable runnable, long nanoTime, long period) {

        super(executor, runnable);
        deadlineNanos = nanoTime;
        periodNanos = validatePeriod(period);
    }

    ScheduledFutureTask(AbstractScheduledEventExecutor executor,
            Callable<V> callable, long nanoTime, long period) {

        super(executor, callable);
        deadlineNanos = nanoTime;
        periodNanos = validatePeriod(period);
    }

    ScheduledFutureTask(AbstractScheduledEventExecutor executor,
            Callable<V> callable, long nanoTime) {

        super(executor, callable);
        deadlineNanos = nanoTime;
        periodNanos = 0;
    }

    private static long validatePeriod(long period) {
        if (period == 0) {
            throw new IllegalArgumentException("period: 0 (expected: != 0)");
        }
        return period;
    }

    ScheduledFutureTask<V> setId(long id) {
        if (this.id == 0L) {
            this.id = id;
        }
        return this;
    }

    @Override
    protected EventExecutor executor() {
        return super.executor();
    }

    public long deadlineNanos() {
        return deadlineNanos;
    }

    void setConsumed() {
        // Optimization to avoid checking system clock again
        // after deadline has passed and task has been dequeued
        if (periodNanos == 0) {
            assert nanoTime() >= deadlineNanos;
            deadlineNanos = 0L;
        }
    }

    public long delayNanos() {
        return deadlineToDelayNanos(deadlineNanos());
    }

    static long deadlineToDelayNanos(long deadlineNanos) {
        return deadlineNanos == 0L ? 0L : Math.max(0L, deadlineNanos - nanoTime());
    }

    public long delayNanos(long currentTimeNanos) {
        return deadlineNanos == 0L ? 0L
                : Math.max(0L, deadlineNanos() - (currentTimeNanos - START_TIME));
    }

    @Override
    public long getDelay(TimeUnit unit) {
        return unit.convert(delayNanos(), TimeUnit.NANOSECONDS);
    }

    @Override
    public int compareTo(Delayed o) {
        if (this == o) {
            return 0;
        }

        ScheduledFutureTask<?> that = (ScheduledFutureTask<?>) o;
        long d = deadlineNanos() - that.deadlineNanos();
        if (d < 0) {
            return -1;
        } else if (d > 0) {
            return 1;
        } else if (id < that.id) {
            return -1;
        } else {
            assert id != that.id;
            return 1;
        }
    }

    /**
     * {@link Runnable#run()}的实现，通常会在 {@link AbstractScheduledEventExecutor#schedule(ScheduledFutureTask)}方法中被执行
     * 这个run方法必须在Executor的执行线程中被执行 {@link #task}
     * 在 {@link AbstractScheduledEventExecutor#schedule(ScheduledFutureTask)}中执行一个 {@link ScheduledFutureTask}
     * 会调用子类的{@link AbstractScheduledEventExecutor#execute(Runnable)} 方法，
     * {@link SingleThreadEventExecutor#execute(Runnable)} 的实现中会启动线程将任务放入队列，然后执行子类的run方法去执行这个任务
     */
    @Override
    public void run() {
        assert executor().inEventLoop();
        try {
            if (delayNanos() > 0L) {
                // Not yet expired, need to add or remove from queue
                // 还没有过期
                if (isCancelled()) {
                    scheduledExecutor().scheduledTaskQueue().removeTyped(this);
                } else {
                    scheduledExecutor().scheduleFromEventLoop(this);
                }
                return;
            }
            if (periodNanos == 0) {
                //如果是定时执行一次，那么执行一次就结束了
                if (setUncancellableInternal()) {
                    V result = runTask();
                    setSuccessInternal(result);
                }
            } else {
                // check if is done as it may was cancelled
                if (!isCancelled()) {
                    //执行被封装起来的原生的Runnable对象的run方法
                    runTask();
                    if (!executor().isShutdown()) {
                        if (periodNanos > 0) {
                            //如果按照固定频率，那么这次循环的deadline加上时间段的时长就是下次循环的deadline
                            deadlineNanos += periodNanos;
                        } else {
                            //如果按照固定延迟，就是现在的时间加上时间段的时长，因为这边periodNanos是小于0的，所以这里是减去
                            deadlineNanos = nanoTime() - periodNanos;
                        }
                        if (!isCancelled()) {
                            //如果这个定时任务没有移除 那么将这个任务再次添加到任务的优先队列中去
                            //这就是固定频率和固定延迟的循环执行的实现
                            scheduledExecutor().scheduledTaskQueue().add(this);
                        }
                    }
                }
            }
        } catch (Throwable cause) {
            setFailureInternal(cause);
        }
    }

    private AbstractScheduledEventExecutor scheduledExecutor() {
        return (AbstractScheduledEventExecutor) executor();
    }

    /**
     * {@inheritDoc}
     *
     * @param mayInterruptIfRunning this value has no effect in this implementation.
     */
    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        boolean canceled = super.cancel(mayInterruptIfRunning);
        if (canceled) {
            scheduledExecutor().removeScheduled(this);
        }
        return canceled;
    }

    boolean cancelWithoutRemove(boolean mayInterruptIfRunning) {
        return super.cancel(mayInterruptIfRunning);
    }

    @Override
    protected StringBuilder toStringBuilder() {
        StringBuilder buf = super.toStringBuilder();
        buf.setCharAt(buf.length() - 1, ',');

        return buf.append(" deadline: ")
                  .append(deadlineNanos)
                  .append(", period: ")
                  .append(periodNanos)
                  .append(')');
    }

    @Override
    public int priorityQueueIndex(DefaultPriorityQueue<?> queue) {
        return queueIndex;
    }

    @Override
    public void priorityQueueIndex(DefaultPriorityQueue<?> queue, int i) {
        queueIndex = i;
    }
}
