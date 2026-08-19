/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.core.UriBuilder;

import com.sonatype.insight.brain.model.configuration.saml.SamlConfiguration;
import com.sonatype.insight.brain.service.BaseUrl;
import com.sonatype.insight.brain.tenancy.TenantReference;
import com.sonatype.insight.brain.tenancy.TenantManaged;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.security.saml2.core.Saml2X509Credential;
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistration;
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistrations;
import org.springframework.security.saml2.provider.service.registration.Saml2MessageBinding;
import org.springframework.security.saml2.provider.service.web.RelyingPartyRegistrationResolver;

/**
 * Builds a Spring Security {@link RelyingPartyRegistration} from the tenant's stored
 * {@link SamlConfiguration}.
 *
 * <p>
 * The configuration is read from {@link SamlConfigurationCache} (populated outside the request), so the
 * resolver works in the early servlet-filter context and is MTIQ-safe. Returns {@code null} when SAML is
 * not configured, signalling the filter that SAML login is unavailable.
 *
 * <p>
 * The Assertion Consumer Service (ACS) location and default SP entityId are derived from the trusted
 * {@link BaseUrl} (which honors a forced base URL and {@code x-forwarded-proto}), falling back to the inbound
 * request only when no base URL has been captured, so a spoofed {@code Host} header cannot influence them.
 */
@Named
@Singleton
public class SamlRelyingPartyRegistrationResolver
    implements RelyingPartyRegistrationResolver, TenantManaged
{
  private static final Logger log = LoggerFactory.getLogger(SamlRelyingPartyRegistrationResolver.class);

  static final String REGISTRATION_ID = "saml";

  private final SamlConfigurationCache samlConfigurationCache;

  private final BaseUrl baseUrl;

  // Caches the parsed IdP metadata (the expensive part of build()) per tenant, keyed by the metadata XML
  // content so every caller reusing the same configuration hits the cache; evicted per tenant in deregister().
  private final TenantReference<CachedRegistration> registrationCache = new TenantReference<>();

  @Inject
  public SamlRelyingPartyRegistrationResolver(SamlConfigurationCache samlConfigurationCache, BaseUrl baseUrl) {
    this.samlConfigurationCache = samlConfigurationCache;
    this.baseUrl = baseUrl;
  }

  @Override
  public RelyingPartyRegistration resolve(HttpServletRequest request, String relyingPartyRegistrationId) {
    SamlConfiguration samlConfiguration = samlConfigurationCache.get();
    if (samlConfiguration == null) {
      return null;
    }
    return build(samlConfiguration, assertionConsumerServiceLocation(request));
  }

  public RelyingPartyRegistration build(SamlConfiguration samlConfiguration, String assertionConsumerServiceLocation) {
    // The SP entityId defaults to the ACS location when not explicitly configured (Spring requires a
    // non-empty entityId; the ACS URL is a stable, valid default and matches the SP's own address).
    String entityId = StringUtils.isNotBlank(samlConfiguration.getEntityId())
        ? samlConfiguration.getEntityId()
        : assertionConsumerServiceLocation;
    // Reuse the cached parsed IdP metadata, then apply THIS configuration's own SP credentials plus the
    // request-specific entityId/ACS. Two configurations can share the same IdP metadata yet carry different SP
    // signing/decryption credentials, so credentials are applied per call rather than cached with the template.
    RelyingPartyRegistration.Builder builder = templateFor(samlConfiguration.getIdentityProviderMetadataXml())
        .mutate()
        .entityId(entityId)
        .assertionConsumerServiceLocation(assertionConsumerServiceLocation);
    applyCredentials(builder, samlConfiguration);
    return builder.build();
  }

  /**
   * Returns the parsed IdP-metadata {@link RelyingPartyRegistration} template, parsing the metadata XML once and
   * caching it per tenant keyed by the metadata XML content (not object identity, since
   * {@code SamlConfigurationService.get()} returns a fresh instance per call). SP credentials are NOT part of
   * the cached template — they are applied per call in {@link #build} — so configurations sharing the same IdP
   * metadata safely reuse the parse. Evicted per tenant in {@link #deregister()}.
   */
  private RelyingPartyRegistration templateFor(String metadataXml) {
    if (StringUtils.isBlank(metadataXml)) {
      // A corrupted or partially-written configuration; surface an actionable message rather than an NPE.
      throw new IllegalArgumentException("Identity provider metadata XML is required");
    }
    CachedRegistration cached = registrationCache.get();
    if (cached != null && cached.metadataXml().equals(metadataXml)) {
      return cached.template();
    }
    RelyingPartyRegistration template = parseMetadata(metadataXml);
    registrationCache.set(new CachedRegistration(metadataXml, template));
    return template;
  }

  @Override
  public void deregister() {
    // Evict this tenant's cached registration on tenant teardown.
    registrationCache.remove();
  }

  private RelyingPartyRegistration parseMetadata(String metadataXml) {
    // entityId and ACS are placeholders; build() overrides both per request. Spring requires them set.
    // RelyingPartyRegistrations.fromMetadata() rejects IdP metadata with no verification certificate
    // ("missing verification certificates"), so responses/assertions are always signature-verifiable. See
    // SamlRelyingPartyRegistrationResolverTest#testBuild_RejectsMetadataWithoutVerificationCertificate.
    String placeholder = "https://localhost" + SamlConstants.SAML_REQUEST_PATH;
    return RelyingPartyRegistrations
        .fromMetadata(new ByteArrayInputStream(metadataXml.getBytes(StandardCharsets.UTF_8)))
        .registrationId(REGISTRATION_ID)
        .entityId(placeholder)
        .assertionConsumerServiceLocation(placeholder)
        .assertionConsumerServiceBinding(Saml2MessageBinding.POST)
        .build();
  }

  private void applyCredentials(RelyingPartyRegistration.Builder builder, SamlConfiguration samlConfiguration) {
    Certificate rawCertificate = samlConfiguration.getCertificate();
    if (rawCertificate != null && !(rawCertificate instanceof X509Certificate)) {
      throw new IllegalArgumentException(
          "SAML configuration certificate must be X.509 but was " + rawCertificate.getType());
    }
    X509Certificate certificate = (X509Certificate) rawCertificate;
    boolean hasCertificate = certificate != null;
    boolean hasSigningKeyPair = samlConfiguration.getSigningKeyPair() != null;
    if (hasCertificate != hasSigningKeyPair) {
      log.warn("SAML signing credential is incompletely configured (certificate present: {}, signing key present: "
          + "{}); AuthnRequests will not be signed until both are set.", hasCertificate, hasSigningKeyPair);
    }
    if (hasCertificate && hasSigningKeyPair) {
      Saml2X509Credential signing =
          Saml2X509Credential.signing(samlConfiguration.getSigningKeyPair().getPrivate(), certificate);
      builder.signingX509Credentials(credentials -> {
        credentials.clear();
        credentials.add(signing);
      });
      // Sign outbound AuthnRequests whenever an SP signing credential is configured, matching the pre-migration
      // Keycloak adapter (which defaulted WantAuthnRequestsSigned to true). This also makes the generated SP
      // metadata advertise AuthnRequestsSigned="true" for parity.
      builder.assertingPartyMetadata(party -> party.wantAuthnRequestsSigned(true));
    }
    if (samlConfiguration.getDecryptionKey() != null && !hasCertificate) {
      log.warn("SAML decryption key is configured but the certificate is missing; encrypted assertions cannot be "
          + "decrypted until the certificate is set.");
    }
    if (hasCertificate && samlConfiguration.getDecryptionKey() != null) {
      Saml2X509Credential decryption =
          Saml2X509Credential.decryption(samlConfiguration.getDecryptionKey(), certificate);
      builder.decryptionX509Credentials(credentials -> {
        credentials.clear();
        credentials.add(decryption);
      });
    }
  }

  /**
   * @return {@code scheme://host[:port][/context]/saml}. Prefers the trusted {@link BaseUrl} (which applies a
   *         forced base URL and {@code x-forwarded-proto}); falls back to the inbound request only when no base
   *         URL has been captured for the current request.
   */
  private String assertionConsumerServiceLocation(HttpServletRequest request) {
    try {
      return UriBuilder.fromUri(baseUrl.get()).path(SamlConstants.SAML_REQUEST_PATH).build().toString();
    }
    catch (IllegalStateException e) {
      // No base URL captured for this request (e.g. base URL not configured and no request in scope).
      return requestDerivedLocation(request);
    }
  }

  private static String requestDerivedLocation(HttpServletRequest request) {
    StringBuilder location = new StringBuilder();
    location.append(request.getScheme()).append("://").append(request.getServerName());
    int port = request.getServerPort();
    boolean defaultPort = ("http".equals(request.getScheme()) && port == 80)
        || ("https".equals(request.getScheme()) && port == 443);
    if (port > 0 && !defaultPort) {
      location.append(':').append(port);
    }
    location.append(request.getContextPath()).append(SamlConstants.SAML_REQUEST_PATH);
    return location.toString();
  }

  private record CachedRegistration(String metadataXml, RelyingPartyRegistration template)
  {
  }
}
