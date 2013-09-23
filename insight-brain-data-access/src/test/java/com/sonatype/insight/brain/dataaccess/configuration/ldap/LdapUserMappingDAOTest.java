/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.configuration.ldap;

import com.sonatype.insight.brain.configuration.ldap.LdapGroupMappingType;
import com.sonatype.insight.brain.configuration.ldap.LdapServer;
import com.sonatype.insight.brain.configuration.ldap.LdapUserMapping;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class LdapUserMappingDAOTest
{
  final String userBaseDN = "userBaseDN";
  final boolean userSubtree = true;
  final String userObjectClass = "userObjectClass";
  final String userFilter = "userFilter";
  final String userIDAttribute = "userIDAttribute";
  final String realNameAttribute = "realNameAttribute";
  final String emailAttribute = "emailAttribute";
  final String passwordAttribute = "passwordAttribute";

  final LdapGroupMappingType groupMappingType = LdapGroupMappingType.STATIC;
  final String groupBaseDN = "groupBaseDN";
  final boolean groupSubtree = true;
  final String groupObjectClass = "groupObjectClass";
  final String groupIDAttribute = "groupIDAttribute";
  final String groupMemberAttribute = "groupMemberAttribute";
  final String groupMemberFormat = "groupMemberFormat";
  final String userMemberOfGroupAttribute = "userMemberOfGroupAttribute";

  private LdapUserMappingDAO dao = new LdapUserMappingDAO();

  private LdapServerDAO serverDao = new LdapServerDAO();

  private LdapServer server;

  @Before
  public void createTestServer() {
    server = new LdapServer();
    server.setName("testServer");
    serverDao.insert(server);
  }

  @After
  public void deleteLdapUserMapping() {
    for (LdapUserMapping umap : dao.getAll()) {
      dao.delete(umap);
    }
  }

  @Test
  public void testCRUD() {
    // insert
    LdapUserMapping umap = newLdapUserMapping();

    dao.insert(umap);
    Assert.assertNotNull(umap.getId());

    // select by id
    umap = dao.getById(umap.getId());

    Assert.assertEquals(server.getId(), umap.getServerId());
    Assert.assertEquals(userBaseDN, umap.getUserBaseDN());
    Assert.assertEquals(userSubtree, umap.isUserSubtree());
    Assert.assertEquals(userObjectClass, umap.getUserObjectClass());
    Assert.assertEquals(userFilter, umap.getUserFilter());
    Assert.assertEquals(userIDAttribute, umap.getUserIDAttribute());
    Assert.assertEquals(realNameAttribute, umap.getUserRealNameAttribute());
    Assert.assertEquals(emailAttribute, umap.getUserEmailAttribute());
    Assert.assertEquals(passwordAttribute, umap.getUserPasswordAttribute());

    Assert.assertEquals(groupMappingType, umap.getGroupMappingType());
    Assert.assertEquals(groupBaseDN, umap.getGroupBaseDN());
    Assert.assertEquals(groupObjectClass, umap.getGroupObjectClass());
    Assert.assertEquals(groupIDAttribute, umap.getGroupIDAttribute());
    Assert.assertEquals(groupMemberAttribute, umap.getGroupMemberAttribute());
    Assert.assertEquals(groupMemberFormat, umap.getGroupMemberFormat());
    Assert.assertEquals(userMemberOfGroupAttribute, umap.getUserMemberOfGroupAttribute());

    // server by server id
    Assert.assertNotNull(dao.getByServerId(server.getId()));

    // update
    String userBaseDN_changed = userBaseDN + "-changed";
    umap.setUserBaseDN(userBaseDN_changed);
    dao.update(umap);
    umap = dao.getById(umap.getId());
    Assert.assertEquals(userBaseDN_changed, umap.getUserBaseDN());

    // delete
    dao.delete(umap);
    Assert.assertNull(dao.getById(umap.getId()));
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
