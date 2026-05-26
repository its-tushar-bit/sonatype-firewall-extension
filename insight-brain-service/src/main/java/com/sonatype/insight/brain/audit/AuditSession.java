/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.audit;

import java.util.Objects;

/**
 * Marks the scope of a given audit event which is automatically committed to the audit log at the end. While the
 * session is active, the current thread can populate audit data using {@link AuditData#get()}. Audit sessions
 * can be nested in case of {@link AuditData#recordSubEvent(AuditEvent, boolean) sub events}.
 */
public class AuditSession
    implements AutoCloseable
{
  private static final ThreadLocal<AuditData> currentOfThread = ThreadLocal.withInitial(() -> NoopAuditData.INSTANCE);

  static AuditData getCurrent() {
    return currentOfThread.get();
  }

  private final AuditData current;

  private final AuditData previous;

  public AuditSession(AuditData auditData) {
    current = Objects.requireNonNull(auditData);
    previous = currentOfThread.get();
    currentOfThread.set(current);
  }

  @Override
  public void close() {
    try {
      current.commit();
    }
    finally {
      if (previous == NoopAuditData.INSTANCE) {
        currentOfThread.remove();
      }
      else {
        currentOfThread.set(previous);
      }
    }
  }
}
