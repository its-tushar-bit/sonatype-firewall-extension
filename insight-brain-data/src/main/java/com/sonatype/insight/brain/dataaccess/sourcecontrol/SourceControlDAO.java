/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.sourcecontrol;

import java.sql.Timestamp;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;

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
  // Visible for tests
  static final long PULL_REQUEST_POLLING_INITIAL_OFFSET_MS = 1000L * 60 * 60 * 72; // 72 hours

  private final ApplicationDAO applicationDAO = new ApplicationDAO();

  private final OrganizationDAO organizationDAO = new OrganizationDAO();

  private final GitApiClientFactory gitApiClientFactory = new GitApiClientFactory();

  // Retrieves source control for application with information of repository even if it is not directly available
  // in source control row of application (it searches in parent organization or root organization)
  private static final String SELECT_COMPOSITE_SOURCE_CONTROL =
      "SELECT " +
          "  sc_app.source_control_id, " +
          "  sc_app.owner_id," +
          "  sc_app.repository_url, " +
          "  COALESCE(sc_app.username, sc_p.username, sc_gp.username) AS username, " +
          "  COALESCE(sc_app.token, sc_p.token, sc_gp.token) AS token, " +
          "  COALESCE(sc_app.provider, sc_p.provider, sc_gp.provider) AS provider, " +
          "  COALESCE(sc_app.base_branch, sc_p.base_branch, sc_gp.base_branch) AS base_branch, " +
          "  COALESCE(sc_app.remediation_pull_requests_enabled, sc_p.remediation_pull_requests_enabled, " +
          "   sc_gp.remediation_pull_requests_enabled) AS remediation_pull_requests_enabled, " +
          "  COALESCE(sc_app.status_checks_enabled, sc_p.status_checks_enabled, sc_gp.status_checks_enabled) " +
          "   AS status_checks_enabled, " +
          "  sc_app.pull_request_poll_time, " +
          "  sc_app.pull_request_error_count, " +
          "  COALESCE(sc_app.pull_request_commenting_enabled, sc_p.pull_request_commenting_enabled, " +
          "     sc_gp.pull_request_commenting_enabled) AS pull_request_commenting_enabled, " +
          "  COALESCE(sc_app.source_control_evaluations_enabled, sc_p.source_control_evaluations_enabled, " +
          "     sc_gp.source_control_evaluations_enabled) AS source_control_evaluations_enabled, " +
          "  COALESCE(sc_app.source_control_scan_target, sc_p.source_control_scan_target, " +
          "     sc_gp.source_control_scan_target) AS source_control_scan_target " +
          "FROM insight_brain_ods.application app " +
          "JOIN insight_brain_ods.organization po ON po.organization_id = app.organization_id " +
          "LEFT JOIN insight_brain_ods.organization gpo ON gpo.organization_id = po.parent_organization_id " +
          "JOIN insight_brain_ods.source_control sc_app ON sc_app.owner_id = app.application_id " +
          "LEFT JOIN insight_brain_ods.source_control sc_p ON sc_p.owner_id = po.organization_id " +
          "LEFT JOIN insight_brain_ods.source_control sc_gp ON sc_gp.owner_id = gpo.organization_id ";

  private static final String SELECT_COMPOSITE_SOURCE_CONTROL_FOR_APPLICATION = SELECT_COMPOSITE_SOURCE_CONTROL +
      " WHERE app.application_id = ?1";

  private static final String SELECT_APPLICATIONS_FOR_SOURCE_SCAN =
      "SELECT sc.* " +
          "FROM ( " + SELECT_COMPOSITE_SOURCE_CONTROL + " ) sc " +
          "LEFT JOIN ( " +
          "   SELECT pe.application_id, pe.time, pe.scan_trigger_type " +
          "     FROM insight_brain_ods.last_policy_evaluation lpe " +
          "     JOIN insight_brain_ods.policy_evaluation pe ON pe.policy_evaluation_id = lpe.policy_evaluation_id" +
          "     WHERE lpe.stage_type_id='source' " +
          ") lpe ON lpe.application_id =  sc.owner_id " +
          "WHERE ( lpe.time < ?1 " +
          "        AND lpe.scan_trigger_type " +
          "           IN ('SOURCE_CONTROL_INTERNAL_ONBOARDING', 'SOURCE_CONTROL_INTERNAL_DEFAULT_BRANCH_MONITORING'))" +
          // Here we retrieve applications that don't have a first source policy evaluation
          // This case happens if the user manually creates the application with source control information
          "      OR lpe.application_id IS NULL ";

  /**
   * The purpose of this method is to update the pull request poll time so it is consistent at this particular instant.
   * This accounts for initial setup of polling as well as manual and automatic source control configuration updates
   * that affect polling (i.e. new entries, repository URLs assigned and cleared, etc.).
   *
   * Consistency means:
   * 1 - if the source control entry has no repo URL then it's of no interest so we set the poll time to null
   * 2 - for an 'application' source control entry set the poll time to:
   *     (a) orgs that we poll on a per repo basis (i.e. gitlab):
   *         (1) the initial default polling time (now - 72 hours) if it's not already set, or
   *         (2) the minimum of the earliest policy evaluation timestamp or the initial default polling time
   *     (b) orgs that we poll on an org-wide basis (i.e. github):
   *         (1) always the current time, if it's not already set
   *
   * Poll time is used to determine (a) for which repos and in what sequence we will query the SCM to determine if there
   * are any open pull requests that we can possibly comment on and (b) the cutoff time after which the pull request
   * was created.
   */
  public void initializePullRequestPollTimes() {
    Date initialPollingTime = new Date();

    if (!supportsOrgWidePullRequestQueries()) {
      // we can have per-repo initial polling times that are in the past
      initialPollingTime = new Date(System.currentTimeMillis() - PULL_REQUEST_POLLING_INITIAL_OFFSET_MS);
      updatePullRequestPollTimesPerPolicyEvaluations(initialPollingTime);
    }

    setDefaultPollRequestPollTimes(initialPollingTime);
    clearExtraneousPullRequestPollTimes();
  }

  private boolean supportsOrgWidePullRequestQueries() {
    SourceControl rootOrgSourceControl = getByOwnerId(Organization.ROOT_ORGANIZATION_ID);
    return null != rootOrgSourceControl && null != rootOrgSourceControl.getProvider() &&
        rootOrgSourceControl.getProvider().supportsOrganizationWidePullRequestQueries();
  }

  private void updatePullRequestPollTimesPerPolicyEvaluations(Date defaultPollingTime) {
    EntityManager em = OperationalDataStoreProvider.getJPAEntityManagerFactory().createEntityManager();

    try (TransactionContext txn = new TransactionContext(em)) {
      txn.begin();

      // for each application where the poll time is not already set, the poll time is set to earliest date between
      // the earliest policy evaluation with an associated commit or the given default polling time
      em.createNativeQuery(
          "UPDATE insight_brain_ods.source_control sc" +
              " SET pull_request_poll_time = (" +
              " SELECT" +
              "  CASE WHEN first_commit_time IS NULL THEN ?1" +
              "       WHEN first_commit_time < ?1 THEN first_commit_time" +
              "       ELSE ?1" +
              "       END" +
              " FROM (" +
              "     SELECT application_id, min(time) AS first_commit_time" +
              "     FROM insight_brain_ods.policy_evaluation" +
              "     WHERE commit_hash IS NOT NULL" +
              "     GROUP BY application_id" +
              "     ) AS first_policy_eval_commit" +
              " WHERE sc.owner_id = first_policy_eval_commit.application_id)" +
              " WHERE sc.pull_request_poll_time IS NULL;"
      ).setParameter(1, defaultPollingTime).executeUpdate();
      txn.commit();
    }
  }

  private void setDefaultPollRequestPollTimes(Date defaultPollingTime) {
    EntityManager em = OperationalDataStoreProvider.getJPAEntityManagerFactory().createEntityManager();

    try (TransactionContext txn = new TransactionContext(em)) {
      txn.begin();
      em.createNativeQuery(
          "UPDATE insight_brain_ods.source_control SET pull_request_poll_time = ?1" +
              " WHERE pull_request_poll_time IS NULL AND repository_url IS NOT NULL;"
      ).setParameter(1, defaultPollingTime).executeUpdate();
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
    return getList("SELECT entity FROM SourceControl entity WHERE entity.normalizedRepositoryUrl LIKE ?1",
        "%/" + repositoryOwnerAndName.toLowerCase(Locale.ENGLISH) + '%');
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
   * the root token/provider anywhere in their hierarchy (ie: at the app or org level)
   *
   * @return list of source controls for apps
   */
  public List<SourceControl> getApplicationSourceControlsWithInheritedCredentials() {
    String query = "SELECT entity " +
        "FROM SourceControl entity, Application app " +
        "WHERE entity.repositoryUrl IS NOT NULL and entity.token IS NULL and entity.provider IS NULL " +
        "AND app.id=entity.ownerId " +
        "AND NOT EXISTS (" +
        "SELECT orgEntity FROM SourceControl orgEntity " +
        "WHERE orgEntity.ownerId = app.organizationId AND " +
        " (orgEntity.token IS NOT NULL OR orgEntity.provider IS NOT NULL) " +
        ")";

    return getList(query);
  }

  private List<SourceControl> getByOrganization() {
    String query = "SELECT entity FROM SourceControl entity WHERE entity.repositoryUrl IS NULL";

    return getList(query);
  }

  public List<SourceControl> getApplicationsWithRemediationPullRequestsEnabled() {
    // an application is enabled if it has a valid repository_url and remediation_pull_requests_enabled is set at the
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
        .filter(
            application -> areRemediationPullRequestsEnabled(application, applicationsById, orgSourceControlsByOrgId,
                scRootOrg))
        .collect(ImmutableList.toImmutableList());
  }

  private boolean areRemediationPullRequestsEnabled(
      final SourceControl sourceControl,
      final Map<String, Application> applicationsById,
      final Map<String, SourceControl> orgSourceControlsByOrgId,
      final SourceControl scRootOrg)
  {
    if (sourceControl.getRemediationPullRequestsEnabled() != null) {
      return sourceControl.getRemediationPullRequestsEnabled();
    }

    // application did not define a value, so check organization
    String orgId = applicationsById.get(sourceControl.getOwnerId()).getOrganizationId();
    if (orgSourceControlsByOrgId.containsKey(orgId)) {
      SourceControl orgSourcControl = orgSourceControlsByOrgId.get(orgId);
      if (orgSourcControl.getRemediationPullRequestsEnabled() != null) {
        return orgSourcControl.getRemediationPullRequestsEnabled();
      }
    }

    // organization did not define a value, check root org
    if (scRootOrg != null && scRootOrg.getRemediationPullRequestsEnabled() != null) {
      return scRootOrg.getRemediationPullRequestsEnabled();
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

    SourceControl existingSourceControl = getById(tx, sourceControl.getId());
    if (!Objects.equals(sourceControl.getRepositoryUrl(), existingSourceControl.getRepositoryUrl())
        && !StringUtils.isBlank(existingSourceControl.getRepositoryUrl())) {
      // If the repository URL has changed, clear the SSH URL
      sourceControl.setRepositorySshUrl(null);
      // if other SC entries have the same repo URL, clear their PRs
      List<SourceControl> sourceControlsWithSameRepositoryUrl =
          getByRepositoryUrl(tx, existingSourceControl.getRepositoryUrl());
      if (sourceControlsWithSameRepositoryUrl.size() == 1) {
        // This is the only SourceControl with the old repository URL.
        // Delete all SourceControlPullRequests for this repository URL.
        new SourceControlPullRequestDAO().deleteByRepositoryUrl(tx, existingSourceControl.getRepositoryUrl());
      }
    }

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

  @Override
  public void delete(TransactionContext tx, SourceControl entity) {
    if (entity == null) {
      return;
    }

    // Cascade to SourceControlPullRequest
    if (!StringUtils.isBlank(entity.getRepositoryUrl())) {
      List<SourceControl> sourceControlsWithSameRepositoryUrl = getByRepositoryUrl(tx, entity.getRepositoryUrl());
      if (sourceControlsWithSameRepositoryUrl.size() == 1) {
        // This is the only SourceControl with this repository URL.
        // Delete all SourceControlPullRequests for this repository URL.
        new SourceControlPullRequestDAO().deleteByRepositoryUrl(tx, entity.getRepositoryUrl());
      }
    }

    super.delete(tx, entity);
  }

  public List<SourceControl> getByRepositoryUrl(String repositoryUrl) {
    return getByRepositoryUrl(null, repositoryUrl);
  }

  private List<SourceControl> getByRepositoryUrl(TransactionContext tx, String repositoryUrl) {
    if (repositoryUrl == null) {
      return Collections.emptyList();
    }

    repositoryUrl = SourceControl.normalizeRepositoryUrl(repositoryUrl);
    String sQuery = "SELECT entity FROM SourceControl entity WHERE entity.normalizedRepositoryUrl=?1";
    if (tx == null) {
      return getList(sQuery, repositoryUrl);
    }
    return getList(tx, sQuery, repositoryUrl);
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
      if (null == sourceControl.getRemediationPullRequestsEnabled()) {
        sourceControl.setRemediationPullRequestsEnabled(SourceControl.ENABLE_REMEDIATION_PULL_REQUESTS_BY_DEFAULT);
      }
      if (null == sourceControl.getStatusChecksEnabled()) {
        sourceControl.setStatusChecksEnabled(SourceControl.ENABLE_STATUS_CHECKS_BY_DEFAULT);
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
        throw new BadRequestException("SourceControl baseBranch is required for the root organization");
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
          .createProjectUri(sourceControl.getNormalizedRepositoryUrl());
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

  public SourceControl getCompositeSourceControlByApplicationId(final String applicationId) {
    try (TransactionContext tx = createTransactionContext()) {
      javax.persistence.Query query =
          tx.createNativeQuery(SELECT_COMPOSITE_SOURCE_CONTROL_FOR_APPLICATION, SourceControl.class);
      query.setParameter(1, applicationId);

      return (SourceControl) query.getSingleResult();
    }
    catch (NoResultException e) {
      return null;
    }
  }

  public List<SourceControl> getCompositeSourceControlForOutdatedSourceScans(
      final Date scanLimitDate)
  {
    try (TransactionContext tx = createTransactionContext()) {
      javax.persistence.Query query = tx.createNativeQuery(SELECT_APPLICATIONS_FOR_SOURCE_SCAN);

      query.setParameter(1, scanLimitDate);

      return ((Stream<Object[]>) query.getResultStream()).parallel()
          .map(array -> {
            SourceControl sc = new SourceControl();
            sc.setId((String) array[0]);
            sc.setOwnerId((String) array[1]);
            sc.setRepositoryUrl((String) array[2]);
            sc.setUsername((String) array[3]);
            sc.setToken((String) array[4]);
            sc.setProvider(SourceControlProvider.fromString((String) array[5]));
            sc.setBaseBranch((String) array[6]);
            sc.setRemediationPullRequestsEnabled((Boolean) array[7]);
            sc.setStatusChecksEnabled((Boolean) array[8]);
            sc.setPullRequestPollTime(array[9] == null ? null : new Date(((Timestamp) array[9]).getTime()));
            sc.setPullRequestErrorCount((int) array[10]);
            sc.setPullRequestCommentingEnabled((Boolean) array[11]);
            sc.setSourceControlEvaluationsEnabled((Boolean) array[12]);
            sc.setSourceControlScanTarget((String) array[13]);

            return sc;
          })
          .collect(Collectors.toList());
    }
  }
}
