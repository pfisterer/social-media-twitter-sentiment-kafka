/**
 * Twitter and Yammer Stream Mining with Esper Demo. This project is open-source under the terms of the GPL license. It was created and is
 * maintained by Dennis Pfisterer.
 */
package de.farberg.social.twittersentimentkafka;

public enum Sentiment {
	NONE_DETECTED(-1), NEGATIVE(1), NEUTRAL(2), HAPPY(3), EUPHORIC(4);

	private int code;

	private Sentiment(int code) {
		this.code = code;
	}

	private int getCode() {
		return code;
	}

	public boolean isBetterOrEqualTo(Sentiment other) {
		return code >= other.getCode();
	}
}