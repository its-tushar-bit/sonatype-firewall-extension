/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.tag;

import java.util.List;

import javax.persistence.EntityManager;

import com.sonatype.insight.brain.dataaccess.AbstractOperationalSqlDAO;
import com.sonatype.insight.brain.model.tag.PolicyTag;

/**
 * @since 1.9
 */
public class PolicyTagDAO
    extends AbstractOperationalSqlDAO<PolicyTag>
{
  @Override
  protected PolicyTag getById(EntityManager em, String id) {
    String sQuery = "SELECT entity FROM PolicyTag entity" + //
        " WHERE entity.id=?1";
    return get(em, sQuery, id);
  }

  @Override
  public void update(EntityManager em, PolicyTag entity) {
    throw new UnsupportedOperationException("The PolicyTag table does not support update operations");
  }

  public List<PolicyTag> getByPolicyId(String policyId) {
    String sQuery = "SELECT entity FROM PolicyTag entity" + //
        " WHERE entity.policyId=?1";
    return getList(sQuery, policyId);
  }

  public List<PolicyTag> getByTagId(String tagId) {
    EntityManager em = createEntityManager();
    try {
      return getByTagId(em, tagId);
    }
    finally {
      close(em);
    }
  }

  public List<PolicyTag> getByTagId(EntityManager em, String tagId) {
    String sQuery = "SELECT entity FROM PolicyTag entity" + //
        " WHERE entity.tagId=?1";
    return getList(em, sQuery, tagId);
  }

  public PolicyTag getByPolicyIdAndTagId(String policyId, String tagId) {
    String sQuery = "SELECT entity FROM PolicyTag entity" + //
        " WHERE entity.policyId=?1 AND entity.tagId=?2";
    return get(sQuery, policyId, tagId);
  }
}
