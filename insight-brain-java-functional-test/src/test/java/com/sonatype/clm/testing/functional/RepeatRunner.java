/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional;

import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.Statement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RepeatRunner
    extends BlockJUnit4ClassRunner
{
  private static final Logger log = LoggerFactory.getLogger(RepeatRunner.class);

  public RepeatRunner(Class<?> cls) throws Exception {
    super(cls);
  }

  @Override
  protected Statement methodBlock(FrameworkMethod method) {
    Repeat repeat = method.getAnnotation(Repeat.class);
    if (repeat != null) {
      return new RepeatStatement(super.methodBlock(method), repeat.value());
    }
    return super.methodBlock(method);
  }

  private static class RepeatStatement
      extends Statement
  {
    private final Statement statement;

    private final int repeat;

    RepeatStatement(Statement statement, int repeat) {
      this.statement = statement;
      this.repeat = repeat;
    }

    @Override
    public void evaluate() throws Throwable {
      for (int i = 0; i < repeat; i++) {
        log.debug("Iteration " + i);
        statement.evaluate();
      }
    }
  }
}
