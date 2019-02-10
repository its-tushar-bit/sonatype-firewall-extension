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

import org.junit.Before;
import org.junit.Test;

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

  private LdapUserMappingDAO dao = new LdapUserMappingDAO();

  private LdapServer server;

  @Before
  public void createTestServer() {
    server = tempEntity.newLdapServer("testServer");
  }

  @Test
  public void testCRUD() {
    // insert
    LdapUserMapping umap = newLdapUserMapping();
    tempEntity.newLdapUserMapping(umap);
    assertThat(umap.getId()).isNotNull();

    // select by id
    umap = dao.getById(umap.getId());

    assertThat(umap.getServerId()).isEqualTo(server.getId());
    assertThat(umap.getUserBaseDN()).isEqualTo(userBaseDN);
    assertThat(umap.isUserSubtree()).isEqualTo(userSubtree);
    assertThat(umap.getUserObjectClass()).isEqualTo(userObjectClass);
    assertThat(umap.getUserFilter()).isEqualTo(userFilter);
    assertThat(umap.getUserIDAttribute()).isEqualTo(userIDAttribute);
    assertThat(umap.getUserRealNameAttribute()).isEqualTo(realNameAttribute);
    assertThat(umap.getUserEmailAttribute()).isEqualTo(emailAttribute);
    assertThat(umap.getUserPasswordAttribute()).isEqualTo(passwordAttribute);

    assertThat(umap.getGroupMappingType()).isEqualTo(groupMappingType);
    assertThat(umap.getGroupBaseDN()).isEqualTo(groupBaseDN);
    assertThat(umap.getGroupObjectClass()).isEqualTo(groupObjectClass);
    assertThat(umap.getGroupIDAttribute()).isEqualTo(groupIDAttribute);
    assertThat(umap.getGroupMemberAttribute()).isEqualTo(groupMemberAttribute);
    assertThat(umap.getGroupMemberFormat()).isEqualTo(groupMemberFormat);
    assertThat(umap.getUserMemberOfGroupAttribute()).isEqualTo(userMemberOfGroupAttribute);

    // server by server id
    assertThat(dao.getByServerId(server.getId())).isNotNull();

    // update
    String userBaseDnChanged = userBaseDN + "-changed";
    umap.setUserBaseDN(userBaseDnChanged);
    dao.update(umap);
    umap = dao.getById(umap.getId());
    assertThat(umap.getUserBaseDN()).isEqualTo(userBaseDnChanged);

    // delete
    dao.delete(umap);
    assertThat(dao.getById(umap.getId())).isNull();
  }

  private LdapUserMapping newLdapUserMapping() {
    LdapUserMapping umap = new LdapUserMapping();
    umap.setServerId(server.getId());
    umap.setUserBaseDN(userBaseDN);
    umap.setUserSubtree(userSubtree);
    umap.setUserObjectClass(userObjectClass);
    umap.setUserFilter(userFilter);
    umap.setUserIDAttribute(userIDAttribute);
    umap.setUserRealNameAttribute(realNameAttribute);
    umap.setUserEmailAttribute(emailAttribute);
    umap.setUserPasswordAttribute(passwordAttribute);
    umap.setGroupMappingType(groupMappingType);
    umap.setGroupBaseDN(groupBaseDN);
    umap.setGroupSubtree(groupSubtree);
    umap.setGroupObjectClass(groupObjectClass);
    umap.setGroupIDAttribute(groupIDAttribute);
    umap.setGroupMemberAttribute(groupMemberAttribute);
    umap.setGroupMemberFormat(groupMemberFormat);
    umap.setUserMemberOfGroupAttribute(userMemberOfGroupAttribute);
    return umap;
  }
}
