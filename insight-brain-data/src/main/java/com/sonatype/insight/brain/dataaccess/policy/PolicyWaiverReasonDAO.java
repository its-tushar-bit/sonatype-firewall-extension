/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.policy;

import com.sonatype.insight.brain.dataaccess.AbstractOperationalSqlDAO;
import com.sonatype.insight.brain.db.datastore.OperationalDataStore;
import com.sonatype.insight.brain.model.policy.PolicyWaiverReason;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.util.List;

@Named
@Singleton
public class PolicyWaiverReasonDAO
    extends AbstractOperationalSqlDAO<PolicyWaiverReason>
{
  @Inject
  public PolicyWaiverReasonDAO(OperationalDataStore operationalDataStore) {
    super(operationalDataStore);
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
