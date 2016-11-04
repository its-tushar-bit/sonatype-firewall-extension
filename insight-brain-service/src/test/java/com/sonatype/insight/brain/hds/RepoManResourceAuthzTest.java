/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.hds;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.service.AbstractResourceAuthzTest;
import com.sonatype.insight.brain.service.InsightConfig;
import com.sonatype.insight.brain.service.TestInsightBrainService.Configurator;

import org.junit.Test;

public class RepoManResourceAuthzTest
    extends AbstractResourceAuthzTest
{
  @Test
  public void testUploadScan() throws Exception {
    grantPermission(app.getId(), Permission.EVALUATE_APPLICATION);
    testAuthzPut(scanRequest());
  }

  @Test
  public void testUploadScan_UnauthorizedAnonymousNotAllowed() throws Exception {
    HttpResponse response = scanRequest().anon().put();
    assertResponseStatus(401, response);
  }

  @Test
  @ManualServerInit
  public void testUploadScan_UnauthorizedAnonymousAllowed_AnonymousClientAccessAllowed() throws Exception {
    initServer(new Configurator() {
      @Override
      public void configure(final InsightConfig config) {
        config.setAnonymousClientAccessAllowed(true);
      }
    });
    HttpResponse response = scanRequest().anon().put();
    assertResponseStatus(200, response);
  }

  @Test
  public void testUploadScan_Unauthorized() throws Exception {
    HttpResponse response = scanRequest().auth("unknownUser", "unknownPassword").put();
    assertResponseStatus(401, response);
  }

  private HttpRequest scanRequest() {
    return restRequest().path(RepoManResource.RESOURCE_PATH, RepoManResource.SCAN_PATH).parameter(app.getPublicId());
  }
}
