/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.configuration.saml;

import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPublicKey;
import java.util.Calendar;
import java.util.Date;
import jakarta.inject.Inject;

import com.sonatype.insight.brain.model.InvalidNameException;
import com.sonatype.insight.brain.model.configuration.saml.SamlConfiguration;
import com.sonatype.insight.brain.variant.AbstractComponentH2Test;
import com.sonatype.insight.brain.variant.ComponentH2Test;
import com.sonatype.insight.error.exception.BadRequestException;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ComponentH2Test
public class SamlConfigurationServiceTest
    extends AbstractComponentH2Test
{
  @Inject
  private SamlConfigurationService samlConfigurationService;

  @Test
  public void testCRUD() {
    // Create
    Date before = new Date();
    SamlConfiguration samlConfiguration = tempEntity.newSamlConfiguration(null, null);
    samlConfigurationService.insert(samlConfiguration);
    Date after = new Date();
    assertSamlConfiguration(samlConfiguration, before, after);

    // Read
    String samlConfigurationId = samlConfiguration.getId();
    samlConfiguration = samlConfigurationService.getById(samlConfigurationId);
    assertSamlConfiguration(samlConfiguration, before, after);

    // Update
    X509Certificate certificate = (X509Certificate) samlConfiguration.getCertificate();
    PrivateKey decryptionKey = samlConfiguration.getDecryptionKey();
    KeyPair signingKeyPair = samlConfiguration.getSigningKeyPair();
    String identityProviderName = "My Awesome IdP";
    samlConfiguration.setIdentityProviderName(identityProviderName);
    samlConfiguration.setFirstNameAttributeName("updated firstname");
    samlConfiguration.setValidateResponseSignature(true);
    samlConfiguration.setValidateAssertionSignature(false);
    samlConfiguration.setCertificate(null);
    samlConfiguration.setDecryptionKey(null);
    samlConfiguration.setSigningKeyPair(null);
    samlConfigurationService.update(samlConfiguration);
    samlConfiguration = samlConfigurationService.getById(samlConfigurationId);
    assertThat(samlConfiguration.getIdentityProviderName()).isEqualTo(identityProviderName);
    assertThat(samlConfiguration.getFirstNameAttributeName()).isEqualTo("updated firstname");
    assertThat(samlConfiguration.getValidateResponseSignature()).isTrue();
    assertThat(samlConfiguration.getValidateAssertionSignature()).isFalse();
    assertThat(samlConfiguration.getCertificate().toString()).isEqualTo(certificate.toString());
    assertThat(samlConfiguration.getDecryptionKey().getEncoded()).isEqualTo(decryptionKey.getEncoded());
    assertThat(samlConfiguration.getSigningKeyPair().getPrivate().getEncoded())
        .isEqualTo(signingKeyPair.getPrivate().getEncoded());
    assertThat(samlConfiguration.getSigningKeyPair().getPublic().getEncoded())
        .isEqualTo(signingKeyPair.getPublic().getEncoded());

    // Delete
    samlConfigurationService.delete();
    samlConfiguration = samlConfigurationService.getById(samlConfigurationId);
    assertThat(samlConfiguration).isNull();
  }

  @Test
  public void testInsert_MoreThanOneSamlConfigurations() {
    samlConfigurationService.insert(tempEntity.newSamlConfiguration());

    assertThatThrownBy(() -> samlConfigurationService.insert(new SamlConfiguration()))
        .isInstanceOf(BadRequestException.class)
        .hasMessage("A SAML configuration already exists.");
  }

  @Test
  public void testInsert_IdentityProviderNameTooLong() {
    SamlConfiguration samlConfiguration = tempEntity
        .newSamlConfiguration(StringUtils.repeat("a", SamlConfiguration.IDENTITY_PROVIDER_NAME_MAXIMUM_LENGTH + 1),
            "<xml></xml>", "ent-id", "name-first", "name-last", "mail-e", "name-user", "teamz", null, null);

    assertThatExceptionOfType(InvalidNameException.class).isThrownBy(
        () -> samlConfigurationService.insert(samlConfiguration))
        .withMessageContaining("Identity provider name")
        .withMessageContaining("characters or less");
  }

  @Test
  public void testUpdate_IdentityProviderNameTooLong() {
    SamlConfiguration samlConfiguration = tempEntity
        .newSamlConfiguration("My Awesome IdP", "<xml></xml>", "ent-id", "name-first", "name-last", "mail-e",
            "name-user", "teamz", null, null);
    samlConfiguration
        .setIdentityProviderName(StringUtils.repeat("a", SamlConfiguration.IDENTITY_PROVIDER_NAME_MAXIMUM_LENGTH + 1));

    assertThatExceptionOfType(InvalidNameException.class)
        .isThrownBy(() -> samlConfigurationService.update(samlConfiguration))
        .withMessageContaining("Identity provider name")
        .withMessageContaining("characters or less");
  }

  @Test
  public void testDelete_Null() {
    assertThat(samlConfigurationService.get()).isNull();
    samlConfigurationService.delete();
  }

  private void assertSamlConfiguration(SamlConfiguration samlConfiguration, Date before, Date after) {
    assertThat(samlConfiguration.getIdentityProviderName()).isEqualTo("identity provider");
    assertThat(samlConfiguration.getIdentityProviderMetadataXml()).isNull();
    assertThat(samlConfiguration.getEntityId()).isNull();
    assertThat(samlConfiguration.getFirstNameAttributeName()).isEqualTo("firstName");
    assertThat(samlConfiguration.getLastNameAttributeName()).isEqualTo("lastName");
    assertThat(samlConfiguration.getEmailAttributeName()).isEqualTo("email");
    assertThat(samlConfiguration.getUsernameAttributeName()).isEqualTo("username");
    assertThat(samlConfiguration.getGroupsAttributeName()).isEqualTo("groups");
    assertThat(samlConfiguration.getValidateResponseSignature()).isNull();
    assertThat(samlConfiguration.getValidateAssertionSignature()).isNull();
    X509Certificate certificate = (X509Certificate) samlConfiguration.getCertificate();
    Calendar beforeExpiration = Calendar.getInstance();
    beforeExpiration.setTime(before);
    beforeExpiration.add(Calendar.YEAR, 10);
    beforeExpiration.add(Calendar.MILLISECOND, -1000);
    Calendar afterExpiration = Calendar.getInstance();
    afterExpiration.setTime(after);
    afterExpiration.add(Calendar.YEAR, 10);
    afterExpiration.add(Calendar.MILLISECOND, 1000);
    Date certificateExpiration = certificate.getNotAfter();
    assertThat(((RSAPublicKey) certificate.getPublicKey()).getModulus().bitLength()).isEqualTo(2048);
    assertThat(certificateExpiration).isBetween(beforeExpiration.getTime(), afterExpiration.getTime());
    PrivateKey decryptionKey = samlConfiguration.getDecryptionKey();
    assertThat(decryptionKey).isNotNull();
    KeyPair signingKeyPair = samlConfiguration.getSigningKeyPair();
    assertThat(signingKeyPair.getPrivate()).isNotNull();
    assertThat(signingKeyPair.getPublic()).isNotNull();
  }
}
