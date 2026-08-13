/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.migration;

import java.util.concurrent.Callable;
import java.util.function.Consumer;

import jakarta.inject.Inject;

import com.sonatype.insight.brain.variant.AbstractComponentH2Test;
import com.sonatype.insight.brain.variant.ComponentH2Test;

import org.junit.jupiter.api.Test;
import org.mockito.stubbing.Answer;

import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.spy;

@ComponentH2Test
public class DataMigratorTest
    extends AbstractComponentH2Test
{
  @Inject
  private DataMigrator dataMigrator;

  @Test
  public void testMigrate_DisallowConcurrentExecution() throws Exception {
    DataMigrator spyDataMigrator = spy(dataMigrator);
    Callable<Void> callable = () -> {
      spyDataMigrator.migrate();
      return null;
    };
    Consumer<Answer<Void>> answerConsumer = answer -> {
      try {
        doAnswer(answer).when(spyDataMigrator).runMigrators();
      }
      catch (Exception e) {
        throw new RuntimeException(e.getMessage(), e);
      }
    };
    testCallable_DisallowConcurrentExecution(callable, answerConsumer);
  }
}
