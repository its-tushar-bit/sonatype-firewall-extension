/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.client;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.Set;

import javax.inject.Named;

import com.sonatype.clm.dto.model.ProprietaryConfig;
import com.sonatype.clm.dto.model.ScanReceipt;
import com.sonatype.clm.dto.model.callflowanalysis.ApiCallFlowAnalysisConfigDTO;
import com.sonatype.clm.dto.model.policy.PolicyEvaluationPollingResult;
import com.sonatype.clm.dto.model.policy.PolicyEvaluationResult;
import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.clm.dto.model.signature.ComponentWithSignaturesList;
import com.sonatype.clm.dto.model.signature.VulnerabilitySignatureAnalysisDTO;
import com.sonatype.insight.client.utils.HttpClientUtils.Configuration;
import com.sonatype.insight.scan.model.ClientScanResult;
import com.sonatype.insight.scan.model.ClientScanType;

import org.slf4j.Logger;

/**
 * @since 1.12.1
 */
@Named
public class RestClientFactory
{
  public RestClient newRestCIClient(Configuration config) {
    return newRestCIClient(config, null);
  }

  public RestClient newRestCIClient(Configuration config, Logger logger) {
    return new RestCIClient(config, logger);
  }

  public RestClient newRestCLIClient(Configuration config) {
    return new RestCLIClient(config);
  }

  /**
   * @since 1.19.0
   */
  public static class RestCLIClient
      extends RestClient
  {
    RestCLIClient(Configuration config) {
      super(config);
    }

    @Override
    public PolicyEvaluationPollingResult evaluatePolicy(String appId,
                                                        String stageId,
                                                        final ClientScanResult clientScanResult,
                                                        final ClientScanType clientScanType) throws IOException
    {
      return new PolicyClient(config, appId).evaluateCLI(clientScanResult, clientScanType, new Stage(stageId));
    }

    @Override
    public ScanReceipt uploadScan(String appId, File scanFile, ClientScanType clientScanType) throws IOException {
      return new ScanClient(config, appId).uploadCLIScan(scanFile, clientScanType);
    }

    @Override
    public PolicyEvaluationPollingResult runPolicyEvaluation(
        final String appId,
        final String stageId,
        final ClientScanResult clientScanResult,
        final ClientScanType clientScanType,
        final String statusId,
        final VulnerabilitySignatureAnalysisDTO analysisDTO) throws IOException
    {
      return new PolicyClient(config, appId)
          .runPolicyEvaluationForCLI(clientScanResult, clientScanType, new Stage(stageId), statusId, analysisDTO);
    }

    @Override
    public PolicyEvaluationPollingResult runComponentAnalysis(
        final String appId,
        final String stageId,
        final ClientScanResult clientScanResult,
        final ClientScanType clientScanType) throws IOException
    {
      return new PolicyClient(config, appId).runComponentAnalysisForCLI(clientScanResult, clientScanType,
          new Stage(stageId));
    }
  }

  /**
   * @since 1.19.0
   */
  public static class RestCIClient
      extends RestClient
  {
    private final Logger logger;

    RestCIClient(final Configuration config, final Logger logger) {
      super(config);
      this.logger = logger;
    }

    @Override
    public PolicyEvaluationPollingResult evaluatePolicy(String appId,
                                                        String stageId,
                                                        final ClientScanResult clientScanResult,
                                                        final ClientScanType clientScanType) throws IOException
    {
      return new PolicyClient(config, appId, logger).evaluateCI(clientScanResult, new Stage(stageId));
    }

    @Override
    public ScanReceipt uploadScan(String appId, File scanFile, ClientScanType clientScanType) {
      throw new UnsupportedOperationException("Uploading a scan file is not supported.");
    }

    @Override
    public PolicyEvaluationPollingResult runPolicyEvaluation(
        final String appId,
        final String stageId,
        final ClientScanResult clientScanResult,
        final ClientScanType clientScanType,
        final String statusId,
        final VulnerabilitySignatureAnalysisDTO analysisDTO) throws IOException
    {
      return new PolicyClient(config, appId)
          .runPolicyEvaluationForCI(clientScanResult, new Stage(stageId), statusId, analysisDTO);
    }

    @Override
    public PolicyEvaluationPollingResult runComponentAnalysis(
        final String appId,
        final String stageId,
        final ClientScanResult clientScanResult,
        final ClientScanType clientScanType) throws IOException
    {
      return new PolicyClient(config, appId).runComponentAnalysisForCI(clientScanResult, new Stage(stageId));
    }
  }

  public abstract static class RestClient
  {
    protected final Configuration config;

    protected RestClient(Configuration config) {
      this.config = config;
    }

    public abstract ScanReceipt uploadScan(String appId, File scanFile, ClientScanType clientScanType)
        throws IOException;

    /**
     * @since 1.143.0
     */
    public boolean verifyOrCreateApplication(String applicationPublicId, String organizationId) throws IOException {
      return new ConfigurationClient(config).verifyOrCreateApplication(applicationPublicId, organizationId);
    }

    /**
     * @since 1.45.0
     */
    public boolean verifyOrCreateApplication(String applicationPublicId) throws IOException {
      return new ConfigurationClient(config).verifyOrCreateApplication(applicationPublicId);
    }

    /**
     * Get the proprietary configuration used for an application evaluation.
     * 
     * @since 1.22.0
     */
    public ProprietaryConfig getProprietaryConfigForApplicationEvaluation(String applicationPublicId)
        throws IOException
    {
      return new ConfigurationClient(config).getProprietaryConfigForApplicationEvaluation(applicationPublicId);
    }

    /**
     * Get the proprietary configuration used for a component evaluation.
     * 
     * @since 1.22.0
     */
    public ProprietaryConfig getProprietaryConfigForComponentEvaluation(String applicationPublicId)
        throws IOException
    {
      return new ConfigurationClient(config).getProprietaryConfigForComponentEvaluation(applicationPublicId);
    }

    public abstract PolicyEvaluationPollingResult evaluatePolicy(String appId,
                                                                 String stageId,
                                                                 final ClientScanResult clientScanResult,
                                                                 final ClientScanType clientScanType)
        throws IOException;

    public void saveReportBundle(String appId, String scanId, File bundleFile) throws IOException {
      new ReportClient(config, appId, scanId).downloadBundle(bundleFile);
    }

    public void saveResults(String appId,
                            File resultFile,
                            ScanReceipt receipt,
                            PolicyEvaluationResult eval,
                            String outcome) throws IOException
    {
      new ScanClient(config, appId).saveResultData(resultFile, receipt, eval, outcome);
    }

    public void saveErrorData(String appId, File resultFile, String errorMessage, boolean isSystemError)
        throws IOException
    {
      new ScanClient(config, appId).saveErrorData(resultFile, errorMessage, isSystemError);
    }

    /**
     * @since 1.163.0
     */
    public void saveErrorData(
        String appId,
        File resultFile,
        String errorMessage,
        boolean isSystemError,
        boolean isScanningError)
        throws IOException
    {
      new ScanClient(config, appId).saveErrorData(resultFile, errorMessage, isSystemError, isScanningError);
    }

    /**
     * @since 1.50
     */
    public void validateServerVersion(String minimalServerVersionRequiredAsString) throws IOException {
      new ConfigurationClient(config).validateServerVersion(minimalServerVersionRequiredAsString);
    }

    /**
     * @since 1.72
     */
    public void addOrUpdateSourceControlRecord(String publicId, String repositoryUrl) throws IOException {
      addOrUpdateSourceControlRecord(publicId, repositoryUrl, null);
    }

    /*
     * @since 1.170
     */
    public void addOrUpdateSourceControlRecord(String publicId, String repositoryUrl, String repositoryPath)
        throws IOException
    {
      new SourceControlClient(config).addOrUpdateSourceControlRecord(publicId, repositoryUrl, repositoryPath);
    }

    public Set<String> getLicensedFeatures() throws IOException {
      return new ConfigurationClient(config).getLicensedFeatures();
    }

    public void sendTelemetry(Map<String, Object> telemetryData) throws IOException {
      new ConfigurationClient(config).sendTelemetry(telemetryData);
    }

    public ComponentWithSignaturesList getVulnerableComponentsWithSignatures(
        String applicationId,
        String scanId) throws IOException
    {
      return new ScanClient(config, applicationId).getVulnerableComponentsWithSignatures(scanId);
    }

    public PolicyEvaluationResult importReachabilityAnalysis(
        String applicationId,
        String scanId,
        VulnerabilitySignatureAnalysisDTO analysisDTO) throws IOException
    {
      return new PolicyClient(config, applicationId).importReachabilityAnalysis(scanId, analysisDTO);
    }

    /**
     * Get the call flow configuration used for an application.
     *
     * @since 1.175.0
     */
    public ApiCallFlowAnalysisConfigDTO getCallFlowAnalysisConfig(String ownerType, String ownerId)
        throws IOException
    {
      return new CallFlowAnalysisConfigClient(config).getAnalysisCallFlowConfig(ownerType, ownerId);
    }

    /**
     * Run the policy evaluation of a previous run component analysis.
     *
     * @since 1.188
     */
    public abstract PolicyEvaluationPollingResult runPolicyEvaluation(
        final String appId,
        final String stageId,
        final ClientScanResult clientScanResult,
        final ClientScanType clientScanType,
        final String statusId,
        final VulnerabilitySignatureAnalysisDTO analysisDT) throws IOException;

    /**
     * Run the component analysis phase of the distributed evaluation process for the given application and stage.
     *
     * @since 1.188
     */
    public abstract PolicyEvaluationPollingResult runComponentAnalysis(
        final String appId,
        final String stageId,
        final ClientScanResult clientScanResult,
        final ClientScanType clientScanType) throws IOException;
  }
}
