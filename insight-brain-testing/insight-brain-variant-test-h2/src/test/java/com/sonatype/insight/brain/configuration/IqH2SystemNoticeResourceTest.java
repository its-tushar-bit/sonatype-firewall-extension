/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.configuration;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.model.configuration.SystemNotice;
import com.sonatype.insight.brain.variant.IqH2Test;
import com.sonatype.insight.brain.variant.IqTestContext;

import org.apache.http.HttpStatus;
import org.junit.jupiter.api.Test;

/**
 * Kept in {@code SystemNoticeResource}'s own package (rather than {@code com.sonatype.insight.brain.variant})
 * because this test accesses the package-private {@link SystemNoticeResource#FETCH_PATH}.
 */
@IqH2Test
class IqH2SystemNoticeResourceTest
{
  private IqTestContext ctx;

  private HttpRequest restRequest() {
    return ctx.restRequest().path(SystemNoticeResource.RESOURCE_PATH);
  }

  @Test
  void testGetSystemNotice_asAnonymous() throws Exception {
    HttpRequest request = restRequest();
    HttpResponse response = request.path(SystemNoticeResource.FETCH_PATH).anon().get();
    ctx.assertResponseStatus(HttpStatus.SC_OK, response);
  }

  @Test
  void testUpdateSystemNotice() throws Exception {
    HttpRequest request = restRequest();
    HttpResponse response = request.body(new SystemNotice()).put();
    ctx.assertResponseStatus(HttpStatus.SC_OK, response);
  }

  @Test
  void testGetSystemNotice_Unlicensed() throws Exception {
    ctx.uninstallLicense();
    HttpRequest request = restRequest();
    HttpResponse response = request.path(SystemNoticeResource.FETCH_PATH).get();
    ctx.assertResponseStatus(HttpStatus.SC_OK, response);
  }
}
