/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Provider;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.dataaccess.configuration.CiIntegrationsConfigDao;
import com.sonatype.insight.brain.dataaccess.configuration.CpeMatchingConfigurationDAO;
import com.sonatype.insight.brain.dataaccess.configuration.ProprietaryConfigDAO;
import com.sonatype.insight.brain.dataaccess.configuration.ScanHealthConfigDAO;
import com.sonatype.insight.brain.dataaccess.configuration.VersionEvaluationWindowDAO;
import com.sonatype.insight.brain.dataaccess.label.LabelDAO;
import com.sonatype.insight.brain.dataaccess.license.LicenseThreatGroupDAO;
import com.sonatype.insight.brain.dataaccess.policy.AutoPolicyWaiverDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyWaiverRequestDAO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryConnectionDAO;
import com.sonatype.insight.brain.dataaccess.sast.SastScanDAO;
import com.sonatype.insight.brain.dataaccess.search.SearchIndexManager;
import com.sonatype.insight.brain.dataaccess.security.MembershipMappingDAO;
import com.sonatype.insight.brain.dataaccess.sourcecontrol.SourceControlDAO;
import com.sonatype.insight.brain.dataaccess.successmetrics.PolicyViolationAggregationDAO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyFileDAO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartySbomMetadataDAO;
import com.sonatype.insight.brain.db.datastore.OperationalDataStore;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.ApplicationRiskDTO;
import com.sonatype.insight.brain.model.InvalidNameException;
import com.sonatype.insight.brain.model.NameHelper;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.SearchIndexChange;
import com.sonatype.insight.brain.model.SearchIndexChange.ChangeType;
import com.sonatype.insight.brain.model.configuration.CpeMatchingConfiguration;
import com.sonatype.insight.brain.model.configuration.ProprietaryConfig;
import com.sonatype.insight.brain.model.label.Label;
import com.sonatype.insight.brain.model.license.LicenseThreatGroup;
import com.sonatype.insight.brain.model.policy.AutoPolicyWaiver;
import com.sonatype.insight.brain.model.policy.PolicyWaiverRequest;
import com.sonatype.insight.brain.model.repository.RepositoryConnection;
import com.sonatype.insight.brain.model.security.MembershipMapping;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControl;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartySbomMetadata;
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.error.exception.NotFoundException;

import com.google.common.collect.ImmutableSortedSet;
import org.apache.commons.lang3.StringUtils;
import org.jooq.impl.DSL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.sonatype.insight.brain.jooq.generated.ods.tables.Application.APPLICATION;
import static com.sonatype.insight.brain.jooq.generated.ods.tables.ApplicationAncestor.APPLICATION_ANCESTOR;
import static com.sonatype.insight.brain.jooq.generated.ods.tables.ApplicationTag.APPLICATION_TAG;
import static com.sonatype.insight.brain.jooq.generated.ods.tables.Label.LABEL;
import static com.sonatype.insight.brain.jooq.generated.ods.tables.SourceControl.SOURCE_CONTROL;

@Named
@Singleton
public class ApplicationDAO
    extends AbstractOperationalSqlDAO<Application>
{
  private static final Logger log = LoggerFactory.getLogger(ApplicationDAO.class);

  private static final Pattern WHITESPACE_PATTERN = Pattern.compile("\\s");

  public static final int MAX_PUBLIC_ID_LENGTH = 200;

  private final Provider<SourceControlDAO> sourceControlDAOProvider;

  private final Provider<LicenseThreatGroupDAO> licenseThreatGroupDAOProvider;

  private final Provider<LabelDAO> labelDAOProvider;

  private final Provider<PolicyDAO> policyDAOProvider;

  private final Provider<OwnerDAO> ownerDAOProvider;

  private final ProprietaryConfigDAO proprietaryConfigDAO;

  private final MembershipMappingDAO membershipMappingDAO;

  private final PolicyViolationAggregationDAO policyViolationAggregationDAO;

  private final RepositoryConnectionDAO repositoryConnectionDAO;

  private final SastScanDAO sastScanDAO;

  private final ThirdPartySbomMetadataDAO thirdPartySbomMetadataDAO;

  private final ThirdPartyFileDAO thirdPartyFileDAO;

  private final AutoPolicyWaiverDAO autoPolicyWaiverDAO;

  private final Provider<PolicyWaiverRequestDAO> policyWaiverRequestDAOProvider;

  private final CpeMatchingConfigurationDAO cpeMatchingConfigurationDAO;

  private final CiIntegrationsConfigDao ciIntegrationsConfigDao;

  private final OrganizationDAO organizationDAO;

  private final VersionEvaluationWindowDAO versionEvaluationWindowDAO;

  private final ScanHealthConfigDAO scanHealthConfigDAO;

  private final TemporaryTableHelper temporaryTableHelper;

  @Inject
  public ApplicationDAO(
      final OperationalDataStore operationalDataStore,
      final SearchIndexManager searchIndexManager,
      final Provider<SourceControlDAO> sourceControlDAOProvider,
      final Provider<LicenseThreatGroupDAO> licenseThreatGroupDAOProvider,
      final Provider<LabelDAO> labelDAOProvider,
      final Provider<PolicyDAO> policyDAOProvider,
      final Provider<OwnerDAO> ownerDAOProvider,
      final ProprietaryConfigDAO proprietaryConfigDAO,
      final MembershipMappingDAO membershipMappingDAO,
      final PolicyViolationAggregationDAO policyViolationAggregationDAO,
      final RepositoryConnectionDAO repositoryConnectionDAO,
      final SastScanDAO sastScanDAO,
      final ThirdPartySbomMetadataDAO thirdPartySbomMetadataDAO,
      final ThirdPartyFileDAO thirdPartyFileDAO,
      final AutoPolicyWaiverDAO autoPolicyWaiverDAO,
      final Provider<PolicyWaiverRequestDAO> policyWaiverRequestDAOProvider,
      final CpeMatchingConfigurationDAO cpeMatchingConfigurationDAO,
      final CiIntegrationsConfigDao ciIntegrationsConfigDao,
      final OrganizationDAO organizationDAO,
      final VersionEvaluationWindowDAO versionEvaluationWindowDAO,
      final ScanHealthConfigDAO scanHealthConfigDAO,
      final TemporaryTableHelper temporaryTableHelper)
  {
    super(operationalDataStore, searchIndexManager);
    this.sourceControlDAOProvider = sourceControlDAOProvider;
    this.licenseThreatGroupDAOProvider = licenseThreatGroupDAOProvider;
    this.labelDAOProvider = labelDAOProvider;
    this.policyDAOProvider = policyDAOProvider;
    this.ownerDAOProvider = ownerDAOProvider;
    this.proprietaryConfigDAO = proprietaryConfigDAO;
    this.membershipMappingDAO = membershipMappingDAO;
    this.policyViolationAggregationDAO = policyViolationAggregationDAO;
    this.repositoryConnectionDAO = repositoryConnectionDAO;
    this.sastScanDAO = sastScanDAO;
    this.thirdPartySbomMetadataDAO = thirdPartySbomMetadataDAO;
    this.thirdPartyFileDAO = thirdPartyFileDAO;
    this.autoPolicyWaiverDAO = autoPolicyWaiverDAO;
    this.policyWaiverRequestDAOProvider = policyWaiverRequestDAOProvider;
    this.cpeMatchingConfigurationDAO = cpeMatchingConfigurationDAO;
    this.ciIntegrationsConfigDao = ciIntegrationsConfigDao;
    this.organizationDAO = organizationDAO;
    this.versionEvaluationWindowDAO = versionEvaluationWindowDAO;
    this.scanHealthConfigDAO = scanHealthConfigDAO;
    this.temporaryTableHelper = temporaryTableHelper;
  }

  @Override
  public org.jooq.Table<?> getJooqTable() {
    return APPLICATION;
  }

  @Override
  public Class<Application> getEntityClass() {
    return Application.class;
  }

  public Application getByPublicId(TransactionContext tx, String publicId) {
    if (publicId == null || publicId.trim().isEmpty()) {
      throw new DataAccessException("The application public ID cannot be null or empty.");
    }

    publicId = normalizePublicId(publicId);
    return toEntity(tx.dsl()
        .selectFrom(APPLICATION)
        .where(APPLICATION.PUBLIC_ID_LOWERCASE.eq(publicId))
        .fetchOne());
  }

  public Application getByPublicId(String publicId) {
    try (TransactionContext tx = createTransactionContext()) {
      return getByPublicId(tx, publicId);
    }
  }

  public Application getByPublicIdNotNull(String publicId) {
    try (TransactionContext tx = createTransactionContext()) {
      return getByPublicIdNotNull(tx, publicId);
    }
  }

  public Application getByPublicIdNotNull(TransactionContext tx, String publicId) {
    Application application = getByPublicId(tx, publicId);
    if (application == null) {
      throw new NotFoundException("Could not find an application with public ID " + publicId + ".");
    }
    return application;
  }

  /**
   * Returns the ancestor IDs for an application identified by its public ID, using a single query that joins the
   * application table with the application_ancestor view. The result is ordered by ancestor_distance (child to parent).
   *
   * @param publicId the application's public ID (case-insensitive)
   * @return ancestor IDs ordered by distance, or empty list if the application is not found
   */
  public List<String> getAncestorIdsByPublicId(String publicId) {
    publicId = normalizePublicId(publicId);
    try (TransactionContext tx = createTransactionContext()) {
      return tx.dsl()
          .select(APPLICATION_ANCESTOR.ANCESTOR_ID)
          .from(APPLICATION_ANCESTOR)
          .join(APPLICATION)
          .on(APPLICATION.APPLICATION_ID.eq(APPLICATION_ANCESTOR.APPLICATION_ID))
          .where(APPLICATION.PUBLIC_ID_LOWERCASE.eq(publicId))
          .orderBy(APPLICATION_ANCESTOR.ANCESTOR_DISTANCE)
          .fetch(APPLICATION_ANCESTOR.ANCESTOR_ID);
    }
  }

  public Application getByName(TransactionContext tx, String name) {
    if (name == null || name.trim().isEmpty()) {
      throw new DataAccessException("The application name cannot be null or empty.");
    }
    // Application Name is whitespace and case insensitive
    name = NameHelper.normalize(name);
    return toEntity(tx.dsl()
        .selectFrom(APPLICATION)
        .where(APPLICATION.NAME_LOWERCASE_NO_WHITESPACE.eq(name))
        .fetchOne());
  }

  public Application getByName(String name) {
    try (TransactionContext tx = createTransactionContext()) {
      return getByName(tx, name);
    }
  }

  public List<Application> getByContactInternalName(String contactInternalName) {
    try (TransactionContext tx = createTransactionContext()) {
      return tx.dsl()
          .selectFrom(APPLICATION)
          .where(APPLICATION.CONTACT_INTERNAL_NAME.eq(contactInternalName))
          .fetch(this::toEntity);
    }
  }

  @Override
  public List<Application> getAll(TransactionContext tx) {
    return tx.dsl()
        .selectFrom(APPLICATION)
        .orderBy(APPLICATION.PUBLIC_ID_LOWERCASE)
        .fetch(this::toEntity);
  }

  @Override
  public List<Application> getAll() {
    try (TransactionContext tx = createTransactionContext()) {
      return getAll(tx);
    }
  }

  public List<Application> getAll(
      final int page,
      final int pageSize)
  {
    try (TransactionContext tx = createTransactionContext()) {
      return getAll(tx, page, pageSize);
    }
  }

  public List<Application> getAll(
      final TransactionContext tx,
      final int page,
      final int pageSize)
  {
    int offset = (page - 1) * pageSize;
    return tx.dsl()
        .selectFrom(APPLICATION)
        .orderBy(APPLICATION.PUBLIC_ID_LOWERCASE)
        .offset(offset)
        .limit(pageSize)
        .fetch(this::toEntity);
  }

  public List<Application> getAllOrderedByName(TransactionContext tx) {
    return tx.dsl()
        .selectFrom(APPLICATION)
        .orderBy(APPLICATION.NAME)
        .fetch(this::toEntity);
  }

  public List<Application> getAllOrderedByName() {
    try (TransactionContext tx = createTransactionContext()) {
      return getAllOrderedByName(tx);
    }
  }

  public List<Application> getAllWithoutRelatedRepositoriesOrderedByName() {
    try (TransactionContext tx = createTransactionContext()) {
      return tx.dsl()
          .select(APPLICATION.fields())
          .from(APPLICATION)
          .join(com.sonatype.insight.brain.jooq.generated.ods.tables.Organization.ORGANIZATION)
          .on(APPLICATION.ORGANIZATION_ID.eq(
              com.sonatype.insight.brain.jooq.generated.ods.tables.Organization.ORGANIZATION.ORGANIZATION_ID))
          .where(com.sonatype.insight.brain.jooq.generated.ods.tables.Organization.ORGANIZATION.RELATED_REPOSITORY_ID
              .isNull()
              .and(
                  com.sonatype.insight.brain.jooq.generated.ods.tables.Organization.ORGANIZATION.RELATED_REPOSITORY_MANAGER_ID
                      .isNull()))
          .orderBy(APPLICATION.NAME)
          .fetch(this::toEntity);
    }
  }

  public List<Application> getAllWithoutRelatedRepositories(TransactionContext tx) {
    return tx.dsl()
        .select(APPLICATION.fields())
        .from(APPLICATION)
        .join(com.sonatype.insight.brain.jooq.generated.ods.tables.Organization.ORGANIZATION)
        .on(APPLICATION.ORGANIZATION_ID.eq(
            com.sonatype.insight.brain.jooq.generated.ods.tables.Organization.ORGANIZATION.ORGANIZATION_ID))
        .where(com.sonatype.insight.brain.jooq.generated.ods.tables.Organization.ORGANIZATION.RELATED_REPOSITORY_ID
            .isNull()
            .and(
                com.sonatype.insight.brain.jooq.generated.ods.tables.Organization.ORGANIZATION.RELATED_REPOSITORY_MANAGER_ID
                    .isNull()))
        .orderBy(APPLICATION.PUBLIC_ID_LOWERCASE)
        .fetch(this::toEntity);
  }

  public List<Application> getAllWithoutRelatedRepositories() {
    try (TransactionContext tx = createTransactionContext()) {
      return getAllWithoutRelatedRepositories(tx);
    }
  }

  public List<Application> getByOrganizationId(TransactionContext tx, String organizationId) {
    return tx.dsl()
        .selectFrom(APPLICATION)
        .where(APPLICATION.ORGANIZATION_ID.eq(organizationId))
        .orderBy(APPLICATION.PUBLIC_ID_LOWERCASE)
        .fetch(this::toEntity);
  }

  public List<Application> getByOrganizationIds(Set<String> organizationIds) {
    return getListWithSqlInClause(new ArrayList<>(organizationIds), ids -> {
      try (TransactionContext tx = createTransactionContext()) {
        return tx.dsl()
            .selectFrom(APPLICATION)
            .where(APPLICATION.ORGANIZATION_ID.in(ids))
            .fetch(this::toEntity);
      }
    }, getDataStore());
  }

  public List<Application> getByOrganizationId(String organizationId) {
    try (TransactionContext tx = createTransactionContext()) {
      return getByOrganizationId(tx, organizationId);
    }
  }

  public List<Application> getByAncestorId(String organizationId) {
    try (TransactionContext tx = createTransactionContext()) {
      return getByAncestorId(tx, organizationId);
    }
  }

  public List<Application> getByAncestorId(TransactionContext tx, String organizationId) {
    return tx.dsl()
        .select(APPLICATION.fields())
        .from(APPLICATION)
        .join(APPLICATION_ANCESTOR)
        .on(APPLICATION_ANCESTOR.APPLICATION_ID.eq(APPLICATION.APPLICATION_ID))
        .where(APPLICATION_ANCESTOR.ANCESTOR_ID.eq(organizationId))
        .and(APPLICATION_ANCESTOR.APPLICATION_ID.ne(APPLICATION_ANCESTOR.ANCESTOR_ID))
        .fetch(this::toEntity);
  }

  public Set<String> getIdsByAncestorIds(final Set<String> ancestorIds) {
    try (TransactionContext tx = createTransactionContext()) {
      return getIdsByAncestorIds(tx, ancestorIds);
    }
  }

  public Set<String> getIdsByAncestorIds(final TransactionContext tx, final Set<String> ancestorIds) {
    if (ancestorIds.isEmpty()) {
      return Collections.emptySet();
    }
    return new HashSet<>(getListWithSqlInClause(ancestorIds,
        l -> tx.dsl()
            .selectDistinct(APPLICATION_ANCESTOR.APPLICATION_ID)
            .from(APPLICATION_ANCESTOR)
            .where(APPLICATION_ANCESTOR.ANCESTOR_ID.in(l))
            .fetchInto(String.class)));
  }

  public List<Application> getByIdsAndTagIds(Set<String> applicationIds, Set<String> tagIds) {
    try (TransactionContext tx = createTransactionContext()) {
      // Filter out null from tagIds for the main query
      Set<String> nonNullTagIds = tagIds.stream().filter(id -> id != null).collect(Collectors.toSet());

      List<Application> taggedApplications = tx.dsl()
          .selectDistinct(APPLICATION.fields())
          .from(APPLICATION)
          .join(APPLICATION_TAG)
          .on(APPLICATION.APPLICATION_ID.eq(APPLICATION_TAG.APPLICATION_ID))
          .where(APPLICATION.APPLICATION_ID.in(applicationIds))
          .and(APPLICATION_TAG.TAG_ID.in(nonNullTagIds))
          .fetch(this::toEntity);

      if (tagIds.contains(null)) {
        // Get untagged applications
        List<Application> untaggedApplications = tx.dsl()
            .selectFrom(APPLICATION)
            .where(APPLICATION.APPLICATION_ID.in(applicationIds))
            .andNotExists(
                DSL.selectOne()
                    .from(APPLICATION_TAG)
                    .where(APPLICATION_TAG.APPLICATION_ID.eq(APPLICATION.APPLICATION_ID)))
            .fetch(this::toEntity);

        List<Application> retval = new ArrayList<>(taggedApplications);
        retval.addAll(untaggedApplications);
        return retval;
      }

      return taggedApplications;
    }
  }

  public List<Application> getByTagIds(Set<String> tagIds) {
    try (TransactionContext tx = createTransactionContext()) {
      // Filter out null from tagIds for the main query
      Set<String> nonNullTagIds = tagIds.stream().filter(id -> id != null).collect(Collectors.toSet());

      List<Application> taggedApplications = tx.dsl()
          .selectDistinct(APPLICATION.fields())
          .from(APPLICATION)
          .join(APPLICATION_TAG)
          .on(APPLICATION.APPLICATION_ID.eq(APPLICATION_TAG.APPLICATION_ID))
          .where(APPLICATION_TAG.TAG_ID.in(nonNullTagIds))
          .fetch(this::toEntity);

      if (tagIds.contains(null)) {
        // Get untagged applications
        List<Application> untaggedApplications = tx.dsl()
            .selectFrom(APPLICATION)
            .whereNotExists(
                DSL.selectOne()
                    .from(APPLICATION_TAG)
                    .where(APPLICATION_TAG.APPLICATION_ID.eq(APPLICATION.APPLICATION_ID)))
            .fetch(this::toEntity);

        List<Application> retval = new ArrayList<>(taggedApplications);
        retval.addAll(untaggedApplications);
        return retval;
      }

      return taggedApplications;
    }
  }

  public List<Application> getByPublicIds(Set<String> applicationPublicIds) {
    applicationPublicIds =
        applicationPublicIds.stream().map(ApplicationDAO::normalizePublicId).collect(Collectors.toSet());
    try (TransactionContext tx = createTransactionContext()) {
      return tx.dsl()
          .selectFrom(APPLICATION)
          .where(APPLICATION.PUBLIC_ID_LOWERCASE.in(applicationPublicIds))
          .fetch(this::toEntity);
    }
  }

  public List<Application> getByIds(Set<String> applicationIds) {
    try (TransactionContext tx = createTransactionContext()) {
      return tx.dsl()
          .selectFrom(APPLICATION)
          .where(APPLICATION.APPLICATION_ID.in(applicationIds))
          .fetch(this::toEntity);
    }
  }

  public List<Application> getByAncestorIds(
      final Set<String> ancestorIds,
      final int page,
      final int pageSize)
  {
    try (TransactionContext tx = createTransactionContext()) {
      return getByAncestorIds(tx, ancestorIds, page, pageSize);
    }
  }

  public List<Application> getByAncestorIds(
      final TransactionContext tx,
      final Set<String> ancestorIds,
      final int page,
      final int pageSize)
  {
    if (ancestorIds.isEmpty()) {
      return Collections.emptyList();
    }
    if (isDatabaseEmbedded()) {
      return getByAncestorIdsH2(tx, ancestorIds, page, pageSize);
    }
    else {
      return getByAncestorIdsPostgres(tx, ancestorIds, page, pageSize);
    }
  }

  private List<Application> getByAncestorIdsH2(
      final TransactionContext tx,
      final Set<String> ancestorIds,
      final int page,
      final int pageSize)
  {
    int offset = (page - 1) * pageSize;
    boolean splitQuery = ancestorIds.size() > getInOperatorThreshold();
    if (splitQuery) {
      return getListWithSqlInClause(ancestorIds, l -> tx.dsl()
          .selectDistinct(APPLICATION.fields())
          .from(APPLICATION)
          .join(APPLICATION_ANCESTOR)
          .on(APPLICATION.APPLICATION_ID.eq(APPLICATION_ANCESTOR.APPLICATION_ID))
          .where(APPLICATION_ANCESTOR.ANCESTOR_ID.in(l))
          .fetch(this::toEntity)).stream()
              .distinct()
              .sorted(Comparator.comparing(Application::getPublicIdLowercase))
              .skip(offset)
              .limit(pageSize)
              .toList();
    }
    else {
      return tx.dsl()
          .selectDistinct(APPLICATION.fields())
          .from(APPLICATION)
          .join(APPLICATION_ANCESTOR)
          .on(APPLICATION.APPLICATION_ID.eq(APPLICATION_ANCESTOR.APPLICATION_ID))
          .where(APPLICATION_ANCESTOR.ANCESTOR_ID.in(ancestorIds))
          .orderBy(APPLICATION.PUBLIC_ID_LOWERCASE)
          .offset(offset)
          .limit(pageSize)
          .fetch(this::toEntity);
    }
  }

  private List<Application> getByAncestorIdsPostgres(
      final TransactionContext tx,
      final Set<String> ancestorIds,
      final int page,
      final int pageSize)
  {
    int offset = (page - 1) * pageSize;
    return tx.dsl()
        .selectDistinct(APPLICATION.fields())
        .from(APPLICATION)
        .join(APPLICATION_ANCESTOR)
        .on(APPLICATION.APPLICATION_ID.eq(APPLICATION_ANCESTOR.APPLICATION_ID))
        .where(APPLICATION_ANCESTOR.ANCESTOR_ID.in(ancestorIds))
        .orderBy(APPLICATION.PUBLIC_ID_LOWERCASE)
        .offset(offset)
        .limit(pageSize)
        .fetch(this::toEntity);
  }

  @Override
  public void insert(TransactionContext tx, Application application) {
    validate(application);
    validatePublicId(application.getPublicId());

    if (getByName(tx, application.getName()) != null) {
      throw new InvalidNameException(application.getName() + " is already used as a name.");
    }
    if (getByPublicId(tx, application.getPublicId()) != null) {
      throw new InvalidApplicationException(application.getPublicId() + " is already used as an ID.");
    }
    if (getById(tx, application.getPublicId()) != null) {
      throw new InvalidApplicationException(
          application.getPublicId() + " is already used as an internal ID and cannot be used as a public ID.");
    }
    if (organizationDAO.getById(tx, application.getPublicId()) != null) {
      throw new InvalidApplicationException(
          application.getPublicId() + " is already used as an internal ID and cannot be used as a public ID.");
    }
    // Note: repository and repositoryManager internal IDs are system-generated UUIDs never entered by users,
    // so the probability of collision is negligible and we intentionally omit those checks here.

    // Generate ID if not set (from AbstractSqlDAO)
    String id = application.getId();
    if (id == null || id.trim().isEmpty()) {
      application.setId(newUUID());
    }

    // jOOQ insert
    tx.dsl()
        .insertInto(APPLICATION)
        .set(APPLICATION.APPLICATION_ID, application.getId())
        .set(APPLICATION.PUBLIC_ID, application.getPublicId())
        .set(APPLICATION.PUBLIC_ID_LOWERCASE, application.getPublicIdLowercase())
        .set(APPLICATION.NAME, application.getName())
        .set(APPLICATION.NAME_LOWERCASE_NO_WHITESPACE, application.getNameLowercaseNoWhitespace())
        .set(APPLICATION.ORGANIZATION_ID, application.getOrganizationId())
        .set(APPLICATION.CONTACT_INTERNAL_NAME, application.getContactInternalName())
        .set(APPLICATION.LEGACY_VIOLATION_ENABLED, application.isLegacyViolationEnabled())
        .set(APPLICATION.REPOSITORY_CONNECTION_ENABLED, application.isRepositoryConnectionEnabled())
        .set(APPLICATION.ARTIFACTORY_CONNECTION_ENABLED, application.isArtifactoryConnectionEnabled())
        .execute();

    // Handle search index (from AbstractSqlDAO)
    if (shouldAddSearchIndexChange(tx, application)) {
      insertSearchIndexChange(tx, newSearchIndexChangeForInsert(application));
    }
  }

  @Override
  public void update(TransactionContext tx, Application application) {
    update(tx, application, false);
  }

  public void update(TransactionContext tx, Application application, boolean changeParent) {
    validate(application);

    Application existingApplication = getById(tx, application.getId());
    if (existingApplication == null) {
      throw new InvalidApplicationException("Attempting to edit an application that doesn't exist. ID : "
          + application.getPublicId() + ".");
    }
    if (!existingApplication.getPublicId().equals(application.getPublicId())) {
      // Only validate PublicId when it is being changed by this update operation
      // to support invalid public IDs created before the public ID validation was introduced.
      // See test: ApplicationDAOTest.testUpdateApplicationWithInvalidPublicId()
      validatePublicId(application.getPublicId());
      log.info("Application ID: {}, Changing public ID from {} to {}.", existingApplication.getId(),
          existingApplication.getPublicId(),
          application.getPublicId());
      Application collidingById = getById(tx, application.getPublicId());
      if (collidingById != null && !collidingById.getId().equals(application.getId())) {
        throw new InvalidApplicationException(
            application.getPublicId() + " is already used as an internal ID and cannot be used as a public ID.");
      }
      if (organizationDAO.getById(tx, application.getPublicId()) != null) {
        throw new InvalidApplicationException(
            application.getPublicId() + " is already used as an internal ID and cannot be used as a public ID.");
      }
      // Note: repository and repositoryManager internal IDs are system-generated UUIDs never entered by users,
      // so the probability of collision is negligible and we intentionally omit those checks here.
    }
    if (!changeParent && !existingApplication.getOrganizationId().equals(application.getOrganizationId())) {
      throw new InvalidApplicationException("Cannot change the parent organization of an application.");
    }
    existingApplication = getByName(tx, application.getName());
    if (existingApplication != null && !existingApplication.getId().equals(application.getId())) {
      throw new InvalidNameException(application.getName() + " is already used as a name.");
    }
    existingApplication = getByPublicId(tx, application.getPublicId());
    if (existingApplication != null && !existingApplication.getId().equals(application.getId())) {
      throw new InvalidApplicationException(application.getPublicId() + " is already used as an ID.");
    }

    // jOOQ update
    tx.dsl()
        .update(APPLICATION)
        .set(APPLICATION.PUBLIC_ID, application.getPublicId())
        .set(APPLICATION.PUBLIC_ID_LOWERCASE, application.getPublicIdLowercase())
        .set(APPLICATION.NAME, application.getName())
        .set(APPLICATION.NAME_LOWERCASE_NO_WHITESPACE, application.getNameLowercaseNoWhitespace())
        .set(APPLICATION.ORGANIZATION_ID, application.getOrganizationId())
        .set(APPLICATION.CONTACT_INTERNAL_NAME, application.getContactInternalName())
        .set(APPLICATION.LEGACY_VIOLATION_ENABLED, application.isLegacyViolationEnabled())
        .set(APPLICATION.REPOSITORY_CONNECTION_ENABLED, application.isRepositoryConnectionEnabled())
        .set(APPLICATION.ARTIFACTORY_CONNECTION_ENABLED, application.isArtifactoryConnectionEnabled())
        .where(APPLICATION.APPLICATION_ID.eq(application.getId()))
        .execute();

    // Handle search index (from AbstractSqlDAO)
    if (shouldAddSearchIndexChange(tx, application)) {
      insertSearchIndexChange(tx, newSearchIndexChangeForUpdate(application));
    }
  }

  private void validate(Application application) {
    NameHelper.validate("Name", application.getName(), NameHelper.MAX_NAME_LENGTH_APP_ORG);
  }

  private void validatePublicId(String publicId) {
    NameHelper.validate("Public ID", publicId, MAX_PUBLIC_ID_LENGTH);
    if (WHITESPACE_PATTERN.matcher(publicId).find()) {
      throw new InvalidApplicationException("Public ID cannot contain whitespaces.");
    }
    if (".".equals(publicId) || "..".equals(publicId)) {
      throw new InvalidApplicationException("Public ID cannot be '.' or '..'");
    }
  }

  public List<Application> getByOrganizationIdAndLabelLowercase(
      TransactionContext tx,
      String organizationId,
      String labelLowercase)
  {
    return tx.dsl()
        .select(APPLICATION.fields())
        .from(LABEL)
        .join(APPLICATION)
        .on(LABEL.OWNER_ID.eq(APPLICATION.APPLICATION_ID))
        .where(APPLICATION.ORGANIZATION_ID.eq(organizationId))
        .and(LABEL.LABEL_LOWERCASE.eq(labelLowercase))
        .fetch(this::toEntity);
  }

  /**
   * fetches the #Application objects associated with the given repository URL; the association is specified via the
   * #SourceControl entries
   *
   * @return List of #Application objects associated with the given repository URL or an empty list if there are none
   */
  public List<Application> getByRepositoryUrl(String repositoryUrl) {
    if (repositoryUrl != null) {
      repositoryUrl = SourceControl.normalizeRepositoryUrl(repositoryUrl);
    }
    try (TransactionContext tx = createTransactionContext()) {
      return tx.dsl()
          .select(APPLICATION.fields())
          .from(APPLICATION)
          .join(SOURCE_CONTROL)
          .on(APPLICATION.APPLICATION_ID.eq(SOURCE_CONTROL.OWNER_ID))
          .where(SOURCE_CONTROL.NORMALIZED_REPOSITORY_URL.eq(repositoryUrl))
          .fetch(this::toEntity);
    }
  }

  /**
   * Batch variant of {@link #getByRepositoryUrl(String)}. For every input URL it returns the #Application objects
   * associated with that URL via #SourceControl entries, keyed by normalized URL. URLs with no associated applications
   * are simply absent from the map (callers should use {@link Map#getOrDefault} with {@link List#of}).
   * <p>
   * Issues a single IN-clause query (chunked by the inherited #AbstractSqlDAO threshold) instead of one query per URL.
   */
  public Map<String, List<Application>> getByRepositoryUrls(Collection<String> repositoryUrls) {
    if (repositoryUrls == null || repositoryUrls.isEmpty()) {
      return Collections.emptyMap();
    }
    Set<String> normalizedUrls = repositoryUrls.stream()
        .filter(Objects::nonNull)
        .map(SourceControl::normalizeRepositoryUrl)
        .collect(Collectors.toSet());
    if (normalizedUrls.isEmpty()) {
      return Collections.emptyMap();
    }

    try (TransactionContext tx = createTransactionContext()) {
      List<org.jooq.Record> rows = getListWithSqlInClause(normalizedUrls,
          urls -> tx.dsl()
              .select(SOURCE_CONTROL.NORMALIZED_REPOSITORY_URL)
              .select(APPLICATION.fields())
              .from(APPLICATION)
              .join(SOURCE_CONTROL)
              .on(APPLICATION.APPLICATION_ID.eq(SOURCE_CONTROL.OWNER_ID))
              .where(SOURCE_CONTROL.NORMALIZED_REPOSITORY_URL.in(urls))
              .fetch());

      Map<String, List<Application>> result = new HashMap<>();
      for (org.jooq.Record row : rows) {
        String url = row.get(SOURCE_CONTROL.NORMALIZED_REPOSITORY_URL);
        Application app = toEntity(row.into(APPLICATION));
        result.computeIfAbsent(url, k -> new ArrayList<>()).add(app);
      }
      return result;
    }
  }

  /**
   * Efficiently fetch application IDs for all of the provided normalizedRepositoryUrls
   *
   * @return Map of normalizedRepositoryUrl to applicationIds. The sets that are the values of the map will never be
   *         empty, and are sorted alphabetically.
   */
  public Map<String, SortedSet<String>> getApplicationIdsByNormalizedRepositoryUrls(
      Set<String> normalizedRepositoryUrls)
  {
    if (normalizedRepositoryUrls.isEmpty()) {
      return Collections.emptyMap();
    }

    try (TransactionContext tx = createTransactionContext()) {
      final List<org.jooq.Record2<String, String>> results = getListWithSqlInClause(normalizedRepositoryUrls,
          urls -> tx.dsl()
              .select(SOURCE_CONTROL.NORMALIZED_REPOSITORY_URL, SOURCE_CONTROL.OWNER_ID)
              .from(SOURCE_CONTROL)
              .where(SOURCE_CONTROL.NORMALIZED_REPOSITORY_URL.in(urls))
              .fetch());

      final Map<String, SortedSet<String>> retval = results.stream()
          .collect(Collectors.toMap(
              r -> r.value1(),
              r -> ImmutableSortedSet.of(r.value2()),
              (set1, set2) -> {
                // Merge the two sets
                var mergedSet = new TreeSet<>(set1);
                mergedSet.addAll(set2);
                return mergedSet;
              }));

      Set<String> missingUrls = new HashSet<>(normalizedRepositoryUrls);
      missingUrls.removeAll(retval.keySet());

      if (!missingUrls.isEmpty()) {
        throw new IllegalArgumentException("Repository URLs not found: " + missingUrls);
      }

      return retval;
    }
  }

  public static String normalizePublicId(String publicId) {
    return publicId.trim().toLowerCase(Locale.ENGLISH);
  }

  public Application getByIdOrPublicIdNotNull(final String idOrPublicId) {
    Application application = getByIdOrPublicId(idOrPublicId);
    if (application == null) {
      throw new NotFoundException("Cannot find an application with id/public id '" + idOrPublicId + "'.");
    }
    return application;
  }

  public Application getByIdOrPublicId(final String idOrPublicId) {
    try (TransactionContext tx = createTransactionContext()) {
      return getByIdOrPublicId(tx, idOrPublicId);
    }
  }

  private Application getByIdOrPublicId(final TransactionContext tx, final String idOrPublicId) {
    if (StringUtils.isBlank(idOrPublicId)) {
      return null;
    }
    String normalizedIdOrPublicId = normalizePublicId(idOrPublicId);
    return toEntity(tx.dsl()
        .selectFrom(APPLICATION)
        .where(APPLICATION.APPLICATION_ID.eq(idOrPublicId)
            .or(APPLICATION.PUBLIC_ID_LOWERCASE.eq(normalizedIdOrPublicId)))
        .fetchOne());
  }

  @Override
  protected SearchIndexChange newSearchIndexChange(Application entity) {
    return new SearchIndexChange(ChangeType.APPLICATION, entity.getId());
  }

  @Override
  protected boolean shouldAddSearchIndexChange(TransactionContext tx, Application entity) {
    Organization org = organizationDAO.getById(tx, entity.getOrganizationId());
    if (org != null && (org.getRelatedRepositoryManagerId() != null || org.getRelatedRepositoryId() != null)) {
      return false;
    }
    return true;
  }

  @Override
  public void delete(TransactionContext tx, Application application) {
    long start = System.currentTimeMillis();

    // The following entity deletions are cascaded via foreign key ON DELETE CASCADE:
    // - ApplicationTag
    // - PolicyEvaluation
    // - PolicyViolation
    // - SourceControlDefaultBranchCommitHistory
    // - SourceControlEvent
    // - SourceControlPullRequestComment
    // - SourceControlPullRequestResult
    // - SourceControlUser
    // - VersionEvaluationWindow

    // Cascade to source control config
    sourceControlDAOProvider.get().deleteByOwnerId(tx, application.getId());

    // Cascade to license threat groups
    LicenseThreatGroupDAO licenseThreatGroupDAO = this.licenseThreatGroupDAOProvider.get();
    List<LicenseThreatGroup> licenseThreatGroups = licenseThreatGroupDAO.getByOwnerId(tx, application.getId());
    for (LicenseThreatGroup licenseThreatGroup : licenseThreatGroups) {
      licenseThreatGroupDAO.delete(tx, licenseThreatGroup);
    }

    // Cascade to labels
    LabelDAO labelDAO = labelDAOProvider.get();
    List<Label> labels = labelDAO.getByOwnerId(tx, application.getId());
    for (Label label : labels) {
      labelDAO.delete(tx, label);
    }

    PolicyWaiverRequestDAO policyWaiverRequestDAO = policyWaiverRequestDAOProvider.get();
    List<PolicyWaiverRequest> policyWaiverRequests =
        policyWaiverRequestDAO.getByOwnerId(tx, application.getId());
    for (PolicyWaiverRequest policyWaiverRequest : policyWaiverRequests) {
      policyWaiverRequestDAO.delete(tx, policyWaiverRequest);
    }

    // Cascade to policies
    policyDAOProvider.get().deleteByOwnerId(tx, application.getId());

    // Cascade to owned entities
    ownerDAOProvider.get().cascadeDelete(tx, application);

    // Cascade to proprietary config
    ProprietaryConfig proprietaryConfig = proprietaryConfigDAO.getByOwnerId(tx, application.getId());
    if (proprietaryConfig != null) {
      proprietaryConfigDAO.delete(tx, proprietaryConfig);
    }

    // Cascade to CI integrations config
    ciIntegrationsConfigDao.delete(tx, "APPLICATION", application.getId());

    // Cascade to scan health config
    scanHealthConfigDAO.deleteByOwnerId(tx, application.getId());

    // Cascade to SastScan table
    sastScanDAO.deleteByApplicationId(tx, application.getId());

    // Delete application from database
    tx.dsl()
        .deleteFrom(APPLICATION)
        .where(APPLICATION.APPLICATION_ID.eq(application.getId()))
        .execute();

    // Handle search index (from AbstractSqlDAO)
    if (shouldAddSearchIndexChange(tx, application)) {
      insertSearchIndexChange(tx, newSearchIndexChangeForDelete(application));
    }

    // Cascade to membership mappings
    for (MembershipMapping membershipMapping : membershipMappingDAO.getByContextId(tx, application.getId())) {
      membershipMappingDAO.delete(tx, membershipMapping);
    }

    // Cascade to aggregation tables. These are in a separate schema and therefore use a separate transaction.
    try (TransactionContext aggregationTx = policyViolationAggregationDAO.createTransactionContext()) {
      aggregationTx.begin();

      policyViolationAggregationDAO.deleteByApplicationId(aggregationTx, application.getId());

      aggregationTx.commit();
    }

    // Cascade to aggregation tables. These are in a separate schema and therefore use a separate transaction.
    try (TransactionContext thirdPartyScansTx = thirdPartyFileDAO.createTransactionContext()) {
      thirdPartyScansTx.begin();

      List<ThirdPartySbomMetadata> thirdPartySbomMetadataList =
          thirdPartySbomMetadataDAO.getByApplicationId(thirdPartyScansTx, application.getId());
      thirdPartySbomMetadataList.forEach(thirdPartySbomMetadata -> thirdPartyFileDAO.delete(thirdPartyScansTx,
          thirdPartySbomMetadata.getThirdPartyFileId()));

      thirdPartyScansTx.commit();
    }

    // Cascade to repository connections
    for (RepositoryConnection repositoryConnection : repositoryConnectionDAO.getByOwnerId(tx, application.getId())) {
      repositoryConnectionDAO.delete(tx, repositoryConnection);
    }

    // Cascade to Auto Policy Waivers
    for (AutoPolicyWaiver autoPolicyWaiver : autoPolicyWaiverDAO.getByOwnerId(tx, application.getId())) {
      autoPolicyWaiverDAO.delete(tx, autoPolicyWaiver);
    }

    CpeMatchingConfiguration cpeMatchingConfiguration =
        cpeMatchingConfigurationDAO.getByOwnerId(tx, application.getId());
    if (cpeMatchingConfiguration != null) {
      cpeMatchingConfigurationDAO.delete(tx, cpeMatchingConfiguration);
    }

    versionEvaluationWindowDAO.deleteByOwnerId(tx, application.getId());

    long duration = System.currentTimeMillis() - start;
    if (duration > 500) {
      log.debug("Deleted application '{}' with id {} in {} ms.", application.getName(), application.getId(), duration);
    }
  }

  public List<ApplicationRiskDTO> getDashboardApplicationRisk(
      final Set<String> applicationIds,
      final Set<String> stageTypes,
      final Set<String> policyThreatCategoryFilter,
      final int minPolicyThreatLevel,
      final int maxPolicyThreatLevel,
      final Set<String> policyViolationStateFilter,
      final String sortColumn,
      final String direction,
      final int page,
      final int pageSize)
  {
    if (!isDatabasePostgresql()) {
      throw new UnsupportedOperationException("This operation is only supported for PostgreSQL databases");
    }

    if (applicationIds.isEmpty()) {
      return List.of();
    }

    try (TransactionContext tx = createTransactionContext()) {
      boolean useTemporaryTable =
          temporaryTableHelper.maybeCreateTemporaryTableWithIds(tx, applicationIds);

      String applicationWhereClause =
          useTemporaryTable ? "" : "application_id IN (?" + StringUtils.repeat(",?", applicationIds.size() - 1) + ")";
      String stageTypeWhereClause = "stage_type_id IN (?" + StringUtils.repeat(",?", stageTypes.size() - 1) + ")";
      String threatCategoryWhereClause = policyThreatCategoryFilter.isEmpty()
          ? ""
          : "threat_category IN (?" + StringUtils.repeat(",?", policyThreatCategoryFilter.size() - 1) + ")";
      String violationStateWhereClause;
      if (policyViolationStateFilter.isEmpty()) {
        violationStateWhereClause = "";
      }
      else {
        violationStateWhereClause = "(" + StringUtils.joinWith(" OR ",
            policyViolationStateFilter.stream().map(state -> switch (state)
            {
              case "WAIVED" -> "waive_time IS NOT NULL";
              case "LEGACY_VIOLATION" -> "legacy_violation_time IS NOT NULL";
              case "OPEN" -> "(waive_time IS NULL AND legacy_violation_time IS NULL)";
              default -> "";
            }).filter(StringUtils::isNotBlank).toArray()) + ")";
      }

      String whereClause = StringUtils.joinWith(" AND ",
          Stream.of(applicationWhereClause, stageTypeWhereClause, threatCategoryWhereClause, violationStateWhereClause)
              .filter(StringUtils::isNotBlank)
              .toArray());

      String sortClause = "name".equals(sortColumn)
          ? "lower(a.name)"
          : "SUM(%s) OVER (PARTITION BY application_id)".formatted(sortColumn);

      String databaseSchema = getDatabaseSchema();

      String sQuery = """
          SELECT o.organization_id,
                 o.name      AS organization_name,
                 x.application_name,
                 x.application_public_id,
                 pe.scan_id,
                 x.stage_type_id,
                 x.application_id,
                 x.rank,
                 x.total_risk_per_stage_unique,
                 x.critical_per_stage_unique,
                 x.severe_per_stage_unique,
                 x.moderate_per_stage_unique,
                 x.low_per_stage_unique,
                 x.total_risk_per_stage,
                 x.critical_per_stage,
                 x.severe_per_stage,
                 x.moderate_per_stage,
                 x.low_per_stage
          FROM (
                   -- The paging of this cannot use traditional limit/offset
                   -- because there can be multiple rows per application.
                   -- So DENSE_RANK is added to give the ranking and page on that ranking.
                   SELECT DENSE_RANK() OVER (ORDER BY sort_column %s, application_id) AS rank,
                          *
                   FROM (
                            -- PARTITION is added to sum the risks for an application, then the result is used to sort
                            -- the risks is not only one specific column, this value depends on the sortColumn parameter
                            SELECT stage_type_id,
                                   a.organization_id,
                                   a.application_id,
                                   a.name AS application_name,
                                   a.public_id AS application_public_id,
                                   %s AS sort_column,
                                   total_risk_per_stage_unique,
                                   critical_per_stage_unique,
                                   severe_per_stage_unique,
                                   moderate_per_stage_unique,
                                   low_per_stage_unique,
                                   total_risk_per_stage,
                                   critical_per_stage,
                                   severe_per_stage,
                                   moderate_per_stage,
                                   low_per_stage
                            FROM (SELECT application_id,
                                         stage_type_id,
                                         SUM(CASE
                                                 WHEN first_policy_violation = policy_violation_id
                                                     THEN threat_level
                                                 ELSE 0 END) total_risk_per_stage_unique,
                                         SUM(CASE
                                                 WHEN first_policy_violation = policy_violation_id
                                                     AND threat_level >= 8 THEN threat_level
                                                 ELSE 0 END) AS critical_per_stage_unique,
                                         SUM(CASE
                                                 WHEN first_policy_violation = policy_violation_id AND threat_level >= 4
                                                     AND threat_level < 8 THEN threat_level
                                                 ELSE 0 END) AS severe_per_stage_unique,
                                         SUM(CASE
                                                 WHEN first_policy_violation = policy_violation_id AND threat_level >= 2
                                                     AND threat_level < 4 THEN threat_level
                                                 ELSE 0 END) AS moderate_per_stage_unique,
                                         SUM(CASE
                                                 WHEN first_policy_violation = policy_violation_id AND threat_level < 2
                                                     THEN threat_level
                                                 ELSE 0 END) AS low_per_stage_unique,
                                         SUM(threat_level) AS total_risk_per_stage,
                                         SUM(CASE
                                                 WHEN threat_level >= 8
                                                     THEN threat_level ELSE 0 END) AS critical_per_stage,
                                         SUM(CASE
                                                 WHEN threat_level >= 4 AND threat_level < 8
                                                     THEN threat_level ELSE 0 END) AS severe_per_stage,
                                         SUM(CASE
                                                 WHEN threat_level >= 2 AND threat_level < 4
                                                     THEN threat_level ELSE 0 END) AS moderate_per_stage,
                                         SUM(CASE
                                                 WHEN threat_level < 2 THEN threat_level ELSE 0 END) AS low_per_stage
                                  FROM (
                                  -- FIRST_VALUE is added to get the first policy_violation_id,
                                  -- then this value is used to sum the risks for application
                                  SELECT application_id,
                                               policy_id,
                                               stage_type_id,
                                               threat_level,
                                               hash,
                                               FIRST_VALUE(policy_violation_id) OVER (PARTITION BY hash,
                                                   application_id,
                                                   policy_name,
                                                   threat_level,
                                                   hash,
                                                   component_id_format,
                                                   component_id_coordinates_json,
                                                   constraint_facts_id
                                                   ) AS first_policy_violation,
                                               policy_violation_id,
                                               component_id_coordinates_json
                                        FROM %s.policy_violation
                                        %s
                                        WHERE fix_time IS null AND threat_level BETWEEN ? AND ? AND %s
                                  ) pv_first_value
                                  GROUP BY application_id, stage_type_id
                            ) pv_risk_per_stage
                            JOIN %s.application a USING (application_id)
                  ) pv_risk_per_app
          ) x
          JOIN %s.last_policy_evaluation lpe USING (application_id, stage_type_id)
          JOIN %s.policy_evaluation pe USING (policy_evaluation_id)
          JOIN %s.organization o USING (organization_id)
          WHERE rank BETWEEN ? AND ?
          ORDER BY sort_column %s, lower(application_name)""".formatted(direction, sortClause, databaseSchema,
          useTemporaryTable ? "JOIN temporary_ids ti ON (policy_violation.application_id = ti.id)" : "",
          whereClause, databaseSchema, databaseSchema, databaseSchema, databaseSchema, direction);

      List<Object> params = new ArrayList<>();
      params.add(minPolicyThreatLevel);
      params.add(maxPolicyThreatLevel);

      if (!useTemporaryTable) {
        params.addAll(applicationIds);
      }
      params.addAll(stageTypes);
      params.addAll(policyThreatCategoryFilter);

      int first = page * pageSize + 1;
      int last = first + pageSize;
      if (pageSize == Integer.MAX_VALUE) {
        last = Integer.MAX_VALUE;
      }

      params.add(first);
      params.add(last);

      List<ApplicationRiskDTO> results = tx.dsl()
          .resultQuery(sQuery, params.toArray())
          .fetchStream()
          .map(record -> new ApplicationRiskDTO(
              record.get(0, String.class), // organizationId
              record.get(1, String.class), // organizationName
              record.get(2, String.class), // applicationName
              record.get(3, String.class), // publicId
              record.get(4, String.class), // scanId
              record.get(5, String.class), // stageTypeId
              record.get(6, String.class), // applicationId
              record.get(7, Long.class), // rank
              record.get(8, Long.class) == null ? 0 : record.get(8, Long.class).intValue(),
              record.get(9, Long.class) == null ? 0 : record.get(9, Long.class).intValue(),
              record.get(10, Long.class) == null ? 0 : record.get(10, Long.class).intValue(),
              record.get(11, Long.class) == null ? 0 : record.get(11, Long.class).intValue(),
              record.get(12, Long.class) == null ? 0 : record.get(12, Long.class).intValue(), // lowPerStageUnique
              record.get(13, Long.class) == null ? 0 : record.get(13, Long.class).intValue(), // totalRiskPerStage
              record.get(14, Long.class) == null ? 0 : record.get(14, Long.class).intValue(), // criticalPerStage
              record.get(15, Long.class) == null ? 0 : record.get(15, Long.class).intValue(), // severePerStage
              record.get(16, Long.class) == null ? 0 : record.get(16, Long.class).intValue(), // moderatePerStage
              record.get(17, Long.class) == null ? 0 : record.get(17, Long.class).intValue() // lowPerStage
          ))
          .toList();
      return results;
    }
  }

  public long getCountWithoutRelatedRepositories() {
    try (TransactionContext tx = createTransactionContext()) {
      return tx.dsl()
          .selectCount()
          .from(APPLICATION)
          .join(com.sonatype.insight.brain.jooq.generated.ods.tables.Organization.ORGANIZATION)
          .on(APPLICATION.ORGANIZATION_ID.eq(
              com.sonatype.insight.brain.jooq.generated.ods.tables.Organization.ORGANIZATION.ORGANIZATION_ID))
          .where(com.sonatype.insight.brain.jooq.generated.ods.tables.Organization.ORGANIZATION.RELATED_REPOSITORY_ID
              .isNull()
              .and(
                  com.sonatype.insight.brain.jooq.generated.ods.tables.Organization.ORGANIZATION.RELATED_REPOSITORY_MANAGER_ID
                      .isNull()))
          .fetchOne(0, Long.class);
    }
  }

  public long getApplicationsCountByOrganizationId(String organizationId) {
    try (TransactionContext tx = createTransactionContext()) {
      return tx.dsl()
          .selectCount()
          .from(APPLICATION)
          .where(APPLICATION.ORGANIZATION_ID.eq(organizationId))
          .fetchOne(0, Long.class);
    }
  }
}
