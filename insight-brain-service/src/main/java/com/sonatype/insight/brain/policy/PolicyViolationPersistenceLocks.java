/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.inject.Named;
import javax.inject.Singleton;

@Named
@Singleton
public class PolicyViolationPersistenceLocks
{
  private final ConcurrentMap<String, String> persistenceLocksByAppId = new ConcurrentHashMap<>();

  public Object getLock(String appId) {
    Object lock = persistenceLocksByAppId.get(appId);
    if (lock == null) {
      lock = persistenceLocksByAppId.putIfAbsent(appId, appId);
      if (lock == null) {
        lock = appId;
      }
    }
    return lock;
  }
}
