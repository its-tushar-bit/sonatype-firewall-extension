/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.tenancy;

/**
 * This is a marker interface for MTIQ. If this interface is present then this job will only be run on a Mtiq Batch
 * instance of MTIQ and not the main instances
 */
public interface MtiqBatchJob
{
}
