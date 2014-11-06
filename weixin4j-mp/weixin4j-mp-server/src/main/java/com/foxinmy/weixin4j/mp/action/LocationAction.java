package com.foxinmy.weixin4j.mp.action;

import com.foxinmy.weixin4j.mp.mapping.Action;
import com.foxinmy.weixin4j.msg.LocationMessage;
import com.foxinmy.weixin4j.type.MessageType;

/**
 * 地理位置处理
 * 
 * @className LocationAction
 * @author jy
 * @date 2014年10月9日
 * @since JDK 1.7
 * @see com.foxinmy.weixin4j.msg.LocationMessage
 */
@Action(msgType = MessageType.location)
public class LocationAction extends DebugAction<LocationMessage> {

}