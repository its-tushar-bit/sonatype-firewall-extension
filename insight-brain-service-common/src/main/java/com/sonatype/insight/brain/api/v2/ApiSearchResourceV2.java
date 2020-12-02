/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.api.v2.dto.ApiSearchResultsDTOV2;

/**
 * Resource for API Search
 */
public interface ApiSearchResourceV2
{
  /**
   * Searches all currently registered applications for a component matching the given search criteria. A component can
   * be searched for by its hash or its coordinates (or its equivalent packageUrl format), the latter supporting
   * wildcards like the equivalent policy condition. The mandatory stageId parameter restricts which scans/reports
   * of the applications are inspected for the component.
   */
  ApiSearchResultsDTOV2 searchComponent(String stageId,
                                        String hash,
                                        ComponentIdentifier componentIdentifier,
                                        String packageUrl);
}
