/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.concurrent;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Semaphore;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.util.concurrent.AtomicLongMap;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The purpose of the SemaphorePool is to try to maintain a finite availableSemaphorePool of semaphores. The
 * availableSemaphorePool will grow to the needed size, based on access, but will shrink back to the desired
 * availableSemaphorePool size as semaphores are released.
 */
public class SemaphorePool
{
  private static final Logger log = LoggerFactory.getLogger(SemaphorePool.class);

  // this is an 'ideal' size for the availableSemaphorePool; the availableSemaphorePool is unbounded but can shrink
  // back to the ideal availableSemaphorePool size over time as semaphores are released
  private final int idealPoolSize;

  // the internal lock is used to synchronize access to the internal availableSemaphorePool list and semaphoresInUse map
  private final Semaphore internalLock;

  // holds semaphores that have been created but are no longer in use and are available for acquisition
  private List<Semaphore> availableSemaphorePool = new LinkedList<>();

  // represents semaphores that are in use, mapped to the keys used to acquire them; other callers wanting to
  // acquire access to a semaphore already in use for that key will join the existing one
  private Map<String, Semaphore> semaphoresInUse = new HashMap<>();

  // keep track of acquire (increment) and release (decrement) calls for the given key. Relying on the semaphores
  // themselves is unreliable because we don't call acquire on them inside the internal lock
  private AtomicLongMap<String> keyUsageCounter = AtomicLongMap.create();

  public SemaphorePool(int idealPoolSize) {
    this.idealPoolSize = idealPoolSize;
    this.internalLock = new Semaphore(1, true);
  }

  /**
   * runs acquire on the semaphore associated with the given key
   */
  public void acquire(final String key) throws InterruptedException {
    if (StringUtils.isBlank(key)) {
      log.error("Trying to acquire with invalid key '{}'", key);
      throw new IllegalArgumentException("key is required");
    }
    internalLock.acquire();

    Semaphore semaphoreForCallingThread;

    try {
      // can we join one that's already in use?
      semaphoreForCallingThread = semaphoresInUse.get(key);

      if (null == semaphoreForCallingThread) {
        // can we get one out of the pool?
        if (!availableSemaphorePool.isEmpty()) {
          semaphoreForCallingThread = availableSemaphorePool.remove(0);
        }
        else {
          // growth is theoretically unbounded, but as semaphores are released they will be removed from the pool
          semaphoreForCallingThread = new Semaphore(1, true);
        }
        semaphoresInUse.put(key, semaphoreForCallingThread);
      }
      keyUsageCounter.getAndIncrement(key);
    }
    finally {
      internalLock.release();
    }

    // note: it is critical to release the internal lock first; otherwise this call will block for other threads
    // until the calling thread releases their keyed semaphore
    semaphoreForCallingThread.acquire();
  }

  /**
   * releases the semaphore associated with the given key
   */
  public void release(final String key) throws InterruptedException {
    internalLock.acquire();

    try {
      if (StringUtils.isBlank(key) || !semaphoresInUse.containsKey(key)) {
        log.warn("Trying to release with invalid key '{}'", key);
      }
      else {
        Semaphore semaphore = semaphoresInUse.get(key);
        if (keyUsageCounter.get(key) == 1) {
          // there are no other threads currently using this semaphore, only this one
          semaphoresInUse.remove(key);
          // this is how we move back towards our ideal pool size if we've grown past it
          if (getInUseCount() + getAvailableCount() < idealPoolSize) {
            availableSemaphorePool.add(semaphore);
          }
        }
        semaphore.release();
        if (keyUsageCounter.get(key) > 0) {
          keyUsageCounter.decrementAndGet(key);
        }
      }
    }
    finally {
      internalLock.release();
    }
  }

  @VisibleForTesting
  int getInUseCount() {
    return semaphoresInUse.size();
  }

  @VisibleForTesting
  int getAvailableCount() {
    return availableSemaphorePool.size();
  }

  @VisibleForTesting
  long getKeyUsage(String key) {
    return keyUsageCounter.get(key);
  }
}
