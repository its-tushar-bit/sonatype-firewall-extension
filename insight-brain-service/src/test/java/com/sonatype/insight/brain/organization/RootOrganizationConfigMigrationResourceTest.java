/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.organization;

import java.io.IOException;

import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.migration.RootOrganizationConfigMigrationUtils;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.service.AbstractResourceTest;
import com.sonatype.insight.brain.service.InsightWork;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

public class RootOrganizationConfigMigrationResourceTest
    extends AbstractResourceTest
{
  private Organization org;

  private RootOrganizationConfigMigrationUtils migrationUtils;

  @Before
  public void setup() throws Exception {
    org = tempEntity.newOrganization();
    InsightWork insightWork = new InsightWork(getCLMServer().getConfiguration());
    migrationUtils = new RootOrganizationConfigMigrationUtils(insightWork);
    migrationUtils.clean();
  }

  @After
  public void tearDown() throws IOException {
    migrationUtils.clean();
    migrationUtils.setMigrated();
  }

  @Test
  public void testSetRootOrganizationTemplate() throws Exception {
    HttpResponse response = restRequest().path(RootOrganizationConfigMigrationResource.RESOURCE_PATH, org.getId())
        .post();
    assertResponseStatus(204, response);
    assertTrue(migrationUtils.isMigrationScheduled());
  }

  @Test
  public void testSetRootOrganizationEmptyTemplate() throws Exception {
    HttpResponse response = restRequest().path(RootOrganizationConfigMigrationResource.RESOURCE_PATH).post();
    assertResponseStatus(204, response);
    assertTrue(migrationUtils.isMigrated());
  }
}
