/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.scan.cli;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import com.sonatype.clm.dto.model.ScanReceipt;
import com.sonatype.clm.dto.model.callflowanalysis.ApiCallFlowAnalysisConfigDTO;
import com.sonatype.clm.dto.model.policy.PolicyEvaluationPollingResult;
import com.sonatype.clm.dto.model.policy.PolicyEvaluationResult;
import com.sonatype.insight.brain.client.LicenseNotEnabledException;
import com.sonatype.insight.brain.client.PolicyAction;
import com.sonatype.insight.brain.client.RestClientFactory.RestClient;
import com.sonatype.insight.scan.cli.AbstractParametersTest.TestParameters;
import com.sonatype.insight.scan.model.ClientScanType;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class AbstractPolicyEvaluatorTest
{
  private final AbstractPolicyEvaluator abstractPolicyEvaluator = new AbstractPolicyEvaluator(null, null)
  {
    @Override
    protected ClientScanType getClientScanType() {
      return null;
    }

    @Override
    protected void processResults(
        final AbstractParameters params,
        final ScanReceipt receipt,
        final PolicyEvaluationResult eval,
        final PolicyAction outcome,
        final RestClient restClient)
    {
      // noop
    }
  };

  @Test
  public void testGetModuleIndices() {
    File baseDirectory = new File(
        Objects.requireNonNull(getClass().getClassLoader().getResource("AbstractPolicyEvaluatorTest")).getFile());

    List<File> moduleIndices = abstractPolicyEvaluator.getModuleIndices(baseDirectory, null);

    assertThat(moduleIndices)
        .extracting(File::getPath)
        .extracting(path -> path.substring(path.indexOf("AbstractPolicyEvaluatorTest")).replaceAll("\\\\", "/"))
        .containsExactly(
            "AbstractPolicyEvaluatorTest/artifact/nested/nexus-iq/module.xml",
            "AbstractPolicyEvaluatorTest/artifact/nested/sonatype-clm/module.xml",
            "AbstractPolicyEvaluatorTest/artifact/nexus-iq/module.xml",
            "AbstractPolicyEvaluatorTest/artifact/sonatype-clm/module.xml");
  }

  @Test
  public void testGetModuleIndices_WithModuleAsTarget() {
    File module = new File(
        Objects.requireNonNull(getClass().getClassLoader()
            .getResource("AbstractPolicyEvaluatorTest/artifact/sonatype-clm/module.xml")).getFile());

    List<File> moduleIndices = abstractPolicyEvaluator.getModuleIndices(null, Collections.singletonList(module),null);

    assertThat(moduleIndices)
        .extracting(File::getPath)
        .extracting(path -> path.substring(path.indexOf("AbstractPolicyEvaluatorTest")).replaceAll("\\\\", "/"))
        .containsExactly("AbstractPolicyEvaluatorTest/artifact/sonatype-clm/module.xml");
  }

  @Test
  public void testGetModuleIndices_WithExcludes() {
    File baseDirectory = new File(
        Objects.requireNonNull(getClass().getClassLoader().getResource("AbstractPolicyEvaluatorTest")).getFile());

    List<File> moduleIndices = abstractPolicyEvaluator.getModuleIndices(baseDirectory,
        Arrays.asList("**/nested/**", "**/sonatype-clm/module.xml"));

    assertThat(moduleIndices)
        .extracting(File::getPath)
        .extracting(path -> path.substring(path.indexOf("AbstractPolicyEvaluatorTest")).replaceAll("\\\\", "/"))
        .containsExactly("AbstractPolicyEvaluatorTest/artifact/nexus-iq/module.xml");
  }

  @Test
  public void testGetModuleIndices_RelativePath() {
    File baseDirectory = new File(
        Objects.requireNonNull(getClass().getClassLoader().getResource("AbstractPolicyEvaluatorTest")).getFile());
    List<File> targets = Collections.singletonList(new File("./artifact/nested"));

    List<File> moduleIndices = abstractPolicyEvaluator.getModuleIndices(baseDirectory, targets, null);

    assertThat(moduleIndices)
        .extracting(File::getPath)
        .extracting(path -> path.substring(path.indexOf("AbstractPolicyEvaluatorTest")).replaceAll("\\\\", "/"))
        .containsExactly(
            "AbstractPolicyEvaluatorTest/./artifact/nested/nexus-iq/module.xml",
            "AbstractPolicyEvaluatorTest/./artifact/nested/sonatype-clm/module.xml");
  }

  @Test
  public void testGetModuleIndices_IgnoresFiles() {
    File target = new File(
        Objects.requireNonNull(
                getClass().getClassLoader().getResource("AbstractParametersTest/invalid-metadata.json"))
            .getFile());

    List<File> moduleIndices = abstractPolicyEvaluator.getModuleIndices(null, Collections.singletonList(target), null);

    assertThat(moduleIndices).isEmpty();
  }

  @Test
  public void enrichResultIfCallFlowAnalysisEnabled_noCallFlowAnalysisLicense_hasParam()
      throws ExitException, IOException
  {
    // Even if call flow analysis parameters are present, server side license is not enabled
    AbstractParameters params = new TestParameters();
    params.parse("-c");
    params.parse("-cn", "test");

    RestClient restClient = mock(RestClient.class);
    PolicyEvaluationPollingResult eval = new PolicyEvaluationPollingResult();
    PolicyEvaluationResult originalResult = new PolicyEvaluationResult();
    eval.setResult(originalResult);

    // IQ server does not have call flow analysis license
    when(restClient.getCallFlowAnalysisConfig(any(), any())).thenThrow(new LicenseNotEnabledException());

    abstractPolicyEvaluator.enrichResultIfCallFlowAnalysisEnabled(params, restClient, eval, originalResult);

    // Should fetch call flow analysis configuration (and license info) when parameters are present
    verify(restClient).getCallFlowAnalysisConfig(any(), any());
    // No follow-up calls with real call flow analysis logic should be made
    verify(restClient, never()).getVulnerableComponentsWithSignatures(any(), any());
  }

  @Test
  public void enrichResultIfCallFlowAnalysisEnabled_hasLicense_noParam_configDisabled()
      throws ExitException, IOException
  {
    AbstractParameters params = new TestParameters();
    RestClient restClient = mock(RestClient.class);
    ApiCallFlowAnalysisConfigDTO apiCallFlowAnalysisConfigDTO = new ApiCallFlowAnalysisConfigDTO();
    apiCallFlowAnalysisConfigDTO.enabled = false;
    when(restClient.getCallFlowAnalysisConfig(any(), any())).thenReturn(apiCallFlowAnalysisConfigDTO);
    PolicyEvaluationPollingResult eval = new PolicyEvaluationPollingResult();
    PolicyEvaluationResult originalResult = new PolicyEvaluationResult();
    eval.setResult(originalResult);
    ScanReceipt scanReceipt = new ScanReceipt();
    scanReceipt.setScanId("scanId");
    eval.setScanReceipt(scanReceipt);
    eval.setResult(originalResult);
    abstractPolicyEvaluator.enrichResultIfCallFlowAnalysisEnabled(params, restClient, eval, originalResult);

    // Even if no parameters are present,
    // should still fetch call flow analysis configuration because experimental users might have the feature enabled
    verify(restClient).getCallFlowAnalysisConfig(any(), any());
    // No follow-up calls with real call flow analysis logic should be made
    verify(restClient, never()).getVulnerableComponentsWithSignatures(any(), any());
  }

  @Test
  public void enrichResultIfCallFlowAnalysisEnabled_hasLicense_noParam_configEnabled()
      throws ExitException, IOException
  {
    AbstractParameters params = new TestParameters();
    RestClient restClient = mock(RestClient.class);
    ApiCallFlowAnalysisConfigDTO apiCallFlowAnalysisConfigDTO = new ApiCallFlowAnalysisConfigDTO();
    apiCallFlowAnalysisConfigDTO.enabled = true;
    when(restClient.getCallFlowAnalysisConfig(any(), any())).thenReturn(apiCallFlowAnalysisConfigDTO);
    PolicyEvaluationPollingResult eval = new PolicyEvaluationPollingResult();
    PolicyEvaluationResult originalResult = new PolicyEvaluationResult();
    eval.setResult(originalResult);
    ScanReceipt scanReceipt = new ScanReceipt();
    scanReceipt.setScanId("scanId");
    eval.setScanReceipt(scanReceipt);
    eval.setResult(originalResult);
    abstractPolicyEvaluator.enrichResultIfCallFlowAnalysisEnabled(params, restClient, eval, originalResult);

    // Even if no parameters are present,
    // should still fetch call flow analysis configuration because experimental users might have the feature enabled
    verify(restClient).getCallFlowAnalysisConfig(any(), any());
    // Follow-up calls with real call flow analysis logic should be made
    verify(restClient).getVulnerableComponentsWithSignatures(any(), any());
  }

  @Test
  public void enrichResultIfCallFlowAnalysisEnabled_hasLicense_hasParam_configDisabled()
      throws ExitException, IOException
  {
    AbstractParameters params = new TestParameters();
    params.parse("-c");
    params.parse("-cn", "test");

    RestClient restClient = mock(RestClient.class);
    ApiCallFlowAnalysisConfigDTO apiCallFlowAnalysisConfigDTO = new ApiCallFlowAnalysisConfigDTO();
    apiCallFlowAnalysisConfigDTO.enabled = false;
    when(restClient.getCallFlowAnalysisConfig(any(), any())).thenReturn(apiCallFlowAnalysisConfigDTO);
    PolicyEvaluationPollingResult eval = new PolicyEvaluationPollingResult();
    PolicyEvaluationResult originalResult = new PolicyEvaluationResult();
    eval.setResult(originalResult);
    ScanReceipt scanReceipt = new ScanReceipt();
    scanReceipt.setScanId("scanId");
    eval.setScanReceipt(scanReceipt);
    eval.setResult(originalResult);
    abstractPolicyEvaluator.enrichResultIfCallFlowAnalysisEnabled(params, restClient, eval, originalResult);

    // Should fetch call flow analysis configuration (and license info) when parameters are present
    verify(restClient).getCallFlowAnalysisConfig(any(), any());
    // Follow-up calls with real call flow analysis logic should be made
    verify(restClient).getVulnerableComponentsWithSignatures(any(), any());
  }
}
