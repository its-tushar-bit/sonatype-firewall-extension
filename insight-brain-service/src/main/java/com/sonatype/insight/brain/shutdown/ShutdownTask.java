/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.shutdown;

import com.sonatype.insight.brain.service.Configuration;
import com.sonatype.insight.brain.service.AdminTask;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import java.io.PrintWriter;
import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * Task for triggering server shutdown.
 * Previously extended Dropwizard's Task class.
 * With Spring Boot, shutdown is handled via Actuator endpoints.
 */
@Named
@Singleton
public class ShutdownTask
    extends AdminTask
{
  public static final String PATH = "shutdown";

  private static final Duration TIMEOUT_BUFFER = Duration.ofMinutes(10);

  static final String SKIP_SYSTEM_EXIT_QUERY_PARAM = "skipSystemExit";

  private final Configuration configuration;

  private final ShutdownHandler shutdownHandler;

  @Inject
  public ShutdownTask(final Configuration configuration, final ShutdownHandler shutdownHandler) {
    super(PATH);
    this.configuration = configuration;
    this.shutdownHandler = shutdownHandler;
  }

  /**
   * Execute the shutdown task.
   *
   * @param parameters the query parameters
   */
  @Override
  public void execute(final Map<String, List<String>> parameters, final PrintWriter output) throws Exception {
    List<String> params = parameters.get(SKIP_SYSTEM_EXIT_QUERY_PARAM);
    boolean skipSystemExit = params != null && params.size() == 1 && params.contains("true");
    shutdownHandler.trigger(getTimeout(), skipSystemExit);
  }

  private Duration getTimeout() {
    return Duration
        .ofSeconds(configuration.getReportTimeoutInSeconds())
        .plus(TIMEOUT_BUFFER);
  }
}
