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
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import com.sonatype.clm.dto.model.component.ComponentDisplayNameUtil;
import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.model.component.MatchState;
import com.sonatype.insight.brain.report.Report;
import com.sonatype.insight.brain.report.ReportEntry;
import com.sonatype.insight.brain.service.Zipper;
import com.sonatype.insight.json.store.JsonUtils;
import com.sonatype.insight.test.LogOutput;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static com.sonatype.insight.brain.component.ComponentDisplayNameUtil.fromJsonNode;
import static org.assertj.core.api.Assertions.assertThat;

public class ThirdPartyComponentDAOTest
{
  @Rule
  public TemporaryFolder tempDir = new TemporaryFolder();

  @Rule
  public LogOutput logOutput = new LogOutput(ThirdPartyComponentDAO.class);

  private ThirdPartyComponentDAO dao = new ThirdPartyComponentDAO();

  private final String hashGlibc = "e587ce87ed894c1d5283";

  private final String hashApt = "683620ac905c1d32b58c";

  private final Map<String, ComponentIdentifier> testData = ImmutableMap.of(
      hashGlibc, componentIdentifierFrom("debian:9", "glibc", "2.24-11+deb9u3"),
      hashApt, componentIdentifierFrom("debian:9", "apt", "1.4.8"));

  @Test
  public void testGetData() {
    final File reportZip = zipReportDir("/ThirdPartyComponentDAOTest/report");
    final Map<String, ThirdPartyReportComponentDTO> data = dao.getData(reportZip);

    assertThat(data).hasSize(2);
    assertThat(data.keySet()).containsExactlyInAnyOrderElementsOf(testData.keySet());
    assertThat(data.get(hashGlibc).componentIdentifier).isEqualTo(testData.get(hashGlibc));
    assertThat(data.get(hashApt).componentIdentifier).isEqualTo(testData.get(hashApt));

    assertThat(data.get(hashGlibc).bomRow.matchState).isEqualTo(MatchState.EXACT.toString());

    assertThat(data.get(hashGlibc).securityRows).hasSize(2);
    assertThat(data.get(hashApt).securityRows).hasSize(1);

    assertThat(data.get(hashGlibc).securityRows.stream().map(s -> s.reference))
        .containsExactlyInAnyOrder("CVE-2017-16997", "CVE-2018-1000001");
    assertThat(data.get(hashApt).securityRows.stream().map(s -> s.reference)).containsOnly("CVE-2019-3462");

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
    List<ThirdPartyBillOfMaterialsRowDTO> componentList = Lists
        .newArrayList(newThirdPartyBom(hashGlibc, testData.get(hashGlibc)),
            newThirdPartyBom(hashApt, testData.get(hashApt)));

    dao.applyIdentifiedComponentUpdates(componentList, reportZip);

    assertSummaryCountsUpdated(reportZip, 3);
    assertDataCountsUpdated(reportZip, 3);
    assertUpdatedBom(reportZip);
  }

  private void assertUpdatedBom(final File reportZip) throws IOException {
    final ReportEntry bomEntry = Report.getEntry(reportZip, "bom.json");
    assertThat(bomEntry).isNotNull();

    JsonNode jsonNode = JsonUtils.parse(bomEntry.buf);
    final JsonNode aaDataNode = jsonNode.path("aaData");
    for (JsonNode node : aaDataNode) {
      Stream.of(hashGlibc, hashApt).forEach(hash -> {
        if (hash.equals(node.path("hash").asText())) {
          final String displayName = pathNameFromDisplayName(hash);
          final List<String> pathNames = getArrayValues(node, "pathnames");
          final List<String> fileNames = getArrayValues(node, "filenames");
          final String displayNameWithoutSpaces = displayName.replaceAll("\\s", "");
          assertThat(pathNames).containsExactly("dependency:/clair-scanner-output.json/" + displayNameWithoutSpaces);
          assertThat(fileNames).containsExactly(displayNameWithoutSpaces);
          assertThat(nodeDisplayName(node)).isEqualTo(displayName);
        }
      });
    }
  }

  private String nodeDisplayName(final JsonNode node) {
    return fromJsonNode((ObjectNode) node).toString();
  }

  private List<String> getArrayValues(final JsonNode node, final String fieldName) {
    return StreamSupport.stream(node.get(fieldName).spliterator(), false).map(JsonNode::textValue)
        .collect(Collectors.toList());
  }

  private String pathNameFromDisplayName(final String hash) {
    return ComponentDisplayNameUtil.fromIdentifier(testData.get(hash)).toString();
  }

  private ThirdPartyBillOfMaterialsRowDTO newThirdPartyBom(
      final String hash,
      final ComponentIdentifier componentIdentifier)
  {
    return new ThirdPartyBillOfMaterialsRowDTO(componentIdentifier, hash);
  }

  @Test
  public void testApplyThirdPartyComponentSummary_NoUpdateForEmptyList() throws Exception {
    final File reportZip = zipReportDir("/ThirdPartyComponentDAOTest/report");
    List<ThirdPartyBillOfMaterialsRowDTO> componentList = new ArrayList<>();

    dao.applyIdentifiedComponentUpdates(componentList, reportZip);

    assertSummaryCountsUpdated(reportZip, 1); // non-third party component only
    assertDataCountsUpdated(reportZip, 1);
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

  private void assertDataCountsUpdated(final File reportZip, final int expected) throws IOException {
    final ReportEntry entry = Report.getEntry(reportZip, "data.json");
    JsonNode jsonNode = JsonUtils.parse(entry.buf);
    assertThat(jsonNode.path("exactlyMatchedComponentCount").asInt()).isEqualTo(expected);
    assertThat(jsonNode.path("knownArtifactCount").asInt()).isEqualTo(expected);
  }

  private void assertSummaryCountsUpdated(final File reportZip, final int expected) throws IOException {
    final ReportEntry entry = Report.getEntry(reportZip, "summary.json");
    JsonNode jsonNode = JsonUtils.parse(entry.buf);
    assertThat(jsonNode.path("knownArtifactCount").asInt()).isEqualTo(expected);
  }
}
