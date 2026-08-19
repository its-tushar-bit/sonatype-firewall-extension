/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.testsupport.wiremock;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;

/**
 * Reuse-safe, JUnit 5-native WireMock extension for stubbing external HTTP services in tests that run inside a
 * <em>reused</em> IQ server / Spring-context cohort (the {@code insight-brain-variant-test-*} modules) as well as in
 * plain unit tests.
 * <p>
 * Register it as a {@code static} field so the mock HTTP server boots <b>once</b> per test class and is reused across
 * every test method:
 *
 * <pre>
 * {@code
 * &#64;RegisterExtension
 * static ReusableWireMockExtension gitService = new ReusableWireMockExtension();
 *
 * &#64;Test
 * void stubsAreIsolatedPerTest() {
 *   gitService.stubFor(get("/repos").willReturn(ok()));
 *   String url = gitService.baseUrl();
 *   ...
 * }
 * }
 * </pre>
 *
 * Between test methods {@link WireMockExtension#beforeEach} calls {@code resetToDefaultMappings()}, which clears all
 * stubs, the request journal and scenario state, so there is no cross-test leakage even though the underlying server is
 * shared. A {@code static} registration therefore avoids restarting the mock server (and, more importantly, avoids
 * restarting the IQ server / Spring context) between tests.
 * <p>
 * The per-test reset relies on the default sequential execution of test methods within a class. The shared static
 * server is <b>not</b> safe if per-method parallel execution is enabled for the class (JUnit&nbsp;5
 * {@code junit.jupiter.execution.parallel.enabled}); concurrent methods would race on a single stub/journal state.
 * <p>
 * The server always binds to a dynamic (ephemeral) port, so concurrent CI agents and Surefire/Failsafe forks never
 * collide on a fixed port. Read the port/URL through {@link #getPort()} / {@link #baseUrl()} at run time rather than
 * hard-coding it.
 * <p>
 * This class is the generalized primitive for reuse-safe external-service mocks. Domain-specific mocks (Artifactory,
 * ZScaler, Crowd, GitHub, ...) can extend it and add their own {@code mock*} stubbing helpers on top of the inherited
 * {@link com.github.tomakehurst.wiremock.junit.DslWrapper DSL} methods, resetting any per-instance state they hold by
 * overriding {@link #onBeforeEach}.
 */
public class ReusableWireMockExtension
    extends WireMockExtension
{
  public ReusableWireMockExtension() {
    super(WireMockExtension.extensionOptions().options(wireMockConfig().dynamicPort()));
  }

  /**
   * For subclasses that need to add options (e.g. HTTPS, extra transformers) while keeping the reuse-safe, dynamic-port
   * defaults. Callers should start from {@link WireMockExtension#extensionOptions()} and
   * {@code .options(wireMockConfig().dynamicPort())} unless they deliberately need a fixed port.
   */
  protected ReusableWireMockExtension(Builder builder) {
    super(builder);
  }
}
