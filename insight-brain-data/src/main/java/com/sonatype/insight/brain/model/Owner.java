/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model;

import com.sonatype.insight.model.HasStringId;

/**
 * Interface for entities that can own other entities, like {@link Organization} and {@link Application}.
 *
 * @since 1.17.0
 */
public interface Owner
    extends HasStringId
{
  String getName();

  String getPublicId();

  String getParentOwnerId();

  boolean canHaveChildren();

  OwnerType getType();
}
