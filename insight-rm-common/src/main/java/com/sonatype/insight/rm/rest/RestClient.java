/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.rm.rest;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Map;

import com.sonatype.clm.dto.model.ProprietaryConfig;
import com.sonatype.clm.dto.model.Resource;
import com.sonatype.clm.dto.model.ScanReceipt;
import com.sonatype.clm.dto.model.application.ApplicationSummaryList;
import com.sonatype.clm.dto.model.component.FirewallIgnorePatterns;
import com.sonatype.clm.dto.model.component.ProprietaryComponentNames;
import com.sonatype.clm.dto.model.component.RepositoryComponentEvaluationDataList;
import com.sonatype.clm.dto.model.component.RepositoryComponentEvaluationDataRequestList;
import com.sonatype.clm.dto.model.component.UnquarantinedComponentList;
import com.sonatype.clm.dto.model.policy.PolicyEvaluationResult;
import com.sonatype.clm.dto.model.policy.RepositoryPolicyEvaluationSummary;
import com.sonatype.clm.dto.model.repository.QuarantinedComponentReport;
import com.sonatype.clm.dto.model.repository.migration.MigrationDetails;

public interface RestClient
{
  interface Base
  {
    void validateConfiguration() throws IOException;

    void validateServerVersion(String version) throws IOException;

    ApplicationSummaryList getApplicationsForApplicationEvaluation() throws IOException;

    /**
     * Get the proprietary configuration used for an application evaluation.
     * 
     * @since 1.22.0
     */
    ProprietaryConfig getProprietaryConfigForApplicationEvaluation(String applicationPublicId)
        throws IOException;

    Scan forApplicationScan(String appId, File scanFile);

    Repository forRepository(final String repositoryManagerInstanceId,
                             final String repositoryPublicId,
                             final RepositoryManagerType repositoryManagerType);

    /**
     * @since 1.30
     */
    FirewallMigration forFirewallMigration();

    Resource getResource(String path) throws IOException, URISyntaxException;

    Resource getResource(String path, Map<String, String[]> params) throws IOException, URISyntaxException;

    /**
     * @since 1.35
     */
    FirewallIgnorePatterns getFirewallIgnorePatterns() throws IOException;
  }

  interface Scan
  {
    PolicyEvaluationResult evaluatePolicies(Stage stage) throws IOException;

    ScanReceipt evaluatePoliciesWithReportId(Stage stage) throws IOException;
  }

  interface Repository
  {
    void setEnabled(boolean enabled) throws IOException;

    void setQuarantine(final boolean enabled) throws IOException;

    void removeComponent(String pathname) throws IOException;

    void evaluateComponents(final RepositoryComponentEvaluationDataRequestList componentEvaluationDataRequestList)
        throws IOException;

    RepositoryComponentEvaluationDataList evaluateComponentsAdhoc(
        RepositoryComponentEvaluationDataRequestList componentEvaluationDataRequestList) throws IOException;

    RepositoryComponentEvaluationDataList evaluateComponentWithQuarantine(
        final RepositoryComponentEvaluationDataRequestList repositoryComponentEvaluationDataRequest) throws IOException;

    /**
     * Evaluates policies on versions of the same component.
     * The specified componentEvaluationDataRequestList must contain only versions of the same component
     * Only the npm format is supported.
     * 
     * @since 1.133
     */
    RepositoryComponentEvaluationDataList evaluateComponentMetadata(
        RepositoryComponentEvaluationDataRequestList repositoryComponentEvaluationDataRequest) throws IOException;

    RepositoryPolicyEvaluationSummary getPolicyEvaluationSummary() throws IOException;

    UnquarantinedComponentList getUnquarantinedComponents(long sinceUtcTimestamp) throws IOException;

    void addProprietaryComponentNames(ProprietaryComponentNames proprietaryComponentNames) throws IOException;

    /**
     * @since 1.127
     */
    QuarantinedComponentReport getQuarantinedComponentReport(String pathname) throws IOException;
  }

  interface FirewallMigration
  {
    String PROTOCOL_V1 = "v1";

    void verifyMigrationSupport(final String protocolVersion) throws IOException;
    
    void migrateRepositoryHistory(final String sourceRepositoryManagerInstanceId,
                                  final String sourceRepositoryPublicId,
                                  final String targetRepositoryManagerInstanceId,
                                  final String targetRepositoryPublicId) throws IOException;

    MigrationDetails getRepositoryMigrationState(final String targetRepositoryManagerInstanceId,
                                                 final String targetRepositoryPublicId) throws IOException;
  }
}
