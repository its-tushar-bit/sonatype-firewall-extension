/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
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
import com.sonatype.clm.dto.model.ScanReceipt;
import com.sonatype.clm.dto.model.policy.PolicyEvaluationResult;
import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.client.ConfigurationClient;
import com.sonatype.insight.brain.client.PolicyClient;
import com.sonatype.insight.brain.client.ResourceClient;
import com.sonatype.insight.brain.client.ScanClient;
import com.sonatype.insight.client.utils.HttpClientUtils.Configuration;
import com.sonatype.insight.client.utils.Result;
import com.sonatype.insight.rm.rest.RestClient.App;
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
    public Map<String, String> getApplications() throws IOException {
      try {
        return newConfigurationClient(config).getApplicationIdNameMap();
      }
      catch (IOException e) {
        throw handleError(e);
      }
    }

    @Override
    public ProprietaryConfig getProprietaryConfiguration() throws IOException {
      try {
        return newConfigurationClient(config).getProprietaryConfiguration();
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
      return getResource(path, Collections.<String, String[]> emptyMap());
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
      path = builder.build().toString();

      Result result = new ResourceClient(config).getResource(path);
      if (result.status() != 200) {
        throw new IOException(result.text());
      }
      return new Resource(result.data(), result.header("Content-Type"));
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
    public void validateApplicationId() throws IOException {
      try {
        newConfigurationClient(config).validateApplicationId(appId);
      }
      catch (IOException e) {
        throw handleError(e);
      }
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

  private static class Resource
      implements RestClient.Resource
  {
    private byte[] data;
    private String contentType;

    Resource(byte[] data, String contentType) {
      this.data = data;
      this.contentType = contentType;
    }

    @Override
    public byte[] getData() {
      return data;
    }

    @Override
    public String getContentType() {
      return contentType;
    }
  }
}
