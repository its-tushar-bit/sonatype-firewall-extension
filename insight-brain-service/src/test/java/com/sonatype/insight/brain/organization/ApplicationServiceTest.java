/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.organization;

import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

import com.sonatype.insight.brain.dataaccess.TemporaryEntity;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.tag.Tag;
import com.sonatype.insight.brain.service.AbstractComponentTest;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;

public class ApplicationServiceTest
    extends AbstractComponentTest
{
  @Rule
  public TemporaryEntity tempEntity = new TemporaryEntity();

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
  public void testGetApplicationsByPublicIdsAndTagIds_NullParams() {
    List<Application> apps = dashboardService
        .getApplicationsByPublicIdsAndTagIds(null /* applicationPublicIds */, null /* tagIds */);
    assertThat(apps, hasSize(2));
  }

  @Test
  public void testGetApplicationsByPublicIdsAndTagIds_EmptyParams() {
    List<Application> apps = dashboardService.getApplicationsByPublicIdsAndTagIds(
        Collections.<String> emptySet() /* applicationPublicIds */, Collections.<String> emptySet() /* tagIds */);
    assertThat(apps, hasSize(2));
  }

  @Test
  public void testGetApplicationsByPublicIdsAndTagIds_AppPublicId() {
    List<Application> apps = dashboardService.getApplicationsByPublicIdsAndTagIds(
        Collections.singleton(app1.getPublicId()), null /* tagIds */);
    assertThat(apps, hasSize(1));
    assertThat(apps.get(0).getId(), is(app1.getId()));
  }

  @Test
  public void testGetApplicationsByPublicIdsAndTagIds_TagId() {
    Tag tag = tempEntity.newTag(org.getId());
    tempEntity.newApplicationTag(app2.getId(), tag.getId());
    List<Application> apps = dashboardService.getApplicationsByPublicIdsAndTagIds(null /* applicationPublicIds */,
        Collections.singleton(tag.getId()));
    assertThat(apps, hasSize(1));
    assertThat(apps.get(0).getId(), is(app2.getId()));
  }

  @Test
  public void testGetApplicationsByPublicIdsAndTagIds_AppPublicIdAndTagId() {
    Tag tag = tempEntity.newTag(org.getId());
    tempEntity.newApplicationTag(app2.getId(), tag.getId());
    List<Application> apps = dashboardService.getApplicationsByPublicIdsAndTagIds(
        Collections.singleton(app1.getPublicId()), Collections.singleton(tag.getId()));
    assertThat(apps, hasSize(0));
  }
}
