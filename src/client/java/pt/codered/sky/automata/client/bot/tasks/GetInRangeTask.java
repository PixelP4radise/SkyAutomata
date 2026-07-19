package pt.codered.sky.automata.client.bot.tasks;

import baritone.api.BaritoneAPI;
import baritone.api.IBaritone;
import baritone.api.pathing.goals.GoalNear;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;

import pt.codered.sky.automata.client.bot.Task;

/**
 * Paths the player within {@link #RANGE_BLOCKS} of {@code target} via Baritone's custom goal
 * process, completing once {@link BaritonePathTracker} observes a terminal {@code PathEvent}.
 * Already-in-range is short-circuited as an immediate no-op: {@code setGoalAndPath} on an
 * already-satisfied goal never triggers a path calculation, so no {@code PathEvent} would ever
 * fire for the tracker to catch and this task would otherwise stall the queue forever.
 */
public class GetInRangeTask implements Task {
	private static final int RANGE_BLOCKS = 3;

	private final Entity target;
	private final IBaritone baritone = BaritoneAPI.getProvider().getPrimaryBaritone();
	private boolean alreadyDone;

	public GetInRangeTask(Entity target) {
		this.target = target;
	}

	@Override
	public void start() {
		LocalPlayer player = Minecraft.getInstance().player;
		if (player == null || !target.isAlive() || target.isRemoved() || player.distanceTo(target) <= RANGE_BLOCKS) {
			alreadyDone = true;
			return;
		}

		BaritonePathTracker.startTracking(baritone);
		BlockPos goalPos = BlockPos.containing(target.getX(), target.getY(), target.getZ());
		baritone.getCustomGoalProcess().setGoalAndPath(new GoalNear(goalPos, RANGE_BLOCKS));
	}

	@Override
	public boolean tick() {
		if (alreadyDone) {
			return true;
		}
		if (!target.isAlive() || target.isRemoved()) {
			baritone.getPathingBehavior().cancelEverything();
			return true;
		}
		return BaritonePathTracker.isSettled();
	}
}
