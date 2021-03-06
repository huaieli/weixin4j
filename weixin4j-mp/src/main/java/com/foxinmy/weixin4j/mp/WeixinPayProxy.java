package com.foxinmy.weixin4j.mp;

import java.io.File;
import java.util.Date;

import com.alibaba.fastjson.JSON;
import com.foxinmy.weixin4j.exception.WeixinException;
import com.foxinmy.weixin4j.http.weixin.JsonResult;
import com.foxinmy.weixin4j.http.weixin.XmlResult;
import com.foxinmy.weixin4j.mp.api.CashApi;
import com.foxinmy.weixin4j.mp.api.CouponApi;
import com.foxinmy.weixin4j.mp.api.Pay2Api;
import com.foxinmy.weixin4j.mp.api.Pay3Api;
import com.foxinmy.weixin4j.mp.api.PayApi;
import com.foxinmy.weixin4j.mp.model.WeixinMpAccount;
import com.foxinmy.weixin4j.mp.payment.coupon.CouponDetail;
import com.foxinmy.weixin4j.mp.payment.coupon.CouponResult;
import com.foxinmy.weixin4j.mp.payment.coupon.CouponStock;
import com.foxinmy.weixin4j.mp.payment.v3.ApiResult;
import com.foxinmy.weixin4j.mp.payment.v3.MPPayment;
import com.foxinmy.weixin4j.mp.payment.v3.MPPaymentResult;
import com.foxinmy.weixin4j.mp.payment.v3.Redpacket;
import com.foxinmy.weixin4j.mp.payment.v3.RedpacketRecord;
import com.foxinmy.weixin4j.mp.payment.v3.RedpacketSendResult;
import com.foxinmy.weixin4j.mp.token.WeixinTokenCreator;
import com.foxinmy.weixin4j.mp.type.BillType;
import com.foxinmy.weixin4j.mp.type.CurrencyType;
import com.foxinmy.weixin4j.mp.type.IdQuery;
import com.foxinmy.weixin4j.mp.type.IdType;
import com.foxinmy.weixin4j.mp.type.RefundType;
import com.foxinmy.weixin4j.token.FileTokenStorager;
import com.foxinmy.weixin4j.token.TokenHolder;
import com.foxinmy.weixin4j.token.TokenStorager;
import com.foxinmy.weixin4j.util.ConfigUtil;

/**
 * 微信支付接口实现
 * 
 * @className WeixinPayProxy
 * @author jy
 * @date 2015年1月3日
 * @since JDK 1.7
 * @see com.foxinmy.weixin4j.mp.api.Pay2Api
 * @see com.foxinmy.weixin4j.mp.api.Pay3Api
 * @see <a href="http://pay.weixin.qq.com/wiki/doc/api/index.html">商户平台支付API</a>
 */
public class WeixinPayProxy {

	private final PayApi payApi;
	private final Pay2Api pay2Api;
	private final Pay3Api pay3Api;
	private final CouponApi couponApi;
	private final CashApi cashApi;

	/**
	 * 默认使用文件保存token、使用weixin4j.properties配置的账号信息
	 */
	public WeixinPayProxy() {
		this(new FileTokenStorager());
	}

	/**
	 * 使用weixin4j.properties配置的账号信息
	 */
	public WeixinPayProxy(TokenStorager tokenStorager) {
		this(tokenStorager, JSON.parseObject(ConfigUtil.getValue("account"),
				WeixinMpAccount.class));
	}

	/**
	 * 
	 * @param tokenStorager
	 *            token的存储策略
	 * @param weixinAccount
	 *            公众号账号信息
	 */
	public WeixinPayProxy(TokenStorager tokenStorager,
			WeixinMpAccount weixinAccount) {
		TokenHolder tokenHolder = new TokenHolder(new WeixinTokenCreator(
				weixinAccount), tokenStorager);
		this.pay2Api = new Pay2Api(weixinAccount, tokenHolder);
		this.pay3Api = new Pay3Api(weixinAccount, tokenHolder);
		int version = weixinAccount.getVersion();
		if (version == 2) {
			this.payApi = this.pay2Api;
		} else if (version == 3) {
			this.payApi = this.pay3Api;
		} else {
			this.payApi = this.pay3Api;
		}
		this.couponApi = new CouponApi(weixinAccount);
		this.cashApi = new CashApi(weixinAccount);
	}

	/**
	 * 发货通知
	 * 
	 * @param openId
	 *            用户ID
	 * @param transid
	 *            交易单号
	 * @param outTradeNo
	 *            订单号
	 * @param status
	 *            成功|失败
	 * @param statusMsg
	 *            status为失败时携带的信息
	 * @return 发货处理结果
	 * @since V2 & V3
	 * @see com.foxinmy.weixin4j.mp.api.PayApi
	 * @throws WeixinException
	 */
	public JsonResult deliverNotify(String openId, String transid,
			String outTradeNo, boolean status, String statusMsg)
			throws WeixinException {
		return payApi.deliverNotify(openId, transid, outTradeNo, status,
				statusMsg);
	}

	/**
	 * 维权处理
	 * 
	 * @param openId
	 *            用户ID
	 * @param feedbackId
	 *            维权单号
	 * @return 调用结果
	 * @see com.foxinmy.weixin4j.mp.api.PayApi
	 * @since V2 & V3
	 * @throws WeixinException
	 */
	public JsonResult updateFeedback(String openId, String feedbackId)
			throws WeixinException {
		return payApi.updateFeedback(openId, feedbackId);
	}

	/**
	 * V2订单查询
	 * 
	 * @param idQuery
	 *            商户系统内部的订单号, transaction_id、out_trade_no 二 选一,如果同时存在优先级:
	 *            transaction_id> out_trade_no
	 * @since V2
	 * @see com.foxinmy.weixin4j.mp.payment.v2.Order
	 * @see com.foxinmy.weixin4j.mp.api.PayApi
	 * @see com.foxinmy.weixin4j.mp.api.Pay2Api
	 * @return 订单详情
	 * @throws WeixinException
	 */
	public com.foxinmy.weixin4j.mp.payment.v2.Order orderQueryV2(
			String outTradeNo) throws WeixinException {
		return pay2Api.orderQuery(new IdQuery(outTradeNo, IdType.TRADENO));
	}

	/**
	 * V3订单查询
	 * <p>
	 * 当商户后台、网络、服务器等出现异常，商户系统最终未接收到支付通知；</br> 调用支付接口后，返回系统错误或未知交易状态情况；</br>
	 * 调用被扫支付API，返回USERPAYING的状态；</br> 调用关单或撤销接口API之前，需确认支付状态；
	 * </P>
	 * 
	 * @param idQuery
	 *            商户系统内部的订单号, transaction_id、out_trade_no 二 选一,如果同时存在优先级:
	 *            transaction_id> out_trade_no
	 * @since V3
	 * @see com.foxinmy.weixin4j.mp.payment.v3.Order
	 * @see com.foxinmy.weixin4j.mp.api.PayApi
	 * @see com.foxinmy.weixin4j.mp.api.Pay3Api
	 * @see <a
	 *      href="http://pay.weixin.qq.com/wiki/doc/api/jsapi.php?chapter=9_2">订单查询API</a>
	 * @return 订单详情
	 * @throws WeixinException
	 */
	public com.foxinmy.weixin4j.mp.payment.v3.Order orderQueryV3(IdQuery idQuery)
			throws WeixinException {
		return pay3Api.orderQuery(idQuery);
	}

	/**
	 * V2申请退款(请求需要双向证书)</br>
	 * <p style="color:red">
	 * 交易时间超过 1 年的订单无法提交退款; </br> 支持部分退款,部分退需要设置相同的订单号和不同的 out_refund_no。一笔退款失
	 * 败后重新提交,要采用原来的 out_refund_no。总退款金额不能超过用户实际支付金额。</br>
	 * </p>
	 * 
	 * @param caFile
	 *            证书文件(后缀为*.pfx)
	 * @param idQuery
	 *            ) 商户系统内部的订单号, transaction_id 、 out_trade_no 二选一,如果同时存在优先级:
	 *            transaction_id> out_trade_no
	 * @param outRefundNo
	 *            商户系统内部的退款单号,商 户系统内部唯一,同一退款单号多次请求只退一笔
	 * @param totalFee
	 *            订单总金额,单位为元
	 * @param refundFee
	 *            退款总金额,单位为元,可以做部分退款
	 * @param opUserId
	 *            操作员帐号, 默认为商户号
	 * @param opUserPasswd
	 *            操作员密码
	 * 
	 * @return 退款申请结果
	 * @see com.foxinmy.weixin4j.mp.api.PayApi
	 * @see com.foxinmy.weixin4j.mp.api.Pay2Api
	 * @see com.foxinmy.weixin4j.mp.payment.v2.RefundResult
	 * @since V2
	 * @throws WeixinException
	 */
	public com.foxinmy.weixin4j.mp.payment.v2.RefundResult refundV2(
			File caFile, IdQuery idQuery, String outRefundNo, double totalFee,
			double refundFee, String opUserId, String opUserPasswd)
			throws WeixinException {
		return pay2Api.refund(caFile, idQuery, outRefundNo, totalFee,
				refundFee, opUserId, opUserPasswd);
	}

	/**
	 * V2退款申请采用properties中配置的ca文件
	 * 
	 * @see {@link com.foxinmy.weixin4j.mp.WeixinPayProxy#refundV2(File, IdQuery, String, double, double, String,String)}
	 */
	public com.foxinmy.weixin4j.mp.payment.v2.RefundResult refundV2(
			IdQuery idQuery, String outRefundNo, double totalFee,
			double refundFee, String opUserId, String opUserPasswd)
			throws WeixinException {
		File caFile = new File(ConfigUtil.getClassPathValue("ca_file"));
		return refundV2(caFile, idQuery, outRefundNo, totalFee, refundFee,
				opUserId, opUserPasswd);
	}

	/**
	 * V2退款申请
	 * 
	 * @param caFile
	 *            证书文件(V2版本后缀为*.pfx)
	 * @param idQuery
	 *            商户系统内部的订单号, transaction_id 、 out_trade_no 二选一,如果同时存在优先级:
	 *            transaction_id> out_trade_no
	 * @param outRefundNo
	 *            商户系统内部的退款单号,商 户系统内部唯一,同一退款单号多次请求只退一笔
	 * @param totalFee
	 *            订单总金额,单位为元
	 * @param refundFee
	 *            退款总金额,单位为元,可以做部分退款
	 * @param opUserId
	 *            操作员帐号, 默认为商户号
	 * @param opUserPasswd
	 *            操作员密码,默认为商户后台登录密码
	 * @param recvUserId
	 *            转账退款接收退款的财付通帐号。 一般无需填写,只有退银行失败,资金转入商 户号现金账号时(即状态为转入代发,查询返 回的
	 *            refund_status 是 7 或 11),填写原退款 单号并填写此字段,资金才会退到指定财付通
	 *            账号。其他情况此字段忽略
	 * @param reccvUserName
	 *            转账退款接收退款的姓名(需与接收退款的财 付通帐号绑定的姓名一致)
	 * @param refundType
	 *            为空或者填 1:商户号余额退款;2:现金帐号 退款;3:优先商户号退款,若商户号余额不足, 再做现金帐号退款。使用 2 或
	 *            3 时,需联系财 付通开通此功能
	 * @see com.foxinmy.weixin4j.mp.api.PayApi
	 * @see com.foxinmy.weixin4j.mp.api.Pay2Api
	 * @see com.foxinmy.weixin4j.mp.payment.v2.RefundResult
	 * @return 退款结果
	 */
	public com.foxinmy.weixin4j.mp.payment.v2.RefundResult refundV2(
			File caFile, IdQuery idQuery, String outRefundNo, double totalFee,
			double refundFee, String opUserId, String opUserPasswd,
			String recvUserId, String reccvUserName, RefundType refundType)
			throws WeixinException {
		return pay2Api.refund(caFile, idQuery, outRefundNo, totalFee,
				refundFee, opUserId, opUserPasswd, recvUserId, reccvUserName,
				refundType);
	}

	/**
	 * V2退款查询</br> 退款有一定延时,用零钱支付的退款20分钟内到账,银行卡支付的退款 3 个工作日后重新查询退款状态
	 * 
	 * @param idQuery
	 *            单号 refund_id、out_refund_no、 out_trade_no 、 transaction_id
	 *            四个参数必填一个,优先级为:
	 *            refund_id>out_refund_no>transaction_id>out_trade_no
	 * @return 退款记录
	 * @see com.foxinmy.weixin4j.mp.payment.v2.RefundRecord
	 * @see com.foxinmy.weixin4j.mp.api.PayApi
	 * @see com.foxinmy.weixin4j.mp.api.Pay2Api
	 * @since V2
	 * @throws WeixinException
	 */
	public com.foxinmy.weixin4j.mp.payment.v2.RefundRecord refundQueryV2(
			IdQuery idQuery) throws WeixinException {
		return pay2Api.refundQuery(idQuery);
	}

	/**
	 * V3申请退款(请求需要双向证书)</br>
	 * <p>
	 * 当交易发生之后一段时间内，由于买家或者卖家的原因需要退款时，卖家可以通过退款接口将支付款退还给买家，微信支付将在收到退款请求并且验证成功之后，
	 * 按照退款规则将支付款按原路退到买家帐号上。
	 * </p>
	 * <p style="color:red">
	 * 1.交易时间超过半年的订单无法提交退款；
	 * 2.微信支付退款支持单笔交易分多次退款，多次退款需要提交原支付订单的商户订单号和设置不同的退款单号。一笔退款失败后重新提交
	 * ，要采用原来的退款单号。总退款金额不能超过用户实际支付金额。
	 * </p>
	 * 
	 * @param caFile
	 *            证书文件(后缀为*.p12)
	 * @param idQuery
	 *            商户系统内部的订单号, transaction_id 、 out_trade_no 二选一,如果同时存在优先级:
	 *            transaction_id> out_trade_no
	 * @param outRefundNo
	 *            商户系统内部的退款单号,商 户系统内部唯一,同一退款单号多次请求只退一笔
	 * @param totalFee
	 *            订单总金额,单位为元
	 * @param refundFee
	 *            退款总金额,单位为元,可以做部分退款
	 * @param refundFeeType
	 *            货币类型，符合ISO 4217标准的三位字母代码，默认人民币：CNY
	 * @param opUserId
	 *            操作员帐号, 默认为商户号
	 * 
	 * @return 退款申请结果
	 * @see com.foxinmy.weixin4j.mp.payment.v3.RefundResult
	 * @see com.foxinmy.weixin4j.mp.api.PayApi
	 * @see com.foxinmy.weixin4j.mp.api.Pay3Api
	 * @see <a
	 *      href="http://pay.weixin.qq.com/wiki/doc/api/jsapi.php?chapter=9_4">申请退款API</a>
	 * @since V3
	 * @throws WeixinException
	 */
	public com.foxinmy.weixin4j.mp.payment.v3.RefundResult refundV3(
			File caFile, IdQuery idQuery, String outRefundNo, double totalFee,
			double refundFee, CurrencyType refundFeeType, String opUserId)
			throws WeixinException {
		return pay3Api.refund(caFile, idQuery, outRefundNo, totalFee,
				refundFee, refundFeeType, opUserId);
	}

	/**
	 * V3退款申请采用properties中配置的ca文件
	 * 
	 * @see {@link com.foxinmy.weixin4j.mp.WeixinPayProxy#refundV3(File, IdQuery, String, double, double,CurrencyType, String)}
	 */
	public com.foxinmy.weixin4j.mp.payment.v3.RefundResult refundV3(
			IdQuery idQuery, String outRefundNo, double totalFee,
			double refundFee, String opUserId) throws WeixinException {
		File caFile = new File(ConfigUtil.getClassPathValue("ca_file"));
		return pay3Api.refund(caFile, idQuery, outRefundNo, totalFee,
				refundFee, CurrencyType.CNY, opUserId);
	}

	/**
	 * V3退款查询
	 * <p>
	 * 提交退款申请后，通过调用该接口查询退款状态。退款有一定延时，用零钱支付的退款20分钟内到账，银行卡支付的退款3个工作日后重新查询退款状态。
	 * </p>
	 * 
	 * @param idQuery
	 *            单号 refund_id、out_refund_no、 out_trade_no 、 transaction_id
	 *            四个参数必填一个,优先级为:
	 *            refund_id>out_refund_no>transaction_id>out_trade_no
	 * @return 退款记录
	 * @see com.foxinmy.weixin4j.mp.api.PayApi
	 * @see com.foxinmy.weixin4j.mp.api.Pay3Api
	 * @see com.foxinmy.weixin4j.mp.payment.v3.RefundRecord
	 * @see <a
	 *      href="http://pay.weixin.qq.com/wiki/doc/api/jsapi.php?chapter=9_5">退款查询API</a>
	 * @since V3
	 * @throws WeixinException
	 */
	public com.foxinmy.weixin4j.mp.payment.v3.RefundRecord refundQueryV3(
			IdQuery idQuery) throws WeixinException {
		return pay3Api.refundQuery(idQuery);
	}

	/**
	 * 下载对账单<br>
	 * 1.微信侧未成功下单的交易不会出现在对账单中。支付成功后撤销的交易会出现在对账 单中,跟原支付单订单号一致,bill_type 为
	 * REVOKED;<br>
	 * 2.微信在次日 9 点启动生成前一天的对账单,建议商户 9 点半后再获取;<br>
	 * 3.对账单中涉及金额的字段单位为“元”。<br>
	 * 
	 * @param billDate
	 *            下载对账单的日期
	 * @param billType
	 *            下载对账单的类型 ALL,返回当日所有订单信息, 默认值 SUCCESS,返回当日成功支付的订单
	 *            REFUND,返回当日退款订单
	 * @return excel表格
	 * @since V2 & V3
	 * @see com.foxinmy.weixin4j.mp.api.PayApi
	 * @see <a
	 *      href="http://pay.weixin.qq.com/wiki/doc/api/jsapi.php?chapter=9_6">下载对账单API</a>
	 * @throws WeixinException
	 */
	public File downloadbill(Date billDate, BillType billType)
			throws WeixinException {
		return payApi.downloadbill(billDate, billType);
	}

	/**
	 * 冲正订单(需要证书)</br> 当支付返回失败,或收银系统超时需要取消交易,可以调用该接口</br> 接口逻辑:支
	 * 付失败的关单,支付成功的撤销支付</br> <font color="red">7天以内的单可撤销,其他正常支付的单
	 * 如需实现相同功能请调用退款接口</font></br> <font
	 * color="red">调用扣款接口后请勿立即调用撤销,需要等待5秒以上。先调用查单接口,如果没有确切的返回,再调用撤销</font></br>
	 * 
	 * @param ca
	 *            证书文件(V2版本后缀为*.pfx,V3版本后缀为*.p12)
	 * @param idQuery
	 *            商户系统内部的订单号, transaction_id 、 out_trade_no 二选一,如果同时存在优先级:
	 *            transaction_id> out_trade_no
	 * @return 撤销结果
	 * @see com.foxinmy.weixin4j.mp.api.PayApi
	 * @see com.foxinmy.weixin4j.mp.api.Pay2Api
	 * @see com.foxinmy.weixin4j.mp.api.Pay3Api
	 * @since V3
	 * @throws WeixinException
	 */
	public ApiResult reverse(File caFile, IdQuery idQuery)
			throws WeixinException {
		return payApi.reverse(caFile, idQuery);
	}

	/**
	 * 冲正撤销:默认采用properties中配置的ca文件
	 * 
	 * @param idQuery
	 *            transaction_id、out_trade_no 二选一
	 * @return 撤销结果
	 * @see {@link com.foxinmy.weixin4j.mp.WeixinProxy#reverse(File, IdQuery)}
	 * @throws WeixinException
	 */
	public ApiResult reverse(IdQuery idQuery) throws WeixinException {
		File caFile = new File(ConfigUtil.getClassPathValue("ca_file"));
		return payApi.reverse(caFile, idQuery);
	}

	/**
	 * 关闭订单
	 * <p>
	 * 商户订单支付失败需要生成新单号重新发起支付，要对原订单号调用关单，避免重复支付；系统下单后，用户支付超时，系统退出不再受理，避免用户继续
	 * ，请调用关单接口,如果关单失败,返回已完 成支付请按正常支付处理。如果出现银行掉单,调用关单成功后,微信后台会主动发起退款。
	 * </p>
	 * 
	 * @param outTradeNo
	 *            商户系统内部的订单号
	 * @return 执行结果
	 * @see com.foxinmy.weixin4j.mp.api.PayApi
	 * @see com.foxinmy.weixin4j.mp.api.Pay3Api
	 * @since V3
	 * @throws WeixinException
	 * @see <a
	 *      href="http://pay.weixin.qq.com/wiki/doc/api/jsapi.php?chapter=9_3">关闭订单API</a>
	 */
	public ApiResult closeOrder(String outTradeNo) throws WeixinException {
		return payApi.closeOrder(outTradeNo);
	}

	/**
	 * native支付URL转短链接:用于扫码原生支付模式一中的二维码链接转成短链接(weixin://wxpay/s/XXXXXX)，减小二维码数据量
	 * ，提升扫描速度和精确度。
	 * 
	 * @param url
	 *            具有native标识的支付URL
	 * @return 转换后的短链接
	 * @see com.foxinmy.weixin4j.mp.api.PayApi
	 * @see com.foxinmy.weixin4j.mp.api.Pay2Api
	 * @see com.foxinmy.weixin4j.mp.api.Pay3Api
	 * @see <a
	 *      href="http://pay.weixin.qq.com/wiki/doc/api/jsapi.php?chapter=9_9">转换短链接API</a>
	 * @since V2 & V3
	 * @throws WeixinException
	 */
	public String getPayShorturl(String url) throws WeixinException {
		return payApi.getShorturl(url);
	}

	/**
	 * 接口上报
	 * 
	 * @param interfaceUrl
	 *            上报对应的接口的完整 URL, 类似: https://api.mch.weixin.q
	 *            q.com/pay/unifiedorder
	 * @param executeTime
	 *            接口耗时情况,单位为毫秒
	 * @param outTradeNo
	 *            商户系统内部的订单号,商 户可以在上报时提供相关商户订单号方便微信支付更好 的提高服务质量。
	 * @param ip
	 *            发起接口调用时的机器 IP
	 * @param time
	 *            ￼商户调用该接口时商户自己 系统的时间
	 * @param returnXml
	 *            调用接口返回的基本数据
	 * @return 处理结果
	 * @see com.foxinmy.weixin4j.mp.api.PayApi
	 * @see com.foxinmy.weixin4j.mp.api.Pay3Api
	 * @see <a
	 *      href="http://pay.weixin.qq.com/wiki/doc/api/jsapi.php?chapter=9_8">接口测试上报API</a>
	 * @throws WeixinException
	 */
	public XmlResult interfaceReport(String interfaceUrl, int executeTime,
			String outTradeNo, String ip, Date time, XmlResult returnXml)
			throws WeixinException {
		return pay3Api.interfaceReport(interfaceUrl, executeTime, outTradeNo,
				ip, time, returnXml);
	}

	/**
	 * 发放代金券(需要证书)
	 * 
	 * @param caFile
	 *            证书文件(后缀为*.p12)
	 * @param couponStockId
	 *            代金券批次id
	 * @param partnerTradeNo
	 *            商户发放凭据号（格式：商户id+日期+流水号），商户侧需保持唯一性
	 * @param openId
	 *            用户的openid
	 * @param opUserId
	 *            操作员帐号, 默认为商户号 可在商户平台配置操作员对应的api权限 可为空
	 * @return 发放结果
	 * @see com.foxinmy.weixin4j.mp.api.CouponApi
	 * @see com.foxinmy.weixin4j.mp.payment.coupon.CouponResult
	 * @see <a
	 *      href="http://pay.weixin.qq.com/wiki/doc/api/sp_coupon.php?chapter=12_3">发放代金券接口</a>
	 * @throws WeixinException
	 */
	public CouponResult sendCoupon(File caFile, String couponStockId,
			String partnerTradeNo, String openId, String opUserId)
			throws WeixinException {
		return couponApi.sendCoupon(caFile, couponStockId, partnerTradeNo,
				openId, opUserId);
	}

	/**
	 * 发放代金券采用properties中配置的ca文件
	 * 
	 * @see {@link com.foxinmy.weixin4j.mp.WeixinPayProxy#sendCoupon(File, String, String, String, String)}
	 */
	public CouponResult sendCoupon(String couponStockId, String partnerTradeNo,
			String openId) throws WeixinException {
		File caFile = new File(ConfigUtil.getClassPathValue("ca_file"));
		return couponApi.sendCoupon(caFile, couponStockId, partnerTradeNo,
				openId, null);
	}

	/**
	 * 查询代金券批次
	 * 
	 * @param couponStockId
	 *            代金券批次ID
	 * @return 代金券批次信息
	 * @see com.foxinmy.weixin4j.mp.api.CouponApi
	 * @see com.foxinmy.weixin4j.mp.payment.coupon.CouponStock
	 * @see <a
	 *      href="http://pay.weixin.qq.com/wiki/doc/api/sp_coupon.php?chapter=12_4">查询代金券信息</a>
	 * @throws WeixinException
	 */
	public CouponStock queryCouponStock(String couponStockId)
			throws WeixinException {
		return couponApi.queryCouponStock(couponStockId);
	}

	/**
	 * 查询代金券详细
	 * 
	 * @param couponId
	 *            代金券ID
	 * @return 代金券详细信息
	 * @see com.foxinmy.weixin4j.mp.api.CouponApi
	 * @see com.foxinmy.weixin4j.mp.payment.coupon.CouponDetail
	 * @see <a
	 *      href="http://pay.weixin.qq.com/wiki/doc/api/sp_coupon.php?chapter=12_5">查询代金券详细信息</a>
	 * @throws WeixinException
	 */
	public CouponDetail queryCouponDetail(String couponId)
			throws WeixinException {
		return couponApi.queryCouponDetail(couponId);
	}

	/**
	 * 发放红包 企业向微信用户个人发现金红包
	 * 
	 * @param caFile
	 *            证书文件(V3版本后缀为*.p12)
	 * @param redpacket
	 *            红包信息
	 * @return 发放结果
	 * @see com.foxinmy.weixin4j.mp.api.CashApi
	 * @see com.foxinmy.weixin4j.mp.payment.v3.Redpacket
	 * @see com.foxinmy.weixin4j.mp.payment.v3.RedpacketSendResult
	 * @see <a
	 *      href="http://pay.weixin.qq.com/wiki/doc/api/cash_coupon.php?chapter=13_5">红包接口说明</a>
	 * @throws WeixinException
	 */
	public RedpacketSendResult sendRedpack(File caFile, Redpacket redpacket)
			throws WeixinException {
		return cashApi.sendRedpack(caFile, redpacket);
	}

	/**
	 * 发放红包采用properties中配置的ca文件
	 * 
	 * @see {@link com.foxinmy.weixin4j.mp.WeixinPayProxy#sendRedpack(File, Redpacket)}
	 */
	public RedpacketSendResult sendRedpack(Redpacket redpacket)
			throws WeixinException {
		File caFile = new File(ConfigUtil.getClassPathValue("ca_file"));
		return cashApi.sendRedpack(caFile, redpacket);
	}

	/**
	 * 查询红包记录
	 * 
	 * @param caFile
	 *            证书文件(V3版本后缀为*.p12)
	 * @param outTradeNo
	 *            商户发放红包的商户订单号
	 * @return 红包记录
	 * @see com.foxinmy.weixin4j.mp.api.CashApi
	 * @see com.foxinmy.weixin4j.mp.payment.v3.RedpacketRecord
	 * @see <a
	 *      href="http://pay.weixin.qq.com/wiki/doc/api/cash_coupon.php?chapter=13_6">查询红包接口说明</a>
	 * @throws WeixinException
	 */
	public RedpacketRecord queryRedpack(File caFile, String outTradeNo)
			throws WeixinException {
		return cashApi.queryRedpack(caFile, outTradeNo);
	}

	/**
	 * 查询红包采用properties中配置的ca文件
	 * 
	 * @see {@link com.foxinmy.weixin4j.mp.WeixinPayProxy#queryRedpack(File,String)}
	 */
	public RedpacketRecord queryRedpack(String outTradeNo)
			throws WeixinException {
		File caFile = new File(ConfigUtil.getClassPathValue("ca_file"));
		return cashApi.queryRedpack(caFile, outTradeNo);
	}

	/**
	 * 企业付款 实现企业向个人付款，针对部分有开发能力的商户， 提供通过API完成企业付款的功能。 比如目前的保险行业向客户退保、给付、理赔。
	 * 
	 * @param caFile
	 *            证书文件(V3版本后缀为*.p12)
	 * @param mpPayment
	 *            付款信息
	 * @return 付款结果
	 * @see com.foxinmy.weixin4j.mp.api.CashApi
	 * @see com.foxinmy.weixin4j.mp.payment.v3.MPPayment
	 * @see com.foxinmy.weixin4j.mp.payment.v3.MPPaymentResult
	 * @see <a
	 *      href="http://pay.weixin.qq.com/wiki/doc/api/mch_pay.php?chapter=14_1">企业付款</a>
	 * @throws WeixinException
	 */
	public MPPaymentResult mpPayment(File caFile, MPPayment mpPayment)
			throws WeixinException {
		return cashApi.mpPayment(caFile, mpPayment);
	}

	/**
	 * 企业付款采用properties中配置的ca文件
	 * 
	 * @see {@link com.foxinmy.weixin4j.mp.WeixinPayProxy#mpPayment(File, MPPayment)}
	 */
	public MPPaymentResult mpPayment(MPPayment mpPayment)
			throws WeixinException {
		File caFile = new File(ConfigUtil.getClassPathValue("ca_file"));
		return cashApi.mpPayment(caFile, mpPayment);
	}
}
