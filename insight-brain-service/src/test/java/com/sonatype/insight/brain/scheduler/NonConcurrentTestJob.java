/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.scheduler;

import jakarta.inject.Named;

import org.quartz.DisallowConcurrentExecution;

@Named
@DisallowConcurrentExecution
public class NonConcurrentTestJob
    extends TestJob
{
  public static final String NAME = "NonConcurrentTestJob";

  @Override
  public String getJobName() {
    return NAME;
  }
}
