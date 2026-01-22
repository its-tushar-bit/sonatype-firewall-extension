/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.policy;

import com.sonatype.insight.brain.dataaccess.AbstractOperationalSqlDAO;
import com.sonatype.insight.brain.db.datastore.OperationalDataStore;
import com.sonatype.insight.brain.model.policy.PolicyWaiverReason;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import java.util.List;
import java.util.Map;

import static java.util.function.UnaryOperator.identity;
import static java.util.stream.Collectors.toMap;

@Named
@Singleton
public class PolicyWaiverReasonDAO
    extends AbstractOperationalSqlDAO<PolicyWaiverReason>
{
  @Inject
  public PolicyWaiverReasonDAO(OperationalDataStore operationalDataStore) {
    super(operationalDataStore);
  }

  @Override
  public List<PolicyWaiverReason> getAll() {
    final String sQuery = "SELECT entity FROM PolicyWaiverReason entity" +
        "  ORDER BY " +
        "    entity.sortOrder ASC," +
        // this is a fallback for when sortOrder is null, it guarantees determinant behavior, currently we are providing
        // sort_order values for all rows so this should not get hit, but we have left the column nullable because there
        // is discussion around
        "    entity.reasonText ASC";

    return getList(sQuery);
  }

  // Returns all waivers reasons as a convenient lookup map;
  // Call this once, outside any loops, so we don't go the db over and over;
  // The number of waivers reasons is very small and should always be very small;
  // We can get all of them once per request, keep them in memory, and just look them up id from this map;
  public Map<String, PolicyWaiverReason> getPolicyWaiverReasonIdToPolicyWaiverReasonMap() {
    return getAll()
        .stream()
        .collect(toMap(PolicyWaiverReason::getId, identity(), (existing, replacement) -> existing));
  }

  public List<PolicyWaiverReason> getAllByIds(List<String> policyWaiverReasonIds) {
    String sQuery = "SELECT entity FROM PolicyWaiverReason entity" + //
        " WHERE entity.id IN ?1";
    return getList(sQuery, policyWaiverReasonIds);
  }

  public PolicyWaiverReason getByReasonText(String reasontext) {
    String sQuery = "SELECT entity FROM PolicyWaiverReason entity" + //
        " WHERE entity.reasonText=?1";
    return get(sQuery, reasontext);
  }
}
