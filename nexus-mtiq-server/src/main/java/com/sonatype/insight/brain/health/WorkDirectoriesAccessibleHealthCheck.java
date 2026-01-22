/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.health;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.operational.check.AbstractOperationalCheck;
import com.sonatype.insight.brain.service.InsightConfig;

import com.google.common.annotations.VisibleForTesting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Health check for clustered IQ to verify the work directories are accessible. This health check runs in a separate
 * thread with a 5 second timeout.
 * <p>
 * Note: Currently this check is in the MTIQ module, but it can be moved up to on-prem to replace the current ones.
 */
@Named
@Singleton
public class WorkDirectoriesAccessibleHealthCheck
    extends AbstractOperationalCheck
{
  private static final Logger log = LoggerFactory.getLogger(WorkDirectoriesAccessibleHealthCheck.class);

  private final File sonatypeWork;

  private final File clusterDirectory;

  private final ExecutorService executorService = Executors.newSingleThreadExecutor();

  static final Integer TIMEOUT_IN_SECONDS = 5;

  @Inject
  public WorkDirectoriesAccessibleHealthCheck(final InsightConfig insightConfig) {
    super("work-directories");
    this.sonatypeWork = insightConfig.getSonatypeWork();
    this.clusterDirectory = insightConfig.getClusterDirectory();
  }

  @Override
  protected Result check() throws Exception {
    ResultBuilder resultBuilder = Result.builder();

    isDirectoryInaccessible("sonatypeWork", sonatypeWork, resultBuilder);
    isDirectoryInaccessible("clusterDirectory", clusterDirectory, resultBuilder);

    return resultBuilder.build();
  }

  private void isDirectoryInaccessible(
      final String configName,
      final File directory,
      final ResultBuilder resultBuilder)
  {
    Callable<Void> callable = () -> readTestFile(directory);
    Future<Void> task = executorService.submit(callable);
    try {
      task.get(TIMEOUT_IN_SECONDS, TimeUnit.SECONDS);
    }
    catch (ExecutionException | InterruptedException | TimeoutException e) {
      if (e instanceof InterruptedException) {
        Thread.currentThread().interrupt();
      }

      String message = String.format(
          "Work directory '%s' failed accessibility check. Directory being checked: '%s'. Type: %s, Message: %s",
          configName, directory.getAbsolutePath(), e.getClass().getSimpleName(), e.getMessage());
      log.error(message, e);

      resultBuilder.withMessage(message).unhealthy();
    }
  }

  @VisibleForTesting
  Void readTestFile(final File dir) throws IOException {
    Path path = Path.of(dir.getAbsolutePath());
    Files.readAttributes(path, BasicFileAttributes.class);
    return null;
  }
}
