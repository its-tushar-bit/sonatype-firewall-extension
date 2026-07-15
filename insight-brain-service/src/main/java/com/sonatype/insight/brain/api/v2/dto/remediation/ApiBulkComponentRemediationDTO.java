/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.dto.remediation;

import java.util.ArrayList;
import java.util.List;

/**
 * @since 1.205
 */
public class ApiBulkComponentRemediationDTO
{
  public List<ApiBulkComponentRemediationResultDTO> results = new ArrayList<>();

  public ApiBulkComponentRemediationDTO() {
  }

  public ApiBulkComponentRemediationDTO(List<ApiBulkComponentRemediationResultDTO> results) {
    this.results = results;
  }
}
