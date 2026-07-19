package pt.codered.sky.automata.client.bot.tasks;

import baritone.api.utils.Rotation;
import baritone.api.utils.RotationUtils;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;

import pt.codered.sky.automata.client.bot.Task;

/**
 * Smoothly turns the local player's real view (yaw/pitch) onto {@code target}'s eye position,
 * a bounded step per tick. This sets {@link Entity#setYRot}/{@link Entity#setXRot} directly
 * rather than going through Baritone's {@code ILookBehavior} — that behavior can be configured
 * "silent" (packet-only, camera doesn't move), which satisfies neither half of "look at it and
 * let me see it too". Setting the player's own rotation fields does both: the client renders the
 * turn immediately, and the vanilla movement packet picks up the changed rotation on its own.
 */
public class LookAtEntityTask implements Task {
	private static final float MAX_DEGREES_PER_TICK = 25f;
	private static final float ALIGNED_THRESHOLD_DEGREES = 1.5f;

	private final Entity target;

	public LookAtEntityTask(Entity target) {
		this.target = target;
	}

	@Override
	public void start() {
	}

	@Override
	public boolean tick() {
		LocalPlayer player = Minecraft.getInstance().player;
		if (player == null || !target.isAlive() || target.isRemoved()) {
			return true;
		}

		Rotation current = new Rotation(player.getYRot(), player.getXRot());
		Rotation desired = RotationUtils.calcRotationFromVec3d(player.getEyePosition(), target.getEyePosition(), current);

		float newYaw = step(current.getYaw(), desired.getYaw());
		float newPitch = step(current.getPitch(), desired.getPitch());
		player.setYRot(newYaw);
		player.setXRot(newPitch);

		return Math.abs(desired.getYaw() - newYaw) < ALIGNED_THRESHOLD_DEGREES
				&& Math.abs(desired.getPitch() - newPitch) < ALIGNED_THRESHOLD_DEGREES;
	}

	private static float step(float current, float desired) {
		return current + Mth.clamp(desired - current, -MAX_DEGREES_PER_TICK, MAX_DEGREES_PER_TICK);
	}
}
