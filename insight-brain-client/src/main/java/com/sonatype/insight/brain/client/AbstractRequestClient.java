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
import com.sonatype.insight.json.store.JsonUtils;

public abstract class AbstractRequestClient
    extends AbstractClient
{
  protected AbstractRequestClient(final Configuration config) {
    super(config);
  }

  protected <T> T parseResult(Result result, Class<T> type) throws IOException {
    verifyStatusCode(result);
    String json = result.text();
    try {
      return json != null ? JsonUtils.parse(json, type) : null;
    }
    catch (IOException e) {
      throw new IOException("Could not parse: " + json, e);
    }
  }
}
