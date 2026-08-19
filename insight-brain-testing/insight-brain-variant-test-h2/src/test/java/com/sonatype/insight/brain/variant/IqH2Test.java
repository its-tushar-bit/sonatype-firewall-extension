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
 * IQ Server on an embedded H2 database.
 *
 * <p>
 * Put this single annotation on a plain JUnit 5 class and you get a full IQ Server (JAX-RS -&gt; DB)
 * backed by H2. There is <b>no base class to extend</b> — that is the point. {@link IqH2ServerExtension}
 * boots the proven test launcher <b>once</b> for this variant and reuses the running server (and its
 * Spring context) across every {@code @IqH2Test} in the fork; a {@link SpikeRestClient} field is
 * injected automatically.
 *
 * <p>
 * Use when: your test exercises single-tenant IQ behaviour and does not care about the DB engine
 * (the fast default). Use {@link IqPostgresTest} instead only when the test depends on
 * PostgreSQL-specific behaviour.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@ExtendWith(IqH2ServerExtension.class)
public @interface IqH2Test
{
}
