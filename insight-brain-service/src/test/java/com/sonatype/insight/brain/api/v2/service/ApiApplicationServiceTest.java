/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import java.util.List;

import javax.inject.Inject;

import com.sonatype.insight.brain.api.v2.dto.ApiApplicationDTO;
import com.sonatype.insight.brain.dataaccess.InvalidApplicationException;
import com.sonatype.insight.brain.dataaccess.security.MembershipMappingDAO;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.security.MemberType;
import com.sonatype.insight.brain.model.security.MembershipMapping;
import com.sonatype.insight.brain.model.security.Role;
import com.sonatype.insight.brain.service.AbstractComponentTest;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

public class ApiApplicationServiceTest
    extends AbstractComponentTest
{
  @Inject
  private ApiApplicationService applicationService;

  @Test
  public void testAddApplication_RootOrgIsNoValidParent() {
    ApiApplicationDTO app = new ApiApplicationDTO();
    app.publicId = "appPublicId";
    app.name = "appName";
    app.organizationId = Organization.ROOT_ORGANIZATION_ID;
    assertThatExceptionOfType(InvalidApplicationException.class).isThrownBy(() -> {
      applicationService.addApplication(app);
    }).withMessage("Applications cannot have the root organization as parent.");
  }

  @Test
  public void testAddApplication_addsUserToOwnerRole() {
    Organization org = tempEntity.newOrganization();
    ApiApplicationDTO app = new ApiApplicationDTO();
    app.publicId = "appPublicId";
    app.name = "appName";
    app.organizationId = org.getId();
    app = applicationService.addApplication(app);
    List<MembershipMapping> mappings = new MembershipMappingDAO()
        .getByContextIdAndRoleId(app.id, Role.OWNER_ROLE_ID);
    assertThat(mappings).hasSize(1);
    assertThat(mappings.get(0).getMemberName()).isEqualTo(USERNAME);
    assertThat(mappings.get(0).getMemberType()).isEqualTo(MemberType.USER);
  }
}
