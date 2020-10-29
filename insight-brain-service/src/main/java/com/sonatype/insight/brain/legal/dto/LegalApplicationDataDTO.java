/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.legal.dto;

import java.util.List;

/**
 * @since 1.101
 */
public class LegalApplicationDataDTO
{
  public String applicationPublicId;

  public List<LegalReportComponentDTO> components;

  public LegalApplicationDataDTO(String applicationPublicId, List<LegalReportComponentDTO> components) {
    this.applicationPublicId = applicationPublicId;
    this.components = components;
  }
}
