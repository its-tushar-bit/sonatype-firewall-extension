/*
 * Copyright (c) 2011-2015 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.organization;

import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.tag.Tag;
import com.sonatype.insight.brain.service.AbstractComponentTest;

import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;

public class ApplicationServiceTest
    extends AbstractComponentTest
{
  @Inject
  private ApplicationService dashboardService;

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
    List<Application> apps = dashboardService
        .getApplicationsByIdsAndTagIds(null /* applicationIds */, null /* tagIds */);
    assertThat(apps, hasSize(2));
  }

  @Test
  public void testGetApplicationsByIdsAndTagIds_EmptyParams() {
    List<Application> apps = dashboardService.getApplicationsByIdsAndTagIds(
        Collections.<String> emptySet() /* applicationIds */, Collections.<String> emptySet() /* tagIds */);
    assertThat(apps, hasSize(2));
  }

  @Test
  public void testGetApplicationsByIdsAndTagIds_AppId() {
    List<Application> apps = dashboardService
        .getApplicationsByIdsAndTagIds(Collections.singleton(app1.getId()), null /* tagIds */);
    assertThat(apps, hasSize(1));
    assertThat(apps.get(0).getId(), is(app1.getId()));
  }

  @Test
  public void testGetApplicationsByIdsAndTagIds_TagId() {
    Tag tag = tempEntity.newTag(org.getId());
    tempEntity.newApplicationTag(app2.getId(), tag.getId());
    List<Application> apps = dashboardService.getApplicationsByIdsAndTagIds(null /* applicationIds */,
        Collections.singleton(tag.getId()));
    assertThat(apps, hasSize(1));
    assertThat(apps.get(0).getId(), is(app2.getId()));
  }

  @Test
  public void testGetApplicationsByIdsAndTagIds_AppIdAndTagId() {
    Tag tag = tempEntity.newTag(org.getId());
    tempEntity.newApplicationTag(app2.getId(), tag.getId());
    List<Application> apps = dashboardService.getApplicationsByIdsAndTagIds(Collections.singleton(app1.getId()),
        Collections.singleton(tag.getId()));
    assertThat(apps, hasSize(0));
  }
}
