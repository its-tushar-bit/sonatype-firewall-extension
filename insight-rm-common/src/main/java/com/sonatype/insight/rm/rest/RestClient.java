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
import com.sonatype.clm.dto.model.component.RepositoryComponentEvaluationDataList;
import com.sonatype.clm.dto.model.component.RepositoryComponentEvaluationDataRequestList;
import com.sonatype.clm.dto.model.component.UnquarantinedComponentList;
import com.sonatype.clm.dto.model.policy.PolicyEvaluationResult;
import com.sonatype.clm.dto.model.policy.RepositoryPolicyEvaluationSummary;

public interface RestClient
{

  interface Base
  {

    void validateConfiguration() throws IOException;

    ApplicationSummaryList getApplicationsForApplicationEvaluation() throws IOException;

    ProprietaryConfig getProprietaryConfiguration() throws IOException;

    App forApplication(String appId);

    Repository forRepository(final String repositoryManagerInstanceId, final String repositoryPublicId);

    Resource getResource(String path) throws IOException, URISyntaxException;

    Resource getResource(String path, Map<String, String[]> params) throws IOException, URISyntaxException;

  }

  interface App
  {

    ScanReceipt uploadScan(File scanFile) throws IOException;

    Scan forScan(String scanId);

  }

  interface Scan
  {

    PolicyEvaluationResult evaluatePolicies(Stage stage) throws IOException;

  }

  interface Repository
  {
    void setEnabled(boolean enabled) throws IOException;

    void setQuarantine(final boolean enabled) throws IOException;

    void removeComponent(String pathname) throws IOException;

    void evaluateComponents(final RepositoryComponentEvaluationDataRequestList componentEvaluationDataRequestList)
        throws IOException;

    RepositoryComponentEvaluationDataList evaluateComponentWithQuarantine(final RepositoryComponentEvaluationDataRequestList repositoryComponentEvaluationDataRequest)
        throws IOException;

    RepositoryPolicyEvaluationSummary getPolicyEvaluationSummary() throws IOException;

    UnquarantinedComponentList getUnquarantinedComponents(long sinceUtcTimestamp) throws IOException;
  }
}
