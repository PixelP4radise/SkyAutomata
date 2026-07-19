package pt.codered.sky.automata.client.hypixel;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Pattern;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.scores.DisplaySlot;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.PlayerScoreEntry;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;

import pt.codered.sky.automata.SkyAutomata;

/**
 * Reads the Hypixel Skyblock sidebar scoreboard once a second and keeps {@link #STATE} up to
 * date. Hypixel still uses the pre-1.20.3 trick of a bare fake-name score entry plus a
 * PlayerTeam prefix/suffix carrying the real visible text — each line is resolved the same way
 * vanilla's own Gui sidebar renderer does it ({@code PlayerTeam.formatNameForTeam}), not by
 * reading {@code PlayerScoreEntry.display()} directly (empty/unused on Hypixel). The resolved
 * text also carries literal "§" codes as plain characters rather than real Style (Hypixel sends
 * it as literal component content). The per-line owner-name marker is itself an arbitrary,
 * often-invalid legacy code letter (e.g. "§s", "§t") chosen only for uniqueness — vanilla's
 * {@code ChatFormatting.stripFormatting} only strips the real code set (0-9a-fk-or) and leaves
 * these behind, so stripping here matches any "§" + one character instead, same as how the
 * client's own font renderer treats an unrecognized code as a no-op rather than visible text.
 */
public final class ScoreboardTracker {
	private static final int INTERVAL_TICKS = 20;
	private static final Pattern ANY_FORMATTING_CODE = Pattern.compile("§.");

	public static final SkyblockState STATE = new SkyblockState();

	private static int ticksUntilNextUpdate = 0;

	private ScoreboardTracker() {
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

		List<String> lines = sidebarLines(client);
		if (lines == null) {
			return;
		}

		STATE.setLocation(ScoreboardParser.parseLocation(lines));
		STATE.setDate(ScoreboardParser.parseDate(lines));
		STATE.setTime(ScoreboardParser.parseTime(lines));

		String summary = "location=" + STATE.getLocation() + " date=" + STATE.getDate() + " time=" + STATE.getTime();
		SkyAutomata.LOGGER.info("[ScoreboardTracker] {}", summary);
		player.displayClientMessage(Component.literal("§7[Skyblock] §f" + summary), false);
	}

	private static List<String> sidebarLines(Minecraft client) {
		Scoreboard scoreboard = client.level.getScoreboard();
		Objective sidebar = scoreboard.getDisplayObjective(DisplaySlot.SIDEBAR);
		if (sidebar == null) {
			return null;
		}

		List<PlayerScoreEntry> entries = new ArrayList<>(scoreboard.listPlayerScores(sidebar));
		entries.removeIf(PlayerScoreEntry::isHidden);
		entries.sort(Comparator.comparingInt(PlayerScoreEntry::value).reversed());

		List<String> lines = new ArrayList<>(entries.size());
		for (PlayerScoreEntry entry : entries) {
			PlayerTeam team = scoreboard.getPlayersTeam(entry.owner());
			String rawLine = PlayerTeam.formatNameForTeam(team, entry.ownerName()).getString();
			lines.add(ANY_FORMATTING_CODE.matcher(rawLine).replaceAll(""));
		}
		return lines;
	}
}
