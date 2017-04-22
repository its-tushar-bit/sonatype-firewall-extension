/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
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
import org.apache.http.client.HttpResponseException;

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

  protected Result deleteRequest(RequestBuilder builder) throws IOException {
    try {
      return builder.delete();
    }
    catch (UnknownHostException e) {
      // improve error msg
      throw (IOException) new UnknownHostException("Unknown host: " + e.getMessage()).initCause(e);
    }
  }

  protected void verifyStatusCode(Result result) throws IOException {
    int status = result.status();
    if (status >= 300) {
      String msg = result.message();
      throw new HttpResponseException(status, msg);
    }
  }
}
