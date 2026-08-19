/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.continuousmonitoring;

/**
 * Discriminator for rows in the shared {@code continuous_monitoring_queue} table. Each value
 * corresponds to a distinct producer/consumer pair and (where applicable) its own per-flow detail
 * (satellite) table. v1 ships HOSTED_REPO; SBOM and LIFECYCLE follow as mechanical migrations.
 */
public enum ContinuousMonitoringFlowType
{
  HOSTED_REPO,
  SBOM,
  LIFECYCLE
}
