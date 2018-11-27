/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.organization;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.audit.AuditDTO;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.service.AbstractAuditTest;

import org.junit.Before;
import org.junit.Test;

public class OrganizationResourceAuditTest
    extends AbstractAuditTest
{
  private Organization organization;

  @Before
  public void before() {
    organization = tempEntity.newOrganization();
  }

  @Test
  public void testAddOrganization() throws Exception {
    Organization organization = new Organization("orgName");

    organization = organizationRequest().body(organization).post().getBody(Organization.class);
    tempEntity.register(organization);

    AuditDTO auditDTO = assertAuditLog(AuditEvent.CREATE_ORGANIZATION, null);
    assertOrganizationData(auditDTO, organization);
  }

  @Test
  public void testAddOrganization_Unauthorized() throws Exception {
    organizationRequest().body(new Organization()).with(unauthorizedUser()).post();

    assertAuditLog(AuditEvent.CREATE_ORGANIZATION, "unauthorized");
  }

  @Test
  public void testUpdateOrganization() throws Exception {
    organization.setName("updatedName");

    organizationRequest().body(organization).put();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.UPDATE_ORGANIZATION, null);
    assertOrganizationData(auditDTO, organization);
  }

  @Test
  public void testUpdateOrganization_Unauthorized() throws Exception {
    organizationRequest().body(organization).with(unauthorizedUser()).put();

    assertAuditLog(AuditEvent.UPDATE_ORGANIZATION, "unauthorized");
  }

  @Test
  public void testDeleteOrganization() throws Exception {
    organizationRequest().path(OrganizationResource.DELETE_ORGANIZATION_PATH).parameter(organization.getId()).delete();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.DELETE_ORGANIZATION, null);
    assertOrganizationData(auditDTO, organization);
  }

  @Test
  public void testDeleteOrganization_Unauthorized() throws Exception {
    organizationRequest().path(OrganizationResource.DELETE_ORGANIZATION_PATH).parameter(organization.getId())
        .with(unauthorizedUser()).delete();

    assertAuditLog(AuditEvent.DELETE_ORGANIZATION, "unauthorized");
  }

  @Test
  public void testSetIcon_Robot() throws Exception {
    organizationRequest().path(OrganizationResource.ORGANIZATION_ICON_PATH).parameter(organization.getId())
        .part("hasRobotSource", "true").part("hashcode", "").post();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.CONFIGURE_ORGANIZATION_ICON, null);
    assertOrganizationData(auditDTO, organization);
    assertCustomData(auditDTO, "iconType", "robot");
  }

  @Test
  public void testSetIcon_File() throws Exception {
    String iconFilename = "defaulticon_application.png";

    organizationRequest().path(OrganizationResource.ORGANIZATION_ICON_PATH).parameter(organization.getId())
        .part("hasRobotSource", "false")
        .part("file", iconFilename, IconUtils.loadIconFromProductAssets("defaulticon_application.png")).post();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.CONFIGURE_ORGANIZATION_ICON, null);
    assertOrganizationData(auditDTO, organization);
    assertCustomData(auditDTO, "iconType", "file");
    assertCustomData(auditDTO, "iconFilename", iconFilename);
  }

  @Test
  public void testSetIcon_Default() throws Exception {
    organizationRequest().path(OrganizationResource.ORGANIZATION_ICON_PATH).parameter(organization.getId())
        .part("hasRobotSource", "false").post();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.CONFIGURE_ORGANIZATION_ICON, null);
    assertOrganizationData(auditDTO, organization);
    assertCustomData(auditDTO, "iconType", "default");
  }

  @Test
  public void testSetIcon_Unauthorized() throws Exception {
    organizationRequest().path(OrganizationResource.ORGANIZATION_ICON_PATH).parameter(organization.getId())
        .part("hasRobotSource", "false").with(unauthorizedUser()).post();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.CONFIGURE_ORGANIZATION_ICON, "unauthorized");
    assertOrganizationData(auditDTO, organization);
  }

  private HttpRequest organizationRequest() {
    return restRequest().path(OrganizationResource.RESOURCE_PATH);
  }
}
