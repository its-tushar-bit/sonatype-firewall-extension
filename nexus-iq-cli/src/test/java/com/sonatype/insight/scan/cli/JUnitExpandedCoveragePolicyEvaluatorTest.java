/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.scan.cli;

import java.io.File;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import com.sonatype.insight.brain.client.RestClientFactory;
import com.sonatype.insight.brain.client.RestClientFactory.RestClient;
import com.sonatype.insight.client.utils.HttpClientUtils.Configuration;
import com.sonatype.insight.scan.client.ClientScanner;
import com.sonatype.insight.scan.model.ClientScanType;
import com.sonatype.insight.scan.model.io.ScanWriterFactory;

import com.google.common.collect.ImmutableList;
import org.junit.Before;
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
import org.owasp.dependencycheck.utils.Settings;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Runs the default unit tests in {@link ExpandedCoveragePolicyEvaluatorTest}, as well as some specific unit tests.
 */
public class JUnitExpandedCoveragePolicyEvaluatorTest
    extends ExpandedCoveragePolicyEvaluatorTest
{
  protected ExpandedCoveragePolicyEvaluator evaluator;

  private RestClientFactory restClientFactory = mock(RestClientFactory.class);

  @Before
  public void before() {
    evaluator = new ExpandedCoveragePolicyEvaluator(getCLMServer().getInstance(Scanner.class), restClientFactory,
        getCLMServer().getInstance(ClientScanner.class), getCLMServer().getInstance(ScanWriterFactory.class));
  }

  @Override
  protected AbstractPolicyEvaluatorTestRunner withTestRunner(final List<String> params) {
    return new JUnitPolicyEvaluatorTestRunner(params, evaluator, logOutput);
  }

  @Test
  public void testRun_ExpandedCoverage() throws Exception {
    List<String> params = ImmutableList.of("-s", "http://localhost:8070/", "-i", "the-app-id", "-a", "user:pass",
        "src/test/data/artifact.jar");
    RestClient restClient = mock(RestClient.class);
    when(restClientFactory.newRestCLIClient(any(Configuration.class))).thenReturn(restClient);
    when(restClient.verifyOrCreateApplication("the-app-id")).thenReturn(true);
    when(restClient.uploadScan(eq("the-app-id"), any(File.class), eq(ClientScanType.EXPANDED_COVERAGE)))
        .thenReturn(newReceipt());
    withTestRunner(params)
        .doPolicyEvaluationRun();
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
    List<String> params = ImmutableList.of("-s", "http://localhost:8070/", "-i", "non-existent-app-public-id", "-a",
        "user:pass", "src/test/data/artifact.jar");
    RestClient restClient = mock(RestClient.class);
    when(restClientFactory.newRestCLIClient(any(Configuration.class))).thenReturn(restClient);
    when(restClient.verifyOrCreateApplication("non-existent-app-public-id")).thenReturn(true);
    when(restClient.uploadScan(eq("non-existent-app-public-id"), any(File.class), eq(ClientScanType.EXPANDED_COVERAGE)))
        .thenReturn(newReceipt());
    withTestRunner(params).doPolicyEvaluationRun();
    verify(restClient)
        .uploadScan(eq("non-existent-app-public-id"), any(File.class), eq(ClientScanType.EXPANDED_COVERAGE));
    verify(restClient).prepareExpandedCoverageReport(eq("non-existent-app-public-id"), eq("the-scan-id"));
  }

  @Test
  public void testRun_AutoAppCreationDisabled() throws Exception {
    List<String> params = ImmutableList.of("-s", "http://localhost:8070/", "-i", "non-existent-app-public-id", "-a",
        "user:pass", "src/test/data/artifact.jar");
    RestClient restClient = mock(RestClient.class);
    when(restClientFactory.newRestCLIClient(any(Configuration.class))).thenReturn(restClient);
    when(restClient.verifyOrCreateApplication("non-existent-app-public-id")).thenReturn(false);
    when(restClient.uploadScan(eq("non-existent-app-public-id"), any(File.class), eq(ClientScanType.EXPANDED_COVERAGE)))
        .thenReturn(newReceipt());
    withTestRunner(params)
        .expectFailExit()
        .expectErrorLog("The application ID non-existent-app-public-id is invalid.")
        .doPolicyEvaluationRun();
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
