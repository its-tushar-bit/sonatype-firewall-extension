/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.shutdown;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ShutdownRequestTest
{
  @Test
  public void testExecute() throws Exception {
    Future<?> future = new TestShutdownRequest1().execute(null);

    assertThat(future.isDone()).isTrue();
    assertThat(future.get()).isNull();
  }

  @Test
  public void testIsValid() {
    assertThat(new TestShutdownRequest1().isValid()).isTrue();
  }

  @Test
  public void testCompareTo() {
    ShutdownRequest<Integer> s1 = new TestShutdownRequest1();
    ShutdownRequest<Integer> s2 = new TestShutdownRequest2();

    assertThat(s1.compareTo(s2)).isEqualTo(-1);
    assertThat(s1.compareTo(s1)).isEqualTo(0);
    assertThat(s2.compareTo(s1)).isEqualTo(1);
  }

  private static final class TestShutdownRequest1
      implements ShutdownRequest<Integer>
  {
    @Override
    public Integer getItem() {
      return null;
    }

    @Override
    public int getOrder() {
      return 1;
    }

    @Override
    public String getOrigin() {
      return null;
    }

    @Override
    public Future<?> execute(final ExecutorService executorService) {
      return CompletableFuture.completedFuture(null);
    }
  }

  private static final class TestShutdownRequest2
      implements ShutdownRequest<Integer>
  {
    @Override
    public Integer getItem() {
      return null;
    }

    @Override
    public int getOrder() {
      return 2;
    }

    @Override
    public String getOrigin() {
      return null;
    }

    @Override
    public Future<?> execute(final ExecutorService executorService) {
      return CompletableFuture.completedFuture(null);
    }
  }
}
