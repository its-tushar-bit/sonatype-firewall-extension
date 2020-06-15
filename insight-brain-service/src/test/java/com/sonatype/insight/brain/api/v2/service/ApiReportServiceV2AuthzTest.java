/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import java.io.IOException;
import java.net.URISyntaxException;

import javax.inject.Inject;

import com.sonatype.insight.brain.api.v2.dto.ApiReportHistoryDTO;
import com.sonatype.insight.brain.service.AbstractServiceAuthzTest;
import com.sonatype.insight.error.exception.NotFoundException;

import org.apache.shiro.authz.UnauthenticatedException;
import org.apache.shiro.authz.UnauthorizedException;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class ApiReportServiceV2AuthzTest
    extends AbstractServiceAuthzTest
{
  @Inject
  private ApiReportServiceV2 apiReportServiceV2;

  @Test(expected = UnauthenticatedException.class)
  public void testGetData_Anon() throws Exception {
    apiReportServiceV2.getByApplicationId(app.getId());
  }

  @Test(expected = UnauthorizedException.class)
  public void testGetData_Unauthorized() throws Exception {
    login();
    apiReportServiceV2.getByApplicationId(app.getId());
  }

  @Test(expected = NotFoundException.class)
  public void testGetData_Authorized() throws Exception {
    grantReadPermission(app.getId());
    apiReportServiceV2.getByApplicationId("fakeappid");
  }

  @Test
  public void testGetReportHistoryForApplication_Authorized() throws IOException, URISyntaxException {
    grantReadPermission(app.getId());

    ApiReportHistoryDTO reports = apiReportServiceV2.getReportHistoryForApplication(app.getId());

    assertThat(reports.applicationId).isEqualTo(app.getId());
    assertThat(reports.reports).hasSize(0);
  }

  @Test
  public void testGetReportHistoryForApplication_AuthorizedOrg() throws IOException, URISyntaxException {
    grantReadPermission(org.getId());

    ApiReportHistoryDTO reports = apiReportServiceV2.getReportHistoryForApplication(app.getId());

    assertThat(reports.applicationId).isEqualTo(app.getId());
    assertThat(reports.reports).hasSize(0);
  }

  @Test
  public void testGetReportHistoryForApplication_Unauthenticated() {
    assertThatThrownBy(() -> apiReportServiceV2.getReportHistoryForApplication(app.getId())).isInstanceOf(
        UnauthenticatedException.class);
  }

  @Test
  public void testGetReportHistoryForApplication_UnauthorizedButAuthenticated() {
    login();
    assertThatThrownBy(() -> apiReportServiceV2.getReportHistoryForApplication(app.getId())).isInstanceOf(
        UnauthorizedException.class);
  }
}
