package com.trade.algotrade.client.angelone.utils;

public class Utils {
	private Utils() {
		
	}

	public static boolean isEmpty(final Integer nm) {
		return nm == null || nm.equals(0);
	}

	public static boolean areCharArraysEqual(char[] a, char[] b) {
		if (a == null && b == null) {
			return true;
		}

		if (a != null && b != null) {
			if (a.length == b.length) {
				for (int i = 0; i < a.length; i++) {
					if (a[i] != b[i]) {
						return false;
					}
				}
				return true;
			}
		}
		return false;
	}
	
	public static boolean areByteArraysEqual(byte[] a, byte[] b) {
		if (a == null && b == null) {
			return true;
		}

		if (a != null && b != null) {
			if (a.length == b.length) {
				for (int i = 0; i < a.length; i++) {
					if (a[i] != b[i]) {
						return false;
					}
				}
				return true;
			}
		}
		return false;
	}
	
	public static <T> boolean validateInputNullCheck(T input) {
        return input == null;
    }

    public static <T> boolean validateInputNotNullCheck(T input) {
        return input != null;
    }

}
