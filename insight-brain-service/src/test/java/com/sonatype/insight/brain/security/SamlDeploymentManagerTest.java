/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Base64;

import javax.inject.Inject;

import com.sonatype.insight.brain.model.configuration.saml.SamlConfiguration;
import com.sonatype.insight.brain.service.AbstractComponentTest;

import com.google.common.io.Resources;
import org.junit.Test;
import org.keycloak.adapters.saml.SamlDeployment;
import org.keycloak.adapters.saml.SamlDeployment.Binding;
import org.keycloak.adapters.saml.SamlDeployment.IDP;
import org.keycloak.adapters.saml.SamlDeployment.IDP.SingleLogoutService;
import org.keycloak.adapters.saml.SamlDeployment.IDP.SingleSignOnService;
import org.keycloak.common.enums.SslRequired;
import org.keycloak.saml.SignatureAlgorithm;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

public class SamlDeploymentManagerTest
    extends AbstractComponentTest
{
  @Inject
  private SamlDeploymentManager samlDeploymentManager;

  private String getSamlMetadata(String resourceName) {
    try {
      return Resources.toString(getClass().getResource("/" + getClass().getSimpleName() + "/" + resourceName),
          StandardCharsets.UTF_8);
    }
    catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  @Test
  public void testGet_NotLoaded() {
    assertThat(samlDeploymentManager.get()).isNull();
  }

  @Test
  public void testStart_ValidConfiguration() {
    tempEntity.newSamlConfiguration(getSamlMetadata("valid.xml"), "sp-entity-id");
    samlDeploymentManager.start();
    SamlDeployment samlDeployment = samlDeploymentManager.get();
    assertThat(samlDeployment.getEntityID()).isEqualTo("sp-entity-id");
    assertThat(samlDeployment.getIDP().getEntityID()).isEqualTo("idp-entity-id");
  }

  @Test
  public void testStart_InvalidConfiguration() {
    tempEntity.newSamlConfiguration("", "sp-entity-id");
    samlDeploymentManager.start();
    assertThat(samlDeploymentManager.get()).isNull();
  }

  @Test
  public void testUpdateFromConfiguration_NotConfigured() {
    samlDeploymentManager.updateFromConfiguration();
    assertThat(samlDeploymentManager.get()).isNull();
  }

  @Test
  public void testUpdateFromConfiguration_InvalidConfiguration() {
    tempEntity.newSamlConfiguration();
    assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> {
      samlDeploymentManager.updateFromConfiguration();
    }).withMessageContaining("Invalid SAML metadata");
    assertThat(samlDeploymentManager.get()).isNull();
  }

  @Test
  public void testUpdateFromConfiguration_ValidConfiguration() {
    SamlConfiguration samlConfiguration = tempEntity.newSamlConfiguration(getSamlMetadata("valid.xml"), "sp-entity-id");
    samlDeploymentManager.updateFromConfiguration();

    SamlDeployment samlDeployment = samlDeploymentManager.get();
    assertThat(samlDeployment.getEntityID()).isEqualTo("sp-entity-id");
    assertThat(samlDeployment.getSslRequired()).isEqualTo(SslRequired.EXTERNAL);
    assertThat(samlDeployment.getNameIDPolicyFormat())
        .isEqualTo("urn:oasis:names:tc:SAML:1.1:nameid-format:unspecified");
    assertThat(samlDeployment.getEntityID()).isEqualTo("sp-entity-id");
    assertThat(samlDeployment.isForceAuthentication()).isFalse();
    assertThat(samlDeployment.isIsPassive()).isFalse();
    assertThat(samlDeployment.turnOffChangeSessionIdOnLogin()).isFalse();
    assertThat(samlDeployment.isAutodetectBearerOnly()).isTrue();
    assertThat(samlDeployment.getSignatureAlgorithm()).isEqualTo(SignatureAlgorithm.RSA_SHA256);
    assertThat(samlDeployment.getSignatureCanonicalizationMethod())
        .isEqualTo("http://www.w3.org/2001/10/xml-exc-c14n#");
    assertThat(samlDeployment.getDecryptionKey().getEncoded())
        .isEqualTo(samlConfiguration.getDecryptionKey().getEncoded());
    assertThat(samlDeployment.getSigningKeyPair().getPrivate().getEncoded())
        .isEqualTo(samlConfiguration.getSigningKeyPair().getPrivate().getEncoded());
    assertThat(samlDeployment.getSigningKeyPair().getPublic().getEncoded())
        .isEqualTo(samlConfiguration.getSigningKeyPair().getPublic().getEncoded());

    IDP idp = samlDeployment.getIDP();
    assertThat(idp.getEntityID()).isEqualTo("idp-entity-id");

    assertThat(idp.getSignatureValidationKeyLocator()).isInstanceOf(Iterable.class);
    Iterable<?> keys = (Iterable<?>) idp.getSignatureValidationKeyLocator();
    assertThat(keys).hasSize(1).first().isInstanceOf(Key.class);
    Key key = (Key) keys.iterator().next();
    assertThat(Base64.getEncoder().encodeToString(key.getEncoded())).hasSize(392)
        .startsWith("MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAhIqgoeBkEgsQReJq7lZv")
        .endsWith("k/hdn/uY41/Q82JMtVVNukpid0hpXBoDqqpADT/JQopvrAoRAqrfKhgtQwIDAQAB");

    SingleSignOnService ssoService = idp.getSingleSignOnService();
    assertThat(ssoService.getAssertionConsumerServiceUrl()).isNull();
    assertThat(ssoService.signRequest()).isTrue();
    assertThat(ssoService.validateAssertionSignature()).isTrue();
    assertThat(ssoService.validateResponseSignature()).isTrue();
    assertThat(ssoService.getRequestBinding()).isEqualTo(Binding.POST);
    assertThat(ssoService.getRequestBindingUrl()).isEqualTo("http://localhost:8080/sso");
    assertThat(ssoService.getResponseBinding()).isNull();

    SingleLogoutService sloService = idp.getSingleLogoutService();
    assertThat(sloService.signRequest()).isTrue();
    assertThat(sloService.signResponse()).isTrue();
    assertThat(sloService.validateRequestSignature()).isTrue();
    assertThat(sloService.validateResponseSignature()).isTrue();
    assertThat(sloService.getRequestBinding()).isEqualTo(Binding.REDIRECT);
    assertThat(sloService.getRequestBindingUrl()).isEqualTo("http://localhost:8080/slo");
    assertThat(sloService.getResponseBinding()).isEqualTo(Binding.REDIRECT);
    assertThat(sloService.getResponseBindingUrl()).isEqualTo("http://localhost:8080/slo");
  }

  @Test
  public void testUpdateFromConfiguration_NoRequestSigning() {
    tempEntity.newSamlConfiguration(getSamlMetadata("no-request-signing.xml"), "sp-entity-id");
    samlDeploymentManager.updateFromConfiguration();

    SamlDeployment samlDeployment = samlDeploymentManager.get();
    IDP idp = samlDeployment.getIDP();
    SingleSignOnService ssoService = idp.getSingleSignOnService();
    assertThat(ssoService.signRequest()).isFalse();
    SingleLogoutService sloService = idp.getSingleLogoutService();
    assertThat(sloService.signRequest()).isFalse();
    assertThat(sloService.signResponse()).isFalse();
  }

  @Test
  public void testUpdateFromConfiguration_EncryptionVsSigningKeys() {
    tempEntity.newSamlConfiguration(getSamlMetadata("encryption-vs-signing-keys.xml"), "sp-entity-id");
    samlDeploymentManager.updateFromConfiguration();

    SamlDeployment samlDeployment = samlDeploymentManager.get();
    IDP idp = samlDeployment.getIDP();
    assertThat(idp.getSignatureValidationKeyLocator()).isInstanceOf(Iterable.class);
    Iterable<?> keys = (Iterable<?>) idp.getSignatureValidationKeyLocator();
    assertThat(keys).hasSize(1).first().isInstanceOf(Key.class);
    Key key = (Key) keys.iterator().next();
    assertThat(Base64.getEncoder().encodeToString(key.getEncoded())).hasSize(392)
        .startsWith("MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAhIqgoeBkEgsQReJq7lZv")
        .endsWith("k/hdn/uY41/Q82JMtVVNukpid0hpXBoDqqpADT/JQopvrAoRAqrfKhgtQwIDAQAB");
  }

  @Test
  public void testUpdateFromConfiguration_MultiUseKey() {
    tempEntity.newSamlConfiguration(getSamlMetadata("multi-use-key.xml"), "sp-entity-id");
    samlDeploymentManager.updateFromConfiguration();

    SamlDeployment samlDeployment = samlDeploymentManager.get();
    IDP idp = samlDeployment.getIDP();
    assertThat(idp.getSignatureValidationKeyLocator()).isInstanceOf(Iterable.class);
    Iterable<?> keys = (Iterable<?>) idp.getSignatureValidationKeyLocator();
    assertThat(keys).hasSize(1).first().isInstanceOf(Key.class);
    Key key = (Key) keys.iterator().next();
    assertThat(Base64.getEncoder().encodeToString(key.getEncoded())).hasSize(392)
        .startsWith("MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAhIqgoeBkEgsQReJq7lZv")
        .endsWith("k/hdn/uY41/Q82JMtVVNukpid0hpXBoDqqpADT/JQopvrAoRAqrfKhgtQwIDAQAB");
  }

  @Test
  public void testUpdateFromConfiguration_NoSigningKeys() {
    tempEntity.newSamlConfiguration(getSamlMetadata("no-signing-keys.xml"), "sp-entity-id");
    samlDeploymentManager.updateFromConfiguration();

    SamlDeployment samlDeployment = samlDeploymentManager.get();
    IDP idp = samlDeployment.getIDP();
    assertThat(idp.getSignatureValidationKeyLocator()).isInstanceOf(Iterable.class);
    Iterable<?> keys = (Iterable<?>) idp.getSignatureValidationKeyLocator();
    assertThat(keys).isEmpty();

    SingleSignOnService ssoService = idp.getSingleSignOnService();
    assertThat(ssoService.validateAssertionSignature()).isFalse();
    assertThat(ssoService.validateResponseSignature()).isFalse();

    SingleLogoutService sloService = idp.getSingleLogoutService();
    assertThat(sloService.validateRequestSignature()).isFalse();
    assertThat(sloService.validateResponseSignature()).isFalse();
  }

  @Test
  public void testUpdateFromConfiguration_PostVsRedirectSso() {
    tempEntity.newSamlConfiguration(getSamlMetadata("post-vs-redirect-sso.xml"), "sp-entity-id");
    samlDeploymentManager.updateFromConfiguration();

    SamlDeployment samlDeployment = samlDeploymentManager.get();
    SingleSignOnService ssoService = samlDeployment.getIDP().getSingleSignOnService();
    assertThat(ssoService.getRequestBinding()).isEqualTo(Binding.POST);
    assertThat(ssoService.getRequestBindingUrl()).isEqualTo("http://localhost:8080/post");
  }

  @Test
  public void testUpdateFromConfiguration_PostVsRedirectSlo() {
    tempEntity.newSamlConfiguration(getSamlMetadata("post-vs-redirect-slo.xml"), "sp-entity-id");
    samlDeploymentManager.updateFromConfiguration();

    SamlDeployment samlDeployment = samlDeploymentManager.get();
    SingleLogoutService sloService = samlDeployment.getIDP().getSingleLogoutService();
    assertThat(sloService.getRequestBinding()).isEqualTo(Binding.POST);
    assertThat(sloService.getRequestBindingUrl()).isEqualTo("http://localhost:8080/post");
  }

  @Test
  public void testUpdateFromConfiguration_NoSlo() {
    tempEntity.newSamlConfiguration(getSamlMetadata("no-slo.xml"), "sp-entity-id");
    samlDeploymentManager.updateFromConfiguration();

    SamlDeployment samlDeployment = samlDeploymentManager.get();
    assertThat(samlDeployment.getIDP().getSingleLogoutService()).isNull();
  }
}
