/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.configuration.ldap;

import com.sonatype.insight.brain.dataaccess.AbstractDbDAOTest;
import com.sonatype.insight.brain.model.configuration.ldap.LdapGroupMappingType;
import com.sonatype.insight.brain.model.configuration.ldap.LdapServer;
import com.sonatype.insight.brain.model.configuration.ldap.LdapUserMapping;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class LdapUserMappingDAOTest
    extends AbstractDbDAOTest
{
  private final String userBaseDN = "userBaseDN";

  private final boolean userSubtree = true;

  private final String userObjectClass = "userObjectClass";

  private final String userFilter = "userFilter";

  private final String userIDAttribute = "userIDAttribute";

  private final String realNameAttribute = "realNameAttribute";

  private final String emailAttribute = "emailAttribute";

  private final String passwordAttribute = "passwordAttribute";

  private final LdapGroupMappingType groupMappingType = LdapGroupMappingType.STATIC;

  private final String groupBaseDN = "groupBaseDN";

  private final boolean groupSubtree = true;

  private final String groupObjectClass = "groupObjectClass";

  private final String groupIDAttribute = "groupIDAttribute";

  private final String groupMemberAttribute = "groupMemberAttribute";

  private final String groupMemberFormat = "groupMemberFormat";

  private final String userMemberOfGroupAttribute = "userMemberOfGroupAttribute";

  private LdapUserMappingDAO dao;

  private LdapServer ldapServer;

  @BeforeEach
  @Override
  public void setup() {
    super.setup();
    dao = daoFactory.createLdapUserMappingDAO();
    ldapServer = tempEntity.newLdapServer("testServer");
  }

  @Test
  public void testCRUD() {
    // insert
    LdapUserMapping ldapUserMapping = newLdapUserMapping();
    tempEntity.newLdapUserMapping(ldapUserMapping);
    assertThat(ldapUserMapping.getId()).isNotNull();

    // select by id
    ldapUserMapping = dao.getById(ldapUserMapping.getId());

    assertThat(ldapUserMapping.getServerId()).isEqualTo(ldapServer.getId());
    assertThat(ldapUserMapping.getUserBaseDN()).isEqualTo(userBaseDN);
    assertThat(ldapUserMapping.isUserSubtree()).isEqualTo(userSubtree);
    assertThat(ldapUserMapping.getUserObjectClass()).isEqualTo(userObjectClass);
    assertThat(ldapUserMapping.getUserFilter()).isEqualTo(userFilter);
    assertThat(ldapUserMapping.getUserIDAttribute()).isEqualTo(userIDAttribute);
    assertThat(ldapUserMapping.getUserRealNameAttribute()).isEqualTo(realNameAttribute);
    assertThat(ldapUserMapping.getUserEmailAttribute()).isEqualTo(emailAttribute);
    assertThat(ldapUserMapping.getUserPasswordAttribute()).isEqualTo(passwordAttribute);

    assertThat(ldapUserMapping.getGroupMappingType()).isEqualTo(groupMappingType);
    assertThat(ldapUserMapping.getGroupBaseDN()).isEqualTo(groupBaseDN);
    assertThat(ldapUserMapping.getGroupObjectClass()).isEqualTo(groupObjectClass);
    assertThat(ldapUserMapping.getGroupIDAttribute()).isEqualTo(groupIDAttribute);
    assertThat(ldapUserMapping.getGroupMemberAttribute()).isEqualTo(groupMemberAttribute);
    assertThat(ldapUserMapping.getGroupMemberFormat()).isEqualTo(groupMemberFormat);
    assertThat(ldapUserMapping.getUserMemberOfGroupAttribute()).isEqualTo(userMemberOfGroupAttribute);

    // server by server id
    assertThat(dao.getByServerId(ldapServer.getId())).isNotNull();

    // update
    String userBaseDnChanged = userBaseDN + "-changed";
    ldapUserMapping.setUserBaseDN(userBaseDnChanged);
    dao.update(ldapUserMapping);
    ldapUserMapping = dao.getById(ldapUserMapping.getId());
    assertThat(ldapUserMapping.getUserBaseDN()).isEqualTo(userBaseDnChanged);

    // delete
    dao.delete(ldapUserMapping);
    assertThat(dao.getById(ldapUserMapping.getId())).isNull();
  }

  private LdapUserMapping newLdapUserMapping() {
    LdapUserMapping ldapUserMapping = new LdapUserMapping();
    ldapUserMapping.setServerId(ldapServer.getId());
    ldapUserMapping.setUserBaseDN(userBaseDN);
    ldapUserMapping.setUserSubtree(userSubtree);
    ldapUserMapping.setUserObjectClass(userObjectClass);
    ldapUserMapping.setUserFilter(userFilter);
    ldapUserMapping.setUserIDAttribute(userIDAttribute);
    ldapUserMapping.setUserRealNameAttribute(realNameAttribute);
    ldapUserMapping.setUserEmailAttribute(emailAttribute);
    ldapUserMapping.setUserPasswordAttribute(passwordAttribute);
    ldapUserMapping.setGroupMappingType(groupMappingType);
    ldapUserMapping.setGroupBaseDN(groupBaseDN);
    ldapUserMapping.setGroupSubtree(groupSubtree);
    ldapUserMapping.setGroupObjectClass(groupObjectClass);
    ldapUserMapping.setGroupIDAttribute(groupIDAttribute);
    ldapUserMapping.setGroupMemberAttribute(groupMemberAttribute);
    ldapUserMapping.setGroupMemberFormat(groupMemberFormat);
    ldapUserMapping.setUserMemberOfGroupAttribute(userMemberOfGroupAttribute);
    return ldapUserMapping;
  }
}
