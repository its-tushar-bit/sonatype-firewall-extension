/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.configuration.ldap;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.sonatype.insight.brain.dataaccess.AbstractOperationalSqlDAO;
import com.sonatype.insight.brain.dataaccess.DataAccessException;
import com.sonatype.insight.brain.dataaccess.NameableDAOTest;
import com.sonatype.insight.brain.dataaccess.filter.DashboardFilterDAO;
import com.sonatype.insight.brain.dataaccess.filter.UserFilterDAO;
import com.sonatype.insight.brain.dataaccess.notification.UserViewedProductNotificationDAO;
import com.sonatype.insight.brain.dataaccess.security.UserTokenDAO;
import com.sonatype.insight.brain.model.NameHelper;
import com.sonatype.insight.brain.model.configuration.ldap.LdapConnection;
import com.sonatype.insight.brain.model.configuration.ldap.LdapServer;
import com.sonatype.insight.brain.model.configuration.ldap.LdapUserMapping;
import com.sonatype.insight.brain.model.filter.DashboardFilter;
import com.sonatype.insight.brain.model.filter.UserFilter;
import com.sonatype.insight.brain.model.filter.UserFilterType;
import com.sonatype.insight.brain.model.notification.UserViewedProductNotification;
import com.sonatype.insight.brain.model.security.UserToken;
import com.sonatype.insight.error.exception.NotFoundException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

public class LdapServerDAOTest
    extends NameableDAOTest<LdapServer>
{
  private LdapConnectionDAO ldapConnectionDAO;

  private LdapUserMappingDAO ldapUserMappingDAO;

  private UserTokenDAO userTokenDAO;

  private DashboardFilterDAO dashboardFilterDAO;

  private UserFilterDAO userFilterDAO;

  private UserViewedProductNotificationDAO userViewedProductNotificationDAO;

  private LdapServerDAO dao;

  @BeforeEach
  @Override
  public void setup() {
    super.setup();
    ldapConnectionDAO = daoFactory.createLdapConnectionDAO();
    ldapUserMappingDAO = daoFactory.createLdapUserMappingDAO();
    userTokenDAO = daoFactory.createUserTokenDAO();
    dashboardFilterDAO = daoFactory.createDashboardFilterDAO();
    userFilterDAO = daoFactory.createUserFilterDAO();
    userViewedProductNotificationDAO = daoFactory.createUserViewedProductNotificationDAO();
    dao = daoFactory.createLdapServerDAO();
  }

  @Override
  protected LdapServer createNameable(String a) {
    return tempEntity.newLdapServer(a);
  }

  @Override
  protected AbstractOperationalSqlDAO<LdapServer> getDao() {
    return dao;
  }

  @Override
  protected int getMaxNameLength() {
    return NameHelper.MAX_NAME_LENGTH;
  }

  @Override
  protected LdapServer getEntityByName(String name) {
    return dao.getByName(name);
  }

  @Test
  public void testCRUD() {
    String name = "name";

    // insert

    LdapServer ldapServer = tempEntity.newLdapServer(name);

    // select by id

    LdapServer echo = dao.getById(ldapServer.getId());
    assertThat(echo).isNotNull();
    assertThat(echo.getName()).isEqualTo(name);
    assertThat(echo.getNameLowercaseNoWhitespace()).isEqualTo(NameHelper.normalize(name));

    // select by name

    echo = dao.getByName(name);
    assertThat(echo).isNotNull();

    // update

    String changedName = "changedName";
    ldapServer.setName(changedName);
    dao.update(ldapServer);
    echo = dao.getById(ldapServer.getId());
    assertThat(echo.getName()).isEqualTo(changedName);

    // delete
    dao.delete(ldapServer);
    assertThat(dao.getById(ldapServer.getId())).isNull();
  }

  @Test
  public void testInsert_AutoIncrementsPriority() {
    LdapServer ldapServer1 = tempEntity.newLdapServer("test1");
    LdapServer ldapServer2 = tempEntity.newLdapServer("test2");

    assertThat(ldapServer2.getPriority()).isGreaterThan(ldapServer1.getPriority());
  }

  @Test
  public void testUpdatePriority() {
    LdapServer ldapServer1 = tempEntity.newLdapServer("test1");
    LdapServer ldapServer2 = tempEntity.newLdapServer("test2");

    List<String> serverPriorityList = new ArrayList<>();
    serverPriorityList.add(ldapServer2.getId());
    serverPriorityList.add(ldapServer1.getId());

    dao.updatePriority(serverPriorityList);

    List<LdapServer> servers = dao.getAll();
    assertThat(servers.get(0).getName()).isEqualTo("test2");
    assertThat(servers.get(0).getPriority()).isEqualTo(1);
    assertThat(servers.get(1).getName()).isEqualTo("test1");
    assertThat(servers.get(1).getPriority()).isEqualTo(2);
  }

  @Test
  public void testUpdatePriority_IncorrectNumberOfServers() {
    tempEntity.newLdapServer("test1");
    LdapServer ldapServer2 = tempEntity.newLdapServer("test2");

    List<String> mismatchServerList = Collections.singletonList(ldapServer2.getId());

    assertThatExceptionOfType(DataAccessException.class).isThrownBy(() -> dao.updatePriority(mismatchServerList))
        .withMessageContaining("Unable to update priority of Ldap servers due to server list mismatch.");
  }

  @Test
  public void testUpdatePriority_DuplicateServers() {
    LdapServer ldapServer1 = tempEntity.newLdapServer("test1");
    LdapServer ldapServer2 = tempEntity.newLdapServer("test2");

    List<String> serverPriorityList = new ArrayList<>();
    serverPriorityList.add(ldapServer1.getId());
    serverPriorityList.add(ldapServer1.getId());
    serverPriorityList.add(ldapServer2.getId());

    assertThatExceptionOfType(DataAccessException.class).isThrownBy(() -> dao.updatePriority(serverPriorityList))
        .withMessageContaining("Unable to update priority of Ldap servers due to duplicate server IDs.");
  }

  @Test
  public void testUpdatePriority_IncorrectServerId() {
    tempEntity.newLdapServer("test1");
    LdapServer ldapServer2 = tempEntity.newLdapServer("test2");

    List<String> serverPriorityList = new ArrayList<>();
    serverPriorityList.add(ldapServer2.getId());
    serverPriorityList.add("incorrectServerId");

    assertThatExceptionOfType(NotFoundException.class).isThrownBy(() -> dao.updatePriority(serverPriorityList))
        .withMessageContaining("LdapServer with ID incorrectServerId does not exist.");
  }

  @Test
  public void testDeleteCascadesToLdapConnection() {
    LdapServer ldapServer1 = tempEntity.newLdapServer("test1");
    LdapConnection ldapConnection1 = tempEntity.newLdapConnection(ldapServer1.getId());
    LdapServer ldapServer2 = tempEntity.newLdapServer("test2");
    LdapConnection ldapConnection2 = tempEntity.newLdapConnection(ldapServer2.getId());

    dao.delete(ldapServer1);

    assertThat(ldapConnectionDAO.getById(ldapConnection1.getId())).isNull();
    assertThat(ldapConnectionDAO.getById(ldapConnection2.getId())).isNotNull();
  }

  @Test
  public void testDeleteCascadesToLdapUserMapping() {
    LdapServer ldapServer1 = tempEntity.newLdapServer("test1");
    LdapUserMapping ldapUserMapping1 = tempEntity.newLdapUserMapping(ldapServer1.getId());
    LdapServer ldapServer2 = tempEntity.newLdapServer("test2");
    LdapUserMapping ldapUserMapping2 = tempEntity.newLdapUserMapping(ldapServer2.getId());

    dao.delete(ldapServer1);

    assertThat(ldapUserMappingDAO.getById(ldapUserMapping1.getId())).isNull();
    assertThat(ldapUserMappingDAO.getById(ldapUserMapping2.getId())).isNotNull();
  }

  @Test
  public void testDeleteCascadesToUserToken() {
    LdapServer ldapServer = tempEntity.newLdapServer("test");
    UserToken userToken1 = tempEntity.newUserToken("JohnDoe", ldapServer.getId());
    UserToken userToken2 = tempEntity.newUserToken("JaneDoe", "OtherRealmId");

    dao.delete(ldapServer);

    assertThat(userTokenDAO.getById(userToken1.getId())).isNull();
    assertThat(userTokenDAO.getById(userToken2.getId())).isNotNull();
  }

  @Test
  public void testDeleteCascadesToDashboardFilter() {
    LdapServer ldapServer = tempEntity.newLdapServer("test");
    DashboardFilter dashboardFilter1 =
        tempEntity.newDashboardFilter("testUsername", ldapServer.getId(), "testFilterName", "testFilter");
    DashboardFilter dashboardFilter2 =
        tempEntity.newDashboardFilter("testUsername", "OtherRealmId", "testFilterName", "testFilter");

    dao.delete(ldapServer);

    assertThat(dashboardFilterDAO.getById(dashboardFilter1.getId())).isNull();
    assertThat(dashboardFilterDAO.getById(dashboardFilter2.getId())).isNotNull();
  }

  @Test
  public void testDeleteCascadesToUserFilter() {
    LdapServer ldapServer = tempEntity.newLdapServer("test");
    UserFilter userFilter1 = tempEntity.newUserFilter("testUsername", ldapServer.getId(), "testFilterName",
        UserFilterType.ADVANCED_LEGAL_PACK_DASHBOARD, "testFilter");
    UserFilter userFilter2 = tempEntity.newUserFilter("testUsername", "OtherRealmId", "testFilterName",
        UserFilterType.ADVANCED_LEGAL_PACK_DASHBOARD, "testFilter");

    dao.delete(ldapServer);

    assertThat(userFilterDAO.getById(userFilter1.getId())).isNull();
    assertThat(userFilterDAO.getById(userFilter2.getId())).isNotNull();
  }

  @Test
  public void testDeleteCascadesToUserViewedProductNotification() {
    LdapServer ldapServer = tempEntity.newLdapServer("test");
    UserViewedProductNotification userViewedProductNotification1 =
        tempEntity.newUserViewedProductNotification("testUsername", ldapServer.getId(), "testNotificationId");
    UserViewedProductNotification userViewedProductNotification2 =
        tempEntity.newUserViewedProductNotification("testUsername", "OtherRealmId", "testNotificationId");

    dao.delete(ldapServer);

    assertThat(userViewedProductNotificationDAO.getById(userViewedProductNotification1.getId())).isNull();
    assertThat(userViewedProductNotificationDAO.getById(userViewedProductNotification2.getId())).isNotNull();
  }
}
