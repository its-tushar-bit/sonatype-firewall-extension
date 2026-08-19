/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.experimental.legal;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import jakarta.inject.Inject;
import jakarta.inject.Named;

import com.sonatype.insight.brain.api.v2.dto.legal.AttributionReportTemplateDTO;
import com.sonatype.insight.brain.dataaccess.legal.AttributionReportTemplateDAO;
import com.sonatype.insight.brain.model.InvalidNameException;
import com.sonatype.insight.brain.model.legal.AttributionReportTemplate;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.product.license.ProductLicense;
import com.sonatype.insight.brain.security.Authorize;
import com.sonatype.insight.brain.security.AuthzContext;
import com.sonatype.insight.brain.security.AuthzContext.Key;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.sonatype.insight.brain.model.Organization.ROOT_ORGANIZATION_ID;

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
   *          AttributionReportTemplate} to be created/updated.
   * @return a {@link AttributionReportTemplateDTO} representing the created/updated {@link AttributionReportTemplate}.
   * @since 1.120
   */
  public AttributionReportTemplateDTO saveAttributionReportTemplate(
      final AttributionReportTemplateDTO attributionReportTemplateDTO)
  {
    checkLegalReviewerPermission(ROOT_ORGANIZATION_ID);
    LegalServiceUtil.checkLicense(productLicense, log);
    AttributionReportTemplate attributionReportTemplate = new AttributionReportTemplate(
        attributionReportTemplateDTO.getId(),
        attributionReportTemplateDTO.getTemplateName(),
        attributionReportTemplateDTO.getDocumentTitle(),
        attributionReportTemplateDTO.getHeader(),
        attributionReportTemplateDTO.getFooter(),
        attributionReportTemplateDTO.isIncludeTableOfContents(),
        attributionReportTemplateDTO.isIncludeAppendix(),
        attributionReportTemplateDTO.isIncludeStandardLicenseTexts(),
        attributionReportTemplateDTO.isIncludeInnerSource(),
        attributionReportTemplateDTO.isIncludeSonatypeSpecialLicenses());

    validateTemplateName(attributionReportTemplate);

    if (attributionReportTemplate.getId() == null) {
      attributionReportTemplateDAO.insert(attributionReportTemplate);
    }
    else {
      attributionReportTemplateDAO.update(attributionReportTemplate);
    }
    return AttributionReportTemplateDTO.fromReportTemplate(attributionReportTemplate);
  }

  private void validateTemplateName(final AttributionReportTemplate attributionReportTemplate) {
    Optional<AttributionReportTemplate> existingTemplate =
        getAttributionReportTemplateByTemplateName(attributionReportTemplate.getTemplateName());

    if (existingTemplate.isPresent()) {
      if (attributionReportTemplate.getId() == null
          ||
          (attributionReportTemplate.getId() != null &&
              !attributionReportTemplate.getId().equals(existingTemplate.get().getId())))
      {
        throw new InvalidNameException(
            String.format("Report template already exists with name %s", attributionReportTemplate.getTemplateName()));
      }
    }
  }

  public Optional<AttributionReportTemplateDTO> getAttributionReportTemplateById(String id) {
    checkLegalReviewerPermission(ROOT_ORGANIZATION_ID);
    return getAttributionReportTemplateById_NoAuthz(id);
  }

  public Optional<AttributionReportTemplateDTO> getAttributionReportTemplateById_NoAuthz(String id) {
    LegalServiceUtil.checkLicense(productLicense, log);
    AttributionReportTemplate attributionReportTemplate = attributionReportTemplateDAO.getById(id);
    return Optional.ofNullable(AttributionReportTemplateDTO.fromReportTemplate(attributionReportTemplate));
  }

  /**
   * Get a list of all available {@link AttributionReportTemplateDTO}
   *
   * @return a list of {@link AttributionReportTemplateDTO} representing the {@link AttributionReportTemplate}s.
   * @since 1.120
   */
  public List<AttributionReportTemplateDTO> getAllAttributionReportTemplates() {
    checkLegalReviewerPermission(ROOT_ORGANIZATION_ID);
    LegalServiceUtil.checkLicense(productLicense, log);

    return attributionReportTemplateDAO.getAll()
        .stream()
        .map(AttributionReportTemplateDTO::fromReportTemplate)
        .collect(Collectors.toList());
  }

  /**
   * Delete a {@link AttributionReportTemplate } with single DELETE statement
   *
   * @param attributionReportId
   * @since 1.120
   */
  public void deleteAttributionReportTemplateById(final String attributionReportId) {
    checkLegalReviewerPermission(ROOT_ORGANIZATION_ID);
    LegalServiceUtil.checkLicense(productLicense, log);
    attributionReportTemplateDAO.deleteById(attributionReportId);
  }

  private Optional<AttributionReportTemplate> getAttributionReportTemplateByTemplateName(final String templateName) {
    LegalServiceUtil.checkLicense(productLicense, log);
    return Optional.ofNullable(attributionReportTemplateDAO.getByTemplateName(templateName));
  }

  // Package visibility for the authz annotations to be effective.
  @Authorize(permission = Permission.LEGAL_REVIEWER)
  void checkLegalReviewerPermission(
      @SuppressWarnings("unused") @AuthzContext(Key.ORGANIZATION_ID) String orgId)
  {
    // Do nothing as this method is only used to perform authz check for the caller
  }
}
