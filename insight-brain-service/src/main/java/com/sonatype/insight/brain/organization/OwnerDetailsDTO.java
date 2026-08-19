/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.organization;

import java.util.List;

import com.sonatype.insight.brain.model.label.Label;
import com.sonatype.insight.brain.model.license.LicenseThreatGroup;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.tag.Tag;
import com.sonatype.insight.brain.security.ApplicableMembershipMappings;

/**
 * @since 1.18.0
 */
public class OwnerDetailsDTO
{
  public List<Tag> tags;

  public List<Policy> policies;

  public List<Label> labels;

  public List<LicenseThreatGroup> licenseThreatGroups;

  public ApplicableMembershipMappings roles;
}
