/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.client;

import java.io.IOException;

import com.sonatype.insight.client.utils.AbstractClient;
import com.sonatype.insight.client.utils.HttpClientUtils.Configuration;
import com.sonatype.insight.client.utils.Result;

/**
 * Used to access SourceControl API
 *
 * @since 1.72
 */
public class SourceControlClient
    extends AbstractClient
{
  public SourceControlClient(Configuration config) {
    super(config);
  }

  public int addOrUpdateSourceControlRecord(String publicId, String repositoryUrl) throws IOException {
    Result result = path("api", "v2", "sourceControl")
        .query("publicId", publicId, "repositoryUrl", repositoryUrl)
        .post(null);
    return result.status();
  }
}
