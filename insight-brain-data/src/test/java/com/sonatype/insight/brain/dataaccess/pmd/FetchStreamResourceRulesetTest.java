/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.pmd;

import java.nio.file.Paths;
import java.util.List;

import net.sourceforge.pmd.PMDConfiguration;
import net.sourceforge.pmd.PmdAnalysis;
import net.sourceforge.pmd.lang.LanguageRegistry;
import net.sourceforge.pmd.lang.LanguageVersion;
import net.sourceforge.pmd.lang.document.FileId;
import net.sourceforge.pmd.reporting.Report;
import net.sourceforge.pmd.reporting.RuleViolation;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies the custom PMD rule wired into this module's build actually fires on the
 * {@code fetchStream()}-without-try-with-resources leak pattern, and does not false-positive on the
 * safe pattern. Without this test the ruleset is an unverified guardrail: a broken XPath would
 * silently stop catching the very regression it exists to prevent.
 *
 * <p>
 * The rule is exercised through the same PMD engine version used by {@code maven-pmd-plugin}
 * (see the {@code pmd-core}/{@code pmd-java} test dependencies), so this test reflects exactly what
 * the build does at the {@code verify} phase.
 */
public class FetchStreamResourceRulesetTest
{
  private static final String RULESET =
      Paths.get("src", "main", "pmd", "fetchstream-resource-ruleset.xml").toAbsolutePath().toString();

  private static final String RULE_NAME = "FetchStreamMustBeInTryWithResources";

  // fetchStream() outside try-with-resources -> borrowed connection leaks if map/collect throws.
  private static final String UNSAFE_SOURCE = String.join("\n",
      "public class UnsafeSample {",
      "  interface Query { java.util.stream.Stream<Object> fetchStream(); }",
      "  public java.util.List<String> leak(Query query) {",
      "    return query.fetchStream()",
      "        .map(Object::toString)",
      "        .collect(java.util.stream.Collectors.toList());",
      "  }",
      "}");

  // fetchStream() inside try-with-resources -> stream (and its connection) closed on every path.
  private static final String SAFE_SOURCE = String.join("\n",
      "public class SafeSample {",
      "  interface Query { java.util.stream.Stream<Object> fetchStream(); }",
      "  public java.util.List<String> noLeak(Query query) {",
      "    try (var stream = query.fetchStream().map(Object::toString)) {",
      "      return stream.collect(java.util.stream.Collectors.toList());",
      "    }",
      "  }",
      "}");

  // fetchStream() inside a lambda passed to a stream wrapper (e.g. AbstractSqlDAO.getStreamWithSqlInClause),
  // whose returned stream is closed by try-with-resources at the call site. This is the pre-existing safe
  // pattern in InnerSourceVersionDAO, DevelopmentPrioritizationComponentInfoDAO and PolicyWaiverDAO. The
  // fetchStream() node still has the enclosing ResourceList as an ancestor (the lambda does not stop the
  // XPath ancestor axis), so the rule must NOT fire here.
  private static final String SAFE_LAMBDA_SOURCE = String.join("\n",
      "public class SafeLambdaSample {",
      "  interface Query { java.util.stream.Stream<Object> fetchStream(); }",
      "  <T> java.util.stream.Stream<T> withInClause(java.util.function.Supplier<java.util.stream.Stream<T>> s) {",
      "    return s.get();",
      "  }",
      "  public java.util.List<String> noLeak(Query query) {",
      "    try (var stream = withInClause(() -> query.fetchStream().map(Object::toString))) {",
      "      return stream.collect(java.util.stream.Collectors.toList());",
      "    }",
      "  }",
      "}");

  @Test
  public void flagsFetchStreamNotWrappedInTryWithResources() {
    List<RuleViolation> violations = analyze(FileId.fromPathLikeString("UnsafeSample.java"), UNSAFE_SOURCE);

    assertThat(violations)
        .as("the rule must flag a fetchStream() call that is not inside try-with-resources")
        .hasSize(1);
    assertThat(violations.get(0).getRule().getName()).isEqualTo(RULE_NAME);
  }

  @Test
  public void doesNotFlagFetchStreamWrappedInTryWithResources() {
    List<RuleViolation> violations = analyze(FileId.fromPathLikeString("SafeSample.java"), SAFE_SOURCE);

    assertThat(violations)
        .as("the rule must not flag a fetchStream() call already wrapped in try-with-resources")
        .isEmpty();
  }

  @Test
  public void doesNotFlagFetchStreamInsideLambdaWrappedInTryWithResources() {
    List<RuleViolation> violations = analyze(FileId.fromPathLikeString("SafeLambdaSample.java"), SAFE_LAMBDA_SOURCE);

    assertThat(violations)
        .as("the rule must not flag a fetchStream() inside a lambda whose resulting stream is "
            + "closed by try-with-resources at the call site (the getStreamWithSqlInClause pattern)")
        .isEmpty();
  }

  private static List<RuleViolation> analyze(FileId fileId, String source) {
    LanguageVersion java = LanguageRegistry.PMD.getLanguageById("java").getDefaultVersion();

    PMDConfiguration config = new PMDConfiguration();
    config.setDefaultLanguageVersion(java);

    try (PmdAnalysis pmd = PmdAnalysis.create(config)) {
      pmd.addRuleSet(pmd.newRuleSetLoader().loadFromResource(RULESET));
      pmd.files().addSourceFile(fileId, source);

      Report report = pmd.performAnalysisAndCollectReport();

      assertThat(report.getProcessingErrors())
          .as("PMD should parse the sample without processing errors")
          .isEmpty();

      return report.getViolations();
    }
  }
}
