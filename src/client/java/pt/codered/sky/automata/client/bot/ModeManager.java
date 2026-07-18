package pt.codered.sky.automata.client.bot;

/**
 * The Brain: owns the active {@link Mode} and its {@link TaskQueue}, and exposes the
 * single {@link #tick()} entry point driven from the client tick event.
 */
public class ModeManager {
	private final TaskQueue taskQueue = new TaskQueue();
	private Mode activeMode;

	public void setMode(Mode mode) {
		if (activeMode != null) {
			activeMode.onExit();
		}
		taskQueue.clear();
		activeMode = mode;
		if (activeMode != null) {
			activeMode.onEnter();
		}
	}

	public Mode getActiveMode() {
		return activeMode;
	}

	public void tick() {
		taskQueue.tick();
		if (activeMode != null) {
			activeMode.tick(taskQueue);
		}
	}
}
