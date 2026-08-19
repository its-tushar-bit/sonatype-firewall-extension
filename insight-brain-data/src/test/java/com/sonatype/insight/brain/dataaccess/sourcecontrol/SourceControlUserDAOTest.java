/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.sourcecontrol;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import com.sonatype.insight.brain.dataaccess.AbstractDbDAOTest;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControlUser;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControlUserActivity;
import com.sonatype.insight.dataaccess.TransactionContext;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class SourceControlUserDAOTest
    extends AbstractDbDAOTest
{
  private SourceControlUserDAO sourceControlUserDAO;

  private SourceControlUserActivityDAO sourceControlUserActivityDAO;

  @BeforeEach
  @Override
  public void setup() {
    super.setup();
    sourceControlUserActivityDAO = daoFactory.crateSourceControlUserActivityDAO();
    sourceControlUserDAO = daoFactory.createSourceControlUserDAO();
  }

  @Test
  public void testGetByApplicationId() {
    Application nonQueriedApplication = tempEntity.newApplicationWithParent();

    createSourceControlUsers(10, nonQueriedApplication.getId());

    List<SourceControlUser> expectedRepoUserList = createSourceControlUsers(4, application.getId());
    List<SourceControlUser> storedRepoUserList = sourceControlUserDAO.getByApplicationId(application.getId());

    assertThat(storedRepoUserList.stream().map(SourceControlUser::getId).collect(Collectors.toList())).hasSize(4)
        .containsOnly(expectedRepoUserList.stream().map(SourceControlUser::getId).toArray(String[]::new));
  }

  @Test
  public void testGetUserIdByEmailFilteringByApplicationId() {
    Application nonQueriedApplication = tempEntity.newApplicationWithParent();

    createSourceControlUsers(10, nonQueriedApplication.getId());

    List<SourceControlUser> expectedRepoUserList = createSourceControlUsers(4, application.getId());
    Map<String, String> storedRepoUserList =
        sourceControlUserDAO.getUserIdByEmailFilteringByApplicationId(application.getId());

    assertThat(storedRepoUserList.keySet()).hasSize(4)
        .containsOnly(expectedRepoUserList.stream().map(SourceControlUser::getEmail).toArray(String[]::new));
    assertThat(storedRepoUserList.values()).hasSize(4)
        .containsOnly(expectedRepoUserList.stream().map(SourceControlUser::getId).toArray(String[]::new));
  }

  @Test
  public void testInsertAllIfNew_onlyNewUsers() {
    List<SourceControlUser> sourceControlUsers = createSourceControlUsers(3, application.getId());

    SourceControlUser newUser1 = new SourceControlUser(application.getId(), "newUser1@email.com");
    SourceControlUser newUser2 = new SourceControlUser(application.getId(), "newUser2@email.com");
    newUser1.setId(UUID.randomUUID().toString().replace("-", ""));
    newUser2.setId(UUID.randomUUID().toString().replace("-", ""));

    List<SourceControlUser> usersToInsert = Arrays.asList(newUser1, newUser2);
    executeInTransaction(tx -> sourceControlUserDAO.insertAllIfNew(tx, usersToInsert));

    // Existing users plus the two new users are expected to be returned
    sourceControlUsers.addAll(usersToInsert);

    List<SourceControlUser> storedRepoUserList = sourceControlUserDAO.getByApplicationId(application.getId());
    assertThat(storedRepoUserList.stream().map(SourceControlUser::getId).collect(Collectors.toList())).hasSize(5)
        .containsOnly(sourceControlUsers.stream().map(SourceControlUser::getId).toArray(String[]::new));
  }

  @Test
  public void testInsertAllIfNew_someUserExists_notFailAndIgnore() {
    List<SourceControlUser> sourceControlUsers = createSourceControlUsers(3, application.getId());

    SourceControlUser newUser1 = new SourceControlUser(application.getId(), "newUser1@email.com");
    SourceControlUser existingUser = new SourceControlUser(application.getId(), sourceControlUsers.get(0).getEmail());
    newUser1.setId(UUID.randomUUID().toString().replace("-", ""));
    existingUser.setId(UUID.randomUUID().toString().replace("-", ""));

    List<SourceControlUser> usersToInsert = Arrays.asList(newUser1, existingUser);
    executeInTransaction(tx -> sourceControlUserDAO.insertAllIfNew(tx, usersToInsert));

    // Only the first user is expected to be inserted. The second user should keep the same id it already had in the DB
    sourceControlUsers.add(newUser1);

    List<SourceControlUser> storedRepoUserList = sourceControlUserDAO.getByApplicationId(application.getId());
    assertThat(storedRepoUserList.stream().map(SourceControlUser::getId).collect(Collectors.toList())).hasSize(4)
        .containsOnly(sourceControlUsers.stream().map(SourceControlUser::getId).toArray(String[]::new));
  }

  @Test
  public void testDelete_CascadeToSourceControlUserActivity() {
    SourceControlUser sourceControlUser = createSourceControlUser("user@email.com", application.getId());
    LocalDate commitDate1 = YearMonth.now().minus(1, ChronoUnit.MONTHS).atDay(2);
    LocalDate commitDate2 = YearMonth.now().minus(2, ChronoUnit.MONTHS).atDay(2);
    LocalDate commitDate3 = YearMonth.now().minus(3, ChronoUnit.MONTHS).atDay(2);
    SourceControlUserActivity newActivity1 = new SourceControlUserActivity(sourceControlUser.getId(), commitDate1);
    SourceControlUserActivity newActivity2 = new SourceControlUserActivity(sourceControlUser.getId(), commitDate2);
    SourceControlUserActivity newActivity3 = new SourceControlUserActivity(sourceControlUser.getId(), commitDate3);
    sourceControlUserActivityDAO.insert(newActivity1);
    sourceControlUserActivityDAO.insert(newActivity2);
    sourceControlUserActivityDAO.insert(newActivity3);

    sourceControlUserDAO.delete(sourceControlUser);

    List<SourceControlUser> sourceControlUsers = sourceControlUserDAO.getAll();
    List<SourceControlUserActivity> sourceControlUserActivities = sourceControlUserActivityDAO.getAll();

    assertThat(sourceControlUsers).hasSize(0);
    assertThat(sourceControlUserActivities).hasSize(0);
  }

  private List<SourceControlUser> createSourceControlUsers(int numberOfUsers, String applicationId) {
    List<SourceControlUser> createdUsers = new ArrayList<>(numberOfUsers);
    for (int i = 0; i < numberOfUsers; i++) {
      final String email = "test" + i + "@email.com";
      createdUsers.add(createSourceControlUser(email, applicationId));
    }
    return createdUsers;
  }

  private SourceControlUser createSourceControlUser(String email, String applicationId) {
    SourceControlUser newUser = new SourceControlUser(applicationId, email);
    sourceControlUserDAO.insert(newUser);
    return newUser;
  }

  private void executeInTransaction(Consumer<TransactionContext> operationToExecuteInTransaction) {
    try (TransactionContext tx = sourceControlUserDAO.createTransactionContext()) {
      tx.begin();
      operationToExecuteInTransaction.accept(tx);
      tx.commit();
    }
  }
}
