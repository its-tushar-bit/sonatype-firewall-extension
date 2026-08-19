/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.security;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import com.sonatype.insight.brain.dataaccess.AbstractDbDAOTest;
import com.sonatype.insight.brain.dataaccess.JPA;
import com.sonatype.insight.brain.dataaccess.filter.DashboardFilterDAO;
import com.sonatype.insight.brain.dataaccess.filter.UserFilterDAO;
import com.sonatype.insight.brain.dataaccess.notification.UserViewedProductNotificationDAO;
import com.sonatype.insight.brain.db.rule.DatabaseRuleAnnotations.PostgresTest;
import com.sonatype.insight.brain.model.security.SamlUser;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * PostgreSQL-backed tests relocated from {@link SamlUserDAOTest} (CLM-45228).
 */
@PostgresTest
public class SamlUserDAOPgTest
    extends AbstractDbDAOTest
{
  private SamlUserDAO samlUserDAO;

  private UserTokenDAO userTokenDAO;

  private DashboardFilterDAO dashboardFilterDAO;

  private UserFilterDAO userFilterDAO;

  private UserViewedProductNotificationDAO userViewedProductNotificationDAO;

  private SamlUserGroupDAO samlUserGroupDAO;

  @BeforeEach
  @Override
  public void setup() {
    super.setup();
    samlUserDAO = daoFactory.createSamlUserDAO();
    userTokenDAO = daoFactory.createUserTokenDAO();
    dashboardFilterDAO = daoFactory.createDashboardFilterDAO();
    userFilterDAO = daoFactory.createUserFilterDAO();
    userViewedProductNotificationDAO = daoFactory.createUserViewedProductNotificationDAO();
    samlUserGroupDAO = daoFactory.createSamlUserGroupDAO();
  }

  @Test
  public void testFindUsersByNameOrUsernameQuery_PrefixAndSuffixUserName_postgres() {
    SamlUser samlUser1 = tempEntity.newSamlUser("userA-postgres-1", "johnny", "smith", null, null);
    SamlUser samlUser2 = tempEntity.newSamlUser("userB-postgres-2", "bobby", "smithson", null, null);
    tempEntity.newSamlUser("other", "john", "doe", null, null);

    assertThat(samlUserDAO.findUsersByNameOrUsernameQuery("%-PoStgREs%"))
        .usingRecursiveFieldByFieldElementComparator()
        .containsExactly(samlUser1, samlUser2);
  }

  private void assertSamlUser(SamlUser expectedSamlUser, List<SamlUser> users) {
    SamlUser foundUser = users.stream()
        .filter(samlUser -> expectedSamlUser.getUsername().equals(samlUser.getUsername()))
        .findFirst()
        .orElse(null);

    assertThat(foundUser).isNotNull()
        .usingRecursiveComparison()
        .ignoringFields(JPA.IGNORE_FIELDS)
        .isEqualTo(expectedSamlUser);
  }

  private void assertSamlUserWithGroups(SamlUser expectedSamlUser, SamlUser foundUser, Set<String> expectedGroups) {
    assertThat(foundUser).isNotNull()
        .usingRecursiveComparison()
        .ignoringFields(JPA.IGNORE_FIELDS)
        .ignoringFields("groupsString")
        .isEqualTo(expectedSamlUser);

    assertThat(foundUser.getGroups()).containsAll(expectedGroups);
  }

  private SamlUser createSamlUser() {
    return new SamlUser("someUsername", "someFirstName", "someLastName", "someEmail@someDomain.com",
        new LinkedHashSet<>(Arrays.asList("someGroup1", "someGroup2")));
  }
}
