package pt.codered.sky.automata.client.hypixel;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses a Hypixel mob nameplate armor stand's name, e.g. "[Lv30]  Crypt Ghoul 2,000/2,000❤".
 */
final class MobInfoParser {
	private static final Pattern NAMEPLATE = Pattern.compile("^\\[Lv(\\d+)]\\s+(.+?)\\s+([\\d,]+)/([\\d,]+)❤$");
	private static final Pattern NON_WORD_RUN = Pattern.compile("[^\\p{L}\\p{N}']+");

	private MobInfoParser() {
	}

	record Parsed(int level, String name, double currentHealth, double maxHealth) {
	}

	static Parsed parse(String nameplate) {
		Matcher matcher = NAMEPLATE.matcher(nameplate.trim());
		if (!matcher.matches()) {
			return null;
		}
		int level = Integer.parseInt(matcher.group(1));
		String name = normalizeName(matcher.group(2));
		double currentHealth = parseNumber(matcher.group(3));
		double maxHealth = parseNumber(matcher.group(4));
		return new Parsed(level, name, currentHealth, maxHealth);
	}

	/**
	 * Collapses any run of non-letter/non-digit characters (including whatever invisible filler
	 * Hypixel put there — same class of gotcha already hit twice in {@code ScoreboardParser}'s
	 * location text, and the reason "Graveyard Zombie" from here silently failed to
	 * {@code equalsIgnoreCase} an identical-looking literal in {@code LocationMobs}) down to a
	 * single space, so the name compares and displays cleanly regardless of the exact codepoint.
	 */
	private static String normalizeName(String raw) {
		return NON_WORD_RUN.matcher(raw.trim()).replaceAll(" ").trim();
	}

	private static double parseNumber(String withCommas) {
		return Double.parseDouble(withCommas.replace(",", ""));
	}
}
