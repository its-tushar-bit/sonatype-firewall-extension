/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.configuration;

import javax.inject.Inject;

import com.sonatype.insight.brain.dataaccess.configuration.SystemConfigurationPropertyDAO;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.error.exception.BadRequestException;

import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.fail;

public class AutomaticApplicationsConfigurationServiceTest
    extends AbstractComponentTest
{
  @Inject
  private AutomaticApplicationsConfigurationService service;

  @Inject
  private SystemConfigurationPropertyDAO dao;

  @Test
  public void testUpdate() {
    Organization organization = tempEntity.newOrganization();

    AutomaticApplicationsConfiguration updated = service
        .update(new AutomaticApplicationsConfiguration(true, organization.getId()));

    assertThat(updated.isEnabled(), is(true));
    assertThat(updated.getParentOrganizationId(), is(organization.getId()));

    SystemConfigurationProperty enabled = dao
        .getByNameNotNull(SystemConfigurationProperty.AUTOMATIC_APPLICATION_CREATION_ENABLED);
    SystemConfigurationProperty organizationId = dao
        .getByNameNotNull(SystemConfigurationProperty.AUTOMATIC_APPLICATION_CREATION_ORGANIZATION_ID);

    assertThat(enabled.getValue(), is("true"));
    assertThat(organizationId.getValue(), is(organization.getId()));
  }

  @Test
  public void testUpdate_RootOrganizationId_Enabled() {
    try {
      service.update(new AutomaticApplicationsConfiguration(true, Organization.ROOT_ORGANIZATION_ID));
      fail("Expected exception");
    }
    catch (BadRequestException e) {
      assertThat(e.getMessage(), is("Parent cannot be the root organization."));
    }
  }

  @Test
  public void testUpdate_RootOrganizationId_Disabled() {
    try {
      service.update(new AutomaticApplicationsConfiguration(false, Organization.ROOT_ORGANIZATION_ID));
      fail("Expected exception");
    }
    catch (BadRequestException e) {
      assertThat(e.getMessage(), is("Parent cannot be the root organization."));
    }
  }

  @Test
  public void testUpdate_InvalidOrganizationId_Enabled() {
    try {
      service.update(new AutomaticApplicationsConfiguration(true, "testOrganizationID"));
      fail("Expected exception");
    }
    catch (BadRequestException e) {
      assertThat(e.getMessage(), is("Parent organization ID testOrganizationID not found."));
    }
  }

  @Test
  public void testUpdate_InvalidOrganizationId_Disabled() {
    try {
      service.update(new AutomaticApplicationsConfiguration(false, "testOrganizationID"));
      fail("Expected exception");
    }
    catch (BadRequestException e) {
      assertThat(e.getMessage(), is("Parent organization ID testOrganizationID not found."));
    }
  }

  @Test
  public void testUpdate_EmptyParentOrganizationId_Enabled() {
    try {
      service.update(new AutomaticApplicationsConfiguration(true, ""));
      fail("Expected exception");
    }
    catch (BadRequestException e) {
      assertThat(e.getMessage(),
          is("Parent organization ID is required when automatic application creation is enabled."));
    }
  }

  @Test
  public void testUpdate_EmptyParentOrganizationId_Disabled() {
    service.update(new AutomaticApplicationsConfiguration(false, ""));

    SystemConfigurationProperty enabled = dao
        .getByNameNotNull(SystemConfigurationProperty.AUTOMATIC_APPLICATION_CREATION_ENABLED);
    SystemConfigurationProperty organizationId = dao
        .getByNameNotNull(SystemConfigurationProperty.AUTOMATIC_APPLICATION_CREATION_ORGANIZATION_ID);

    assertThat(enabled.getValue(), is("false"));
    assertThat(organizationId.getValue(), is(""));
  }

  @Test
  public void testUpdate_NullParentOrganizationId_Enabled() {
    try {
      service.update(new AutomaticApplicationsConfiguration(true, null));
      fail("Expected exception");
    }
    catch (BadRequestException e) {
      assertThat(e.getMessage(),
          is("Parent organization ID is required when automatic application creation is enabled."));
    }
  }

  @Test
  public void testUpdate_NullParentOrganizationId_Disabled() {
    service.update(new AutomaticApplicationsConfiguration(false, null));

    SystemConfigurationProperty enabled = dao
        .getByNameNotNull(SystemConfigurationProperty.AUTOMATIC_APPLICATION_CREATION_ENABLED);
    SystemConfigurationProperty organizationId = dao
        .getByNameNotNull(SystemConfigurationProperty.AUTOMATIC_APPLICATION_CREATION_ORGANIZATION_ID);

    assertThat(enabled.getValue(), is("false"));
    assertThat(organizationId.getValue(), is(""));
  }

  @Test
  public void testGet() {
    dao.update(
        new SystemConfigurationProperty(SystemConfigurationProperty.AUTOMATIC_APPLICATION_CREATION_ENABLED, "true"));
    dao.update(new SystemConfigurationProperty(
        SystemConfigurationProperty.AUTOMATIC_APPLICATION_CREATION_ORGANIZATION_ID, "testGetId"));

    AutomaticApplicationsConfiguration configuration = service.get();

    assertThat(configuration.isEnabled(), is(true));
    assertThat(configuration.getParentOrganizationId(), is("testGetId"));
  }
}
