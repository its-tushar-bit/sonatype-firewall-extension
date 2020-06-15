/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.concurrent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Semaphore;

import com.google.common.annotations.VisibleForTesting;
import org.apache.commons.lang3.StringUtils;

/**
 * The purpose of the SemaphorePool is to try to maintain a finite availableSemaphorePool of semaphores.  The
 * availableSemaphorePool will grow to the needed size, based on access, but will shrink back to the desired
 * availableSemaphorePool size as semaphores are released.
 */
public class SemaphorePool
{
  // this is an 'ideal' size for the availableSemaphorePool;  the availableSemaphorePool is unbounded but can shrink
  // back to the ideal availableSemaphorePool size over time as semaphores are released
  private final int idealPoolSize;

  // the internal lock is used to synchronize access to the internal availableSemaphorePool list and semaphoresInUse map
  private final Semaphore internalLock;

  // holds semaphores that have been created but are no longer in use and are available for acquisition
  private List<Semaphore> availableSemaphorePool = new ArrayList<>();

  // represents semaphores that are in use, mapped to the keys used to acquire them; other callers wanting to
  // acquire access to a semaphore already in use for that key will join the existing one
  private Map<String, Semaphore> semaphoresInUse = new HashMap<>();

  public SemaphorePool(int idealPoolSize) {
    this.idealPoolSize = idealPoolSize;
    this.internalLock = new Semaphore(1, true);
  }

  /**
   * runs acquire on the semaphore associated with the given key
   */
  public void acquire(String key) throws InterruptedException {
    if (StringUtils.isBlank(key)) {
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
  public void release(String key) throws InterruptedException {
    if (StringUtils.isBlank(key) || !semaphoresInUse.containsKey(key)) {
      // trying to release on a blank or bogus key could be safely ignored from this pool's perspective, but it is
      // indicative of a problem in using this API so we'll throw an exception
      throw new IllegalArgumentException("trying to release on an invalid key");
    }
    internalLock.acquire();

    try {
      Semaphore semaphore = semaphoresInUse.get(key);
      semaphore.release();
      if (!semaphore.hasQueuedThreads()) {
        // there are no other threads currently using this semaphore
        semaphoresInUse.remove(key);
        // this is how we move back towards our ideal pool size if we've grown past it
        if (getInUseCount() + getAvailableCount() < idealPoolSize) {
          availableSemaphorePool.add(semaphore);
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
}
