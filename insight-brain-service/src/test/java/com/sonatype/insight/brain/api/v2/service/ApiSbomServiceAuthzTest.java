/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import java.io.IOException;
import java.nio.file.Path;
import java.util.UUID;
import javax.inject.Inject;

import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartySbomMetadata;
import com.sonatype.insight.brain.service.AbstractServiceAuthzTest;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.brain.utils.SbomTestsHelper;

import org.apache.commons.lang.RandomStringUtils;
import org.apache.shiro.authz.UnauthenticatedException;
import org.apache.shiro.authz.UnauthorizedException;
import org.junit.Test;

public class ApiSbomServiceAuthzTest
    extends AbstractServiceAuthzTest
{
  private static final String DUMMY_APP_ID = UUID.randomUUID().toString().replace("-", "");

  private static final String DUMMY_APP_VERSION = RandomStringUtils.random(10, true, true);

  @Inject
  private ApiSbomService apiSbomService;

  @Inject
  private InsightWork insightWork;

  @Test(expected = UnauthenticatedException.class)
  public void testDeleteSbomVersion_Unauthenticated() throws IOException {
    apiSbomService.deleteSbomVersion(DUMMY_APP_ID, DUMMY_APP_VERSION);
  }

  @Test(expected = UnauthorizedException.class)
  public void testDeleteSbomVersion_Unauthorized() throws IOException {
    login();
    apiSbomService.deleteSbomVersion(app.getId(), DUMMY_APP_VERSION);
  }

  @Test
  public void testDeleteSbomVersion_Authorized() throws IOException {
    Application app = tempEntity.newApplicationWithParent();
    Path fileInWorkDirPath =
        SbomTestsHelper.createTestFileForSbomMetadata(insightWork.getSbomDir(app.getId()),
            getClass().getResource("/" + getClass().getSimpleName() + "/third-party-simple-bom.xml"));
    final ThirdPartySbomMetadata thirdPartySbomMetadata =
        tempEntity.createSbomMetadata(app.getId(), null, fileInWorkDirPath.getFileName().toString());

    grantWritePermission(thirdPartySbomMetadata.getApplicationId());

    apiSbomService.deleteSbomVersion(thirdPartySbomMetadata.getApplicationId(),
        thirdPartySbomMetadata.getSbomVersion());
  }
}
