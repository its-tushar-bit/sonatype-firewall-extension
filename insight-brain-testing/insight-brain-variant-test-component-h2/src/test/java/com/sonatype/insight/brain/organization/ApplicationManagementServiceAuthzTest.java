/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.organization;

import java.util.List;
import jakarta.inject.Inject;

import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.variant.AbstractComponentH2AuthzTest;
import com.sonatype.insight.brain.variant.ComponentH2Test;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@ComponentH2Test
public class ApplicationManagementServiceAuthzTest
    extends AbstractComponentH2AuthzTest
{
  @Inject
  private ApplicationManagementService applicationManagementService;

  @Test
  public void testGetApplicationManagementSummaries_Global_Authorized() {
    grantGlobalPermission(Permission.READ);
    List<ApplicationManagementSummaryDTO> dtos = applicationManagementService
        .getApplicationManagementSummaries(null, ApplicationManagementSummaryOrder.APP_NAME_ASC, 1, 10);
    assertThat(dtos).extracting(ApplicationManagementSummaryDTO::getId).containsExactly(app.getId());
  }

  @Test
  public void testGetApplicationManagementSummaries_RootOrg_Authorized() {
    grantReadPermission(Organization.ROOT_ORGANIZATION_ID);
    List<ApplicationManagementSummaryDTO> dtos = applicationManagementService
        .getApplicationManagementSummaries(null, ApplicationManagementSummaryOrder.APP_NAME_ASC, 1, 10);
    assertThat(dtos).extracting(ApplicationManagementSummaryDTO::getId).containsExactly(app.getId());
  }

  @Test
  public void testGetApplicationManagementSummaries_Org_Authorized() {
    grantReadPermission(org.getId());
    List<ApplicationManagementSummaryDTO> dtos = applicationManagementService
        .getApplicationManagementSummaries(null, ApplicationManagementSummaryOrder.APP_NAME_ASC, 1, 10);
    assertThat(dtos).extracting(ApplicationManagementSummaryDTO::getId).containsExactly(app.getId());
  }

  @Test
  public void testGetApplicationManagementSummaries_App_Authorized() {
    grantReadPermission(app.getId());
    List<ApplicationManagementSummaryDTO> dtos = applicationManagementService
        .getApplicationManagementSummaries(null, ApplicationManagementSummaryOrder.APP_NAME_ASC, 1, 10);
    assertThat(dtos).extracting(ApplicationManagementSummaryDTO::getId).containsExactly(app.getId());
  }

  @Test
  public void testGetApplicationManagementSummaries_Unauthorized() {
    login();
    List<ApplicationManagementSummaryDTO> dtos = applicationManagementService
        .getApplicationManagementSummaries(null, ApplicationManagementSummaryOrder.APP_NAME_ASC, 1, 10);
    assertThat(dtos).isEmpty();
  }

  @Test
  public void testGetApplicationManagementSummaries_Unauthenticated() {
    List<ApplicationManagementSummaryDTO> dtos = applicationManagementService
        .getApplicationManagementSummaries(null, ApplicationManagementSummaryOrder.APP_NAME_ASC, 1, 10);
    assertThat(dtos).isEmpty();
  }
}
