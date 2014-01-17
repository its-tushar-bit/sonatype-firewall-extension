/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;

import com.sonatype.insight.brain.TemporaryEntity;
import com.sonatype.insight.brain.configuration.ldap.LdapServer;
import com.sonatype.insight.brain.ldap.LdapManager;
import com.sonatype.insight.brain.ldap.TestLdapServer;
import com.sonatype.insight.brain.model.security.MemberType;

import org.apache.commons.lang.StringUtils;
import org.eclipse.sisu.launch.InjectedTest;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class MemberAttributeResolverTest extends InjectedTest
{
  @Inject
  private LdapManager manager;

  private MemberAttributeResolver memberAttributeResolver;

  @Rule
  public TestLdapServer embeddedLdapServer = new TestLdapServer();

  @Rule
  public TemporaryEntity tempEntity = new TemporaryEntity();

  @Before
  public void init() {
    memberAttributeResolver = new MemberAttributeResolver(manager);
  }

  @Test
  public void testResolveCLMUser() {
    tempEntity.newUser("clmUser");

    final Member member = new Member();
    member.setType(MemberType.USER);
    member.setInternalName("clmUser");

    List<Member> members = Arrays.asList(member);

    memberAttributeResolver.resolve(members);

    assertMember(member, MemberType.USER, "clmUser", "John Doe", "clmUser@void.com", "CLM");
  }

  // Test both user and group to reduce the overhead of starting an EmbeddedLdapServer
  @Test
  public void testResolveLDAPUserAndGroup() throws Exception {
    embeddedLdapServer.start();
    embeddedLdapServer.loadData("/UserResourceTest/ldap_users.ldif");

    LdapServer ldapServer = tempEntity.newLdapServer("LDAP");
    tempEntity.newLdapConnection(ldapServer.getId(), embeddedLdapServer.getPort());
    tempEntity.newLdapUserMapping(ldapServer.getId());

    final Member userMember = new Member();
    userMember.setType(MemberType.USER);
    userMember.setInternalName("testuser");

    List<Member> members = Arrays.asList(userMember);

    memberAttributeResolver.resolve(members);

    assertMember(userMember, MemberType.USER, "testuser", "John Doe", "test.user@company.com", "LDAP");

    final Member groupMember = new Member();
    groupMember.setType(MemberType.GROUP);
    groupMember.setInternalName("Alpha");

    members = Arrays.asList(groupMember);

    memberAttributeResolver.resolve(members);

    assertMember(groupMember, MemberType.GROUP, "Alpha", "Alpha", null, "LDAP");
  }

  @Test
  public void testCLMUserShading() throws Exception {
    embeddedLdapServer.start();
    embeddedLdapServer.loadData("/UserResourceTest/ldap_users.ldif");

    LdapServer ldapServer = tempEntity.newLdapServer("LDAP");
    tempEntity.newLdapConnection(ldapServer.getId(), embeddedLdapServer.getPort());
    tempEntity.newLdapUserMapping(ldapServer.getId());

    final Member member = new Member();
    member.setType(MemberType.USER);
    member.setInternalName("testuser");

    List<Member> members = Arrays.asList(member);

    memberAttributeResolver.resolve(members);

    assertMember(member, MemberType.USER, "testuser", "John Doe", "test.user@company.com", "LDAP");

    tempEntity.newUser("testuser");
    // Need to reinitialize attribute resolver to clear cache
    memberAttributeResolver = new MemberAttributeResolver(manager);

    memberAttributeResolver.resolve(members);

    assertMember(member, MemberType.USER, "testuser", "John Doe", "testuser@void.com", "CLM");
  }

  private void assertMember(Member member, MemberType type, String internalName, String displayName, String email,
                            String realm)
  {
    assertThat(member.getType(), is(type));
    assertThat(member.getInternalName(), is(internalName));
    assertThat(member.getDisplayName(), is(displayName));
    if (StringUtils.isNotEmpty(email)) {
      assertThat(member.getEmail(), is(email));
    } else {
      assertThat(member.getEmail(), is(nullValue()));
    }
    assertThat(member.getRealm(), is(realm));
  }
}
