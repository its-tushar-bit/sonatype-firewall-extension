/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.categories;

/**
 * JUnit 4 category marker for the PR pre-merge "sanity" partition of the Playwright suite.
 * <p>
 * <b>Tagging policy.</b> Every Playwright {@code @Test} method belongs to exactly one
 * partition: {@code @Category(SanityTest.class)} for tests that gate PR merge, or
 * {@code @Category(RegressionTest.class)} for tests that run in the nightly regression job.
 * The two partitions are mutually exclusive &mdash; a test never has both categories and
 * never has neither.
 * <p>
 * <b>Inclusion rule for sanity.</b> Tag a {@code @Test} with {@code @Category(SanityTest.class)}
 * only if a failure should block PR merge &mdash; typically critical user paths (login,
 * navigation, base-URL handling, top-level routing) and the happy path of each feature area.
 * <p>
 * <b>Exclusion rule for sanity.</b> Edge cases, error paths, slow data setup, permutation
 * matrices, and feature-flag combinations belong in regression, not sanity.
 * <p>
 * <b>Current state.</b> Most Playwright tests are tagged {@code SanityTest}. Move
 * edge-case and slow tests to {@code @Category(RegressionTest.class)} as the nightly
 * regression job is wired up.
 * <p>
 * Failsafe is wired up (in {@code insight-brain-playwright-test/pom.xml}) to honor the
 * {@code failsafe.groups} / {@code failsafe.excludedGroups} properties. CI activates the
 * sanity partition with the {@code sanity} profile:
 *
 * <pre>{@code
 *   mvn verify -pl insight-brain-playwright-test -Psanity
 *
 *   # equivalent explicit form:
 *   mvn verify -pl insight-brain-playwright-test \
 *     -Dfailsafe.groups=com.sonatype.clm.testing.playwright.categories.SanityTest
 * }</pre>
 */
public interface SanityTest
{
}
