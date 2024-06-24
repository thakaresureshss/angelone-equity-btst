package com.trade.algotrade.client.kotak.response;

import java.io.IOException;

public enum Type {
	TAG_BLUE, TAG_VIOLET, TAG_YELLOW;

	public String toValue() {
		switch (this) {
		case TAG_BLUE:
			return "tag-blue";
		case TAG_VIOLET:
			return "tag-violet";
		case TAG_YELLOW:
			return "tag-yellow";
		}
		return null;
	}

	public static Type forValue(String value) throws IOException {
		if (value.equals("tag-blue"))
			return TAG_BLUE;
		if (value.equals("tag-violet"))
			return TAG_VIOLET;
		if (value.equals("tag-yellow"))
			return TAG_YELLOW;
		throw new IOException("Cannot deserialize Type");
	}
}
