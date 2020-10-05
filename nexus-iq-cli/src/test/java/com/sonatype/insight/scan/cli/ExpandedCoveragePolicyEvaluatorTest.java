/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.scan.cli;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.sonatype.clm.dto.model.ProprietaryConfig;
import com.sonatype.insight.brain.client.RestClientFactory.RestClient;
import com.sonatype.insight.scan.model.ClientScanResult;
import com.sonatype.insight.scan.model.Scan;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import org.junit.Test;
import org.owasp.dependencycheck.dependency.Dependency;
import org.owasp.dependencycheck.dependency.Evidence;
import org.owasp.dependencycheck.dependency.EvidenceType;
import org.owasp.dependencycheck.utils.Settings.KEYS;

import static com.sonatype.insight.scan.cli.ExpandedCoveragePolicyEvaluator.EXPANDED_COVERAGE_SCAN_DISCLAIMER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * The primary set of tests for the {@link ExpandedCoveragePolicyEvaluator}.
 *
 * This set of test cases powers not only the regular unit tests (see @{@link JUnitExpandedCoveragePolicyEvaluatorTest},
 * but also the native image configuration and testing. This allows us to have one set of tests which covers all three
 * cases.
 */
public abstract class ExpandedCoveragePolicyEvaluatorTest
    extends AbstractPolicyEvaluatorTest
{
  private String getDependencyName(Dependency dependency) {
    return dependency.getDisplayFileName().replace('\\', '/');
  }

  @Test
  public void testScan_Java() throws Exception {
    List<Dependency> dependencies = testScan("java");

    assertThat(logOutput).atInfoLevel().contains("Found 3 items.");
    assertThat(dependencies).extracting(this::getDependencyName).containsExactlyInAnyOrder( //
        "uber-1.0-SNAPSHOT.jar", //
        "uber-1.0-SNAPSHOT.jar (shaded: com.example:uber:1.0-SNAPSHOT)", //
        "uber-1.0-SNAPSHOT.jar (shaded: org.apache.commons:commons-lang3:3.6)");
  }

  @Test
  public void testScan_CMake() throws Exception {
    List<Dependency> dependencies = testScan("cmake");

    assertThat(logOutput).atInfoLevel().contains("Found 1 items.");
    assertThat(dependencies).extracting(this::getDependencyName).containsExactlyInAnyOrder("zlib");
  }

  @Test
  public void testScan_Directory() throws Exception {
    List<Dependency> dependencies = testScan("");

    assertThat(logOutput).atInfoLevel().contains("Found 18 items.");
    assertThat(dependencies).extracting(this::getDependencyName).containsExactlyInAnyOrder( //
        "zlib", //
        "uber-1.0-SNAPSHOT.jar", //
        "uber-1.0-SNAPSHOT.jar (shaded: com.example:uber:1.0-SNAPSHOT)", //
        "uber-1.0-SNAPSHOT.jar (shaded: org.apache.commons:commons-lang3:3.6)", //
        "actionsheet.1.0.0.mod.nupkg", //
        "ActionSheet:1.0.0", //
        "actionsheet.1.0.0.mod.nupkg: ActionSheet.dll", //
        "unreadableJarsAroundReadableJar.zip: b_jarWithStruts2pom.jar", //
        "macCompressWithMetaData.zip: ._OpenCVDetectPython.cmake", //
        "macCompressWithMetaData.zip: OpenCVDetectPython.cmake", //
        "test/opensslv.h", //
        "macCompressWithMetaData.zip: uber-1.1-SNAPSHOT.jar", //
        "macCompressWithMetaData.zip: uber-1.1-SNAPSHOT.jar (shaded: com.example:uber:1.1-SNAPSHOT)", //
        "macCompressWithMetaData.zip: uber-1.1-SNAPSHOT.jar (shaded: org.apache.commons:commons-lang3:3.6)", //
        "test.csproj", //
        "Microsoft.AspNetCore.All:2.0.5", //
        "packages.config", //
        "Microsoft.AspNet.WebApi.Core:5.2.4");
  }

  @Test
  public void testScan_DependencyWithEmptyVendorEvidenceItemValue_IsSerialized() throws Exception {
    List<Dependency> dependencies = testScan("actionsheet.1.0.0.mod.nupkg");
    Set<String> values = new HashSet<>();
    for (Dependency dependency : dependencies) {
      for (Evidence evidence : dependency.getEvidence(EvidenceType.VENDOR)) {
        assertThat(evidence.getValue()).isNotNull();
        values.add(evidence.getValue());
      }
    }
    assertThat(values).contains("");
  }

  private List<Dependency> testScan(String scanTarget) throws Exception {
    List<String> params = ImmutableList.of("-o", tempDir.newFolder().getAbsolutePath(),
        getClass().getResource("/" + getTestClassName() + "/" + scanTarget).getFile());

    RestClient restClient = mock(RestClient.class);

    ClientScanResult clientScanResult = withTestRunner(params)
        // Assert that the disclaimer banner is logged
        .expectInfoLog(EXPANDED_COVERAGE_SCAN_DISCLAIMER)
        // Assert applied client settings are logged in debug move
        .expectDebugLog("Setting: " + KEYS.ANALYZER_EXPERIMENTAL_ENABLED + "='true'")
        .doPolicyEvaluationScan(new ProprietaryConfig(), restClient);

    Scan scan = scanReader.read(clientScanResult.getScanFile());

    assertThat(scan.getExpandedCoverage()).isNotNull();
    assertThat(scan.getExpandedCoverage().getVersion().matches("[0-9]+\\.[0-9]+.*")).isTrue();

    String dependenciesJson = scan.getExpandedCoverage().getDependenciesJson();
    assertThat(dependenciesJson).isNotNull();
    // Assert that Jackson is configured correctly to leave out derived properties
    assertThat(dependenciesJson).doesNotContain("\"actualFile\"", "\"fileNameForJavaScript\"", "\"displayFileName\"",
        "\"evidence\"", "\"evidenceForDisplay\"", "\"evidenceUsed\"");

    ObjectMapper objectMapper = new ObjectMapper();
    objectMapper.setVisibility(objectMapper.getDeserializationConfig().getDefaultVisibilityChecker()
        .withFieldVisibility(JsonAutoDetect.Visibility.ANY).withGetterVisibility(JsonAutoDetect.Visibility.NONE));
    return objectMapper.readValue(dependenciesJson, new TypeReference<List<Dependency>>() { });
  }

  private String getTestClassName() {
    return getClass().getSuperclass().getSimpleName();
  }
}
