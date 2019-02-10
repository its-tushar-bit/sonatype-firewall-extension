/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.tools.urlrunner;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.sonatype.insight.brain.service.AbstractBrainServiceTest;
import com.sonatype.insight.brain.tools.common.PerfTestConfig;
import com.sonatype.insight.brain.tools.common.PerfTestConfig.RepeatConfig;
import com.sonatype.insight.brain.tools.common.PerfTestConfig.TestUrl;

import org.apache.http.impl.client.CloseableHttpClient;
import org.junit.Test;
import org.mockito.Mockito;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class UrlRunnerTest
    extends AbstractBrainServiceTest
{
  private static String serverUrl;

  private static String adminUrl;

  @Override
  public void initTest() throws Exception {
    super.initTest();
    serverUrl = getCLMServer().getClientConfiguration().getServerUrl();
    adminUrl = getCLMServer().getClientConfiguration().getServerAdminUrl();
  }

  @Test
  public void testReturnMinimum() throws Exception {
    UrlRunner urlRunner = spy(UrlRunner.class);
    urlRunner.run(getObjectGetRepeat(), serverUrl, "admin", "admin123", (it) -> {
    }, adminUrl, null);
    verify(urlRunner, times(3))
        .makeGetCall(Mockito.any(CloseableHttpClient.class), Mockito.any(TestUrl.class), Mockito.anyString(),
            Mockito.any());
  }

  @Test
  public void testReturnMaximum() throws Exception {
    UrlRunner urlRunner = spy(UrlRunner.class);

    doReturn(50000L).when(urlRunner).makeGetCall(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any());
    urlRunner.run(getObjectGetRepeat(), serverUrl, "admin", "admin123", null, adminUrl, null);
    verify(urlRunner, times(5)).makeSingleHttpCall(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any());
  }

  @Test
  public void testSuccessfulGet() throws Exception {
    UrlRunner urlRunner = new UrlRunner();
    List<Stats> statsList = new ArrayList<>();
    urlRunner.run(getObjectBasicGet(), serverUrl, "admin", "admin123", statsList::add, adminUrl, null);
    assertThat(statsList).isNotEmpty();
    assertThat(statsList.get(0).getStatusLine().getStatusCode()).isEqualTo(200);
  }

  @Test
  public void testUnsuccessfulGet() throws Exception {
    UrlRunner urlRunner = new UrlRunner();
    List<Stats> statsList = new ArrayList<>();
    urlRunner.run(getObjectBadGetUrl(), serverUrl, "admin", "admin123", statsList::add, null, null);
    assertThat(statsList).isNotEmpty();
    assertThat(statsList.get(0).getStatusLine().getStatusCode()).isNotEqualTo(200);
  }

  @Test
  public void testSuccessfulPost() throws Exception {
    UrlRunner urlRunner = new UrlRunner();
    List<Stats> statsList = new ArrayList<>();
    urlRunner.run(getObjectPostUrl(), serverUrl, "admin", "admin123", statsList::add, null, null);
    assertThat(statsList).isNotEmpty();
    assertThat(statsList.get(0).getStatusLine().getStatusCode()).isEqualTo(200);
  }

  @Test
  public void testUnsuccessfulPost() throws Exception {
    UrlRunner urlRunner = new UrlRunner();
    List<Stats> statsList = new ArrayList<>();
    urlRunner.run(getObjectPostBadUrl(), serverUrl, "admin", "admin123", statsList::add, null, null);
    assertThat(statsList).isNotEmpty();
    assertThat(statsList.get(0).getStatusLine().getStatusCode()).isNotEqualTo(200);
  }

  private PerfTestConfig getObjectGetRepeat() {
    PerfTestConfig config = new PerfTestConfig();
    PerfTestConfig.TestUrl testUrl = new PerfTestConfig.TestUrl();
    testUrl.setUrl("rest/application/services/summary");
    testUrl.setType("GET");
    PerfTestConfig.RepeatConfig repeat = new RepeatConfig();
    repeat.setIfLongerThan(1000L);
    repeat.setMinRuns(3);
    repeat.setMaxRuns(5);
    testUrl.setRepeat(repeat);
    config.setUrls(Collections.singletonList(testUrl));
    return config;
  }

  private PerfTestConfig getObjectBasicGet() {
    PerfTestConfig config = new PerfTestConfig();
    PerfTestConfig.TestUrl testUrl = new PerfTestConfig.TestUrl();
    testUrl.setUrl("rest/tag/application");
    testUrl.setType("GET");
    config.setUrls(Collections.singletonList(testUrl));
    return config;
  }

  private PerfTestConfig getObjectBadGetUrl() {
    PerfTestConfig config = new PerfTestConfig();
    PerfTestConfig.TestUrl testUrl = new PerfTestConfig.TestUrl();
    testUrl.setUrl("rest/tag/applicationn");
    testUrl.setType("GET");
    config.setUrls(Collections.singletonList(testUrl));
    return config;
  }

  private PerfTestConfig getObjectPostUrl() {
    PerfTestConfig config = new PerfTestConfig();
    PerfTestConfig.TestUrl testUrl = new PerfTestConfig.TestUrl();
    testUrl.setUrl("rest/dashboard/policy/componentRisks");
    testUrl.setType("POST");
    String payloadString = "{\"orderBy\":\"-TOTAL_RISK\",\"maxResults\":101,\"organizationIds\":[],\"applicationIds\"" +
        ":[],\"stageIds\":[],\n    \"tagIds\":[],\"policyViolationStates\":[\"OPEN\"],\"maxDaysOld\":30,\"" +
        "policyThreatLevelRange\":\"2,10\"}\n    }";
    testUrl.setPayload(payloadString);
    config.setUrls(Collections.singletonList(testUrl));
    return config;
  }

  private PerfTestConfig getObjectPostBadUrl() {
    PerfTestConfig config = new PerfTestConfig();
    PerfTestConfig.TestUrl testUrl = new PerfTestConfig.TestUrl();
    testUrl.setUrl("rest/dashboard/policy/componentRiskss");
    testUrl.setType("POST");
    String payloadString = "{\"orderBy\":\"-TOTAL_RISK\",\"maxResults\":101,\"organizationIds\":[],\"applicationIds\"" +
        ":[],\"stageIds\":[],\n    \"tagIds\":[],\"policyViolationStates\":[\"OPEN\"],\"maxDaysOld\":30,\"" +
        "policyThreatLevelRange\":\"2,10\"}\n    }";
    testUrl.setPayload(payloadString);
    config.setUrls(Collections.singletonList(testUrl));
    return config;
  }
}
