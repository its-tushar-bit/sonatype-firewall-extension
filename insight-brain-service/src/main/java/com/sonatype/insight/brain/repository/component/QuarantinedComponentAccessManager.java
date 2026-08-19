/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.repository.component;

import java.util.Date;

import com.sonatype.insight.brain.model.repository.QuarantinedComponentAccess;
import com.sonatype.insight.brain.model.repository.ProxyRepositoryComponent;

public interface QuarantinedComponentAccessManager
{
  String createToken(final ProxyRepositoryComponent proxyRepositoryComponent);

  QuarantinedComponentAccess getQuarantinedComponentAccessFromToken(final String token);

  Date getTokenExpiryTime(final Date tokenGenerationTime);
}
