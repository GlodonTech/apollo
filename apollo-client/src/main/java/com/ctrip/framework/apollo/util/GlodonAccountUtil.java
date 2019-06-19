package com.ctrip.framework.apollo.util;

import com.ctrip.framework.apollo.build.ApolloInjector;
import com.glodon.paas.foundation.accountCore.AccountConfiguration;
import com.glodon.paas.foundation.accountCore.AccountService;
import com.glodon.paas.foundation.accountCore.service.AccountServiceImpl;
import org.apache.commons.lang3.StringUtils;

/**
 * Created by ZhaoYun on 2019-06-18
 **/
public class GlodonAccountUtil {
    private AccountService accountService;

    public GlodonAccountUtil() {
        ConfigUtil m_configUtil = ApolloInjector.getInstance(ConfigUtil.class);

        String appKey = m_configUtil.getAppKey();
        String appSecret = m_configUtil.getAppSecret();
        String accountPath = m_configUtil.getAccountPath();

        if (StringUtils.isNotEmpty(appKey) && StringUtils.isNotEmpty(appSecret) && StringUtils.isNotEmpty(accountPath)) {
            accountService = new AccountServiceImpl(new AccountConfiguration(null, appKey, appSecret, accountPath, false));
        }
    }

    public boolean needAuth() {
        return accountService != null;
    }

    public String getAuthorization() {
        return "Bearer " + accountService.getServiceToken();
    }
}
