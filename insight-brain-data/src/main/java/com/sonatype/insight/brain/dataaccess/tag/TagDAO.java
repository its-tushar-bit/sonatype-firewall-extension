/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.tag;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.dataaccess.AbstractOperationalSqlDAO;
import com.sonatype.insight.brain.dataaccess.DataAccessException;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.dataaccess.search.SearchIndexManager;
import com.sonatype.insight.brain.dataaccess.vulnerability.VulnerabilityCustomCvssSeverityTagDAO;
import com.sonatype.insight.brain.dataaccess.vulnerability.VulnerabilityCustomCvssVectorTagDAO;
import com.sonatype.insight.brain.dataaccess.vulnerability.VulnerabilityCustomCweTagDAO;
import com.sonatype.insight.brain.dataaccess.vulnerability.VulnerabilityCustomRemediationTagDAO;
import com.sonatype.insight.brain.db.datastore.OperationalDataStore;
import com.sonatype.insight.brain.model.Color;
import com.sonatype.insight.brain.model.DescriptionHelper;
import com.sonatype.insight.brain.model.InvalidNameException;
import com.sonatype.insight.brain.model.NameHelper;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.SearchIndexChange;
import com.sonatype.insight.brain.model.SearchIndexChange.ChangeType;
import com.sonatype.insight.brain.model.tag.ApplicationTag;
import com.sonatype.insight.brain.model.tag.PolicyTag;
import com.sonatype.insight.brain.model.tag.Tag;
import com.sonatype.insight.brain.model.vulnerability.VulnerabilityCustomCvssSeverityTag;
import com.sonatype.insight.brain.model.vulnerability.VulnerabilityCustomCvssVectorTag;
import com.sonatype.insight.brain.model.vulnerability.VulnerabilityCustomCweTag;
import com.sonatype.insight.brain.model.vulnerability.VulnerabilityCustomRemediationTag;
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.error.exception.BadRequestException;

import org.jooq.Record;
import org.jooq.Table;

import static com.sonatype.insight.brain.jooq.generated.ods.tables.ApplicationTag.APPLICATION_TAG;
import static com.sonatype.insight.brain.jooq.generated.ods.tables.PolicyTag.POLICY_TAG;
import static com.sonatype.insight.brain.jooq.generated.ods.tables.Tag.TAG;

/**
 * @since 1.9
 */
@Named
@Singleton
public class TagDAO
    extends AbstractOperationalSqlDAO<Tag>
{
  private final OrganizationDAO orgDAO;

  private final PolicyTagDAO policyTagDAO;

  private final ApplicationTagDAO applicationTagDAO;

  private final VulnerabilityCustomRemediationTagDAO vulnerabilityCustomRemediationTagDAO;

  private final VulnerabilityCustomCweTagDAO vulnerabilityCustomCweTagDAO;

  private final VulnerabilityCustomCvssVectorTagDAO vulnerabilityCustomCvssVectorTagDAO;

  private final VulnerabilityCustomCvssSeverityTagDAO vulnerabilityCustomCvssSeverityTagDAO;

  @Inject
  public TagDAO(
      final OperationalDataStore operationalDataStore,
      final SearchIndexManager searchIndexManager,
      final OrganizationDAO orgDAO,
      final PolicyTagDAO policyTagDAO,
      final ApplicationTagDAO applicationTagDAO,
      final VulnerabilityCustomRemediationTagDAO vulnerabilityCustomRemediationTagDAO,
      final VulnerabilityCustomCweTagDAO vulnerabilityCustomCweTagDAO,
      final VulnerabilityCustomCvssVectorTagDAO vulnerabilityCustomCvssVectorTagDAO,
      final VulnerabilityCustomCvssSeverityTagDAO vulnerabilityCustomCvssSeverityTagDAO)
  {
    super(operationalDataStore, searchIndexManager);
    this.orgDAO = orgDAO;
    this.policyTagDAO = policyTagDAO;
    this.applicationTagDAO = applicationTagDAO;
    this.vulnerabilityCustomRemediationTagDAO = vulnerabilityCustomRemediationTagDAO;
    this.vulnerabilityCustomCweTagDAO = vulnerabilityCustomCweTagDAO;
    this.vulnerabilityCustomCvssVectorTagDAO = vulnerabilityCustomCvssVectorTagDAO;
    this.vulnerabilityCustomCvssSeverityTagDAO = vulnerabilityCustomCvssSeverityTagDAO;
  }

  public List<Tag> getByOrganizationId(String organizationId) {
    try (TransactionContext tx = createTransactionContext()) {
      return getByOrganizationId(tx, organizationId);
    }
  }

  public List<Tag> getByOrganizationId(TransactionContext tx, String organizationId) {
    return tx.dsl()
        .selectFrom(TAG)
        .where(TAG.ORGANIZATION_ID.eq(organizationId))
        .orderBy(TAG.NAME_LOWERCASE_NO_WHITESPACE)
        .fetchInto(Tag.class);
  }

  public List<Tag> getByOrganizationIds(Collection<String> organizationIds) {
    try (TransactionContext tx = createTransactionContext()) {
      return getByOrganizationIds(tx, organizationIds);
    }
  }

  public List<Tag> getByOrganizationIds(TransactionContext tx, Collection<String> organizationIds) {
    if (organizationIds == null || organizationIds.isEmpty()) {
      return Collections.emptyList();
    }
    return getListWithSqlInClause(organizationIds,
        ids -> tx.dsl()
            .selectFrom(TAG)
            .where(TAG.ORGANIZATION_ID.in(ids))
            .orderBy(TAG.NAME_LOWERCASE_NO_WHITESPACE)
            .fetchInto(Tag.class),
        getDataStore());
  }

  public Tag getByOrganizationIdAndName(TransactionContext tx, String organizationId, String name) {
    if (name == null || name.trim().isEmpty()) {
      throw new DataAccessException("The application category name cannot be null or empty.");
    }

    // Tag Name is whitespace and case insensitive
    name = NameHelper.normalize(name);
    return toEntity(tx.dsl()
        .selectFrom(TAG)
        .where(TAG.ORGANIZATION_ID.eq(organizationId)
            .and(TAG.NAME_LOWERCASE_NO_WHITESPACE.eq(name)))
        .fetchOne());
  }

  /**
   * Retrieve list of Tags applied to specified Application
   */
  public List<Tag> getByApplicationId(String applicationId) {
    try (TransactionContext tx = createTransactionContext()) {
      return getByApplicationId(tx, applicationId);
    }
  }

  public List<Tag> getByApplicationId(TransactionContext tx, String applicationId) {
    return tx.dsl()
        .select(TAG.fields())
        .from(TAG)
        .join(APPLICATION_TAG)
        .on(APPLICATION_TAG.TAG_ID.eq(TAG.TAG_ID))
        .where(APPLICATION_TAG.APPLICATION_ID.eq(applicationId))
        .fetchInto(Tag.class);
  }

  public List<Tag> getByApplicationIds(List<String> applicationIds) {
    if (applicationIds == null || applicationIds.isEmpty()) {
      return Collections.emptyList();
    }

    try (TransactionContext tx = createTransactionContext()) {
      return tx.dsl()
          .selectDistinct(TAG.fields())
          .from(TAG)
          .join(APPLICATION_TAG)
          .on(APPLICATION_TAG.TAG_ID.eq(TAG.TAG_ID))
          .where(APPLICATION_TAG.APPLICATION_ID.in(applicationIds))
          .fetch()
          .map(this::toEntity);
    }
  }

  /**
   * Tags applied to each of the given applications, preserving the application-to-tag association
   * (unlike {@link #getByApplicationIds(List)}, which returns a flat de-duplicated list across all
   * apps). Returns a map keyed by application id; an application with no tags is absent from the map.
   * The IN clause is auto-chunked over large id collections via {@link #getListWithSqlInClause}.
   */
  public Map<String, List<Tag>> getByApplicationIdsGrouped(Collection<String> applicationIds) {
    if (applicationIds == null || applicationIds.isEmpty()) {
      return Collections.emptyMap();
    }
    try (TransactionContext tx = createTransactionContext()) {
      List<Record> rows = getListWithSqlInClause(applicationIds,
          ids -> tx.dsl()
              .select(APPLICATION_TAG.APPLICATION_ID)
              .select(TAG.fields())
              .from(TAG)
              .join(APPLICATION_TAG)
              .on(APPLICATION_TAG.TAG_ID.eq(TAG.TAG_ID))
              .where(APPLICATION_TAG.APPLICATION_ID.in(ids))
              .fetch(),
          getDataStore());
      Map<String, List<Tag>> tagsByApplicationId = new LinkedHashMap<>();
      for (Record row : rows) {
        String applicationId = row.get(APPLICATION_TAG.APPLICATION_ID);
        // TAG has no application_id column, so mapping the mixed record into Tag ignores the extra key column.
        Tag tag = toEntity(row);
        tagsByApplicationId.computeIfAbsent(applicationId, id -> new ArrayList<>()).add(tag);
      }
      return tagsByApplicationId;
    }
  }

  public List<Tag> getByIds(List<String> tagIds) {
    if (tagIds == null || tagIds.isEmpty()) {
      return Collections.emptyList();
    }

    try (TransactionContext tx = createTransactionContext()) {
      return tx.dsl()
          .selectFrom(TAG)
          .where(TAG.TAG_ID.in(tagIds))
          .fetch()
          .map(this::toEntity);
    }
  }

  /**
   * Retrieve list of Tags applied to specified Policy
   */
  public List<Tag> getByPolicyId(String policyId) {
    try (TransactionContext tx = createTransactionContext()) {
      return tx.dsl()
          .select(TAG.fields())
          .from(TAG)
          .join(POLICY_TAG)
          .on(POLICY_TAG.TAG_ID.eq(TAG.TAG_ID))
          .where(POLICY_TAG.POLICY_ID.eq(policyId))
          .fetch()
          .map(this::toEntity);
    }
  }

  public List<Tag> getByName(String name) {
    name = NameHelper.normalize(name);
    try (TransactionContext tx = createTransactionContext()) {
      return tx.dsl()
          .selectFrom(TAG)
          .where(TAG.NAME_LOWERCASE_NO_WHITESPACE.eq(name))
          .fetch()
          .map(this::toEntity);
    }
  }

  private void validateColor(Color color) {
    if (color == null) {
      throw new InvalidTagException("The application category color must be assigned.");
    }

    if (color.isLegacy()) {
      throw new InvalidTagException("The application category color " + color.toValue() + " is invalid.");
    }
  }

  @Override
  public int insert(TransactionContext tx, Tag entity) {
    NameHelper.validate(entity.getName());
    DescriptionHelper.validate(entity.getDescription());
    validateColor(entity.getColor());

    if (getByOrganizationIdAndName(tx, entity.getOrganizationId(), entity.getName()) != null) {
      throw new InvalidNameException(entity.getName() + " is already used as a name.");
    }
    validateNameWithinHierarchy(tx, entity.getOrganizationId(), entity.getName());

    return super.insert(tx, entity);
  }

  @Override
  public int update(TransactionContext tx, Tag entity) {
    NameHelper.validate(entity.getName());
    DescriptionHelper.validate(entity.getDescription());
    validateColor(entity.getColor());

    Tag existingEntity = getByOrganizationIdAndName(tx, entity.getOrganizationId(), entity.getName());
    if (existingEntity != null && !existingEntity.getId().equals(entity.getId())) {
      throw new InvalidNameException(entity.getName() + " is already used as a name.");
    }
    validateNameWithinHierarchy(tx, entity.getOrganizationId(), entity.getName());

    return super.update(tx, entity);
  }

  private void validateNameWithinHierarchy(TransactionContext tx, String orgId, String name) {
    Organization org = orgDAO.getById(tx, orgId);

    validateNameWithinHierarchyUp(tx, org.getParentOrganizationId(), name);
    validateNameWithinHierarchyDown(tx, org, name);
  }

  private void validateNameWithinHierarchyUp(TransactionContext tx, String parentId, String name) {
    if (parentId == null) {
      return; // no parent, we're done
    }
    Organization parentOrganization = orgDAO.getByIdNotNull(parentId);
    if (getByOrganizationIdAndName(tx, parentOrganization.getId(), name) != null) {
      throw new InvalidNameException("An application category with the same name already exists for organization '"
          + parentOrganization.getName() + "'");
    }
    validateNameWithinHierarchyUp(tx, parentOrganization.getParentOrganizationId(), name);
  }

  private void validateNameWithinHierarchyDown(TransactionContext tx, Organization org, String name) {
    List<Organization> childOrgs = orgDAO.getByParentOrganizationId(tx, org.getId());
    for (Organization childOrg : childOrgs) {
      if (getByOrganizationIdAndName(tx, childOrg.getId(), name) != null) {
        throw new InvalidNameException("An application category with the same name already exists for organization '"
            + childOrg.getName() + "'");
      }

      validateNameWithinHierarchyDown(tx, childOrg, name);
    }
  }

  @Override
  public void delete(TransactionContext tx, Tag tag) {
    // Do not allow the delete if the tag is applied to policies
    List<PolicyTag> policyTags = policyTagDAO.getByTagId(tx, tag.getId());
    if (policyTags.size() > 0) {
      throw new BadRequestException("Cannot delete the application category because it is associated with policies.");
    }

    // Cascade to application tags
    List<ApplicationTag> appTags = applicationTagDAO.getByTagId(tx, tag.getId());
    for (ApplicationTag appTag : appTags) {
      applicationTagDAO.delete(tx, appTag);
    }

    // Cascade to vulnerability custom remediation tags
    for (VulnerabilityCustomRemediationTag vulnerabilityCustomRemediationTag : vulnerabilityCustomRemediationTagDAO
        .getByTagId(tx, tag.getId()))
    {
      vulnerabilityCustomRemediationTagDAO.delete(tx, vulnerabilityCustomRemediationTag);
    }

    // Cascade to vulnerability custom CWE tags
    for (VulnerabilityCustomCweTag vulnerabilityCustomCweTag : vulnerabilityCustomCweTagDAO.getByTagId(tx,
        tag.getId()))
    {
      vulnerabilityCustomCweTagDAO.delete(tx, vulnerabilityCustomCweTag);
    }

    // Cascade to vulnerability custom CVSS vector tags
    for (VulnerabilityCustomCvssVectorTag vulnerabilityCustomCvssVectorTag : vulnerabilityCustomCvssVectorTagDAO
        .getByTagId(tx, tag.getId()))
    {
      vulnerabilityCustomCvssVectorTagDAO.delete(tx, vulnerabilityCustomCvssVectorTag);
    }

    // Cascade to vulnerability custom CVSS severity tags
    for (VulnerabilityCustomCvssSeverityTag vulnerabilityCustomCvssSeverityTag : vulnerabilityCustomCvssSeverityTagDAO
        .getByTagId(tx, tag.getId()))
    {
      vulnerabilityCustomCvssSeverityTagDAO.delete(tx, vulnerabilityCustomCvssSeverityTag);
    }

    super.delete(tx, tag);
  }

  @Override
  protected SearchIndexChange newSearchIndexChange(Tag entity) {
    return new SearchIndexChange(ChangeType.APPLICATION_CATEGORY, entity.getId());
  }

  @Override
  public Table<?> getJooqTable() {
    return TAG;
  }

  @Override
  public Class<Tag> getEntityClass() {
    return Tag.class;
  }
}
