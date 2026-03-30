/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.sourcecontrol;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.sonatype.insight.brain.dataaccess.AbstractOperationalSqlDAO;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.dataaccess.OwnerDAO;
import com.sonatype.insight.brain.db.datastore.OperationalDataStore;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControl;
import com.sonatype.insight.brain.security.RotatableSecrets;
import com.sonatype.insight.brain.validation.SourceControlSshValidator;
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.nexus.git.utils.GitBranchNameValidator;
import com.sonatype.nexus.git.utils.InvalidBranchNameException;
import com.sonatype.nexus.scm.GitApiClientFactory;
import com.sonatype.nexus.scm.SourceControlProvider;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import org.apache.commons.lang3.StringUtils;
import org.apache.shiro.util.CollectionUtils;
import org.jooq.Table;
import org.jooq.impl.DSL;
import org.jooq.impl.SQLDataType;

import static com.sonatype.insight.brain.jooq.generated.ods.tables.SourceControl.SOURCE_CONTROL;
import static com.sonatype.insight.brain.model.Organization.ROOT_ORGANIZATION_ID;
import static org.apache.commons.lang3.StringUtils.isBlank;

@Named
@Singleton
public class SourceControlDAO
    extends AbstractOperationalSqlDAO<SourceControl>
    implements RotatableSecrets
{
  public static final int EXTERNAL_EVALUATION_WINDOW_IN_DAYS = 7;

  // Visible for tests
  static final long PULL_REQUEST_POLLING_INITIAL_OFFSET_MS = 1000L * 60 * 60 * 72; // 72 hours

  private final ApplicationDAO applicationDAO;

  private final OrganizationDAO organizationDAO;

  private final OwnerDAO ownerDAO;

  private final GitApiClientFactory gitApiClientFactory;

  private final SourceControlPullRequestDAO sourceControlPullRequestDAO;

  private final SourceControlSshValidator sourceControlSshValidator;

  // The '_SCHEMA_' string will be replaced by the proper schema at runtime.
  // Uses JDBC-style positional parameters (?) for jOOQ compatibility.
  private static final String SELECT_APPLICATIONS_FOR_SOURCE_SCAN =
      "SELECT sc.owner_id " +
          "FROM _SCHEMA_.source_control sc " +
          "JOIN _SCHEMA_.application a ON sc.owner_id = a.application_id " +
          "LEFT JOIN ( " +
          "   SELECT pe.application_id, pe.time, pe.scan_trigger_type " +
          "     FROM _SCHEMA_.last_policy_evaluation lpe " +
          "     JOIN _SCHEMA_.policy_evaluation pe ON pe.policy_evaluation_id = lpe.policy_evaluation_id" +
          "     WHERE lpe.stage_type_id='source' " +
          ") lpe ON lpe.application_id = sc.owner_id " +
          "WHERE " +
          // Either: last source stage PE is internally triggered, but not by DBM, and it's older than the DBM run
          // window
          "( lpe.time < ? AND lpe.scan_trigger_type " +
          "           IN ('SOURCE_CONTROL_INTERNAL_ONBOARDING', 'SOURCE_CONTROL_INTERNAL_PULL_REQUEST') ) " +
          // Or: the last source stage PE is triggered by SC API, and it's older than the external eval. window (7 days)
          "      OR ( lpe.time < ? AND lpe.scan_trigger_type = 'SOURCE_CONTROL_API' ) " +
          // Or: the last source stage PE is triggered by DBM; we'll keep doing DBM for this app
          "      OR lpe.scan_trigger_type = 'SOURCE_CONTROL_INTERNAL_DEFAULT_BRANCH_MONITORING' " +
          // Or: we don't have any source-stage PE
          // This case happens if the user manually creates the application with source control information
          "      OR lpe.application_id IS NULL ";

  private static final String BUILD_COMPOSITE_SOURCE_CONTROL =
      "SELECT " +
      // select the first non-null value for each column in the ordered set of Source Control records
          "(ARRAY_REMOVE(ARRAY_AGG(source_control_id ORDER BY hierarchy_order), NULL))[1] as source_control_id," +
          "(ARRAY_REMOVE(ARRAY_AGG(owner_id ORDER BY hierarchy_order), NULL))[1] as owner_id," +
          "(ARRAY_REMOVE(ARRAY_AGG(repository_url ORDER BY hierarchy_order), NULL))[1] as repository_url," +
          "(ARRAY_REMOVE(ARRAY_AGG(normalized_repository_url ORDER BY hierarchy_order), NULL))[1] " +
          "as normalized_repository_url," +
          "(ARRAY_REMOVE(ARRAY_AGG(repository_ssh_url ORDER BY hierarchy_order), NULL))[1] as repository_ssh_url," +
          "(ARRAY_REMOVE(ARRAY_AGG(username ORDER BY hierarchy_order), NULL))[1] as username," +
          "(ARRAY_REMOVE(ARRAY_AGG(token ORDER BY hierarchy_order), NULL))[1] as token," +
          "(ARRAY_REMOVE(ARRAY_AGG(provider ORDER BY hierarchy_order), NULL))[1] as provider," +
          "(ARRAY_REMOVE(ARRAY_AGG(base_branch ORDER BY hierarchy_order), NULL))[1] as base_branch," +
          "(ARRAY_REMOVE(ARRAY_AGG(remediation_pull_requests_enabled ORDER BY hierarchy_order), NULL))[1] " +
          "as remediation_pull_requests_enabled," +
          "(ARRAY_REMOVE(ARRAY_AGG(status_checks_enabled ORDER BY hierarchy_order), NULL))[1] " +
          "as status_checks_enabled," +
          "(ARRAY_REMOVE(ARRAY_AGG(pull_request_poll_time ORDER BY hierarchy_order), NULL))[1] " +
          "as pull_request_poll_time," +
          "(ARRAY_REMOVE(ARRAY_AGG(pull_request_error_count ORDER BY hierarchy_order), NULL))[1] " +
          "as pull_request_error_count," +
          "(ARRAY_REMOVE(ARRAY_AGG(pull_request_commenting_enabled ORDER BY hierarchy_order), NULL))[1] " +
          "as pull_request_commenting_enabled," +
          "(ARRAY_REMOVE(ARRAY_AGG(source_control_evaluations_enabled ORDER BY hierarchy_order), NULL))[1] " +
          "as source_control_evaluations_enabled," +
          "(ARRAY_REMOVE(ARRAY_AGG(source_control_scan_target ORDER BY hierarchy_order), NULL))[1] " +
          "as source_control_scan_target," +
          "(ARRAY_REMOVE(ARRAY_AGG(ssh_enabled ORDER BY hierarchy_order), NULL))[1] as ssh_enabled," +
          "(ARRAY_REMOVE(ARRAY_AGG(commit_status_enabled ORDER BY hierarchy_order), NULL))[1] " +
          "as commit_status_enabled, " +
          "(ARRAY_REMOVE(ARRAY_AGG(manual_pull_requests_enabled ORDER BY hierarchy_order), NULL))[1] " +
          "as manual_pull_requests_enabled, " +
          "(ARRAY_REMOVE(ARRAY_AGG(inner_source_automated_updates_enabled ORDER BY hierarchy_order), NULL))[1] " +
          "as inner_source_automated_updates_enabled " +
          "FROM (" +
          // Create ordered set of Source Control records, from application to root organization
          "SELECT * FROM (" +
          "WITH RECURSIVE ownership_hierarchy(entity_id, hierarchy_order) AS (" +
          "SELECT organization_id, 1 from _SCHEMA_.application WHERE application_id={0}" +
          "  UNION " +
          "SELECT org.parent_organization_id, oh.hierarchy_order+1 " +
          "FROM ownership_hierarchy oh, _SCHEMA_.organization org " +
          "WHERE org.organization_id = oh.entity_id AND org.parent_organization_id IS NOT NULL" +
          ") SELECT DISTINCT hierarchy_order, entity_id " +
          "FROM ownership_hierarchy UNION SELECT 0, {0} FROM ownership_hierarchy " +
          "ORDER BY hierarchy_order" +
          ") AS ORDERED_SOURCE_CONTROLS LEFT JOIN _SCHEMA_.source_control " +
          "ON source_control.owner_id = ORDERED_SOURCE_CONTROLS.entity_id" +
          ") AS FINAL_RESULT";

  @Inject
  public SourceControlDAO(
      final OperationalDataStore operationalDataStore,
      final ApplicationDAO applicationDAO,
      final OrganizationDAO organizationDAO,
      final OwnerDAO ownerDAO,
      final GitApiClientFactory gitApiClientFactory,
      final SourceControlPullRequestDAO sourceControlPullRequestDAO,
      final SourceControlSshValidator sourceControlSshValidator)
  {
    super(operationalDataStore);
    this.applicationDAO = applicationDAO;
    this.organizationDAO = organizationDAO;
    this.ownerDAO = ownerDAO;
    this.gitApiClientFactory = gitApiClientFactory;
    this.sourceControlPullRequestDAO = sourceControlPullRequestDAO;
    this.sourceControlSshValidator = sourceControlSshValidator;
  }

  /**
   * The purpose of this method is to update the pull request poll time so it is consistent at this particular instant.
   * This accounts for initial setup of polling as well as manual and automatic source control configuration updates
   * that affect polling (i.e. new entries, repository URLs assigned and cleared, etc.).
   *
   * Consistency means:
   * 1 - if the source control entry has no repo URL then it's of no interest so we set the poll time to null
   * 2 - for an 'application' source control entry set the poll time to:
   * (a) orgs that we poll on a per repo basis (i.e. gitlab):
   * (1) the initial default polling time (now - 72 hours) if it's not already set, or
   * (2) the minimum of the earliest policy evaluation timestamp or the initial default polling time
   * (b) orgs that we poll on an org-wide basis (i.e. github):
   * (1) always the current time, if it's not already set
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

    setDefaultPullRequestPollTimes(initialPollingTime);
    clearExtraneousPullRequestPollTimes();
  }

  private boolean supportsOrgWidePullRequestQueries() {
    SourceControl rootOrgSourceControl = getByOwnerId(Organization.ROOT_ORGANIZATION_ID);
    return null != rootOrgSourceControl && null != rootOrgSourceControl.getProvider() &&
        rootOrgSourceControl.getProvider().supportsOrganizationWidePullRequestQueries();
  }

  private void updatePullRequestPollTimesPerPolicyEvaluations(Date defaultPollingTime) {
    try (TransactionContext txn = createTransactionContext()) {
      txn.begin();

      // for each application where the poll time is not already set, the poll time is set to earliest date between
      // the earliest policy evaluation with an associated commit or the given default polling time
      String sql = "UPDATE " + getDatabaseSchema() + ".source_control sc" +
          " SET pull_request_poll_time = (" +
          " SELECT" +
          "  CASE WHEN first_commit_time IS NULL THEN ?" +
          "       WHEN first_commit_time < ? THEN first_commit_time" +
          "       ELSE ?" +
          "       END" +
          " FROM (" +
          "     SELECT application_id, min(time) AS first_commit_time" +
          "     FROM " + getDatabaseSchema() + ".policy_evaluation" +
          "     WHERE commit_hash IS NOT NULL" +
          "     GROUP BY application_id" +
          "     ) AS first_policy_eval_commit" +
          " WHERE sc.owner_id = first_policy_eval_commit.application_id)" +
          " WHERE sc.pull_request_poll_time IS NULL;";
      txn.dsl()
          .execute(sql,
              DSL.val(defaultPollingTime, SQLDataType.TIMESTAMP),
              DSL.val(defaultPollingTime, SQLDataType.TIMESTAMP),
              DSL.val(defaultPollingTime, SQLDataType.TIMESTAMP));
      txn.commit();
    }
  }

  private void setDefaultPullRequestPollTimes(Date defaultPollingTime) {
    try (TransactionContext txn = createTransactionContext()) {
      txn.begin();
      String sql = "UPDATE " + getDatabaseSchema() +
          ".source_control SET pull_request_poll_time = ?" +
          " WHERE pull_request_poll_time IS NULL AND repository_url IS NOT NULL;";
      txn.dsl().execute(sql, DSL.val(defaultPollingTime, SQLDataType.TIMESTAMP));
      txn.commit();
    }
  }

  private void clearExtraneousPullRequestPollTimes() {
    try (TransactionContext txn = createTransactionContext()) {
      txn.begin();

      // set poll time to null where repo url is null
      String sql = "UPDATE " + getDatabaseSchema() +
          ".source_control SET pull_request_poll_time = NULL WHERE repository_url IS NULL;";
      txn.dsl().execute(sql);

      txn.commit();
    }
  }

  public SourceControl getNextRepositoryToPoll() {
    try (TransactionContext tx = createTransactionContext()) {
      return toEntity(tx.dsl()
          .selectFrom(SOURCE_CONTROL)
          .where(SOURCE_CONTROL.REPOSITORY_URL.isNotNull())
          .and(SOURCE_CONTROL.PULL_REQUEST_POLL_TIME.isNotNull())
          .and(SOURCE_CONTROL.PULL_REQUEST_POLL_TIME.le(new Date()))
          .orderBy(SOURCE_CONTROL.PULL_REQUEST_POLL_TIME.asc())
          .limit(1)
          .fetchOne());
    }
  }

  private SourceControl getCompositeSourceControlByOwnerIds(List<String> ownerIds) {
    SourceControl sourceControl = new SourceControl();

    // we can't guarantee that the list of source control entries will be in the proper order so we'll order them here
    orderByHierarchy(ownerIds, getByOwnerIds(ownerIds)).forEach(sc -> SourceControl.coalesce(sourceControl, sc));

    return sourceControl;
  }

  private SourceControl buildCompositeSourceControlInPostgres(String applicationId) {
    // Single query to build Source Control instance for an application. Relies on Postgres-specific features
    try (TransactionContext tx = createTransactionContext()) {
      String sql = injectSchemaName(BUILD_COMPOSITE_SOURCE_CONTROL);
      // Using {0} indexed placeholder allows the same parameter to be referenced multiple times in the query
      SourceControl result = tx.dsl()
          .resultQuery(sql, DSL.val(applicationId))
          .fetchOneInto(SourceControl.class);
      // The aggregate query returns a row with all NULLs when no matching application exists.
      // Return null in that case to match the expected behavior.
      return result != null && result.getId() == null ? null : result;
    }
  }

  public SourceControl buildCompositeSourceControlForApplicationId(String applicationId) {
    // Use optimized single query if using Postgres. Otherwise, use application logic to build composite SourceControl.
    if (this.isDatabasePostgresql()) {
      return buildCompositeSourceControlInPostgres(applicationId);
    }
    else {
      return buildCompositeSourceControlInApplication(applicationId);
    }
  }

  public List<SourceControl> getByOwnerIds(final List<String> ownerIds) {
    try (TransactionContext tx = createTransactionContext()) {
      return tx.dsl()
          .selectFrom(SOURCE_CONTROL)
          .where(SOURCE_CONTROL.OWNER_ID.in(ownerIds))
          .fetch(this::toEntity);
    }
  }

  public List<SourceControl> orderByHierarchy(List<String> ownerIds, List<SourceControl> unordered) {
    List<SourceControl> sorted = new ArrayList<>();

    // create a lookup map for the unordered source control entries
    Map<String, SourceControl> sourceControlMap = new HashMap<>();
    unordered.forEach(sc -> sourceControlMap.put(sc.getOwnerId(), sc));

    // assemble the unordered source control entries in the order provided
    ownerIds.forEach(ownerId -> {
      // we may not have SourceControl records for all ownerIds, so the sorted list above may contain nulls,
      // which we have to filter out
      if (sourceControlMap.containsKey(ownerId)) {
        sorted.add(sourceControlMap.get(ownerId));
      }
    });

    return sorted;
  }

  public List<SourceControl> getByApplication() {
    try (TransactionContext tx = createTransactionContext()) {
      return tx.dsl()
          .selectFrom(SOURCE_CONTROL)
          .where(SOURCE_CONTROL.REPOSITORY_URL.isNotNull())
          .fetch(this::toEntity);
    }
  }

  /**
   * Get all the application-level source control entries which are under a given organization
   * <br/>
   * It filters out apps with app-level tokens. We're interested in apps that can be loaded using the
   * existing org tokens, otherwise they may be on custom hosts
   */
  public List<SourceControl> getApplicationSourceControlsByOrganizationWithRepositories(String orgId) {
    final Organization organization = organizationDAO.getByIdNotNull(orgId);
    final Set<String> descendantApplicationIds = ownerDAO.getDescendantOrSelfApplicationIds(organization);
    if (CollectionUtils.isEmpty(descendantApplicationIds)) {
      return Collections.emptyList();
    }

    return getByOwnerIds(new ArrayList<>(descendantApplicationIds))
        .stream()
        .filter(sc -> sc.getRepositoryUrl() != null && sc.getToken() == null)
        .collect(Collectors.toList());
  }

  /**
   * Gets a list of source control entries for applications that do not override
   * the root token/provider anywhere in their hierarchy (ie: at the app or org level)
   *
   * @return list of source controls for apps
   */
  public List<SourceControl> getApplicationSourceControlsWithInheritedCredentials() {
    List<SourceControl> result = new ArrayList<>();

    // first get a list of app source controls that haven't overridden credentials
    List<SourceControl> candidateAppSourceControlList;
    try (TransactionContext tx = createTransactionContext()) {
      candidateAppSourceControlList = tx.dsl()
          .selectFrom(SOURCE_CONTROL)
          .where(SOURCE_CONTROL.REPOSITORY_URL.isNotNull())
          .and(SOURCE_CONTROL.TOKEN.isNull())
          .and(SOURCE_CONTROL.PROVIDER.isNull())
          .fetch(this::toEntity);
    }

    // next get a set of org source control IDs that have overridden credentials
    Set<String> ownerIdsThatOverrideCredentials = getOwnerIdsForSourceControlsWithOverriddenCredentials();

    // filter out the candidates that have overridden credentials somewhere in their hierarchy
    OwnerHierarchyHelper hierarchyHelper = new OwnerHierarchyHelper(ownerDAO);
    for (SourceControl sourceControl : candidateAppSourceControlList) {
      List<String> ownerIds = hierarchyHelper.getHierarchyIds(sourceControl.getOwnerId());
      if (Collections.disjoint(ownerIds, ownerIdsThatOverrideCredentials)) {
        result.add(sourceControl);
      }
    }

    return result;
  }

  private Set<String> getOwnerIdsForSourceControlsWithOverriddenCredentials() {
    Set<String> result = new HashSet<>();

    try (TransactionContext txn = createTransactionContext()) {
      List<String> ownerIdsForSourceControlsWithOverriddenCredentials = txn.dsl()
          .select(SOURCE_CONTROL.OWNER_ID)
          .from(SOURCE_CONTROL)
          .where(SOURCE_CONTROL.OWNER_ID.ne(ROOT_ORGANIZATION_ID))
          .and(SOURCE_CONTROL.TOKEN.isNotNull().or(SOURCE_CONTROL.PROVIDER.isNotNull()))
          .fetchInto(String.class);

      if (!CollectionUtils.isEmpty(ownerIdsForSourceControlsWithOverriddenCredentials)) {
        result.addAll(ownerIdsForSourceControlsWithOverriddenCredentials);
      }
    }

    return result;
  }

  private List<SourceControl> getByOrganization() {
    try (TransactionContext tx = createTransactionContext()) {
      return tx.dsl()
          .selectFrom(SOURCE_CONTROL)
          .where(SOURCE_CONTROL.REPOSITORY_URL.isNull())
          .fetch(this::toEntity);
    }
  }

  public List<SourceControl> getApplicationsWithRemediationPullRequestsEnabled() {
    // an application is enabled if it has a valid repository_url and remediation_pull_requests_enabled is set at the
    // application, parent organization, or root organization level

    Map<String, Application> appsById = applicationDAO.getAll()
        .stream()
        .collect(Collectors.toMap(Application::getId, Function.identity()));

    Map<String, Organization> orgsById = organizationDAO.getAll()
        .stream()
        .collect(Collectors.toMap(Organization::getId, Function.identity()));

    Map<String, SourceControl> orgSourceControlsByOrgId = getByOrganization()
        .stream()
        .collect(Collectors.toMap(SourceControl::getOwnerId, Function.identity()));

    return getByApplication()
        .stream()
        .filter(
            application -> areRemediationPullRequestsEnabled(application, appsById, orgsById, orgSourceControlsByOrgId))
        .collect(Collectors.toList());
  }

  private boolean areRemediationPullRequestsEnabled(
      final SourceControl sourceControl,
      final Map<String, Application> applicationsById,
      final Map<String, Organization> organizationsById,
      final Map<String, SourceControl> orgSourceControlsByOrgId)
  {
    if (sourceControl.getRemediationPullRequestsEnabled() != null) {
      return sourceControl.getRemediationPullRequestsEnabled();
    }

    // application did not define a value, so check organization
    String orgId = applicationsById.get(sourceControl.getOwnerId()).getOrganizationId();

    return getRemediationPullRequestsEnabled(orgId, organizationsById, orgSourceControlsByOrgId);
  }

  private boolean getRemediationPullRequestsEnabled(
      final String orgId,
      final Map<String, Organization> organizationsById,
      final Map<String, SourceControl> orgSourceControlsByOrgId)
  {
    if (orgSourceControlsByOrgId.containsKey(orgId)) {
      SourceControl orgSourceControl = orgSourceControlsByOrgId.get(orgId);
      if (orgSourceControl.getRemediationPullRequestsEnabled() != null) {
        return orgSourceControl.getRemediationPullRequestsEnabled();
      }
    }
    final Organization organization = organizationsById.get(orgId);
    if (organization != null) {
      return getRemediationPullRequestsEnabled(organization.getParentOwnerId(), organizationsById,
          orgSourceControlsByOrgId);
    }
    // could not find a defined value
    return false;
  }

  @Override
  public void insert(final TransactionContext tx, final SourceControl sourceControl) {
    validate(tx, sourceControl);
    setDefaultsAsNecessary(sourceControl);
    super.insert(tx, sourceControl);
  }

  public void updateWithoutValidation(final SourceControl sourceControl) {
    try (TransactionContext tx = createTransactionContext()) {
      tx.begin();
      updateWithoutValidation(tx, sourceControl);
      tx.commit();
    }
  }

  public void updateWithoutValidation(final TransactionContext tx, final SourceControl sourceControl) {
    setDefaultsAsNecessary(sourceControl);

    SourceControl existingSourceControl = getById(tx, sourceControl.getId());
    if (!Objects.equals(sourceControl.getRepositoryUrl(), existingSourceControl.getRepositoryUrl())
        && !StringUtils.isBlank(existingSourceControl.getRepositoryUrl()))
    {
      // If the repository URL has changed, clear the SSH URL
      sourceControl.setRepositorySshUrl(null);
      // if other SC entries have the same repo URL, clear their PRs
      List<SourceControl> sourceControlsWithSameRepositoryUrl =
          getByRepositoryUrl(tx, existingSourceControl.getRepositoryUrl());
      if (sourceControlsWithSameRepositoryUrl.size() == 1) {
        // This is the only SourceControl with the old repository URL.
        // Delete all SourceControlPullRequests for this repository URL.
        sourceControlPullRequestDAO.deleteByRepositoryUrl(tx, existingSourceControl.getRepositoryUrl());
      }
    }

    super.update(tx, sourceControl);
  }

  @Override
  public void update(final TransactionContext tx, final SourceControl sourceControl) {
    validate(tx, sourceControl);
    updateWithoutValidation(tx, sourceControl);
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
        sourceControlPullRequestDAO.deleteByRepositoryUrl(tx, entity.getRepositoryUrl());
      }
    }

    super.delete(tx, entity);
  }

  public List<SourceControl> getByRepositoryUrl(String repositoryUrl) {
    try (TransactionContext tx = createTransactionContext()) {
      return getByRepositoryUrl(tx, repositoryUrl);
    }
  }

  private List<SourceControl> getByRepositoryUrl(TransactionContext tx, String repositoryUrl) {
    if (repositoryUrl == null) {
      return Collections.emptyList();
    }

    repositoryUrl = SourceControl.normalizeRepositoryUrl(repositoryUrl);
    return tx.dsl()
        .selectFrom(SOURCE_CONTROL)
        .where(SOURCE_CONTROL.NORMALIZED_REPOSITORY_URL.eq(repositoryUrl))
        .fetch(this::toEntity);
  }

  public SourceControl getByOwnerId(final String ownerId) {
    try (TransactionContext tx = createTransactionContext()) {
      return getByOwnerId(tx, ownerId);
    }
  }

  public SourceControl getByOwnerId(final TransactionContext tx, final String ownerId) {
    return toEntity(tx.dsl()
        .selectFrom(SOURCE_CONTROL)
        .where(SOURCE_CONTROL.OWNER_ID.eq(ownerId))
        .fetchOne());
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
      // Use updateWithoutValidation since we only changed poll time and error count fields
      updateWithoutValidation(tx, sourceControl);
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

  private static void validateBaseBranchName(String baseBranchName) {
    if (baseBranchName == null || "".equals(baseBranchName)) {
      return;
    }

    try {
      GitBranchNameValidator.validate(baseBranchName);
    }
    catch (InvalidBranchNameException e) {
      throw new BadRequestException(e.getMessage(), e);
    }
  }

  private void validate(final TransactionContext tx, final SourceControl sourceControl) {
    validateBaseBranchName(sourceControl.getBaseBranch());

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

    sourceControlSshValidator.validate(sourceControl);

    validateUsername(tx, sourceControl);
  }

  private void validateRepositoryUrl(final TransactionContext tx, final SourceControl sourceControl) {
    if (StringUtils.isBlank(sourceControl.getRepositoryUrl())) {
      throw new BadRequestException("SourceControl repositoryUrl is required for application");
    }
    try {
      gitApiClientFactory.getGitApiClientUtils(getProvider(tx, sourceControl))
          .createProjectUrl(sourceControl.getNormalizedRepositoryUrl());
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

  /**
   * Builds a composite source control record starting from the given ownerId and looking up the owner hierarchy for
   * missing fields.
   * <br/>
   * Note: The composite source control owner ID can be different from the given owner ID.
   *
   * @param ownerId an application or organization ID
   */
  public SourceControl buildCompositeSourceControlInApplication(final String ownerId) {
    List<String> ownerIds = ownerDAO.getOwnerIds(ownerId);
    if (CollectionUtils.isEmpty(ownerIds)) {
      return null;
    }
    return getCompositeSourceControlByOwnerIds(ownerIds);
  }

  public List<SourceControl> getCompositeSourceControlForOutdatedSourceScans(
      final Date scanLimitDate)
  {
    long externalEvaluationLimitMs = System.currentTimeMillis() -
        (EXTERNAL_EVALUATION_WINDOW_IN_DAYS * 24L * 60 * 60 * 1000);
    Date externalEvaluationLimitDate = new Date(externalEvaluationLimitMs);

    try (TransactionContext tx = createTransactionContext()) {
      List<String> initialOwnerIdList = tx.dsl()
          .resultQuery(
              injectSchemaName(SELECT_APPLICATIONS_FOR_SOURCE_SCAN),
              DSL.val(scanLimitDate, SQLDataType.TIMESTAMP),
              DSL.val(externalEvaluationLimitDate, SQLDataType.TIMESTAMP))
          .fetchInto(String.class);
      return expandToCompositeSourceControlEntries(initialOwnerIdList);
    }
  }

  // @todo - need to leverage the OwnerHierarychHelper to help minimize the DB calls to the OwnerDAO that are
  // done transitively in the forEach call below
  private List<SourceControl> expandToCompositeSourceControlEntries(List<String> initialOwnerIdList) {
    List<SourceControl> result = new ArrayList<>();
    initialOwnerIdList.forEach(ownerId -> result.add(buildCompositeSourceControlInApplication(ownerId)));
    return result;
  }

  /**
   * this is a helper class to TEMPORARILY cache the org hierarchy for applications as we iterate over them
   * so that we only build those parent-child relationships once during an operation in order to eliminate
   * redundant DB calls
   */
  static class OwnerHierarchyHelper
  {
    private final OwnerDAO ownerDAO;

    private Map<String, String> childParentMap = new HashMap<>();

    OwnerHierarchyHelper(OwnerDAO ownerDAO) {
      this.ownerDAO = ownerDAO;
    }

    List<String> getHierarchyIds(final String childId) {
      populateHierarchyIds(childId);

      List<String> orderedHierarchy = new ArrayList<>();
      String ownerId = childId;
      while (null != ownerId) {
        orderedHierarchy.add(ownerId);
        ownerId = childParentMap.get(ownerId);
      }
      return orderedHierarchy;
    }

    void populateHierarchyIds(final String childId) {
      if (null == childId) {
        return;
      }
      if (!childParentMap.containsKey(childId)) {
        // we haven't traversed this child's ancestry yet
        String ownerId = getOwnerFromDb(childId);
        childParentMap.put(childId, ownerId);
        populateHierarchyIds(ownerId);
      }
    }

    private String getOwnerFromDb(final String childId) {
      Owner owner = ownerDAO.getById(childId);
      return null != owner ? owner.getParentOwnerId() : null;
    }
  }

  @Override
  public Table<?> getJooqTable() {
    return SOURCE_CONTROL;
  }

  @Override
  public Class<SourceControl> getEntityClass() {
    return SourceControl.class;
  }
}
