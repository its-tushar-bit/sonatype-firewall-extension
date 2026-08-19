/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.thirdparty;

import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyFile;

public interface ThirdPartyScanResultHandler
{
  FilteredThirdPartyContent handleAndFilterContents(ThirdPartyScanContent content, ThirdPartyFile thirdPartyFile);
}
