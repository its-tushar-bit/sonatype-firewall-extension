/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.v2.dto.ApiSamlConfigurationDTO;
import com.sonatype.insight.brain.audit.AuditDTO;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.configuration.saml.SamlConfigurationService;
import com.sonatype.insight.brain.model.configuration.saml.SamlConfiguration;
import com.sonatype.insight.brain.service.AbstractAuditTest;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ApiSamlConfigurationResourceAuditTest
    extends AbstractAuditTest
{
  private static final String ENTITY_ID = "http://iqserver";

  private SamlConfigurationService samlConfigurationService;

  private String xml;

  private ApiSamlConfigurationDTO apiSamlConfigurationDTO;

  @Before
  public void before() throws IOException {
    samlConfigurationService = lookup(SamlConfigurationService.class);

    if (xml == null) {
      URL resource = getClass().getResource(
          "/" + ApiSamlConfigurationResourceAuditTest.class.getSimpleName() + "/identity-provider-metadata.xml");
      xml = FileUtils.readFileToString(new File(resource.getFile()), StandardCharsets.UTF_8);
    }
    apiSamlConfigurationDTO = new ApiSamlConfigurationDTO();
    apiSamlConfigurationDTO.entityId = ENTITY_ID;
    apiSamlConfigurationDTO.validateResponseSignature = true;
    apiSamlConfigurationDTO.validateAssertionSignature = false;
  }

  @After
  public void cleanup() {
    samlConfigurationService.delete();
  }

  @Test
  public void testInsertOrUpdateSamlConfiguration_Insert() throws Exception {
    restRequest().part("identityProviderXml", xml).part("samlConfiguration", apiSamlConfigurationDTO).put();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.CONFIGURE_SAML, null);
    assertAuditData(auditDTO);
  }

  @Test
  public void testInsertOrUpdateSamlConfiguration_Update() throws Exception {
    tempEntity.newSamlConfiguration(xml, ENTITY_ID);

    restRequest().part("identityProviderXml", xml).part("samlConfiguration", apiSamlConfigurationDTO).put();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.CONFIGURE_SAML, null);
    assertAuditData(auditDTO);
  }

  @Test
  public void testInsertOrUpdateSamlConfiguration_Unauthorized() throws Exception {
    tempEntity.newSamlConfiguration(xml, ENTITY_ID);

    restRequest().with(unauthorizedUser())
        .part("identityProviderXml", xml)
        .part("samlConfiguration", apiSamlConfigurationDTO)
        .put();

    assertAuditLog(AuditEvent.CONFIGURE_SAML, "unauthorized");
  }

  @Test
  public void testDeleteSamlConfiguration() throws Exception {
    SamlConfiguration samlConfiguration = tempEntity.newSamlConfiguration(xml, ENTITY_ID);
    samlConfigurationService.insert(samlConfiguration);

    restRequest().delete();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.DELETE_SAML, null);
    assertAuditData(auditDTO, samlConfiguration);
  }

  @Test
  public void testDeleteSamlConfiguration_Unauthorized() throws Exception {
    tempEntity.newSamlConfiguration(xml, ENTITY_ID);

    restRequest().with(unauthorizedUser()).delete();

    assertAuditLog(AuditEvent.DELETE_SAML, "unauthorized");
  }

  @Override
  protected HttpRequest restRequest() {
    return super.restRequest().path(PublicApiPaths.SAML_CONFIG_RESOURCE_PATH_V2);
  }

  private void assertAuditData(AuditDTO auditDTO) {
    SamlConfiguration samlConfiguration = samlConfigurationService.get();
    assertAuditData(auditDTO, samlConfiguration);
  }

  private void assertAuditData(AuditDTO auditDTO, SamlConfiguration samlConfiguration) {
    assertThat(auditDTO.data)
        .containsEntry("identityProviderName", samlConfiguration.getIdentityProviderName())
        .containsEntry("entityId", samlConfiguration.getEntityId())
        .containsEntry("firstNameAttributeName", samlConfiguration.getFirstNameAttributeName())
        .containsEntry("lastNameAttributeName", samlConfiguration.getLastNameAttributeName())
        .containsEntry("userNameAttributeName", samlConfiguration.getUsernameAttributeName())
        .containsEntry("emailAttributeName", samlConfiguration.getEmailAttributeName())
        .containsEntry("groupsAttributeName", samlConfiguration.getGroupsAttributeName())
        .containsEntry("identityProviderEntityId", "http://idp-entity-id");
    assertCustomData(auditDTO, "validateResponseSignature", samlConfiguration.getValidateResponseSignature());
    assertCustomData(auditDTO, "validateAssertionSignature", samlConfiguration.getValidateAssertionSignature());
  }
}
