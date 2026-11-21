package com.example.orders.scheduler;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/** Template Method: the skeleton calls the hooks in order and swallows failures. */
class AbstractScheduledJobTest {

    static class RecordingJob extends AbstractScheduledJob {
        final List<String> calls = new ArrayList<>();
        final boolean fail;

        RecordingJob(boolean fail) { this.fail = fail; }

        @Override protected String jobName() { return "test"; }
        @Override protected int doRun() {
            calls.add("run");
            if (fail) throw new IllegalStateException("boom");
            return 3;
        }
        @Override protected void beforeRun() { calls.add("before"); }
        @Override protected void afterRun(int processed, Duration took) { calls.add("after:" + processed); }
        @Override protected void onError(RuntimeException e) { calls.add("error:" + e.getMessage()); }
    }

    @Test
    void runsHooksInOrder() {
        RecordingJob job = new RecordingJob(false);
        job.execute();
        assertThat(job.calls).containsExactly("before", "run", "after:3");
    }

    @Test
    void failureIsHandledAndDoesNotPropagate() {
        RecordingJob job = new RecordingJob(true);
        assertThatCode(job::execute).doesNotThrowAnyException();
        assertThat(job.calls).containsExactly("before", "run", "error:boom");
    }
}
