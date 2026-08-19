/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.policy.conditions.valuetype;

import java.util.ArrayList;
import java.util.List;

import com.sonatype.insight.brain.dataaccess.license.LicenseThreatGroupDAO;
import com.sonatype.insight.brain.model.license.LicenseThreatGroup;
import com.sonatype.insight.brain.model.policy.ConditionValueType;
import com.sonatype.insight.dataaccess.TransactionContext;

public class LicenseThreatGroupValueType
    implements ConditionValueType<LicenseThreatGroup>
{
  public static final String ID = "LicenseThreatGroupValueType";

  public static final String UNASSIGNED_LICENSE_THREAT_GROUP_ID = "UNASSIGNED_LICENSE_THREAT_GROUP_ID";

  public static final String UNASSIGNED_LICENSE_THREAT_GROUP_NAME = "[unassigned]";

  public static final LicenseThreatGroup UNASSIGNED_LICENSE_THREAT_GROUP;

  static {
    UNASSIGNED_LICENSE_THREAT_GROUP = new LicenseThreatGroup();
    UNASSIGNED_LICENSE_THREAT_GROUP.setId(UNASSIGNED_LICENSE_THREAT_GROUP_ID);
    UNASSIGNED_LICENSE_THREAT_GROUP.setName(UNASSIGNED_LICENSE_THREAT_GROUP_NAME);
  }

  private final TransactionContext tx;

  private final String ownerId;

  private final LicenseThreatGroupDAO licenseThreatGroupDAO;

  public LicenseThreatGroupValueType(
      String ownerId,
      final LicenseThreatGroupDAO licenseThreatGroupDAO)
  {
    this(null, ownerId, licenseThreatGroupDAO);
  }

  public LicenseThreatGroupValueType(
      TransactionContext tx,
      String ownerId,
      final LicenseThreatGroupDAO licenseThreatGroupDAO)
  {
    this.tx = tx;
    this.ownerId = ownerId;
    this.licenseThreatGroupDAO = licenseThreatGroupDAO;
  }

  @Override
  public String getId() {
    return ID;
  }

  @Override
  public String getDataType() {
    return "LicenseThreatGroup";
  }

  @Override
  public boolean isAllowMultiple() {
    return false;
  }

  @Override
  public List<LicenseThreatGroup> getAvailableValues() {
    List<LicenseThreatGroup> result = new ArrayList<>(tx != null
        ? licenseThreatGroupDAO.getByOwnerIdWithHierarchy(tx, ownerId)
        : licenseThreatGroupDAO.getByOwnerIdWithHierarchy(ownerId));
    result.add(UNASSIGNED_LICENSE_THREAT_GROUP);
    return result;
  }
}
