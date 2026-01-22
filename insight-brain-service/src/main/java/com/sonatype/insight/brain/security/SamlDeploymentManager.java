/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import java.security.cert.Certificate;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.configuration.saml.SamlConfigurationService;
import com.sonatype.insight.brain.model.configuration.saml.SamlConfiguration;
import com.sonatype.insight.brain.scheduler.TaskScheduler;
import com.sonatype.insight.brain.service.InsightJob;
import com.sonatype.insight.brain.tenancy.TenantReference;

import org.keycloak.adapters.saml.DefaultSamlDeployment;
import org.keycloak.adapters.saml.DefaultSamlDeployment.DefaultIDP;
import org.keycloak.adapters.saml.DefaultSamlDeployment.DefaultSingleLogoutService;
import org.keycloak.adapters.saml.DefaultSamlDeployment.DefaultSingleSignOnService;
import org.keycloak.adapters.saml.SamlDeployment;
import org.keycloak.adapters.saml.SamlDeployment.Binding;
import org.keycloak.common.enums.SslRequired;
import org.keycloak.dom.saml.v2.metadata.EndpointType;
import org.keycloak.dom.saml.v2.metadata.EntityDescriptorType;
import org.keycloak.dom.saml.v2.metadata.EntityDescriptorType.EDTDescriptorChoiceType;
import org.keycloak.dom.saml.v2.metadata.IDPSSODescriptorType;
import org.keycloak.dom.saml.v2.metadata.KeyDescriptorType;
import org.keycloak.dom.saml.v2.metadata.KeyTypes;
import org.keycloak.saml.SignatureAlgorithm;
import org.keycloak.saml.common.exceptions.ConfigurationException;
import org.keycloak.saml.common.exceptions.ProcessingException;
import org.keycloak.saml.processing.core.saml.v2.util.SAMLMetadataUtil;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.util.stream.Collectors.toList;

/**
 * Provides the runtime configuration for the Keycloak client adapter based on the server's SAML configuration.
 */
@Named
@Singleton
@DisallowConcurrentExecution
public class SamlDeploymentManager
    implements InsightJob
{
  private static final Logger log = LoggerFactory.getLogger(SamlDeploymentManager.class);

  // Visible for testing
  static final String TASK_NAME = "SamlDeployment";

  // A SAML assertion may have a "Conditions" element that may contain "NotBefore" and "NotOnOrAfter" attributes.
  // These attributes restrict when the assertion is considered valid. It is possible that the Identity Provider's clock
  // may be slightly out of sync with ours, which can result in us receiving an assertion that is immediately invalid.
  // For example, if their clock is 1 second ahead of ours, we may receive the assertion before its "NotBefore"
  // attribute allows. The allowed clock skew accounts for this possible divergence.
  static final int ALLOWED_CLOCK_SKEW_MILLISECONDS = 1000;

  private static final String DEPLOYMENT_ERROR = "SAML deployment error";

  private final SamlMetadataTool samlMetadataTool;

  private final SamlConfigurationService samlConfigurationService;

  private final TaskScheduler taskScheduler;

  private final TenantReference<SamlDeployment> samlDeployment = new TenantReference<>();

  @Inject
  public SamlDeploymentManager(
      SamlMetadataTool samlMetadataTool,
      SamlConfigurationService samlConfigurationService,
      TaskScheduler taskScheduler)
  {
    this.samlMetadataTool = samlMetadataTool;
    this.samlConfigurationService = samlConfigurationService;
    this.taskScheduler = taskScheduler;
  }

  @Override
  public void register() {
    try {
      updateFromConfiguration();
    }
    catch (RuntimeException e) {
      // if we failed the tenant registration, fixing the configuration gets complicated...
      log.error("The SAML configuration is invalid and needs to be fixed by a system administrator", e);
    }
  }

  @Override
  public void deregister() {
    samlDeployment.remove();
  }

  /**
   * Gets the current SAML deployment for the current tenant or {@code null} if not configured.
   */
  public SamlDeployment get() {
    return samlDeployment.get();
  }

  // Visible for testing
  public void updateFromConfiguration() {
    SamlDeployment saml = parse(samlConfigurationService.get());

    if (saml != null) {
      samlDeployment.set(saml);
    }
    else {
      samlDeployment.remove();
    }

    log.info("SAML integration {}", samlDeployment.get() != null ? "enabled" : "disabled");
  }

  public void updateAllClusterNodesFromConfiguration() {
    updateFromConfiguration();
    taskScheduler.scheduleOneTimeTaskForAllOtherNodes(this);
  }

  public SamlDeployment parse(SamlConfiguration samlConfiguration) {
    if (samlConfiguration == null) {
      return null;
    }

    EntityDescriptorType entityDescriptor =
        samlMetadataTool.parseEntityDescriptor(samlConfiguration.getIdentityProviderMetadataXml());

    DefaultSamlDeployment defaultSamlDeployment = new DefaultSamlDeployment();
    defaultSamlDeployment.setEntityID(samlConfiguration.getEntityId());
    defaultSamlDeployment.setSslRequired(SslRequired.EXTERNAL);
    defaultSamlDeployment.setNameIDPolicyFormat("urn:oasis:names:tc:SAML:1.1:nameid-format:unspecified");
    defaultSamlDeployment.setForceAuthentication(false);
    defaultSamlDeployment.setIsPassive(false);
    defaultSamlDeployment.setTurnOffChangeSessionIdOnLogin(false);
    defaultSamlDeployment.setAutodetectBearerOnly(true);
    defaultSamlDeployment.setSignatureAlgorithm(SignatureAlgorithm.RSA_SHA256);
    defaultSamlDeployment.setSignatureCanonicalizationMethod("http://www.w3.org/2001/10/xml-exc-c14n#");
    defaultSamlDeployment.setSigningKeyPair(samlConfiguration.getSigningKeyPair());
    defaultSamlDeployment.setDecryptionKey(samlConfiguration.getDecryptionKey());

    IDPSSODescriptorType idpDescriptor =
        entityDescriptor.getChoiceType().stream().flatMap(choiceType -> choiceType.getDescriptors().stream())
            .map(EDTDescriptorChoiceType::getIdpDescriptor).filter(Objects::nonNull).findFirst().get();
    DefaultIDP idp = new DefaultIDP();
    defaultSamlDeployment.setIdp(idp);
    idp.setEntityID(entityDescriptor.getEntityID());
    List<KeyDescriptorType> keyDescriptorsForSigning = idpDescriptor.getKeyDescriptor().stream()
        .filter(keyDescriptor -> keyDescriptor.getUse() == null || KeyTypes.SIGNING.equals(keyDescriptor.getUse()))
        .collect(toList());
    for (KeyDescriptorType keyDescriptor : keyDescriptorsForSigning) {
      Certificate certificate;
      try {
        certificate = SAMLMetadataUtil.getCertificate(keyDescriptor);
      }
      catch (ConfigurationException | ProcessingException e) {
        throw new IllegalArgumentException(
            "SAML metadata for identity provider contains invalid certificate: " + e.getMessage(), e);
      }
      catch (NullPointerException e) {
        throw new IllegalArgumentException(
            "SAML metadata for identity provider contains invalid certificate: Missing base64 data", e);
      }
      if (certificate == null) {
        throw new IllegalArgumentException("SAML metadata for identity provider misses signing certificate");
      }
      idp.addSignatureValidationKey(certificate.getPublicKey());
    }
    boolean hasSigningKey = !keyDescriptorsForSigning.isEmpty();
    if (!hasSigningKey && (Boolean.TRUE.equals(samlConfiguration.getValidateResponseSignature())
        || Boolean.TRUE.equals(samlConfiguration.getValidateAssertionSignature()))) {
      throw new IllegalArgumentException(
          "SAML metadata for identity provider misses signing key to perform the requested signature validation");
    }
    boolean validateResponseSignature =
        Optional.ofNullable(samlConfiguration.getValidateResponseSignature()).orElse(hasSigningKey);
    boolean validateAssertionSignature =
        Optional.ofNullable(samlConfiguration.getValidateAssertionSignature()).orElse(hasSigningKey);
    idp.refreshKeyLocatorConfiguration();

    EndpointType singleSignOnEndpoint = getEndpoint(idpDescriptor.getSingleSignOnService());
    DefaultSingleSignOnService singleSignOnService = new DefaultSingleSignOnService();
    idp.setSingleSignOnService(singleSignOnService);
    idp.setAllowedClockSkew(ALLOWED_CLOCK_SKEW_MILLISECONDS);
    singleSignOnService.setSignRequest(Optional.ofNullable(idpDescriptor.isWantAuthnRequestsSigned()).orElse(true));
    singleSignOnService.setValidateResponseSignature(validateResponseSignature);
    singleSignOnService.setValidateAssertionSignature(validateAssertionSignature);
    singleSignOnService.setRequestBindingUrl(singleSignOnEndpoint.getLocation().toString());
    singleSignOnService.setRequestBinding(getBinding(singleSignOnEndpoint));

    EndpointType singleLogoutEndpoint = getEndpoint(idpDescriptor.getSingleLogoutService());
    if (singleLogoutEndpoint != null) {
      DefaultSingleLogoutService singleLogoutService = new DefaultSingleLogoutService();
      idp.setSingleLogoutService(singleLogoutService);
      singleLogoutService.setSignRequest(Optional.ofNullable(idpDescriptor.isWantAuthnRequestsSigned()).orElse(true));
      singleLogoutService.setSignResponse(singleLogoutService.signRequest());
      singleLogoutService.setValidateRequestSignature(!keyDescriptorsForSigning.isEmpty());
      singleLogoutService.setValidateResponseSignature(!keyDescriptorsForSigning.isEmpty());
      singleLogoutService.setRequestBindingUrl(singleLogoutEndpoint.getLocation().toString());
      singleLogoutService.setRequestBinding(getBinding(singleLogoutEndpoint));
      singleLogoutService.setResponseBindingUrl(singleLogoutEndpoint.getLocation().toString());
      singleLogoutService.setResponseBinding(getBinding(singleLogoutEndpoint));
    }

    defaultSamlDeployment.setRoleAttributeNames(Collections.emptySet());
    return defaultSamlDeployment;
  }

  private EndpointType getEndpoint(List<EndpointType> endpoints) {
    EndpointType postEndpoint = null;
    EndpointType redirectEndpoint = null;
    for (EndpointType endpoint : endpoints) {
      if (SamlMetadataTool.POST_BINDING.equals(endpoint.getBinding())) {
        postEndpoint = endpoint;
      }
      else if (SamlMetadataTool.REDIRECT_BINDING.equals(endpoint.getBinding())) {
        redirectEndpoint = endpoint;
      }
    }
    return postEndpoint != null ? postEndpoint : redirectEndpoint;
  }

  private Binding getBinding(EndpointType endpoint) {
    if (SamlMetadataTool.POST_BINDING.equals(endpoint.getBinding())) {
      return Binding.POST;
    }
    if (SamlMetadataTool.REDIRECT_BINDING.equals(endpoint.getBinding())) {
      return Binding.REDIRECT;
    }
    return null;
  }

  @Override
  public void execute(JobExecutionContext context) {
    execute(this::updateFromConfiguration, log, DEPLOYMENT_ERROR);
  }

  @Override
  public String getJobName() {
    return TASK_NAME;
  }
}
