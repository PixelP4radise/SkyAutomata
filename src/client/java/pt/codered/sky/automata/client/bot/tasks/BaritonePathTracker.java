package pt.codered.sky.automata.client.bot.tasks;

import baritone.api.IBaritone;
import baritone.api.event.events.PathEvent;
import baritone.api.event.listener.AbstractGameEventListener;

/**
 * Bridges Baritone's async {@link PathEvent} callbacks into a flag {@link GetInRangeTask} can
 * check from its own non-blocking {@code tick()}, per the "advance the queue only once the
 * callback fires" rule for Baritone-backed tasks. Baritone's public API has no way to unregister
 * an {@link baritone.api.event.listener.IGameEventListener}, so this registers exactly one
 * listener per {@link IBaritone} (lazily, on first use) instead of leaking a fresh one every time
 * a task starts a path.
 */
final class BaritonePathTracker {
	private static volatile boolean listenerRegistered = false;
	private static volatile boolean settled = true;

	private BaritonePathTracker() {
	}

	static synchronized void startTracking(IBaritone baritone) {
		if (!listenerRegistered) {
			baritone.getGameEventHandler().registerEventListener(new AbstractGameEventListener() {
				@Override
				public void onPathEvent(PathEvent event) {
					if (event == PathEvent.AT_GOAL || event == PathEvent.CANCELED || event == PathEvent.CALC_FAILED) {
						settled = true;
					}
				}
			});
			listenerRegistered = true;
		}
		settled = false;
	}

	static boolean isSettled() {
		return settled;
	}
}
