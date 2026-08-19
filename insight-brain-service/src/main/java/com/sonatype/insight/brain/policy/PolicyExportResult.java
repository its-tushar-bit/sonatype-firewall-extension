/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy;

import java.util.Collections;
import java.util.List;

import com.sonatype.insight.brain.model.label.Label;
import com.sonatype.insight.brain.model.license.LicenseThreatGroup;
import com.sonatype.insight.brain.model.license.LicenseThreatGroupLicense;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.tag.PolicyTag;
import com.sonatype.insight.brain.model.tag.Tag;

public class PolicyExportResult
{
  public List<Policy> policies = Collections.emptyList();

  public List<Label> labels = Collections.emptyList();

  public List<LicenseThreatGroup> licenseThreatGroups = Collections.emptyList();

  public List<LicenseThreatGroupLicense> licenseThreatGroupLicenses = Collections.emptyList();

  /**
   * @since 1.9
   */
  public List<Tag> tags = Collections.emptyList();

  /**
   * @since 1.9
   */
  public List<PolicyTag> policyTags = Collections.emptyList();
}
