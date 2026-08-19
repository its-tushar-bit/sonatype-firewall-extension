/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search;

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Arrays;
import java.util.List;

/**
 * The annotations for use by {@link SearchIndexRule} and tests using it.
 * <p>
 * Implementation note: Java annotations do not support inheritance. However, some of these annotations need to share
 * the same values. So you will see them defined in each annotation here. There is an alternative implementation with
 * nesting annotations but this is more complex for the developer to use in code so was not chosen.
 */
public class SearchIndexRuleAnnotations
{
  static final List<Class<? extends Annotation>> ANNOTATION_TYPES =
      Arrays.asList(LuceneTest.class, OpenSearchHttpTest.class);

  public static boolean isLuceneTest(final Annotation annotation) {
    return getLuceneTest(annotation) != null;
  }

  public static boolean isOpenSearchHttpTest(final Annotation annotation) {
    return getOpenSearchHttpTest(annotation) != null;
  }

  public static LuceneTest getLuceneTest(final Annotation annotation) {
    if (annotation instanceof LuceneTest) {
      return (LuceneTest) annotation;
    }
    return null;
  }

  public static OpenSearchHttpTest getOpenSearchHttpTest(final Annotation annotation) {
    if (annotation instanceof OpenSearchHttpTest) {
      return (OpenSearchHttpTest) annotation;
    }
    return null;
  }

  public static boolean hasAnyAnnotation(final Annotation annotation) {
    return isLuceneTest(annotation) || isOpenSearchHttpTest(annotation);
  }

  public static boolean getForceClean(final Annotation annotation) {
    if (isOpenSearchHttpTest(annotation)) {
      return getOpenSearchHttpTest(annotation).forceClean();
    }
    else if (isLuceneTest(annotation)) {
      return getLuceneTest(annotation).forceClean();
    }
    else {
      return false;
    }
  }

  @Retention(RetentionPolicy.RUNTIME)
  @Target({ElementType.METHOD, ElementType.TYPE})
  @Inherited
  public @interface LuceneTest
  {
    /**
     * Force a new clean fixture to be provisioned for this test.
     */
    boolean forceClean() default false;
  }

  @Retention(RetentionPolicy.RUNTIME)
  @Target({ElementType.METHOD, ElementType.TYPE})
  @Inherited
  public @interface OpenSearchHttpTest
  {
    /**
     * Force a new clean fixture to be provisioned for this test.
     */
    boolean forceClean() default false;
  }
}
