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
 * IQ Server on a real PostgreSQL database.
 *
 * <p>
 * Identical ergonomics to {@link IqH2Test}, but {@link IqPostgresServerExtension} points the
 * DataSource at the codebase's existing embedded-postgres (zonky) test cluster. Because it is a
 * different variant key it is a <b>separate</b> cached server from the H2 variant — exactly one
 * PostgreSQL IQ server for the fork, reused across every {@code @IqPostgresTest} (no per-test
 * restart).
 *
 * <p>
 * Use when: the test depends on PostgreSQL-specific behaviour (SQL dialect, JSONB, sequences,
 * concurrency, migrations). Otherwise prefer {@link IqH2Test} — it is faster.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@ExtendWith(IqPostgresServerExtension.class)
public @interface IqPostgresTest
{
}
