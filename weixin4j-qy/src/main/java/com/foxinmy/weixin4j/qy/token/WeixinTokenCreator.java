package com.foxinmy.weixin4j.qy.token;

import com.alibaba.fastjson.TypeReference;
import com.foxinmy.weixin4j.exception.WeixinException;
import com.foxinmy.weixin4j.http.weixin.WeixinHttpClient;
import com.foxinmy.weixin4j.http.weixin.WeixinResponse;
import com.foxinmy.weixin4j.model.Consts;
import com.foxinmy.weixin4j.model.Token;
import com.foxinmy.weixin4j.model.WeixinAccount;
import com.foxinmy.weixin4j.token.TokenCreator;
import com.foxinmy.weixin4j.util.ConfigUtil;

/**
 * 微信企业号TOKEN创建
 * 
 * @className WeixinTokenCreator
 * @author jy
 * @date 2015年1月10日
 * @since JDK 1.7
 * @see <a
 *      href="http://qydev.weixin.qq.com/wiki/index.php?title=%E4%B8%BB%E5%8A%A8%E8%B0%83%E7%94%A8">微信企业号获取token说明</a>
 * @see com.foxinmy.weixin4j.model.Token
 */
public class WeixinTokenCreator implements TokenCreator {

	private final WeixinHttpClient httpClient;
	private final String corpid;
	private final String corpsecret;

	public WeixinTokenCreator() {
		this(ConfigUtil.getWeixinAccount());
	}

	public WeixinTokenCreator(WeixinAccount weixinAccount) {
		this(weixinAccount.getId(), weixinAccount.getSecret());
	}

	public WeixinTokenCreator(String corpid, String corpsecret) {
		this.corpid = corpid;
		this.corpsecret = corpsecret;
		this.httpClient = new WeixinHttpClient();
	}

	@Override
	public String getCacheKey() {
		return String.format("qy_token_%s", corpid);
	}

	@Override
	public Token createToken() throws WeixinException {
		String tokenUrl = String.format(Consts.QY_ASSESS_TOKEN_URL, corpid,
				corpsecret);
		WeixinResponse response = httpClient.get(tokenUrl);
		Token token = response.getAsObject(new TypeReference<Token>() {
		});
		token.setTime(System.currentTimeMillis());
		return token;
	}
}
