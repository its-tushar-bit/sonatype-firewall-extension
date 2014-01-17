/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.tag;

import java.util.List;

import javax.persistence.EntityManager;

import com.sonatype.insight.brain.dataaccess.AbstractOperationalSqlDAO;
import com.sonatype.insight.brain.dataaccess.DataAccessException;
import com.sonatype.insight.brain.model.InvalidNameException;
import com.sonatype.insight.brain.model.NameHelper;
import com.sonatype.insight.brain.model.tag.Tag;
import com.sonatype.insight.error.exception.NotFoundException;

import org.codehaus.plexus.util.StringUtils;

/**
 * @since 1.9
 */
public class TagDAO
    extends AbstractOperationalSqlDAO<Tag>
{
  public static final int MAX_DESC_SIZE = 255;

  @Override
  protected Tag getById(EntityManager em, String id) {
    String sQuery = "SELECT entity FROM Tag entity" + //
        " WHERE entity.id=?1";
    return get(em, sQuery, id);
  }

  public List<Tag> getByOrganizationId(String organizationId) {
    EntityManager em = createEntityManager();
    try {
      return getByOrganizationId(em, organizationId);
    }
    finally {
      close(em);
    }
  }

  public List<Tag> getByOrganizationId(EntityManager em, String organizationId) {
    String sQuery = "SELECT entity FROM Tag entity" + //
        " WHERE entity.organizationId=?1" + //
        " ORDER BY entity.nameLowercaseNoWhitespace";
    return getList(em, sQuery, organizationId);
  }

  public Tag getByOrganizationIdAndName(EntityManager em, String organizationId, String name) {
    if (name == null || name.trim().isEmpty()) {
      throw new DataAccessException("The tag name cannot be null or empty.");
    }

    // Tag Name is whitespace and case insensitive
    name = NameHelper.normalize(name);
    String sQuery = "SELECT entity FROM Tag entity" + //
        " WHERE entity.organizationId=?1 and entity.nameLowercaseNoWhitespace=?2";
    return get(em, sQuery, organizationId, name);
  }

  /**
   * Retrieve list of Tags applied to specified Application
   */
  public List<Tag> getByApplicationId(String applicationId) {
    EntityManager em = createEntityManager();
    try {
      String sQuery = "SELECT tag FROM ApplicationTag appTag, Tag tag" + //
          " WHERE appTag.tagId=tag.id AND appTag.applicationId=?1";
      return getList(em, sQuery, applicationId);
    }
    finally {
      close(em);
    }
  }

  private void validateDescription(String description) {
    if (StringUtils.isEmpty(description)) {
      throw new InvalidTagException("The description is required.");
    }
    if (description.length() > MAX_DESC_SIZE) {
      throw new InvalidTagException("The description cannot be longer than " + MAX_DESC_SIZE
          + " characters, the one supplied has " + description.length() + " characters.");
    }
  }

  @Override
  public void insert(EntityManager em, Tag entity) {
    NameHelper.validate(entity.getName());
    validateDescription(entity.getDescription());

    if (getByOrganizationIdAndName(em, entity.getOrganizationId(), entity.getName()) != null) {
      throw new InvalidNameException(entity.getName() + " is already used as a name.");
    }

    super.insert(em, entity);
  }

  @Override
  public void update(EntityManager em, Tag entity) {
    NameHelper.validate(entity.getName());
    validateDescription(entity.getDescription());

    Tag existingEntity = getByOrganizationIdAndName(em, entity.getOrganizationId(), entity.getName());
    if (existingEntity != null && !existingEntity.getId().equals(entity.getId())) {
      throw new InvalidNameException(entity.getName() + " is already used as a name.");
    }

    super.update(em, entity);
  }

  private Tag getByIdNotNull(EntityManager em, String id) {
    Tag tag = getById(em, id);
    if (tag == null) {
      throw new NotFoundException("Cannot find a tag with id " + id);
    }
    return tag;
  }

  public Tag getByIdNotNull(String id) {
    EntityManager em = createEntityManager();
    try {
      return getByIdNotNull(em, id);
    }
    finally {
      close(em);
    }
  }
}
