/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import java.io.File;
import java.io.IOException;

import jakarta.inject.Inject;
import jakarta.ws.rs.core.MediaType;

import com.sonatype.insight.brain.dataaccess.TemporaryEntity;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;
import com.sonatype.insight.brain.service.AbstractServiceAuthzTest;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.brain.utils.ReportHelper;

import org.apache.commons.io.FileUtils;
import org.apache.shiro.authz.UnauthenticatedException;
import org.apache.shiro.authz.UnauthorizedException;
import org.cyclonedx.Version;
import org.junit.Before;
import org.junit.Test;

public class ApiCycloneDxServiceV2AuthzTest
    extends AbstractServiceAuthzTest
{
  @Inject
  private ApiCycloneDxServiceV2 service;

  @Inject
  private InsightWork work;

  private String scanId;

  @Before
  public void setup() {
    scanId = TemporaryEntity.uuid();
    setBaseUrl("http://localhost:8070/");
  }

  private void createReportAndPolicyEvaluation() throws IOException {
    File reportFile = work.getReportFile(app.getId(), scanId);
    FileUtils.copyURLToFile(ReportHelper.zipReport("/" + getClass().getSimpleName() + "/report", tempDir), reportFile);

    tempEntity.newPolicyEvaluation(app.getId(), BuildStageType.ID, scanId);
  }

  @Test
  public void testGetByScanId_Authorized() throws Exception {
    createReportAndPolicyEvaluation();
    grantReadPermission(app.getId());
    service.getByScanId(app.getId(), scanId, MediaType.APPLICATION_XML, Version.VERSION_11);
  }

  @Test(expected = UnauthorizedException.class)
  public void testGetByScanId_Unauthorized() {
    login();
    service.getByScanId(app.getId(), scanId, MediaType.APPLICATION_JSON, Version.VERSION_12);
  }

  @Test(expected = UnauthenticatedException.class)
  public void testGetByScanId_Unauthenticated() {
    service.getByScanId("fakeappid", scanId, MediaType.APPLICATION_XML, Version.VERSION_11);
  }

  @Test
  public void testGetLatest_Authorized() throws Exception {
    createReportAndPolicyEvaluation();
    grantReadPermission(app.getId());
    service.getLatest(app.getId(), BuildStageType.ID, MediaType.APPLICATION_JSON, Version.VERSION_12);
  }

  @Test(expected = UnauthorizedException.class)
  public void testGetLatest_Unauthorized() {
    login();
    service.getLatest(app.getId(), BuildStageType.ID, MediaType.APPLICATION_XML, Version.VERSION_11);
  }

  @Test(expected = UnauthenticatedException.class)
  public void testGetLatest_Unauthenticated() {
    service.getLatest(app.getId(), BuildStageType.ID, MediaType.APPLICATION_XML, Version.VERSION_11);
  }
}
