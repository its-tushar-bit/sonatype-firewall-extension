/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import java.util.List;

import com.sonatype.insight.brain.api.v2.dto.legal.AttributionReportTemplateDTO;

/**
 * Resource for API License Legal
 */
public interface ApiLicenseLegalReportResourceV2
{
  AttributionReportTemplateDTO getAttributionReportTemplateById(final String reportId);

  List<AttributionReportTemplateDTO> getAllAttributionReportTemplates();

  AttributionReportTemplateDTO saveAttributionReportTemplate(AttributionReportTemplateDTO reportTemplateDTO);

  void deleteAttributionReportTemplate(String id);
}
