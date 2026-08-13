/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.variant;

import com.sonatype.insight.brain.service.AbstractServiceAuthzTest;

import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.context.junit.jupiter.SpringExtension;

/**
 * Authz counterpart of {@link AbstractComponentPgTest}: the Jupiter Spring wiring
 * ({@code @ExtendWith(SpringExtension.class)}) for reused-context Postgres authz component tests, kept off the shared
 * {@code SpringInjectedTest}/{@code AbstractServiceAuthzTest} chain so the JUnit-4 (vintage) service tests stay
 * vintage-only and keep their {@code @Category} group exclusions. See {@link AbstractComponentPgTest} for details.
 *
 * <p>
 * Converted authz component tests extend this instead of {@code AbstractServiceAuthzTest} and carry
 * {@code @ComponentPgTest}. The real Shiro {@code SecurityManager}/{@code Subject}/{@code user} setup is inherited
 * unchanged from {@code AbstractServiceAuthzTest}.
 */
@ExtendWith(SpringExtension.class)
public abstract class AbstractComponentPgAuthzTest
    extends AbstractServiceAuthzTest
{
}
