/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.policy.stages;

import com.sonatype.insight.brain.model.policy.StageType;

/**
 * Stage type used for policy evaluation at the time an artifact is deployed to a monitored
 * hosted repository in NXRM (synchronous upload-time enforcement).
 * <p>
 * Mirrors the {@link ProxyStageType} pattern: the identifier is treated as a free-form string
 * in the database ({@code stage_type_id} is a {@code varchar}) so no referential schema is
 * required. The identifier is not yet part of the upstream {@code com.sonatype.clm.dto.model.policy.Stage}
 * valid-stage set; call sites that consult {@code Stage.isValidStageTypeId(...)} must also
 * accept {@link #ID} via an OR-branch, consistent with how {@link ProxyStageType#ID} is handled.
 */
public class HostedStageType
    implements StageType
{
  public static final String ID = "hosted";

  @Override
  public String getId() {
    return ID;
  }

  @Override
  public String getName() {
    return "Hosted";
  }
}
