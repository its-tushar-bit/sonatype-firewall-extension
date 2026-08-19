/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search;

import static com.sonatype.insight.brain.search.SearchIndexRuleAnnotations.isOpenSearchHttpTest;

import com.sonatype.insight.brain.common.test.InsightFixtureRule;
import com.sonatype.insight.brain.search.SearchIndexRuleAnnotations.LuceneTest;
import com.sonatype.insight.brain.search.SearchIndexRuleAnnotations.OpenSearchHttpTest;
import com.sonatype.insight.brain.search.lucene.LuceneSearchIndexFixture;
import com.sonatype.insight.test.SpringTestExecutionContext;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Objects;

/**
 * <p>
 * Encapsulate the search index test fixtures needed for IQ as a JUnit rule. The intent of the rule is to mange the
 * {@link SearchIndexFixture} which is the running search index. This rule is a singleton designed to be used with the
 * {@link SearchIndexRule#getInstance(Class)} method to encapsulate that logic. Namely that the fixture itself can keep
 * running between tests, but it still allows for 'before' and 'after' logic to reset it as needed.
 * </p>
 *
 * <p>
 * The test fixtures encapsulated are:
 * <ul>
 * <li>the {@link SearchIndexFixture} itself</li>
 * <li>the {@link SearchIndexType} (i.e. Lucene (default), OpenSearch)</li>
 * </ul>
 * <p>
 * For each test it will manage if a new fixture needs to be provisioned.
 * </p>
 *
 * <p>
 * Example:
 *
 * <pre>
 * {@code
 * @Rule(order = 2)
 * public SearchIndexRule searchIndexRule = SearchIndexRule.getInstance();
 * }
 * </pre>
 * </p>
 *
 * <p>
 * Notes:
 * <ul>
 * <li>The 'order' <strong>MUST</strong> be a value greater than 1 which is reserved for the database.</li>
 * <li>The default search index is Lucene.</li>
 * <li>Use the {@link LuceneTest} annotation for a Lucene search index. Passed to {@link LuceneSearchIndexFixture}.</li>
 * <li>Use the {@link OpenSearchHttpTest} annotation for an OpenSearch http-based search index. Passed to
 * OpenSearch HTTP search index (removed — OpenSearch tests require testcontainers).</li>
 * <li>Each annotation has some common options as well as some custom ones. Common options:</li>
 * <ul>
 * <li>Use the `forceClean` value on the annotations to force a new clean fixture to be provisioned</li>
 * </ul>
 * </ul>
 * </p>
 */
public class SearchIndexRule
    extends InsightFixtureRule<SearchIndexType, SearchIndexFixture>
{
  private static final SearchIndexRule INSTANCE = new SearchIndexRule();

  private static Class<?> currentTestClassType;

  private boolean springContextFixturePrepared = false;

  private SearchIndexType springContextFixtureType;

  private Class<?> springContextTestClass;

  private String springContextTestName;

  private boolean springContextAnnotationFromMethod;

  protected SearchIndexRule() {
    // private constructor for singleton enforcement
  }

  /**
   * Return the singleton {@link SearchIndexRule}
   *
   * @param baseTestClassType Any class that uses this rule to manage the fixture, should pass in its class type here.
   *          The value is tracked between subsequent tests and when the value changes it is considered
   *          as making the currently active fixture NOT reusable and therefore a fresh fixture will be
   *          automatically be re-provisioned
   */
  public static SearchIndexRule getInstance(Class<?> baseTestClassType) {
    if (currentTestClassType != baseTestClassType) {
      INSTANCE.markFixtureAsDirty();
      currentTestClassType = baseTestClassType;
    }
    return INSTANCE;
  }

  public synchronized void ensureInitializedForSpringContext() {
    applySpringExecutionContext();

    try {
      before();
      previousType = type;
      isNewFixtureForCurrentTest = false;
      springContextFixturePrepared = true;
      springContextFixtureType = type;
      springContextTestClass = currentTestClass;
      springContextTestName = testName;
      springContextAnnotationFromMethod =
          hasMethodLevelFixtureAnnotation(SpringTestExecutionContext.getCurrentTestMethod());
    }
    catch (Throwable t) {
      throw new IllegalStateException("Failed to initialize SearchIndexRule for Spring test context", t);
    }
  }

  private void applySpringExecutionContext() {
    Class<?> springTestClass = SpringTestExecutionContext.getCurrentTestClass();
    Method springTestMethod = SpringTestExecutionContext.getCurrentTestMethod();

    if (springTestClass == null && springTestMethod == null) {
      return;
    }

    annotation = resolveFixtureAnnotation(springTestMethod, springTestClass);
    testName = springTestMethod != null ? springTestMethod.getName() : springTestClass.getSimpleName();
    currentTestClass = springTestClass;
  }

  private Annotation resolveFixtureAnnotation(final Method springTestMethod, final Class<?> springTestClass) {
    Annotation methodAnnotation = resolveMethodFixtureAnnotation(springTestMethod);
    if (methodAnnotation != null) {
      return methodAnnotation;
    }

    if (springTestClass != null) {
      for (Annotation candidate : springTestClass.getAnnotations()) {
        if (SearchIndexRuleAnnotations.ANNOTATION_TYPES.contains(candidate.annotationType())) {
          return candidate;
        }
      }
    }

    return null;
  }

  private Annotation resolveMethodFixtureAnnotation(final Method springTestMethod) {
    if (springTestMethod == null) {
      return null;
    }
    for (Annotation candidate : springTestMethod.getAnnotations()) {
      if (SearchIndexRuleAnnotations.ANNOTATION_TYPES.contains(candidate.annotationType())) {
        return candidate;
      }
    }
    return null;
  }

  private boolean hasMethodLevelFixtureAnnotation(final Method springTestMethod) {
    return resolveMethodFixtureAnnotation(springTestMethod) != null;
  }

  @Override
  protected void before() throws Throwable {
    if (shouldReuseSpringContextFixture()) {
      return;
    }

    springContextFixturePrepared = false;
    super.before();
  }

  private boolean shouldReuseSpringContextFixture() {
    if (!springContextFixturePrepared || isCurrentFixtureDirty) {
      return false;
    }

    SearchIndexType currentFixtureType = getType();
    boolean sameClass = isSameTestClass(springContextTestClass, currentTestClass);
    boolean sameScope = springContextAnnotationFromMethod
        ? sameClass && Objects.equals(springContextTestName, testName)
        : sameClass;

    if (springContextFixtureType == currentFixtureType && sameScope) {
      type = currentFixtureType;
      return true;
    }

    springContextFixturePrepared = false;
    return false;
  }

  private boolean isSameTestClass(final Class<?> left, final Class<?> right) {
    if (left == right) {
      return true;
    }
    if (left == null || right == null) {
      return false;
    }
    return left.getName().equals(right.getName());
  }

  @Override
  protected List<Class<? extends Annotation>> getAnnotationTypes() {
    return SearchIndexRuleAnnotations.ANNOTATION_TYPES;
  }

  @Override
  protected boolean getForceClean(final Annotation annotation) {
    return SearchIndexRuleAnnotations.getForceClean(annotation);
  }

  @Override
  protected boolean hasAnnotation() {
    return SearchIndexRuleAnnotations.hasAnyAnnotation(annotation);
  }

  @Override
  protected SearchIndexFixture createNewFixture() {
    if (type.equals(SearchIndexType.OPENSEARCH_HTTP)) {
      throw new UnsupportedOperationException("OpenSearch tests have been removed");
    }
    return new InProcessLuceneSearchIndexFixture();
  }

  @Override
  protected SearchIndexType getType() {
    if (isOpenSearchHttpTest(annotation)) {
      return SearchIndexType.OPENSEARCH_HTTP;
    }
    else {
      return SearchIndexType.LUCENE;
    }
  }

  public SearchConfig getSearchConfig() {
    return fixture != null ? fixture.getSearchConfig() : null;
  }

  private static final class InProcessLuceneSearchIndexFixture
      implements SearchIndexFixture
  {
    @Override
    public com.sonatype.insight.brain.search.SearchConfig getSearchConfig() {
      // With legacy code a null SearchConfig means Lucene (which has no config)
      return null;
    }

    @Override
    public boolean isFixtureReusable() {
      return true;
    }

    @Override
    public void close() throws Exception {
      // no-op
    }
  }
}
