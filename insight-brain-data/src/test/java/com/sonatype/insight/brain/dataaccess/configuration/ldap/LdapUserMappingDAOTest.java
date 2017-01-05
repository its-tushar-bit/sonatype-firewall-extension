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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

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
    assertNotNull(umap.getId());

    // select by id
    umap = dao.getById(umap.getId());

    assertEquals(server.getId(), umap.getServerId());
    assertEquals(userBaseDN, umap.getUserBaseDN());
    assertEquals(userSubtree, umap.isUserSubtree());
    assertEquals(userObjectClass, umap.getUserObjectClass());
    assertEquals(userFilter, umap.getUserFilter());
    assertEquals(userIDAttribute, umap.getUserIDAttribute());
    assertEquals(realNameAttribute, umap.getUserRealNameAttribute());
    assertEquals(emailAttribute, umap.getUserEmailAttribute());
    assertEquals(passwordAttribute, umap.getUserPasswordAttribute());

    assertEquals(groupMappingType, umap.getGroupMappingType());
    assertEquals(groupBaseDN, umap.getGroupBaseDN());
    assertEquals(groupObjectClass, umap.getGroupObjectClass());
    assertEquals(groupIDAttribute, umap.getGroupIDAttribute());
    assertEquals(groupMemberAttribute, umap.getGroupMemberAttribute());
    assertEquals(groupMemberFormat, umap.getGroupMemberFormat());
    assertEquals(userMemberOfGroupAttribute, umap.getUserMemberOfGroupAttribute());

    // server by server id
    assertNotNull(dao.getByServerId(server.getId()));

    // update
    String userBaseDN_changed = userBaseDN + "-changed";
    umap.setUserBaseDN(userBaseDN_changed);
    dao.update(umap);
    umap = dao.getById(umap.getId());
    assertEquals(userBaseDN_changed, umap.getUserBaseDN());

    // delete
    dao.delete(umap);
    assertNull(dao.getById(umap.getId()));
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
