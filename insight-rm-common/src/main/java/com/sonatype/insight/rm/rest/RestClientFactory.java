/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.rm.rest;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.Map;
import java.util.Map.Entry;

import com.sonatype.clm.dto.model.ProprietaryConfig;
import com.sonatype.clm.dto.model.Resource;
import com.sonatype.clm.dto.model.ScanReceipt;
import com.sonatype.clm.dto.model.application.ApplicationSummaryList;
import com.sonatype.clm.dto.model.component.FirewallIgnorePatterns;
import com.sonatype.clm.dto.model.component.RepositoryComponentEvaluationDataList;
import com.sonatype.clm.dto.model.component.RepositoryComponentEvaluationDataRequestList;
import com.sonatype.clm.dto.model.component.UnquarantinedComponentList;
import com.sonatype.clm.dto.model.policy.PolicyEvaluationResult;
import com.sonatype.clm.dto.model.policy.RepositoryPolicyEvaluationSummary;
import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.clm.dto.model.repository.migration.MigrationDetails;
import com.sonatype.insight.brain.client.ConfigurationClient;
import com.sonatype.insight.brain.client.FirewallClient;
import com.sonatype.insight.brain.client.FirewallClient.RepositoryManagerType;
import com.sonatype.insight.brain.client.FirewallMigrationClient;
import com.sonatype.insight.brain.client.PolicyClient;
import com.sonatype.insight.brain.client.ResourceClient;
import com.sonatype.insight.brain.client.ScanClient;
import com.sonatype.insight.client.utils.HttpClientUtils.Configuration;
import com.sonatype.insight.rm.rest.RestClient.App;
import com.sonatype.insight.rm.rest.RestClient.FirewallMigration;
import com.sonatype.insight.rm.rest.RestClient.Repository;
import com.sonatype.insight.rm.rest.RestClient.Scan;

import org.apache.http.client.HttpResponseException;
import org.apache.http.client.utils.URIBuilder;

public class RestClientFactory
{
  public RestClient.Base forConfiguration(final RestClientConfiguration config) {
    if (config == null) {
      throw new IllegalArgumentException("REST client configuration missing");
    }
    return new BaseClient(config.getConfig());
  }

  ConfigurationClient newConfigurationClient(final Configuration config) {
    return new ConfigurationClient(config);
  }

  FirewallClient newFirewallClient(final Configuration config,
                                   final String repositoryManagerInstanceId,
                                   final String repositoryPublicId)
  {
    return new FirewallClient(config, repositoryManagerInstanceId, repositoryPublicId, RepositoryManagerType.NEXUS);
  }

  FirewallClient newFirewallClient(final Configuration config,
                                   final String repositoryManagerInstanceId,
                                   final String repositoryPublicId,
                                   final RepositoryManagerType repositoryManagerType)
  {
    return new FirewallClient(config, repositoryManagerInstanceId, repositoryPublicId, repositoryManagerType);
  }

  FirewallMigrationClient newFirewallMigrationClient(final Configuration config) {
    return new FirewallMigrationClient(config);
  }

  ScanClient newScanClient(final Configuration config, final String appId) {
    return new ScanClient(config, appId);
  }

  private class BaseClient
      implements RestClient.Base
  {
    protected final Configuration config;

    public BaseClient(final Configuration config) {
      this.config = config;
    }

    @Override
    public void validateConfiguration() throws IOException {
      try {
        newConfigurationClient(config).validateConfiguration();
      }
      catch (IOException e) {
        throw handleError(e);
      }
    }

    @Override
    public ApplicationSummaryList getApplicationsForApplicationEvaluation() throws IOException {
      try {
        return newConfigurationClient(config).getApplicationsForApplicationEvaluation();
      }
      catch (IOException e) {
        throw handleError(e);
      }
    }

    @Override
    public App forApplication(final String appId) {
      return new AppSpecificClient(config, appId);
    }

    protected IOException handleError(IOException e) {
      if (e instanceof HttpResponseException) {
        HttpResponseException re = (HttpResponseException) e;
        return new HttpException(re.getStatusCode(), re.getMessage(), re);
      }
      return e;
    }

    @Override
    public Resource getResource(String path) throws IOException, URISyntaxException {
      return getResource(path, Collections.emptyMap());
    }

    @Override
    public Resource getResource(String path, Map<String, String[]> params) throws IOException, URISyntaxException {
      URIBuilder builder = new URIBuilder();
      builder.setPath(path);

      for (Entry<String, String[]> param : params.entrySet()) {
        for (String value : param.getValue()) {
          builder.addParameter(param.getKey(), value);
        }
      }

      return new ResourceClient(config).getResource(builder.build().toString());
    }

    @Override
    public Repository forRepository(final String repositoryManagerInstanceId, final String repositoryPublicId) {
      return new RepositorySpecificClient(config, repositoryManagerInstanceId, repositoryPublicId,
          RepositoryManagerType.NEXUS);
    }

    @Override
    public Repository forRepository(final String repositoryManagerInstanceId,
                                    final String repositoryPublicId,
                                    final RepositoryManagerType repositoryManagerType)
    {
      return new RepositorySpecificClient(config, repositoryManagerInstanceId, repositoryPublicId,
          repositoryManagerType);
    }

    @Override
    public FirewallMigration forFirewallMigration() {
      return new FirewallMigrationSpecificClient(config);
    }

    @Override
    public ProprietaryConfig getProprietaryConfigForApplicationEvaluation(String applicationPublicId)
        throws IOException
    {
      try {
        return newConfigurationClient(config).getProprietaryConfigForApplicationEvaluation(applicationPublicId);
      }
      catch (IOException e) {
        throw handleError(e);
      }
    }

    @Override
    public FirewallIgnorePatterns getFirewallIgnorePatterns() throws IOException {
      try {
        return newConfigurationClient(config).getFirewallIgnorePatterns();
      }
      catch (HttpResponseException e) {
        if (e.getStatusCode() == 404) {
          throw new UnsupportedOperationException("IQ Server doesn't support firewall ignore patterns, "
              + "upgrade it to version 1.35, or newer, to support it.", e);
        }
        throw handleError(e);
      }
    }
  }

  private class RepositorySpecificClient
      extends BaseClient
      implements Repository
  {
    private final String repositoryManagerInstanceId;

    private final String repositoryPublicId;
    
    private final RepositoryManagerType repositoryManagerType;

    public RepositorySpecificClient(final Configuration config,
                                    final String repositoryManagerInstanceId,
                                    final String repositoryPublicId,
                                    final RepositoryManagerType repositoryManagerType)
    {
      super(config);
      this.repositoryManagerInstanceId = repositoryManagerInstanceId;
      this.repositoryPublicId = repositoryPublicId;
      this.repositoryManagerType = repositoryManagerType;
    }

    @Override
    public void setEnabled(boolean enabled) throws IOException {
      newFirewallClient(config, repositoryManagerInstanceId, repositoryPublicId).setEnabled(enabled);
    }

    @Override
    public void setQuarantine(final boolean enabled) throws IOException {
      newFirewallClient(config, repositoryManagerInstanceId, repositoryPublicId).setQuarantine(enabled);
    }

    @Override
    public void evaluateComponents(
        final RepositoryComponentEvaluationDataRequestList componentEvaluationDataRequestList) throws IOException
    {
      newFirewallClient(config, repositoryManagerInstanceId, repositoryPublicId).evaluateComponents(
          componentEvaluationDataRequestList);
    }

    @Override
    public RepositoryComponentEvaluationDataList evaluateComponentWithQuarantine(
        RepositoryComponentEvaluationDataRequestList repositoryComponentEvaluationDataRequestList) throws IOException
    {
      return newFirewallClient(config, repositoryManagerInstanceId, repositoryPublicId)
          .evaluateComponentWithQuarantine(repositoryComponentEvaluationDataRequestList);
    }

    @Override
    public RepositoryPolicyEvaluationSummary getPolicyEvaluationSummary() throws IOException {
      return newFirewallClient(config, repositoryManagerInstanceId, repositoryPublicId).getPolicyEvaluationSummary();
    }

    @Override
    public void removeComponent(String pathname) throws IOException {
      newFirewallClient(config, repositoryManagerInstanceId, repositoryPublicId).removeComponent(pathname);
    }

    @Override
    public UnquarantinedComponentList getUnquarantinedComponents(final long sinceUtcTimestamp) throws IOException {
      try {
        return newFirewallClient(config, repositoryManagerInstanceId, repositoryPublicId)
            .getUnquarantinedComponents(sinceUtcTimestamp);
      }
      catch (HttpResponseException e) {
        if (e.getStatusCode() == 405) {
          throw new UnsupportedOperationException("IQ Server doesn't support unquarantined component updates, " +
              "upgrade it to version 1.20, or newer, to support it.", e);
        }
        throw e;
      }
    }
  }

  private class AppSpecificClient
      extends BaseClient
      implements RestClient.App
  {
    protected final String appId;

    public AppSpecificClient(final Configuration config, final String appId) {
      super(config);
      this.appId = appId;
    }

    @Override
    public ScanReceipt uploadScan(File scanFile) throws IOException {
      try {
        return newScanClient(config, appId).uploadRepoManScan(scanFile);
      }
      catch (IOException e) {
        throw handleError(e);
      }
    }

    @Override
    public Scan forScan(String scanId) {
      return new ScanSpecificClient(config, appId, scanId);
    }
  }

  private class ScanSpecificClient
      extends AppSpecificClient
      implements RestClient.Scan
  {
    protected final String scanId;

    public ScanSpecificClient(final Configuration config, final String appId, final String scanId) {
      super(config, appId);
      this.scanId = scanId;
    }

    @Override
    public PolicyEvaluationResult evaluatePolicies(com.sonatype.insight.rm.rest.Stage stage) throws IOException {
      if (stage == null) {
        throw new IllegalArgumentException("stage missing");
      }
      Stage st;
      switch (stage) {
        case CLOSE_REPOSITORY:
          st = new Stage(Stage.ID_STAGE_RELEASE);
          break;
        case RELEASE_REPOSITORY:
          st = new Stage(Stage.ID_RELEASE);
          break;
        default:
          throw new IllegalStateException("unsupported stage " + stage);
      }
      return new PolicyClient(config, appId).evaluate(scanId, st);
    }
  }

  private class FirewallMigrationSpecificClient
      extends BaseClient
      implements RestClient.FirewallMigration
  {
    FirewallMigrationSpecificClient(final Configuration config) {
      super(config);
    }

    @Override
    public void verifyMigrationSupport(final String protocolVersion) throws IOException {
      newFirewallMigrationClient(config).verifyMigrationSupport(protocolVersion);
    }

    @Override
    public void migrateRepositoryHistory(final String sourceRepositoryManagerInstanceId,
                                         final String sourceRepositoryPublicId,
                                         final String targetRepositoryManagerInstanceId,
                                         final String targetRepositoryPublicId) throws IOException
    {
      newFirewallMigrationClient(config).migrateRepositoryHistory(sourceRepositoryManagerInstanceId,
          sourceRepositoryPublicId, targetRepositoryManagerInstanceId, targetRepositoryPublicId);
    }

    @Override
    public MigrationDetails getRepositoryMigrationState(final String targetRepositoryManagerInstanceId,
                                                        final String targetRepositoryPublicId) throws IOException
    {
      return newFirewallMigrationClient(config).getRepositoryMigrationState(targetRepositoryManagerInstanceId,
          targetRepositoryPublicId);
    }
  }
}
