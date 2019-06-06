package com.ctrip.framework.apollo.portal.controller;


import com.ctrip.framework.apollo.portal.entity.po.Org;
import com.ctrip.framework.apollo.portal.entity.vo.Organization;
import com.ctrip.framework.apollo.portal.service.OrgService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Jason Song(song_s@ctrip.com)
 */
@RestController
@RequestMapping("/organizations")
public class OrganizationController {

  private final OrgService orgService;

  public OrganizationController(final OrgService orgService) {
    this.orgService = orgService;
  }

  @RequestMapping
  public List<Organization> loadOrganization() {
    List<Org> orgs = orgService.findAll();
    return orgs.stream().map(o -> {
      Organization organization = new Organization();
      organization.setOrgId(o.getOrgId());
      organization.setOrgName(o.getOrgName());
      return organization;
    }).collect(Collectors.toList());
  }
}
