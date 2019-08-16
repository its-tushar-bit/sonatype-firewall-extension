/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.configuration.saml;

import com.sonatype.insight.brain.dataaccess.AbstractDbDAOTest;
import com.sonatype.insight.brain.model.configuration.saml.SamlConfiguration;
import com.sonatype.insight.error.exception.BadRequestException;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class SamlConfigurationDAOTest
    extends AbstractDbDAOTest
{
  private SamlConfigurationDAO dao = new SamlConfigurationDAO();

  @Test
  public void testCRUD() throws Exception {
    // Create
    SamlConfiguration samlConfiguration = tempEntity.newSamlConfiguration();
    String samlConfigurationId = samlConfiguration.getId();

    // Read
    samlConfiguration = dao.getById(samlConfigurationId);
    assertThat(samlConfiguration.getIdentityProviderMetadataXml()).isNull();
    assertThat(samlConfiguration.getEntityId()).isNull();
    assertThat(samlConfiguration.getFirstNameAttributeName()).isEqualTo("firstName");
    assertThat(samlConfiguration.getLastNameAttributeName()).isEqualTo("lastName");
    assertThat(samlConfiguration.getEmailAttributeName()).isEqualTo("email");
    assertThat(samlConfiguration.getUsernameAttributeName()).isEqualTo("username");
    assertThat(samlConfiguration.getGroupsAttributeName()).isEqualTo("groups");

    // Update
    samlConfiguration.setFirstNameAttributeName("updated firstname");
    dao.update(samlConfiguration);
    samlConfiguration = dao.getById(samlConfigurationId);
    assertThat(samlConfiguration.getFirstNameAttributeName()).isEqualTo("updated firstname");

    // Delete
    dao.delete(samlConfiguration);
    samlConfiguration = dao.getById(samlConfigurationId);
    assertThat(samlConfiguration).isNull();
  }

  @Test
  public void testInsert_MoreThanOneSamlConfigurations() {
    tempEntity.newSamlConfiguration();

    assertThatThrownBy(() -> {
      tempEntity.newSamlConfiguration();
    }).isInstanceOf(BadRequestException.class).hasMessage("A SAML configuration already exists.");
  }
}
