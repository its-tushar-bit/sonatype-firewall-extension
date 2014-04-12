/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import com.sonatype.insight.brain.model.Application;

/**
 * @since 1.11.0
 */
interface OwnerMapper
{

  String getInternalOwnerId(String ownerType, String ownerId);

  String getExternalId(Application app);
}
