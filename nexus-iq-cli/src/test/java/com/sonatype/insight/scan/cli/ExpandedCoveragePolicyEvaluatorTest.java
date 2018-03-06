/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.scan.cli;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import javax.inject.Inject;

import com.sonatype.clm.dto.model.ProprietaryConfig;
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
import org.hamcrest.Matchers;
import org.hamcrest.TypeSafeDiagnosingMatcher;
import org.junit.Test;
import org.owasp.dependencycheck.Engine;
import org.owasp.dependencycheck.analyzer.AbstractFileTypeAnalyzer;
import org.owasp.dependencycheck.analyzer.Analyzer;
import org.owasp.dependencycheck.analyzer.ArchiveAnalyzer;
import org.owasp.dependencycheck.analyzer.AssemblyAnalyzer;
import org.owasp.dependencycheck.analyzer.AutoconfAnalyzer;
import org.owasp.dependencycheck.analyzer.CMakeAnalyzer;
import org.owasp.dependencycheck.analyzer.CentralAnalyzer;
import org.owasp.dependencycheck.analyzer.CocoaPodsAnalyzer;
import org.owasp.dependencycheck.analyzer.ComposerLockAnalyzer;
import org.owasp.dependencycheck.analyzer.DependencyMergingAnalyzer;
import org.owasp.dependencycheck.analyzer.FileNameAnalyzer;
import org.owasp.dependencycheck.analyzer.JarAnalyzer;
import org.owasp.dependencycheck.analyzer.NexusAnalyzer;
import org.owasp.dependencycheck.analyzer.NodePackageAnalyzer;
import org.owasp.dependencycheck.analyzer.NspAnalyzer;
import org.owasp.dependencycheck.analyzer.NuspecAnalyzer;
import org.owasp.dependencycheck.analyzer.OpenSSLAnalyzer;
import org.owasp.dependencycheck.analyzer.PythonDistributionAnalyzer;
import org.owasp.dependencycheck.analyzer.PythonPackageAnalyzer;
import org.owasp.dependencycheck.analyzer.RubyBundleAuditAnalyzer;
import org.owasp.dependencycheck.analyzer.RubyBundlerAnalyzer;
import org.owasp.dependencycheck.analyzer.RubyGemspecAnalyzer;
import org.owasp.dependencycheck.analyzer.SwiftPackageManagerAnalyzer;
import org.owasp.dependencycheck.analyzer.VersionFilterAnalyzer;
import org.owasp.dependencycheck.dependency.Dependency;
import org.owasp.dependencycheck.dependency.Evidence;
import org.owasp.dependencycheck.dependency.EvidenceType;
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
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
    assertThat(dependencies, hasItem(dependencyWithName("zlib")));
  }

  @Test
  public void testScan_Directory() throws Exception {
    List<Dependency> dependencies = testScan("");

    logOutput.assertInfo("Found 15 items.");
    assertThat(dependencies, hasSize(15));
    assertThat(dependencies, hasItem(dependencyWithName("dns-sync:0.1.0")));
    assertThat(dependencies, hasItem(dependencyWithName("zlib")));
    assertThat(dependencies, hasItem(dependencyWithName("uber-1.0-SNAPSHOT.jar")));
    assertThat(dependencies,
        hasItem(dependencyWithName("uber-1.0-SNAPSHOT.jar/META-INF/maven/com.example/uber/pom.xml")));
    assertThat(dependencies,
        hasItem(dependencyWithName("uber-1.0-SNAPSHOT.jar/META-INF/maven/org.apache.commons/commons-lang3/pom.xml")));
    assertThat(dependencies, hasItem(dependencyWithName("actionsheet.1.0.0.mod.nupkg")));
    assertThat(dependencies, hasItem(dependencyWithName("ActionSheet:1.0.0")));
    assertThat(dependencies, hasItem(dependencyWithName("actionsheet.1.0.0.mod.nupkg: ActionSheet.dll")));
    assertThat(dependencies,
        hasItem(dependencyWithName("unreadableJarsAroundReadableJar.zip: b_jarWithStruts2pom.jar")));
    assertThat(dependencies, hasItem(dependencyWithName("macCompressWithMetaData.zip: ._OpenCVDetectPython.cmake")));
    assertThat(dependencies, hasItem(dependencyWithName("macCompressWithMetaData.zip: OpenCVDetectPython.cmake")));
    assertThat(dependencies, hasItem(dependencyWithName("test/opensslv.h")));
    assertThat(dependencies, hasItem(dependencyWithName("macCompressWithMetaData.zip: uber-1.1-SNAPSHOT.jar")));
    assertThat(dependencies, hasItem(dependencyWithName(
        "macCompressWithMetaData.zip: uber-1.1-SNAPSHOT.jar/META-INF/maven/com.example/uber/pom.xml")));
    assertThat(dependencies, hasItem(dependencyWithName(
        "macCompressWithMetaData.zip: uber-1.1-SNAPSHOT.jar/META-INF/maven/org.apache.commons/commons-lang3/pom.xml")));
  }

  @Test
  public void testScan_DependencyWithEmptyVendorEvidenceItemValue_IsSerialized() throws Exception {
    List<Dependency> dependencies = testScan("actionsheet.1.0.0.mod.nupkg");
    Set<String> values = new HashSet<>();
    for (Dependency dependency : dependencies) {
      for (Evidence evidence : dependency.getEvidence(EvidenceType.VENDOR)) {
        assertThat(evidence.getValue(), is(notNullValue()));
        values.add(evidence.getValue());
      }
    }
    assertThat(values, hasItem(""));
  }

  @Test
  public void testRun_ExpandedCoverage() throws IOException, ExitException {
    Parameters params = new Parameters("-s", "http://localhost:8070/", "-i", "the-app-id", "-a", "user:pass",
        "src/test/data/artifact.jar");
    RestClient restClient = mock(RestClient.class);
    when(restClientFactory.newRestCLIClient(any(Configuration.class))).thenReturn(restClient);
    when(restClient.verifyOrCreateApplication("the-app-id")).thenReturn(true);
    when(restClient.uploadScan(eq("the-app-id"), any(File.class), eq(ClientScanType.EXPANDED_COVERAGE)))
        .thenReturn(newReceipt());
    evaluator.run(params);
    verify(restClient).uploadScan(eq("the-app-id"), any(File.class), eq(ClientScanType.EXPANDED_COVERAGE));
    verify(restClient).prepareExpandedCoverageReport(eq("the-app-id"), eq("the-scan-id"));
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

  private List<Dependency> testScan(String scanTarget) throws Exception {
    return testScan(scanTarget, evaluator);
  }

  private List<Dependency> testScan(String scanTarget, ExpandedCoveragePolicyEvaluator evaluator) throws Exception {
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

  @Test
  public void testGetExpandedCoverageConfiguration_ConfigureAnalyzers() throws Exception {
    Settings settings = evaluator.getExpandedCoverageConfiguration();
    assertThat(settings.getBoolean(Settings.KEYS.ANALYZER_EXPERIMENTAL_ENABLED), is(true));
    assertThat(settings.getBoolean(Settings.KEYS.ANALYZER_RETIRED_ENABLED), is(true));
    assertThat(settings.getBoolean(Settings.KEYS.ANALYZER_CENTRAL_ENABLED), is(false));
    assertThat(settings.getBoolean(Settings.KEYS.ANALYZER_NEXUS_ENABLED), is(false));
    assertThat(settings.getBoolean(Settings.KEYS.ANALYZER_NSP_PACKAGE_ENABLED), is(false));
    assertThat(settings.getBoolean(Settings.KEYS.ANALYZER_BUNDLE_AUDIT_ENABLED), is(false));
  }

  @Test
  public void testNewExpandedCoverageEngine_EnableWantedAnalyzers() throws Exception {
    List<Class<?>> analyzers = getEnabledAnalyzers();
    analyzers.remove(AssemblyAnalyzer.class); // windows-specific
    assertThat(analyzers,
        Matchers.<Class<?>> containsInAnyOrder( //
            ArchiveAnalyzer.class, //
            AutoconfAnalyzer.class, //
            CMakeAnalyzer.class, //
            CocoaPodsAnalyzer.class, //
            ComposerLockAnalyzer.class, //
            DependencyMergingAnalyzer.class, //
            FileNameAnalyzer.class, //
            JarAnalyzer.class, //
            NodePackageAnalyzer.class, //
            NuspecAnalyzer.class, //
            OpenSSLAnalyzer.class, //
            PythonDistributionAnalyzer.class, //
            PythonPackageAnalyzer.class, //
            RubyBundlerAnalyzer.class, //
            RubyGemspecAnalyzer.class, //
            SwiftPackageManagerAnalyzer.class, //
            VersionFilterAnalyzer.class));
  }

  @Test
  public void testNewExpandedCoverageEngine_DisableAnalyzersUsingExternalResources() throws Exception {
    List<Class<?>> analyzers = getEnabledAnalyzers();
    assertThat(analyzers, not(hasItem(CentralAnalyzer.class)));
    assertThat(analyzers, not(hasItem(NexusAnalyzer.class)));
    assertThat(analyzers, not(hasItem(NspAnalyzer.class)));
    assertThat(analyzers, not(hasItem(RubyBundleAuditAnalyzer.class)));
  }

  @Test
  public void testRun_AutoAppCreationEnabled() throws Exception {
    Parameters params = new Parameters("-s", "http://localhost:8070/", "-i", "non-existent-app-public-id", "-a",
        "user:pass", "src/test/data/artifact.jar");
    RestClient restClient = mock(RestClient.class);
    when(restClientFactory.newRestCLIClient(any(Configuration.class))).thenReturn(restClient);
    when(restClient.verifyOrCreateApplication("non-existent-app-public-id")).thenReturn(true);
    when(restClient.uploadScan(eq("non-existent-app-public-id"), any(File.class), eq(ClientScanType.EXPANDED_COVERAGE)))
        .thenReturn(newReceipt());
    evaluator.run(params);
    verify(restClient)
        .uploadScan(eq("non-existent-app-public-id"), any(File.class), eq(ClientScanType.EXPANDED_COVERAGE));
    verify(restClient).prepareExpandedCoverageReport(eq("non-existent-app-public-id"), eq("the-scan-id"));
  }

  @Test
  public void testRun_AutoAppCreationDisabled() throws Exception {
    Parameters params = new Parameters("-s", "http://localhost:8070/", "-i", "non-existent-app-public-id", "-a",
        "user:pass", "src/test/data/artifact.jar");
    RestClient restClient = mock(RestClient.class);
    when(restClientFactory.newRestCLIClient(any(Configuration.class))).thenReturn(restClient);
    when(restClient.verifyOrCreateApplication("non-existent-app-public-id")).thenReturn(false);
    when(restClient.uploadScan(eq("non-existent-app-public-id"), any(File.class), eq(ClientScanType.EXPANDED_COVERAGE)))
        .thenReturn(newReceipt());
    try {
      evaluator.run(params);
      fail("Expected error");
    }
    catch (ExitException e) {
      logOutput.assertError("The application ID non-existent-app-public-id is invalid.");
    }
  }

  private List<Class<?>> getEnabledAnalyzers() throws Exception {
    Engine engine = evaluator.newExpandedCoverageEngine();

    // analyzers only remain enabled when they matched something, cf. AbstractFileTypeAnalyzer.prepareAnalyzer()
    Method setFilesMatched = AbstractFileTypeAnalyzer.class.getDeclaredMethod("setFilesMatched", Boolean.TYPE);
    setFilesMatched.setAccessible(true);
    for (Analyzer analyzer : engine.getAnalyzers()) {
      if (analyzer instanceof AbstractFileTypeAnalyzer) {
        setFilesMatched.invoke(analyzer, true);
      }
    }

    // analyzers only get disabled upon actual analyis run, cf. AbstractAnalyzer.prepare()
    engine.analyzeDependencies();

    List<Class<?>> analyzers = new ArrayList<>();
    for (Analyzer analyzer : engine.getAnalyzers()) {
      if (analyzer.isEnabled()) {
        analyzers.add(analyzer.getClass());
      }
    }
    return analyzers;
  }
}
