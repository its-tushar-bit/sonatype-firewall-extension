/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security.oauth2;

import java.util.regex.Pattern;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import com.sonatype.insight.brain.dataaccess.security.OidcTokenDAO;
import com.sonatype.insight.brain.model.security.OidcToken;

import org.apache.commons.lang3.StringUtils;
import org.apache.shiro.authc.AuthenticationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This service has the needed abstractions to register and remove OIDC tokens for user authentication.
 * <p>
 * <ul>
 *   <li>Check {@link com.sonatype.insight.brain.security.oauth2.OidcLoginFilter} for the OIDC token registration.</li>
 *   <li>Check {@link com.sonatype.insight.brain.security.oauth2.JwtAuthenticationFilter} for the OIDC token removal.</li>
 * <ul/>
 */
@Named
@Singleton
public class OidcTokenService
{
  private static final Logger log = LoggerFactory.getLogger(OidcTokenService.class);

  private static final Pattern JWT_TOKEN_PATTERN =
      Pattern.compile("^[A-Za-z0-9-_]+\\.[A-Za-z0-9-_]+\\.[A-Za-z0-9-_]+$");

  private final OidcTokenDAO oidcTokenDAO;

  @Inject
  public OidcTokenService(final OidcTokenDAO oidcTokenDAO) {
    this.oidcTokenDAO = oidcTokenDAO;
  }

  public String registerOidcToken(final String token) {
    if (StringUtils.isBlank(token)) {
      log.debug("OIDC token is empty");
      throw new AuthenticationException("OIDC token is empty");
    }

    log.debug("Registering OIDC token");
    OidcToken oidcToken = new OidcToken(token);
    oidcTokenDAO.insert(oidcToken);
    return oidcToken.getId();
  }

  public String getOidcToken(final String tokenId) {
    if (StringUtils.isBlank(tokenId)) {
      return "";
    }

    if (isOidcToken(tokenId)) {
      return tokenId;
    }

    OidcToken oidcToken = oidcTokenDAO.getById(tokenId);

    if (oidcToken == null || StringUtils.isBlank(oidcToken.getToken())) {
      log.debug("OIDC token not found with id: {}", tokenId);
      return "";
    }
    return oidcToken.getToken();
  }

  public String pullOidcToken(final String tokenId) {
    if (isOidcToken(tokenId)) {
      return tokenId;
    }

    log.debug("Pulling OIDC token with id: {}", tokenId);

    String oidcToken = getOidcToken(tokenId);
    if (StringUtils.isNotBlank(oidcToken)) {
      oidcTokenDAO.deleteById(tokenId);
      log.debug("OIDC token with id: {}, was deleted", tokenId);
    }
    return oidcToken;
  }

  private boolean isOidcToken(String tokenId) {
    return StringUtils.isNotBlank(tokenId) && JWT_TOKEN_PATTERN.matcher(tokenId).matches();
  }
}
