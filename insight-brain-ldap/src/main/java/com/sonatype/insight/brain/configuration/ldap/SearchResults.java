/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.configuration.ldap;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.SearchResult;

import org.apache.shiro.realm.ldap.LdapUtils;

/**
 * Helper to enable try-with-resources with LDAP searches.
 */
class SearchResults
    implements NamingEnumeration<SearchResult>, AutoCloseable
{
  private final NamingEnumeration<SearchResult> delegate;

  public SearchResults(NamingEnumeration<SearchResult> delegate) {
    this.delegate = delegate;
  }

  @Override
  public boolean hasMoreElements() {
    return delegate.hasMoreElements();
  }

  @Override
  public SearchResult nextElement() {
    return delegate.nextElement();
  }

  @Override
  public boolean hasMore() throws NamingException {
    return delegate.hasMore();
  }

  @Override
  public SearchResult next() throws NamingException {
    return delegate.next();
  }

  @Override
  public void close() {
    LdapUtils.closeEnumeration(delegate);
  }
}
