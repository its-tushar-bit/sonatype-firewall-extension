/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.client;

import java.io.IOException;

import com.sonatype.clm.dto.model.callflowanalysis.ApiCallFlowAnalysisConfigDTO;
import com.sonatype.clm.dto.model.callflowanalysis.CallFlowAlgorithm;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.testing.AbstractBrainServiceIntegrationTest;
import com.sonatype.insight.client.utils.HttpClientUtils.Configuration;
import com.sonatype.insight.client.utils.SimpleAuthentication;

import org.apache.http.client.HttpResponseException;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatExceptionOfType;

public class CallFlowAnalysisConfigClientTest
    extends AbstractBrainServiceIntegrationTest
{
  @Test
  public void testGetAnalysisCallFlowConfig() throws IOException {
    CallFlowAnalysisConfigClient client = new CallFlowAnalysisConfigClient(getConfiguration());
    Application app = tempEntity.newApplicationWithParent("the-app-id");
    tempEntity.newCallFlowAnalysisConfig(app.getId(), 1);
    ApiCallFlowAnalysisConfigDTO dto = client.getAnalysisCallFlowConfig("application", "the-app-id");
    assertThat(dto.ownerId).isEqualTo(app.getId());
    assertThat(dto.threadCount).isEqualTo(1);
    assertThat(dto.algorithm).isEqualTo(CallFlowAlgorithm.CLASS_HIERARCHY_ANALYSIS);
  }

  @Test
  public void testGetAnalysisCallFlowConfig_NotFound() {
    CallFlowAnalysisConfigClient client = new CallFlowAnalysisConfigClient(getConfiguration());
    Application app = tempEntity.newApplicationWithParent("the-app-id");
    assertThatExceptionOfType(HttpResponseException.class).isThrownBy(() ->
            client.getAnalysisCallFlowConfig("application", "the-app-id"))
        .withMessage("Call Flow Analysis Config not found for ownerId " + app.getId())
        .satisfies(e -> assertThat(e.getStatusCode()).isEqualTo(404));
  }

  private Configuration getConfiguration() {
    Configuration config = getCLMServer().getClientConfiguration();
    SimpleAuthentication auth = new SimpleAuthentication();
    auth.setPassword("admin123");
    auth.setUsername("admin");
    config.setServerAuth(auth);
    return config;
  }
}
