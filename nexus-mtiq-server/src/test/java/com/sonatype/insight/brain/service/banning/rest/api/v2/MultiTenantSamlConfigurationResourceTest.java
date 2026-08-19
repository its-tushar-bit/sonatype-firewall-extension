/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service.banning.rest.api.v2;

import java.io.File;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.v2.ApiSamlConfigurationResource;
import com.sonatype.insight.brain.api.v2.dto.ApiSamlConfigurationDTO;
import com.sonatype.insight.brain.configuration.saml.SamlConfigurationService;
import com.sonatype.insight.brain.model.configuration.saml.SamlConfiguration;
import com.sonatype.insight.brain.service.AbstractMultiTenantBaseIntegrationTest;

import org.apache.commons.io.FileUtils;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class MultiTenantSamlConfigurationResourceTest
    extends AbstractMultiTenantBaseIntegrationTest
{
  @Override
  protected HttpRequest restRequest() {
    return super.restRequest().path(PublicApiPaths.SAML_CONFIG_RESOURCE_PATH_V2);
  }

  @Test
  public void test_getSamlConfiguration_shouldBeBanned() throws Exception {
    HttpResponse response = restRequest().get();
    assertResponseStatus(404, response);
  }

  @Test
  public void test_insertOrUpdateSamlConfiguration_shouldBeBanned() throws Exception {
    disableSamlByConfiguration();

    String xml = validIdentityProviderXml();
    ApiSamlConfigurationDTO apiSamlConfigurationDTO = new ApiSamlConfigurationDTO();
    HttpResponse response =
        restRequest().part("identityProviderXml", xml).part("samlConfiguration", apiSamlConfigurationDTO).put();
    assertResponseStatus(404, response);
  }

  @Test
  public void test_insertOrUpdateSamlConfiguration_shouldBeAvailable() throws Exception {
    enableSamlByConfiguration();

    String xml = validIdentityProviderXml();
    ApiSamlConfigurationDTO apiSamlConfigurationDTO = new ApiSamlConfigurationDTO();
    HttpResponse response =
        restRequest().part("identityProviderXml", xml).part("samlConfiguration", apiSamlConfigurationDTO).put();
    assertResponseStatus(204, response);
  }

  @Test
  public void test_deleteSamlConfiguration_shouldBeBanned() throws Exception {
    HttpResponse response = restRequest().delete();
    assertResponseStatus(404, response);
  }

  @Test
  public void test_getMetadata() throws Exception {
    enableSamlByConfiguration();

    SamlConfigurationService samlConfigurationService = lookup(SamlConfigurationService.class);
    SamlConfiguration samlConfiguration =
        tenantTemporaryEntity.newSamlConfiguration("My Awesome IdP", validIdentityProviderXml(), "ent-id", "first-name",
            "last-name", "e-mail", "user-name", "teams", null, null);
    samlConfigurationService.insert(samlConfiguration);

    HttpResponse response = restRequest().path(ApiSamlConfigurationResource.METADATA).get();
    assertResponseStatus(200, response);

    String xmlMetadata = response.getBodyText();
    assertThat(xmlMetadata).startsWith("<?xml");
    assertThat(xmlMetadata).contains("EntityDescriptor");
    assertThat(xmlMetadata).contains("SPSSODescriptor");
    assertThat(xmlMetadata).contains("AssertionConsumerService");
    // ACS endpoint + binding, and both SP key descriptors (signing + encryption) — SAML interop guarantees.
    assertThat(xmlMetadata).contains("urn:oasis:names:tc:SAML:2.0:bindings:HTTP-POST");
    assertThat(xmlMetadata).contains("/saml");
    assertThat(xmlMetadata).contains("use=\"signing\"");
    assertThat(xmlMetadata).contains("use=\"encryption\"");
  }

  private String validIdentityProviderXml() throws Exception {
    URL resource = MultiTenantSamlConfigurationResourceTest.class.getResource(
        "/" + MultiTenantSamlConfigurationResourceTest.class.getSimpleName() + "/identity-provider-metadata.xml");
    return FileUtils.readFileToString(new File(resource.getFile()), StandardCharsets.UTF_8);
  }
}
