package com.messickmedia.grandline.fruit;

import net.minecraft.server.MinecraftServer;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.IntConsumer;

/** Runs server-side follow-ups (extra punches, beam ticks, dash steps) a few ticks later. */
public final class Scheduler {
    private Scheduler() {}

    private record Task(int runAt, Runnable action) {}

    private static final List<Task> TASKS = new ArrayList<>();
    private static int now;

    public static void schedule(int delayTicks, Runnable action) {
        TASKS.add(new Task(now + Math.max(1, delayTicks), action));
    }

    /** Runs action(i) for i in [0, count) every "every" ticks, starting after "every" ticks. */
    public static void repeat(int count, int every, IntConsumer action) {
        for (int i = 0; i < count; i++) {
            final int k = i;
            schedule(every * (i + 1), () -> action.accept(k));
        }
    }

    public static void tick(MinecraftServer server) {
        now = server.getTickCount();
        if (TASKS.isEmpty()) return;
        List<Task> due = new ArrayList<>();
        for (Iterator<Task> it = TASKS.iterator(); it.hasNext(); ) {
            Task t = it.next();
            if (t.runAt() <= now) {
                due.add(t);
                it.remove();
            }
        }
        for (Task t : due) t.action().run();
    }

    public static void clear() {
        TASKS.clear();
    }
}
