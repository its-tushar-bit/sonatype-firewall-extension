/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.scan.cli;

import java.io.File;
import java.util.List;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class AbstractParametersTest
{
  static class TestParameters
      extends AbstractParameters
  {
    @Override
    protected String getProgramName() {
      return "test-cli";
    }

    @Override
    public List<String> getScanTargets() {
      return null;
    }

    @Override
    public List<String> getModuleExcludes() {
      return null;
    }

    @Override
    public String getServerUser() {
      return null;
    }
  }

  @Test
  public void testGetApplicationId() {
    TestParameters testParameters = new TestParameters();
    testParameters.parse("-i", "test");

    assertThat(testParameters.getApplicationId()).isEqualTo("test");
  }

  @Test
  public void testGetApplicationId_ApplicationIdIsTrimmed() {
    TestParameters testParameters = new TestParameters();
    testParameters.parse("-i", "  test  ");

    assertThat(testParameters.getApplicationId()).isEqualTo("test");
  }

  @Test
  public void testGetApplicationId_ApplicationIdIsNull() {
    TestParameters testParameters = new TestParameters();

    assertThat(testParameters.getApplicationId()).isNull();
  }

  @Test
  public void testGetOrganizationId() {
    TestParameters testParameters = new TestParameters();
    testParameters.parse("-O", "test");

    assertThat(testParameters.getOrganizationId()).isEqualTo("test");
  }

  @Test
  public void testGetOrganizationId_OrganizationIdIsTrimmed() {
    TestParameters testParameters = new TestParameters();
    testParameters.parse("-O", "  test  ");

    assertThat(testParameters.getOrganizationId()).isEqualTo("test");
  }

  @Test
  public void testGetOrganizationId_OrganizationIdIsNull() {
    TestParameters testParameters = new TestParameters();

    assertThat(testParameters.getOrganizationId()).isNull();
  }

  @Test
  public void testGetServerUrl() {
    TestParameters testParameters = new TestParameters();
    testParameters.parse("-s", "http://localhost:8070");

    assertThat(testParameters.getServerUrl()).isEqualTo("http://localhost:8070");
  }

  @Test
  public void testGetServerUrl_serverUrlIsTrimmed() {
    TestParameters testParameters = new TestParameters();
    testParameters.parse("-s", "  http://localhost:8070  ");

    assertThat(testParameters.getServerUrl()).isEqualTo("http://localhost:8070");
  }

  @Test
  public void testGetServerUrl_serverUrlIsNull() {
    TestParameters testParameters = new TestParameters();

    assertThat(testParameters.getServerUrl()).isNull();
  }

  @Test
  public void testParse_SystemPropertiesWithCommaSeparatedValues() {
    TestParameters params = new TestParameters();
    params.parse("-D", "key1=value1a, value1b", "-D", "key2=value2");
    assertThat(params.getProperties()).containsExactly("key1=value1a, value1b", "key2=value2");
  }

  @Test
  public void testCreateUsageHelp_MentionsDebugOption() {
    TestParameters params = new TestParameters();
    String help = params.createUsageHelp();
    assertThat(help).contains("--debug");
  }

  @Test
  public void testParse_WithValidMetadataJsonFile() {
    TestParameters params = new TestParameters();
    params.parse("-m", getFilePath("valid-metadata.json"), "-i", "appId", "-s", "http://localhost:8070");
    assertThat(params.getError()).isNull();
  }

  @Test
  public void testParse_WithInvalidMetadaJsonFile() {
    TestParameters params = new TestParameters();
    params.parse("--metadata-file", getFilePath("invalid-metadata.json"), "-i", "appId", "-s",
        "http://localhost:8070");
    assertThat(params.getError()).isNotNull();
    assertThat(params.getError()).hasMessageStartingWith("The specified metadata file ")
        .hasMessageContaining(" is invalid due to Unrecognized token 'commitHash'");
  }

  @Test
  public void testParse_KeepScanFileFlag() {
    TestParameters params = new TestParameters();
    params.parse("--keep-scan-file");
    assertThat(params.isKeepScanFile()).isTrue();
  }

  @Test
  public void testParse_KeepScanFileFlag_Short() {
    TestParameters params = new TestParameters();
    params.parse("-k");
    assertThat(params.isKeepScanFile()).isTrue();
  }

  @Test
  public void testParse_RunCallFlowAnalysis() {
    TestParameters params = new TestParameters();
    params.parse("--call-flow-analysis");
    assertThat(params.isRunCallFlowAnalysis()).isTrue();
  }

  @Test
  public void testParse_RunCallFlowAnalysisShort() {
    TestParameters params = new TestParameters();
    params.parse("-c");
    assertThat(params.isRunCallFlowAnalysis()).isTrue();
  }

  @Test
  public void testParse_CallFlowAnalysisNamespaces() {
    TestParameters params = new TestParameters();
    params.parse("--call-flow-analysis-namespaces", "test");
    assertThat(params.getCallFlowAnalysisNamespaces()).containsExactly("test");
  }

  @Test
  public void testParse_CallFlowAnalysisNamespacesShort() {
    TestParameters params = new TestParameters();
    params.parse("-cn", "test");
    assertThat(params.getCallFlowAnalysisNamespaces()).containsExactly("test");
  }

  private String getFilePath(String filename) {
    ClassLoader classLoader = getClass().getClassLoader();
    File file = new File(classLoader.getResource("AbstractParametersTest/" + filename).getFile());
    return file.getAbsolutePath();
  }
}
