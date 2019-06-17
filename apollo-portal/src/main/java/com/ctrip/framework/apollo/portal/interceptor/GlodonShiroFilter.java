package com.ctrip.framework.apollo.portal.interceptor;

import com.glodon.paas.shiro.filter.CustomUserFilter;
import org.apache.commons.lang3.StringUtils;

import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;

/**
 * Created by ZhaoYun on 2019-06-11
 **/
public final class GlodonShiroFilter extends CustomUserFilter {

    @Override
    protected String extractWorkbenchIdFromRequest(ServletRequest request) {
        HttpServletRequest httpServletRequest = (HttpServletRequest) request;
        String workbenchId = httpServletRequest.getParameter("workbenchId");
        return StringUtils.isEmpty(workbenchId) ? null : workbenchId;
    }
}
