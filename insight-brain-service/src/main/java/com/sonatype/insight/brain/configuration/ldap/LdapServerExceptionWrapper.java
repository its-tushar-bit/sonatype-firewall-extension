/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.configuration.ldap;

import java.util.List;

import javax.naming.AuthenticationException;
import javax.naming.NameNotFoundException;
import javax.naming.NamingException;
import javax.naming.NamingSecurityException;

import com.sonatype.insight.brain.model.configuration.ldap.LdapServer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Wrap LDAP authentication error with the LDAP server against which the error occurred.
 *
 * @since 1.23
 */
class LdapServerExceptionWrapper
{
  private static final Logger log = LoggerFactory.getLogger(LdapServerExceptionWrapper.class);

  private final LdapServer ldapServer;

  private final NamingException namingException;

  LdapServerExceptionWrapper(final LdapServer ldapServer, final NamingException namingException) {
    this.ldapServer = ldapServer;
    this.namingException = namingException;
  }

  private String createServerWrappedExplanationMessage() {
    return "LDAP Server: " + ldapServer.getName() + " -> " + namingException.getMessage();
  }

  /**
   * Throw authentication errors according to the precedence below. Helps maintain client side error messaging, as per
   * https://issues.sonatype.org/browse/CLM-6045.
   *
   * @see com.sonatype.insight.brain.service.ErrorResponseGenerator#buildErrorResponse()
   */
  static NamingException getSomethingToThrow(final List<LdapServerExceptionWrapper> ldapServerExceptionWrappers) {
    // if only one exception exists, just use that exception. Produces existing "single-LDAP Server" behavior.
    if (ldapServerExceptionWrappers.size() == 1) {
      return ldapServerExceptionWrappers.get(0).namingException;
    }

    // throw more common 'bad password' type of exception first
    for (final LdapServerExceptionWrapper exception : ldapServerExceptionWrappers) {
      if (exception.namingException instanceof NamingSecurityException) {
        return addAllToSuppressedAndLogAll(ldapServerExceptionWrappers,
            new AuthenticationException(exception.createServerWrappedExplanationMessage()));
      }
    }

    // aggregate and throw any "timeout" exceptions next
    final StringBuilder timeoutMsg = new StringBuilder();
    for (final LdapServerExceptionWrapper exception : ldapServerExceptionWrappers) {
      if (String.valueOf(exception.namingException.getMessage()).contains("timeout")) {
        timeoutMsg.append(exception.createServerWrappedExplanationMessage()).append(";\n");
      }
    }
    if (timeoutMsg.length() > 0) {
      return addAllToSuppressedAndLogAll(ldapServerExceptionWrappers, new NamingException(timeoutMsg.toString()));
    }

    // if all errors are 'NameNotFoundException' (unknown user),
    // then throw an exception that will result in 'Invalid Credentials' message at client.
    boolean isAllUnknownUser = true;
    for (final LdapServerExceptionWrapper exception : ldapServerExceptionWrappers) {
      if (!(exception.namingException instanceof NameNotFoundException)) {
        isAllUnknownUser = false;
        break;
      }
    }
    if (isAllUnknownUser) {
      final StringBuilder msg = new StringBuilder();
      for (final LdapServerExceptionWrapper exception : ldapServerExceptionWrappers) {
        msg.append(exception.createServerWrappedExplanationMessage()).append(";\n");
      }
      return addAllToSuppressedAndLogAll(ldapServerExceptionWrappers, new NameNotFoundException(msg.toString()));
    }

    // throw what we know
    final StringBuilder msg = new StringBuilder();
    for (final LdapServerExceptionWrapper exception : ldapServerExceptionWrappers) {
      msg.append(exception.createServerWrappedExplanationMessage()).append(";\n");
    }
    return addAllToSuppressedAndLogAll(ldapServerExceptionWrappers, new NamingException(msg.toString()));
  }

  private static NamingException addAllToSuppressedAndLogAll(
      final List<LdapServerExceptionWrapper> ldapServerExceptionWrappers,
      final NamingException exceptionToThrow)
  {
    for (final LdapServerExceptionWrapper exception : ldapServerExceptionWrappers) {
      exceptionToThrow.addSuppressed(exception.namingException);
      // especially when using RUT auth, unknown usernames are to be expected, so let's keep the noise from
      // NameNotFoundException down to a minimum and omit its stack trace
      log.info(exception.createServerWrappedExplanationMessage(),
          exception.namingException instanceof NameNotFoundException ? null : exception.namingException);
    }
    return exceptionToThrow;
  }
}
