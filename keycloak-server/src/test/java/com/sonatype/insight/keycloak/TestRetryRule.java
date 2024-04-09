/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.keycloak;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

public class TestRetryRule
    implements TestRule
{
  private final int attemptCount;

  public TestRetryRule(int attemptCount) {
    this.attemptCount = attemptCount;
  }

  @Override
  public Statement apply(Statement base, Description description) {
    return statement(base, description);
  }

  private Statement statement(Statement base, Description description) {
    return new Statement()
    {
      @Override
      public void evaluate() throws Throwable {
        Throwable lastError = null;

        for (int attempt = 0; attempt < attemptCount; attempt++) {
          try {
            base.evaluate();
            return;
          }
          catch (Exception | AssertionError e) {
            lastError = e;
            System.err.println(description.getDisplayName() + ": run " + (attempt + 1) + " failed: " + e.getMessage());
            e.printStackTrace();
          }
        }
        System.err.println(description.getDisplayName() + ": giving up after " + attemptCount + " failures.");
        throw lastError;
      }
    };
  }
}
