/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.tools.urlrunner;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.sonatype.insight.brain.service.AbstractBrainServiceTest;

import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
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

    assertThat(results, hasSize(8));
    // all but the last one should succeed
    List<Stats> failedCalls = results.stream()
        .filter(result -> result.getResponse().getStatusLine().getStatusCode() != 200).collect(Collectors.toList());

    assertThat(failedCalls, hasSize(1));
    assertThat(failedCalls.get(0).getUrl(), is("rest/dashboard/policy/applicationRiskss"));
  }
}
