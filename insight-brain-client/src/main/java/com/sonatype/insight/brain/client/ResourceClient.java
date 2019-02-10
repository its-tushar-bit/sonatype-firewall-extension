/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.client;

import java.io.IOException;

import com.sonatype.clm.dto.model.Resource;
import com.sonatype.insight.client.utils.AbstractClient;
import com.sonatype.insight.client.utils.HttpClientUtils.Configuration;
import com.sonatype.insight.client.utils.Result;

import org.apache.http.client.HttpResponseException;

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

  public Resource getResource(String path) throws IOException {
    Result result = path(path).get();

    if (result.status() != 200) {
      throw new HttpResponseException(result.status(), result.message());
    }
    return new Resource(result.data(), result.header("Content-Type"));
  }
}
