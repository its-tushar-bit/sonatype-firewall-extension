/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import org.slf4j.MDC;

public class MDCUsernameScope
    implements AutoCloseable
{
  public static final String USERNAME = "username";

  public static final String SYSTEM = "*SYSTEM";

  public static final String ANONYMOUS = "*UNKNOWN";

  public static MDCUsernameScope forAnonymous() {
    return forUser(ANONYMOUS);
  }

  public static MDCUsernameScope forSystem() {
    return forUser(SYSTEM);
  }

  public static MDCUsernameScope forUser(final String username) {
    return new MDCUsernameScope(username);
  }

  private MDCUsernameScope(final String username) {
    MDC.put(USERNAME, username);
  }

  @Override
  public void close() {
    MDC.remove(USERNAME);
  }
}
