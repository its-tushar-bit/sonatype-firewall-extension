/*
 * Copyright (c) 2011-2015 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.client;

import java.io.IOException;
import java.net.UnknownHostException;

import com.sonatype.insight.client.utils.AbstractClient;
import com.sonatype.insight.client.utils.HttpClientUtils.Configuration;
import com.sonatype.insight.client.utils.Result;

import org.apache.http.HttpEntity;

public abstract class AbstractRequestClient
    extends AbstractClient
{
  protected AbstractRequestClient(final Configuration config) {
    super(config);
  }

  protected Result postRequest(RequestBuilder builder, HttpEntity entity) throws IOException {
    try {
      return builder.post(entity);
    }
    catch (UnknownHostException e) {
      // improve error msg
      throw (IOException) new UnknownHostException("Unknown host: " + e.getMessage()).initCause(e);
    }
  }

  protected Result getRequest(RequestBuilder builder) throws IOException {
    try {
      return builder.get();
    }
    catch (UnknownHostException e) {
      // improve error msg
      throw (IOException) new UnknownHostException("Unknown host: " + e.getMessage()).initCause(e);
    }
  }
}
