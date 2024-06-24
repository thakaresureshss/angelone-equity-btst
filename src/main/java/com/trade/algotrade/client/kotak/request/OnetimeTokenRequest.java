package com.trade.algotrade.client.kotak.request;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
public class OnetimeTokenRequest extends TwoFactorAuthRequest {
	private String password;

	public OnetimeTokenRequest(String userid, String password) {
		super(userid);
		this.password = password;
	}

}