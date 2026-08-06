/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.security.saml2.provider.service.authentication.Saml2AuthenticatedPrincipal;

/**
 * {@link SamlPrincipalAttributes} backed by a Spring Security SAML2 authenticated principal.
 *
 * <p>
 * Spring's {@link Saml2AuthenticatedPrincipal} exposes a single attribute map keyed by attribute name. SAML
 * {@code FriendlyName}s are folded into that same map (aliased to the formal {@code Name} values) by the
 * response converter configured in {@code SpringSamlAuthenticatingFilter}, so a formal-name lookup already
 * covers friendly names; the friendly-specific accessors therefore return empty results to avoid duplicate
 * values.
 */
public class SpringSamlPrincipal
    implements SamlPrincipalAttributes
{
  private final Saml2AuthenticatedPrincipal delegate;

  public SpringSamlPrincipal(Saml2AuthenticatedPrincipal delegate) {
    this.delegate = Objects.requireNonNull(delegate);
  }

  @Override
  public String getName() {
    return delegate.getName();
  }

  @Override
  public String getAttribute(String name) {
    List<String> values = getAttributes(name);
    return values.isEmpty() ? null : values.get(0);
  }

  @Override
  public List<String> getAttributes(String name) {
    List<Object> values = delegate.getAttribute(name);
    if (values == null) {
      return Collections.emptyList();
    }
    return values.stream().filter(Objects::nonNull).map(Object::toString).collect(Collectors.toList());
  }

  @Override
  public String getFriendlyAttribute(String name) {
    return null;
  }

  @Override
  public List<String> getFriendlyAttributes(String name) {
    return Collections.emptyList();
  }

  @Override
  public Set<String> getFriendlyNames() {
    return Collections.emptySet();
  }

  @Override
  public Map<String, List<String>> getAllAttributes() {
    return delegate.getAttributes()
        .entrySet()
        .stream()
        .collect(Collectors.toMap(
            Map.Entry::getKey,
            entry -> entry.getValue()
                .stream()
                .filter(Objects::nonNull)
                .map(Object::toString)
                .collect(Collectors.toList())));
  }
}
