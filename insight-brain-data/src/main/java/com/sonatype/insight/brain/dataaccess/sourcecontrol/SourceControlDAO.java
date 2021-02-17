/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.sourcecontrol;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.persistence.EntityManager;

import com.sonatype.insight.brain.dataaccess.AbstractOperationalSqlDAO;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.db.OperationalDataStoreProvider;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControl;
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.NotFoundException;
import com.sonatype.nexus.scm.GitApiClientFactory;
import com.sonatype.nexus.scm.SourceControlProvider;

import com.google.common.collect.ImmutableList;
import org.apache.commons.lang3.StringUtils;

import static com.sonatype.insight.brain.model.Organization.ROOT_ORGANIZATION_ID;
import static org.apache.commons.lang3.StringUtils.isBlank;

public class SourceControlDAO
    extends AbstractOperationalSqlDAO<SourceControl>
{
  private final ApplicationDAO applicationDAO = new ApplicationDAO();

  private final OrganizationDAO organizationDAO = new OrganizationDAO();

  private final GitApiClientFactory gitApiClientFactory = new GitApiClientFactory();

  /**
   * The purpose of this method is to update the pull request poll time so it is consistent at this particular instant.
   * This accounts for initial setup of polling as well as manual and automatic source control configuration updates
   * that affect polling (i.e. new entries, repository URLs assigned and cleared, etc.).
   *
   * Consistency means:
   * 1 - if the source control entry has no repo URL then it's of no interest so we set the poll time to null
   * 2 - for an 'application' source control entry and if not already set, set the poll time to the time of the
   * earliest policy evaluation we have for that application that also has a commit associated
   * 3 - otherwise, set the poll time to the current timestamp where it is not otherwise set and a repo url exists
   *
   * Poll time is used to determine for which repos and in what sequence we will query the SCM to determine if there
   * are any open pull requests that we can possibly comment on.
   */
  public void initializePullRequestPollTimes() {
    updatePullRequestPollTimesPerPolicyEvaluations();
    setDefaultPollRequestPollTimes();
    clearExtraneousPullRequestPollTimes();
  }

  private void updatePullRequestPollTimesPerPolicyEvaluations() {
    EntityManager em = OperationalDataStoreProvider.getJPAEntityManagerFactory().createEntityManager();

    try (TransactionContext txn = new TransactionContext(em)) {
      txn.begin();

      // for each application where the poll time is not already set, the poll time is set to the earliest policy
      // evaluation with an associated commit
      em.createNativeQuery(
          "UPDATE insight_brain_ods.source_control sc" +
              " SET pull_request_poll_time = (" +
              " SELECT first_commit_time" +
              " FROM (" +
              "     SELECT application_id, min(time) AS first_commit_time" +
              "     FROM insight_brain_ods.policy_evaluation" +
              "     WHERE commit_hash IS NOT NULL" +
              "     GROUP BY application_id" +
              "     ) AS first_policy_eval_commit" +
              " WHERE sc.owner_id = first_policy_eval_commit.application_id)" +
              " WHERE sc.pull_request_poll_time IS NULL;"
      ).executeUpdate();
      txn.commit();
    }
  }

  private void setDefaultPollRequestPollTimes() {
    EntityManager em = OperationalDataStoreProvider.getJPAEntityManagerFactory().createEntityManager();

    try (TransactionContext txn = new TransactionContext(em)) {
      txn.begin();
      // where not set and a repo url exists set the poll time to the current timestamp
      em.createNativeQuery(
          "UPDATE insight_brain_ods.source_control SET pull_request_poll_time = CURRENT_TIMESTAMP" +
              " WHERE pull_request_poll_time IS NULL AND repository_url IS NOT NULL;"
      ).executeUpdate();
      txn.commit();
    }
  }

  private void clearExtraneousPullRequestPollTimes() {
    EntityManager em = OperationalDataStoreProvider.getJPAEntityManagerFactory().createEntityManager();

    try (TransactionContext txn = new TransactionContext(em)) {
      txn.begin();

      // set poll time to null where repo url is null
      em.createNativeQuery(
          "UPDATE insight_brain_ods.source_control SET pull_request_poll_time = NULL WHERE repository_url IS NULL;"
      ).executeUpdate();

      txn.commit();
    }
  }

  public SourceControl getNextRepositoryToPoll() {
    String sQuery =
        "SELECT entity FROM SourceControl entity" +
            " WHERE entity.repositoryUrl IS NOT NULL" +
            " AND entity.pullRequestPollTime IS NOT NULL" +
            " AND entity.pullRequestPollTime <= CURRENT_TIMESTAMP" +
            " ORDER BY entity.pullRequestPollTime ASC";
    return createQuery(sQuery).forceSingleResult().get();
  }

  public List<SourceControl> getByRepositoryOwnerAndName(String repositoryOwnerAndName) {
    return getList("SELECT entity FROM SourceControl entity WHERE UPPER(entity.repositoryUrl) LIKE ?1",
        "%/" + repositoryOwnerAndName.toUpperCase() + '%');
  }

  @Override
  public SourceControl getById(final TransactionContext tx, final String id) {
    return get(tx, "SELECT entity FROM SourceControl entity WHERE entity.id=?1", id);
  }

  public SourceControl getByIdNotNull(final String id) {
    SourceControl sourceControl = getById(id);
    if (sourceControl == null) {
      throw new NotFoundException("Could not find a SourceControl with ID " + id + ".");
    }
    return sourceControl;
  }

  public List<SourceControl> getByApplication() {
    String query = "SELECT entity FROM SourceControl entity WHERE entity.repositoryUrl IS NOT NULL";

    return getList(query);
  }

  /**
   * Get all the application-level source control entries which are under a given organization
   */
  public List<SourceControl> getApplicationSourceControlsByOrganizationWithRepositories(String orgId) {
    String query = "SELECT entity " +
        "FROM SourceControl entity, Application app " +
        "WHERE entity.ownerId=app.id AND app.organizationId=?1 AND entity.repositoryUrl IS NOT NULL " +
        // filter out apps with a custom token. We're interested in apps that can be loaded using the
        // existing org tokens, otherwise they may be on custom hosts
        "AND entity.token IS NULL";
    return getList(query, orgId);
  }

  /**
   * Gets a list of source control entries for applications that do not override
   * the root token anywhere in their hierarchy (ie: at the app or org level)
   * @return list of source controls for apps
   */
  public List<SourceControl> getApplicationSourceControlsWithRepositoriesAndDefaultToken() {
    String query = "SELECT entity " +
        "FROM SourceControl entity, Application app " +
        "WHERE entity.repositoryUrl IS NOT NULL and entity.token IS NULL " +
        "AND app.id=entity.ownerId " +
        "AND NOT EXISTS (" +
        "SELECT orgEntity FROM SourceControl orgEntity " +
        "WHERE orgEntity.ownerId = app.organizationId AND orgEntity.token IS NOT NULL " +
        ")";

    return getList(query);
  }

  private List<SourceControl> getByOrganization() {
    String query = "SELECT entity FROM SourceControl entity WHERE entity.repositoryUrl IS NULL";

    return getList(query);
  }

  public List<SourceControl> getApplicationsWithPullReqsEnabled() {
    // an application is enabled if it has a valid repository_url and enable_pull_requests is set at the
    // application, parent organization, or root organization level

    SourceControl scRootOrg = getByOwnerId(Organization.ROOT_ORGANIZATION_ID);

    Map<String, Application> applicationsById = applicationDAO.getAll()
        .stream()
        .collect(Collectors.toMap(Application::getId, Function.identity()));

    Map<String, SourceControl> orgSourceControlsByOrgId = getByOrganization()
        .stream()
        .collect(Collectors.toMap(SourceControl::getOwnerId, Function.identity()));

    return getByApplication()
        .stream()
        .filter(application -> isPrEnabled(application, applicationsById, orgSourceControlsByOrgId, scRootOrg))
        .collect(ImmutableList.toImmutableList());
  }

  private boolean isPrEnabled(
      final SourceControl application,
      final Map<String, Application> applicationsById,
      final Map<String, SourceControl> orgSourceControlsByOrgId,
      final SourceControl scRootOrg)
  {
    if (application.getEnablePullRequests() != null) {
      return application.getEnablePullRequests();
    }

    // application did not define a value, so check organization
    String orgId = applicationsById.get(application.getOwnerId()).getOrganizationId();
    if (orgSourceControlsByOrgId.containsKey(orgId)) {
      SourceControl orgSourcControl = orgSourceControlsByOrgId.get(orgId);
      if (orgSourcControl.getEnablePullRequests() != null) {
        return orgSourcControl.getEnablePullRequests();
      }
    }

    // organization did not define a value, check root org
    if (scRootOrg != null && scRootOrg.getEnablePullRequests() != null) {
      return scRootOrg.getEnablePullRequests();
    }

    // could not find a defined value
    return false;
  }

  public List<SourceControl> getAll() {
    return getList("SELECT entity FROM SourceControl entity");
  }

  @Override
  public void insert(final TransactionContext tx, final SourceControl sourceControl) {
    validate(tx, sourceControl);
    setDefaultsAsNecessary(sourceControl);
    super.insert(tx, sourceControl);
  }

  @Override
  public void update(final TransactionContext tx, final SourceControl sourceControl) {
    validate(tx, sourceControl);
    setDefaultsAsNecessary(sourceControl);
    super.update(tx, sourceControl);
  }

  /**
   * Support cascade delete from Application or Organization.
   */
  public void deleteByOwnerId(final TransactionContext tx, final String ownerId) {
    SourceControl existing = getByOwnerId(tx, ownerId);
    if (existing != null) {
      delete(tx, existing);
    }
  }

  public SourceControl getByOwnerId(final String ownerId) {
    try (TransactionContext tx = createTransactionContext()) {
      return getByOwnerId(tx, ownerId);
    }
  }

  public SourceControl getByOwnerId(final TransactionContext tx, final String ownerId) {
    return get(tx, "SELECT entity FROM SourceControl entity WHERE entity.ownerId=?1", ownerId);
  }

  public void updatePollTimeAndErrorCounts(String sourceControlId, Date pollTime, int errorCount) {
    try (TransactionContext tx = createTransactionContext()) {
      tx.begin();
      updatePollTimeAndErrorCounts(tx, sourceControlId, pollTime, errorCount);
      tx.commit();
    }
  }

  public void updatePollTimeAndErrorCounts(
      TransactionContext tx,
      String sourceControlId,
      Date pollTime,
      int errorCount)
  {
    SourceControl sourceControl = getById(tx, sourceControlId);
    if (null != sourceControl) {
      sourceControl.setPullRequestErrorCount(errorCount);
      sourceControl.setPullRequestPollTime(pollTime);
      super.update(tx, sourceControl);
    }
  }

  private void setDefaultsAsNecessary(SourceControl sourceControl) {
    if (isForRootOrganization(sourceControl)) {
      if (null == sourceControl.getEnablePullRequests()) {
        sourceControl.setEnablePullRequests(SourceControl.ENABLE_PULL_REQUESTS_BY_DEFAULT);
      }
      if (null == sourceControl.getEnableStatusChecks()) {
        sourceControl.setEnableStatusChecks(SourceControl.ENABLE_STATUS_CHECKS_BY_DEFAULT);
      }
    }
  }

  private void validate(final TransactionContext tx, final SourceControl sourceControl) {
    if (StringUtils.isBlank(sourceControl.getOwnerId())) {
      throw new BadRequestException("SourceControl owner id is required");
    }

    if (isForRootOrganization(sourceControl)) {
      if (sourceControl.getProvider() == null) {
        throw new BadRequestException("SourceControl provider is required for the root organization");
      }
      if (isBlank(sourceControl.getBaseBranch())) {
        throw new BadRequestException("SourceControl default branch is required for the root organization");
      }
    }
    else {
      if (sourceControl.getProvider() != null) {
        throw new BadRequestException("SourceControl provider can only be specified on the root organization");
      }
    }

    if (isForOrganization(tx, sourceControl)) {
      if (sourceControl.getRepositoryUrl() != null) {
        throw new BadRequestException("SourceControl repositoryUrl is not allowed for organization");
      }
    }
    else if (isForApplication(tx, sourceControl)) {
      validateRepositoryUrl(tx, sourceControl);
    }
    else {
      throw new BadRequestException(
          "SourceControl ownerId '" + sourceControl.getOwnerId() + "' cannot be found");
    }

    validateUsername(tx, sourceControl);
  }

  private void validateRepositoryUrl(final TransactionContext tx, final SourceControl sourceControl) {
    if (StringUtils.isBlank(sourceControl.getRepositoryUrl())) {
      throw new BadRequestException("SourceControl repositoryUrl is required for application");
    }
    try {
      gitApiClientFactory.getGitApiClientUtils(getProvider(tx, sourceControl))
          .createProjectUri(sourceControl.getRepositoryUrl());
    }
    catch (IllegalArgumentException e) {
      throw new BadRequestException("SourceControl repositoryUrl is invalid: " + e.getMessage(), e);
    }
  }

  private void validateUsername(final TransactionContext tx, final SourceControl sourceControl) {
    SourceControlProvider sourceControlProvider = getProvider(tx, sourceControl);
    if (!sourceControlProvider.requiresUsername() && StringUtils.isNotEmpty(sourceControl.getUsername())) {
      throw new BadRequestException("SourceControl provider '" + sourceControlProvider + "' does not allow username");
    }
  }

  private boolean isForOrganization(final TransactionContext tx, final SourceControl sourceControl) {
    return organizationDAO.getById(tx, sourceControl.getOwnerId()) != null;
  }

  private boolean isForApplication(final TransactionContext tx, final SourceControl sourceControl) {
    return applicationDAO.getById(tx, sourceControl.getOwnerId()) != null;
  }

  private boolean isForRootOrganization(final SourceControl sourceControl) {
    return ROOT_ORGANIZATION_ID.equals(sourceControl.getOwnerId());
  }

  private SourceControlProvider getProvider(
      final TransactionContext tx,
      final SourceControl sourceControl)
  {
    SourceControlProvider sourceControlProvider;
    if (sourceControl.getProvider() != null) {
      sourceControlProvider = sourceControl.getProvider();
    }
    else {
      if (isForApplication(tx, sourceControl)) {
        Application application = applicationDAO.getById(tx, sourceControl.getOwnerId());
        sourceControlProvider = getProviderFromOrganization(tx, application.getOrganizationId());
      }
      else {
        sourceControlProvider = getProviderFromOrganization(tx, sourceControl.getOwnerId());
      }
      if (sourceControlProvider == null) {
        throw new BadRequestException("Cannot validate SourceControl repositoryUrl. " +
            "The root organization source control provider is not set. " +
            "Please configure the root organization source control provider");
      }
    }
    return sourceControlProvider;
  }

  private SourceControlProvider getProviderFromOrganization(
      final TransactionContext tx,
      final String organizationId)
  {
    SourceControl orgSourceControl = getByOwnerId(tx, organizationId);
    if (orgSourceControl != null && orgSourceControl.getProvider() != null) {
      return orgSourceControl.getProvider();
    }

    Organization organization = organizationDAO.getById(organizationId);
    if (StringUtils.isEmpty(organization.getParentOrganizationId())) {
      return null;
    }

    return getProviderFromOrganization(tx, organization.getParentOrganizationId());
  }
}
