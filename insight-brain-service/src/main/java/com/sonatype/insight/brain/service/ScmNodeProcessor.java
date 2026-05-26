/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

import jakarta.inject.Named;
import jakarta.inject.Singleton;

/**
 * Default implementation allows all nodes to run SCM event and pull request polling processes
 */
@Named
@Singleton
public class ScmNodeProcessor
{
  public boolean shouldRun() {
    return true;
  }
}
