package pt.codered.sky.automata.client.bot;

import java.util.ArrayList;
import java.util.List;

/**
 * The Brain: owns the active {@link Mode} and its {@link TaskQueue}, and exposes the
 * single {@link #tick()} entry point driven from the client tick event.
 */
public class ModeManager {
	private final TaskQueue taskQueue = new TaskQueue();
	private final List<ModeChangeListener> listeners = new ArrayList<>();
	private Mode activeMode;

	/**
	 * Notified whenever {@link #setMode(Mode)} switches the active mode — e.g. to surface
	 * the change as UI/chat feedback without coupling this Model layer to any View.
	 */
	public interface ModeChangeListener {
		void onModeChange(Mode previous, Mode next);
	}

	public void addListener(ModeChangeListener listener) {
		listeners.add(listener);
	}

	public void setMode(Mode mode) {
		Mode previous = activeMode;
		if (activeMode != null) {
			activeMode.onExit();
		}
		taskQueue.clear();
		activeMode = mode;
		if (activeMode != null) {
			activeMode.onEnter();
		}
		for (ModeChangeListener listener : listeners) {
			listener.onModeChange(previous, activeMode);
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
