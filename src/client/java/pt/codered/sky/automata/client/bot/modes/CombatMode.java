package pt.codered.sky.automata.client.bot.modes;

import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;

import pt.codered.sky.automata.SkyAutomata;
import pt.codered.sky.automata.client.bot.ChoiceSetting;
import pt.codered.sky.automata.client.bot.ModeSetting;
import pt.codered.sky.automata.client.bot.TaskQueue;
import pt.codered.sky.automata.client.hypixel.LocationMobs;
import pt.codered.sky.automata.client.hypixel.MobInfo;
import pt.codered.sky.automata.client.hypixel.MobTracker;
import pt.codered.sky.automata.client.hypixel.ScoreboardTracker;

/**
 * Combat mode: for this iteration only tracks a player-selected target mob name (filtered to the
 * current Hypixel location via {@link LocationMobs}) and reports its nearest live match once a
 * second — no attacking/pathing behavior yet, so {@link #tick(TaskQueue)} never touches the queue.
 */
public class CombatMode extends AbstractMode {
	private static final int INTERVAL_TICKS = 20;

	private final ChoiceSetting<String> targetMobSetting = new TargetMobSetting();
	private volatile String targetMobName;
	private int ticksUntilNextLog = 0;

	public CombatMode() {
		super("Combat");
	}

	@Override
	public List<ModeSetting<?>> getSettings() {
		return List.of(targetMobSetting);
	}

	@Override
	public void onEnter() {
		super.onEnter();
		ticksUntilNextLog = 0;
	}

	@Override
	public void tick(TaskQueue taskQueue) {
		if (--ticksUntilNextLog > 0) {
			return;
		}
		ticksUntilNextLog = INTERVAL_TICKS;

		LocalPlayer player = Minecraft.getInstance().player;
		if (player == null) {
			return;
		}

		String target = targetMobName;
		String message;
		if (target == null) {
			message = "no target mob selected";
		} else {
			MobInfo nearest = findNearest(target);
			message = nearest != null
					? String.format("target=%s Lv%d %.0f/%.0f dist=%.1f", target, nearest.level(),
							nearest.currentHealth(), nearest.maxHealth(), player.distanceTo(nearest.entity()))
					: "target=" + target + " (not currently nearby)";
		}
		SkyAutomata.LOGGER.info("[CombatMode] {}", message);
		player.displayClientMessage(Component.literal("§7[Combat] §f" + message), false);
	}

	private static MobInfo findNearest(String name) {
		for (MobInfo mob : MobTracker.NEARBY_MOBS) {
			if (mob.name().equalsIgnoreCase(name)) {
				return mob; // NEARBY_MOBS is already sorted nearest-first by MobTracker
			}
		}
		return null;
	}

	private final class TargetMobSetting implements ChoiceSetting<String> {
		@Override
		public String getLabel() {
			return "Target Mob";
		}

		@Override
		public String getValue() {
			return targetMobName;
		}

		@Override
		public void setValue(String value) {
			targetMobName = value;
		}

		@Override
		public List<String> getOptions() {
			return LocationMobs.forLocation(ScoreboardTracker.STATE.getLocation());
		}
	}
}
