/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.variant;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Marks an own-server integration test (a test extending {@code AbstractBaseIntegrationTest} /
 * {@code AbstractResourceTest} / {@code AbstractAuditTest}) that runs under the JUnit 5 (Jupiter) engine.
 * Registers {@link LegacyServerHarnessExtension}, which reproduces the chain's ordered JUnit 4 {@code @Rule}
 * lifecycle (DB / search / license / temp-entity fixtures + {@code @Rule TestName} seeding) as Jupiter
 * callbacks. The chain's {@code @Before}/{@code @After} bodies (server boot/reset) run via their dual
 * {@code @BeforeEach}/{@code @AfterEach} annotations.
 *
 * <p>
 * No {@code SpringExtension} is needed: these tests reach beans through {@code getCLMServer().getInstance(..)}
 * / {@code lookup(..)} rather than field injection.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@ExtendWith(LegacyServerHarnessExtension.class)
public @interface LegacyServerTest
{
}
