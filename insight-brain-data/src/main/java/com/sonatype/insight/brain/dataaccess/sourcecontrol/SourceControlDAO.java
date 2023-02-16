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
import com.sonatype.insight.brain.db.OperationalDataStoreProvider;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControl;
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.NotFoundException;
import com.sonatype.nexus.scm.GitApiClientFactory;
import com.sonatype.nexus.scm.SourceControlProvider;

import org.apache.commons.lang3.StringUtils;
import org.apache.shiro.util.CollectionUtils;

import static com.sonatype.insight.brain.model.Organization.ROOT_ORGANIZATION_ID;
import static org.apache.commons.lang3.StringUtils.isBlank;

public class SourceControlDAO
    extends AbstractOperationalSqlDAO<SourceControl>
{
  // Visible for tests
  static final long PULL_REQUEST_POLLING_INITIAL_OFFSET_MS = 1000L * 60 * 60 * 72; // 72 hours

  private final ApplicationDAO applicationDAO = new ApplicationDAO();

  private final OrganizationDAO organizationDAO = new OrganizationDAO();

  private final OwnerDAO ownerDAO = new OwnerDAO();

  private final GitApiClientFactory gitApiClientFactory = new GitApiClientFactory();

  // The '_SCHEMA_' string will be replaced by the proper schema at runtime.
  private static final String SELECT_APPLICATIONS_FOR_SOURCE_SCAN =
      "SELECT sc.owner_id " +
      "FROM _SCHEMA_.source_control sc " +
      "JOIN application a ON sc.owner_id = a.application_id " +
      "LEFT JOIN ( " +
      "   SELECT pe.application_id, pe.time, pe.scan_trigger_type " +
      "     FROM _SCHEMA_.last_policy_evaluation lpe " +
      "     JOIN _SCHEMA_.policy_evaluation pe ON pe.policy_evaluation_id = lpe.policy_evaluation_id" +
      "     WHERE lpe.stage_type_id='source' " +
      ") lpe ON lpe.application_id = sc.owner_id " +
      "WHERE ( lpe.time < ?1 " +
      "        AND lpe.scan_trigger_type " +
      "           IN ('SOURCE_CONTROL_INTERNAL_ONBOARDING', 'SOURCE_CONTROL_INTERNAL_DEFAULT_BRANCH_MONITORING', " +
      "                'SOURCE_CONTROL_INTERNAL_PULL_REQUEST'))" +
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
      txn.createNativeQuery(
          "UPDATE " + OperationalDataStoreProvider.getDatabaseSchema() + ".source_control sc" +
              " SET pull_request_poll_time = (" +
              " SELECT" +
              "  CASE WHEN first_commit_time IS NULL THEN ?1" +
              "       WHEN first_commit_time < ?1 THEN first_commit_time" +
              "       ELSE ?1" +
              "       END" +
              " FROM (" +
              "     SELECT application_id, min(time) AS first_commit_time" +
              "     FROM " + OperationalDataStoreProvider.getDatabaseSchema() + ".policy_evaluation" +
              "     WHERE commit_hash IS NOT NULL" +
              "     GROUP BY application_id" +
              "     ) AS first_policy_eval_commit" +
              " WHERE sc.owner_id = first_policy_eval_commit.application_id)" +
              " WHERE sc.pull_request_poll_time IS NULL;"
      ).setParameter(1, defaultPollingTime).executeUpdate();
      txn.commit();
    }
  }

  private void setDefaultPullRequestPollTimes(Date defaultPollingTime) {
    try (TransactionContext txn = createTransactionContext()) {
      txn.begin();
      txn.createNativeQuery(
          "UPDATE " + OperationalDataStoreProvider.getDatabaseSchema() +
              ".source_control SET pull_request_poll_time = ?1" +
              " WHERE pull_request_poll_time IS NULL AND repository_url IS NOT NULL;"
      ).setParameter(1, defaultPollingTime).executeUpdate();
      txn.commit();
    }
  }

  private void clearExtraneousPullRequestPollTimes() {
    try (TransactionContext txn = createTransactionContext()) {
      txn.begin();

      // set poll time to null where repo url is null
      txn.createNativeQuery(
          "UPDATE " + OperationalDataStoreProvider.getDatabaseSchema() +
              ".source_control SET pull_request_poll_time = NULL WHERE repository_url IS NULL;"
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

  @Override
  public SourceControl getById(final TransactionContext tx, final String id) {
    return get(tx, "SELECT entity FROM SourceControl entity WHERE entity.id=?1", id);
  }

  private SourceControl getCompositeSourceControlByOwnerIds(List<String> ownerIds) {
    SourceControl sourceControl = new SourceControl();

    // we can't guarantee that the list of source control entries will be in the proper order so we'll order them here
    orderByHierarchy(ownerIds, getByOwnerIds(ownerIds)).forEach(sc -> SourceControl.coalesce(sourceControl, sc));

    return sourceControl;
  }

  private List<SourceControl> getByOwnerIds(final List<String> ownerIds) {
    return getList("SELECT entity FROM SourceControl entity WHERE entity.ownerId IN ?1", ownerIds);
  }

  private List<SourceControl> orderByHierarchy(List<String> ownerIds, List<SourceControl> unordered) {
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
    String query = "SELECT entity " +
        "FROM SourceControl entity " +
        "WHERE entity.repositoryUrl IS NOT NULL AND entity.token IS NULL AND entity.provider IS NULL";

    List<SourceControl> candidateAppSourceControlList = getList(query);

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
      txn.begin();

      javax.persistence.Query query = txn.createNativeQuery(injectSchemaName(
          "SELECT owner_id FROM _SCHEMA_.source_control " +
              "WHERE owner_id != ?1 AND (token IS NOT NULL OR provider IS NOT NULL);"));
      query.setParameter(1, ROOT_ORGANIZATION_ID);

      List<String> ownerIdsForSourceControlsWithOverriddenCredentials = query.getResultList();

      if (!CollectionUtils.isEmpty(ownerIdsForSourceControlsWithOverriddenCredentials)) {
        result.addAll(ownerIdsForSourceControlsWithOverriddenCredentials);
      }
    }

    return result;
  }

  private List<SourceControl> getByOrganization() {
    String query = "SELECT entity FROM SourceControl entity WHERE entity.repositoryUrl IS NULL";

    return getList(query);
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
        .filter( application ->
            areRemediationPullRequestsEnabled(application, appsById, orgsById, orgSourceControlsByOrgId))
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
    try (TransactionContext tx = createTransactionContext()) {
      return getByRepositoryUrl(tx, repositoryUrl);
    }
  }

  private List<SourceControl> getByRepositoryUrl(TransactionContext tx, String repositoryUrl) {
    if (repositoryUrl == null) {
      return Collections.emptyList();
    }

    repositoryUrl = SourceControl.normalizeRepositoryUrl(repositoryUrl);
    String sQuery = "SELECT entity FROM SourceControl entity WHERE entity.normalizedRepositoryUrl=?1";
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
    List<String> ownerIds = ownerDAO.getOwnerIds(applicationId);
    if (CollectionUtils.isEmpty(ownerIds)) {
      return null;
    }
    return getCompositeSourceControlByOwnerIds(ownerIds);
  }

  public List<SourceControl> getCompositeSourceControlForOutdatedSourceScans(
      final Date scanLimitDate)
  {
    try (TransactionContext tx = createTransactionContext()) {
      javax.persistence.Query query = tx.createNativeQuery(injectSchemaName(SELECT_APPLICATIONS_FOR_SOURCE_SCAN));
      query.setParameter(1, scanLimitDate);
      List<String> initialOwnerIdList = query.getResultList();
      return expandToCompositeSourceControlEntries(initialOwnerIdList);
    }
  }

  // @todo - need to leverage the OwnerHierarychHelper to help minimize the DB calls to the OwnerDAO that are
  // done transitively in the forEach call below
  private List<SourceControl> expandToCompositeSourceControlEntries(List<String> initialOwnerIdList) {
    List<SourceControl> result = new ArrayList<>();
    initialOwnerIdList.forEach(ownerId -> result.add(getCompositeSourceControlByApplicationId(ownerId)));
    return result;
  }

  private String injectSchemaName(final String sql) {
    return sql.replace("_SCHEMA_", OperationalDataStoreProvider.getDatabaseSchema());
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
}
