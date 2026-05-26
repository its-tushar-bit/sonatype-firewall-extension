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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.Collection;
import java.util.Map.Entry;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.sourcecontrol.SourceControlUserActivityDAO;
import com.sonatype.insight.brain.dataaccess.sourcecontrol.SourceControlUserDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControlUser;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControlUserActivity;
import com.sonatype.insight.brain.security.Authorize;
import com.sonatype.insight.brain.security.AuthzContext;
import com.sonatype.insight.brain.security.AuthzContext.Key;
import com.sonatype.insight.dataaccess.TransactionContext;

import com.google.common.annotations.VisibleForTesting;
import datadog.trace.api.Trace;
import org.apache.commons.collections4.MapUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
@Singleton
public class SourceControlUserActivityService
{
  private static final Logger log = LoggerFactory.getLogger(SourceControlUserActivityService.class);

  private final ApplicationDAO applicationDAO;

  private final SourceControlUserDAO sourceControlUserDAO;

  private final SourceControlUserActivityDAO sourceControlUserActivityDAO;

  @Inject
  public SourceControlUserActivityService(
      final ApplicationDAO applicationDAO,
      final SourceControlUserDAO sourceControlUserDAO,
      final SourceControlUserActivityDAO sourceControlUserActivityDAO)
  {
    this.applicationDAO = applicationDAO;
    this.sourceControlUserDAO = sourceControlUserDAO;
    this.sourceControlUserActivityDAO = sourceControlUserActivityDAO;
  }

  @Trace
  @Authorize(permission = Permission.EVALUATE_APPLICATION)
  void saveRepoUserList(
      @AuthzContext(Key.APPLICATION_PUBLIC_ID) final String publicApplicationId,
      final Map<String, Collection<Instant>> emailAndCommitDateMap)
  {
    final String internalApplicationId =
        MapUtils.isNotEmpty(emailAndCommitDateMap) ? getInternalApplicationId(publicApplicationId) : null;
    if (internalApplicationId == null) {
      return;
    }

    Map<String, String> sourceControlUserIdByEmail =
        sourceControlUserDAO.getUserIdByEmailFilteringByApplicationId(internalApplicationId);

    final List<SourceControlUser> usersToInsert = new ArrayList<>();
    final SourceControlUserActivityCollection userActivities =
        new SourceControlUserActivityCollection(sourceControlUserIdByEmail);
    for (Entry<String, Collection<Instant>> dataPoint : emailAndCommitDateMap.entrySet()) {
      final String committerEmail = dataPoint.getKey();
      if (!sourceControlUserIdByEmail.containsKey(committerEmail)) {
        SourceControlUser newUser = getNewSourceControlUser(internalApplicationId, committerEmail);
        sourceControlUserIdByEmail.put(committerEmail, newUser.getId());
        usersToInsert.add(newUser);
      }

      for (Instant commitInstant : dataPoint.getValue()) {
        userActivities.addInstantForEmail(commitInstant, committerEmail);
      }
    }

    saveUsersAndActivities(usersToInsert, userActivities.getUserActivities());
  }

  private String getInternalApplicationId(final String publicApplicationId) {
    Application application = applicationDAO.getByPublicId(publicApplicationId);
    if (application == null) {
      log.debug("User does not have permissions to query application {} or it does not exists", publicApplicationId);
      return null;
    }
    return application.getId();
  }

  private static SourceControlUser getNewSourceControlUser(
      final String internalApplicationId,
      final String committerEmail)
  {
    SourceControlUser newUser = new SourceControlUser(internalApplicationId, committerEmail);
    // The id is calculated here in order to prepare it for massive insertion
    newUser.setId(getNewRandomUuid());
    return newUser;
  }

  private static String getNewRandomUuid() {
    // We perform "-" character replacement to keep it consistent with our other identifiers in the DB
    return UUID.randomUUID().toString().replace("-", "");
  }

  private void saveUsersAndActivities(
      final List<SourceControlUser> usersToInsert,
      final List<SourceControlUserActivity> userActivities)
  {
    try (TransactionContext tx = sourceControlUserDAO.createTransactionContext()) {
      tx.begin();
      sourceControlUserDAO.insertAllIfNew(tx, usersToInsert);
      sourceControlUserActivityDAO.insertAllIfNew(tx, userActivities);
      tx.commit();
    }
  }

  @VisibleForTesting
  static LocalDate truncateDayAndTime(Instant instant) {
    // Because of a bug in H2 we store the day at the second of the month to prevent any timezone automatic
    // conversion setting it to the previous month
    return YearMonth.from(instant.atZone(ZoneOffset.UTC)).atDay(2);
  }

  private static final class SourceControlUserActivityCollection
  {
    private final List<SourceControlUserActivity> userActivities = new ArrayList<>();

    private final Map<String, String> sourceControlUserIdByEmail;

    public SourceControlUserActivityCollection(final Map<String, String> sourceControlUserIdByEmail) {
      this.sourceControlUserIdByEmail = sourceControlUserIdByEmail;
    }

    private void addInstantForEmail(Instant commitInstant, String committerEmail) {
      String sourceControlUserId = sourceControlUserIdByEmail.get(committerEmail);
      if (sourceControlUserId == null) {
        log.debug("Could not find source control user Id for email {}", committerEmail);
        return;
      }

      SourceControlUserActivity sourceControlUserActivity = new SourceControlUserActivity();
      sourceControlUserActivity.setSourceControlUserId(sourceControlUserId);
      sourceControlUserActivity.setCommitYearMonth(truncateDayAndTime(commitInstant));
      userActivities.add(sourceControlUserActivity);
    }

    private List<SourceControlUserActivity> getUserActivities() {
      return userActivities;
    }
  }
}
