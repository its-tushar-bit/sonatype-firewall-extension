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
import com.sonatype.clm.dto.model.signature.ComponentWithSignatures;
import com.sonatype.clm.dto.model.signature.ComponentWithSignaturesList;
import com.sonatype.insight.brain.client.PolicyAction;
import com.sonatype.insight.brain.client.RestClientFactory.RestClient;
import com.sonatype.insight.scan.cli.AbstractParametersTest.TestParameters;
import com.sonatype.insight.scan.model.ClientScanType;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
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
  public void enrichResultIfCallFlowAnalysisEnabled_whenCallFlowAnalysisNotEnabled() throws ExitException, IOException {
    AbstractParameters params = new TestParameters();
    RestClient restClient = mock(RestClient.class);
    PolicyEvaluationPollingResult eval = new PolicyEvaluationPollingResult();
    PolicyEvaluationResult originalResult = new PolicyEvaluationResult();
    eval.setResult(originalResult);
    PolicyEvaluationResult enrichedResult =
        abstractPolicyEvaluator.enrichResultIfCallFlowAnalysisEnabled(params, restClient, eval, originalResult);

    // Should return original result without enrichment
    assertThat(enrichedResult).isSameAs(originalResult);
    // Should not fetch call flow analysis configuration
    verify(restClient, times(0)).getCallFlowAnalysisConfig(any(), any());
  }

  @Test
  public void enrichResultIfCallFlowAnalysisEnabled_whenCallFlowAnalysisEnabled_configEnabled()
      throws ExitException, IOException
  {
    AbstractParameters params = new TestParameters();
    params.parse("-c");
    params.parse("-cn", "test");

    RestClient restClient = mock(RestClient.class);
    ApiCallFlowAnalysisConfigDTO apiCallFlowAnalysisConfigDTO = new ApiCallFlowAnalysisConfigDTO();
    apiCallFlowAnalysisConfigDTO.enabled = true;
    when(restClient.getCallFlowAnalysisConfig(any(), any())).thenReturn(apiCallFlowAnalysisConfigDTO);

    ComponentWithSignaturesList componentWithSignaturesList = new ComponentWithSignaturesList();
    componentWithSignaturesList.setComponents(List.of(new ComponentWithSignatures()));
    when(restClient.getVulnerableComponentsWithSignatures(any(), any())).thenReturn(componentWithSignaturesList);

    PolicyEvaluationPollingResult eval = new PolicyEvaluationPollingResult();
    PolicyEvaluationResult originalResult = new PolicyEvaluationResult();
    ScanReceipt scanReceipt = new ScanReceipt();
    scanReceipt.setScanId("scanId");
    eval.setScanReceipt(scanReceipt);
    eval.setResult(originalResult);
    abstractPolicyEvaluator.enrichResultIfCallFlowAnalysisEnabled(params, restClient, eval, originalResult);

    verify(restClient, times(1)).getVulnerableComponentsWithSignatures(any(), any());
    verify(restClient, times(1)).getCallFlowAnalysisConfig(any(), any());
  }

  @Test
  public void enrichResultIfCallFlowAnalysisEnabled_whenCallFlowAnalysisEnabled_configDisabled()
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
    PolicyEvaluationResult enrichedResult =
        abstractPolicyEvaluator.enrichResultIfCallFlowAnalysisEnabled(params, restClient, eval, originalResult);

    // Should return original result without enrichment
    assertThat(enrichedResult).isSameAs(originalResult);
    // Should fetch call flow analysis configuration
    verify(restClient, times(1)).getCallFlowAnalysisConfig(any(), any());
  }
}
