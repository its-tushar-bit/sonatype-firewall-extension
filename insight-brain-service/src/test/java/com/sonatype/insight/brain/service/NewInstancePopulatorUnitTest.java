/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

import java.util.concurrent.Callable;
import java.util.function.Consumer;

import javax.inject.Inject;

import com.sonatype.insight.brain.common.test.SlowTest;

import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.stubbing.Answer;

import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.spy;

@Category(SlowTest.class)
public class NewInstancePopulatorUnitTest
    extends AbstractComponentTest
{
  @Inject
  private NewInstancePopulator newInstancePopulator;

  @Test
  public void testPopulateIfNewInstance_DisallowConcurrentExecution() throws Exception {
    NewInstancePopulator spyNewInstancePopulator = spy(newInstancePopulator);
    Callable<Void> callable = () -> {
      spyNewInstancePopulator.populateIfNewInstance();
      return null;
    };
    Consumer<Answer<Void>> answerConsumer = answer -> {
      try {
        doAnswer(answer).when(spyNewInstancePopulator).doPopulateIfNewInstance();
      }
      catch (Exception e) {
        throw new RuntimeException(e.getMessage(), e);
      }
    };
    testCallable_DisallowConcurrentExecution(callable, answerConsumer, false);
  }
}
