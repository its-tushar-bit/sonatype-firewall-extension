/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.db.rule;

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Arrays;
import java.util.Optional;

import org.apache.commons.lang3.ArrayUtils;
import org.junit.runner.Description;

/**
 * The annotations for use by {@link DatabaseRule} and tests using it.
 * <p>
 * Implementation note: Java annotations do not support inheritance. However, some of these annotations need to share
 * the same values. So you will see them defined in each annotation here. There is an alternative implementation with
 * nesting annotations but this is more complex for the developer to use in code so was not chosen.
 */
public class DatabaseRuleAnnotations
{
  @Retention(RetentionPolicy.RUNTIME)
  @Target({ElementType.METHOD, ElementType.TYPE})
  @Inherited
  public @interface H2InMemoryTest
  {
    /**
     * Should the migrations be suppressed on provisioning of a database
     */
    boolean suppressMigrations() default false;

    /**
     * Force a new clean database to be provisioned for this test.
     */
    boolean cleanDatabase() default false;

    /**
     * Custom settings string used in the H2 connection URL. Ex:
     * DATABASE_TO_UPPER=FALSE;LOCK_TIMEOUT=10000;MV_STORE=FALSE
     */
    String customSettings() default "";
  }

  @Retention(RetentionPolicy.RUNTIME)
  @Target({ElementType.METHOD, ElementType.TYPE})
  @Inherited
  public @interface H2DiskTest
  {
    public static final String DATABASE_PATH = "DATABASE_PATH ";

    /**
     * Should the migrations be suppressed on provisioning of a database
     */
    boolean suppressMigrations() default false;

    /**
     * Force a new clean database to be provisioned for this test.
     */
    boolean cleanDatabase() default false;

    /**
     * Custom settings string used in the H2 connection URL. Ex:
     * DATABASE_TO_UPPER=FALSE;LOCK_TIMEOUT=10000;MV_STORE=FALSE
     */
    String customSettings() default "";

    /**
     * Max number of connections for the DB connection. The number should be greater or equal to 1.
     */
    int maxConnections() default 50;

    /**
     * Specify a classpath to a test resource which is an existing H2 disk folder to copy. Ex:
     * DatabaseMigratorTest/PostIncrementalMigrator
     */
    String copyExistingDatabase() default "";
  }

  @Retention(RetentionPolicy.RUNTIME)
  @Target({ElementType.METHOD, ElementType.TYPE})
  @Inherited
  public @interface PostgresTest
  {
    /**
     * Should the migrations be suppressed on provisioning of a database
     */
    boolean suppressMigrations() default false;

    /**
     * Force a new clean database to be provisioned for this test.
     */
    boolean cleanDatabase() default false;

    /**
     * Max number of connections for the DB connection. The number should be greater or equal to 1.
     */
    int maxConnections() default 50;
  }

  public static boolean hasAnyAnnotation(final Annotation annotation) {
    return isH2InMemoryTest(annotation) || isH2DiskTest(annotation) || isPostgresTest(annotation);
  }

  public static boolean isH2InMemoryTest(final Annotation annotation) {
    return getH2InMemoryTest(annotation) != null;
  }

  public static boolean isH2DiskTest(final Annotation annotation) {
    return getH2DiskTest(annotation) != null;
  }

  public static boolean isPostgresTest(final Annotation annotation) {
    return getPostgresTest(annotation) != null;
  }

  public static H2InMemoryTest getH2InMemoryTest(final Annotation annotation) {
    if (annotation instanceof H2InMemoryTest) {
      return (H2InMemoryTest) annotation;
    }
    return null;
  }

  public static H2DiskTest getH2DiskTest(final Annotation annotation) {
    if (annotation instanceof H2DiskTest) {
      return (H2DiskTest) annotation;
    }
    return null;
  }

  public static PostgresTest getPostgresTest(final Annotation annotation) {
    if (annotation instanceof PostgresTest) {
      return (PostgresTest) annotation;
    }
    return null;
  }

  public static boolean getSuppressMigrations(final Annotation annotation) {
    if (isPostgresTest(annotation)) {
      return getPostgresTest(annotation).suppressMigrations();
    }
    else if (isH2DiskTest(annotation)) {
      return getH2DiskTest(annotation).suppressMigrations();
    }
    else if (isH2InMemoryTest(annotation)) {
      return getH2InMemoryTest(annotation).suppressMigrations();
    }
    else {
      return false;
    }
  }

  public static boolean getCleanDatabase(final Annotation annotation) {
    if (isPostgresTest(annotation)) {
      return getPostgresTest(annotation).cleanDatabase();
    }
    else if (isH2DiskTest(annotation)) {
      return getH2DiskTest(annotation).cleanDatabase();
    }
    else if (isH2InMemoryTest(annotation)) {
      return getH2InMemoryTest(annotation).cleanDatabase();
    }
    else {
      return false;
    }
  }

  public static boolean hasCustomSettings(final Annotation annotation) {
    if (isPostgresTest(annotation)) {
      // PostgresTest currently doesn't have `customSettings`
      return false;
    }
    else if (isH2DiskTest(annotation)) {
      return getH2DiskTest(annotation).customSettings() != null;
    }
    else if (isH2InMemoryTest(annotation)) {
      return getH2InMemoryTest(annotation).customSettings() != null;
    }
    else {
      return false;
    }
  }

  /**
   * Retrieves any defined db annotation for the test:
   * <ul>
   *   <li>annotations on the method have higher precedence than the class</li>
   *   <li>annotations on the subclass have higher precedence than the super class</li>
   * </ul>
   */
  public static Annotation getAnnotation(final Description description) {
    // method annotations have higher priority
    Annotation[] methodAnnotations =
        description.getAnnotations().toArray(new Annotation[description.getAnnotations().size()]);
    Annotation[] classAnnotations = description.getTestClass().getAnnotations();

    // reverse them as the subclass should have priority
    ArrayUtils.reverse(methodAnnotations);
    ArrayUtils.reverse(classAnnotations);

    Annotation[] annotations = ArrayUtils.addAll(methodAnnotations, classAnnotations);

    Optional<Annotation> annotation = Arrays.stream(annotations)
        .filter(a -> a instanceof H2InMemoryTest || a instanceof H2DiskTest || a instanceof PostgresTest)
        .findFirst();
    return annotation.orElse(null);
  }
}
