/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess;

import java.sql.Connection;
import java.sql.JDBCType;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
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

import com.sonatype.insight.brain.dataaccess.configuration.CpeMatchingConfigurationDAO;
import com.sonatype.insight.brain.dataaccess.configuration.ProprietaryConfigDAO;
import com.sonatype.insight.brain.dataaccess.label.LabelDAO;
import com.sonatype.insight.brain.dataaccess.license.LicenseThreatGroupDAO;
import com.sonatype.insight.brain.dataaccess.policy.AutoPolicyWaiverDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
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
import com.sonatype.insight.brain.model.configuration.ProprietaryConfig;
import com.sonatype.insight.brain.model.label.Label;
import com.sonatype.insight.brain.model.license.LicenseThreatGroup;
import com.sonatype.insight.brain.model.policy.AutoPolicyWaiver;
import com.sonatype.insight.brain.model.repository.RepositoryConnection;
import com.sonatype.insight.brain.model.security.MembershipMapping;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControl;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartySbomMetadata;
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.error.exception.NotFoundException;

import com.google.common.collect.ImmutableSortedSet;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

  private final CpeMatchingConfigurationDAO cpeMatchingConfigurationDAO;

  private final OrganizationDAO organizationDAO;

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
      final CpeMatchingConfigurationDAO cpeMatchingConfigurationDAO,
      final OrganizationDAO organizationDAO,
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
    this.cpeMatchingConfigurationDAO = cpeMatchingConfigurationDAO;
    this.organizationDAO = organizationDAO;
    this.temporaryTableHelper = temporaryTableHelper;
  }

  public Application getByPublicId(TransactionContext tx, String publicId) {
    if (publicId == null || publicId.trim().isEmpty()) {
      throw new DataAccessException("The application public ID cannot be null or empty.");
    }

    publicId = normalizePublicId(publicId);
    String sQuery = "SELECT entity FROM Application entity" + //
        " WHERE entity.publicIdLowercase=?1";
    return get(tx, sQuery, publicId);
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

  public Application getByName(TransactionContext tx, String name) {
    if (name == null || name.trim().isEmpty()) {
      throw new DataAccessException("The application name cannot be null or empty.");
    }
    // Application Name is whitespace and case insensitive
    name = NameHelper.normalize(name);
    String sQuery = "SELECT entity FROM Application entity WHERE entity.nameLowercaseNoWhitespace=?1";
    return get(tx, sQuery, name);
  }

  public Application getByName(String name) {
    try (TransactionContext tx = createTransactionContext()) {
      return getByName(tx, name);
    }
  }

  public List<Application> getByContactInternalName(String contactInternalName) {
    String sQuery = "SELECT entity FROM Application entity WHERE entity.contactInternalName=?1";
    return getList(sQuery, contactInternalName);
  }

  @Override
  public List<Application> getAll(TransactionContext tx) {
    String sQuery = "SELECT entity FROM Application entity" + //
        " ORDER BY entity.publicIdLowercase";
    return getList(tx, sQuery);
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

  @SuppressWarnings("unchecked")
  public List<Application> getAll(
      final TransactionContext tx,
      final int page,
      final int pageSize)
  {
    String sQuery = "SELECT app FROM Application app" +
        " ORDER BY app.publicIdLowercase";
    int offset = (page - 1) * pageSize;
    jakarta.persistence.Query paginationQuery = createPaginationQuery(tx, sQuery, offset, pageSize);
    return paginationQuery.getResultList();
  }

  public List<Application> getAllOrderedByName(TransactionContext tx) {
    String sQuery = "SELECT entity FROM Application entity" + //
        " ORDER BY entity.name";
    return getList(tx, sQuery);
  }

  public List<Application> getAllOrderedByName() {
    try (TransactionContext tx = createTransactionContext()) {
      return getAllOrderedByName(tx);
    }
  }

  public List<Application> getAllWithoutRelatedRepositoriesOrderedByName() {
    String sQuery = "SELECT entity FROM Application entity, Organization org" + //
        " WHERE entity.organizationId = org.id" +
        " AND org.relatedRepositoryId IS NULL" +
        " AND org.relatedRepositoryManagerId IS NULL" +
        " ORDER BY entity.name";
    return getList(sQuery);
  }

  public List<Application> getAllWithoutRelatedRepositories(TransactionContext tx) {
    String sQuery = "SELECT entity FROM Application entity, Organization org" + //
        " WHERE entity.organizationId = org.id" +
        " AND org.relatedRepositoryId IS NULL" +
        " AND org.relatedRepositoryManagerId IS NULL" +
        " ORDER BY entity.publicIdLowercase";
    return getList(tx, sQuery);
  }

  public List<Application> getAllWithoutRelatedRepositories() {
    try (TransactionContext tx = createTransactionContext()) {
      return getAllWithoutRelatedRepositories(tx);
    }
  }

  public List<Application> getByOrganizationId(TransactionContext tx, String organizationId) {
    String sQuery = "SELECT entity FROM Application entity" + //
        " WHERE entity.organizationId=?1" + //
        " ORDER BY entity.publicIdLowercase";
    return getList(tx, sQuery, organizationId);
  }

  public List<Application> getByOrganizationIds(Set<String> organizationIds) {
    String sQuery = "SELECT entity FROM Application entity WHERE entity.organizationId IN ?1";
    return getList(sQuery, organizationIds);
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
    String sQuery = "SELECT app FROM Application app, ApplicationAncestor aa " +
        "WHERE aa.ancestorId = ?1 AND aa.id = app.id AND aa.id <> aa.ancestorId";

    return getList(tx, sQuery, organizationId);
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
    String sQuery = "SELECT DISTINCT aa.id FROM ApplicationAncestor aa" +
        " WHERE aa.ancestorId IN (?1)";
    return new HashSet<>(getListWithSqlInClause(ancestorIds, l -> getScalars(tx, String.class, sQuery, l)));
  }

  public List<Application> getByIdsAndTagIds(Set<String> applicationIds, Set<String> tagIds) {
    String sQuery = "SELECT DISTINCT application FROM Application application, ApplicationTag applicationTag" + //
        " WHERE application.id = applicationTag.applicationId" + //
        " AND application.id IN (?1)" + " AND applicationTag.tagId IN (?2)";

    List<Application> sQueryApplications = getList(sQuery, applicationIds, tagIds);

    if (tagIds.contains(null)) {
      String untaggedQuery = "SELECT application FROM Application application" + //
          " WHERE application.id IN (?1) AND NOT EXISTS (" + //
          "  SELECT applicationTag FROM ApplicationTag applicationTag" + //
          "   WHERE applicationTag.applicationId = application.id" + //
          " )";

      List<Application> untaggedApplications = getList(untaggedQuery, applicationIds);

      List<Application> retval = new ArrayList<>(sQueryApplications);
      retval.addAll(untaggedApplications);
      return retval;
    }

    return sQueryApplications;
  }

  public List<Application> getByTagIds(Set<String> tagIds) {
    String sQuery = "SELECT DISTINCT application FROM Application application, ApplicationTag applicationTag" + //
        " WHERE application.id = applicationTag.applicationId" + //
        " AND applicationTag.tagId IN (?1)";

    List<Application> sQueryApplications = getList(sQuery, tagIds);

    if (tagIds.contains(null)) {
      String untaggedQuery = "SELECT application FROM Application application" + //
          " WHERE NOT EXISTS (" + //
          "  SELECT applicationTag FROM ApplicationTag applicationTag" + //
          "   WHERE applicationTag.applicationId = application.id" + //
          " )";

      List<Application> untaggedApplications = getList(untaggedQuery);

      List<Application> retval = new ArrayList<>(sQueryApplications);
      retval.addAll(untaggedApplications);
      return retval;
    }

    return sQueryApplications;
  }

  public List<Application> getByPublicIds(Set<String> applicationPublicIds) {
    applicationPublicIds =
        applicationPublicIds.stream().map(ApplicationDAO::normalizePublicId).collect(Collectors.toSet());
    String sQuery = "SELECT entity FROM Application entity" + //
        " WHERE entity.publicIdLowercase IN (?1)";
    return getList(sQuery, applicationPublicIds);
  }

  public List<Application> getByIds(Set<String> applicationIds) {
    String sQuery = "SELECT entity FROM Application entity" + //
        " WHERE entity.id IN (?1)";
    return getList(sQuery, applicationIds);
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

  @SuppressWarnings("unchecked")
  private List<Application> getByAncestorIdsH2(
      final TransactionContext tx,
      final Set<String> ancestorIds,
      final int page,
      final int pageSize)
  {
    int offset = (page - 1) * pageSize;
    boolean splitQuery = ancestorIds.size() > getInOperatorThreshold();
    String sQuery = "SELECT DISTINCT app FROM Application app, ApplicationAncestor aa" +
        " WHERE app.id = aa.id" +
        " AND aa.ancestorId IN (?1)" + (splitQuery ? "" : " ORDER BY app.publicIdLowercase");
    if (splitQuery) {
      return getListWithSqlInClause(ancestorIds, l -> getList(tx, sQuery, l)).stream()
          .distinct()
          .sorted(Comparator.comparing(Application::getPublicIdLowercase))
          .skip(offset)
          .limit(pageSize)
          .toList();
    }
    else {
      jakarta.persistence.Query paginationQuery = createPaginationQuery(tx, sQuery, offset, pageSize);
      paginationQuery.setParameter(1, ancestorIds);
      return paginationQuery.getResultList();
    }
  }

  @SuppressWarnings("unchecked")
  private List<Application> getByAncestorIdsPostgres(
      final TransactionContext tx,
      final Set<String> ancestorIds,
      final int page,
      final int pageSize)
  {
    int offset = (page - 1) * pageSize;
    String sQuery = "SELECT DISTINCT app.* FROM " + getDatabaseSchema() + ".application app" +
        " INNER JOIN " + getDatabaseSchema() + ".application_ancestor aa" +
        " ON app.application_id = aa.application_id" +
        " WHERE aa.ancestor_id = ANY(?)" +
        " ORDER BY app.public_id_lowercase";
    jakarta.persistence.Query paginationQuery =
        createPaginationNativeQuery(tx, Application.class, sQuery, offset, pageSize);
    java.sql.Array array;
    // Creating an sql Array to pass to the postgres specific ANY function
    // This avoids the 65,535 parameter limit for postgres
    try (Connection connection = getDataStore().getDataSource().getConnection()) {
      array = connection.createArrayOf(JDBCType.VARCHAR.name(), ancestorIds.toArray());
    }
    catch (SQLException e) {
      throw new RuntimeException(e);
    }
    paginationQuery.setParameter(1, array);
    return paginationQuery.getResultList();
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

    super.insert(tx, application);
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

    super.update(tx, application);
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
    final String oQuery = "SELECT app FROM Label label, Application app" + //
        " WHERE label.ownerId=app.id AND app.organizationId=?1" + //
        "    AND label.labelLowercase=?2";
    return getList(tx, oQuery, organizationId, labelLowercase);
  }

  /**
   * fetches the #Application objects associated with the given repository URL;  the association is specified via the
   * #SourceControl entries
   *
   * @return List of #Application objects associated with the given repository URL or an empty list if there are none
   */
  public List<Application> getByRepositoryUrl(String repositoryUrl) {
    if (repositoryUrl != null) {
      repositoryUrl = SourceControl.normalizeRepositoryUrl(repositoryUrl);
    }
    final String sQuery = "SELECT app FROM Application app, SourceControl sc " +
        " WHERE app.id = sc.ownerId AND sc.normalizedRepositoryUrl = ?1";
    return getList(sQuery, repositoryUrl);
  }

  /**
   * Efficiently fetch application IDs for all of the provided normalizedRepositoryUrls
   * @return Map of normalizedRepositoryUrl to applicationIds. The sets that are the values of the map will never be
   * empty, and are sorted alphabetically.
   */
  public Map<String, SortedSet<String>> getApplicationIdsByNormalizedRepositoryUrls(
      Set<String> normalizedRepositoryUrls)
  {
    if (normalizedRepositoryUrls.isEmpty()) {
      return Collections.emptyMap();
    }

    final String sQuery = """
        SELECT sc.normalizedRepositoryUrl, sc.ownerId
        FROM SourceControl sc
        WHERE sc.normalizedRepositoryUrl IN ?1
        """;

    final List<?> results = getListWithSqlInClause(normalizedRepositoryUrls, urls -> getUntypedResult(sQuery, urls));
    final Map<String, SortedSet<String>> retval = results.stream()
        .map(Object[].class::cast)
        .collect(Collectors.toMap(
            row -> (String) row[0],
            row -> ImmutableSortedSet.of((String) row[1]),
            (set1, set2) -> {
              // Merge the two sets
              var mergedSet = new TreeSet<>(set1);
              mergedSet.addAll(set2);
              return mergedSet;
            }
        ));

    Set<String> missingUrls = new HashSet<>(normalizedRepositoryUrls);
    missingUrls.removeAll(retval.keySet());

    if (!missingUrls.isEmpty()) {
      throw new IllegalArgumentException("Repository URLs not found: " + missingUrls);
    }

    return retval;
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
    String sQuery = "SELECT app FROM Application app" +
        " WHERE app.id=?1" +
        " OR app.publicIdLowercase=?2";
    return get(tx, sQuery, idOrPublicId, normalizedIdOrPublicId);
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

    // Cascade to policies
    policyDAOProvider.get().deleteByOwnerId(tx, application.getId());

    // Cascade to owned entities
    ownerDAOProvider.get().cascadeDelete(tx, application);

    // Cascade to proprietary config
    ProprietaryConfig proprietaryConfig = proprietaryConfigDAO.getByOwnerId(tx, application.getId());
    if (proprietaryConfig != null) {
      proprietaryConfigDAO.delete(tx, proprietaryConfig);
    }

    // Cascade to SastScan table
    sastScanDAO.deleteByApplicationId(tx, application.getId());

    // Delete application DAO
    super.delete(tx, application);

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
      thirdPartySbomMetadataList.forEach(thirdPartySbomMetadata ->
          thirdPartyFileDAO.delete(thirdPartyScansTx, thirdPartySbomMetadata.getThirdPartyFileId()));

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

    cpeMatchingConfigurationDAO.delete(tx, cpeMatchingConfigurationDAO.getByOwnerId(tx, application.getId()));

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

      String applicationWhereClause = useTemporaryTable ? "" :
          "application_id IN (?" + StringUtils.repeat(",?", applicationIds.size() - 1) + ")";
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
            policyViolationStateFilter.stream().map(state -> switch (state) {
              case "WAIVED" -> "waive_time IS NOT NULL";
              case "LEGACY_VIOLATION" -> "legacy_violation_time IS NOT NULL";
              case "OPEN" -> "(waive_time IS NULL AND legacy_violation_time IS NULL)";
              default -> "";
            }).filter(StringUtils::isNotBlank).toArray()) + ")";
      }

      String whereClause = StringUtils.joinWith(" AND ",
          Stream.of(applicationWhereClause, stageTypeWhereClause, threatCategoryWhereClause, violationStateWhereClause)
              .filter(StringUtils::isNotBlank).toArray());

      String sortClause = "name".equals(sortColumn) ?
          "lower(a.name)" : "SUM(%s) OVER (PARTITION BY application_id)".formatted(sortColumn);

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

      jakarta.persistence.Query query = tx.createNativeQuery(sQuery);
      int i = 1;

      query.setParameter(i++, minPolicyThreatLevel);
      query.setParameter(i++, maxPolicyThreatLevel);

      if (!useTemporaryTable) {
        for (String appId : applicationIds) {
          query.setParameter(i++, appId);
        }
      }
      for (String stageType : stageTypes) {
        query.setParameter(i++, stageType);
      }
      for (String threatCategory : policyThreatCategoryFilter) {
        query.setParameter(i++, threatCategory);
      }

      int first = page * pageSize + 1;
      int last = first + pageSize;
      if (pageSize == Integer.MAX_VALUE) {
        last = Integer.MAX_VALUE;
      }

      query.setParameter(i++, first);
      query.setParameter(i++, last);

      @SuppressWarnings("unchecked")
      List<ApplicationRiskDTO> results =
          ((Stream<Object[]>) query.getResultStream()).map(array -> new ApplicationRiskDTO(
                  (String) array[0], // organizationId
                  (String) array[1], // organizationName
                  (String) array[2], // applicationName
                  (String) array[3], // publicId
                  (String) array[4], // scanId
                  (String) array[5], // stageTypeId
                  (String) array[6], // applicationId
                  (Long) array[7], // rank
                  array[8] == null ? 0 : Long.valueOf((long) array[8]).intValue(), // totalRiskPerStageUnique
                  array[9] == null ? 0 : Long.valueOf((long) array[9]).intValue(), // criticalPerStageUnique
                  array[10] == null ? 0 : Long.valueOf((long) array[10]).intValue(), // severePerStageUnique
                  array[11] == null ? 0 : Long.valueOf((long) array[11]).intValue(), // moderatePerStageUnique
                  array[12] == null ? 0 : Long.valueOf((long) array[12]).intValue(), // lowPerStageUnique
                  array[13] == null ? 0 : Long.valueOf((long) array[13]).intValue(), // totalRiskPerStage
                  array[14] == null ? 0 : Long.valueOf((long) array[14]).intValue(), // criticalPerStage
                  array[15] == null ? 0 : Long.valueOf((long) array[15]).intValue(), // severePerStage
                  array[16] == null ? 0 : Long.valueOf((long) array[16]).intValue(), // moderatePerStage
                  array[17] == null ? 0 : Long.valueOf((long) array[17]).intValue() // lowPerStage
              )
          ).toList();
      return results;
    }
  }

  public long getCountWithoutRelatedRepositories() {
    String sQuery = "" +
        "SELECT COUNT(application)" +
        "  FROM Application application, Organization organization" +
        " WHERE application.organizationId = organization.id" +
        "   AND organization.relatedRepositoryId IS NULL" +
        "   AND organization.relatedRepositoryManagerId IS NULL";
    return getSingle(Long.class, sQuery);
  }

  public long getApplicationsCountByOrganizationId(String organizationId) {
    String sQuery = "" +
        "SELECT COUNT(application)" +
        " FROM Application application" +
        " WHERE application.organizationId=?1";

    return getSingle(Long.class, sQuery, organizationId);
  }
}
