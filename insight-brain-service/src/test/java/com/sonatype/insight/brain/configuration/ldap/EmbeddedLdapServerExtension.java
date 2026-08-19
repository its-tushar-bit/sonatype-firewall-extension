/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.configuration.ldap;

import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

/**
 * Reuse-safe, JUnit 5-native embedded LDAP server for tests that run inside a <em>reused</em> IQ server /
 * Spring-context cohort (the {@code insight-brain-variant-test-*} modules) as well as in plain unit tests.
 * <p>
 * Register it as an instance field with {@code @RegisterExtension}; each test method gets its own server:
 *
 * <pre>
 * {@code
 * &#64;RegisterExtension
 * private final EmbeddedLdapServerExtension ldapServer = new EmbeddedLdapServerExtension();
 *
 * &#64;Test
 * void authenticatesAgainstLdap() throws Exception {
 *   ldapServer.start();
 *   ldapServer.loadData("/MyTest/ldap_users.ldif");
 *   ... ldapServer.getPort() ...
 * }
 * }
 * </pre>
 *
 * The server is <b>not</b> started automatically because tests configure the LDIF data / authentication mode and then
 * call {@link #start()} themselves. It is always stopped after each test method by {@link #afterEach}, which frees the
 * dynamically allocated port and deletes the working directory. {@link #stop()} is idempotent (it returns early when
 * the server is not running), so a test that stops the server itself, or that never started it, is handled cleanly and
 * without cross-test leakage even though the surrounding Spring context / H2 fixture is reused.
 * <p>
 * The server binds to a dynamically allocated port by default (unless a caller pins one via {@code setPort}), so
 * concurrent CI agents and Surefire/Failsafe forks never collide on a fixed port. Read the port through
 * {@link #getPort()} at run time rather than hard-coding it.
 * <p>
 * This is the JUnit 5 counterpart to {@link TestLdapServer}, whose auto-teardown relies on the JUnit 4 {@code TestRule}
 * hook that is inert under Jupiter (leaving the embedded server running and its working directory undeleted). Prefer
 * this extension in Jupiter tests; {@link TestLdapServer} remains for tests that still run under the JUnit 4 Vintage
 * engine.
 */
public class EmbeddedLdapServerExtension
    extends EmbeddedLdapServer
    implements AfterEachCallback
{
  @Override
  public void afterEach(final ExtensionContext context) throws Exception {
    stop();
  }
}
