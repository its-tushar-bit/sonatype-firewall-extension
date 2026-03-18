/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.tenant.auth.provisionig;

import java.io.File;

import com.sonatype.insight.test.LogOutput;

import com.auth0.client.mgmt.Auth0ManagementAPI;
import com.auth0.json.mgmt.client.Client;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.EnvironmentVariables;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.mockito.quality.Strictness;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class Auth0ProvisioningServiceTest
{
  @Rule
  public EnvironmentVariables environmentVariables = new EnvironmentVariables();

  @Rule
  public TemporaryFolder temporaryFolder = new TemporaryFolder();

  @Rule
  public LogOutput logOutput = new LogOutput(Auth0ProvisioningService.class);

  @Rule
  public MockitoRule mockito = MockitoJUnit.rule().strictness(Strictness.STRICT_STUBS);

  @Mock
  private Auth0ManagementAPI managementAPI;

  @Spy
  private Auth0ProvisioningService auth0ProvisioningService;

  @Test
  public void testProvision() throws Exception {
    environmentVariables.set("AUTH0_API_TOKEN", "token");
    File tempFile = temporaryFolder.newFile();
    Client mockClient = mock(Client.class);
    String mockClientId = "abcdefg";

    when(auth0ProvisioningService.getManagementAPI()).thenReturn(managementAPI);
    TenantParameters parameters = new TenantParameters();
    parameters.setAction("provision");
    parameters.setSubdomain("tenant1");

    when(mockClient.getClientId()).thenReturn(mockClientId);
    when(managementAPI.createOrUpdateTenant("tenant1", "https://tenant1.mtiq.cloudy.sonatype.dev", null,
        null, null)).thenReturn(mockClient);
    when(managementAPI.getSamlMetaDataFile(mockClientId)).thenReturn(tempFile);

    auth0ProvisioningService.provision(parameters);
    logOutput.assertThat().contains("Created new auth0 account for tenant=tenant1, clientId=abcdefg").atInfoLevel();
    logOutput.assertThat().contains("Downloaded saml metadata file for tenant=tenant1").atInfoLevel();
  }

  @Test
  public void testProvision_missingApiToken() {
    TenantParameters parameters = new TenantParameters();
    parameters.setAction("provision");
    parameters.setSubdomain("tenant1");

    assertThatExceptionOfType(IllegalArgumentException.class)
        .isThrownBy(() -> auth0ProvisioningService.provision(parameters))
        .withMessage("AUTH0_API_TOKEN environment variable not set");
  }

  @Test
  public void testProvision_unableToDownloadSamlMetadataFile() {
    environmentVariables.set("AUTH0_API_TOKEN", "token");
    Client mockClient = mock(Client.class);
    String mockClientId = "abcdefg";

    TenantParameters parameters = new TenantParameters();
    parameters.setAction("provision");
    parameters.setSubdomain("tenant1");

    when(auth0ProvisioningService.getManagementAPI()).thenReturn(managementAPI);
    when(mockClient.getClientId()).thenReturn(mockClientId);
    when(managementAPI.createOrUpdateTenant("tenant1", "https://tenant1.mtiq.cloudy.sonatype.dev", null,
        null, null)).thenReturn(mockClient);
    when(managementAPI.getSamlMetaDataFile(mockClientId)).thenReturn(null);

    auth0ProvisioningService.provision(parameters);
    logOutput.assertThat().contains("Created new auth0 account for tenant=tenant1, clientId=abcdefg").atInfoLevel();
    logOutput.assertThat()
        .contains("Unable to download the saml metadata for tenant=tenant1, clientId=abcdefg")
        .atInfoLevel();
  }

  @Test
  public void testResolveAuth0Domain_usesDefaultWhenEnvVariableNotSet() {
    environmentVariables.set(Auth0ProvisioningService.AUTH0_DOMAIN, null);
    String resolved = auth0ProvisioningService.resolveAuth0Domain();
    assertThat(resolved).isEqualTo(Auth0ProvisioningService.DEFAULT_AUTH0_DOMAIN);
  }

  @Test
  public void testResolveAuth0Domain_usesEnvironmentVariable() {
    environmentVariables.set(Auth0ProvisioningService.AUTH0_DOMAIN, "https://auth0.com");
    String resolved = auth0ProvisioningService.resolveAuth0Domain();
    assertThat(resolved).isEqualTo("https://auth0.com");
  }

  @Test
  public void testResolveAuth0Domain_validatesUrl() {
    environmentVariables.set(Auth0ProvisioningService.AUTH0_DOMAIN, "new-domain");

    assertThatExceptionOfType(RuntimeException.class).isThrownBy(() -> auth0ProvisioningService.resolveAuth0Domain())
        .withMessage("AUTH0_DOMAIN value must be a URL");
  }
}
