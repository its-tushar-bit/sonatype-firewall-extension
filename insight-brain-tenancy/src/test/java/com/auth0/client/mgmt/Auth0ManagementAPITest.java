/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.auth0.client.mgmt;

import java.io.File;
import java.nio.file.Files;

import com.auth0.exception.Auth0Exception;
import com.auth0.json.mgmt.client.Client;
import com.auth0.net.Request;
import org.apache.commons.lang3.StringUtils;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.EnvironmentVariables;
import org.junit.rules.TemporaryFolder;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.mockito.quality.Strictness;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

public class Auth0ManagementAPITest
{
  @Rule
  public MockitoRule mockito = MockitoJUnit.rule().strictness(Strictness.STRICT_STUBS);

  @Rule
  public EnvironmentVariables environmentVariables = new EnvironmentVariables();

  @Rule
  public TemporaryFolder temporaryFolder = new TemporaryFolder();

  @Captor
  private ArgumentCaptor<Client> clientCaptor;

  public Auth0ManagementAPI auth0ManagementAPI;

  @Before
  public void before() {
    auth0ManagementAPI =
        spy(new Auth0ManagementAPI("https://sonatype.auth0.com", "abcdefg", "http://<tenant>.sonatype.app"));
  }

  @Test
  public void testCreate_validateTenantSubdomain_Empty() {
    assertThatExceptionOfType(IllegalArgumentException.class)
        .isThrownBy(() -> auth0ManagementAPI.createTenant("", "blah", "http://tenant1.com/logo.gif"))
        .withMessage("tenant name cannot be blank or invalid characters <,>");
  }

  @Test
  public void testCreate_validateTenantSubdomain_InvalidCharacters() {
    assertThatExceptionOfType(IllegalArgumentException.class)
        .isThrownBy(() -> auth0ManagementAPI.createTenant("<sub-domain>", "blah", "http://tenant1.com/logo.gif"))
        .withMessage("tenant name cannot be blank or invalid characters <,>");
  }

  @Test
  public void testCreate_validateDescription() {
    assertThatExceptionOfType(IllegalArgumentException.class)
        .isThrownBy(() -> auth0ManagementAPI.createTenant("tenant1", StringUtils.repeat("a", 141),
            "http://tenant1.com/logo.gif"))
        .withMessage("tenant description should be less that 140 characters");
  }

  @Test
  public void testCreate_validateLogoUrl() {
    assertThatExceptionOfType(IllegalArgumentException.class)
        .isThrownBy(() -> auth0ManagementAPI.createTenant("tenant1", "blah", "blah"))
        .withMessage("tenant logo url must be a valid url");
  }

  @Test
  public void testCreate() throws Exception {
    String name = "tenant1";
    String description = "blah";
    String logoUrl = "http://tenanat1.com/logo.gif";
    Client mockClient = mockClient(name, description, logoUrl);

    ClientsEntity mockClientsEntity = mock(ClientsEntity.class);
    when(auth0ManagementAPI.clients()).thenReturn(mockClientsEntity);
    Request<Client> mockRequest = mock(Request.class);
    when(mockClientsEntity.create(clientCaptor.capture())).thenReturn(mockRequest);
    when(mockRequest.execute()).thenReturn(mockClient);

    Client tenant = auth0ManagementAPI.createTenant(name, description, logoUrl);
    Assertions.assertThat(tenant.getName()).isEqualTo(name);
    Assertions.assertThat(tenant.getDescription()).isEqualTo(description);
    Assertions.assertThat(tenant.getLogoUri()).isEqualTo(logoUrl);

    Client clientParameter = clientCaptor.getValue();
    Assertions.assertThat(clientParameter.getName()).isEqualTo(name);
    Assertions.assertThat(clientParameter.getDescription()).isEqualTo(description);
    Assertions.assertThat(clientParameter.getLogoUri()).isEqualTo(logoUrl);
    Assertions.assertThat(clientParameter.getAllowedLogoutUrls()).contains("http://tenant1.sonatype.app");
    Assertions.assertThat(clientParameter.getCallbacks()).contains("http://tenant1.sonatype.app/saml");
  }

  @Test
  public void testCreate_withCustomApplicationName() throws Exception {
    String subDomain = "tenant1";
    String description = "blah";
    String logoUrl = "http://tenanat1.com/logo.gif";
    String name = "tenant1-mtiq";
    Client mockClient = mockClient(subDomain, description, logoUrl);

    ClientsEntity mockClientsEntity = mock(ClientsEntity.class);
    when(auth0ManagementAPI.clients()).thenReturn(mockClientsEntity);
    Request<Client> mockRequest = mock(Request.class);
    when(mockClientsEntity.create(clientCaptor.capture())).thenReturn(mockRequest);
    when(mockRequest.execute()).thenReturn(mockClient);

    Client tenant = auth0ManagementAPI.createTenant(name, subDomain, description, logoUrl);
    Assertions.assertThat(tenant.getDescription()).isEqualTo(description);
    Assertions.assertThat(tenant.getLogoUri()).isEqualTo(logoUrl);

    Client clientParameter = clientCaptor.getValue();
    Assertions.assertThat(clientParameter.getName()).isEqualTo(name);
    Assertions.assertThat(clientParameter.getDescription()).isEqualTo(description);
    Assertions.assertThat(clientParameter.getLogoUri()).isEqualTo(logoUrl);
    Assertions.assertThat(clientParameter.getAllowedLogoutUrls()).contains("http://tenant1.sonatype.app");
    Assertions.assertThat(clientParameter.getCallbacks()).contains("http://tenant1.sonatype.app/saml");
  }

  @Test
  public void testCreate_Auth0Error() throws Exception {
    String name = "tenant1";
    String description = "blah";
    String logoUrl = "http://tenanat1.com/logo.gif";

    ClientsEntity mockClientsEntity = mock(ClientsEntity.class);
    when(auth0ManagementAPI.clients()).thenReturn(mockClientsEntity);
    Request<Client> mockRequest = mock(Request.class);
    when(mockClientsEntity.create(any(Client.class))).thenReturn(mockRequest);
    when(mockRequest.execute()).thenThrow(new Auth0Exception("remote error"));

    assertThatExceptionOfType(RuntimeException.class)
        .isThrownBy(() -> auth0ManagementAPI.createTenant(name, description, logoUrl))
        .withMessageContaining("remote error");
  }

  @Test
  public void testGetSamlMetaDataFile() throws Exception {
    doReturn("saml content").when(auth0ManagementAPI)
        .downloadSamlMetadata("https://sonatype.auth0.com/samlp/metadata/abcdefg");
    File samlMetaDataFile = null;
    try {
      samlMetaDataFile = auth0ManagementAPI.getSamlMetaDataFile("abcdefg");
      assertThat(samlMetaDataFile).isNotNull().content().isEqualTo("saml content");
    }
    finally {
      if (samlMetaDataFile != null) {
        Files.delete(samlMetaDataFile.toPath());
      }
    }
  }

  @Test
  public void testGetSamlMetaData() throws Exception {
    doReturn("saml content").when(auth0ManagementAPI)
        .downloadSamlMetadata("https://sonatype.auth0.com/samlp/metadata/abcdefg");

    String samlMetaDataFile = auth0ManagementAPI.getSamlMetaData("abcdefg");
    assertThat(samlMetaDataFile).isNotNull().isEqualTo("saml content");
  }

  @Test
  public void testGetSamlMetaData_NoAuth0Content() {
    doReturn(null).when(auth0ManagementAPI)
        .downloadSamlMetadata("https://sonatype.auth0.com/samlp/metadata/abcdefg");
    File samlMetaDataFile = auth0ManagementAPI.getSamlMetaDataFile("abcdefg");
    assertThat(samlMetaDataFile).isNull();
  }

  private Client mockClient(String name, String description, String logoUrl) {
    Client client = new Client(name);
    client.setDescription(description);
    client.setLogoUri(logoUrl);
    return client;
  }
}
