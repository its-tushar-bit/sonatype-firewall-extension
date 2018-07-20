/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.tools.dbutil;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import com.sonatype.insight.brain.tools.common.PerfTestConfig.TestUrl;

import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.core.IsCollectionContaining.hasItem;

public class ListReplacerTest
{
  private final List<String> appList = Arrays.asList("app1", "App2", "apP3", "APP4");

  private static final String BASE_PAYLOAD1 = "{\"orderBy\": \"-AGE,-THREAT_LEVEL\",\"maxResults\": 101," +
      "\"organizationIds\": [],\"applicationIds\": [";

  private static final String BASE_PAYLOAD2 = "],\"stageIds\": []}";

  @Test
  public void testGenerateUrls_basicFormula() {
    String payload = getBasePayload("\"{appIdList(0,1,MAX-1,MAX)}\"");
    ListReplacer replacer = new ListReplacer("{appIdList", appList, 4);
    TestUrl testUrl = getBasicTestUrl(payload);
    List<TestUrl> testUrls = replacer.generateUrls(testUrl);
    List<String> allPayloads = testUrls.stream().map(TestUrl::getPayload).collect(Collectors.toList());

    assertThat(allPayloads, hasItem(BASE_PAYLOAD1 + BASE_PAYLOAD2));
    assertThat(allPayloads, hasItem(BASE_PAYLOAD1 + buildReplacement(appList.subList(0, 1)) + BASE_PAYLOAD2));
    assertThat(allPayloads, hasItem(BASE_PAYLOAD1 + buildReplacement(appList.subList(0, 3)) + BASE_PAYLOAD2));
    assertThat(allPayloads, hasItem(BASE_PAYLOAD1 + buildReplacement(appList.subList(0, 4)) + BASE_PAYLOAD2));
    assertThat(testUrls, hasSize(4));
  }

  @Test
  public void testGenerateUrls_negativeBecomesZeroIfNoZeroExists() {
    String payload = getBasePayload("\"{appIdList(1,MAX-1,MAX,MAX-6)}\"");
    ListReplacer replacer = new ListReplacer("{appIdList", appList, 4);
    TestUrl testUrl = getBasicTestUrl(payload);
    List<TestUrl> testUrls = replacer.generateUrls(testUrl);
    List<String> allPayloads = testUrls.stream().map(TestUrl::getPayload).collect(Collectors.toList());

    assertThat(testUrls, hasSize(4));
    assertThat(allPayloads, hasItem(BASE_PAYLOAD1 + BASE_PAYLOAD2));
  }

  @Test
  public void testGenerateUrls_zeroBecomesZeroIfNoZeroExists() {
    String payload = getBasePayload("\"{appIdList(1,MAX-1,MAX,MAX-4)}\"");
    ListReplacer replacer = new ListReplacer("{appIdList", appList, 4);
    TestUrl testUrl = getBasicTestUrl(payload);
    List<TestUrl> testUrls = replacer.generateUrls(testUrl);
    List<String> allPayloads = testUrls.stream().map(TestUrl::getPayload).collect(Collectors.toList());

    assertThat(testUrls, hasSize(4));
    assertThat(allPayloads, hasItem(BASE_PAYLOAD1 + BASE_PAYLOAD2));
  }

  @Test
  public void testGenerateUrls_negativeGetsDroppedIfZeroExists() {
    String payload = getBasePayload("\"{appIdList(0,1,MAX-1,MAX,MAX-6)}\"");
    ListReplacer replacer = new ListReplacer("{appIdList", appList, 4);
    TestUrl testUrl = getBasicTestUrl(payload);
    List<TestUrl> testUrls = replacer.generateUrls(testUrl);
    assertThat(testUrls, hasSize(4));
  }

  @Test
  public void testGenerateUrls_zeroGetsDroppedIfZeroExists() {
    String payload = getBasePayload("\"{appIdList(0,1,MAX-1,MAX,MAX-4)}\"");
    ListReplacer replacer = new ListReplacer("{appIdList", appList, 4);
    TestUrl testUrl = getBasicTestUrl(payload);
    List<TestUrl> testUrls = replacer.generateUrls(testUrl);
    assertThat(testUrls, hasSize(4));
  }

  @Test
  public void testGenerateUrls_maxPlusNinetyNineBecomesMax() {
    String payload = getBasePayload("\"{appIdList(0,1,MAX-1,MAX,MAX+99)}\"");
    ListReplacer replacer = new ListReplacer("{appIdList", appList, 4);
    TestUrl testUrl = getBasicTestUrl(payload);
    List<TestUrl> testUrls = replacer.generateUrls(testUrl);

    assertThat(testUrls, hasSize(4));
  }

  private TestUrl getBasicTestUrl(String payload) {
    TestUrl testUrl = new TestUrl();
    testUrl.setUrl("/rest/api");
    testUrl.setType("POST");
    testUrl.setPayload(payload);
    return testUrl;
  }

  private String getBasePayload(String variable) {
    return BASE_PAYLOAD1 + variable + BASE_PAYLOAD2;
  }

  private String buildReplacement(List<String> appNames) {
    StringBuilder appNamesString = new StringBuilder();
    appNamesString.append("\"").append(appNames.get(0)).append("\"");
    for (int i = 1; i < appNames.size(); i++) {
      appNamesString.append(",\"").append(appNames.get(i)).append("\"");
    }
    return appNamesString.toString();
  }
}
