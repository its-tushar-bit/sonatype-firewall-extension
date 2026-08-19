/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.configuration.ldap;

import java.io.IOException;
import java.util.NoSuchElementException;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import javax.naming.ldap.Control;
import javax.naming.ldap.LdapContext;
import javax.naming.ldap.PagedResultsControl;
import javax.naming.ldap.PagedResultsResponseControl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Helper to transparently perform paged searches.
 */
class PagedNamingEnumeration
    implements NamingEnumeration<SearchResult>
{
  private static final Logger log = LoggerFactory.getLogger(PagedNamingEnumeration.class);

  private final LdapContext ctx;

  private final String name;

  private final String filter;

  private final SearchControls controls;

  private final Control[] originalRequestControls;

  private int pageSize;

  private NamingEnumeration<SearchResult> results;

  private byte[] cookie;

  public PagedNamingEnumeration(
      LdapContext ctx,
      String name,
      String filter,
      SearchControls controls,
      int pageSize) throws NamingException
  {
    this.ctx = ctx;
    this.name = name;
    this.filter = filter;
    this.controls = controls;
    this.pageSize = pageSize;

    originalRequestControls = ctx.getRequestControls();
    try {
      executeSearchForNextPage();
    }
    catch (RuntimeException | NamingException e) {
      try {
        close();
      }
      catch (RuntimeException | NamingException suppressed) {
        e.addSuppressed(suppressed);
      }
      throw e;
    }
  }

  private Control[] newRequestControls() throws NamingException {
    PagedResultsControl control;
    try {
      if (cookie == null) {
        control = new PagedResultsControl(pageSize, Control.NONCRITICAL);
      }
      else {
        control = new PagedResultsControl(pageSize, cookie, pageSize > 0 ? Control.CRITICAL : Control.NONCRITICAL);
      }
    }
    catch (IOException e) {
      throw (NamingException) new NamingException("Failed to create request controls for paged search").initCause(e);
    }
    return new Control[]{control};
  }

  private void executeSearchForNextPage() throws NamingException {
    ctx.setRequestControls(newRequestControls());
    results = ctx.search(name, filter, controls);
  }

  @Override
  public boolean hasMoreElements() {
    try {
      return hasMore();
    }
    catch (NamingException e) {
      log.debug("Failed to check for more LDAP search results", e);
      return false;
    }
  }

  @Override
  public SearchResult nextElement() {
    try {
      return next();
    }
    catch (NamingException e) {
      throw (NoSuchElementException) new NoSuchElementException().initCause(e);
    }
  }

  private byte[] getCookie() throws NamingException {
    Control[] responseControls = ctx.getResponseControls();
    if (responseControls != null) {
      for (Control responseControl : responseControls) {
        if (responseControl instanceof PagedResultsResponseControl) {
          return ((PagedResultsResponseControl) responseControl).getCookie();
        }
      }
    }
    return null;
  }

  @Override
  public boolean hasMore() throws NamingException {
    if (results.hasMore()) {
      return true;
    }
    cookie = getCookie();
    if (cookie == null) {
      return false;
    }
    executeSearchForNextPage();
    return results.hasMore();
  }

  @Override
  public SearchResult next() throws NamingException {
    if (!hasMore()) {
      throw new NoSuchElementException();
    }
    return results.next();
  }

  @Override
  public void close() throws NamingException {
    try {
      try {
        closeResults();
      }
      finally {
        closeCookie();
      }
    }
    finally {
      ctx.setRequestControls(originalRequestControls);
    }
  }

  private void closeResults() throws NamingException {
    if (results != null) {
      results.close();
      results = null;
    }
  }

  private void closeCookie() throws NamingException {
    if (cookie != null) {
      pageSize = 0;
      ctx.setRequestControls(newRequestControls());
      ctx.search(name, filter, controls).close();
      cookie = null;
    }
  }
}
