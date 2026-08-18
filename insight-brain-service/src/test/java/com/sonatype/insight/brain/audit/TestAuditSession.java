/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.audit;

import java.lang.reflect.Field;

import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.rules.ExternalResource;

/**
 * JUnit rule that automatically clears the thread local value of {@link AuditData#get()} after a test to avoid
 * interferences with other tests. Implements the JUnit 5 callbacks so it can be used via {@code @RegisterExtension}
 * as well as the legacy {@code @Rule}.
 */
public class TestAuditSession
    extends ExternalResource
    implements BeforeEachCallback, AfterEachCallback
{
  @Override
  public void beforeEach(final ExtensionContext context) throws Exception {
    try {
      before();
    }
    catch (Throwable t) {
      throw new IllegalStateException(t);
    }
  }

  @Override
  public void afterEach(final ExtensionContext context) {
    after();
  }

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
    // Guard against a null threadLocal: under JUnit 5 afterEach() always runs even if beforeEach()/before() threw
    // (unlike the JUnit 4 ExternalResource contract), so a failed setup must not mask itself with an NPE here.
    if (threadLocal != null) {
      threadLocal.remove();
    }
  }

  public void set(AuditData auditData) {
    threadLocal.set(auditData);
  }
}
