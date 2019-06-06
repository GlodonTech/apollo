package com.ctrip.framework.apollo.portal.repository;

import com.ctrip.framework.apollo.portal.entity.po.Org;
import org.springframework.data.repository.PagingAndSortingRepository;

/**
 * Created by ZhaoYun on 2019-06-04
 **/
public interface OrgRepository extends PagingAndSortingRepository<Org, Long> {
}
