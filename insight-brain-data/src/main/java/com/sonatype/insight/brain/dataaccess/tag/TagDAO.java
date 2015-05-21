/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.tag;

import java.util.List;

import com.sonatype.insight.brain.dataaccess.AbstractOperationalSqlDAO;
import com.sonatype.insight.brain.dataaccess.DataAccessException;
import com.sonatype.insight.brain.model.Color;
import com.sonatype.insight.brain.model.DescriptionHelper;
import com.sonatype.insight.brain.model.InvalidNameException;
import com.sonatype.insight.brain.model.NameHelper;
import com.sonatype.insight.brain.model.tag.ApplicationTag;
import com.sonatype.insight.brain.model.tag.PolicyTag;
import com.sonatype.insight.brain.model.tag.Tag;
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.NotFoundException;

/**
 * @since 1.9
 */
public class TagDAO
    extends AbstractOperationalSqlDAO<Tag>
{
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

  public List<Tag> getUsedByApplicationId(String applicationId) {
    String sQuery = "SELECT tag FROM Tag tag, ApplicationTag appTag, Tag tag" + //
        " WHERE tag.id = appTag.tagId AND appTag.applicationId =?1";
    return getList(sQuery, applicationId);
  }

  public Tag getByOrganizationIdAndName(TransactionContext tx, String organizationId, String name) {
    if (name == null || name.trim().isEmpty()) {
      throw new DataAccessException("The tag name cannot be null or empty.");
    }

    // Tag Name is whitespace and case insensitive
    name = NameHelper.normalize(name);
    String sQuery = "SELECT entity FROM Tag entity" + //
        " WHERE entity.organizationId=?1 and entity.nameLowercaseNoWhitespace=?2";
    return get(tx, sQuery, organizationId, name);
  }

  /**
   * Retrieve list of Tags owned by the specified Organization, and applied to at least one Policy
   */
  public List<Tag> getAppliedToPolicyByOrganizationId(String organizationId) {
    String sQuery = "SELECT DISTINCT tag FROM PolicyTag policyTag, Tag tag" + //
        " WHERE policyTag.tagId = tag.id AND tag.organizationId =?1";
    return getList(sQuery, organizationId);
  }

  /**
   * Retrieve list of Tags applied to specified Application
   */
  public List<Tag> getByApplicationId(String applicationId) {
    String sQuery = "SELECT tag FROM ApplicationTag appTag, Tag tag" + //
        " WHERE appTag.tagId=tag.id AND appTag.applicationId=?1";
    return getList(sQuery, applicationId);
  }

  /**
   * Retrieve list of Tags applied to specified Policy
   */
  public List<Tag> getByPolicyId(String policyId) {
    String sQuery = "SELECT tag FROM PolicyTag policyTag, Tag tag" + //
        " WHERE policyTag.tagId=tag.id AND policyTag.policyId=?1";
    return getList(sQuery, policyId);
  }
  
  private void validateColor(Color color) {
    if (color == null) {
      throw new InvalidTagException("The tag color must be assigned.");
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

    super.update(tx, entity);
  }

  private Tag getByIdNotNull(TransactionContext tx, String id) {
    Tag tag = getById(tx, id);
    if (tag == null) {
      throw new NotFoundException("Cannot find a tag with ID " + id + ".");
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
      throw new BadRequestException("Cannot delete the tag because it is associated with policies.");
    }

    // Cascade to application tags
    ApplicationTagDAO applicationTagDAO = new ApplicationTagDAO();
    List<ApplicationTag> appTags = applicationTagDAO.getByTagId(tx, tag.getId());
    for (ApplicationTag appTag : appTags) {
      applicationTagDAO.delete(tx, appTag);
    }

    super.delete(tx, tag);
  }
}
