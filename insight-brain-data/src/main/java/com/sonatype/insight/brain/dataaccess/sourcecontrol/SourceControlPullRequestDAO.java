/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.sourcecontrol;

import java.util.Arrays;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.sonatype.insight.brain.dataaccess.AbstractOperationalSqlDAO;
import com.sonatype.insight.brain.db.datastore.OperationalDataStore;
import com.sonatype.insight.brain.model.sourcecontrol.PullRequestSource;
import com.sonatype.insight.brain.model.sourcecontrol.PullRequestState;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControl;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControlPullRequest;
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.nexus.git.utils.GitBranchNameValidator;
import com.sonatype.nexus.git.utils.InvalidBranchNameException;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import org.jooq.Record;
import org.jooq.Table;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.sonatype.insight.brain.jooq.generated.ods.tables.SourceControlPullRequest.SOURCE_CONTROL_PULL_REQUEST;

@Named
@Singleton
public class SourceControlPullRequestDAO
    extends AbstractOperationalSqlDAO<SourceControlPullRequest>
{
  private static final Logger log = LoggerFactory.getLogger(SourceControlPullRequestDAO.class);

  @Inject
  public SourceControlPullRequestDAO(OperationalDataStore operationalDataStore) {
    super(operationalDataStore);
  }

  @Override
  public List<SourceControlPullRequest> getAll() {
    try (TransactionContext tx = createTransactionContext()) {
      return tx.dsl()
          .selectFrom(SOURCE_CONTROL_PULL_REQUEST)
          .orderBy(SOURCE_CONTROL_PULL_REQUEST.REPOSITORY_URL, SOURCE_CONTROL_PULL_REQUEST.PULL_REQUEST_ID)
          .fetch(this::toEntity);
    }
  }

  public List<SourceControlPullRequest> getBySources(PullRequestSource... sources) {
    try (TransactionContext tx = createTransactionContext()) {
      return getBySources(tx, sources);
    }
  }

  public List<SourceControlPullRequest> getBySources(TransactionContext tx, PullRequestSource... sources) {
    if (sources.length == 0) {
      return List.of();
    }
    Set<PullRequestSource> param = new HashSet<>(Arrays.asList(sources));
    Set<String> sourceNames = param.stream().map(Enum::name).collect(java.util.stream.Collectors.toSet());

    // Note that SourceControlPullRequest prior to SDEV-1952 were always external have this set to null
    var condition = SOURCE_CONTROL_PULL_REQUEST.SOURCE.in(sourceNames);
    if (param.contains(PullRequestSource.EXTERNAL)) {
      condition = condition.or(SOURCE_CONTROL_PULL_REQUEST.SOURCE.isNull());
    }

    return tx.dsl()
        .selectFrom(SOURCE_CONTROL_PULL_REQUEST)
        .where(condition)
        .orderBy(SOURCE_CONTROL_PULL_REQUEST.REPOSITORY_URL, SOURCE_CONTROL_PULL_REQUEST.PULL_REQUEST_ID)
        .fetch(this::toEntity);
  }

  void deleteByRepositoryUrl(TransactionContext tx, String repositoryUrl) {
    repositoryUrl = SourceControl.normalizeRepositoryUrl(repositoryUrl).trim();
    getByRepositoryUrl(tx, repositoryUrl).forEach(entity -> delete(tx, entity));
  }

  private List<SourceControlPullRequest> getByRepositoryUrl(TransactionContext tx, String repositoryUrl) {
    repositoryUrl = SourceControl.normalizeRepositoryUrl(repositoryUrl).trim();
    return tx.dsl()
        .selectFrom(SOURCE_CONTROL_PULL_REQUEST)
        .where(SOURCE_CONTROL_PULL_REQUEST.REPOSITORY_URL.eq(repositoryUrl))
        .fetch(this::toEntity);
  }

  /**
   * Returns the number of known externally created PRs for which the last detected update time falls in the given date
   * range. At least the start or the end of the range must be specified i.e. not {@code null}.
   *
   * @param startDate start of the date range; can be {@code null}, in which case the range has no left boundary
   * @param endDate end of the date range; can be {@code null}, in which case the range has no right boundary
   */
  public int getExternalCountByUpdateTimeRange(Date startDate, Date endDate) {
    if (startDate == null && endDate == null) {
      throw new IllegalArgumentException("Either startDate or endDate must not be null.");
    }

    try (TransactionContext tx = createTransactionContext()) {
      var baseCondition = SOURCE_CONTROL_PULL_REQUEST.SOURCE.isNull()
          .or(SOURCE_CONTROL_PULL_REQUEST.SOURCE.eq(PullRequestSource.EXTERNAL.name()));

      var condition = baseCondition;
      if (startDate != null) {
        condition = condition.and(SOURCE_CONTROL_PULL_REQUEST.LAST_DETECTED_UPDATE_TIME.ge(startDate));
      }
      if (endDate != null) {
        condition = condition.and(SOURCE_CONTROL_PULL_REQUEST.LAST_DETECTED_UPDATE_TIME.lt(endDate));
      }

      return tx.dsl()
          .selectCount()
          .from(SOURCE_CONTROL_PULL_REQUEST)
          .where(condition)
          .fetchOne(0, Integer.class);
    }
  }

  /**
   * @param startDate the date from which to start looking for PRs created by IQ Manual PR and Auto PR features. Can be
   *          {@code null}, in which case the date range has no left boundary.
   * @return a list of PRs created by IQ Manual PR and Auto PR features since the specified date
   */
  public List<SourceControlPullRequest> getInternalCreatedSince(Date startDate) {
    Set<String> internalSources = EnumSet.of(
        PullRequestSource.AUTOMATIC,
        PullRequestSource.AUTOMATIC_INNER_SOURCE,
        PullRequestSource.MANUAL,
        PullRequestSource.MANUAL_INNER_SOURCE).stream().map(Enum::name).collect(java.util.stream.Collectors.toSet());

    try (TransactionContext tx = createTransactionContext()) {
      var condition = SOURCE_CONTROL_PULL_REQUEST.SOURCE.in(internalSources);
      if (startDate != null) {
        condition = condition.and(SOURCE_CONTROL_PULL_REQUEST.CREATE_TIME.ge(startDate));
      }

      return tx.dsl()
          .selectFrom(SOURCE_CONTROL_PULL_REQUEST)
          .where(condition)
          .fetch(this::toEntity);
    }
  }

  @Override
  public void insert(TransactionContext tx, SourceControlPullRequest entity) {
    validateBranchName(entity.getBranchName());
    validateBranchName(entity.getBaseBranchName());
    super.insert(tx, entity);
    log.trace("Inserted SourceControlPullRequest: " + entity);
  }

  @Override
  public void update(TransactionContext tx, SourceControlPullRequest entity) {
    validateBranchName(entity.getBranchName());
    validateBranchName(entity.getBaseBranchName());
    super.update(tx, entity);
    log.trace("Updated SourceControlPullRequest: " + entity);
  }

  public SourceControlPullRequest getByRepositoryUrlAndPullRequestId(String repositoryUrl, int pullRequestId) {
    try (TransactionContext tx = createTransactionContext()) {
      return toEntity(tx.dsl()
          .selectFrom(SOURCE_CONTROL_PULL_REQUEST)
          .where(SOURCE_CONTROL_PULL_REQUEST.REPOSITORY_URL.eq(repositoryUrl))
          .and(SOURCE_CONTROL_PULL_REQUEST.PULL_REQUEST_ID.eq(pullRequestId))
          .limit(1)
          .fetchOne());
    }
  }

  /**
   * Returns the pull request with the given pull request ID and a repository URL matching that of the SourceControl for
   * the given application ID
   */
  public SourceControlPullRequest getByApplicationIdAndPullRequestId(String applicationId, int pullRequestId) {
    try (TransactionContext tx = createTransactionContext()) {
      Record record = tx.dsl()
          .select(SOURCE_CONTROL_PULL_REQUEST.asterisk())
          .from(SOURCE_CONTROL_PULL_REQUEST)
          .join(com.sonatype.insight.brain.jooq.generated.ods.tables.SourceControl.SOURCE_CONTROL)
          .on(SOURCE_CONTROL_PULL_REQUEST.REPOSITORY_URL.eq(
              com.sonatype.insight.brain.jooq.generated.ods.tables.SourceControl.SOURCE_CONTROL.NORMALIZED_REPOSITORY_URL))
          .where(com.sonatype.insight.brain.jooq.generated.ods.tables.SourceControl.SOURCE_CONTROL.OWNER_ID
              .eq(applicationId))
          .and(SOURCE_CONTROL_PULL_REQUEST.PULL_REQUEST_ID.eq(pullRequestId))
          .limit(1)
          .fetchOne();
      return record != null ? toEntity(record.into(SOURCE_CONTROL_PULL_REQUEST)) : null;
    }
  }

  public List<SourceControlPullRequest> getByStatesAndSources(
      Set<PullRequestState> states,
      Set<PullRequestSource> sources)
  {
    Set<String> stateNames = states.stream().map(Enum::name).collect(java.util.stream.Collectors.toSet());
    Set<String> sourceNames = sources.stream().map(Enum::name).collect(java.util.stream.Collectors.toSet());

    try (TransactionContext tx = createTransactionContext()) {
      return tx.dsl()
          .selectFrom(SOURCE_CONTROL_PULL_REQUEST)
          .where(SOURCE_CONTROL_PULL_REQUEST.STATE.in(stateNames))
          .and(SOURCE_CONTROL_PULL_REQUEST.SOURCE.in(sourceNames))
          .fetch(this::toEntity);
    }
  }

  private static void validateBranchName(String branchName) {
    if (branchName == null) {
      return;
    }

    try {
      GitBranchNameValidator.validate(branchName);
    }
    catch (InvalidBranchNameException e) {
      throw new BadRequestException(e.getMessage(), e);
    }
  }

  @Override
  public Table<?> getJooqTable() {
    return SOURCE_CONTROL_PULL_REQUEST;
  }

  @Override
  public Class<SourceControlPullRequest> getEntityClass() {
    return SourceControlPullRequest.class;
  }
}
