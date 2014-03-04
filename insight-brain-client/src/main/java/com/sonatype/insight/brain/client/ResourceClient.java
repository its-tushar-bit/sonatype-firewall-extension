/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.client;

import java.io.IOException;

import com.sonatype.insight.client.utils.AbstractClient;
import com.sonatype.insight.client.utils.HttpClientUtils.Configuration;
import com.sonatype.insight.client.utils.Result;

/**
 * Used to access arbitrary CLM resources
 *
 * @since 1.10
 */
public class ResourceClient
    extends AbstractClient
{

  public ResourceClient(Configuration config) {
    super(config);
  }

  public Result getResource(String path) throws IOException {
    return path(path).get();
  }
}
