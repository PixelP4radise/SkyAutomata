package pt.codered.sky.automata.client.bot;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * The Actuators: a FIFO queue of {@link Task}s that advances at most one task's state
 * per {@link #tick()}, so a {@link Mode} never performs more than one primitive action
 * within a single client tick.
 */
public class TaskQueue {
	private final Deque<Task> pending = new ArrayDeque<>();
	private Task current;

	public void push(Task task) {
		pending.addLast(task);
	}

	public boolean isIdle() {
		return current == null && pending.isEmpty();
	}

	public void tick() {
		if (current == null) {
			current = pending.poll();
			if (current == null) {
				return;
			}
			current.start();
		}

		if (current.tick()) {
			current = null;
		}
	}

	public void clear() {
		pending.clear();
		current = null;
	}
}
