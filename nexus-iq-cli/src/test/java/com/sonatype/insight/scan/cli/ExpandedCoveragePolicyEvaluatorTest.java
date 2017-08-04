/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.scan.cli;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import javax.inject.Inject;

import com.sonatype.clm.dto.model.ProprietaryConfig;
import com.sonatype.clm.dto.model.policy.PolicyEvaluationResult;
import com.sonatype.insight.brain.client.RestClientFactory;
import com.sonatype.insight.brain.client.RestClientFactory.RestClient;
import com.sonatype.insight.client.utils.HttpClientUtils.Configuration;
import com.sonatype.insight.scan.model.ClientScanType;
import com.sonatype.insight.scan.model.Scan;
import com.sonatype.insight.test.SslProperties;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Binder;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeDiagnosingMatcher;
import org.junit.Test;
import org.owasp.dependencycheck.dependency.Dependency;
import org.owasp.dependencycheck.dependency.Evidence;
import org.owasp.dependencycheck.utils.Settings;
import org.owasp.dependencycheck.utils.Settings.KEYS;

import static com.sonatype.insight.scan.cli.ExpandedCoveragePolicyEvaluator.EXPANDED_COVERAGE_SCAN_DISCLAIMER;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ExpandedCoveragePolicyEvaluatorTest
    extends AbstractPolicyEvaluatorTest
{
  static {
    SslProperties.use();
  }

  @Inject
  private ExpandedCoveragePolicyEvaluator evaluator;

  private RestClientFactory restClientFactory = mock(RestClientFactory.class);

  @Override
  public void configure(Binder binder) {
    binder.bind(RestClientFactory.class).toInstance(restClientFactory);
  }

  @Test
  public void testScan_Java() throws Exception {
    List<Dependency> dependencies = testScan("java");

    logOutput.assertInfo("Found 3 items.");
    assertThat(dependencies, hasSize(3));
    assertThat(dependencies, hasItems(dependencyWithName("uber-1.0-SNAPSHOT.jar"),
        dependencyWithName("uber-1.0-SNAPSHOT.jar/META-INF/maven/com.example/uber/pom.xml"),
        dependencyWithName("uber-1.0-SNAPSHOT.jar/META-INF/maven/org.apache.commons/commons-lang3/pom.xml")));
  }

  @Test
  public void testScan_CMake() throws Exception {
    List<Dependency> dependencies = testScan("cmake");

    logOutput.assertInfo("Found 1 items.");
    assertThat(dependencies, hasSize(1));
    assertThat(dependencies, hasItem(dependencyWithName("cmake/CMakeLists.txt")));
  }

  @Test
  public void testScan_Directory() throws Exception {
    List<Dependency> dependencies = testScan("");

    logOutput.assertInfo("Found 7 items.");
    assertThat(dependencies, hasSize(7));
    assertThat(dependencies, hasItem(dependencyWithName("cmake/CMakeLists.txt")));
    assertThat(dependencies, hasItem(dependencyWithName("uber-1.0-SNAPSHOT.jar")));
    assertThat(dependencies,
        hasItem(dependencyWithName("uber-1.0-SNAPSHOT.jar/META-INF/maven/com.example/uber/pom.xml")));
    assertThat(dependencies,
        hasItem(dependencyWithName("uber-1.0-SNAPSHOT.jar/META-INF/maven/org.apache.commons/commons-lang3/pom.xml")));
    assertThat(dependencies, hasItem(dependencyWithName("actionsheet.1.0.0.mod.nupkg")));
    assertThat(dependencies, hasItem(dependencyWithName("actionsheet.1.0.0.mod.nupkg: ActionSheet.nuspec")));
    assertThat(dependencies, hasItem(dependencyWithName("actionsheet.1.0.0.mod.nupkg: ActionSheet.dll")));
  }

  @Test
  public void testScan_DependencyWithEmptyVendorEvidenceItemValue_IsSerialized() throws Exception {
    List<Dependency> dependencies = testScan("actionsheet.1.0.0.mod.nupkg");
    Set<String> values = new HashSet<>();
    for (Dependency dependency : dependencies) {
      for (Evidence evidence : dependency.getVendorEvidence().getEvidence()) {
        assertThat(evidence.getValue(), is(notNullValue()));
        values.add(evidence.getValue());
      }
    }
    assertThat(values, hasItem(""));
  }

  @Test
  public void testRun_withExpandedCoverage() throws IOException, ExitException {
    Parameters params = new Parameters("-s", "http://localhost:8070/", "-i", "the-app-id", "-a", "user:pass",
        "src/test/data/artifact.jar");
    RestClient restClient = mock(RestClient.class);
    when(restClientFactory.newRestCLIClient(any(Configuration.class))).thenReturn(restClient);
    when(restClient.getApplicationsForApplicationEvaluation()).thenReturn(
        newApplicationSummaryList("the-app-id", "My App"));
    when(restClient.uploadScan(eq("the-app-id"), any(File.class), eq(ClientScanType.EXPANDED_COVERAGE)))
        .thenReturn(newReceipt());
    when(restClient.evaluatePolicy(eq("the-app-id"), eq("the-scan-id"), anyString())).thenReturn(
        new PolicyEvaluationResult());
    evaluator.run(params);
    verify(restClient).uploadScan(eq("the-app-id"), any(File.class), eq(ClientScanType.EXPANDED_COVERAGE));
  }

  private Matcher<Dependency> dependencyWithName(final String name) {
    return new TypeSafeDiagnosingMatcher<Dependency>()
    {
      @Override
      protected boolean matchesSafely(final Dependency item, final Description mismatchDescription) {
        if (!Objects.equals(item.getDisplayFileName().replace('\\', '/'), name)) {
          mismatchDescription.appendText("has displayFilename ").appendValue(item.getDisplayFileName());
          return false;
        }
        return true;
      }

      @Override
      public void describeTo(final Description description) {
        description.appendText("Dependency with displayFilename ").appendValue(name);
      }
    };
  }

  @Test
  public void testScan_AnalyzersThatConnectToExternalResourcesAreDisabled() throws Exception {
    testScan("java");

    assertThat(Settings.getBoolean(Settings.KEYS.ANALYZER_CENTRAL_ENABLED), is(false));
    assertThat(Settings.getBoolean(Settings.KEYS.ANALYZER_NEXUS_ENABLED), is(false));
    assertThat(Settings.getBoolean(Settings.KEYS.ANALYZER_NSP_PACKAGE_ENABLED), is(false));
  }

  private List<Dependency> testScan(String scanTarget) throws Exception {
    Parameters params = new Parameters("-o", tmpDir.newFolder().getAbsolutePath(),
        getClass().getResource("/" + getClass().getSimpleName() + "/" + scanTarget).getFile());

    File scanFile = evaluator.scan(params, new ProprietaryConfig());

    // Assert that the disclaimer banner is logged
    logOutput.assertInfo(EXPANDED_COVERAGE_SCAN_DISCLAIMER);

    // Assert applied client settings are logged in debug move
    logOutput.assertDebug("Setting: " + KEYS.ANALYZER_EXPERIMENTAL_ENABLED + "='true'");

    Scan scan = scanReader.read(scanFile);

    assertThat(scan.getExpandedCoverage(), is(notNullValue()));
    assertThat(scan.getExpandedCoverage().getVersion().matches("[0-9]+\\.[0-9]+.*"), is(true));

    String dependenciesJson = scan.getExpandedCoverage().getDependenciesJson();
    assertThat(dependenciesJson, is(notNullValue()));
    // Assert that Jackson is configured correctly to leave out derived properties
    assertThat(dependenciesJson, not(containsString("\"actualFile\"")));
    assertThat(dependenciesJson, not(containsString("\"fileNameForJavaScript\"")));
    assertThat(dependenciesJson, not(containsString("\"displayFileName\"")));
    assertThat(dependenciesJson, not(containsString("\"evidence\"")));
    assertThat(dependenciesJson, not(containsString("\"evidenceForDisplay\"")));
    assertThat(dependenciesJson, not(containsString("\"evidenceUsed\"")));

    ObjectMapper objectMapper = new ObjectMapper();
    objectMapper.setVisibility(objectMapper.getDeserializationConfig().getDefaultVisibilityChecker()
        .withFieldVisibility(JsonAutoDetect.Visibility.ANY).withGetterVisibility(JsonAutoDetect.Visibility.NONE));
    return objectMapper.readValue(dependenciesJson, new TypeReference<List<Dependency>>() { });
  }
}
