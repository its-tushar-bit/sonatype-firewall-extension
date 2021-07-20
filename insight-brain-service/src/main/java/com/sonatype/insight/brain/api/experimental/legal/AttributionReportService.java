/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.experimental.legal;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;

import com.sonatype.insight.brain.api.v2.dto.legal.AttributionReportTemplateDTO;
import com.sonatype.insight.brain.dataaccess.legal.AttributionReportTemplateDAO;
import com.sonatype.insight.brain.model.legal.AttributionReportTemplate;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.product.license.ProductLicense;
import com.sonatype.insight.brain.security.Authorize;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
public class AttributionReportService
{
  private static final Logger log = LoggerFactory.getLogger(AttributionReportService.class);

  private final AttributionReportTemplateDAO attributionReportTemplateDAO;

  private final ProductLicense productLicense;

  @Inject
  public AttributionReportService(
      final AttributionReportTemplateDAO attributionReportTemplateDAO,
      final ProductLicense productLicense)
  {
    this.attributionReportTemplateDAO = attributionReportTemplateDAO;
    this.productLicense = productLicense;
  }

  /**
   * Create or update a {@link AttributionReportTemplate}. If {@link AttributionReportTemplateDTO#getId()} is null, then
   * the {@link AttributionReportTemplate} will be created. Otherwise, if {@link AttributionReportTemplateDTO#getId()}
   * is not null, then it must correspond to an existing {@link AttributionReportTemplate#getId()} and this will be
   * updated.
   *
   * @param attributionReportTemplateDTO the {@link AttributionReportTemplateDTO} representing the {@link
   *                                     AttributionReportTemplate} to be created/updated.
   * @return a {@link AttributionReportTemplateDTO} representing the created/updated {@link AttributionReportTemplate}.
   * @since 1.120
   */
  @Authorize(permission = Permission.LEGAL_REVIEWER)
  public AttributionReportTemplateDTO saveAttributionReportTemplate(
      final AttributionReportTemplateDTO attributionReportTemplateDTO)
  {
    LegalServiceUtil.checkLicense(productLicense, log);
    AttributionReportTemplate attributionReportTemplate = new AttributionReportTemplate(
        attributionReportTemplateDTO.getId(),
        attributionReportTemplateDTO.getDocumentTitle(),
        attributionReportTemplateDTO.getHeader(),
        attributionReportTemplateDTO.getFooter(),
        attributionReportTemplateDTO.isIncludeTableOfContents(),
        attributionReportTemplateDTO.isIncludeAppendix());
    if (attributionReportTemplate.getId() == null) {
      attributionReportTemplateDAO.insert(attributionReportTemplate);
    }
    else {
      attributionReportTemplateDAO.update(attributionReportTemplate);
    }
    return AttributionReportTemplateDTO.fromReportTemplate(attributionReportTemplate);
  }

  @Authorize(permission = Permission.LEGAL_REVIEWER)
  public Optional<AttributionReportTemplateDTO> getAttributionReportTemplateById(
      String id
  )
  {
    LegalServiceUtil.checkLicense(productLicense, log);
    AttributionReportTemplate attributionReportTemplate = attributionReportTemplateDAO.getById(id);
    return Optional.ofNullable(AttributionReportTemplateDTO.fromReportTemplate(attributionReportTemplate));
  }

  /**
   * Get a list of all available {@link AttributionReportTemplateDTO}
   *
   * @return a list of {@link AttributionReportTemplateDTO} representing the {@link
   * AttributionReportTemplate}s.
   * @since 1.120
   */
  @Authorize(permission = Permission.LEGAL_REVIEWER)
  public List<AttributionReportTemplateDTO> getAllAttributionReportTemplates() {
    LegalServiceUtil.checkLicense(productLicense, log);

    return attributionReportTemplateDAO.getAll().stream()
        .map(AttributionReportTemplateDTO::fromReportTemplate).collect(Collectors.toList());
  }

  /**
   * Delete a {@link AttributionReportTemplate } with single DELETE statement
   *
   * @param attributionReportId
   * @since 1.120
   */
  @Authorize(permission = Permission.LEGAL_REVIEWER)
  public void deleteAttributionReportById(final String attributionReportId) {
    LegalServiceUtil.checkLicense(productLicense, log);
    attributionReportTemplateDAO.deleteById(attributionReportId);
  }
}
