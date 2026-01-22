/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security.oauth2;

import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import jakarta.inject.Named;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

@Named
public class JWTGenerator
{
  public static final String FIRST_NAME_CLAIM = "given_name";

  public static final String LAST_NAME_CLAIM = "family_name";

  public static final String USERNAME_CLAIM = "preferred_username";

  public static final String EMAIL_CLAIM = "email";

  public static final String GROUPS_CLAIM = "groups";

  // RSA signatures require a public and private RSA key pair, the public key
  // must be made known to the JWS recipient in order to verify the signatures
  private static final RSAKey RSA_KEY;

  private static final RSAKey RSA_PUBLIC_JWK;

  static {
    try {
      RSA_KEY = new RSAKeyGenerator(2048)
          .keyID("123")
          .generate();
      RSA_PUBLIC_JWK = RSA_KEY.toPublicJWK();
    }
    catch (JOSEException e) {
      throw new RuntimeException("Error generating RSA Keys", e);
    }
  }

  public String getJWKSetString() {
    JWKSet jwkSet = new JWKSet(RSA_PUBLIC_JWK);
    return jwkSet.toString();
  }

  public String getJwsAlgorithm() {
    return JWSAlgorithm.RS256.getName();
  }

  public String generateJWT(String subject, String issuer) {
    Instant fiveMinutesInTheFuture = Instant.now().plusSeconds(300);

    JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
        .subject(subject)
        .issuer(issuer)
        .issueTime(Date.from(Instant.now()))
        .expirationTime(Date.from(fiveMinutesInTheFuture))
        .build();
    return generateJWT(claimsSet);
  }

  public String generateJWT(String subject, String issuer, Instant expirationDate) {
    JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
        .subject(subject)
        .issuer(issuer)
        .issueTime(Date.from(expirationDate.minusSeconds(300)))
        .expirationTime(Date.from(expirationDate))
        .build();
    return generateJWT(claimsSet);
  }

  public String generateJWT(String subject, String issuer, final Map<String, Object> claims) {
    Instant fiveMinutesInTheFuture = Instant.now().plusSeconds(300);

    JWTClaimsSet.Builder builder = new JWTClaimsSet.Builder();
    for (Entry<String, Object> entry : claims.entrySet()) {
      builder.claim(entry.getKey(), entry.getValue());
    }

    JWTClaimsSet claimsSet = builder
        .subject(subject)
        .issuer(issuer)
        .issueTime(Date.from(Instant.now()))
        .expirationTime(Date.from(fiveMinutesInTheFuture))
        .build();

    return generateJWT(claimsSet);
  }

  public String generateJWT(String subject, String issuer, final Map<String, Object> claims, Instant expirationDate) {
    JWTClaimsSet.Builder builder = new JWTClaimsSet.Builder();
    for (Entry<String, Object> entry : claims.entrySet()) {
      builder.claim(entry.getKey(), entry.getValue());
    }

    JWTClaimsSet claimsSet = builder
        .subject(subject)
        .issuer(issuer)
        .issueTime(Date.from(expirationDate.minusSeconds(300)))
        .expirationTime(Date.from(expirationDate))
        .build();

    return generateJWT(claimsSet);
  }

  public Map<String, Object> getCustomClaims(
      final String username,
      final String firstName,
      final String lastName,
      final String email,
      final List<String> groups)
  {
    Map<String, Object> claims = new HashMap<>();
    claims.put(FIRST_NAME_CLAIM, firstName);
    claims.put(LAST_NAME_CLAIM, lastName);
    claims.put(USERNAME_CLAIM, username);
    claims.put(EMAIL_CLAIM, email);
    claims.put(GROUPS_CLAIM, groups);

    return claims;
  }

  public Map<String, Object> getStandardCustomClaims(
      final String username,
      final String firstName,
      final String lastName,
      final String email,
      final List<String> groups)
  {
    Map<String, Object> claims = new HashMap<>();
    claims.put(OAuth2Realm.GIVEN_NAME_CLAIM, firstName);
    claims.put(OAuth2Realm.FAMILY_NAME_CLAIM, lastName);
    claims.put(OAuth2Realm.NICKNAME_CLAIM, username);
    claims.put(OAuth2Realm.EMAIL_CLAIM, email);
    claims.put(OAuth2Realm.GROUPS_CLAIM, groups);

    return claims;
  }

  private String generateJWT(JWTClaimsSet claimsSet) {
    try {

      // Create RSA-signer with the private key
      JWSSigner signer = new RSASSASigner(RSA_KEY);

      // Create JWT with claims
      SignedJWT signedJWT = new SignedJWT(
          new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(RSA_KEY.getKeyID()).build(),
          claimsSet);

      // Compute the RSA signature
      signedJWT.sign(signer);

      // Getting JWT string
      return signedJWT.serialize();
    }
    catch (JOSEException e) {
      throw new RuntimeException("Error creating JWT token", e);
    }
  }
}
