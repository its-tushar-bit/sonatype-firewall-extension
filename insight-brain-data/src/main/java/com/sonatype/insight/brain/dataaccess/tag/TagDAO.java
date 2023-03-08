/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.tag;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.sonatype.insight.brain.dataaccess.AbstractOperationalSqlDAO;
import com.sonatype.insight.brain.dataaccess.DataAccessException;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.dataaccess.vulnerability.VulnerabilityCustomDetailDAO;
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
import com.sonatype.insight.brain.model.vulnerability.VulnerabilityCustomDetail;
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.NotFoundException;

/**
 * @since 1.9
 */
public class TagDAO
    extends AbstractOperationalSqlDAO<Tag>
{
  private static final OrganizationDAO orgDAO = new OrganizationDAO();

  @Override
  protected Tag getById(TransactionContext tx, String id) {
    String sQuery = "SELECT entity FROM Tag entity" + //
        " WHERE entity.id=?1";
    return get(tx, sQuery, id);
  }

  public List<Tag> getByOrganizationId(String organizationId) {
    try (TransactionContext tx = createTransactionContext()) {
      return getByOrganizationId(tx, organizationId);
    }
  }

  public List<Tag> getByOrganizationId(TransactionContext tx, String organizationId) {
    String sQuery = "SELECT entity FROM Tag entity" + //
        " WHERE entity.organizationId=?1" + //
        " ORDER BY entity.nameLowercaseNoWhitespace";
    return getList(tx, sQuery, organizationId);
  }

  public Tag getByOrganizationIdAndName(TransactionContext tx, String organizationId, String name) {
    if (name == null || name.trim().isEmpty()) {
      throw new DataAccessException("The application category name cannot be null or empty.");
    }

    // Tag Name is whitespace and case insensitive
    name = NameHelper.normalize(name);
    String sQuery = "SELECT entity FROM Tag entity" + //
        " WHERE entity.organizationId=?1 and entity.nameLowercaseNoWhitespace=?2";
    return get(tx, sQuery, organizationId, name);
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
    String sQuery = "SELECT tag FROM ApplicationTag appTag, Tag tag" + //
        " WHERE appTag.tagId=tag.id AND appTag.applicationId=?1";
    return getList(tx, sQuery, applicationId);
  }

  public List<Tag> getByApplicationIds(List<String> applicationIds) {
    String sQuery = "SELECT DISTINCT tag FROM ApplicationTag appTag, Tag tag" + //
        " WHERE appTag.tagId=tag.id AND appTag.applicationId IN ?1";
    if (applicationIds == null || applicationIds.isEmpty()) {
      return Collections.emptyList();
    }
    int inOperatorThreshold = getInOperatorThreshold();
    if (applicationIds.size() >= inOperatorThreshold) {
      Map<String, Tag> tagsById = new LinkedHashMap<>();
      for (int i = 0; i < applicationIds.size(); i += inOperatorThreshold) {
        List<Tag> tags =
            getList(sQuery, applicationIds.subList(i, Math.min(i + inOperatorThreshold, applicationIds.size())));
        tags.forEach(tag -> tagsById.put(tag.getId(), tag));
      }
      return new ArrayList<>(tagsById.values());
    }
    else {
      return getList(sQuery, applicationIds);
    }
  }

  /**
   * Retrieve list of Tags applied to specified Policy
   */
  public List<Tag> getByPolicyId(String policyId) {
    String sQuery = "SELECT tag FROM PolicyTag policyTag, Tag tag" + //
        " WHERE policyTag.tagId=tag.id AND policyTag.policyId=?1";
    return getList(sQuery, policyId);
  }

  public List<Tag> getByName(String name) {
    name = NameHelper.normalize(name);
    String sQuery = "SELECT entity FROM Tag entity" + //
        " WHERE entity.nameLowercaseNoWhitespace=?1";
    return getList(sQuery, name);
  }

  /**
   * @since 1.35
   */
  public List<Tag> getAll() {
    String sQuery = "SELECT entity FROM Tag entity";
    return getList(sQuery);
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
  public void insert(TransactionContext tx, Tag entity) {
    NameHelper.validate(entity.getName());
    DescriptionHelper.validate(entity.getDescription());
    validateColor(entity.getColor());

    if (getByOrganizationIdAndName(tx, entity.getOrganizationId(), entity.getName()) != null) {
      throw new InvalidNameException(entity.getName() + " is already used as a name.");
    }
    validateNameWithinHierarchy(tx, entity.getOrganizationId(), entity.getName());

    super.insert(tx, entity);
  }

  @Override
  public void update(TransactionContext tx, Tag entity) {
    NameHelper.validate(entity.getName());
    DescriptionHelper.validate(entity.getDescription());
    validateColor(entity.getColor());

    Tag existingEntity = getByOrganizationIdAndName(tx, entity.getOrganizationId(), entity.getName());
    if (existingEntity != null && !existingEntity.getId().equals(entity.getId())) {
      throw new InvalidNameException(entity.getName() + " is already used as a name.");
    }
    validateNameWithinHierarchy(tx, entity.getOrganizationId(), entity.getName());

    super.update(tx, entity);
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

  private Tag getByIdNotNull(TransactionContext tx, String id) {
    Tag tag = getById(tx, id);
    if (tag == null) {
      throw new NotFoundException("Cannot find an application category with ID " + id + ".");
    }
    return tag;
  }

  public Tag getByIdNotNull(String id) {
    try (TransactionContext tx = createTransactionContext()) {
      return getByIdNotNull(tx, id);
    }
  }

  @Override
  public void delete(TransactionContext tx, Tag tag) {
    // Do not allow the delete if the tag is applied to policies
    List<PolicyTag> policyTags = new PolicyTagDAO().getByTagId(tx, tag.getId());
    if (policyTags.size() > 0) {
      throw new BadRequestException("Cannot delete the application category because it is associated with policies.");
    }

    // Cascade to application tags
    ApplicationTagDAO applicationTagDAO = new ApplicationTagDAO();
    List<ApplicationTag> appTags = applicationTagDAO.getByTagId(tx, tag.getId());
    for (ApplicationTag appTag : appTags) {
      applicationTagDAO.delete(tx, appTag);
    }
    // Cascade to vulnerability custom detail
    VulnerabilityCustomDetailDAO vulnerabilityCustomDetailDAO = new VulnerabilityCustomDetailDAO();
    for (VulnerabilityCustomDetail vulnerabilityCustomDetail : vulnerabilityCustomDetailDAO.getByTagId(tx,
        tag.getId())) {
      vulnerabilityCustomDetailDAO.delete(tx, vulnerabilityCustomDetail);
    }
    super.delete(tx, tag);
  }

  @Override
  protected SearchIndexChange newSearchIndexChange(Tag entity) {
    return new SearchIndexChange(ChangeType.APPLICATION_CATEGORY, entity.getId());
  }
}
