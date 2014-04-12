/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.utils.IdUtils;

/**
 * @since 1.11.0
 */
class PublicIdOwnerMapper
    implements OwnerMapper
{
  @Override
  public String getInternalOwnerId(final String ownerType, final String ownerId) {
    return IdUtils.getInternalOwnerId(ownerType, ownerId);
  }

  @Override
  public String getExternalId(final Application app) {
    return app.getPublicId();
  }
}
