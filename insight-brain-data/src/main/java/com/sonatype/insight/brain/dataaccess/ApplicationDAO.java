/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.sonatype.insight.brain.dataaccess.configuration.ProprietaryConfigDAO;
import com.sonatype.insight.brain.dataaccess.label.LabelDAO;
import com.sonatype.insight.brain.dataaccess.license.LicenseThreatGroupDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyEvaluationDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyMonitoringDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyViolationDAO;
import com.sonatype.insight.brain.dataaccess.security.MembershipMappingDAO;
import com.sonatype.insight.brain.dataaccess.sourcecontrol.SourceControlDAO;
import com.sonatype.insight.brain.dataaccess.sourcecontrol.SourceControlDefaultBranchCommitHistoryDAO;
import com.sonatype.insight.brain.dataaccess.sourcecontrol.SourceControlEventDAO;
import com.sonatype.insight.brain.dataaccess.successmetrics.PolicyViolationAggregationDAO;
import com.sonatype.insight.brain.dataaccess.tag.ApplicationTagDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.ApplicationComponent;
import com.sonatype.insight.brain.model.InvalidNameException;
import com.sonatype.insight.brain.model.NameHelper;
import com.sonatype.insight.brain.model.SearchIndexChange;
import com.sonatype.insight.brain.model.SearchIndexChange.ChangeType;
import com.sonatype.insight.brain.model.configuration.ProprietaryConfig;
import com.sonatype.insight.brain.model.label.Label;
import com.sonatype.insight.brain.model.license.LicenseThreatGroup;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyMonitoring;
import com.sonatype.insight.brain.model.security.MembershipMapping;
import com.sonatype.insight.brain.model.tag.ApplicationTag;
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.error.exception.NotFoundException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ApplicationDAO
    extends AbstractOperationalSqlDAO<Application>
{
  private static final Logger log = LoggerFactory.getLogger(ApplicationDAO.class);

  private static final Pattern WHITESPACE_PATTERN = Pattern.compile("\\s");

  public static final int MAX_PUBLIC_ID_LENGTH = 200;

  @Override
  public Application getById(TransactionContext tx, String id) {
    String sQuery = "SELECT entity FROM Application entity" + //
        " WHERE entity.id=?1";
    return get(tx, sQuery, id);
  }

  public Application getByIdNotNull(String id) {
    try (TransactionContext tx = createTransactionContext()) {
      return getByIdNotNull(tx, id);
    }
  }

  public Application getByIdNotNull(TransactionContext tx, String id) {
    Application application = getById(tx, id);
    if (application == null) {
      throw new NotFoundException("Could not find an application with ID " + id + ".");
    }
    return application;
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

  public List<Application> getAll(TransactionContext tx) {
    String sQuery = "SELECT entity FROM Application entity" + //
        " ORDER BY entity.publicIdLowercase";
    return getList(tx, sQuery);
  }

  public List<Application> getAll() {
    try (TransactionContext tx = createTransactionContext()) {
      return getAll(tx);
    }
  }

  public List<Application> getByOrganizationId(TransactionContext tx, String organizationId) {
    String sQuery = "SELECT entity FROM Application entity" + //
        " WHERE entity.organizationId=?1" + //
        " ORDER BY entity.publicIdLowercase";
    return getList(tx, sQuery, organizationId);
  }

  public List<Application> getByOrganizationId(String organizationId) {
    try (TransactionContext tx = createTransactionContext()) {
      return getByOrganizationId(tx, organizationId);
    }
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

  @Override
  public void delete(TransactionContext tx, Application application) {
    long start = System.currentTimeMillis();

    // For H2, we do not enroll the policy violation and evaluation deletions in the transaction on purpose.
    // This improves performance and keeps db operations (including commits) reasonably short, which means other
    // concurrent db operations are blocked for shorter periods of time (H2 is single threaded).
    // Since non-transactional, we delete violations first such that no violations without corresponding evaluation
    // are left behind in case of a failure.

    // Cascade to policy violations
    new PolicyViolationDAO().deleteByApplicationId(tx, application.getId());

    // Cascade to policy evaluations
    PolicyEvaluationDAO policyEvaluationDAO = new PolicyEvaluationDAO();
    for (PolicyEvaluation policyEvaluation : policyEvaluationDAO.getByApplicationId(tx, application.getId())) {
      // The update of the last policy evaluation is time consuming. Since the application is deleted and all policy
      // evaluations are deleted as well, there is no point in updating the last policy evaluation. This improves
      // performance 75 times.

      // We do not enroll the policy evaluation deletes in the transaction on purpose. For applications with a lot of
      // policy evaluations, the transaction becomes huge and that slows down the delete operation. By doing the policy
      // evaluation deletes outside of the transaction, performance is improved 17 times.
      policyEvaluationDAO.delete(policyEvaluation, false /* updateLastPolicyEvaluation */);
    }

    // Cascade to license threat groups
    LicenseThreatGroupDAO licenseThreatGroupDAO = new LicenseThreatGroupDAO();
    List<LicenseThreatGroup> licenseThreatGroups = licenseThreatGroupDAO.getByOwnerId(tx, application.getId());
    for (LicenseThreatGroup licenseThreatGroup : licenseThreatGroups) {
      licenseThreatGroupDAO.delete(tx, licenseThreatGroup);
    }

    // Cascade to labels
    LabelDAO labelDAO = new LabelDAO();
    List<Label> labels = labelDAO.getByOwnerId(tx, application.getId());
    for (Label label : labels) {
      labelDAO.delete(tx, label);
    }

    // Cascade to policies
    new PolicyDAO().deleteByOwnerId(tx, application.getId());

    // Cascade to membership mappings
    MembershipMappingDAO membershipMappingDAO = new MembershipMappingDAO();
    for (MembershipMapping membershipMapping : membershipMappingDAO.getByContextId(tx, application.getId())) {
      membershipMappingDAO.delete(tx, membershipMapping);
    }

    // Cascade to policy monitoring
    PolicyMonitoringDAO policyMonitoringDAO = new PolicyMonitoringDAO();
    PolicyMonitoring policyMonitoring = policyMonitoringDAO.getByOwnerId(tx, application.getId());
    if (policyMonitoring != null) {
      policyMonitoringDAO.delete(tx, policyMonitoring);
    }

    // Cascade to owned entities
    new OwnerDAO().cascadeDelete(tx, application);

    // Cascade to applied tags
    ApplicationTagDAO applicationTagDAO = new ApplicationTagDAO();
    List<ApplicationTag> appTags = applicationTagDAO.getByApplicationId(tx, application.getId());
    for (ApplicationTag appTag : appTags) {
      applicationTagDAO.delete(tx, appTag);
    }

    // Cascade to components
    ApplicationComponentDAO applicationComponentDAO = new ApplicationComponentDAO();
    List<ApplicationComponent> appComponents = applicationComponentDAO.getByApplicationId(tx, application.getId());
    for (ApplicationComponent appComponent : appComponents) {
      applicationComponentDAO.delete(tx, appComponent);
    }

    // Cascade to proprietary config
    ProprietaryConfigDAO proprietaryConfigDAO = new ProprietaryConfigDAO();
    ProprietaryConfig proprietaryConfig = proprietaryConfigDAO.getByOwnerId(tx, application.getId());
    if (proprietaryConfig != null) {
      proprietaryConfigDAO.delete(tx, proprietaryConfig);
    }

    // Cascade to SourceControl config
    new SourceControlDAO().deleteByOwnerId(tx, application.getId());

    // Cascade to SourceControl default branch commit history
    new SourceControlDefaultBranchCommitHistoryDAO().deleteByApplicationId(tx, application.getId());

    // Cascade to SourceControl events
    new SourceControlEventDAO().deleteByApplicationId(tx, application.getId());

    // Cascade to locks
    ClusterLock.deleteForPolicyViolations(tx, application);
    ClusterLock.deleteForPolicyViolationAggregations(tx, application.getId());

    super.delete(tx, application);

    // Cascade to aggregation tables. These are in a separate database and therefore use a separate transaction.
    PolicyViolationAggregationDAO policyViolationAggregationDAO = new PolicyViolationAggregationDAO();
    try (TransactionContext aggregationTx = policyViolationAggregationDAO.createTransactionContext()) {
      aggregationTx.begin();

      policyViolationAggregationDAO.deleteByApplicationId(aggregationTx, application.getId());

      aggregationTx.commit();
    }

    long duration = System.currentTimeMillis() - start;
    if (duration > 500) {
      log.debug("Deleted application '{}' with id {} in {} ms.", application.getName(), application.getId(), duration);
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

  public List<Application> getByOrganizationIdAndLabelLowercase(TransactionContext tx,
                                                                String organizationId,
                                                                String labelLowercase)
  {
    final String oQuery = "SELECT app FROM Label label, Application app" + //
        " WHERE label.ownerId=app.id AND app.organizationId=?1" + //
        "    AND label.labelLowercase=?2";
    return getList(tx, oQuery, organizationId, labelLowercase);
  }

  public static String normalizePublicId(String publicId) {
    return publicId.trim().toLowerCase(Locale.ENGLISH);
  }

  @Override
  protected SearchIndexChange newSearchIndexChange(Application entity) {
    return new SearchIndexChange(ChangeType.APPLICATION, entity.getId());
  }
}
