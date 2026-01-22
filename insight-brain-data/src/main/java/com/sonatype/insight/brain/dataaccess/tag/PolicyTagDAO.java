/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.tag;

import java.util.List;
import java.util.Set;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.dataaccess.AbstractOperationalSqlDAO;
import com.sonatype.insight.brain.db.datastore.OperationalDataStore;
import com.sonatype.insight.brain.model.tag.PolicyTag;
import com.sonatype.insight.dataaccess.TransactionContext;

/**
 * @since 1.9
 */
@Named
@Singleton
public class PolicyTagDAO
    extends AbstractOperationalSqlDAO<PolicyTag>
{
  @Inject
  public PolicyTagDAO(OperationalDataStore operationalDataStore) {
    super(operationalDataStore);
  }

  @Override
  public void update(TransactionContext tx, PolicyTag entity) {
    throw new UnsupportedOperationException("The PolicyTag table does not support update operations");
  }

  public List<PolicyTag> getByPolicyId(String policyId) {
    try (TransactionContext tx = createTransactionContext()) {
      return getByPolicyId(tx, policyId);
    }
  }

  public List<PolicyTag> getByPolicyId(TransactionContext tx, String policyId) {
    String sQuery = "SELECT entity FROM PolicyTag entity" + //
        " WHERE entity.policyId=?1";
    return getList(tx, sQuery, policyId);
  }

  public List<PolicyTag> getByTagId(String tagId) {
    try (TransactionContext tx = createTransactionContext()) {
      return getByTagId(tx, tagId);
    }
  }

  public List<PolicyTag> getByTagId(TransactionContext tx, String tagId) {
    String sQuery = "SELECT entity FROM PolicyTag entity" + //
        " WHERE entity.tagId=?1";
    return getList(tx, sQuery, tagId);
  }

  /**
   * Retrieve list of PolicyTags for Tags that are owned by the specified Organization
   */
  public List<PolicyTag> getByOrganizationId(String organizationId) {
    String sQuery = "SELECT policyTag FROM PolicyTag policyTag, Tag tag" + //
        " WHERE policyTag.tagId = tag.id AND tag.organizationId =?1";
    return getList(sQuery, organizationId);
  }

  public PolicyTag getByPolicyIdAndTagId(String policyId, String tagId) {
    String sQuery = "SELECT entity FROM PolicyTag entity" + //
        " WHERE entity.policyId=?1 AND entity.tagId=?2";
    return get(sQuery, policyId, tagId);
  }

  public boolean isPolicyApplicable(TransactionContext tx, String policyId, Set<String> tagIds) {
    List<PolicyTag> policyTags = getByPolicyId(tx, policyId);
    if (policyTags.isEmpty()) {
      return true;
    }
    if (!tagIds.isEmpty()) {
      for (PolicyTag policyTag : policyTags) {
        if (tagIds.contains(policyTag.getTagId())) {
          return true;
        }
      }
    }
    return false;
  }
}
