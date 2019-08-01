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
import java.util.Set;

import javax.inject.Inject;

import com.sonatype.clm.dto.model.ProprietaryConfig;
import com.sonatype.insight.brain.client.RestClientFactory;
import com.sonatype.insight.brain.client.RestClientFactory.RestClient;
import com.sonatype.insight.client.utils.HttpClientUtils.Configuration;
import com.sonatype.insight.scan.model.ClientScanResult;
import com.sonatype.insight.scan.model.ClientScanType;
import com.sonatype.insight.scan.model.Scan;
import com.sonatype.insight.test.SslProperties;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Binder;
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
import org.owasp.dependencycheck.analyzer.MSBuildProjectAnalyzer;
import org.owasp.dependencycheck.analyzer.NexusAnalyzer;
import org.owasp.dependencycheck.analyzer.NodePackageAnalyzer;
import org.owasp.dependencycheck.analyzer.NspAnalyzer;
import org.owasp.dependencycheck.analyzer.NugetconfAnalyzer;
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
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
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

  @Test
  public void testFixCMakeAnalyzerDisplayName_NullDisplayFileName() {
    Dependency dependency = new Dependency();

    evaluator.fixCMakeAnalyzerDisplayName(dependency);

    assertThat(dependency.getDisplayFileName()).isNull();
  }

  @Test
  public void testFixCMakeAnalyzerDisplayName_NotMatchingDisplayFileName() {
    Dependency dependency = new Dependency();
    dependency.setDisplayFileName("NotCMakeLists.txt");

    evaluator.fixCMakeAnalyzerDisplayName(dependency);

    assertThat(dependency.getDisplayFileName()).isEqualTo("NotCMakeLists.txt");
  }

  @Test
  public void testFixCMakeAnalyzerDisplayName_MatchesNullNameAndNullVersion() {
    Dependency dependency = new Dependency();
    dependency.setDisplayFileName("CMakeLists.txt");

    evaluator.fixCMakeAnalyzerDisplayName(dependency);

    assertThat(dependency.getDisplayFileName()).isEqualTo("CMakeLists.txt");
  }

  @Test
  public void testFixCMakeAnalyzerDisplayName_MatchesNullName() {
    Dependency dependency = new Dependency();
    dependency.setDisplayFileName("CMakeLists.txt");
    dependency.setVersion("version");

    evaluator.fixCMakeAnalyzerDisplayName(dependency);

    assertThat(dependency.getDisplayFileName()).isEqualTo("CMakeLists.txt");
  }

  @Test
  public void testFixCMakeAnalyzerDisplayName_MatchesNullVersion() {
    Dependency dependency = new Dependency();
    dependency.setDisplayFileName("CMakeLists.txt");
    dependency.setName("name");

    evaluator.fixCMakeAnalyzerDisplayName(dependency);

    assertThat(dependency.getDisplayFileName()).isEqualTo("name");
  }

  @Test
  public void testFixCMakeAnalyzerDisplayName_MatchesCmakeLists() {
    Dependency dependency = new Dependency();
    dependency.setDisplayFileName("CMakeLists.txt");
    dependency.setName("name");
    dependency.setVersion("version");

    evaluator.fixCMakeAnalyzerDisplayName(dependency);

    assertThat(dependency.getDisplayFileName()).isEqualTo("name:version");
  }

  @Test
  public void testFixCMakeAnalyzerDisplayName_MatchesCmake() {
    Dependency dependency = new Dependency();
    dependency.setDisplayFileName("file.cmake");
    dependency.setName("name");
    dependency.setVersion("version");

    evaluator.fixCMakeAnalyzerDisplayName(dependency);

    assertThat(dependency.getDisplayFileName()).isEqualTo("name:version");
  }

  private List<Dependency> testScan(String scanTarget) throws Exception {
    return testScan(scanTarget, evaluator);
  }

  private List<Dependency> testScan(String scanTarget, ExpandedCoveragePolicyEvaluator evaluator) throws Exception {
    Parameters params = new Parameters("-o", tmpDir.newFolder().getAbsolutePath(),
        getClass().getResource("/" + getClass().getSimpleName() + "/" + scanTarget).getFile());

    ClientScanResult clientScanResult = evaluator.scan(params, new ProprietaryConfig());

    assertThat(logOutput)
        // Assert that the disclaimer banner is logged
        .atInfoLevel().contains(EXPANDED_COVERAGE_SCAN_DISCLAIMER)
        // Assert applied client settings are logged in debug move
        .atDebugLevel().contains("Setting: " + KEYS.ANALYZER_EXPERIMENTAL_ENABLED + "='true'");

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

  @Test
  public void testGetExpandedCoverageConfiguration_ConfigureAnalyzers() throws Exception {
    Settings settings = evaluator.getExpandedCoverageConfiguration();
    assertThat(settings.getBoolean(Settings.KEYS.ANALYZER_EXPERIMENTAL_ENABLED)).isTrue();
    assertThat(settings.getBoolean(Settings.KEYS.ANALYZER_RETIRED_ENABLED)).isTrue();
    assertThat(settings.getBoolean(Settings.KEYS.ANALYZER_CENTRAL_ENABLED)).isFalse();
    assertThat(settings.getBoolean(Settings.KEYS.ANALYZER_ARTIFACTORY_ENABLED)).isFalse();
    assertThat(settings.getBoolean(Settings.KEYS.ANALYZER_NEXUS_ENABLED)).isFalse();
    assertThat(settings.getBoolean(Settings.KEYS.ANALYZER_NSP_PACKAGE_ENABLED)).isFalse();
    assertThat(settings.getBoolean(Settings.KEYS.ANALYZER_BUNDLE_AUDIT_ENABLED)).isFalse();
    assertThat(settings.getBoolean(Settings.KEYS.ANALYZER_RETIREJS_ENABLED)).isFalse();
  }

  @Test
  public void testNewExpandedCoverageEngine_EnableWantedAnalyzers() throws Exception {
    List<Class<?>> analyzers = getEnabledAnalyzers();
    analyzers.remove(AssemblyAnalyzer.class); // windows-specific
    assertThat(analyzers).contains( //
        ArchiveAnalyzer.class, //
        AutoconfAnalyzer.class, //
        CMakeAnalyzer.class, //
        CocoaPodsAnalyzer.class, //
        ComposerLockAnalyzer.class, //
        DependencyMergingAnalyzer.class, //
        FileNameAnalyzer.class, //
        JarAnalyzer.class, //
        MSBuildProjectAnalyzer.class, //
        NodePackageAnalyzer.class, //
        NugetconfAnalyzer.class, //
        NuspecAnalyzer.class, //
        OpenSSLAnalyzer.class, //
        PythonDistributionAnalyzer.class, //
        PythonPackageAnalyzer.class, //
        RubyBundlerAnalyzer.class, //
        RubyGemspecAnalyzer.class, //
        SwiftPackageManagerAnalyzer.class, //
        VersionFilterAnalyzer.class);
  }

  @Test
  public void testNewExpandedCoverageEngine_DisableAnalyzersUsingExternalResources() throws Exception {
    List<Class<?>> analyzers = getEnabledAnalyzers();
    assertThat(analyzers).doesNotContain( //
        CentralAnalyzer.class, //
        NexusAnalyzer.class, //
        NspAnalyzer.class, //
        RubyBundleAuditAnalyzer.class);
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
    assertThatExceptionOfType(ExitException.class).isThrownBy(() -> {
      evaluator.run(params);
    });
    assertThat(logOutput).atErrorLevel().contains("The application ID non-existent-app-public-id is invalid.");
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
