package pt.codered.sky.automata.client.hypixel;

/**
 * Latest Hypixel Skyblock state parsed off the sidebar scoreboard — kept up to date by
 * {@link ScoreboardTracker}. Fields are null until the first successful parse (e.g. before the
 * sidebar has loaded, or a field the current sidebar doesn't show).
 */
public final class SkyblockState {
	private String location;
	private String date;
	private String time;

	public String getLocation() {
		return location;
	}

	void setLocation(String location) {
		this.location = location;
	}

	public String getDate() {
		return date;
	}

	void setDate(String date) {
		this.date = date;
	}

	public String getTime() {
		return time;
	}

	void setTime(String time) {
		this.time = time;
	}
}
