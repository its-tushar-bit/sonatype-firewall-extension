/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import com.sonatype.insight.brain.variant.AbstractBrainInjectedH2Test;
import com.sonatype.insight.brain.variant.ComponentH2Test;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import jakarta.inject.Inject;

import com.sonatype.insight.brain.configuration.ldap.LdapService;
import com.sonatype.insight.brain.configuration.ldap.TestLdapServer;
import com.sonatype.insight.brain.model.configuration.ldap.LdapServer;
import com.sonatype.insight.brain.model.security.MemberType;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@ComponentH2Test
public class MemberAttributeResolverTest
    extends AbstractBrainInjectedH2Test
{
  @Inject
  private UserDirectory userDirectory;

  @Inject
  private LdapService ldapService;

  private MemberAttributeResolver memberAttributeResolver;

  public TestLdapServer embeddedLdapServer1 = new TestLdapServer();

  public TestLdapServer embeddedLdapServer2 = new TestLdapServer();

  @BeforeEach
  public void init() {
    memberAttributeResolver = new MemberAttributeResolver(userDirectory);
  }

  @Test
  public void testResolveIqUser() {
    tempEntity.newUser("clmUser");

    final Member member = new Member();
    member.setType(MemberType.USER);
    member.setInternalName("clmUser");

    List<Member> members = Collections.singletonList(member);

    memberAttributeResolver.resolve(members);

    assertMember(member, MemberType.USER, "clmUser", "John Doe", "clmUser@void.com", "IQ Server", null);
  }

  // Test both user and group to reduce the overhead of starting an EmbeddedLdapServer
  @Test
  public void testResolveLDAPUserAndGroup() throws Exception {
    embeddedLdapServer1.start();
    embeddedLdapServer1.loadData("/MemberAttributeResolverTest/ldap_users1.ldif");

    embeddedLdapServer2.start();
    embeddedLdapServer2.loadData("/MemberAttributeResolverTest/ldap_users2.ldif");

    LdapServer ldapServer1 = tempEntity.newLdapServer("LDAP");
    tempEntity.newLdapConnection(ldapServer1.getId(), embeddedLdapServer1.getPort());
    tempEntity.newLdapUserMapping(ldapServer1.getId());

    LdapServer ldapServer2 = tempEntity.newLdapServer("LDAP2");
    tempEntity.newLdapConnection(ldapServer2.getId(), embeddedLdapServer2.getPort());
    tempEntity.newLdapUserMapping(ldapServer2.getId());

    final Member userMember1 = new Member();
    userMember1.setType(MemberType.USER);
    userMember1.setInternalName("testuser1_1");

    final Member userMember2 = new Member();
    userMember2.setType(MemberType.USER);
    userMember2.setInternalName("testuser1_2");

    List<Member> members = Arrays.asList(userMember1, userMember2);

    memberAttributeResolver.resolve(members);

    assertMember(userMember1, MemberType.USER, "testuser1_1", "John Doe",
        "test.user1_1@company.com", "LDAP", "uid=testuser1_1,ou=users,dc=company,dc=com");
    assertMember(userMember2, MemberType.USER, "testuser1_2", "John Doe",
        "test.user1_2@company.com", "LDAP2", "uid=testuser1_2,ou=users,dc=company,dc=com");

    final Member groupMember1 = new Member();
    groupMember1.setType(MemberType.GROUP);
    groupMember1.setInternalName("Alpha1");

    final Member groupMember2 = new Member();
    groupMember2.setType(MemberType.GROUP);
    groupMember2.setInternalName("Alpha2");

    members = Arrays.asList(groupMember1, groupMember2);

    memberAttributeResolver.resolve(members);

    assertMember(groupMember1, MemberType.GROUP, "Alpha1", "Alpha1", null, "LDAP",
        "cn=Alpha1,ou=groups,dc=company,dc=com");
    assertMember(groupMember2, MemberType.GROUP, "Alpha2", "Alpha2", null, "LDAP2",
        "cn=Alpha2,ou=groups,dc=company,dc=com");
  }

  @Test
  public void testResolveLDAPGroup_GroupSearchNotEnabled() throws Exception {
    embeddedLdapServer1.start();
    embeddedLdapServer1.loadData("/MemberAttributeResolverTest/ldap_users1.ldif");

    embeddedLdapServer2.start();
    embeddedLdapServer2.loadData("/MemberAttributeResolverTest/ldap_users2.ldif");

    LdapServer ldapServer1 = tempEntity.newLdapServer("LDAP");
    tempEntity.newLdapConnection(ldapServer1.getId(), embeddedLdapServer1.getPort());

    LdapServer ldapServer2 = tempEntity.newLdapServer("LDAP2");
    tempEntity.newLdapConnection(ldapServer2.getId(), embeddedLdapServer2.getPort());
    tempEntity.newLdapUserMapping(ldapServer2.getId());

    assertThat(ldapService.isGroupSearchEnabled(ldapServer1)).isFalse();
    assertThat(ldapService.isGroupSearchEnabled(ldapServer2)).isTrue();

    final Member groupMember1 = new Member();
    groupMember1.setType(MemberType.GROUP);
    groupMember1.setInternalName("Alpha1");

    final Member groupMember2 = new Member();
    groupMember2.setType(MemberType.GROUP);
    groupMember2.setInternalName("Alpha2");

    List<Member> members = Arrays.asList(groupMember1, groupMember2);

    memberAttributeResolver.resolve(members);

    assertThat(members).hasSize(2);
    assertMember(groupMember1, MemberType.GROUP, "Alpha1", null, null, null,
        null);
    assertMember(groupMember2, MemberType.GROUP, "Alpha2", "Alpha2", null, "LDAP2",
        "cn=Alpha2,ou=groups,dc=company,dc=com");
  }

  @Test
  public void testIqUserShading() throws Exception {
    embeddedLdapServer1.start();
    embeddedLdapServer1.loadData("/MemberAttributeResolverTest/ldap_users1.ldif");

    embeddedLdapServer2.start();
    embeddedLdapServer2.loadData("/MemberAttributeResolverTest/ldap_users2.ldif");

    LdapServer ldapServer1 = tempEntity.newLdapServer("LDAP");
    tempEntity.newLdapConnection(ldapServer1.getId(), embeddedLdapServer1.getPort());
    tempEntity.newLdapUserMapping(ldapServer1.getId());

    LdapServer ldapServer2 = tempEntity.newLdapServer("LDAP2");
    tempEntity.newLdapConnection(ldapServer2.getId(), embeddedLdapServer2.getPort());
    tempEntity.newLdapUserMapping(ldapServer2.getId());

    final Member member1 = new Member();
    member1.setType(MemberType.USER);
    member1.setInternalName("testuser1_1");

    final Member member2 = new Member();
    member2.setType(MemberType.USER);
    member2.setInternalName("testuser1_2");

    List<Member> members = Arrays.asList(member1, member2);

    memberAttributeResolver.resolve(members);

    assertMember(member1, MemberType.USER, "testuser1_1", "John Doe",
        "test.user1_1@company.com", "LDAP", "uid=testuser1_1,ou=users,dc=company,dc=com");
    assertMember(member2, MemberType.USER, "testuser1_2", "John Doe",
        "test.user1_2@company.com", "LDAP2", "uid=testuser1_2,ou=users,dc=company,dc=com");

    tempEntity.newUser("testuser1_1");
    tempEntity.newUser("testuser1_2");

    // Need to reinitialize attribute resolver to clear cache
    memberAttributeResolver = new MemberAttributeResolver(userDirectory);

    memberAttributeResolver.resolve(members);

    assertMember(member1, MemberType.USER, "testuser1_1", "John Doe",
        "testuser1_1@void.com", "IQ Server", null);
    assertMember(member2, MemberType.USER, "testuser1_2", "John Doe",
        "testuser1_2@void.com", "IQ Server", null);
  }

  private void assertMember(
      Member member,
      MemberType type,
      String internalName,
      String displayName,
      String email,
      String realm,
      String dn)
  {
    assertThat(member.getType()).isEqualTo(type);
    assertThat(member.getInternalName()).isEqualTo(internalName);
    assertThat(member.getDisplayName()).isEqualTo(displayName);
    assertThat(member.getEmail()).isEqualTo(email);
    assertThat(member.getRealm()).isEqualTo(realm);
    assertThat(member.getDn()).isEqualTo(dn);
  }
}
