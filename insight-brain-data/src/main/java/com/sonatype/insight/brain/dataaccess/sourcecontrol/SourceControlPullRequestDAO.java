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
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    String sQuery = "SELECT entity FROM SourceControlPullRequest entity" + //
        " ORDER BY entity.repositoryUrl, entity.pullRequestId";
    return getList(sQuery);
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
    // Note that SourceControlPullRequest prior to SDEV-1952 were always external have this set to null
    String sQuery = "SELECT entity FROM SourceControlPullRequest entity" +
        " WHERE entity.source IN ?1" + (param.contains(PullRequestSource.EXTERNAL) ? " OR entity.source IS NULL" : "") +
        " ORDER BY entity.repositoryUrl, entity.pullRequestId";
    return getList(tx, sQuery, param);
  }

  void deleteByRepositoryUrl(TransactionContext tx, String repositoryUrl) {
    repositoryUrl = SourceControl.normalizeRepositoryUrl(repositoryUrl).trim();
    getByRepositoryUrl(tx, repositoryUrl).forEach(entity -> delete(tx, entity));
  }

  private List<SourceControlPullRequest> getByRepositoryUrl(TransactionContext tx, String repositoryUrl) {
    repositoryUrl = SourceControl.normalizeRepositoryUrl(repositoryUrl).trim();
    String sQuery = "SELECT entity FROM SourceControlPullRequest entity WHERE entity.repositoryUrl=?1";
    return getList(tx, sQuery, repositoryUrl);
  }

  /**
   * Returns the number of known externally created PRs for which the last detected update time falls in the given date
   * range. At least the start or the end of the range must be specified i.e. not {@code null}.
   *
   * @param startDate start of the date range; can be {@code null}, in which case the range has no left boundary
   * @param endDate   end of the date range; can be {@code null}, in which case the range has no right boundary
   */
  public int getExternalCountByUpdateTimeRange(Date startDate, Date endDate) {
    String sQuery = "SELECT COUNT(entity.id) FROM SourceControlPullRequest entity" +
        " WHERE (entity.source IS NULL OR entity.source=?1)";
    if (startDate == null) {
      if (endDate == null) {
        throw new IllegalArgumentException("Either startDate or endDate must not be null.");
      }
      else {
        // endDate is provided
        sQuery += " AND entity.lastDetectedUpdateTime<?2";
        return getSingle(Long.class, sQuery, PullRequestSource.EXTERNAL, endDate).intValue();
      }
    }
    else {
      if (endDate == null) {
        // startDate is provided
        sQuery += " AND entity.lastDetectedUpdateTime>=?2";
        return getSingle(Long.class, sQuery, PullRequestSource.EXTERNAL, startDate).intValue();
      }
      else {
        // startDate and endDate are provided
        sQuery += " AND entity.lastDetectedUpdateTime>=?2 AND entity.lastDetectedUpdateTime<?3";
        return getSingle(Long.class, sQuery, PullRequestSource.EXTERNAL, startDate, endDate).intValue();
      }
    }
  }

  /**
   * @param startDate the date from which to start looking for PRs created by IQ Manual PR and Auto PR features.
   *                Can be {@code null}, in which case the date range has no left boundary.
   * @return a list of PRs created by IQ Manual PR and Auto PR features since the specified date
   */
  public List<SourceControlPullRequest> getInternalCreatedSince(Date startDate) {
    String sQuery = """
        SELECT entity
        FROM SourceControlPullRequest entity
        WHERE entity.source IN ?1 AND (entity.createTime >= ?2 OR ?2 IS NULL)
        """;

    return getList(sQuery, EnumSet.of(
        PullRequestSource.AUTOMATIC,
        PullRequestSource.AUTOMATIC_INNER_SOURCE,
        PullRequestSource.MANUAL,
        PullRequestSource.MANUAL_INNER_SOURCE
    ), startDate);
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

  @Override
  public void delete(TransactionContext tx, SourceControlPullRequest entity) {
    super.delete(tx, entity);
    log.trace("Deleted SourceControlPullRequest: " + entity);
  }

  public SourceControlPullRequest getByRepositoryUrlAndPullRequestId(String repositoryUrl, int pullRequestId) {
    String sQuery =
        "SELECT entity FROM SourceControlPullRequest entity WHERE entity.repositoryUrl=?1 AND entity.pullRequestId=?2";
    return createQuery(sQuery, repositoryUrl, pullRequestId).forceSingleResult().get();
  }

  /**
   * Returns the pull request with the given pull request ID and a repository URL matching that of the SourceControl
   * for the given application ID
   */
  public SourceControlPullRequest getByApplicationIdAndPullRequestId(String applicationId, int pullRequestId) {
    String sQuery = """
        SELECT scpr
        FROM SourceControlPullRequest scpr, SourceControl sc
        WHERE scpr.repositoryUrl = sc.normalizedRepositoryUrl AND sc.ownerId = ?1 AND scpr.pullRequestId = ?2
        """;
    return createQuery(sQuery, applicationId, pullRequestId).forceSingleResult().get();
  }

  public List<SourceControlPullRequest> getByStatesAndSources(
      Set<PullRequestState> states,
      Set<PullRequestSource> sources)
  {
    String sQuery = """
        SELECT entity
        FROM SourceControlPullRequest entity
        WHERE entity.state IN ?1 AND entity.source IN ?2
        """;
    return getList(sQuery, states, sources);
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
}
