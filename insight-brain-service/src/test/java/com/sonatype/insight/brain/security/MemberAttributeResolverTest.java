/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import com.sonatype.insight.brain.TemporaryEntity;
import com.sonatype.insight.brain.configuration.ldap.LdapServer;
import com.sonatype.insight.brain.ldap.EmbeddedLdapServer;
import com.sonatype.insight.brain.ldap.LdapManager;
import com.sonatype.insight.brain.model.security.MemberType;
import com.sonatype.insight.brain.security.MembershipMappingResource.Member;

import org.sonatype.guice.bean.containers.InjectedTest;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static com.sonatype.insight.brain.ldap.EmbeddedLdapServer.newEmbeddedLdapServer;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class MemberAttributeResolverTest extends InjectedTest
{
  @Inject
  private LdapManager manager;

  private MemberAttributeResolver memberAttributeResolver;

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
    member.type = MemberType.USER;
    member.internalName = "clmUser";

    List<Member> members = new ArrayList<>();
    members.add(member);

    memberAttributeResolver.resolve(members);

    assertThat(member.internalName, is("clmUser"));
    assertThat(member.type, is(MemberType.USER));
    assertThat(member.email, is("clmUser@void.com"));
    assertThat(member.displayName, is("John Doe"));
    assertThat(member.realm, is("CLM"));
  }

  // Test both user and group to reduce the overhead of starting an EmbeddedLdapServer
  @Test
  public void testResolveLDAPUserAndGroup() throws Exception {
    EmbeddedLdapServer embeddedLdapServer = newEmbeddedLdapServer();
    embeddedLdapServer.start();
    embeddedLdapServer.loadData("/UserResourceTest/ldap_users.ldif");

    LdapServer ldapServer = tempEntity.newLdapServer("LDAP");
    tempEntity.newLdapConnection(ldapServer.getId(), embeddedLdapServer.getPort());
    tempEntity.newLdapUserMapping(ldapServer.getId());

    final Member userMember = new Member();
    userMember.type = MemberType.USER;
    userMember.internalName = "testuser";

    List<Member> members = new ArrayList<>();
    members.add(userMember);

    memberAttributeResolver.resolve(members);

    assertThat(userMember.internalName, is("testuser"));
    assertThat(userMember.type, is(MemberType.USER));
    assertThat(userMember.email, is("test.user@company.com"));
    assertThat(userMember.displayName, is("John Doe"));
    assertThat(userMember.realm, is("LDAP"));

    final Member groupMember = new Member();
    groupMember.type = MemberType.GROUP;
    groupMember.internalName = "Alpha";

    members = new ArrayList<>();
    members.add(groupMember);

    memberAttributeResolver.resolve(members);

    assertThat(groupMember.internalName, is("Alpha"));
    assertThat(groupMember.type, is(MemberType.GROUP));
    assertThat(groupMember.email, is(nullValue()));
    assertThat(groupMember.displayName, is("Alpha"));
    assertThat(groupMember.realm, is("LDAP"));
  }

  @Test
  public void testCLMUserShading() throws Exception {
    EmbeddedLdapServer embeddedLdapServer = newEmbeddedLdapServer();
    embeddedLdapServer.start();
    embeddedLdapServer.loadData("/UserResourceTest/ldap_users.ldif");

    LdapServer ldapServer = tempEntity.newLdapServer("LDAP");
    tempEntity.newLdapConnection(ldapServer.getId(), embeddedLdapServer.getPort());
    tempEntity.newLdapUserMapping(ldapServer.getId());

    final Member member = new Member();
    member.type = MemberType.USER;
    member.internalName = "testuser";

    List<Member> members = new ArrayList<>();
    members.add(member);

    memberAttributeResolver.resolve(members);

    assertThat(member.internalName, is("testuser"));
    assertThat(member.type, is(MemberType.USER));
    assertThat(member.email, is("test.user@company.com"));
    assertThat(member.displayName, is("John Doe"));
    assertThat(member.realm, is("LDAP"));

    tempEntity.newUser("testuser");
    // Need to reinitialize attribute resolver to clear cache
    memberAttributeResolver = new MemberAttributeResolver(manager);

    memberAttributeResolver.resolve(members);

    assertThat(member.internalName, is("testuser"));
    assertThat(member.type, is(MemberType.USER));
    assertThat(member.email, is("testuser@void.com"));
    assertThat(member.displayName, is("John Doe"));
    assertThat(member.realm, is("CLM"));
  }
}
