/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.organization;

import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

import com.sonatype.insight.brain.dataaccess.InvalidApplicationException;
import com.sonatype.insight.brain.dataaccess.security.MembershipMappingDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.security.MemberType;
import com.sonatype.insight.brain.model.security.MembershipMapping;
import com.sonatype.insight.brain.model.security.Role;
import com.sonatype.insight.brain.model.tag.Tag;
import com.sonatype.insight.brain.service.AbstractComponentTest;

import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.fail;

public class ApplicationServiceTest
    extends AbstractComponentTest
{
  @Inject
  private ApplicationService applicationService;

  private Organization org;
  private Application app1;
  private Application app2;

  @Before
  public void before() {
    org = tempEntity.newOrganization();
    app1 = tempEntity.newApplication(org.getId());
    app2 = tempEntity.newApplication(org.getId());
  }

  @Test
  public void testGetApplicationsByIdsAndTagIds_NullParams() {
    List<Application> apps = applicationService
        .getApplicationsByIdsAndTagIds(null /* applicationIds */, null /* tagIds */);
    assertThat(apps, hasSize(2));
  }

  @Test
  public void testGetApplicationsByIdsAndTagIds_EmptyParams() {
    List<Application> apps = applicationService.getApplicationsByIdsAndTagIds(
        Collections.<String> emptySet() /* applicationIds */, Collections.<String> emptySet() /* tagIds */);
    assertThat(apps, hasSize(2));
  }

  @Test
  public void testGetApplicationsByIdsAndTagIds_AppId() {
    List<Application> apps = applicationService
        .getApplicationsByIdsAndTagIds(Collections.singleton(app1.getId()), null /* tagIds */);
    assertThat(apps, hasSize(1));
    assertThat(apps.get(0).getId(), is(app1.getId()));
  }

  @Test
  public void testGetApplicationsByIdsAndTagIds_TagId() {
    Tag tag = tempEntity.newTag(org.getId());
    tempEntity.newApplicationTag(app2.getId(), tag.getId());
    List<Application> apps = applicationService.getApplicationsByIdsAndTagIds(null /* applicationIds */,
        Collections.singleton(tag.getId()));
    assertThat(apps, hasSize(1));
    assertThat(apps.get(0).getId(), is(app2.getId()));
  }

  @Test
  public void testGetApplicationsByIdsAndTagIds_AppIdAndTagId() {
    Tag tag = tempEntity.newTag(org.getId());
    tempEntity.newApplicationTag(app2.getId(), tag.getId());
    List<Application> apps = applicationService.getApplicationsByIdsAndTagIds(Collections.singleton(app1.getId()),
        Collections.singleton(tag.getId()));
    assertThat(apps, hasSize(0));
  }

  @Test
  public void testAddApplication_RootOrgIsNoValidParent() {
    Application app = new Application("appPublicId", "appName", Organization.ROOT_ORGANIZATION_ID);
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
    Application app = new Application("appPublicId", "appName", org.getId());
    app = applicationService.addApplication(app);
    tempEntity.register(app);
    List<MembershipMapping> mappings = new MembershipMappingDAO().getByContextIdAndRoleId(app.getId(),
        Role.OWNER_ROLE_ID);
    assertThat(mappings.size(), is(1));
    assertThat(mappings.get(0).getMemberName(), is(USERNAME));
    assertThat(mappings.get(0).getMemberType(), is(MemberType.USER));
  }
}
