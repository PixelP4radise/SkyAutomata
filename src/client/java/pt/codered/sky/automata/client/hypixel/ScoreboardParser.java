package pt.codered.sky.automata.client.hypixel;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Pulls specific fields out of the Hypixel Skyblock sidebar's formatting-stripped lines.
 * Matches by content, not line position — the sidebar grows/shrinks extra lines (guild status,
 * events, dungeon info, ...) depending on where you are, so a fixed line index isn't reliable.
 */
final class ScoreboardParser {
	private static final Pattern DATE = Pattern.compile("(Early|Late)? ?(Spring|Summer|Autumn|Winter) \\d{1,2}(st|nd|rd|th)");
	private static final Pattern TIME = Pattern.compile("\\d{1,2}:\\d{2}\\s?[ap]m");
	private static final Pattern PURSE = Pattern.compile("^Purse:.*");

	private ScoreboardParser() {
	}

	static String parseDate(List<String> lines) {
		return findMatch(lines, DATE);
	}

	static String parseTime(List<String> lines) {
		return findMatch(lines, TIME);
	}

	/**
	 * Hypixel doesn't mark the area/location line with any fixed prefix — the one stable
	 * convention across islands is that it sits directly above the "Purse:" line.
	 */
	static String parseLocation(List<String> lines) {
		int purseIndex = -1;
		for (int i = 0; i < lines.size(); i++) {
			if (PURSE.matcher(lines.get(i).trim()).matches()) {
				purseIndex = i;
				break;
			}
		}
		if (purseIndex < 0) {
			return null;
		}
		for (int i = purseIndex - 1; i >= 0; i--) {
			String candidate = lines.get(i).trim();
			if (!candidate.isEmpty()) {
				return candidate;
			}
		}
		return null;
	}

	private static String findMatch(List<String> lines, Pattern pattern) {
		for (String line : lines) {
			Matcher matcher = pattern.matcher(line);
			if (matcher.find()) {
				return matcher.group().trim();
			}
		}
		return null;
	}
}
