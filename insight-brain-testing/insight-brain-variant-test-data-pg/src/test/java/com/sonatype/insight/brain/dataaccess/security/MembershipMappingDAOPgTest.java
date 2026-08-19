/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.security;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import com.sonatype.insight.brain.dataaccess.AbstractDbDAOTest;
import com.sonatype.insight.brain.db.IdUtil;
import com.sonatype.insight.brain.db.rule.DatabaseRuleAnnotations.PostgresTest;
import com.sonatype.insight.brain.model.security.MemberType;
import com.sonatype.insight.brain.model.security.MembershipMapping;
import com.sonatype.insight.brain.model.security.Role;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * PostgreSQL-backed tests relocated from {@link MembershipMappingDAOTest} (CLM-45228).
 */
@PostgresTest
public class MembershipMappingDAOPgTest
    extends AbstractDbDAOTest
{
  private final String contextId = "some-app";

  Role roleDeveloper;

  private MembershipMappingDAO membershipDAO;

  private RoleDAO roleDAO;

  private static final Comparator<MembershipMapping> MEMBERSHIP_COMPARATOR =
      Comparator.comparing(MembershipMapping::getContextId)
          .thenComparing(MembershipMapping::getRoleId)
          .thenComparing(MembershipMapping::getMemberName)
          .thenComparing(MembershipMapping::getMemberType);

  @BeforeEach
  @Override
  public void setup() {
    super.setup();
    membershipDAO = daoFactory.createMembershipMappingDAO();
    roleDAO = daoFactory.createRoleDAO();
    roleDeveloper = roleDAO.getByName("Developer");
  }

  @AfterEach
  public void cleanup() {
    for (MembershipMapping membership : membershipDAO.getByContextId(contextId)) {
      membershipDAO.delete(membership);
    }
  }

  @Test
  public void testInsertAll_onlyNewMembershipsPostgres() throws SQLException {
    doInsertAll_onlyNewMemberships();
  }

  private void doInsertAll_onlyNewMemberships() throws SQLException {
    List<String> usernames = new ArrayList<>();
    usernames.add("user1");
    usernames.add("user2");
    usernames.add("user3");
    List<MembershipMapping> membershipMappings = createUserMembershipMappings(usernames);

    MembershipMapping newMembership1 = new MembershipMapping(application.getId(), roleDeveloper.getId(),
        "user4", MemberType.USER);
    MembershipMapping newMembership2 = new MembershipMapping(application.getId(), roleDeveloper.getId(),
        "user5", MemberType.USER);

    List<MembershipMapping> membershipsToInsert = Arrays.asList(newMembership1, newMembership2);
    membershipDAO.insertAll(membershipsToInsert);

    membershipMappings.addAll(membershipsToInsert);

    List<MembershipMapping> storedMemberships = membershipDAO
        .getByRoleIdsForTestsOnly(Collections.singleton(roleDeveloper.getId()));
    assertThat(storedMemberships.stream()
        .map(MembershipMapping::getMemberName)
        .collect(Collectors.toList()))
            .hasSize(5)
            .containsExactlyInAnyOrder(
                membershipMappings.stream()
                    .map(MembershipMapping::getMemberName)
                    .toArray(String[]::new));
  }

  @Test
  public void testInsertAll_someMembershipsExist_notFailAndIgnorePostgres() throws SQLException {
    doInsertAll_someMembershipsExist();
  }

  private void doInsertAll_someMembershipsExist() throws SQLException {
    List<String> usernames = new ArrayList<>();
    usernames.add("user1");
    usernames.add("user2");
    usernames.add("user3");
    usernames.add("user4");
    usernames.add("user5");
    List<MembershipMapping> membershipMappings = createUserMembershipMappings(usernames);
    membershipMappings.sort(Comparator.comparing(MembershipMapping::getMemberName));

    MembershipMapping newMembership1 = new MembershipMapping(application.getId(), roleDeveloper.getId(),
        "user6", MemberType.USER);
    MembershipMapping existingMembership1 = membershipMappings.get(0);
    MembershipMapping existingMembership2 = membershipMappings.get(1);
    existingMembership1.setId(IdUtil.newUUID());
    existingMembership2.setId(IdUtil.newUUID());

    List<MembershipMapping> membershipsToInsert = Arrays.asList(newMembership1, existingMembership1,
        existingMembership2);
    membershipDAO.insertAll(membershipsToInsert);

    membershipMappings.add(newMembership1);

    List<MembershipMapping> storedMemberships = membershipDAO
        .getByRoleIdsForTestsOnly(Collections.singleton(roleDeveloper.getId()));
    assertThat(storedMemberships.stream()
        .map(MembershipMapping::getMemberName)
        .collect(Collectors.toList()))
            .hasSize(6)
            .containsExactlyInAnyOrder(
                membershipMappings.stream()
                    .map(MembershipMapping::getMemberName)
                    .toArray(String[]::new));
  }

  private List<MembershipMapping> createUserMembershipMappings(List<String> usernames) {
    List<MembershipMapping> createdMembershipMappings = new ArrayList<>(usernames.size());
    for (String username : usernames) {
      MembershipMapping membershipMapping = new MembershipMapping(application.getId(), roleDeveloper.getId(),
          username, MemberType.USER);
      membershipDAO.insert(membershipMapping);
      createdMembershipMappings.add(membershipMapping);
    }
    return createdMembershipMappings;
  }
}
