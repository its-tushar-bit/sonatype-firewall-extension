/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

import javax.inject.Inject;

import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;
import com.sonatype.insight.brain.service.AbstractServiceAuthzTest;
import com.sonatype.insight.brain.service.InsightWork;

import org.apache.shiro.authz.UnauthenticatedException;
import org.apache.shiro.authz.UnauthorizedException;
import org.codehaus.plexus.util.FileUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class ApiCycloneDxServiceV2AuthzTest
    extends AbstractServiceAuthzTest
{
  @Inject
  private ApiCycloneDxServiceV2 service;

  @Inject
  private InsightWork work;

  @Rule
  public ExpectedException expected = ExpectedException.none();

  private Application application;

  private String scanId;

  @Before
  public void setup() throws IOException {
    scanId = UUID.randomUUID().toString();
    application = tempEntity.newApplication(tempEntity.newOrganization().getId());

    File reportFile = work.getReportFile(application.getId(), scanId);
    reportFile.getParentFile().mkdirs();
    FileUtils.copyURLToFile(getClass().getResource("/ApiCycloneDxServiceV2Test/report.zip"), reportFile);

    tempEntity.newPolicyEvaluation(application.getId(), BuildStageType.ID, scanId);
  }

  @Test
  public void testGetByScanId_Authorized() {
    grantReadPermission(application.getId());
    service.getByScanId(application.getId(), scanId);
  }

  @Test(expected = UnauthorizedException.class)
  public void testGetByScanId_Unauthorized() {
    login();
    service.getByScanId(application.getId(), scanId);
  }

  @Test(expected = UnauthenticatedException.class)
  public void testGetByScanId_Unauthenticated() {
    service.getByScanId("fakeappid", scanId);
  }

  @Test
  public void testGetLatest_Authorized() {
    grantReadPermission(application.getId());
    service.getLatest(application.getId(), BuildStageType.ID);
  }

  @Test(expected = UnauthorizedException.class)
  public void testGetLatest_Unauthorized() {
    login();
    service.getLatest(application.getId(), BuildStageType.ID);
  }

  @Test(expected = UnauthenticatedException.class)
  public void testGetLatest_Unauthenticated() {
    service.getLatest(application.getId(), BuildStageType.ID);
  }
}
