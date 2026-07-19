package pt.codered.sky.automata.client.hypixel;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.ArmorStand;

/**
 * A Hypixel Skyblock mob resolved from its nameplate armor stand. {@code currentHealth}/
 * {@code maxHealth} come from the nameplate text, not {@code entity}'s own vanilla health
 * attribute — the two diverge for high-HP mobs (e.g. a "2,000/2,000" mob can sit on a base
 * entity whose real attribute is capped at 1024), so the nameplate is the source of truth.
 */
public record MobInfo(int level, String name, double currentHealth, double maxHealth, Entity entity, ArmorStand nameplate) {

	/**
	 * {@code false} once either half of the entity/nameplate pairing MobTracker resolved has
	 * despawned or died since the last scan — MobTracker only rescans once a second, so a
	 * cached MobInfo can go stale mid-cycle (e.g. another player kills it) before combat logic
	 * acts on it. Callers should skip any target that fails this check.
	 */
	public boolean isValid() {
		return entity != null && entity.isAlive() && !entity.isRemoved()
				&& nameplate != null && !nameplate.isRemoved();
	}
}
