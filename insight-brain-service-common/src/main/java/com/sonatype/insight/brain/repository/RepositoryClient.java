/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.repository;

import java.io.IOException;
import java.util.Map;
import javax.ws.rs.core.Response.Status;

/**
 * @since 1.127
 */
public interface RepositoryClient
{
  RepositoryAllVersionsResponse getAllVersions(Map<String, String> queryParams) throws IOException;

  Status getServerStatus() throws IOException;
}
