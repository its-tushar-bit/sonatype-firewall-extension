/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

import io.dropwizard.lifecycle.Managed;

/**
 * Responsible for setting up (on startup) and tearing down (on shutdown) quartz jobs.
 */
public interface QuartzJobInitializer
    extends Managed
{
}
