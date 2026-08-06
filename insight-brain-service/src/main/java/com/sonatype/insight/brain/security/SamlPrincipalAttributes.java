/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Engine-neutral view of the attributes carried by an authenticated SAML principal.
 *
 * <p>
 * {@link SamlRealm} maps a SAML assertion onto an internal user using configurable attribute names,
 * independent of the underlying SAML library. {@link SpringSamlPrincipal} adapts the Spring Security
 * SAML2
 * ({@link org.springframework.security.saml2.provider.service.authentication.Saml2AuthenticatedPrincipal})
 * principal to this view.
 */
public interface SamlPrincipalAttributes
{
  /**
   * @return the SAML NameID, or {@code null} when absent.
   */
  String getName();

  /**
   * @return the first value of the attribute with the given formal {@code Name}, or {@code null}.
   */
  String getAttribute(String name);

  /**
   * @return all values of the attribute with the given formal {@code Name}; never {@code null}.
   */
  List<String> getAttributes(String name);

  /**
   * @return the first value of the attribute with the given {@code FriendlyName}, or {@code null}.
   */
  String getFriendlyAttribute(String name);

  /**
   * @return all values of the attribute with the given {@code FriendlyName}; never {@code null}.
   */
  List<String> getFriendlyAttributes(String name);

  /**
   * @return the set of {@code FriendlyName}s present on the assertion; never {@code null}.
   */
  Set<String> getFriendlyNames();

  /**
   * @return all attributes keyed by formal {@code Name}, for diagnostic logging; never {@code null}.
   */
  Map<String, List<String>> getAllAttributes();
}
