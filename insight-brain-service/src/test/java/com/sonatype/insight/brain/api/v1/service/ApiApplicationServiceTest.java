/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v1.service;

import java.util.List;

import javax.inject.Inject;

import com.sonatype.insight.brain.api.v1.dto.ApiApplicationDTO;
import com.sonatype.insight.brain.dataaccess.InvalidApplicationException;
import com.sonatype.insight.brain.dataaccess.security.MembershipMappingDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.security.MemberType;
import com.sonatype.insight.brain.model.security.MembershipMapping;
import com.sonatype.insight.brain.model.security.Role;
import com.sonatype.insight.brain.service.AbstractComponentTest;

import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.fail;

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
    try {
      applicationService.addApplication(app);
      fail("Expected exception");
    }
    catch (InvalidApplicationException e) {
      assertThat(e.getMessage(), is("Applications cannot have the root organization as parent."));
    }
  }

  @Test
  public void testAddApplication_addsUserToOwnerRole() {
    Organization org = tempEntity.newOrganization();
    ApiApplicationDTO app = new ApiApplicationDTO();
    app.publicId = "appPublicId";
    app.name = "appName";
    app.organizationId = org.getId();
    app = applicationService.addApplication(app);
    Application forRegistration = new Application(app.publicId, app.name, app.organizationId);
    forRegistration.setId(app.id);
    tempEntity.register(forRegistration);
    List<MembershipMapping> mappings = new MembershipMappingDAO()
        .getByContextIdAndRoleId(app.id, Role.OWNER_ROLE_ID);
    assertThat(mappings.size(), is(1));
    assertThat(mappings.get(0).getMemberName(), is(USERNAME));
    assertThat(mappings.get(0).getMemberType(), is(MemberType.USER));
  }
}
