/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.variant;

import com.sonatype.insight.brain.service.AbstractComponentAuditTest;

import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.context.junit.jupiter.SpringExtension;

/**
 * Audit counterpart of {@link AbstractComponentH2Test}: the Jupiter Spring wiring
 * ({@code @ExtendWith(SpringExtension.class)}) for reused-context H2 component audit tests, kept off the shared
 * {@code SpringInjectedTest}/{@code AbstractComponentAuditTest} chain so the JUnit-4 (vintage) service tests stay
 * vintage-only. Converted component audit tests extend this instead of {@code AbstractComponentAuditTest} and carry
 * {@code @ComponentH2Test}. The audit-log capture helpers are inherited unchanged from
 * {@code AbstractComponentAuditTest}.
 */
@ExtendWith(SpringExtension.class)
public abstract class AbstractComponentH2AuditTest
    extends AbstractComponentAuditTest
{
}
