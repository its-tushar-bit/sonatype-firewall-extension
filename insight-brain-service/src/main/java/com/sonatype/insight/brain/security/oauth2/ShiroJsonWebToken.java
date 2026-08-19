/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security.oauth2;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.apache.commons.lang3.StringUtils;
import org.apache.shiro.authc.AuthenticationToken;

public class ShiroJsonWebToken
    implements AuthenticationToken
{
  private final SignedJWT signedJWT;

  private final JWTClaimsSet claimsSet;

  private final boolean isIdToken;

  public ShiroJsonWebToken(String rawToken) {
    this(rawToken, false);
  }

  public ShiroJsonWebToken(String rawToken, boolean isIdToken) {
    try {
      this.signedJWT = SignedJWT.parse(rawToken);
      this.claimsSet = signedJWT.getJWTClaimsSet();
      this.isIdToken = isIdToken;
    }
    catch (ParseException e) {
      throw new RuntimeException("Error parsing JWT token", e);
    }
  }

  public boolean isIdToken() {
    return isIdToken;
  }

  @Override
  public JWTClaimsSet getPrincipal() {
    return claimsSet;
  }

  @Override
  public SignedJWT getCredentials() {
    return signedJWT;
  }

  public String getValueFromClaimOrDefaultClaim(String claim, String defaultClaim) {
    String claimValue = getClaimValue(claim);

    if (StringUtils.isBlank(claimValue)) {
      claimValue = getClaimValue(defaultClaim);
    }

    return claimValue;
  }

  public String getClaimValue(String claim) {
    try {
      return claimsSet.getStringClaim(claim);
    }
    catch (ParseException e) {
      throw new RuntimeException(String.format("Error parsing claim: %s", claim), e);
    }
  }

  public List<String> getValueAsListFromClaimOrDefaultClaim(String claim, String defaultClaim) {
    List<String> claimValue = getClaimValueAsList(claim);

    if (claimValue.isEmpty()) {
      claimValue = getClaimValueAsList(defaultClaim);
    }

    return claimValue;
  }

  public List<String> getClaimValueAsList(String claim) {
    try {
      return Optional.ofNullable(claimsSet.getStringListClaim(claim)).orElseGet(ArrayList::new);
    }
    catch (ParseException e) {
      throw new RuntimeException(String.format("Error parsing claim: %s", claim), e);
    }
  }
}
