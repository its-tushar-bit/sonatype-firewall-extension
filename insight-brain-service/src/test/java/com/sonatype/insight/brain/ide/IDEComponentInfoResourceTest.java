/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.ide;

import java.time.Duration;

import org.junit.experimental.categories.Category;
import com.sonatype.insight.brain.common.test.SlowTest;

import com.sonatype.clm.dto.model.component.NamedComponentDetails;
import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.hds.AbstractComponentInfoResourceTest;
import com.sonatype.insight.brain.hds.IdeComponentDetailsHdsClient;
import com.sonatype.insight.brain.model.component.MatchState;

import org.eclipse.jetty.http.HttpStatus;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static com.sonatype.insight.brain.hds.ComponentInfoResourceTestUtils.convertToHdsUrl;

public class IDEComponentInfoResourceTest
    extends AbstractComponentInfoResourceTest
{
  @Override
  protected String getResourcePath() {
    return IDEComponentInfoResource.RESOURCE_PATH;
  }

  @Before
  @After
  public void resetCircuitBreaker() {
    // The circuit breaker is a field on a singleton IdeComponentDetailsHdsClient bean, shared across
    // all tests in this JVM fork (reuseForks=true). Reset it to closed before/after each test so a
    // failure in one test doesn't poison subsequent ones. Must use lookup() — field @Inject is never
    // populated on AbstractBaseIntegrationTest subclasses (no injectMembers on the JUnit instance).
    IdeComponentDetailsHdsClient ideHdsClient = lookup(IdeComponentDetailsHdsClient.class);
    if (ideHdsClient != null) {
      ideHdsClient.getCircuitBreaker().recordSuccess();
    }
  }

  @Test
  public void testGetComponentDetails() throws Exception {
    super.testGetComponentDetails_EvaluateComponentPermission();
  }

  @Test
  public void testGetComponentDetailsList() throws Exception {
    super.testGetComponentDetailsList_EvaluateComponentPermission();
  }

  /**
   * Fast test proving the resource is wired to the breaker-protected client (not SlowTest, runs on PR builds).
   * A wiring regression (e.g., constructor no longer sets @Named("ideComponentDetails") client) would let
   * this test fail: 5+ HDS failures would not trip the breaker because the resource would be using the
   * default unprotected HdsClient instead.
   */
  @Test
  public void testGetComponentDetails_hdsFailuresTripCircuitBreaker() throws Exception {
    HttpRequest request = detailsRequest(getOwnerId(), MAVEN_COORDINATES, "hash", MatchState.SIMILAR, false);
    hdsRespondWith("service unavailable").atUri(convertToHdsUrl(request.getUrl())).andStatus(503);

    // 5 requests (each retried once, so 10 servlet hits) trip the 5-consecutive-failure threshold.
    // BadGatewayException surfaces as 502.
    for (int i = 0; i < 5; i++) {
      HttpResponse response = request.get();
      assertResponseStatus(HttpStatus.BAD_GATEWAY_502, response);
    }

    // Breaker is now open: the next call fails fast with 504.
    HttpResponse response = request.get();
    assertResponseStatus(HttpStatus.GATEWAY_TIMEOUT_504, response);
    // Note: breaker will be reset in @After
  }

  /**
   * Full recovery test: proves the breaker closes after cooldown + successful probe.
   * Tagged SlowTest because it waits 35s for the 30s cooldown.
   */
  @Test
  @Category(SlowTest.class)
  public void testGetComponentDetails_hdsFailuresOpenCircuitBreakerAndRecover() throws Exception {
    HttpRequest request = detailsRequest(getOwnerId(), MAVEN_COORDINATES, "hash", MatchState.SIMILAR, false);
    hdsRespondWith("service unavailable").atUri(convertToHdsUrl(request.getUrl())).andStatus(503);

    // 5 requests trip the breaker (same as fast test above).
    for (int i = 0; i < 5; i++) {
      HttpResponse response = request.get();
      assertResponseStatus(HttpStatus.BAD_GATEWAY_502, response);
    }

    // Breaker is open.
    HttpResponse response = request.get();
    assertResponseStatus(HttpStatus.GATEWAY_TIMEOUT_504, response);

    // Wait out the 30s cooldown, then let a successful probe through.
    hdsRespondWith(new NamedComponentDetails()).atUri(convertToHdsUrl(request.getUrl()));
    Thread.sleep(Duration.ofSeconds(35).toMillis());
    assertResponseStatus(HttpStatus.OK_200, request.get());
  }
}
