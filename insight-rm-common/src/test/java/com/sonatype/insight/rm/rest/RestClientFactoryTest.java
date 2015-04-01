/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.rm.rest;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import com.sonatype.clm.dto.model.ProprietaryConfig;
import com.sonatype.clm.dto.model.application.ApplicationSummary;
import com.sonatype.clm.dto.model.application.ApplicationSummaryList;
import com.sonatype.insight.brain.client.ConfigurationClient;
import com.sonatype.insight.brain.client.ScanClient;
import com.sonatype.insight.client.utils.HttpClientUtils.Configuration;

import org.apache.http.client.HttpResponseException;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

public class RestClientFactoryTest
{

  @Test
  public void testSaasUnreachable() throws Exception {
    HttpResponseException hre = new HttpResponseException(504, "nobody there");
    ScanClient scanClient = mock(ScanClient.class);
    when(scanClient.uploadRepoManScan(any(File.class))).thenThrow(hre);
    RestClientFactory factory = spy(new RestClientFactory());
    doReturn(scanClient).when(factory).newScanClient(any(Configuration.class), eq("appId"));
    try {
      RestClient.App client = factory.forConfiguration(new RestClientConfiguration()).forApplication("appId");
      client.uploadScan(new File(""));
      fail("Expected HttpException");
    }
    catch (HttpException e) {
      assertEquals(hre.getStatusCode(), e.getStatus());
      assertEquals(hre.getMessage(), e.getReason());
    }
  }

  @Test
  public void testGetProprietaryConfiguration() throws Exception {
    ProprietaryConfig config = new ProprietaryConfig();
    ConfigurationClient configClient = mock(ConfigurationClient.class);
    when(configClient.getProprietaryConfiguration()).thenReturn(config);
    RestClientFactory factory = spy(new RestClientFactory());
    doReturn(configClient).when(factory).newConfigurationClient(any(Configuration.class));
    RestClient.Base client = factory.forConfiguration(new RestClientConfiguration());
    assertSame(config, client.getProprietaryConfiguration());
  }

  @Test
  public void testGetProprietaryConfiguration_OldBrain() throws Exception {
    HttpResponseException hre = new HttpResponseException(404, "old brain");
    ConfigurationClient configClient = mock(ConfigurationClient.class);
    when(configClient.getProprietaryConfiguration()).thenThrow(hre);
    RestClientFactory factory = spy(new RestClientFactory());
    doReturn(configClient).when(factory).newConfigurationClient(any(Configuration.class));
    try {
      RestClient.Base client = factory.forConfiguration(new RestClientConfiguration());
      client.getProprietaryConfiguration();
      fail("Expected HttpException");
    }
    catch (HttpException e) {
      assertEquals(hre.getStatusCode(), e.getStatus());
      assertEquals(hre.getMessage(), e.getReason());
    }
  }

  @Test
  public void testGetApplicationSummaryList() throws Exception {
    ApplicationSummary summary = new ApplicationSummary();
    summary.setId("test-id");
    summary.setPublicId("test-public-id");
    summary.setName("test-name");

    List<ApplicationSummary> summaries = new ArrayList<>();
    summaries.add(summary);
    ApplicationSummaryList applicationSummaryList = new ApplicationSummaryList();
    applicationSummaryList.setApplicationSummaries(summaries);

    ConfigurationClient configClient = mock(ConfigurationClient.class);
    when(configClient.getApplicationsForApplicationEvaluation()).thenReturn(applicationSummaryList);

    RestClientFactory factory = spy(new RestClientFactory());
    doReturn(configClient).when(factory).newConfigurationClient(any(Configuration.class));
    RestClient.Base client = factory.forConfiguration(new RestClientConfiguration());

    assertSame(applicationSummaryList, client.getApplicationsForApplicationEvaluation());
  }

}
