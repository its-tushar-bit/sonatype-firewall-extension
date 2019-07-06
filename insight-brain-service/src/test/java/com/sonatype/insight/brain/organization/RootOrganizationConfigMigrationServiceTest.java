/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.organization;

import javax.inject.Inject;

import com.sonatype.insight.brain.dataaccess.MigrationTrackerDAO;
import com.sonatype.insight.brain.migration.RootOrganizationConfigMigrationUtils;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.service.AbstractComponentTest;
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
  private RootOrganizationConfigMigrationService service;

  @Inject
  private RootOrganizationConfigMigrationUtils migrationUtils;

  @Inject
  private MigrationTrackerDAO migrationTrackerDAO;

  @Before
  public void before() {
    migrationTrackerDAO.delete(migrationTrackerDAO.getById(RootOrganizationConfigMigrationUtils.MIGRATION_CONFIG_ID));
    migrationTrackerDAO.delete(migrationTrackerDAO.getById(RootOrganizationConfigMigrationUtils.MIGRATION_ID));
  }

  @Test
  public void testSetRootOrganizationTemplate() {
    Organization org = tempEntity.newOrganization();
    service.setRootOrganizationTemplate(org.getId());

    assertThat(migrationUtils.isMigrationScheduled()).isTrue();
    assertThat(migrationUtils.getSourceOrganizationId()).isEqualTo(org.getId());
  }

  @Test
  public void testSetRootOrganizationTemplate_previouslyScheduled() {
    migrationUtils.setSourceOrganizationId("bla");
    Organization org = tempEntity.newOrganization();
    assertThatExceptionOfType(BadRequestException.class).isThrownBy(() -> {
      service.setRootOrganizationTemplate(org.getId());
    }).withMessageContaining("has previously been scheduled or performed");
    assertThat(migrationUtils.getSourceOrganizationId()).isNotEqualTo(org.getId());
  }

  @Test
  public void testSetRootOrganizationTemplate_previouslyMigrated() {
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
  public void testSetRootOrganizationEmptyTemplate() {
    service.setRootOrganizationEmptyTemplate();
    assertThat(migrationUtils.isMigrated()).isTrue();
  }

  @Test
  public void testSetRootOrganizationEmptyTemplate_alreadyScheduled() {
    migrationUtils.setSourceOrganizationId("bla");
    assertThatExceptionOfType(BadRequestException.class).isThrownBy(() -> {
      service.setRootOrganizationEmptyTemplate();
    }).withMessageContaining("has previously been scheduled or performed");
    assertThat(migrationUtils.isMigrated()).isFalse();
  }

  @Test
  public void testSetRootOrganizationEmptyTemplate_alreadyMigrated() {
    migrationUtils.setMigrated();
    assertThatExceptionOfType(BadRequestException.class).isThrownBy(() -> {
      service.setRootOrganizationEmptyTemplate();
    }).withMessageContaining("has previously been scheduled or performed");
  }
}
