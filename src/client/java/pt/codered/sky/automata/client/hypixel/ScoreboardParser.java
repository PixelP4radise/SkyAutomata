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
	private static final Pattern NON_WORD_EDGE = Pattern.compile("^[^\\p{L}\\p{N}]+|[^\\p{L}\\p{N}]+$");

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
			if (PURSE.matcher(strip(lines.get(i))).matches()) {
				purseIndex = i;
				break;
			}
		}
		if (purseIndex < 0) {
			return null;
		}
		for (int i = purseIndex - 1; i >= 0; i--) {
			String candidate = strip(lines.get(i));
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
				return strip(matcher.group());
			}
		}
		return null;
	}

	/**
	 * Hypixel pads sidebar lines with invisible filler characters that aren't always classic
	 * whitespace — a non-breaking space (U+00A0) on at least the Crypts line, and something else
	 * entirely on the Graveyard line that isn't even {@link Character#isSpaceChar}, both of which
	 * survive {@link String#trim()}/{@link String#strip()} (Java's whitespace definition
	 * deliberately excludes NBSP, and apparently doesn't cover whatever the other one is either).
	 * Rather than chase each new filler codepoint individually, strip any edge run that isn't a
	 * letter or digit — real location text is always a plain word, so this can't over-trim it.
	 */
	private static String strip(String s) {
		return NON_WORD_EDGE.matcher(s).replaceAll("");
	}
}
