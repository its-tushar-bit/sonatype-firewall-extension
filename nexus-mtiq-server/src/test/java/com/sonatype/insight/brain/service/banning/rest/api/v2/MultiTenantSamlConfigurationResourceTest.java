/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service.banning.rest.api.v2;

import com.sonatype.insight.brain.common.test.SlowTest;

import java.io.File;
import java.io.StringReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import javax.xml.transform.stream.StreamSource;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.v2.ApiSamlConfigurationResource;
import com.sonatype.insight.brain.api.v2.dto.ApiSamlConfigurationDTO;
import com.sonatype.insight.brain.configuration.saml.SamlConfigurationService;
import com.sonatype.insight.brain.model.configuration.saml.SamlConfiguration;
import com.sonatype.insight.brain.security.SamlDeploymentManager;
import com.sonatype.insight.brain.service.AbstractMultiTenantBaseIntegrationTest;

import org.apache.commons.io.FileUtils;
import org.junit.Test;
import org.keycloak.dom.saml.v2.metadata.EntityDescriptorType;
import org.keycloak.dom.saml.v2.metadata.SPSSODescriptorType;
import org.keycloak.saml.processing.core.parsers.saml.SAMLParser;
import org.keycloak.saml.processing.core.util.JAXPValidationUtil;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.experimental.categories.Category;

@Category(SlowTest.class)
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

    SamlDeploymentManager samlDeploymentManager = getCLMServer().getInstance(SamlDeploymentManager.class);
    samlDeploymentManager.updateFromConfiguration();

    HttpResponse response = restRequest().path(ApiSamlConfigurationResource.METADATA).get();
    assertResponseStatus(200, response);

    String xmlMetadata = response.getBodyText();
    JAXPValidationUtil.validator().validate(new StreamSource(new StringReader(xmlMetadata)));
    Object parsed = SAMLParser.getInstance().parse(new StreamSource(new StringReader(xmlMetadata)));
    assertThat(parsed).isInstanceOf(EntityDescriptorType.class);

    EntityDescriptorType entityDescriptorType = (EntityDescriptorType) parsed;
    assertThat(entityDescriptorType.getChoiceType()).hasSize(1);
    assertThat(entityDescriptorType.getChoiceType().get(0).getDescriptors()).hasSize(1);

    SPSSODescriptorType spssoDescriptorType =
        entityDescriptorType.getChoiceType().get(0).getDescriptors().get(0).getSpDescriptor();
    assertThat(spssoDescriptorType).isNotNull();
  }

  private String validIdentityProviderXml() throws Exception {
    URL resource = MultiTenantSamlConfigurationResourceTest.class.getResource(
        "/" + MultiTenantSamlConfigurationResourceTest.class.getSimpleName() + "/identity-provider-metadata.xml");
    return FileUtils.readFileToString(new File(resource.getFile()), StandardCharsets.UTF_8);
  }
}
