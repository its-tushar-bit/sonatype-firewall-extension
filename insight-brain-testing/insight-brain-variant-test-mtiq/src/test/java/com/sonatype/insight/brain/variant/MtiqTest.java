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
 * Multi-Tenant IQ (MTIQ).
 *
 * <p>
 * Same single-annotation ergonomics as {@code @IqH2Test}/{@code @IqPostgresTest}, but
 * {@link MtiqServerExtension} boots {@code MultiTenantInsightBrainService} (the IQ bean graph plus the
 * tenancy layer) on the multi-tenant embedded-postgres cluster. Because the variant key differs it is
 * its own cached server — exactly one MTIQ server for the fork, reused across every {@code @MtiqTest}.
 *
 * <p>
 * Requests run under the global tenant. Use when: the test exercises tenant-agnostic behaviour on
 * the multi-tenant server. Tenant-isolation tests would additionally provision/select a tenant; that
 * is out of scope for this API spike.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@ExtendWith(MtiqServerExtension.class)
public @interface MtiqTest
{
}
