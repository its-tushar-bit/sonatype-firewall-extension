/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.policy.facts;

import com.sonatype.insight.brain.model.license.LicenseThreatGroup;

/**
 * Holds data about a license threat group that triggered a policy condition.
 * Instances of this class are serialized in JSON format in policy violations in the database and
 * they are compared in policy violation comparison.
 * Any change to this class structure or to its JSON serialization may break policy violation comparison.
 */
public class TriggerLicenseThreatGroup
{
  public String id;

  public int threatLevel;

  public TriggerLicenseThreatGroup() {
  }

  public TriggerLicenseThreatGroup(LicenseThreatGroup licenseThreatGroup) {
    id = licenseThreatGroup.getId();
    threatLevel = licenseThreatGroup.getThreatLevel();
  }

  @Override
  public String toString() {
    return "TriggerLicenseThreatGroup [id=" + id + ", threatLevel=" + threatLevel + "]";
  }
}
