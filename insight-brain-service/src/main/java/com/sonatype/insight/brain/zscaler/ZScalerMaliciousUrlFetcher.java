/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.zscaler;

import java.io.InputStream;

import com.sonatype.insight.brain.api.v2.HasFeature;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;

@HasFeature(SystemConfigurationPropertyFeature.ZSCALER)
public interface ZScalerMaliciousUrlFetcher
{
  InputStream fetchMaliciousUrls(ZScalerSupportedFormat format);
}
