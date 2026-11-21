package com.example.orders.scheduler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;

/**
 * <b>Template Method pattern</b>: the skeleton of every background job (timing, logging,
 * and never letting an exception kill the schedule) is fixed in {@link #execute()};
 * subclasses fill in {@link #doRun()} and may override the hooks.
 */
public abstract class AbstractScheduledJob {

    protected final Logger log = LoggerFactory.getLogger(getClass());

    /** The template method. */
    public final void execute() {
        long start = System.nanoTime();
        try {
            beforeRun();
            int processed = doRun();
            afterRun(processed, Duration.ofNanos(System.nanoTime() - start));
        } catch (RuntimeException e) {
            onError(e);   // the next scheduled run will retry
        }
    }

    protected abstract String jobName();

    /** Does the work; returns how many items were processed. */
    protected abstract int doRun();

    protected void beforeRun() {
        log.debug("Job '{}' starting", jobName());
    }

    protected void afterRun(int processed, Duration took) {
        if (processed > 0) {
            log.info("Job '{}' processed {} item(s) in {} ms", jobName(), processed, took.toMillis());
        } else {
            log.debug("Job '{}' found nothing to do", jobName());
        }
    }

    protected void onError(RuntimeException e) {
        log.error("Job '{}' failed", jobName(), e);
    }
}
