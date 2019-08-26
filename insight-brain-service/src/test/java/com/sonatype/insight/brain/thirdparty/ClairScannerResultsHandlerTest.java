/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.thirdparty;

import java.util.HashSet;
import java.util.Set;

import javax.inject.Inject;

import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.scan.file.clair.ClairScannerResult;
import com.sonatype.insight.scan.file.clair.ClairScannerVulnerability;

import com.google.gson.Gson;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ClairScannerResultsHandlerTest
    extends AbstractComponentTest
{
  @Inject
  private ClairScannerResultHandler clairhandler;

  private static final Gson GSON = new Gson();

  @Test
  public void testHandle_filterContent() throws Exception {
    ClairScannerResult clairScannerResult = new ClairScannerResult();
    clairScannerResult.setImage("imageTest");
    Set<ClairScannerVulnerability> vulnerabilities = new HashSet<>();
    ClairScannerVulnerability vulnerability = new ClairScannerVulnerability();
    vulnerability.setFeatureName("fn");
    vulnerability.setFeatureVersion("fv");
    vulnerability.setNamespace("nm");
    vulnerability.setDescription("test");
    vulnerability.setVulnerability("CSV-test");
    vulnerability.setLink("www.test.com");
    vulnerability.setSeverity("High");
    vulnerabilities.add(vulnerability);
    clairScannerResult.setVulnerabilities(vulnerabilities);

    ThirdPartyScanContent content = new ThirdPartyScanContent(null, null, null, null, toJson(clairScannerResult));

    String filteredContent = clairhandler.handleAndFilterContents(content);
    assertThat(filteredContent).isNotNull();

    ClairScannerResult filteredClairScannerResult = toClairScannerResult(filteredContent);

    assertThat(filteredClairScannerResult).isNotNull();
    assertThat(filteredClairScannerResult.getImage()).isNull();
    assertThat(filteredClairScannerResult.getVulnerabilities()).isNotNull();
    assertThat(filteredClairScannerResult.getVulnerabilities()).hasSize(1);

    for (ClairScannerVulnerability filteredVulnerability : filteredClairScannerResult.getVulnerabilities()) {
      assertThat(filteredVulnerability).isNotNull();
      assertThat(filteredVulnerability.getFeatureName()).isNotNull();
      assertThat(filteredVulnerability.getFeatureVersion()).isNotNull();
      assertThat(filteredVulnerability.getNamespace()).isNotNull();

      assertThat(filteredVulnerability.getDescription()).isNull();
      assertThat(filteredVulnerability.getVulnerability()).isNull();
      assertThat(filteredVulnerability.getLink()).isNull();
      assertThat(filteredVulnerability.getSeverity()).isNull();
    }

  }

  @Test
  public void testHandle_nullContent() throws Exception {
    ThirdPartyScanContent content = new ThirdPartyScanContent(null, null, null, null, null);

    String filteredContent = clairhandler.handleAndFilterContents(content);
    assertThat(filteredContent).isNull();
  }

  @Test
  public void testHandle_emptyContent() throws Exception {
    ThirdPartyScanContent content = new ThirdPartyScanContent(null, null, null, null, toJson(new ClairScannerResult()));

    String filteredContent = clairhandler.handleAndFilterContents(content);
    ClairScannerResult filteredClairScannerResult = toClairScannerResult(filteredContent);

    assertThat(filteredClairScannerResult).isNotNull();
    assertThat(filteredClairScannerResult.getImage()).isNull();
    assertThat(filteredClairScannerResult.getVulnerabilities()).isNull();

  }

  private String toJson(ClairScannerResult clairScannerResult) {
    return GSON.toJson(clairScannerResult);
  }

  private ClairScannerResult toClairScannerResult(String content) {
    return GSON.fromJson(content, ClairScannerResult.class);
  }
}
