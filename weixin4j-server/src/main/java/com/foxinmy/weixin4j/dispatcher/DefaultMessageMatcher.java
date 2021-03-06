package com.foxinmy.weixin4j.dispatcher;

import java.util.HashMap;
import java.util.Map;

import com.foxinmy.weixin4j.message.ImageMessage;
import com.foxinmy.weixin4j.message.LinkMessage;
import com.foxinmy.weixin4j.message.LocationMessage;
import com.foxinmy.weixin4j.message.TextMessage;
import com.foxinmy.weixin4j.message.VideoMessage;
import com.foxinmy.weixin4j.message.VoiceMessage;
import com.foxinmy.weixin4j.message.event.LocationEventMessage;
import com.foxinmy.weixin4j.message.event.MenuEventMessage;
import com.foxinmy.weixin4j.message.event.MenuLocationEventMessage;
import com.foxinmy.weixin4j.message.event.MenuPhotoEventMessage;
import com.foxinmy.weixin4j.message.event.MenuScanEventMessage;
import com.foxinmy.weixin4j.mp.event.KfCloseEventMessage;
import com.foxinmy.weixin4j.mp.event.KfCreateEventMessage;
import com.foxinmy.weixin4j.mp.event.KfSwitchEventMessage;
import com.foxinmy.weixin4j.mp.event.MassEventMessage;
import com.foxinmy.weixin4j.mp.event.TemplatesendjobfinishMessage;
import com.foxinmy.weixin4j.qy.event.BatchjobresultMessage;
import com.foxinmy.weixin4j.qy.event.EnterAgentEventMessage;
import com.foxinmy.weixin4j.type.AccountType;
import com.foxinmy.weixin4j.type.EventType;
import com.foxinmy.weixin4j.type.MessageType;

/**
 * 默认MessageMatcher实现(可以改进)
 * 
 * @className DefaultMessageMatcher
 * @author jy
 * @date 2015年6月10日
 * @since JDK 1.7
 * @see
 */
public class DefaultMessageMatcher implements WeixinMessageMatcher {

	private final Map<MessageKey, Class<?>> messageClassMap;

	public DefaultMessageMatcher() {
		messageClassMap = new HashMap<MessageKey, Class<?>>();
		initMessageClass();
	}

	private void initMessageClass() {
		// /////////////////////////////////////////////////
		/******************** 普通消息 ********************/
		// /////////////////////////////////////////////////
		initGeneralMessageClass();
		// /////////////////////////////////////////////////
		/******************** 事件消息 ********************/
		// /////////////////////////////////////////////////
		initEventMessageClass();
		// /////////////////////////////////////////////////
		/***************** 公众平台事件消息 *****************/
		// /////////////////////////////////////////////////
		initMpEventMessageClass();
		// /////////////////////////////////////////////////
		/****************** 企业号事件消息 ******************/
		// /////////////////////////////////////////////////
		initQyEventMessageClass();
	}

	private void initGeneralMessageClass() {
		for (AccountType accountType : AccountType.values()) {
			messageClassMap.put(new MessageKey(MessageType.text.name(), null,
					accountType), TextMessage.class);
			messageClassMap.put(new MessageKey(MessageType.image.name(), null,
					accountType), ImageMessage.class);
			messageClassMap.put(new MessageKey(MessageType.voice.name(), null,
					accountType), VoiceMessage.class);
			messageClassMap.put(new MessageKey(MessageType.video.name(), null,
					accountType), VideoMessage.class);
			messageClassMap.put(new MessageKey(MessageType.shortvideo.name(),
					null, accountType), VideoMessage.class);
			messageClassMap.put(new MessageKey(MessageType.location.name(),
					null, accountType), LocationMessage.class);
			messageClassMap.put(new MessageKey(MessageType.link.name(), null,
					accountType), LinkMessage.class);
		}
	}

	private void initEventMessageClass() {
		String messageType = MessageType.event.name();
		EventType[] eventTypes = new EventType[] { EventType.subscribe,
				EventType.unsubscribe };
		for (EventType eventType : eventTypes) {
			messageClassMap.put(new MessageKey(messageType, eventType.name(),
					AccountType.MP),
					com.foxinmy.weixin4j.mp.event.ScribeEventMessage.class);
		}
		for (EventType eventType : eventTypes) {
			messageClassMap.put(new MessageKey(messageType, eventType.name(),
					AccountType.QY),
					com.foxinmy.weixin4j.qy.event.ScribeEventMessage.class);
		}
		for (AccountType accountType : AccountType.values()) {
			messageClassMap.put(
					new MessageKey(messageType, EventType.location.name(),
							accountType), LocationEventMessage.class);
			messageClassMap.put(new MessageKey(messageType,
					EventType.location_select.name(), accountType),
					MenuLocationEventMessage.class);
			for (EventType eventType : new EventType[] { EventType.click,
					EventType.view }) {
				messageClassMap.put(
						new MessageKey(messageType, eventType.name(),
								accountType), MenuEventMessage.class);
			}
			for (EventType eventType : new EventType[] {
					EventType.scancode_push, EventType.scancode_waitmsg }) {
				messageClassMap.put(
						new MessageKey(messageType, eventType.name(),
								accountType), MenuScanEventMessage.class);
			}
			for (EventType eventType : new EventType[] {
					EventType.pic_sysphoto, EventType.pic_photo_or_album,
					EventType.pic_weixin }) {
				messageClassMap.put(
						new MessageKey(messageType, eventType.name(),
								accountType), MenuPhotoEventMessage.class);
			}
		}
	}

	private void initMpEventMessageClass() {
		String messageType = MessageType.event.name();
		AccountType accountType = AccountType.MP;
		messageClassMap.put(new MessageKey(messageType, EventType.scan.name(),
				accountType),
				com.foxinmy.weixin4j.mp.event.ScanEventMessage.class);
		messageClassMap.put(new MessageKey(messageType,
				EventType.masssendjobfinish.name(), accountType),
				MassEventMessage.class);
		messageClassMap.put(new MessageKey(messageType,
				EventType.templatesendjobfinish.name(), accountType),
				TemplatesendjobfinishMessage.class);
		messageClassMap.put(new MessageKey(messageType,
				EventType.kf_create_session.name(), accountType),
				KfCreateEventMessage.class);
		messageClassMap.put(new MessageKey(messageType,
				EventType.kf_close_session.name(), accountType),
				KfCloseEventMessage.class);
		messageClassMap.put(new MessageKey(messageType,
				EventType.kf_switch_session.name(), accountType),
				KfSwitchEventMessage.class);
	}

	private void initQyEventMessageClass() {
		String messageType = MessageType.event.name();
		AccountType accountType = AccountType.QY;
		messageClassMap.put(new MessageKey(messageType,
				EventType.batch_job_result.name(), accountType),
				BatchjobresultMessage.class);
		messageClassMap.put(
				new MessageKey(messageType, EventType.enter_agent.name(),
						accountType), EnterAgentEventMessage.class);
	}

	@Override
	public Class<?> match(MessageKey messageKey) {
		return messageClassMap.get(messageKey);
	}

	@Override
	public void regist(MessageKey messageKey, Class<?> messageClass) {
		Class<?> clazz = messageClassMap.get(messageKey);
		if (clazz != null) {
			throw new IllegalArgumentException("duplicate messagekey '"
					+ messageKey + "' define for " + clazz);
		}
		messageClassMap.put(messageKey, messageClass);
	}
}
