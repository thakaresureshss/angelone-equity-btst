package com.trade.algotrade.client.angelone;

import org.apache.commons.codec.binary.Base32;
import org.apache.commons.codec.binary.Hex;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.util.stream.IntStream;

public class TOTPUtility {

    public static void main(String[] args) {
        // System.out.println(generateTOTP("VUYQ36S3IGBI5LQ3ZUTK7JOIXU"));
        System.out.println(getTOTPCode("VUYQ36S3IGBI5LQ3ZUTK7JOIXU"));
    }

    public static String getTOTPCode(String secretKey) {
        Base32 base32 = new Base32();
        byte[] bytes = base32.decode(secretKey);
        String hexKey = Hex.encodeHexString(bytes);
        return de.taimos.totp.TOTP.getOTP(hexKey);
    }
}
