/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.variant;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.sonatype.insight.brain.service.TestCLMServer;
import com.sonatype.insight.brain.service.TestInsightBrainService.Configurator;

import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Shared machinery behind the three self-documenting variant meta-annotations. It boots the proven
 * {@link TestCLMServer}/{@code SpringTestInsightBrainService} launcher <b>once per variant</b> and
 * reuses that running server (and its Spring context) across every test of the variant, injecting a
 * {@link SpikeRestClient} bound to the server port. This is what replaces "restart the server per
 * test": the server is keyed by {@link #variantKey()} in a JVM-wide cache and stopped by a shutdown
 * hook when the fork exits.
 *
 * <p>
 * Concrete subclasses supply the variant differences (which database fixture, which service
 * factory, which configurator, and any per-variant post-start/per-test hooks). There is no test
 * base class to extend — the subclass extension is wired through the meta-annotation instead.
 */
public abstract class AbstractSpikeServerExtension
    implements BeforeEachCallback, AfterEachCallback
{
  private static final Logger log = LoggerFactory.getLogger(AbstractSpikeServerExtension.class);

  private static final Map<String, ServerHandle> SERVERS = new ConcurrentHashMap<>();

  static {
    Runtime.getRuntime().addShutdownHook(new Thread(AbstractSpikeServerExtension::stopAll, "spike-server-shutdown"));
  }

  /** Immutable per-variant server record. */
  public record ServerHandle(TestCLMServer server, int port, SpikeRestClient rest)
  {
  }

  @Override
  public void beforeEach(final ExtensionContext context) throws Exception {
    ServerHandle handle = SERVERS.computeIfAbsent(variantKey(), key -> startServer());
    beforeEachTest(context, handle);
    injectRestClient(context.getRequiredTestInstance(), handle.rest());
  }

  @Override
  public void afterEach(final ExtensionContext context) {
    ServerHandle handle = SERVERS.get(variantKey());
    if (handle != null) {
      afterEachTest(context, handle);
    }
  }

  private ServerHandle startServer() {
    try {
      var container = provisionDatabase();
      // TestCLMServer's second positional arg is isProxyRequiredToReachHds; the reused variant
      // servers reach the mocked HDS directly, so no proxy is required.
      boolean proxyRequiredToReachHds = false;
      TestCLMServer server =
          new TestCLMServer(serviceFactory(), proxyRequiredToReachHds, testConfigurations(), configurator(), container);
      server.start();
      int port = server.getCLMServer().getPort();
      SpikeRestClient rest = new SpikeRestClient("http://localhost:" + port);
      ServerHandle handle = new ServerHandle(server, port, rest);
      afterServerStarted(handle);
      log.info("Spike variant '{}' server booted on port {}", variantKey(), port);
      return handle;
    }
    catch (Exception e) {
      throw new IllegalStateException("Failed to boot spike server for variant " + variantKey(), e);
    }
  }

  private static void injectRestClient(final Object testInstance, final SpikeRestClient rest) {
    for (Class<?> type = testInstance.getClass(); type != null && type != Object.class; type = type.getSuperclass()) {
      for (Field field : type.getDeclaredFields()) {
        if (SpikeRestClient.class.isAssignableFrom(field.getType())) {
          try {
            field.setAccessible(true);
            field.set(testInstance, rest);
          }
          catch (IllegalAccessException e) {
            throw new IllegalStateException("Could not inject SpikeRestClient into " + field, e);
          }
        }
      }
    }
  }

  private static void stopAll() {
    for (ServerHandle handle : SERVERS.values()) {
      try {
        handle.server().stop();
      }
      catch (Throwable e) {
        log.warn("Error stopping spike server: {}", e.getMessage());
      }
    }
    SERVERS.clear();
  }

  // --- variant hooks -------------------------------------------------------------------------

  /** Stable cache key. Distinct keys → distinct cached servers/contexts (exactly one per variant). */
  protected abstract String variantKey();

  /** Provision (and cache) the database fixture for this variant, returning its DatabaseContainer. */
  protected abstract com.sonatype.insight.brain.db.DatabaseContainer provisionDatabase();

  protected abstract com.sonatype.insight.brain.testing.InsightBrainServiceFactory serviceFactory();

  protected abstract Configurator configurator();

  protected abstract List<Class<?>> testConfigurations();

  /** Optional per-variant setup after the server is up (e.g. base URL, tenant provisioning). */
  protected void afterServerStarted(final ServerHandle handle) throws Exception {
  }

  /** Optional per-test setup on the reused server (e.g. establish tenant context). */
  protected void beforeEachTest(final ExtensionContext context, final ServerHandle handle) throws Exception {
  }

  /** Optional per-test teardown that resets mutable state without restarting the server. */
  protected void afterEachTest(final ExtensionContext context, final ServerHandle handle) {
  }
}
