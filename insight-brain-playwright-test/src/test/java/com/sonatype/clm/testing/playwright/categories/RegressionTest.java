/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.categories;

/**
 * JUnit 4 category marker for the nightly "regression" partition of the Playwright suite.
 * <p>
 * <b>Tagging policy.</b> Every Playwright {@code @Test} method belongs to exactly one
 * partition: {@code @Category(SanityTest.class)} for tests that gate PR merge, or
 * {@code @Category(RegressionTest.class)} for tests that run in the nightly regression job.
 * The two partitions are mutually exclusive &mdash; a test never has both categories.
 * <p>
 * <b>Inclusion rule for regression.</b> Tag a {@code @Test} with
 * {@code @Category(RegressionTest.class)} for edge cases, error paths, slow data setup,
 * permutation matrices, and feature-flag combinations that do not need to gate every PR.
 * <p>
 * <b>Status.</b> Reserved for the nightly partition under development. No tests are tagged with
 * this category yet &mdash; every existing {@code @Test} is on the sanity partition. The split
 * will be populated as soon as the nightly job is wired up; no Maven {@code regression}
 * profile is shipped in the meantime to keep the build behaviour predictable.
 *
 * @see SanityTest
 */
public interface RegressionTest
{
}
