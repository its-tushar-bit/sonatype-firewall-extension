/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.artifactory.client;

import java.io.IOException;

import javax.ws.rs.core.Response.StatusType;

/**
 * @since 1.136
 */
public interface ArtifactoryClient
{
  ArtifactoryChecksumSearchResults searchByChecksum(ChecksumType checksumType, String checksum) throws IOException;

  StatusType getServerStatus() throws IOException;
}
