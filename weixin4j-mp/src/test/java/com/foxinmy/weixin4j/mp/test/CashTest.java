package com.foxinmy.weixin4j.mp.test;

import org.junit.Test;

import com.foxinmy.weixin4j.exception.WeixinException;
import com.foxinmy.weixin4j.mp.payment.v3.MPPayment;
import com.foxinmy.weixin4j.mp.payment.v3.MPPaymentResult;
import com.foxinmy.weixin4j.mp.payment.v3.Redpacket;
import com.foxinmy.weixin4j.mp.payment.v3.RedpacketRecord;
import com.foxinmy.weixin4j.mp.payment.v3.RedpacketSendResult;
import com.foxinmy.weixin4j.mp.type.MPPaymentCheckNameType;

/**
 * 现金发放测试
 * 
 * @className CashTest
 * @author jy
 * @date 2015年4月1日
 * @since JDK 1.7
 * @see
 */
public class CashTest extends CouponTest {

	@Test
	public void sendRedpacket() throws WeixinException {
		Redpacket redpacket = new Redpacket("HB001", "无忧钱庄", "无忧钱庄",
				"oyFLst1bqtuTcxK-ojF8hOGtLQao", 1d);
		redpacket.setActName("红包测试");
		redpacket.setClientIp("127.0.0.1");
		redpacket.setMaxValue(1d);
		redpacket.setMinValue(1d);
		redpacket.setRemark("快来领取红包吧！");
		redpacket.setTotalNum(1);
		redpacket.setWishing("来就送钱");
		RedpacketSendResult result = WEIXINPAY.sendRedpack(caFile, redpacket);
		System.err.println(result);
	}

	@Test
	public void queryRedpacket() throws WeixinException {
		String outTradeNo = "HB001";
		RedpacketRecord record = WEIXINPAY.queryRedpack(caFile, outTradeNo);
		System.err.println(record);
	}

	@Test
	public void mpPayment() throws WeixinException {
		MPPayment payment = new MPPayment("MP001",
				"oyFLst1bqtuTcxK-ojF8hOGtLQao",
				MPPaymentCheckNameType.NO_CHECK, "企业付款测试", 0.01d, "127.0.0.1");
		MPPaymentResult result = WEIXINPAY.mpPayment(caFile, payment);
		System.err.println(result);
	}
}
