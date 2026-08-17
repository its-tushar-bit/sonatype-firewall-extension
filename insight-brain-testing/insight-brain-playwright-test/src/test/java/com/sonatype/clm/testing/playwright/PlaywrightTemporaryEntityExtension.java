/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright;

import java.lang.reflect.Field;

import com.sonatype.insight.brain.dataaccess.TemporaryEntity;

import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

/**
 * Drives the {@link TemporaryEntity} JUnit 4 {@code @Rule} lifecycle under the JUnit 5 (Jupiter)
 * engine: {@link BeforeEachCallback} snapshots the pristine database state before each test and
 * {@link AfterEachCallback} restores it afterwards.
 *
 * <p>
 * The Playwright functional modules boot the embedded IQ Server in a static initializer (not via
 * the ordered {@code @Rule} chain used by {@code AbstractBaseIntegrationTest}), so once
 * {@code TemporaryFolder} moves to {@code @TempDir} this is the only rule field that still needs
 * Jupiter driving. {@code TemporaryEntity.before()}/{@code after()} are public, so no reflection
 * is needed to invoke them — only to locate the field on the concrete test's class hierarchy.
 */
public final class PlaywrightTemporaryEntityExtension
    implements BeforeEachCallback, AfterEachCallback
{
  @Override
  public void beforeEach(final ExtensionContext context) {
    TemporaryEntity tempEntity = findTemporaryEntity(context.getRequiredTestInstance());
    if (tempEntity != null) {
      tempEntity.before();
    }
  }

  @Override
  public void afterEach(final ExtensionContext context) {
    TemporaryEntity tempEntity = findTemporaryEntity(context.getRequiredTestInstance());
    if (tempEntity != null) {
      tempEntity.after();
    }
  }

  private static TemporaryEntity findTemporaryEntity(final Object testInstance) {
    for (Class<?> current = testInstance.getClass(); current != null && current != Object.class; current =
        current.getSuperclass())
    {
      for (Field field : current.getDeclaredFields()) {
        if (TemporaryEntity.class.isAssignableFrom(field.getType())) {
          try {
            field.setAccessible(true);
            return (TemporaryEntity) field.get(testInstance);
          }
          catch (IllegalAccessException e) {
            throw new IllegalStateException("Could not read TemporaryEntity field " + field, e);
          }
        }
      }
    }
    return null;
  }
}
