/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.shutdown;

import java.io.PrintWriter;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import com.sonatype.insight.brain.service.Configuration;

import io.dropwizard.servlets.tasks.Task;

@Named
@Singleton
public class ShutdownTask
    extends Task
{
  private static final String PATH = "shutdown";

  private static final Duration TIMEOUT_BUFFER = Duration.ofMinutes(10);

  private final Configuration configuration;

  private final ShutdownHandler shutdownHandler;

  @Inject
  public ShutdownTask(final Configuration configuration, final ShutdownHandler shutdownHandler) {
    super(PATH);
    this.configuration = configuration;
    this.shutdownHandler = shutdownHandler;
  }

  @Override
  public void execute(final Map<String, List<String>> parameters, final PrintWriter printWriter) throws Exception {
    shutdownHandler.trigger(getTimeout());
  }

  private Duration getTimeout() {
    return Duration
        .ofSeconds(configuration.getReportTimeoutInSeconds())
        .plus(TIMEOUT_BUFFER);
  }
}
