/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.db;

import com.sonatype.insight.brain.service.InsightBrainService;
import com.sonatype.insight.brain.service.InsightConfig;

import io.dropwizard.setup.Environment;

/**
 * <p>
 * Any Dropwizard class which requires database support should implement this interface.
 * </p>
 * <p>
 * For example any Dropwizard 'application' (i.e. something that extends {@link io.dropwizard.Application}) or
 * Dropwizard command (i.e. extends {@link io.dropwizard.cli.Command}). It is recommended to read the Dropwizard <a
 * href="https://www.dropwizard.io/en/latest/manual/internals.html?highlight=ServerCommand#startup-sequence">Startup
 * Sequence</a> docs to understand this a bit more. The nuanced part is that a normal Dropwizard http application will
 * extend {@link io.dropwizard.Application} and then add a {@link io.dropwizard.cli.Command} which specifically extends
 * {@link io.dropwizard.cli.ServerCommand}. Then extra CLI commands will extend something like
 * {@link io.dropwizard.cli.ConfiguredCommand}. The nuance is the application (in our case {@link InsightBrainService})
 * has an inline run method {@link InsightBrainService#run(String...)} which is the entry point for EVERYTHING but also
 * a {@link InsightBrainService#run(InsightConfig, Environment)} which is the command itself, but implement in
 * {@link InsightBrainService} instead of the {@link io.dropwizard.cli.Command} class like the others.
 * </p>
 * <p>
 * The bottom line is that each individual command needs to create the {@link DatabaseContainer} <strong>AFTER</strong>
 * it has the {@link InsightConfig} available. Despite the 'inside-out' difference between the main HTTP server command
 * the all the other commands.
 * </p>
 */
public interface DatabaseContainerSupport
{
  /**
   * Create the {@link DatabaseContainer} for the application/command
   */
  DatabaseContainer createDatabaseContainer(final InsightConfig insightConfig);
}
