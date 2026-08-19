// SpellScheduler.java
package top.ydog01.mmagic.spell;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;

@EventBusSubscriber
public final class SpellScheduler {
    private static final Deque<Runnable> QUEUE = new ArrayDeque<>();
    private static final List<Task> DELAYED = new ArrayList<>();
    
    private static ActiveSpellManager manager;

    private SpellScheduler() {}

    public static void setManager(ActiveSpellManager m) {
        manager = m;
    }

    public static void schedule(int delayTicks, Runnable action) {
        if (delayTicks <= 0) {
            QUEUE.addLast(action);
        } else {
            DELAYED.add(new Task(delayTicks, action));
        }
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        if (manager != null) {
            manager.tick();
        }
        
        for (Iterator<Task> it = DELAYED.iterator(); it.hasNext(); ) {
            Task task = it.next();
            if (--task.remaining <= 0) {
                it.remove();
                QUEUE.addLast(task.action);
            }
        }
        while (!QUEUE.isEmpty()) {
            Runnable action = QUEUE.pollFirst();
            try {
                action.run();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private static final class Task {
        int remaining;
        final Runnable action;
        Task(int remaining, Runnable action) {
            this.remaining = remaining;
            this.action = action;
        }
    }
}