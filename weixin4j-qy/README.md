weixin4j-qy
===========

[微信企业号](http://qydev.weixin.qq.com/wiki/index.php)开发工具包
---------------------------------------------------------------

功能列表
-------

  * PartyApi `部门管理API`
	
  * UserApi `成员管理API`
  
  * TagApi `标签管理API`
  
  * MediaApi `媒体素材API`
  
  * MenuApi `菜单管理API`
  
  * NotifyApi `消息发送API`
  
  * AgentApi `应用设置API`
  
  * BatchApi `批量操作API`
  
  * OauthApi `oauth授权登陆API`

如何使用
--------
0.maven依赖(1.5.0,2015-06-10 released)

	<dependency>
	    <groupId>com.foxinmy</groupId>
	    <artifactId>weixin4j-qy</artifactId>
	    <version>1.5.0</version>
	</dependency>
1.需新增或拷贝`weixin4j.properties`文件到项目的`classpath`中

weixin4j.properties说明

| 属性名       |       说明      |
| :---------- | :-------------- |
| account     | 微信企业号信息 `json格式`  |
| token_path  | 使用FileTokenStorager时token保存的物理路径 |
| media_path  | 调用媒体接口时保存媒体文件的物理路径 |
| redirect_uri     | 调用OauthApi接口时需要填写的重定向路径 |

示例(properties中换行用右斜杆\\)

	account={"id":"corpid","secret":"corpsecret",\
		"token":"企业号中应用在回调模式下的token",\
		"encodingAesKey":"企业号中应用在回调模式下AES加密密钥",\
		"providerSecret:"提供商的secret"}
	
	token_path=/tmp/weixin4j/token
	media_path=/tmp/weixin4j/media
	
	#企业号登陆授权的重定向路径(使用OauthApi时需要填写)
	redirect_uri=http://xxx

2.实例化一个`WeixinProxy`对象,调用API

    WeixinProxy weixinProxy = new WeixinProxy();
    // weixinProxy = new WeixinProxy(corpid,corpsecret);
    // weixinProxy = new WeixinProxy(weixinAccount);
    weixinProxy.getUser(userid);

> 针对`token`存储有两种方案,`File存储`/`Redis存储`,当然也可自己实现`TokenStorager`,默认使用文件(xml)的方式保存token,如果环境中支持`redis`,建议使用[RedisTokenStorager](https://github.com/foxinmy/weixin4j/wiki/%E7%94%A8redis%E4%BF%9D%E5%AD%98token).

>   WeixinProxy weixinProxy = new WeixinProxy(new RedisTokenStorager());

>   // weixinProxy = new WeixinProxy(new RedisTokenStorager(weixinAccount));

[更新LOG](./CHANGE.md)
----------------------