/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.testing.functional;

import java.lang.reflect.Field;

import com.yammer.dropwizard.Service;
import com.yammer.dropwizard.config.Configuration;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

/**
 * Workaround for https://github.com/dropwizard/dropwizard/issues/427
 */
public class DropwizardServiceRule<C extends Configuration>
    extends com.yammer.dropwizard.testing.junit.DropwizardServiceRule<C>
{
  private final Field jettyServer;

  public DropwizardServiceRule(Class<? extends Service<C>> serviceClass, String configPath) {
    super(serviceClass, configPath);

    try {
      jettyServer = getClass().getSuperclass().getDeclaredField("jettyServer");
      jettyServer.setAccessible(true);
    }
    catch (NoSuchFieldException | SecurityException e) {
      throw new IllegalStateException(e);
    }
  }

  @Override
  public Statement apply(Statement base, Description description) {
    final Statement dw = super.apply(base, description);
    return new Statement()
    {
      @Override
      public void evaluate() throws Throwable {
        try {
          dw.evaluate();
        }
        finally {
          jettyServer.set(DropwizardServiceRule.this, null);
        }
      }
    };
  }
}
