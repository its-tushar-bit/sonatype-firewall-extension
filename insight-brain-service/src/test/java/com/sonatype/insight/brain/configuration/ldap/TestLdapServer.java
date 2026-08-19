/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.configuration.ldap;

import java.io.IOException;

import org.junit.rules.TemporaryFolder;
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
  public TestLdapServer setWorkingDirectory(TemporaryFolder tempDir) {
    try {
      setWorkingDirectory(tempDir.newFolder());
    }
    catch (IOException e) {
      throw new IllegalStateException(e);
    }
    return this;
  }

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
