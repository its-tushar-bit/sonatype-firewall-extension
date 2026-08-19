/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import java.io.File;
import java.io.IOException;

import jakarta.inject.Inject;

import com.sonatype.insight.brain.dataaccess.TemporaryEntity;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;
import com.sonatype.insight.brain.model.repository.HostedRepositoryComponent;
import com.sonatype.insight.brain.variant.AbstractComponentH2AuthzTest;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.brain.utils.ReportHelper;
import com.sonatype.insight.brain.variant.ComponentH2Test;

import org.apache.commons.io.FileUtils;
import org.apache.shiro.authz.UnauthenticatedException;
import org.apache.shiro.authz.UnauthorizedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

@ComponentH2Test
public class ApiSpdxServiceAuthzTest
    extends AbstractComponentH2AuthzTest
{
  @Inject
  private ApiSpdxService service;

  @Inject
  private InsightWork work;

  private String scanId;

  @BeforeEach
  public void setup() {
    scanId = TemporaryEntity.uuid();
    setBaseUrl("http://localhost:8070/");
  }

  private void createReportAndPolicyEvaluation() throws IOException {
    File reportFile = work.getReportFile(app.getId(), scanId);
    FileUtils.copyURLToFile(ReportHelper.zipReport("/ApiSpdxServiceTest/report", tempDir), reportFile);

    tempEntity.newPolicyEvaluation(app.getId(), BuildStageType.ID, scanId);
  }

  @Test
  public void testGetByScanId_Authorized() throws Exception {
    createReportAndPolicyEvaluation();
    grantReadPermission(app.getId());
    service.getByScanId(app.getId(), scanId, "json", false, "2.3");
  }

  @Test
  public void testGetByScanId_Unauthorized() {
    login();
    assertThrows(UnauthorizedException.class,
        () -> service.getByScanId(app.getId(), scanId, "json", false, "2.3"));
  }

  @Test
  public void testGetByScanId_Unauthenticated() {
    assertThrows(UnauthenticatedException.class,
        () -> service.getByScanId("fakeappid", scanId, "json", false, "2.3"));
  }

  @Test
  public void testGetLatestForStage_Authorized() throws Exception {
    createReportAndPolicyEvaluation();
    grantReadPermission(app.getId());
    service.getLatestForStage(app.getId(), BuildStageType.ID, "json", false, "2.3");
  }

  @Test
  public void testGetLatestForStage_Unauthorized() {
    login();
    assertThrows(UnauthorizedException.class,
        () -> service.getLatestForStage(app.getId(), BuildStageType.ID, "json", false, "2.3"));
  }

  @Test
  public void testGetLatestForStage_Unauthenticated() {
    assertThrows(UnauthenticatedException.class,
        () -> service.getLatestForStage(app.getId(), BuildStageType.ID, "json", false, "2.3"));
  }

  @Test
  public void testGetByScanId_Hrc_Unauthenticated() {
    HostedRepositoryComponent hrc = tempEntity.newHostedRepositoryComponent(repository);
    assertThrows(UnauthenticatedException.class,
        () -> service.getByScanId(hrc, scanId, "json", false, "2.3"));
  }

  @Test
  public void testGetByScanId_Hrc_Unauthorized() {
    HostedRepositoryComponent hrc = tempEntity.newHostedRepositoryComponent(repository);
    login();
    assertThrows(UnauthorizedException.class,
        () -> service.getByScanId(hrc, scanId, "json", false, "2.3"));
  }

  @Test
  public void testGetLatestForStage_Hrc_Unauthenticated() {
    HostedRepositoryComponent hrc = tempEntity.newHostedRepositoryComponent(repository);
    assertThrows(UnauthenticatedException.class,
        () -> service.getLatestForStage(hrc, BuildStageType.ID, "json", false, "2.3"));
  }

  @Test
  public void testGetLatestForStage_Hrc_Unauthorized() {
    HostedRepositoryComponent hrc = tempEntity.newHostedRepositoryComponent(repository);
    login();
    assertThrows(UnauthorizedException.class,
        () -> service.getLatestForStage(hrc, BuildStageType.ID, "json", false, "2.3"));
  }
}
