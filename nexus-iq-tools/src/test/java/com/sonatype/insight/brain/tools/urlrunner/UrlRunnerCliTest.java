/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.tools.urlrunner;

import java.util.ArrayList;
import java.util.List;

import com.sonatype.insight.brain.service.AbstractBrainServiceTest;

import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.spy;

public class UrlRunnerCliTest
    extends AbstractBrainServiceTest
{
  @Test
  public void testRunEndToEnd() throws Exception {
    String serverUrl = getCLMServer().getClientConfiguration().getServerUrl();
    List<Stats> results = new ArrayList<>();
    String[] args = new String[]{
        "-f", "src/test/resources/TestInputFile1.json", "-s", serverUrl
    };
    UrlRunnerCli urlRunnerCli = spy(UrlRunnerCli.class);
    doAnswer((Answer<Void>) invocation -> {
      results.add(invocation.getArgument(0));
      return null;
    }).when(urlRunnerCli).printStats(Mockito.any());
    urlRunnerCli.run(args);

    // all but the last one should succeed
    assertThat(results).hasSize(8).filteredOn(result -> result.getStatusLine().getStatusCode() != 200)
        .extracting(Stats::getUrl).containsExactly("rest/dashboard/policy/applicationRiskss");
  }
}
