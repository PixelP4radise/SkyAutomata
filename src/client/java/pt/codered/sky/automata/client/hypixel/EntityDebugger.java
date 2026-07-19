package pt.codered.sky.automata.client.hypixel;

import java.util.Comparator;
import java.util.List;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;

import pt.codered.sky.automata.SkyAutomata;

/**
 * Temporary diagnostic — dumps every entity within {@link #RADIUS} blocks of the player to chat
 * and the log once a second, completely unparsed (raw name string incl. any "§" content, id,
 * position, health). Same exploratory step as the old sidebar dump: see the real data Hypixel
 * sends for mobs (custom name text, health-bar armor stands, ...) before writing a real
 * mob-info parser against it.
 */
public final class EntityDebugger {
	private static final int INTERVAL_TICKS = 20;
	private static final double RADIUS = 16.0;

	private static int ticksUntilNextDump = 0;

	private EntityDebugger() {
	}

	public static void register() {
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			if (--ticksUntilNextDump > 0) {
				return;
			}
			ticksUntilNextDump = INTERVAL_TICKS;
			dump(client);
		});
	}

	private static void dump(Minecraft client) {
		LocalPlayer player = client.player;
		if (player == null || client.level == null) {
			return;
		}

		AABB area = player.getBoundingBox().inflate(RADIUS);
		List<Entity> nearby = client.level.getEntities(player, area, entity -> true).stream()
				.sorted(Comparator.comparingDouble(player::distanceTo))
				.toList();

		StringBuilder log = new StringBuilder("[EntityDebugger] ").append(nearby.size()).append(" nearby entities");
		player.displayClientMessage(Component.literal("§7[Entities] §f" + nearby.size() + " nearby"), false);

		for (Entity entity : nearby) {
			String line = describe(player, entity);
			log.append("\n  ").append(line);
			player.displayClientMessage(Component.literal("§7  " + line), false);
		}

		SkyAutomata.LOGGER.info(log.toString());
	}

	private static String describe(LocalPlayer player, Entity entity) {
		String health = "";
		if (entity instanceof LivingEntity living) {
			health = " health=" + living.getHealth() + "/" + living.getMaxHealth();
		}
		return String.format(
				"id=%d type=%s dist=%.1f pos=(%.1f,%.1f,%.1f) name=\"%s\" customName=%b%s",
				entity.getId(),
				EntityType.getKey(entity.getType()),
				player.distanceTo(entity),
				entity.getX(), entity.getY(), entity.getZ(),
				entity.getName().getString(),
				entity.hasCustomName(),
				health);
	}
}
