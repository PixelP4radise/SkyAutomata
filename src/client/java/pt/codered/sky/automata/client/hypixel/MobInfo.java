package pt.codered.sky.automata.client.hypixel;

import net.minecraft.world.entity.Entity;

/**
 * A Hypixel Skyblock mob resolved from its nameplate armor stand. {@code currentHealth}/
 * {@code maxHealth} come from the nameplate text, not {@code entity}'s own vanilla health
 * attribute — the two diverge for high-HP mobs (e.g. a "2,000/2,000" mob can sit on a base
 * entity whose real attribute is capped at 1024), so the nameplate is the source of truth.
 */
public record MobInfo(int level, String name, double currentHealth, double maxHealth, Entity entity) {
}
