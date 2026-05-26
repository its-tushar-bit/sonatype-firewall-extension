/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import static com.sonatype.insight.brain.tenancy.TenantTestHelper.testAsNewTenant;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.io.Resources;
import com.sonatype.insight.brain.configuration.saml.SamlConfigurationService;
import com.sonatype.insight.brain.model.configuration.saml.SamlConfiguration;
import com.sonatype.insight.brain.scheduler.TaskScheduler;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import jakarta.inject.Inject;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Base64;
import org.junit.Test;
import org.keycloak.adapters.saml.SamlDeployment;
import org.keycloak.adapters.saml.SamlDeployment.Binding;
import org.keycloak.adapters.saml.SamlDeployment.IDP;
import org.keycloak.adapters.saml.SamlDeployment.IDP.SingleLogoutService;
import org.keycloak.adapters.saml.SamlDeployment.IDP.SingleSignOnService;
import org.keycloak.adapters.saml.SamlPrincipal;
import org.keycloak.common.enums.SslRequired;
import org.keycloak.saml.SignatureAlgorithm;
import org.mockito.Mock;
import org.quartz.JobBuilder;
import org.quartz.JobExecutionContext;
import org.slf4j.MDC;

public class SamlDeploymentManagerTest
    extends AbstractComponentTest
{
  @Inject
  private SamlDeploymentManager samlDeploymentManager;

  @Inject
  private SamlMetadataTool samlMetadataTool;

  @Mock
  private TaskScheduler taskSchedulerMock;

  private String getSamlMetadata(String resourceName) {
    try {
      URL resource = SamlDeploymentManagerTest.class.getResource(
          "/" + SamlDeploymentManagerTest.class.getSimpleName() + "/" + resourceName);
      return Resources.toString(resource, StandardCharsets.UTF_8);
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
    SamlConfiguration samlConfiguration = tempEntity.newSamlConfiguration(getSamlMetadata("valid.xml"), "sp-entity-id");
    samlConfigurationService.insert(samlConfiguration);
    samlDeploymentManager.register();
    SamlDeployment samlDeployment = samlDeploymentManager.get();
    assertThat(samlDeployment.getEntityID()).isEqualTo("sp-entity-id");
    assertThat(samlDeployment.getIDP().getEntityID()).isEqualTo("idp-entity-id");
  }

  @Test
  public void testStart_InvalidConfiguration() {
    tempEntity.newSamlConfiguration("", "sp-entity-id");
    samlDeploymentManager.register();
    assertThat(samlDeploymentManager.get()).isNull();
  }

  @Test
  public void testUpdateFromConfiguration_NotConfigured() {
    samlDeploymentManager.updateFromConfiguration();
    assertThat(samlDeploymentManager.get()).isNull();
  }

  @Test
  public void testUpdateFromConfiguration_InvalidConfiguration() {
    SamlConfiguration samlConfiguration = tempEntity.newSamlConfiguration(null, null);
    samlConfigurationService.insert(samlConfiguration);
    assertThatExceptionOfType(IllegalArgumentException.class)
        .isThrownBy(() -> samlDeploymentManager.updateFromConfiguration())
        .withMessageContaining("Invalid SAML metadata");
    assertThat(samlDeploymentManager.get()).isNull();
  }

  @Test
  public void testUpdateFromConfiguration_SigningKeyWithoutCertificate() {
    SamlConfiguration samlConfiguration = tempEntity.newSamlConfiguration(getSamlMetadata("no-certificate.xml"), null);
    samlConfigurationService.insert(samlConfiguration);
    assertThatExceptionOfType(IllegalArgumentException.class)
        .isThrownBy(() -> samlDeploymentManager.updateFromConfiguration())
        .withMessageContaining("SAML metadata for identity provider contains invalid certificate");
    assertThat(samlDeploymentManager.get()).isNull();
  }

  @Test
  public void testUpdateFromConfiguration_ValidConfiguration() {
    SamlConfiguration samlConfiguration = tempEntity.newSamlConfiguration(getSamlMetadata("valid.xml"), "sp-entity-id");
    samlConfigurationService.insert(samlConfiguration);
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
    assertThat(samlDeployment.getRoleAttributeNames())
        .containsExactly(SamlPrincipal.DEFAULT_ROLE_ATTRIBUTE_NAME);

    IDP idp = samlDeployment.getIDP();
    assertThat(idp.getEntityID()).isEqualTo("idp-entity-id");

    assertThat(idp.getSignatureValidationKeyLocator()).isInstanceOf(Iterable.class);
    Iterable<?> keys = idp.getSignatureValidationKeyLocator();
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

    assertThat(idp.getAllowedClockSkew()).isEqualTo(SamlDeploymentManager.ALLOWED_CLOCK_SKEW_MILLISECONDS);
  }

  @Test
  public void testUpdateFromConfiguration_NoRequestSigning() {
    SamlConfiguration samlConfiguration =
        tempEntity.newSamlConfiguration(getSamlMetadata("no-request-signing.xml"), "sp-entity-id");
    samlConfigurationService.insert(samlConfiguration);
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
    SamlConfiguration samlConfiguration =
        tempEntity.newSamlConfiguration(getSamlMetadata("encryption-vs-signing-keys.xml"), "sp-entity-id");
    samlConfigurationService.insert(samlConfiguration);
    samlDeploymentManager.updateFromConfiguration();

    SamlDeployment samlDeployment = samlDeploymentManager.get();
    IDP idp = samlDeployment.getIDP();
    assertThat(idp.getSignatureValidationKeyLocator()).isInstanceOf(Iterable.class);
    Iterable<?> keys = idp.getSignatureValidationKeyLocator();
    assertThat(keys).hasSize(1).first().isInstanceOf(Key.class);
    Key key = (Key) keys.iterator().next();
    assertThat(Base64.getEncoder().encodeToString(key.getEncoded())).hasSize(392)
        .startsWith("MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAhIqgoeBkEgsQReJq7lZv")
        .endsWith("k/hdn/uY41/Q82JMtVVNukpid0hpXBoDqqpADT/JQopvrAoRAqrfKhgtQwIDAQAB");
  }

  @Test
  public void testUpdateFromConfiguration_MultiUseKey() {
    SamlConfiguration samlConfiguration =
        tempEntity.newSamlConfiguration(getSamlMetadata("multi-use-key.xml"), "sp-entity-id");
    samlConfigurationService.insert(samlConfiguration);
    samlDeploymentManager.updateFromConfiguration();

    SamlDeployment samlDeployment = samlDeploymentManager.get();
    IDP idp = samlDeployment.getIDP();
    assertThat(idp.getSignatureValidationKeyLocator()).isInstanceOf(Iterable.class);
    Iterable<?> keys = idp.getSignatureValidationKeyLocator();
    assertThat(keys).hasSize(1).first().isInstanceOf(Key.class);
    Key key = (Key) keys.iterator().next();
    assertThat(Base64.getEncoder().encodeToString(key.getEncoded())).hasSize(392)
        .startsWith("MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAhIqgoeBkEgsQReJq7lZv")
        .endsWith("k/hdn/uY41/Q82JMtVVNukpid0hpXBoDqqpADT/JQopvrAoRAqrfKhgtQwIDAQAB");
  }

  @Test
  public void testUpdateFromConfiguration_NoSigningKeys() {
    SamlConfiguration samlConfiguration =
        tempEntity.newSamlConfiguration(getSamlMetadata("no-signing-keys.xml"), "sp-entity-id");
    samlConfigurationService.insert(samlConfiguration);
    samlDeploymentManager.updateFromConfiguration();

    SamlDeployment samlDeployment = samlDeploymentManager.get();
    IDP idp = samlDeployment.getIDP();
    assertThat(idp.getSignatureValidationKeyLocator()).isInstanceOf(Iterable.class);
    Iterable<?> keys = idp.getSignatureValidationKeyLocator();
    assertThat(keys).isEmpty();

    SingleSignOnService ssoService = idp.getSingleSignOnService();
    assertThat(ssoService.validateAssertionSignature()).isFalse();
    assertThat(ssoService.validateResponseSignature()).isFalse();

    SingleLogoutService sloService = idp.getSingleLogoutService();
    assertThat(sloService.validateRequestSignature()).isFalse();
    assertThat(sloService.validateResponseSignature()).isFalse();
  }

  @Test
  public void testUpdateFromConfiguration_NoSigningKeysButResponseSignatureValidationEnabled() {
    SamlConfiguration samlConfiguration =
        tempEntity.newSamlConfiguration("My Awesome IdP", getSamlMetadata("no-signing-keys.xml"), "sp-entity-id",
            "firstName", "lastName", "email", "username", "groups", true, null);
    samlConfigurationService.insert(samlConfiguration);
    assertThatExceptionOfType(IllegalArgumentException.class)
        .isThrownBy(() -> samlDeploymentManager.updateFromConfiguration())
        .withMessageContaining("SAML metadata for identity provider misses signing key");
    assertThat(samlDeploymentManager.get()).isNull();
  }

  @Test
  public void testUpdateFromConfiguration_NoSigningKeysButAssertionSignatureValidationEnabled() {
    SamlConfiguration samlConfiguration =
        tempEntity.newSamlConfiguration("My Awesome IdP", getSamlMetadata("no-signing-keys.xml"), "sp-entity-id",
            "firstName", "lastName", "email", "username", "groups", null, true);
    samlConfigurationService.insert(samlConfiguration);
    assertThatExceptionOfType(IllegalArgumentException.class)
        .isThrownBy(() -> samlDeploymentManager.updateFromConfiguration())
        .withMessageContaining("SAML metadata for identity provider misses signing key");
    assertThat(samlDeploymentManager.get()).isNull();
  }

  @Test
  public void testUpdateFromConfiguration_NoResponseSignatureValidation() {
    SamlConfiguration samlConfiguration =
        tempEntity.newSamlConfiguration("My Awesome IdP", getSamlMetadata("valid.xml"), "sp-entity-id", "firstName",
            "lastName", "email", "username", "groups", false, null);
    samlConfigurationService.insert(samlConfiguration);
    samlDeploymentManager.updateFromConfiguration();

    SamlDeployment samlDeployment = samlDeploymentManager.get();

    SingleSignOnService ssoService = samlDeployment.getIDP().getSingleSignOnService();
    assertThat(ssoService.validateAssertionSignature()).isTrue();
    assertThat(ssoService.validateResponseSignature()).isFalse();
  }

  @Test
  public void testUpdateFromConfiguration_NoAssertionSignatureValidation() {
    SamlConfiguration samlConfiguration =
        tempEntity.newSamlConfiguration("My Awesome IdP", getSamlMetadata("valid.xml"), "sp-entity-id", "firstName",
            "lastName", "email", "username", "groups", null, false);
    samlConfigurationService.insert(samlConfiguration);
    samlDeploymentManager.updateFromConfiguration();

    SamlDeployment samlDeployment = samlDeploymentManager.get();

    SingleSignOnService ssoService = samlDeployment.getIDP().getSingleSignOnService();
    assertThat(ssoService.validateAssertionSignature()).isFalse();
    assertThat(ssoService.validateResponseSignature()).isTrue();
  }

  @Test
  public void testUpdateFromConfiguration_PostVsRedirectSso() {
    SamlConfiguration samlConfiguration =
        tempEntity.newSamlConfiguration(getSamlMetadata("post-vs-redirect-sso.xml"), "sp-entity-id");
    samlConfigurationService.insert(samlConfiguration);
    samlDeploymentManager.updateFromConfiguration();

    SamlDeployment samlDeployment = samlDeploymentManager.get();
    SingleSignOnService ssoService = samlDeployment.getIDP().getSingleSignOnService();
    assertThat(ssoService.getRequestBinding()).isEqualTo(Binding.POST);
    assertThat(ssoService.getRequestBindingUrl()).isEqualTo("http://localhost:8080/post");
  }

  @Test
  public void testUpdateFromConfiguration_PostVsRedirectSlo() {
    SamlConfiguration samlConfiguration =
        tempEntity.newSamlConfiguration(getSamlMetadata("post-vs-redirect-slo.xml"), "sp-entity-id");
    samlConfigurationService.insert(samlConfiguration);
    samlDeploymentManager.updateFromConfiguration();

    SamlDeployment samlDeployment = samlDeploymentManager.get();
    SingleLogoutService sloService = samlDeployment.getIDP().getSingleLogoutService();
    assertThat(sloService.getRequestBinding()).isEqualTo(Binding.POST);
    assertThat(sloService.getRequestBindingUrl()).isEqualTo("http://localhost:8080/post");
  }

  @Test
  public void testUpdateFromConfiguration_NoSlo() {
    SamlConfiguration samlConfiguration =
        tempEntity.newSamlConfiguration(getSamlMetadata("no-slo.xml"), "sp-entity-id");
    samlConfigurationService.insert(samlConfiguration);
    samlDeploymentManager.updateFromConfiguration();

    SamlDeployment samlDeployment = samlDeploymentManager.get();
    assertThat(samlDeployment.getIDP().getSingleLogoutService()).isNull();
  }

  @Test
  public void testExecute() {
    SamlDeploymentManager samlDeploymentManagerSpy = spy(samlDeploymentManager);
    doAnswer(invocationOnMock -> {
      assertThat(MDC.get(MDCUsernameScope.USERNAME)).isEqualTo(MDCUsernameScope.SYSTEM);
      return null;
    }).when(samlDeploymentManagerSpy).updateFromConfiguration();

    try (MDCUsernameScope mdcUsernameScope = MDCUsernameScope.forUser("username")) {
      samlDeploymentManagerSpy.execute(mock(JobExecutionContext.class));
    }

    verify(samlDeploymentManagerSpy).updateFromConfiguration();
  }

  @Test
  public void testUpdateAllClusterNodesFromConfiguration() {
    SamlDeploymentManager samlDeploymentManagerSpy = spy(samlDeploymentManager);

    samlDeploymentManagerSpy.updateAllClusterNodesFromConfiguration();

    verify(samlDeploymentManagerSpy).updateFromConfiguration();
    verify(taskSchedulerMock).scheduleOneTimeTaskForAllOtherNodes(samlDeploymentManagerSpy);
  }

  @Test
  public void testDisallowConcurrentExecution() {
    assertThat(JobBuilder.newJob(SamlDeploymentManager.class).build().isConcurrentExectionDisallowed()).isTrue();
  }

  @Test
  public void testConfigurationsStoredIndependently_ForEachTenant() {
    SamlConfigurationService samlConfigurationService = mock(SamlConfigurationService.class);
    SamlDeploymentManager saml =
        new SamlDeploymentManager(samlMetadataTool, samlConfigurationService, taskSchedulerMock);

    String tenant1EntityId = "sp-entity-id";
    String tenant2EntityId = "sp-entity-id2";

    testAsNewTenant(testName, t1 -> {
      SamlConfiguration tenant1SamlConfiguration = new SamlConfiguration();
      tenant1SamlConfiguration.setIdentityProviderMetadataXml(getSamlMetadata("no-request-signing.xml"));
      tenant1SamlConfiguration.setEntityId(tenant1EntityId);
      when(samlConfigurationService.get()).thenReturn(tenant1SamlConfiguration);

      saml.register();

      SamlDeployment tenant1SamlDeployment = saml.get();

      assertThat(tenant1SamlDeployment.getEntityID()).isEqualTo(tenant1EntityId);
    });

    testAsNewTenant(testName, t2 -> {
      SamlConfiguration tenant2SamlConfiguration = new SamlConfiguration();
      tenant2SamlConfiguration.setIdentityProviderMetadataXml(getSamlMetadata("encryption-vs-signing-keys.xml"));
      tenant2SamlConfiguration.setEntityId(tenant2EntityId);
      when(samlConfigurationService.get()).thenReturn(tenant2SamlConfiguration);

      saml.register();

      SamlDeployment tenant2SamlDeployment = saml.get();

      assertThat(tenant2SamlDeployment.getEntityID()).isEqualTo(tenant2EntityId);
    });
  }
}
