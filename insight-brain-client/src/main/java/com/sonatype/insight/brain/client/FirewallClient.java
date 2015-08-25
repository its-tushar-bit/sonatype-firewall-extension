/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.client;

import java.io.IOException;

import com.sonatype.insight.client.utils.HttpClientUtils.Configuration;
import com.sonatype.insight.client.utils.Result;

import org.apache.http.client.HttpResponseException;

public class FirewallClient
    extends AbstractRequestClient
{

  private static final String SERVICE_PATH = "rest/integration/repositories";

  private final String repositoryManagerInstanceId;

  private final String repositoryPublicId;


  public FirewallClient(final Configuration config, final String repositoryManagerInstanceId,
      final String repositoryPublicId)
  {
    super(config);

    this.repositoryManagerInstanceId = repositoryManagerInstanceId;
    this.repositoryPublicId = repositoryPublicId;
  }

  public void enableRepository() throws IOException {
    Result result = postRequest(path(SERVICE_PATH, repositoryManagerInstanceId, repositoryPublicId),
        null);
    int status = result.status();
    if (status >= 300) {
      String msg = result.message();
      throw new HttpResponseException(status, msg);
    }
  }
}
