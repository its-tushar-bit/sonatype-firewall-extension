/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.ldap.test;

import com.sonatype.insight.brain.ldap.EmbeddedLdapServer;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

/**
 * Embedded LDAP server meant to facilitate unit testing of LDAP integration. The LDAP server instance must be started
 * explicitly, but it is stopped automatically by junit.
 * 
 * @since 1.7
 */
public class TestLdapServer
    extends EmbeddedLdapServer
    implements TestRule
{
  @Override
  public Statement apply(final Statement base, Description description) {
    return new Statement()
    {
      @Override
      public void evaluate() throws Throwable {
        try {
          base.evaluate();
        }
        finally {
          stop();
        }
      }
    };
  }
}
