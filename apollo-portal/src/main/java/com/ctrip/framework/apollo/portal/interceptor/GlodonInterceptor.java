package com.ctrip.framework.apollo.portal.interceptor;

import com.ctrip.framework.apollo.common.entity.App;
import com.ctrip.framework.apollo.portal.listener.AppCreationEvent;
import com.ctrip.framework.apollo.portal.service.AppService;
import com.ctrip.framework.apollo.portal.service.RolePermissionService;
import com.ctrip.framework.apollo.portal.util.RoleUtils;
import com.glodon.digiarch.paas.foundation.enums.RoleEnum;
import com.glodon.digiarch.paas.foundation.proxy.PlatformFoundationServiceClient;
import com.glodon.digiarch.paas.foundation.vo.AppInfoVO;
import com.glodon.digiarch.paas.foundation.vo.MemberVO;
import com.glodon.digiarch.paas.foundation.vo.RoleVO;
import com.glodon.digiarch.paas.foundation.vo.WorkbenchInfoVO;
import com.glodon.paas.shiro.constant.ReqAttributeConstant;
import com.glodon.paas.shiro.utils.ShiroUtil;
import com.google.common.collect.Sets;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 若满足以下条件：
 * 1. url 里面包含了workerBenchId
 * 2. 当前用户是超级管理员
 * <p>
 * 就执行初始化应用、给成员赋予权限的流程
 * <p>
 * Created by ZhaoYun on 2019-06-10
 **/
public final class GlodonInterceptor implements HandlerInterceptor {

    private static final Logger logger = LoggerFactory.getLogger(GlodonInterceptor.class);

    private static final String RESOURCE_ID = "apollo";
    private static final int PAGE_SIZE = 1000;

    private final ApplicationContext applicationContext;
    private final ApplicationEventPublisher publisher;

    private final PlatformFoundationServiceClient client;

    private AppService appService;
    private RolePermissionService rolePermissionService;

    public GlodonInterceptor(ApplicationContext applicationContext, ApplicationEventPublisher publisher, String foundationHost) {
        this.applicationContext = applicationContext;
        this.publisher = publisher;

        logger.info("GlodonInterceptor Constructor, foundation host: {}", foundationHost);
        client = new PlatformFoundationServiceClient(foundationHost);
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 若没有 workbenchId, 则跳过
        WorkbenchInfoVO workbenchInfoVO = (WorkbenchInfoVO) request.getAttribute(ReqAttributeConstant.WORKBENCHINFO);
        if (workbenchInfoVO == null) {
            return true;
        }

        logger.info("workbenchId: {}", workbenchInfoVO.getId());

        // 若没有开通 apollo 技术服务的 app，则跳过
        String accessToken = ShiroUtil.getAccessToken(request);
        List<AppInfoVO> tairApps = client.getWorkbenchServiceBelongedApps(workbenchInfoVO.getId(), RESOURCE_ID, accessToken);
        if (CollectionUtils.isEmpty(tairApps)) {
            return true;
        }

        logger.info("app size: {}", tairApps.size());

        // 把当前工作台中开通了 apollo 服务的 app 存入 session 中，
        // 在 apollo 中列出所有的 app 时，只显示这些 app。
        // 即用作过滤。
        Set<String> apolloAppIds = tairApps.stream().map(a -> getApolloAppId(workbenchInfoVO, a)).collect(Collectors.toSet());
        request.getSession(false).setAttribute("apolloAppIds", apolloAppIds);

        // 若不能查到成员信息，则跳过
        List<MemberVO> members = null;

        // 分成普通成员和 admin 成员
        String superAdmin = workbenchInfoVO.getSuperAdminName();
        logger.info("super admin: {}", superAdmin);

        Set<String> admins = new HashSet<>(), normals = new HashSet<>();

        for (AppInfoVO tairApp : tairApps) {
            App apolloApp = getApolloApp(tairApp, superAdmin, workbenchInfoVO);

            if (appExist(apolloApp.getAppId())) {
//                client.getMembersUpdatedTimeBiggerThanIncludeDeleted(workbenchInfoVO.getId(),0L , accessToken);
                continue; // TODO 如果 app 存在，还需检测成员的变动，并同步到 apollo
            }

            if (members == null) {
                members = getMembers(workbenchInfoVO.getId(), accessToken);
                distinctMember(members, admins, normals);

                logger.info("admins: {}, normals: {}", admins, normals);
            }

            createApp(apolloApp, admins, normals, superAdmin);
        }

        return true;
    }

    /**
     * 获取成员信息
     */
    private List<MemberVO> getMembers(String workbenchId, String accessToken) {
        List<MemberVO> members = client.getWorkbenchByMembers(workbenchId, 0, PAGE_SIZE, accessToken).getList();
        if (CollectionUtils.isEmpty(members)) {
            throw new RuntimeException("can't get member information");
        }
        // 不能支持超过 PAGE_SIZE 的成员
        if (members.size() >= PAGE_SIZE) {
            throw new RuntimeException("exceed max " + PAGE_SIZE + " members");
        }
        return members;
    }

    /**
     * 把成员分为：
     * admins: 所有的 admin
     * normals：所有的 member + admin
     */
    private void distinctMember(List<MemberVO> members, Set<String> admins, Set<String> normals) {
        for (MemberVO m : members) {
            for (RoleVO r : m.getRoles()) {
                switch (RoleEnum.idToRoleEnum(r.getId())) {
                    case WORKBENCH_ADMIN: {
                        admins.add(m.getName());
                        normals.add(m.getName());
                        break;
                    }
                    case WORKBENCH_MEMBER: {
                        normals.add(m.getName());
                        break;
                    }
                }
            }
        }
    }

    /**
     * 创建应用 并分配权限
     */
    private void createApp(App app, Set<String> admins, Set<String> normals, String userName) {
        App createdApp = getAppService().createAppInLocal(app);

        publisher.publishEvent(new AppCreationEvent(createdApp));

        logger.info("create app: {}", createdApp);

        // workbench super admin -> apollo master + release + modify
        String masterRoleName = RoleUtils.buildAppMasterRoleName(createdApp.getAppId());
        getRolePermissionService().assignRoleToUsers(masterRoleName, Sets.newHashSet(createdApp.getOwnerName()), userName);

        // workbench admin -> apollo release + modify
        String releaseRoleName = RoleUtils.buildReleaseDefaultNamespaceRoleName(createdApp.getAppId());
        getRolePermissionService().assignRoleToUsers(releaseRoleName, admins, userName);

        // workbench member -> apollo modify
        String modifyRoleName = RoleUtils.buildModifyDefaultNamespaceRoleName(createdApp.getAppId());
        getRolePermissionService().assignRoleToUsers(modifyRoleName, normals, userName);
    }

    private boolean appExist(String appId) {
        return getAppService().findByAppIds(Sets.newHashSet(appId)).size() > 0;
    }

    /**
     * workbench app 转 apollo app
     */
    private App getApolloApp(AppInfoVO tairApp, String superAdmin, WorkbenchInfoVO workbenchInfoVO) {
        return App.builder()
                .appId(getApolloAppId(workbenchInfoVO, tairApp))
                .name(tairApp.getName())
                .ownerName(superAdmin)
                .orgId(workbenchInfoVO.getName())
                .orgName(StringUtils.defaultString(workbenchInfoVO.getProductId(), ""))
                .build();
    }

    /**
     * tair 的 appId 转 apollo 的 appId
     */
    private String getApolloAppId(WorkbenchInfoVO workbenchInfoVO, AppInfoVO tairApp) {
        return workbenchInfoVO.getName() + "_" + tairApp.getName() + "_" + workbenchInfoVO.getId().substring(0, 5);
    }

    private AppService getAppService() {
        return appService != null ? appService :
                (appService = applicationContext.getBean(AppService.class));
    }

    private RolePermissionService getRolePermissionService() {
        return rolePermissionService != null ? rolePermissionService :
                (rolePermissionService = applicationContext.getBean(RolePermissionService.class));
    }
}
