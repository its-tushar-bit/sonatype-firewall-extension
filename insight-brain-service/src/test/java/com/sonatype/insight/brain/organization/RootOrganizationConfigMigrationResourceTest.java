/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.organization;

import java.io.File;
import java.io.IOException;

import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.service.AbstractResourceTest;

import org.codehaus.plexus.util.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

public class RootOrganizationConfigMigrationResourceTest
    extends AbstractResourceTest
{
  private Organization org;

  @Before
  public void setup() {
    org = tempEntity.newOrganization();
    getMigratedFile().delete();
  }

  @After
  public void tearDown() throws IOException {
    getMigrateFile().delete();
    FileUtils.fileWrite(getMigratedFile(), "");
  }

  @Test
  public void testSetRootOrganizationTemplate() throws Exception {
    HttpResponse response = restRequest().path(RootOrganizationConfigMigrationResource.RESOURCE_PATH, org.getId()).post();
    assertResponseStatus(204, response);
    assertTrue(getMigrateFile().isFile());
  }

  @Test
  public void testSetRootOrganizationEmptyTemplate() throws Exception {
    HttpResponse response = restRequest().path(RootOrganizationConfigMigrationResource.RESOURCE_PATH).post();
    assertResponseStatus(204, response);
    assertTrue(getMigratedFile().isFile());
  }

  private File getMigratedFile() {
    return new File(getCLMServer().getConfiguration().getSonatypeWork(),
        RootOrganizationConfigMigrationService.MIGRATED_FILE);
  }

  private File getMigrateFile() {
    return new File(getCLMServer().getConfiguration().getSonatypeWork(),
        RootOrganizationConfigMigrationService.MIGRATE_FILE);
  }
}
