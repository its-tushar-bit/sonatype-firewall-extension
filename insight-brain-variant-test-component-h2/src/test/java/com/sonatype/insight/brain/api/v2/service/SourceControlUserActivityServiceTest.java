/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.time.temporal.ChronoField;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;
import jakarta.inject.Inject;

import com.sonatype.insight.brain.dataaccess.sourcecontrol.SourceControlUserActivityDAO;
import com.sonatype.insight.brain.dataaccess.sourcecontrol.SourceControlUserDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControlUser;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControlUserActivity;
import com.sonatype.insight.brain.variant.AbstractComponentH2Test;
import com.sonatype.insight.brain.variant.ComponentH2Test;
import com.sonatype.insight.test.LogOutput;

import org.junit.Rule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.sonatype.insight.brain.api.v2.service.SourceControlUserActivityService.truncateDayAndTime;
import static java.time.temporal.ChronoUnit.DAYS;
import static java.time.temporal.ChronoUnit.MILLIS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;

@ComponentH2Test
public class SourceControlUserActivityServiceTest
    extends AbstractComponentH2Test
{
  @Inject
  private SourceControlUserActivityDAO sourceControlUserActivityDAO;

  @Inject
  private SourceControlUserDAO sourceControlUserDAO;

  @Inject
  private SourceControlUserActivityService sourceControlUserActivityService;

  @Rule
  public LogOutput logOutput = new LogOutput(SourceControlUserActivityService.class);

  private final DefaultDBTestData testData = new DefaultDBTestData();

  @BeforeEach
  public void setupTest() {
    testData.createDefaultTestData();
  }

  @Test
  public void testSaveRepoUserList_applicationDoesNotExist_logsAttempt() {
    Map<String, Collection<Instant>> activityToSave = new HashMap<>();
    activityToSave.put("key@email.com", Collections.singletonList(Instant.now()));
    sourceControlUserActivityService.saveRepoUserList("nonexistentApplicationId", activityToSave);

    assertThat(logOutput).atDebugLevel()
        .contains("User does not have permissions to query application nonexistentApplicationId or it does not exists");
  }

  @Test
  public void testSaveRepoUserList_noRecordsToSave() {
    Map<String, Collection<Instant>> activityToSave = new HashMap<>();
    sourceControlUserActivityService.saveRepoUserList(testData.application.getPublicId(), activityToSave);

    assertThat(
        sourceControlUserDAO.getAll().stream().map(SourceControlUser::getEmail).collect(Collectors.toList())).hasSize(1)
            .containsOnly(testData.defaultUser.getEmail());
    assertThat(
        sourceControlUserActivityDAO.getAll()
            .stream()
            .map(SourceControlUserActivity::getSourceControlUserId)
            .collect(Collectors.toList())).hasSize(2)
                .containsOnly(testData.commitActivity.stream()
                    .map(SourceControlUserActivity::getSourceControlUserId)
                    .toArray(String[]::new));
  }

  @Test
  public void testSaveRepoUserList_newActivityForNewUser() {
    final String expectedNewEmail = "newUser@email.com";
    final LocalDate expectedNewCommitActivity = YearMonth.of(2023, 10).atDay(2);

    Map<String, Collection<Instant>> activityToSave = new HashMap<>();
    activityToSave.put(expectedNewEmail, Collections.singletonList(expectedNewCommitActivity.atTime(10, 5, 56)
        .toInstant(ZoneOffset.UTC)));

    sourceControlUserActivityService.saveRepoUserList(testData.application.getPublicId(), activityToSave);

    assertThat(getEmailsFromUsers(sourceControlUserDAO.getAll())).hasSize(2)
        .containsOnly(testData.defaultUser.getEmail(), expectedNewEmail);

    SourceControlUser newUserAsSourceControlUser = getSourceControlUserByEmail(expectedNewEmail);
    List<String> expectedUserIdsWithCommitMillis = getActivitiesAsUserIdWithCommitMillis(testData.commitActivity);
    expectedUserIdsWithCommitMillis.add(newUserAsSourceControlUser.getId() + expectedNewCommitActivity);

    assertThat(getActivitiesAsUserIdWithCommitMillis(sourceControlUserActivityDAO.getAll()))
        .hasSize(3)
        .containsOnly(expectedUserIdsWithCommitMillis.toArray(new String[0]));
  }

  @Test
  public void testSaveRepoUserList_newActivityForExistingUser() {
    final LocalDate expectedNewCommitActivity = YearMonth.of(2023, 5).atDay(2);

    Map<String, Collection<Instant>> activityToSave = new HashMap<>();
    activityToSave.put(testData.defaultUser.getEmail(), Collections.singletonList(expectedNewCommitActivity
        .atTime(0, 0, 0)
        .toInstant(ZoneOffset.UTC)));
    sourceControlUserActivityService.saveRepoUserList(testData.application.getPublicId(), activityToSave);

    assertThat(getEmailsFromUsers(sourceControlUserDAO.getAll())).hasSize(1)
        .containsOnly(testData.defaultUser.getEmail());

    List<String> expectedUserIdsWithCommitMillis = getActivitiesAsUserIdWithCommitMillis(testData.commitActivity);
    expectedUserIdsWithCommitMillis.add(testData.defaultUser.getId() + expectedNewCommitActivity);

    assertThat(getActivitiesAsUserIdWithCommitMillis(sourceControlUserActivityDAO.getAll()))
        .hasSize(3)
        .containsOnly(expectedUserIdsWithCommitMillis.toArray(new String[0]));
  }

  @Test
  public void testSaveRepoUserList_existingActivityForExistingUser() {
    final Instant expectedNewCommitActivity = testData.commitActivity.get(0)
        .getCommitYearMonth()
        .atTime(0, 0, 0)
        .toInstant(ZoneOffset.UTC);

    Map<String, Collection<Instant>> activityToSave = new HashMap<>();
    activityToSave.put(testData.defaultUser.getEmail(), Collections.singletonList(expectedNewCommitActivity));
    sourceControlUserActivityService.saveRepoUserList(testData.application.getPublicId(), activityToSave);

    assertThat(getEmailsFromUsers(sourceControlUserDAO.getAll())).hasSize(1)
        .containsOnly(testData.defaultUser.getEmail());

    List<String> expectedUserIdsWithCommitMillis = getActivitiesAsUserIdWithCommitMillis(testData.commitActivity);

    assertThat(getActivitiesAsUserIdWithCommitMillis(sourceControlUserActivityDAO.getAll()))
        .hasSize(2)
        .containsOnly(expectedUserIdsWithCommitMillis.toArray(new String[0]));
  }

  @Test
  public void testTruncateDayAndTime_randomDateToMonthBeginning() {
    Instant randomDate = Instant.ofEpochMilli(new Random().nextInt() * 1000L).plus(new Random().nextInt(), MILLIS);
    LocalDate randomLocalDateTime = randomDate.atZone(ZoneOffset.UTC).toLocalDate();

    LocalDate result = truncateDayAndTime(randomDate);
    assertEquals(result.get(ChronoField.YEAR), randomLocalDateTime.get(ChronoField.YEAR));
    assertEquals(result.get(ChronoField.MONTH_OF_YEAR), randomLocalDateTime.get(ChronoField.MONTH_OF_YEAR));
    assertEquals(result.get(ChronoField.DAY_OF_MONTH), 2);
  }

  private SourceControlUser getSourceControlUserByEmail(final String emailToFind) {
    return sourceControlUserDAO.getAll()
        .stream()
        .filter(sourceControlUser -> sourceControlUser.getEmail().equals(emailToFind))
        .findFirst()
        .get();
  }

  private List<String> getEmailsFromUsers(List<SourceControlUser> sourceControlUsers) {
    return sourceControlUsers.stream().map(SourceControlUser::getEmail).collect(Collectors.toList());
  }

  private List<String> getActivitiesAsUserIdWithCommitMillis(List<SourceControlUserActivity> commitActivity) {
    return commitActivity.stream()
        .map(sourceControlUserActivity -> sourceControlUserActivity.getSourceControlUserId() +
            sourceControlUserActivity.getCommitYearMonth())
        .collect(Collectors.toList());
  }

  private class DefaultDBTestData
  {
    private Application application;

    private SourceControlUser defaultUser;

    private final List<SourceControlUserActivity> commitActivity = new ArrayList<>();

    private void createDefaultTestData() {
      application = tempEntity.newApplicationWithParent();
      defaultUser = new SourceControlUser(application.getId(), "defaultUser@email.com");
      sourceControlUserDAO.insert(defaultUser);

      SourceControlUserActivity commitActivity1 =
          new SourceControlUserActivity(defaultUser.getId(), truncateDayAndTime(Instant.now()));
      SourceControlUserActivity commitActivity2 =
          new SourceControlUserActivity(defaultUser.getId(),
              truncateDayAndTime(Instant.now().minus(60, DAYS)));
      sourceControlUserActivityDAO.insert(commitActivity1);
      sourceControlUserActivityDAO.insert(commitActivity2);

      commitActivity.add(commitActivity1);
      commitActivity.add(commitActivity2);
    }
  }
}
