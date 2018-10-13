/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.audit;

import java.lang.reflect.Field;

import org.junit.rules.ExternalResource;

/**
 * JUnit rule that automatically clears the thread local value of {@link AuditData#get()} after a test to avoid
 * interferences with other tests.
 */
public class TestAuditSession
    extends ExternalResource
{
  private ThreadLocal<AuditData> threadLocal;

  @Override
  @SuppressWarnings("unchecked")
  protected void before() throws Throwable {
    Field field = AuditSession.class.getDeclaredField("currentOfThread");
    field.setAccessible(true);
    threadLocal = (ThreadLocal<AuditData>) field.get(null);
  }

  @Override
  protected void after() {
    threadLocal.remove();
  }

  public void set(AuditData auditData) {
    threadLocal.set(auditData);
  }
}
