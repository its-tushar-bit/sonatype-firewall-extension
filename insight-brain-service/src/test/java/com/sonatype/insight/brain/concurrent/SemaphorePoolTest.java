/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.concurrent;

import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import com.sonatype.insight.brain.git.VerifiableLoggingTestBase;

import com.google.common.collect.ImmutableList;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.lang.System.currentTimeMillis;
import static java.lang.Thread.sleep;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.ThrowableAssert.catchThrowable;

public class SemaphorePoolTest
    extends VerifiableLoggingTestBase
{
  private final Logger log = LoggerFactory.getLogger(SemaphorePoolTest.class);

  public SemaphorePoolTest() {
    super(SemaphorePool.class);
  }

  @Test
  public void testIdealPoolSize() throws Exception {
    // given : a semaphore pool with an ideal size and a bunch of keys
    final int idealSize = 5;
    SemaphorePool semaphorePool = new SemaphorePool(idealSize);
    ImmutableList<String> keys = ImmutableList.of("1", "2", "3", "4", "5", "6", "7");

    // then: assert initial state
    assertThat(semaphorePool.getInUseCount()).isEqualTo(0);
    assertThat(semaphorePool.getAvailableCount()).isEqualTo(0);

    // when: grow the pool beyond ideal size
    for (String key : keys) {
      semaphorePool.acquire(key);
    }

    // then: should have enough semaphores for all keys with no extras in the pool
    assertThat(semaphorePool.getInUseCount()).isEqualTo(keys.size());
    assertThat(semaphorePool.getAvailableCount()).isEqualTo(0);

    // when: release one item from the pool
    semaphorePool.release("1");

    // then: released item is not recycled into the available pool
    assertThat(semaphorePool.getInUseCount()).isEqualTo(6);
    assertThat(semaphorePool.getAvailableCount()).isEqualTo(0);

    // when: release more items from the pool
    semaphorePool.release("2");
    semaphorePool.release("3");

    // then: pool shrunk to the ideal size
    assertThat(semaphorePool.getInUseCount()).isEqualTo(4);
    assertThat(semaphorePool.getAvailableCount()).isEqualTo(1);

    // when: acquire more items
    semaphorePool.acquire("8");

    // then: recycled semaphore is used from the available pool
    assertThat(semaphorePool.getInUseCount()).isEqualTo(5);
    assertThat(semaphorePool.getAvailableCount()).isEqualTo(0);
  }

  private boolean threadFinished = false;

  @Test
  public void testBlocksOnSameKey() throws Exception {
    // given: an empty semaphore pool
    final String key = "k1";
    SemaphorePool semaphorePool = new SemaphorePool(2);
    CountDownLatch countdownLatch = new CountDownLatch(1);
    threadFinished = false;

    // and given : a thread that locks on a key for some period of time
    new Thread(() -> {
      try {
        semaphorePool.acquire(key);
        countdownLatch.countDown();
        // simulate some work
        sleep(600);
        threadFinished = true;
        semaphorePool.release(key);
      }
      catch (InterruptedException e) {
        e.printStackTrace();
      }
    }).start();

    // and given: wait for the thread to acquire the lock
    countdownLatch.await(10, TimeUnit.SECONDS);

    // when: try to acquire the same lock
    long start = currentTimeMillis();
    semaphorePool.acquire(key);
    long duration = currentTimeMillis() - start;

    // then: worker thread finished and duration indicated we had to wait for it
    assertThat(threadFinished).isTrue();
    assertThat(duration).isGreaterThan(500);
  }

  @Test
  public void testAcquire_blankKey() {
    // given: an empty semaphore pool
    SemaphorePool semaphorePool = new SemaphorePool(20);

    // when: acquire lock with null key provided
    Throwable thrownNull = catchThrowable(() -> semaphorePool.acquire(null));
    Throwable thrownBlank = catchThrowable(() -> semaphorePool.acquire(""));

    // then: not expecting any pool usage
    assertThat(thrownNull).isInstanceOf(IllegalArgumentException.class);
    assertThat(thrownBlank).isInstanceOf(IllegalArgumentException.class);
    assertThat(semaphorePool.getInUseCount()).isEqualTo(0);
    assertThat(semaphorePool.getAvailableCount()).isEqualTo(0);
    assertThatLogMessagesEqual(
        error("Trying to acquire with invalid key 'null'"),
        error("Trying to acquire with invalid key ''"));
  }

  @Test
  public void testRelease_blankOrInvalidKey() throws InterruptedException {
    // given: an empty semaphore pool
    SemaphorePool semaphorePool = new SemaphorePool(20);

    // when: trying to release on a blank key or bogus key
    semaphorePool.release(null);
    semaphorePool.release("");
    semaphorePool.release("bogus");

    // then: validate log messages
    assertThatLogMessagesEqual(
        warn("Trying to release with invalid key 'null'"),
        warn("Trying to release with invalid key ''"),
        warn("Trying to release with invalid key 'bogus'"));
  }

  @Test
  public void testSimultaneousAccessFiniteSetOfKeys() throws InterruptedException {
    // given: an empty semaphore pool
    final int threadCount = 50;
    final int keyCount = 3;
    final long workTimeInMs = 100;
    Random random = new Random();
    SemaphorePool semaphorePool = new SemaphorePool(2);
    CountDownLatch countdownLatch = new CountDownLatch(threadCount);

    // when : a bunch of threads that lock and release simultaneously on a small set of random keys
    for (int i = 0; i < threadCount; i++) {
      new Thread(() -> {
        final String key = "key-" + random.nextInt(keyCount);
        final long threadId = Thread.currentThread().getId();
        try {
          log.info("acquiring key {} for thread {}", key, threadId);
          semaphorePool.acquire(key);
          log.info("have key {} for thread {}", key, threadId);

          // then: make sure key usage indicates key is in use
          assertThat(semaphorePool.getKeyUsage(key)).isPositive();

          // simulate some work
          sleep(workTimeInMs);
          log.info("releasing key {} for thread {}", key, threadId);
          semaphorePool.release(key);
          countdownLatch.countDown();
        }
        catch (InterruptedException e) {
          e.printStackTrace();
        }
      }).start();
      // let the threads have some time so they can start while we're still iterating here
      sleep(10);
    }

    // then: all threads completed and key usage is exactly 0 for all keys
    assertThat(countdownLatch.await(threadCount * workTimeInMs + 5000, TimeUnit.MILLISECONDS)).isTrue();
    for (int i = 0; i < keyCount; i++) {
      assertThat(semaphorePool.getKeyUsage("key-" + i)).isZero();
    }
  }
}
