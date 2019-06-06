package com.ctrip.framework.apollo.portal.service;

import com.ctrip.framework.apollo.portal.entity.po.Org;
import com.ctrip.framework.apollo.portal.repository.OrgRepository;
import com.google.common.collect.Lists;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Created by ZhaoYun on 2019-06-04
 **/
@Service
public final class OrgService {

    private final OrgRepository orgRepository;

    public OrgService(OrgRepository orgRepository) {
        this.orgRepository = orgRepository;
    }

    public List<Org> findAll() {
        Iterable<Org> orgs = orgRepository.findAll();
        return Lists.newArrayList(orgs);
    }
}
