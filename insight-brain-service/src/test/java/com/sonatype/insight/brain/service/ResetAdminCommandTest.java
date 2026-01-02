/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

import java.util.List;
import java.util.stream.Collectors;

import com.sonatype.insight.brain.AbstractDataTest;
import com.sonatype.insight.brain.common.test.SlowTest;
import com.sonatype.insight.brain.dataaccess.security.MembershipMappingDAO;
import com.sonatype.insight.brain.dataaccess.security.UserDAO;
import com.sonatype.insight.brain.db.rule.DatabaseRuleAnnotations.H2DiskTest;
import com.sonatype.insight.brain.db.datastore.OperationalDataStore;
import com.sonatype.insight.brain.model.security.MemberType;
import com.sonatype.insight.brain.model.security.MembershipMapping;
import com.sonatype.insight.brain.model.security.Role;
import com.sonatype.insight.brain.model.security.User;
import com.sonatype.insight.db.DatabaseConfig;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.TemporaryFolder;

import static org.assertj.core.api.Assertions.assertThat;

public class ResetAdminCommandTest
    extends AbstractDataTest
{
  @Rule
  public TemporaryFolder temporaryFolder = new TemporaryFolder();

  private InsightConfig insightConfig;

  private UserDAO userDAO;

  private MembershipMappingDAO membershipMappingDAO;

  @Before
  public void before() throws Exception {
    insightConfig = new InsightConfig();
    insightConfig.setSonatypeWork(temporaryFolder.newFolder().getAbsolutePath());
    userDAO = daoFactory.createUserDAO();
    membershipMappingDAO = daoFactory.createMembershipMappingDAO();
  }

  @Test
  @H2DiskTest
  @Category(SlowTest.class)
  public void testRun_AdminDoesNotExist() {
    userDAO.delete(getAdmin());

    runTest();

    assertAdmin();
  }

  @Test
  @H2DiskTest
  @Category(SlowTest.class)
  public void testRun_AdminExists() {
    User admin = getAdmin();
    admin.setPassword("password");
    userDAO.update(admin);
    membershipMappingDAO.getAll().forEach(membershipMappingDAO::delete);

    runTest();

    assertAdmin();
  }

  private void runTest() {
    new ResetAdminCommand()
    {
      // Use the provided OperationalDataStore from DatabaseRule
      @Override
      protected OperationalDataStore getOperationalDataStore(final DatabaseConfig databaseConfig) {
        return databaseRule.getOperationalDataStore();
      }
    }.run(null, null, insightConfig);
  }

  private User getAdmin() {
    return userDAO.getByUsername(User.ADMIN_USERNAME);
  }

  private List<String> getMembers(String roleId) {
    return membershipMappingDAO.getByContextIdAndRoleId(MembershipMapping.GLOBAL_CONTEXT_ID, roleId).stream()
        .filter(membershipMapping -> membershipMapping.getMemberType().equals(MemberType.USER))
        .map(MembershipMapping::getMemberName).collect(Collectors.toList());
  }

  private void assertAdmin() {
    User admin = getAdmin();
    assertThat(admin).isNotNull();
    assertThat(admin.getId()).isEqualTo(ResetAdminCommand.DEFAULT_ADMIN.getId());
    assertThat(admin.getUsername()).isEqualTo(ResetAdminCommand.DEFAULT_ADMIN.getUsername());
    assertThat(admin.getUsernameLowercase()).isEqualTo(ResetAdminCommand.DEFAULT_ADMIN.getUsernameLowercase());
    assertThat(admin.getPassword()).isEqualTo(ResetAdminCommand.DEFAULT_ADMIN.getPassword());
    assertThat(getMembers(Role.SYSTEM_ADMIN_ROLE_ID)).contains(User.ADMIN_USERNAME);
    assertThat(getMembers(Role.POLICY_ADMIN_ROLE_ID)).contains(User.ADMIN_USERNAME);
  }
}
