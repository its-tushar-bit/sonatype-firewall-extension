/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.admin;

import com.sonatype.insight.brain.api.v2.dto.OAuth2ConfigurationDTO;
import com.sonatype.insight.brain.api.v2.dto.OidcConfigurationDTO;
import com.sonatype.insight.brain.api.v2.dto.SsoConfigurationDTO;
import com.sonatype.insight.brain.model.configuration.oauth2.OAuth2Configuration;
import com.sonatype.insight.brain.model.configuration.oauth2.OidcConfiguration;

public class SsoConfigurationTestHelper
{
  public static final String ISSUER = "http://idp/";

  public static final String ALGORITHM = "RS256";

  public static final String JWKS_URL = "http://idp/jwks.json";

  public static final String JWKS = "set";

  public static final String CLIENT_ID = "client-id";

  public static final String CLIENT_SECRET = "client-secret";

  public static final String AUTHORIZATION_URL = "http://idp/authorization";

  public static final String TOKEN_URL = "http://idp/token";

  public static SsoConfigurationDTO createSsoConfigurationDTO() {
    OAuth2ConfigurationDTO oAuth2ConfigurationDTO = new OAuth2ConfigurationDTO(ISSUER, ALGORITHM, JWKS_URL, JWKS);
    OidcConfigurationDTO oidcConfigurationDTO =
        new OidcConfigurationDTO(ISSUER, CLIENT_ID, CLIENT_SECRET, AUTHORIZATION_URL, TOKEN_URL);
    return new SsoConfigurationDTO(oAuth2ConfigurationDTO, oidcConfigurationDTO);
  }

  public static OAuth2Configuration createOAuth2Configuration() {
    return new OAuth2Configuration(ISSUER, ALGORITHM, JWKS_URL, JWKS);
  }

  public static OidcConfiguration createOidcConfiguration() {
    return new OidcConfiguration(ISSUER, CLIENT_ID, CLIENT_SECRET, AUTHORIZATION_URL, TOKEN_URL);
  }
}
