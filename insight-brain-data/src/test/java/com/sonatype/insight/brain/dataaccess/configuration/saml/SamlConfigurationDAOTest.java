/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.configuration.saml;

import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.Date;

import com.sonatype.insight.brain.dataaccess.AbstractDbDAOTest;
import com.sonatype.insight.brain.model.configuration.saml.SamlConfiguration;
import com.sonatype.insight.error.exception.BadRequestException;

import org.junit.Test;

import static com.sonatype.insight.brain.dataaccess.configuration.saml.SamlConfigurationInternalDAO.TEN_YEARS_IN_SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class SamlConfigurationDAOTest
    extends AbstractDbDAOTest
{
  private SamlConfigurationDAO dao = new SamlConfigurationDAO();

  @Test
  public void testCRUD() throws Exception {
    // Create
    Date before = new Date();
    SamlConfiguration samlConfiguration = tempEntity.newSamlConfiguration();
    Date after = new Date();
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
    X509Certificate certificate = (X509Certificate) samlConfiguration.getCertificate();
    Date beforeExpiration = new Date(before.getTime() + TEN_YEARS_IN_SECONDS * 1000 - 1000);
    Date afterExpiration = new Date(after.getTime() + TEN_YEARS_IN_SECONDS * 1000 + 1000);
    Date certificateExpiration = certificate.getNotAfter();
    assertThat(certificateExpiration).isBetween(beforeExpiration, afterExpiration);
    PrivateKey decryptionKey = samlConfiguration.getDecryptionKey();
    assertThat(decryptionKey).isNotNull();
    KeyPair signingKeyPair = samlConfiguration.getSigningKeyPair();
    assertThat(signingKeyPair.getPrivate()).isNotNull();
    assertThat(signingKeyPair.getPublic()).isNotNull();

    // Update
    samlConfiguration.setFirstNameAttributeName("updated firstname");
    samlConfiguration.setCertificate(null);
    samlConfiguration.setDecryptionKey(null);
    samlConfiguration.setSigningKeyPair(null);
    dao.update(samlConfiguration);
    samlConfiguration = dao.getById(samlConfigurationId);
    assertThat(samlConfiguration.getFirstNameAttributeName()).isEqualTo("updated firstname");
    assertThat(samlConfiguration.getCertificate().toString()).isEqualTo(certificate.toString());
    assertThat(samlConfiguration.getDecryptionKey().getEncoded()).isEqualTo(decryptionKey.getEncoded());
    assertThat(samlConfiguration.getSigningKeyPair().getPrivate().getEncoded())
        .isEqualTo(signingKeyPair.getPrivate().getEncoded());
    assertThat(samlConfiguration.getSigningKeyPair().getPublic().getEncoded())
        .isEqualTo(signingKeyPair.getPublic().getEncoded());

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
