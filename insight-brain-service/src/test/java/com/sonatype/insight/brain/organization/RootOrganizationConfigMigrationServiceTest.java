/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.organization;

import java.io.IOException;

import javax.inject.Inject;

import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.migration.RootOrganizationConfigMigrationUtils;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.service.InsightConfig;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.NotFoundException;

import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

public class RootOrganizationConfigMigrationServiceTest
    extends AbstractComponentTest
{
  @Inject
  private InsightConfig insightConfig;

  private RootOrganizationConfigMigrationService service;

  private RootOrganizationConfigMigrationUtils migrationUtils;

  @Before
  public void setup() throws IOException {
    insightConfig.setSonatypeWork(tempDir.newFolder().getAbsolutePath());
    InsightWork insightWork = new InsightWork(insightConfig);
    migrationUtils = new RootOrganizationConfigMigrationUtils(insightWork);

    service = new RootOrganizationConfigMigrationService(new OrganizationDAO(), migrationUtils);
  }

  @Test
  public void testSetRootOrganizationTemplate() throws IOException {
    Organization org = tempEntity.newOrganization();
    service.setRootOrganizationTemplate(org.getId());

    assertThat(migrationUtils.isMigrationScheduled()).isTrue();
    assertThat(migrationUtils.getSourceOrganizationId()).isEqualTo(org.getId());
  }

  @Test
  public void testSetRootOrganizationTemplate_previouslyScheduled() throws IOException {
    migrationUtils.setSourceOrganizationId("bla");
    Organization org = tempEntity.newOrganization();
    assertThatExceptionOfType(BadRequestException.class).isThrownBy(() -> {
      service.setRootOrganizationTemplate(org.getId());
    }).withMessageContaining("has previously been scheduled or performed");
    assertThat(migrationUtils.getSourceOrganizationId()).isNotEqualTo(org.getId());
  }

  @Test
  public void testSetRootOrganizationTemplate_previouslyMigrated() throws IOException {
    migrationUtils.setMigrated();
    Organization org = tempEntity.newOrganization();
    assertThatExceptionOfType(BadRequestException.class).isThrownBy(() -> {
      service.setRootOrganizationTemplate(org.getId());
    }).withMessageContaining("has previously been scheduled or performed");
  }

  @Test
  public void testSetRootOrganizationTemplate_missingOrg() {
    assertThatExceptionOfType(NotFoundException.class).isThrownBy(() -> {
      service.setRootOrganizationTemplate("missing-org");
    }).withMessageContaining("not find organization with ID missing-org");
  }

  @Test
  public void testSetRootOrganizationEmptyTemplate() throws IOException {
    service.setRootOrganizationEmptyTemplate();
    assertThat(migrationUtils.isMigrated()).isTrue();
  }

  @Test
  public void testSetRootOrganizationEmptyTemplate_alreadyScheduled() throws IOException {
    migrationUtils.setSourceOrganizationId("bla");
    assertThatExceptionOfType(BadRequestException.class).isThrownBy(() -> {
      service.setRootOrganizationEmptyTemplate();
    }).withMessageContaining("has previously been scheduled or performed");
    assertThat(migrationUtils.isMigrated()).isFalse();
  }

  @Test
  public void testSetRootOrganizationEmptyTemplate_alreadyMigrated() throws IOException {
    migrationUtils.setMigrated();
    assertThatExceptionOfType(BadRequestException.class).isThrownBy(() -> {
      service.setRootOrganizationEmptyTemplate();
    }).withMessageContaining("has previously been scheduled or performed");
  }
}
