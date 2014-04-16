/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;
import javax.naming.NamingException;

import com.sonatype.insight.brain.dataaccess.security.UserDAO;
import com.sonatype.insight.brain.ldap.LdapManager;
import com.sonatype.insight.brain.ldap.LdapUser;
import com.sonatype.insight.brain.model.security.User;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @since 1.11.0
 */
@Named
public class UserValidationService
{
  private static final Logger log = LoggerFactory.getLogger(UserValidationService.class);

  private final UserDAO userDAO;

  private final LdapManager ldapManager;

  @Inject
  public UserValidationService(final UserDAO userDAO, final LdapManager ldapManager) {
    this.userDAO = userDAO;
    this.ldapManager = ldapManager;
  }

  /**
   * Validate the users in the list and return a list of the invalid users.
   *
   * @param userNames the set of user name to lookup
   * @return set of invalid user names
   */
  public Set<String> validateUsers(final Set<String> userNames) {
    if (userNames == null || userNames.isEmpty()) {
      return Collections.emptySet();
    }

    final Set<String> notFoundSet = new HashSet<>();
    for (final String userName : userNames) {
      final User user = userDAO.getByUsername(userName);
      if (user == null) {
        notFoundSet.add(userName);
      }
    }

    if (notFoundSet.isEmpty()) {
      return Collections.emptySet();
    }

    if (ldapManager.isLdapEnabled()) {
      final String[] internalNames = notFoundSet.toArray(new String[notFoundSet.size()]);
      try {
        final List<LdapUser> ldapUsers = ldapManager.getUsers(internalNames, internalNames.length);
        for (final LdapUser ldapUser : ldapUsers) {
          notFoundSet.remove(ldapUser.getUsername());
        }
      }
      catch (NamingException e) {
        log.error(e.getMessage(), e);
      }
    }

    return notFoundSet;
  }
}
