package pt.codered.sky.automata.client.hypixel;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.phys.AABB;

import pt.codered.sky.automata.SkyAutomata;

/**
 * Reads nearby Hypixel mobs once a second into {@link #NEARBY_MOBS}. Each mob is a base entity
 * (a real vanilla type, e.g. minecraft:zombie, with no custom name of its own) paired with a
 * separate {@link ArmorStand} floating a couple blocks above it carrying the real nameplate text
 * ("[Lv30]  Crypt Ghoul 2,000/2,000❤") — confirmed via a raw unparsed entity dump. The pairing
 * isn't given by any explicit link, so it's inferred here as the nearest non-armor-stand
 * LivingEntity directly below the nameplate, within a small horizontal tolerance.
 */
public final class MobTracker {
	private static final int INTERVAL_TICKS = 20;
	private static final double RADIUS = 16.0;
	private static final double MAX_HORIZONTAL_OFFSET = 0.5;
	private static final double MAX_VERTICAL_OFFSET = 3.0;

	public static volatile List<MobInfo> NEARBY_MOBS = List.of();

	private static int ticksUntilNextUpdate = 0;

	private MobTracker() {
	}

	public static void register() {
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			if (--ticksUntilNextUpdate > 0) {
				return;
			}
			ticksUntilNextUpdate = INTERVAL_TICKS;
			update(client);
		});
	}

	private static void update(Minecraft client) {
		LocalPlayer player = client.player;
		if (player == null || client.level == null) {
			return;
		}

		AABB area = player.getBoundingBox().inflate(RADIUS);
		List<Entity> nearby = client.level.getEntities(player, area, entity -> true);

		List<MobInfo> mobs = new ArrayList<>();
		for (Entity entity : nearby) {
			if (!(entity instanceof ArmorStand) || !entity.hasCustomName()) {
				continue;
			}
			MobInfoParser.Parsed parsed = MobInfoParser.parse(entity.getName().getString());
			if (parsed == null) {
				continue;
			}
			Entity base = findBaseMob(entity, nearby);
			if (base == null) {
				continue;
			}
			mobs.add(new MobInfo(parsed.level(), parsed.name(), parsed.currentHealth(), parsed.maxHealth(), base));
		}
		mobs.sort(Comparator.comparingDouble(mob -> player.distanceTo(mob.entity())));
		NEARBY_MOBS = List.copyOf(mobs);

		StringBuilder log = new StringBuilder("[MobTracker] ").append(mobs.size()).append(" mobs");
		player.displayClientMessage(Component.literal("§7[Mobs] §f" + mobs.size() + " nearby"), false);
		for (MobInfo mob : mobs) {
			String line = String.format("Lv%d %s %.0f/%.0f dist=%.1f",
					mob.level(), mob.name(), mob.currentHealth(), mob.maxHealth(), player.distanceTo(mob.entity()));
			log.append("\n  ").append(line);
			player.displayClientMessage(Component.literal("§7  " + line), false);
		}
		SkyAutomata.LOGGER.info(log.toString());
	}

	private static Entity findBaseMob(Entity nameplate, List<Entity> candidates) {
		Entity best = null;
		double bestVerticalOffset = Double.MAX_VALUE;
		for (Entity candidate : candidates) {
			if (candidate == nameplate || candidate instanceof ArmorStand || !(candidate instanceof LivingEntity)) {
				continue;
			}
			double horizontalOffset = Math.max(
					Math.abs(candidate.getX() - nameplate.getX()),
					Math.abs(candidate.getZ() - nameplate.getZ()));
			if (horizontalOffset > MAX_HORIZONTAL_OFFSET) {
				continue;
			}
			double verticalOffset = nameplate.getY() - candidate.getY();
			if (verticalOffset < 0 || verticalOffset > MAX_VERTICAL_OFFSET) {
				continue;
			}
			if (verticalOffset < bestVerticalOffset) {
				bestVerticalOffset = verticalOffset;
				best = candidate;
			}
		}
		return best;
	}
}
