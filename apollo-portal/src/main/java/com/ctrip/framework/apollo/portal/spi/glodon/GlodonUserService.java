package com.ctrip.framework.apollo.portal.spi.glodon;

import com.ctrip.framework.apollo.portal.entity.bo.UserInfo;
import com.ctrip.framework.apollo.portal.spi.UserService;

import java.util.List;

/**
 * Created by ZhaoYun on 2019-06-06
 **/
public final class GlodonUserService implements UserService {

    @Override
    public List<UserInfo> searchUsers(String keyword, int offset, int limit) {
        return null;
    }

    @Override
    public UserInfo findByUserId(String userId) {
        UserInfo userInfo = new UserInfo(userId);
        userInfo.setEmail(userId + "@glodon.com");
        userInfo.setName(userId);
        return userInfo;
    }

    @Override
    public List<UserInfo> findByUserIds(List<String> userIds) {
        return null;
    }
}
