/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.admin.authorization.provider;

import com.auth0.jwk.Jwk;
import com.auth0.jwk.JwkException;

public interface MultiTenantJwkProvider
{
  Jwk getJsonWebKey(String keyId) throws JwkException;

  String[] getIssuers();

  boolean denyRequest();
}
