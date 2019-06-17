package com.ctrip.framework.apollo.portal.spi.glodon;

import com.ctrip.framework.apollo.portal.entity.bo.UserInfo;
import com.ctrip.framework.apollo.portal.spi.UserInfoHolder;
import com.glodon.digiarch.paas.foundation.proxy.PlatformFoundationServiceClient;
import com.glodon.paas.shiro.utils.ShiroUtil;
import org.springframework.beans.factory.annotation.Autowired;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by ZhaoYun on 2019-06-06
 **/
public final class GlodonUserInfoHolder implements UserInfoHolder {

    @Autowired
    private HttpServletRequest request;

    @Override
    public UserInfo getUser() {
        String userName = ShiroUtil.getUserName(request);
        UserInfo userInfo = new UserInfo(userName);
        userInfo.setUserId(userName);
        userInfo.setEmail(userName + "@glodon.com");
        return userInfo;
    }
}
