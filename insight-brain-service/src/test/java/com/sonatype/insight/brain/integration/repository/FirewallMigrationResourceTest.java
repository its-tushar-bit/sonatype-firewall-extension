/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.integration.repository;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.service.AbstractResourceTest;

import org.apache.http.HttpStatus;
import org.junit.Test;

import static com.sonatype.insight.brain.integration.repository.FirewallMigrationService.PROTOCOL_V1;

/**
 * @since 1.28
 */
public class FirewallMigrationResourceTest
    extends AbstractResourceTest
{
  @Override
  protected HttpRequest restRequest() {
    return super.restRequest().path(FirewallMigrationResource.RESOURCE_PATH);
  }

  private HttpRequest supportedRequest() {
    return restRequest().path(FirewallMigrationResource.SUPPORTED_PATH);
  }

  @Test
  public void testVerifyMigrationSupported() throws Exception {
    HttpResponse response = supportedRequest().parameter(PROTOCOL_V1).get();
    assertResponseStatus(HttpStatus.SC_NO_CONTENT, response);
  }
}
