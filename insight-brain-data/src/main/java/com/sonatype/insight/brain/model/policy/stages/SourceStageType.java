/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.policy.stages;

import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.model.policy.StageType;

/**
 * @since 1.104
 */
public class SourceStageType
    implements StageType
{
  public static final String ID = Stage.ID_SOURCE;

  @Override
  public String getId() {
    return ID;
  }

  @Override
  public String getName() {
    return "Source";
  }
}
