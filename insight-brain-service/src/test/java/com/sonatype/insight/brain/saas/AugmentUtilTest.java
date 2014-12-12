/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.saas;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.sonatype.clm.dto.model.SecurityVulnerability;
import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.service.InsightConfig;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.json.store.JsonUtils;

import com.fasterxml.jackson.databind.node.ArrayNode;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class AugmentUtilTest
{
  @Rule
  public TemporaryFolder tempDir = new TemporaryFolder();

  private InsightWork insightWork;

  private String applicationId = "appFoo";

  @Before
  public void setup() throws IOException {
    InsightConfig insightConfig = new InsightConfig();
    File workDir = tempDir.newFolder();
    insightConfig.setSonatypeWork(workDir.getAbsolutePath());
    insightWork = new InsightWork(insightConfig);
  }

  @Test
  public void testGetSVDataByGAV() throws IOException {
    final String securityJson = "[{\"groupId\":\"g\",\"artifactId\":\"a\",\"version\": \"v\",\"reference\":\"refId\",\"source\":\"source\",\"status\":\"Acknowledged\"}]";
    final String expectedJson = "[{\"groupId\":\"g\",\"artifactId\":\"a\",\"version\":\"v\",\"componentIdentifier\":{\"format\":\"maven\",\"coordinates\":{\"groupId\":\"g\",\"artifactId\":\"a\",\"version\":\"v\"}},\"reference\":\"refId\",\"source\":\"source\",\"status\":\"Acknowledged\"}]";
    augmentAndAssertSecurity(securityJson, expectedJson);
  }

  @Test
  public void testGetSVDataByComponentIdentifier() throws IOException {
    final String securityJson = "[{\"componentIdentifier\":{\"format\":\"maven\",\"coordinates\":{\"groupId\":\"g\",\"artifactId\":\"a\",\"version\":\"v\"}},\"reference\":\"refId\",\"source\":\"source\",\"status\":\"Acknowledged\"}]";
    final String expectedJson = "[{\"componentIdentifier\":{\"format\":\"maven\",\"coordinates\":{\"groupId\":\"g\",\"artifactId\":\"a\",\"version\":\"v\"}},\"reference\":\"refId\",\"source\":\"source\",\"status\":\"Acknowledged\"}]";
    augmentAndAssertSecurity(securityJson, expectedJson);
  }

  @Test
  public void testGetSVDataByGAVNoMatch() throws IOException {
    final String securityJson = "[{\"componentIdentifier\":{\"format\":\"maven\",\"coordinates\":{\"groupId\":\"foo\",\"artifactId\":\"bar\",\"version\": \"baz\"}},\"reference\":\"refId\",\"source\":\"source\",\"status\":\"Acknowledged\"}]";
    final String expectedJson = "[{\"componentIdentifier\":{\"format\":\"maven\",\"coordinates\":{\"groupId\":\"g\",\"artifactId\":\"a\",\"version\":\"v\"}},\"reference\":\"refId\",\"source\":\"source\"}]";
    augmentAndAssertSecurity(securityJson, expectedJson);
  }

  @Test
  public void testGetSVDataByComponentIdentifierNoMatch() throws IOException {
    final String securityJson = "[{\"componentIdentifier\":{\"format\":\"qux\",\"coordinates\":{\"groupId\":\"g\",\"artifactId\":\"a\",\"version\":\"v\"}},\"reference\":\"refId\",\"source\":\"source\",\"status\":\"Acknowledged\"}]";
    final String expectedJson = "[{\"componentIdentifier\":{\"format\":\"maven\",\"coordinates\":{\"groupId\":\"g\",\"artifactId\":\"a\",\"version\":\"v\"}},\"reference\":\"refId\",\"source\":\"source\"}]";
    augmentAndAssertSecurity(securityJson, expectedJson);
  }

  private void augmentAndAssertSecurity(String securityJson, String expectedJson) throws IOException {
    createSecurityJson(securityJson);
    ArrayNode svData = AugmentUtil.getSVData(insightWork, applicationId, ComponentIdentifier.createMavenCoordinates("g", "a", "v"), getSecurityVulnerabilities());
    assertThat(svData, is((ArrayNode)JsonUtils.parse(expectedJson)));
  }

  private List<SecurityVulnerability> getSecurityVulnerabilities() {
    List<SecurityVulnerability> securityVulnerabilities = new ArrayList<>();
    securityVulnerabilities.add(new SecurityVulnerability("refId", "source", 2.5f));
    return securityVulnerabilities;
  }

  private void createSecurityJson(String json) throws IOException {
    File auditDir = insightWork.getAuditDir(applicationId);
    JsonUtils.fileStore(auditDir).commit("security.json", JsonUtils.parse(json));
  }
}
