package pt.codered.sky.automata.client.bot;

/**
 * A single primitive, non-blocking action pushed onto a {@link TaskQueue}.
 * Implementations must never block the client thread or poll — a task waiting on an
 * async callback (e.g. Baritone) should return {@code false} from {@link #tick()} until
 * its callback flips an internal completion flag.
 */
public interface Task {
	void start();

	/**
	 * @return {@code true} once the task has finished and the queue may advance.
	 */
	boolean tick();
}
