/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.thirdparty;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.model.component.Component;
import com.sonatype.insight.brain.model.component.MatchState;
import com.sonatype.insight.brain.report.Report;
import com.sonatype.insight.brain.report.ReportEntry;
import com.sonatype.insight.brain.service.Zipper;
import com.sonatype.insight.json.store.JsonUtils;
import com.sonatype.insight.test.LogOutput;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.Lists;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.assertj.core.api.Assertions.assertThat;

public class ThirdPartyComponentDAOTest
{
  @Rule
  public TemporaryFolder tempDir = new TemporaryFolder();

  @Rule
  public LogOutput logOutput = new LogOutput(ThirdPartyComponentDAO.class);

  private ThirdPartyComponentDAO dao = new ThirdPartyComponentDAO();

  private final ComponentIdentifier glibcIdentifier = componentIdentifierFrom("debian:9", "glibc", "2.24-11+deb9u3");

  private final ComponentIdentifier aptIdentifier = componentIdentifierFrom("debian:9", "apt", "1.4.8");

  private final ComponentIdentifier pythonIdentifier = componentIdentifierFrom("debian:9", "python3.5", "3.5.3-1");

  @Test
  public void testGetData() {
    final String hashGlibc = "e587ce87ed894c1d5283";
    final String hashApt = "683620ac905c1d32b58c";
    final String hashPython = "a19ddea0123f1d8150b2";

    final File reportZip = zipReportDir("/ThirdPartyComponentDAOTest/report");
    final Map<String, ThirdPartyReportComponentDTO> data = dao.getData(reportZip);

    assertThat(data).hasSize(3);
    assertThat(data.keySet()).containsExactlyInAnyOrder(hashGlibc, hashApt, hashPython);
    assertThat(data.get(hashGlibc).componentIdentifier).isEqualTo(glibcIdentifier);
    assertThat(data.get(hashApt).componentIdentifier).isEqualTo(aptIdentifier);
    assertThat(data.get(hashPython).componentIdentifier).isEqualTo(pythonIdentifier);

    assertThat(data.get(hashGlibc).bomRow.matchState).isEqualTo(MatchState.EXACT.toString());

    assertThat(data.get(hashGlibc).securityRows).hasSize(2);
    assertThat(data.get(hashApt).securityRows).hasSize(1);
    assertThat(data.get(hashPython).securityRows).hasSize(1);

    assertThat(data.get(hashGlibc).securityRows.stream().map(s -> s.reference))
        .containsExactlyInAnyOrder("CVE-2017-16997", "CVE-2018-1000001");
    assertThat(data.get(hashApt).securityRows.stream().map(s -> s.reference)).containsOnly("CVE-2019-3462");
    assertThat(data.get(hashPython).securityRows.stream().map(s -> s.reference)).containsOnly("CVE-2019-5010");

    ThirdPartyHealthCheckReportSecurityRowDTO aptSecurityRow = data.get(hashApt).securityRows.get(0);
    assertThat(aptSecurityRow.source).isEqualTo("Clair");
    assertThat(aptSecurityRow.score).isEqualTo(10.0f);
    assertThat(aptSecurityRow.url).isEqualTo("https://security-tracker.debian.org/tracker/CVE-2019-3462");
    assertThat(aptSecurityRow.matchState).isEqualTo(MatchState.EXACT.toString());
    assertThat(aptSecurityRow.description).isEqualTo("vulnerability description");
  }

  @Test
  public void testGetData_InvalidContent() {
    final File reportZip = zipReportDir("/ThirdPartyComponentDAOTest/invalid");
    final Map<String, ThirdPartyReportComponentDTO> data = dao.getData(reportZip);
    assertThat(data).isNotNull();
    assertThat(data).hasSize(0);
    logOutput.assertThat()
        .contains("error attempting to read third party data from report " + reportZip.getAbsolutePath())
        .atErrorLevel();
  }

  @Test
  public void testGetData_NullFile() {
    assertThat(dao.getData(null)).isNull();
  }

  @Test
  public void testApplyThirdPartyComponentSummary() throws Exception {
    final File reportZip = zipReportDir("/ThirdPartyComponentDAOTest/report");
    List<Component> componentList = Lists
        .newArrayList(new Component(glibcIdentifier), new Component(aptIdentifier), new Component(pythonIdentifier));

    dao.applyThirdPartyComponentSummary(componentList, reportZip);

    assertCountsUpdated(reportZip, "summary.json", 3);
    assertCountsUpdated(reportZip, "data.json", 3);
  }

  @Test
  public void testApplyThirdPartyComponentSummary_NoUpdateForEmptyList() throws Exception {
    final File reportZip = zipReportDir("/ThirdPartyComponentDAOTest/report");
    List<Component> componentList = new ArrayList<>();

    dao.applyThirdPartyComponentSummary(componentList, reportZip);

    assertCountsUpdated(reportZip, "summary.json", 0);
    assertCountsUpdated(reportZip, "data.json", 0);
  }

  private ComponentIdentifier componentIdentifierFrom(final String format, final String name, final String version) {
    final HashMap<String, String> coords = new HashMap<>();
    coords.put("name", name);
    coords.put(ComponentIdentifier.VERSION, version);
    return new ComponentIdentifier(format, coords);
  }

  private File zipReportDir(String resourceName) {
    try {
      URL resourceUrl = getClass().getResource(resourceName);
      File resourceDir = new File(resourceUrl.toURI());
      File reportZipFile = new File(tempDir.getRoot(), getClass().getSimpleName() + "-" + UUID.randomUUID() + ".zip");
      Zipper.zip(resourceDir, reportZipFile);
      return reportZipFile;
    }
    catch (IOException | URISyntaxException e) {
      throw new RuntimeException(e);
    }
  }

  private void assertCountsUpdated(final File reportZip, final String filename, final int expected) throws IOException {
    final ReportEntry entry = Report.getEntry(reportZip, filename);
    JsonNode jsonNode = JsonUtils.parse(entry.buf);
    assertThat(jsonNode.path("exactlyMatchedComponentCount").asInt()).isEqualTo(expected);
    assertThat(jsonNode.path("knownArtifactCount").asInt()).isEqualTo(expected);
  }
}
