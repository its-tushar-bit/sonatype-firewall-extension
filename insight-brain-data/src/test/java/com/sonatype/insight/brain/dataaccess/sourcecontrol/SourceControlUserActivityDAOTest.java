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
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import com.sonatype.insight.brain.dataaccess.AbstractDbDAOTest;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControlUser;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControlUserActivity;
import com.sonatype.insight.dataaccess.TransactionContext;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class SourceControlUserActivityDAOTest
    extends AbstractDbDAOTest
{
  private SourceControlUserActivityDAO sourceControlUserActivityDAO;

  private SourceControlUserDAO sourceControlUserDAO;

  private SourceControlUser mrBond;

  private SourceControlUser mrJames;

  @BeforeEach
  @Override
  public void setup() {
    super.setup();
    sourceControlUserActivityDAO = daoFactory.crateSourceControlUserActivityDAO();
    sourceControlUserDAO = daoFactory.createSourceControlUserDAO();

    mrBond = new SourceControlUser(application.getId(), "mrBond@email.com");
    sourceControlUserDAO.insert(mrBond);
    mrJames = new SourceControlUser(application.getId(), "mrJames@email.com");
    sourceControlUserDAO.insert(mrJames);
  }

  @Test
  public void testInsertAllIfNew_onlyNewActivities() {
    List<SourceControlUserActivity> sourceControlUserActivities = createSourceControlUserActivities(3, mrBond.getId());

    SourceControlUserActivity newActivity1 = new SourceControlUserActivity(mrJames.getId(), YearMonth.now().atDay(2));
    SourceControlUserActivity newActivity2 =
        new SourceControlUserActivity(mrJames.getId(), YearMonth.now().atDay(3));
    newActivity1.setId(UUID.randomUUID().toString().replace("-", ""));
    newActivity2.setId(UUID.randomUUID().toString().replace("-", ""));

    List<SourceControlUserActivity> activitiesToInsert = Arrays.asList(newActivity1, newActivity2);
    executeInTransaction(tx -> sourceControlUserActivityDAO.insertAllIfNew(tx, activitiesToInsert));

    // Existing activities plus the two new activities are expected to be returned
    sourceControlUserActivities.addAll(activitiesToInsert);

    List<SourceControlUserActivity> storedActivities = sourceControlUserActivityDAO.getAll();
    assertThat(storedActivities.stream()
        .map(SourceControlUserActivity::getId)
        .collect(Collectors.toList()))
            .hasSize(5)
            .containsOnly(
                sourceControlUserActivities.stream()
                    .map(SourceControlUserActivity::getId)
                    .toArray(String[]::new));
  }

  @Test
  public void testInsertAllIfNew_someActivityExists_notFailAndIgnore() {
    List<SourceControlUserActivity> sourceControlUserActivities = createSourceControlUserActivities(6, mrBond.getId());

    SourceControlUserActivity newActivity1 = new SourceControlUserActivity(mrJames.getId(),
        LocalDate.now());
    SourceControlUserActivity existingActivity =
        new SourceControlUserActivity(mrBond.getId(), sourceControlUserActivities.get(0).getCommitYearMonth());

    List<SourceControlUserActivity> activitiesToInsert = Arrays.asList(newActivity1, existingActivity);
    executeInTransaction(tx -> sourceControlUserActivityDAO.insertAllIfNew(tx, activitiesToInsert));

    // Only the first activity is expected to be inserted.
    // The second one should be already in the db
    sourceControlUserActivities.add(newActivity1);

    List<SourceControlUserActivity> storedActivities = sourceControlUserActivityDAO.getAll();

    // Ids might have been modified, so we must assert the actual data
    assertThat(getActivitiesAsUserIdWithCommitMillis(storedActivities))
        .hasSize(7)
        .containsOnly(getActivitiesAsUserIdWithCommitMillis(sourceControlUserActivities).toArray(new String[0]));
  }

  private List<SourceControlUserActivity> createSourceControlUserActivities(
      int numberOfActivities,
      String userId)
  {
    List<SourceControlUserActivity> createdUserActivities = new ArrayList<>(numberOfActivities);
    for (int i = 0; i < numberOfActivities; i++) {
      LocalDate commitDate = YearMonth.now().minus(i, ChronoUnit.MONTHS).atDay(2);
      SourceControlUserActivity newActivity = new SourceControlUserActivity(userId, commitDate);
      sourceControlUserActivityDAO.insert(newActivity);
      createdUserActivities.add(newActivity);
    }
    return createdUserActivities;
  }

  private List<String> getActivitiesAsUserIdWithCommitMillis(List<SourceControlUserActivity> commitActivity) {
    return commitActivity.stream()
        .map(sourceControlUserActivity -> sourceControlUserActivity.getSourceControlUserId() +
            sourceControlUserActivity.getCommitYearMonth())
        .collect(Collectors.toList());
  }

  private void executeInTransaction(Consumer<TransactionContext> operationToExecuteInTransaction) {
    try (TransactionContext tx = sourceControlUserActivityDAO.createTransactionContext()) {
      tx.begin();
      operationToExecuteInTransaction.accept(tx);
      tx.commit();
    }
  }

  @Test
  public void testGetActivitiesNotSentToTelemetry() {
    SourceControlUserActivity newActivity1 =
        new SourceControlUserActivity(mrJames.getId(), LocalDate.now().minusMonths(2));
    SourceControlUserActivity newActivity2 =
        new SourceControlUserActivity(mrBond.getId(), LocalDate.now().minusMonths(3));

    sourceControlUserActivityDAO.insert(newActivity1);
    sourceControlUserActivityDAO.insert(newActivity2);

    List<SourceControlUserActivityTelemetryDTO> activitiesNotSentToTelemetry =
        sourceControlUserActivityDAO.getActivitiesNotSentToTelemetry()
            .stream()
            .sorted(Comparator.comparing(SourceControlUserActivityTelemetryDTO::getEmail))
            .collect(Collectors.toList());
    assertThat(activitiesNotSentToTelemetry).hasSize(2);
    assertThat(activitiesNotSentToTelemetry.get(0).getSourceControlUserActivityId()).isEqualTo(newActivity2.getId());
    assertThat(activitiesNotSentToTelemetry.get(0).getEmail()).isEqualTo(mrBond.getEmail());
    assertThat(activitiesNotSentToTelemetry.get(0).getApplicationId()).isEqualTo(mrBond.getApplicationId());
    assertThat(activitiesNotSentToTelemetry.get(0).getCommitYearMonth()).isEqualTo(newActivity2.getCommitYearMonth());
    assertThat(activitiesNotSentToTelemetry.get(1).getSourceControlUserActivityId()).isEqualTo(newActivity1.getId());
    assertThat(activitiesNotSentToTelemetry.get(1).getEmail()).isEqualTo(mrJames.getEmail());
    assertThat(activitiesNotSentToTelemetry.get(1).getApplicationId()).isEqualTo(mrJames.getApplicationId());
    assertThat(activitiesNotSentToTelemetry.get(1).getCommitYearMonth()).isEqualTo(newActivity1.getCommitYearMonth());
  }

  @Test
  public void testUpdateActivitiesSentToTelemetry() {
    SourceControlUserActivity newActivity1 = new SourceControlUserActivity(mrJames.getId(), LocalDate.now());
    SourceControlUserActivity newActivity2 =
        new SourceControlUserActivity(mrJames.getId(), LocalDate.now().minusMonths(1));
    newActivity1.setId(UUID.randomUUID().toString().replace("-", ""));
    newActivity2.setId(UUID.randomUUID().toString().replace("-", ""));

    sourceControlUserActivityDAO.insert(newActivity1);
    sourceControlUserActivityDAO.insert(newActivity2);

    Set<String> sourceControlUserActivitiesIds = new HashSet<>();
    sourceControlUserActivitiesIds.add(newActivity1.getId());
    sourceControlUserActivitiesIds.add(newActivity2.getId());
    int result = sourceControlUserActivityDAO.updateActivitiesSentToTelemetry(sourceControlUserActivitiesIds);
    assertThat(result).isEqualTo(2);
    assertThat(sourceControlUserActivityDAO.getById(newActivity1.getId()).isSentToTelemetry()).isTrue();
    assertThat(sourceControlUserActivityDAO.getById(newActivity2.getId()).isSentToTelemetry()).isTrue();
  }
}
