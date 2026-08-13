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
import com.sonatype.insight.brain.model.security.OAuth2User;

import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * PostgreSQL-backed tests relocated from {@link OAuth2UserDAOTest} (CLM-45228).
 */
@PostgresTest
public class OAuth2UserDAOPgTest
    extends AbstractDbDAOTest
{
  private OAuth2UserDAO oAuth2UserDAO;

  private UserTokenDAO userTokenDAO;

  private DashboardFilterDAO dashboardFilterDAO;

  private UserFilterDAO userFilterDAO;

  private UserViewedProductNotificationDAO userViewedProductNotificationDAO;

  private OAuth2UserGroupDAO oAuth2UserGroupDAO;

  @Before
  @Override
  public void setup() {
    super.setup();
    oAuth2UserDAO = daoFactory.createOAuth2UserDAO();
    userTokenDAO = daoFactory.createUserTokenDAO();
    dashboardFilterDAO = daoFactory.createDashboardFilterDAO();
    userFilterDAO = daoFactory.createUserFilterDAO();
    userViewedProductNotificationDAO = daoFactory.createUserViewedProductNotificationDAO();
    oAuth2UserGroupDAO = daoFactory.createOAuth2UserGroupDAO();
  }

  @Test
  public void testFindUsersByNameOrUsernameQuery_PrefixAndSuffixUserName_postgres() {
    OAuth2User oAuth2User1 = tempEntity.newOAuth2User("userA-postgres-1", "johnny", "smith", null, null);
    OAuth2User oAuth2User2 = tempEntity.newOAuth2User("userB-postgres-2", "bobby", "smithson", null, null);
    tempEntity.newOAuth2User("other", "john", "doe", null, null);

    assertThat(oAuth2UserDAO.findUsersByNameOrUsernameQuery("%-PoStgREs%"))
        .usingRecursiveFieldByFieldElementComparator()
        .containsExactly(oAuth2User1, oAuth2User2);
  }

  private void assertOauth2User(OAuth2User expectedOAuth2User, List<OAuth2User> users) {
    OAuth2User foundUser = users.stream()
        .filter(oAuth2User -> expectedOAuth2User.getUsername().equals(oAuth2User.getUsername()))
        .findFirst()
        .orElse(null);
    assertThat(foundUser).isNotNull()
        .usingRecursiveComparison()
        .ignoringFields(JPA.IGNORE_FIELDS)
        .isEqualTo(expectedOAuth2User);
  }

  private void assertOAuth2UserWithGroups(
      OAuth2User expectedSamlUser,
      OAuth2User foundUser,
      Set<String> expectedGroups)
  {
    assertThat(foundUser).isNotNull()
        .usingRecursiveComparison()
        .ignoringFields(JPA.IGNORE_FIELDS)
        .ignoringFields("groupsJson")
        .isEqualTo(expectedSamlUser);

    assertThat(foundUser.getGroups()).containsAll(expectedGroups);
  }

  private OAuth2User createOauth2User() {
    return new OAuth2User("someUsername", "someFirstName", "someLastName", "someEmail@someDomain.com",
        new LinkedHashSet<>(Arrays.asList("someGroup1", "someGroup2")));
  }
}
