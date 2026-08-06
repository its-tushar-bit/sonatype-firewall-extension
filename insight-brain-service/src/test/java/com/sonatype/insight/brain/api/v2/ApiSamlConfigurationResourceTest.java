/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import java.io.File;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.v2.dto.ApiSamlConfigurationDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiSamlConfigurationResponseDTO;
import com.sonatype.insight.brain.configuration.saml.SamlConfigurationService;
import com.sonatype.insight.brain.model.configuration.saml.SamlConfiguration;
import com.sonatype.insight.brain.service.AbstractResourceTest;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ApiSamlConfigurationResourceTest
    extends AbstractResourceTest
{
  private SamlConfigurationService samlConfigurationService;

  @Before
  public void before() {
    samlConfigurationService = lookup(SamlConfigurationService.class);
  }

  @After
  public void cleanup() {
    samlConfigurationService.delete();
  }

  @Test
  public void testGetSamlConfiguration() throws Exception {
    SamlConfiguration samlConfiguration = tempEntity.newSamlConfiguration("My Awesome IdP", "<xml></xml>", "ent-id",
        "first-name", "last-name", "e-mail", "user-name", "teams", true, null);
    samlConfigurationService.insert(samlConfiguration);

    HttpResponse response = restRequest().get();
    assertResponseStatus(200, restRequest().get());

    ApiSamlConfigurationResponseDTO dto = response.getBody(ApiSamlConfigurationResponseDTO.class);
    assertThat(dto.identityProviderName).isEqualTo(samlConfiguration.getIdentityProviderName());
    assertThat(dto.identityProviderMetadataXml).isEqualTo(samlConfiguration.getIdentityProviderMetadataXml());
    assertThat(dto.entityId).isEqualTo(samlConfiguration.getEntityId());
    assertThat(dto.firstNameAttributeName).isEqualTo(samlConfiguration.getFirstNameAttributeName());
    assertThat(dto.lastNameAttributeName).isEqualTo(samlConfiguration.getLastNameAttributeName());
    assertThat(dto.emailAttributeName).isEqualTo(samlConfiguration.getEmailAttributeName());
    assertThat(dto.usernameAttributeName).isEqualTo(samlConfiguration.getUsernameAttributeName());
    assertThat(dto.groupsAttributeName).isEqualTo(samlConfiguration.getGroupsAttributeName());
    assertThat(dto.validateResponseSignature).isTrue();
    assertThat(dto.validateAssertionSignature).isNull();
  }

  @Test
  public void testInsertOrUpdateSamlConfiguration() throws Exception {
    String xml = validIdentityProviderXml();
    ApiSamlConfigurationDTO apiSamlConfigurationDTO = new ApiSamlConfigurationDTO();
    HttpResponse response =
        restRequest().part("identityProviderXml", xml).part("samlConfiguration", apiSamlConfigurationDTO).put();
    assertResponseStatus(204, response);
  }

  @Test
  public void testDeleteSamlConfiguration() throws Exception {
    String xml = validIdentityProviderXml();
    SamlConfiguration samlConfiguration =
        tempEntity.newSamlConfiguration("My Awesome IdP", xml, "ent-id", "first-name", "last-name", "e-mail",
            "user-name",
            "teams", null, null);
    samlConfigurationService.insert(samlConfiguration);
    assertResponseStatus(204, restRequest().delete());
  }

  @Test
  public void testGetMetadata() throws Exception {
    SamlConfiguration samlConfiguration =
        tempEntity.newSamlConfiguration("My Awesome IdP", validIdentityProviderXml(), "ent-id", "first-name",
            "last-name",
            "e-mail", "user-name", "teams", null, null);
    samlConfigurationService.insert(samlConfiguration);

    HttpResponse response = restRequest().path(ApiSamlConfigurationResource.METADATA).get();

    assertResponseStatus(200, response);
    String xmlMetadata = response.getBodyText();
    assertThat(xmlMetadata).startsWith("<?xml");
    assertThat(xmlMetadata).contains("EntityDescriptor");
    assertThat(xmlMetadata).contains("SPSSODescriptor");
    assertThat(xmlMetadata).contains("AssertionConsumerService");
  }

  private String validIdentityProviderXml() throws Exception {
    URL resource = ApiSamlConfigurationResourceTest.class.getResource(
        "/" + ApiSamlConfigurationResourceTest.class.getSimpleName() + "/identity-provider-metadata.xml");
    return FileUtils.readFileToString(new File(resource.getFile()), StandardCharsets.UTF_8);
  }

  @Override
  protected HttpRequest restRequest() {
    return super.restRequest().path(PublicApiPaths.SAML_CONFIG_RESOURCE_PATH_V2);
  }
}
