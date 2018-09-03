/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.audit;

import java.util.concurrent.Callable;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * The data for one audit record. Code populates audit data for the current operation/event using
 * {@link AuditData#get()}.
 */
public abstract class AuditData
{
  private static final ThreadLocal<AuditData> instance = ThreadLocal.withInitial(() -> NoopAuditData.INSTANCE);

  public static AuditData get() {
    return instance.get();
  }

  static AuditData set(AuditData auditData) {
    AuditData previous = instance.get();
    if (auditData == null || auditData == NoopAuditData.INSTANCE) {
      instance.remove();
    }
    else {
      instance.set(auditData);
    }
    return previous;
  }

  public final AuditSession recordSubEvent(AuditEvent event, boolean independent) {
    return new AuditSession(forSubEvent(event, independent));
  }

  protected abstract AuditData forSubEvent(AuditEvent event, boolean independent);

  public final <F> F continueAsync(Runnable task, Function<Runnable, F> taskSubmitter) {
    return continueAsync(auditData -> {
      Runnable auditedTask = () -> {
        try (AuditSession auditSession = new AuditSession(auditData)) {
          try {
            task.run();
          }
          catch (RuntimeException | Error e) {
            auditData.setException(e);
            throw e;
          }
        }
      };
      return taskSubmitter.apply(auditedTask);
    });
  }

  public final <F, V> F continueAsync(Callable<V> task, Function<Callable<V>, F> taskSubmitter) {
    return continueAsync(auditData -> {
      Callable<V> auditedTask = () -> {
        try (AuditSession auditSession = new AuditSession(auditData)) {
          try {
            return task.call();
          }
          catch (Exception | Error e) {
            auditData.setException(e);
            throw e;
          }
        }
      };
      return taskSubmitter.apply(auditedTask);
    });
  }

  public final <F, V> F continueAsync(Supplier<V> task, Function<Supplier<V>, F> taskSubmitter) {
    return continueAsync(auditData -> {
      Supplier<V> auditedTask = () -> {
        try (AuditSession auditSession = new AuditSession(auditData)) {
          try {
            return task.get();
          }
          catch (RuntimeException | Error e) {
            auditData.setException(e);
            throw e;
          }
        }
      };
      return taskSubmitter.apply(auditedTask);
    });
  }

  protected abstract <F> F continueAsync(Function<AuditData, F> taskSubmitter);

  public abstract void commit();

  public abstract void setUsername(String username);

  public abstract void setEvent(AuditEvent event);

  public abstract void setError(String error);

  public abstract void setException(Throwable error);

  public abstract void setHttpStatus(int httpStatus);

  public abstract void addData(String key, Object value);
}
