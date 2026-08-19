/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security.oauth2;

import java.net.MalformedURLException;
import java.net.URL;
import java.text.ParseException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.dataaccess.configuration.oauth2.OAuth2ConfigurationDAO;
import com.sonatype.insight.brain.model.configuration.oauth2.OAuth2Configuration;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.jwk.source.JWKSourceBuilder;
import com.nimbusds.jose.proc.JWSKeySelector;
import com.nimbusds.jose.proc.JWSVerificationKeySelector;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jwt.JWTClaimNames;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.proc.ConfigurableJWTProcessor;
import com.nimbusds.jwt.proc.DefaultJWTClaimsVerifier;
import com.nimbusds.jwt.proc.DefaultJWTProcessor;
import com.nimbusds.jwt.proc.JWTClaimsSetVerifier;
import org.apache.commons.lang3.StringUtils;
import org.apache.shiro.authz.AuthorizationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
@Singleton
public class ShiroJsonWebTokenValidator
{
  public static final String NO_OAUTH_2_CONFIGURATION_AVAILABLE =
      "There is no OAuth2 configuration to validate the JWT token";

  private static final Logger log = LoggerFactory.getLogger(ShiroJsonWebTokenValidator.class);

  public static final long TTL = 60 * 60 * 24 * 1000; // 24 hours

  public static final long REFRESH_TIMEOUT = 60 * 1000; // 1 minute

  private final Map<String, JWSKeySelector> keySelectorMap;

  private final OAuth2ConfigurationDAO oAuth2ConfigurationDAO;

  @Inject
  public ShiroJsonWebTokenValidator(final OAuth2ConfigurationDAO oAuth2ConfigurationDAO) {
    this.oAuth2ConfigurationDAO = oAuth2ConfigurationDAO;
    keySelectorMap = new HashMap<>();
  }

  public boolean isTokenValid(final ShiroJsonWebToken jwtToken) {
    log.debug("Verifying JWT token with next payload: {}", jwtToken.getPrincipal().getClaims().keySet());

    OAuth2Configuration configuration = oAuth2ConfigurationDAO.getById(jwtToken.getPrincipal().getIssuer());

    if (configuration == null) {
      throw new AuthorizationException(NO_OAUTH_2_CONFIGURATION_AVAILABLE);
    }

    try {
      ConfigurableJWTProcessor<SecurityContext> jwtProcessor = new DefaultJWTProcessor<>();

      jwtProcessor.setJWSKeySelector(getKeySelector(configuration));

      // Set the required JWT claims for access tokens
      jwtProcessor.setJWTClaimsSetVerifier(createClaimsSetVerifier(configuration));

      // Process the token
      jwtProcessor.process(jwtToken.getCredentials(), null);
    }
    catch (Exception e) {
      log.error("Error verifying JWT token for subject: {}. Error: {}", jwtToken.getPrincipal().getSubject(),
          e.getMessage());
      return false;
    }

    return true;
  }

  public JWTClaimsSetVerifier createClaimsSetVerifier(final OAuth2Configuration configuration) {
    JWTClaimsSet.Builder builder = new JWTClaimsSet.Builder().issuer(configuration.getId());

    configuration.getExactMatchClaims().forEach(builder::claim);

    return new DefaultJWTClaimsVerifier<>(
        builder.build(),
        new HashSet<>(Arrays.asList(
            JWTClaimNames.SUBJECT,
            JWTClaimNames.ISSUED_AT,
            JWTClaimNames.EXPIRATION_TIME)));
  }

  private JWSKeySelector getKeySelector(final OAuth2Configuration configuration) {
    String issuer = configuration.getId();

    if (keySelectorMap.containsKey(issuer)) {
      return keySelectorMap.get(issuer);
    }

    JWSKeySelector keySelector = createKeySelector(configuration);

    log.trace("Adding JWS key selector for {}", issuer);
    keySelectorMap.put(issuer, keySelector);

    return keySelector;
  }

  private JWSKeySelector createKeySelector(final OAuth2Configuration configuration) {
    JWKSource<SecurityContext> keySource = getKeysSource(configuration);

    JWSKeySelector<SecurityContext> keySelector = new JWSVerificationKeySelector<>(
        JWSAlgorithm.parse(configuration.getIdpJwsAlgorithm()),
        keySource);

    return keySelector;
  }

  private JWKSource<SecurityContext> getKeysSource(final OAuth2Configuration configuration) {
    if (StringUtils.isBlank(configuration.getIdpJwksUrl())) {
      return createFromJwks(configuration.getIdpJwks());
    }
    return createFromJwksUrl(configuration.getIdpJwksUrl());
  }

  private JWKSource<SecurityContext> createFromJwks(final String idpJwks) {
    try {
      JWKSet jwkSet = JWKSet.parse(idpJwks);
      return new ImmutableJWKSet<>(jwkSet);
    }
    catch (ParseException e) {
      log.error("Error parsing JWK Set from configuration. Keys: {}", idpJwks);
      throw new RuntimeException("Error parsing JWK Set from configuration", e);
    }
  }

  private JWKSource createFromJwksUrl(String jwksUrl) {
    try {
      return JWKSourceBuilder
          .create(new URL(jwksUrl))
          .cache(TTL, REFRESH_TIMEOUT)
          .retrying(true)
          .build();
    }
    catch (MalformedURLException e) {
      log.error("Error loading keys from {}", jwksUrl);
      throw new RuntimeException(String.format("Error loading keys from %s", jwksUrl), e);
    }
  }
}
