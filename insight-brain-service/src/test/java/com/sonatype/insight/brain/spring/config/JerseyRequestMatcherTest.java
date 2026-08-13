/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.spring.config;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.ws.rs.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

public class JerseyRequestMatcherTest
{
  private final JerseyRequestMatcher matcher = JerseyRequestMatcher.fromComponents(List.of(
      new LandingTestResource(),
      new VersionTestResource(),
      new ApplicationsTestResource(),
      new IdeTestResource(),
      new NonResourceBean()));

  @Test
  public void shouldMatchRegisteredJaxRsRoutes() {
    assertThat(matcher.matches("/")).isTrue();
    assertThat(matcher.matches("/rest/product/version")).isTrue();
    assertThat(matcher.matches("/api/v2/applications")).isTrue();
    assertThat(matcher.matches("/brain/some/path")).isTrue();
  }

  @Test
  public void shouldNotMatchNonJaxRsInfrastructureRoutes() {
    assertThat(matcher.matches("/assets/index.html")).isFalse();
    assertThat(matcher.matches("/actuator/health")).isFalse();
    assertThat(matcher.matches("/ping")).isFalse();
    assertThat(matcher.matches("/healthcheck/database")).isFalse();
    assertThat(matcher.matches("/tasks/backupDb")).isFalse();
  }

  @Test
  public void shouldRequirePathSegmentBoundaries() {
    assertThat(matcher.matches("/restful")).isFalse();
    assertThat(matcher.matches("/apiary")).isFalse();
    assertThat(matcher.matches("/brainiac")).isFalse();
  }

  @Path("")
  static class LandingTestResource
  {
  }

  @Path("rest/product/version")
  static class VersionTestResource
  {
  }

  @Path("api/v2/applications")
  static class ApplicationsTestResource
  {
  }

  @Path("brain/{path:.*}")
  static class IdeTestResource
  {
  }

  static class NonResourceBean
  {
  }
}
