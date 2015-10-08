/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.version;

import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.Path;

import com.sonatype.insight.brain.product.license.UnlicensedPath;

/**
 * @deprecated As of 1.17 superseded by {@link VersionResource}.
 */
@Deprecated
@Named
@Path(DeprecatedVersionResource.RESOURCE_PATH)
@UnlicensedPath
public class DeprecatedVersionResource
    extends VersionResource
{
  public static final String RESOURCE_PATH = "rest/version";

  @Inject
  public DeprecatedVersionResource(VersionService versionService) {
    super(versionService);
  }
}
