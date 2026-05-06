/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.tag;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.dataaccess.AbstractOperationalSqlDAO;
import com.sonatype.insight.brain.db.datastore.OperationalDataStore;
import com.sonatype.insight.brain.model.tag.PolicyTag;
import com.sonatype.insight.dataaccess.TransactionContext;

import org.jooq.Table;

import static com.sonatype.insight.brain.jooq.generated.ods.tables.PolicyTag.POLICY_TAG;
import static com.sonatype.insight.brain.jooq.generated.ods.tables.Tag.TAG;

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
    return tx.dsl()
        .selectFrom(POLICY_TAG)
        .where(POLICY_TAG.POLICY_ID.eq(policyId))
        .fetch()
        .map(this::toEntity);
  }

  public List<PolicyTag> getByTagId(String tagId) {
    try (TransactionContext tx = createTransactionContext()) {
      return getByTagId(tx, tagId);
    }
  }

  public List<PolicyTag> getByTagId(TransactionContext tx, String tagId) {
    return tx.dsl()
        .selectFrom(POLICY_TAG)
        .where(POLICY_TAG.TAG_ID.eq(tagId))
        .fetch()
        .map(this::toEntity);
  }

  /**
   * Retrieve list of PolicyTags for Tags that are owned by the specified Organization
   */
  public List<PolicyTag> getByOrganizationId(String organizationId) {
    try (TransactionContext tx = createTransactionContext()) {
      return tx.dsl()
          .select(POLICY_TAG.fields())
          .from(POLICY_TAG)
          .join(TAG)
          .on(POLICY_TAG.TAG_ID.eq(TAG.TAG_ID))
          .where(TAG.ORGANIZATION_ID.eq(organizationId))
          .fetch()
          .map(this::toEntity);
    }
  }

  public List<PolicyTag> getByOrganizationIds(Collection<String> organizationIds) {
    try (TransactionContext tx = createTransactionContext()) {
      return getByOrganizationIds(tx, organizationIds);
    }
  }

  public List<PolicyTag> getByOrganizationIds(TransactionContext tx, Collection<String> organizationIds) {
    if (organizationIds == null || organizationIds.isEmpty()) {
      return Collections.emptyList();
    }
    return getListWithSqlInClause(organizationIds,
        ids -> tx.dsl()
            .select(POLICY_TAG.fields())
            .from(POLICY_TAG)
            .join(TAG)
            .on(POLICY_TAG.TAG_ID.eq(TAG.TAG_ID))
            .where(TAG.ORGANIZATION_ID.in(ids))
            .fetch()
            .map(this::toEntity),
        getDataStore());
  }

  public PolicyTag getByPolicyIdAndTagId(String policyId, String tagId) {
    try (TransactionContext tx = createTransactionContext()) {
      return toEntity(tx.dsl()
          .selectFrom(POLICY_TAG)
          .where(POLICY_TAG.POLICY_ID.eq(policyId)
              .and(POLICY_TAG.TAG_ID.eq(tagId)))
          .fetchOne());
    }
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

  @Override
  public Table<?> getJooqTable() {
    return POLICY_TAG;
  }

  @Override
  public Class<PolicyTag> getEntityClass() {
    return PolicyTag.class;
  }
}
