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
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.event.ApplicationEventsTestExecutionListener;
import org.springframework.test.context.event.EventPublishingTestExecutionListener;
import org.springframework.test.context.jdbc.SqlScriptsTestExecutionListener;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.context.support.DirtiesContextBeforeModesTestExecutionListener;
import org.springframework.test.context.support.DirtiesContextTestExecutionListener;
import org.springframework.test.context.transaction.TransactionalTestExecutionListener;
import org.springframework.test.context.web.ServletTestExecutionListener;

/**
 * Variant of {@link ComponentH2Test} for the handful of component tests that MUST NOT share the reused H2
 * {@code ApplicationContext} because they mutate process-wide, hard-to-restore singleton state (for example
 * {@code insightConfig.storage}, which changes the runtime type of the lazily-created {@code @Primary}
 * storage beans in {@code PersistenceConfiguration}). A sibling class that then injects the concrete
 * {@code File*} type by name would otherwise fail with a bean-type mismatch once the module shards its
 * classes across forks in a nondeterministic order.
 *
 * <p>
 * It reproduces {@link ComponentH2Test}'s wiring — the {@code ComponentTestDbHarnessExtension} plus the same
 * Spring {@code TestExecutionListener} set — but KEEPS the two {@code DirtiesContext*TestExecutionListener}s
 * and carries {@code @DirtiesContext(AFTER_CLASS)}, so the shared context is evicted (rebuilt clean) after a
 * class annotated with this runs. Everything else (the {@code extends AbstractComponentTest} chain, injected
 * fields, {@code lookup}/{@code grant*} helpers) is inherited exactly as with {@link ComponentH2Test}.
 *
 * <p>
 * Use this ONLY for the rare context-polluting cases; plain reused-context tests must keep {@link ComponentH2Test}
 * so the context + single H2 fixture are built once and reused for the whole module.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Inherited
@ExtendWith(ComponentTestDbHarnessExtension.class)
@TestExecutionListeners(
    listeners = {
      ServletTestExecutionListener.class,
      DirtiesContextBeforeModesTestExecutionListener.class,
      ApplicationEventsTestExecutionListener.class,
      DependencyInjectionTestExecutionListener.class,
      DirtiesContextTestExecutionListener.class,
      TransactionalTestExecutionListener.class,
      SqlScriptsTestExecutionListener.class,
      EventPublishingTestExecutionListener.class
    },
    inheritListeners = false)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public @interface ComponentH2DirtiesContextTest
{
}
