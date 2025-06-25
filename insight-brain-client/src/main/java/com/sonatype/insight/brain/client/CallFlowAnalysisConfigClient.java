/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.client;

import java.io.IOException;

import com.sonatype.clm.dto.model.callflowanalysis.ApiCallFlowAnalysisConfigDTO;
import com.sonatype.insight.client.utils.HttpClientUtils.Configuration;
import com.sonatype.insight.client.utils.Result;

public class CallFlowAnalysisConfigClient extends AbstractRequestClient
{
  public static final String RESOURCE_PATH =
      "api/experimental/callFlowAnalysis/configuration";

  protected CallFlowAnalysisConfigClient(final Configuration config) {
    super(config);
  }

  public ApiCallFlowAnalysisConfigDTO getAnalysisCallFlowConfig(String ownerType, String ownerId)
      throws IOException
  {
    if ("application".equalsIgnoreCase(ownerType)) {
      ownerId = ApplicationIdUtils.normalizeApplicationPublicId(ownerId);
    }
    Result result = path(RESOURCE_PATH, ownerType, ownerId, "publicId").get();
    // 402 status is received when we get the "Your IQ Server license does not enable this feature" error from server.
    if (result != null && result.status() == 402) {
      throw new LicenseNotEnabledException();
    }
    return parseResult(result, ApiCallFlowAnalysisConfigDTO.class);
  }
}
