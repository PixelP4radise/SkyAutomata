package pt.codered.sky.automata.client.hypixel;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses a Hypixel mob nameplate armor stand's name, e.g. "[Lv30]  Crypt Ghoul 2,000/2,000❤".
 */
final class MobInfoParser {
	private static final Pattern NAMEPLATE = Pattern.compile("^\\[Lv(\\d+)]\\s+(.+?)\\s+([\\d,]+)/([\\d,]+)❤$");

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
		String name = matcher.group(2);
		double currentHealth = parseNumber(matcher.group(3));
		double maxHealth = parseNumber(matcher.group(4));
		return new Parsed(level, name, currentHealth, maxHealth);
	}

	private static double parseNumber(String withCommas) {
		return Double.parseDouble(withCommas.replace(",", ""));
	}
}
