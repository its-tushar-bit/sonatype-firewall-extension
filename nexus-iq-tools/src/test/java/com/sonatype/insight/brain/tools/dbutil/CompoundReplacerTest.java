/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.tools.dbutil;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.sonatype.insight.brain.tools.common.PerfTestConfig.TestUrl;

import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.core.IsCollectionContaining.hasItem;

public class CompoundReplacerTest
{
  private final List<String> appList = Arrays.asList("app1", "App2", "apP3", "APP4");

  private final List<String> orgList = Arrays.asList("org1", "Org2", "orG3", "ORG4");

  private static final String BASE_PAYLOAD1 = "{\"orderBy\": \"-AGE,-THREAT_LEVEL\",\"maxResults\": 101,"
      + "\"organizationIds\": [";

  private static final String BASE_PAYLOAD2 = "],\"applicationIds\": [";

  private static final String BASE_PAYLOAD3 = "],\"stageIds\": []}";

  private final Map<String, String> listPresets;

  public CompoundReplacerTest() {
    listPresets = new HashMap<>();
    listPresets.put("APP_LIST_0", "");
    listPresets.put("APP_LIST_1", buildReplacement(appList.subList(0, 1)));
    listPresets.put("APP_LIST_3", buildReplacement(appList.subList(0, 3)));
    listPresets.put("APP_LIST_4", buildReplacement(appList.subList(0, 4)));
    listPresets.put("ORG_LIST_0", "");
    listPresets.put("ORG_LIST_1", buildReplacement(orgList.subList(0, 1)));
    listPresets.put("ORG_LIST_3", buildReplacement(orgList.subList(0, 3)));
    listPresets.put("ORG_LIST_4", buildReplacement(orgList.subList(0, 4)));
  }

  @Test
  public void testGenerateUrls_basicFormula() {
    String payload = getBasePayload("\"{orgIdList(0,1,MAX-1,MAX)}\"", "\"{appIdList(0,1,MAX-1,MAX)}\"");
    Replacer appReplacer = new ListReplacer("{appIdList", appList, 4);
    Replacer orgReplacer = new ListReplacer("{orgIdList", orgList, 4);

    Replacer replacer = new CompoundReplacer(Arrays.asList(appReplacer, orgReplacer));
    TestUrl testUrl = getBasicTestUrl(payload);
    List<TestUrl> testUrls = replacer.generateUrls(testUrl);
    List<String> allPayloads = testUrls.stream().map(TestUrl::getPayload).collect(Collectors.toList());

    for (String org : new String[]{"ORG_LIST_0", "ORG_LIST_1", "ORG_LIST_3", "ORG_LIST_4"}) {
      assertThat(allPayloads, hasItem(buildFullPayload(org, "APP_LIST_0")));
      assertThat(allPayloads, hasItem(buildFullPayload(org, "APP_LIST_1")));
      assertThat(allPayloads, hasItem(buildFullPayload(org, "APP_LIST_3")));
      assertThat(allPayloads, hasItem(buildFullPayload(org, "APP_LIST_4")));
    }
    assertThat(testUrls, hasSize(16));

    TestUrl directtUrl = getBasicTestUrl("{}");
    assertThat(replacer.generateUrls(directtUrl), hasSize(0));
  }

  @Test
  public void testGenerateUrls_oneFormulaOnly() {
    String payload = getBasePayload("", "\"{appIdList(0,1,MAX-1,MAX)}\"");
    Replacer appReplacer = new ListReplacer("{appIdList", appList, 4);
    Replacer replacer = new CompoundReplacer(Collections.singletonList(appReplacer));
    TestUrl testUrl = getBasicTestUrl(payload);
    List<TestUrl> testUrls = replacer.generateUrls(testUrl);
    List<String> allPayloads = testUrls.stream().map(TestUrl::getPayload).collect(Collectors.toList());

    assertThat(allPayloads, hasItem(buildFullPayload("ORG_LIST_0", "APP_LIST_0")));
    assertThat(allPayloads, hasItem(buildFullPayload("ORG_LIST_0", "APP_LIST_1")));
    assertThat(allPayloads, hasItem(buildFullPayload("ORG_LIST_0", "APP_LIST_3")));
    assertThat(allPayloads, hasItem(buildFullPayload("ORG_LIST_0", "APP_LIST_4")));
  }

  private TestUrl getBasicTestUrl(String payload) {
    TestUrl testUrl = new TestUrl();
    testUrl.setUrl("/rest/api");
    testUrl.setType("POST");
    testUrl.setPayload(payload);
    return testUrl;
  }

  private String getBasePayload(String variable1, String variable2) {
    return BASE_PAYLOAD1 + variable1 + BASE_PAYLOAD2 + variable2 + BASE_PAYLOAD3;
  }

  private String buildReplacement(final List<String> items) {
    StringBuilder appNamesString = new StringBuilder();
    appNamesString.append("\"").append(items.get(0)).append("\"");
    for (int i = 1; i < items.size(); i++) {
      appNamesString.append(",\"").append(items.get(i)).append("\"");
    }
    return appNamesString.toString();
  }

  private String buildFullPayload(String orgKey, String appKey) {
    return BASE_PAYLOAD1 + listPresets.get(orgKey) + BASE_PAYLOAD2 + listPresets.get(appKey) + BASE_PAYLOAD3;
  }
}
