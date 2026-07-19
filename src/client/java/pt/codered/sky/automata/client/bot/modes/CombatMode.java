package pt.codered.sky.automata.client.bot.modes;

import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;

import pt.codered.sky.automata.SkyAutomata;
import pt.codered.sky.automata.client.bot.ModeSetting;
import pt.codered.sky.automata.client.bot.MultiChoiceSetting;
import pt.codered.sky.automata.client.bot.TaskQueue;
import pt.codered.sky.automata.client.bot.tasks.GetInRangeTask;
import pt.codered.sky.automata.client.bot.tasks.LookAtEntityTask;
import pt.codered.sky.automata.client.hypixel.LocationMobs;
import pt.codered.sky.automata.client.hypixel.MobInfo;
import pt.codered.sky.automata.client.hypixel.MobTracker;
import pt.codered.sky.automata.client.hypixel.ScoreboardTracker;

/**
 * Combat mode: chases the player-selected target mobs (filtered to the current Hypixel location
 * via {@link LocationMobs}, tried in selection order so earlier picks act as priority over later
 * ones). Whenever the queue is idle and a valid match is nearby it pushes a
 * {@link LookAtEntityTask} then a {@link GetInRangeTask} — no attacking yet, that's the next
 * task to add once the bot can reliably reach a target.
 */
public class CombatMode extends AbstractMode {
	private static final int INTERVAL_TICKS = 20;

	private final MultiChoiceSetting<String> targetMobsSetting = new TargetMobsSetting();
	private volatile List<String> selectedMobNames = List.of();
	private int ticksUntilNextLog = 0;

	public CombatMode() {
		super("Combat");
	}

	@Override
	public List<ModeSetting<?>> getSettings() {
		return List.of(targetMobsSetting);
	}

	@Override
	public void onEnter() {
		super.onEnter();
		ticksUntilNextLog = 0;
	}

	@Override
	public void tick(TaskQueue taskQueue) {
		LocalPlayer player = Minecraft.getInstance().player;
		if (player == null) {
			return;
		}

		List<String> targets = selectedMobNames;
		if (--ticksUntilNextLog <= 0) {
			ticksUntilNextLog = INTERVAL_TICKS;
			logStatus(player, targets);
		}

		if (targets.isEmpty() || !taskQueue.isIdle()) {
			return;
		}

		MobInfo target = findTarget(targets);
		if (target == null) {
			return;
		}
		taskQueue.push(new LookAtEntityTask(target.entity()));
		taskQueue.push(new GetInRangeTask(target.entity()));
	}

	private void logStatus(LocalPlayer player, List<String> targets) {
		if (targets.isEmpty()) {
			String message = "no target mobs selected";
			SkyAutomata.LOGGER.info("[CombatMode] {}", message);
			player.displayClientMessage(Component.literal("§7[Combat] §f" + message), false);
			return;
		}

		StringBuilder log = new StringBuilder("[CombatMode]");
		for (String name : targets) {
			MobInfo nearest = findNearest(name);
			String line = nearest != null
					? String.format("target=%s Lv%d %.0f/%.0f dist=%.1f", name, nearest.level(),
							nearest.currentHealth(), nearest.maxHealth(), player.distanceTo(nearest.entity()))
					: "target=" + name + " (not currently nearby)";
			log.append("\n  ").append(line);
			player.displayClientMessage(Component.literal("§7[Combat] §f" + line), false);
		}
		SkyAutomata.LOGGER.info(log.toString());
	}

	/** First selected name, in priority order, that currently has a valid nearby match. */
	private static MobInfo findTarget(List<String> targets) {
		for (String name : targets) {
			MobInfo nearest = findNearest(name);
			if (nearest != null) {
				return nearest;
			}
		}
		return null;
	}

	private static MobInfo findNearest(String name) {
		for (MobInfo mob : MobTracker.NEARBY_MOBS) {
			if (mob.name().equalsIgnoreCase(name) && mob.isValid()) {
				return mob; // NEARBY_MOBS is already sorted nearest-first by MobTracker
			}
		}
		return null;
	}

	private final class TargetMobsSetting implements MultiChoiceSetting<String> {
		@Override
		public String getLabel() {
			return "Target Mobs";
		}

		@Override
		public List<String> getValue() {
			return selectedMobNames;
		}

		@Override
		public void setValue(List<String> value) {
			selectedMobNames = List.copyOf(value);
		}

		@Override
		public List<String> getOptions() {
			return LocationMobs.forLocation(ScoreboardTracker.STATE.getLocation());
		}
	}
}
