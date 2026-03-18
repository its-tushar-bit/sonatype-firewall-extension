/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.hds;

/**
 * Represents an IQ integration version with name and version information.
 * Used for integration version validation before sending scans to HDS.
 */
public record IqIntegrationVersion(String name, String version)
{
}
