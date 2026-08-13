/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.configuration.ldap;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

import javax.naming.NamingEnumeration;
import javax.naming.NoPermissionException;
import javax.naming.directory.SearchControls;
import javax.naming.ldap.Control;
import javax.naming.ldap.LdapContext;
import javax.naming.ldap.PagedResultsControl;

import com.sonatype.insight.brain.model.configuration.ldap.LdapConnection;
import com.sonatype.insight.brain.model.configuration.ldap.LdapUserMapping;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

public class LdapQueryTest
{
  @Test
  public void testSearch_PagedSearchNotAllowed() throws Exception {
    AtomicBoolean pagedSearchEnabled = new AtomicBoolean();
    LdapContext ldapContext = mock(LdapContext.class);
    doAnswer(invocation -> {
      Control[] requestControls = invocation.getArgument(0);
      pagedSearchEnabled
          .set(requestControls != null && Stream.of(requestControls).anyMatch(PagedResultsControl.class::isInstance));
      return null;
    }).when(ldapContext).setRequestControls(any());
    doAnswer(invocation -> {
      if (pagedSearchEnabled.get()) {
        throw new NoPermissionException("[LDAP: error code 50 - Insufficient Access Rights]");
      }
      return mock(NamingEnumeration.class);
    }).when(ldapContext).search(anyString(), anyString(), any(SearchControls.class));

    LdapQuery ldapQuery = new LdapQuery(new LdapConnection(), new LdapUserMapping());
    assertThat(ldapQuery.search(ldapContext, "baseDN", "filter", new SearchControls())).isNotNull();
  }
}
