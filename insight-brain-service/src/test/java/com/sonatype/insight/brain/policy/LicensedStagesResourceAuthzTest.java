/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.service.AbstractResourceAuthzTest;
import com.sonatype.insight.brain.service.InsightConfig;
import com.sonatype.insight.brain.service.TestInsightBrainService.Configurator;

import org.junit.Test;

public class LicensedStagesResourceAuthzTest
    extends AbstractResourceAuthzTest
{
  @Override
  protected HttpRequest restRequest() {
    return super.restRequest().path(LicensedStagesResource.RESOURCE_PATH);
  }

  @Test
  public void testGet_AnonymousNotAllowed() throws Exception {
    HttpResponse response = restRequest().anon().get();
    assertResponseStatus(401, response);
  }

  @Test
  @ManualServerInit
  public void testGet_AnonymousAllowed() throws Exception {
    initServer(new Configurator() {
      @Override
      public void configure(final InsightConfig config) {
        config.setAnonymousClientAccessAllowed(true);
      }
    });
    HttpResponse response = restRequest().anon().get();
    assertResponseStatus(200, response);
  }

  @Test
  public void testGet_UnauthenticatedUserNotAllowed() throws Exception {
    HttpResponse response = restRequest().auth("unknownUser", "unknownPassword").get();
    assertResponseStatus(401, response);
  }
}
