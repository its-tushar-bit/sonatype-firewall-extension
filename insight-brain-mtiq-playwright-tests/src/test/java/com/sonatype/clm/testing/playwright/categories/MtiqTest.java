/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.categories;

/**
 * JUnit 4 category marker for the multi-tenant (MTIQ) Playwright tests.
 * <p>
 * <b>Module.</b> MTIQ UI tests live in their own {@code insight-brain-mtiq-playwright-tests} module
 * so that {@code nexus-mtiq-server} stays off the single-tenant Playwright classpath. Every test in
 * this module extends {@code AbstractMtiqUiTest} and boots an embedded multi-tenant IQ server.
 * <p>
 * <b>Running.</b> The whole module is the MTIQ partition, so no group filtering is required:
 * {@code mvn verify -pl insight-brain-mtiq-playwright-tests}. This marker remains as a triage tag
 * identifying tests that target the MTIQ variant of Nexus IQ Server.
 */
public interface MtiqTest
{
}
