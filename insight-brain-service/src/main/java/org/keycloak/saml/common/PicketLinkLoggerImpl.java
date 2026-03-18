/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package org.keycloak.saml.common;

/**
 * Custom logger impl for Keycloak that prefers to rethrow exceptions instead of merely logging them and then letting
 * the code continue into a cryptic/misleading follow-up exception.
 *
 * NOTE: Fully-qualified class name must comply with lookup logic in PicketLinkLoggerFactory.
 */
public class PicketLinkLoggerImpl
    extends DefaultPicketLinkLogger
{
  @Override
  public void samlBase64DecodingError(Throwable t) {
    throw new IllegalArgumentException("Invalid SAML message: " + t.getMessage(), t);
  }
}
