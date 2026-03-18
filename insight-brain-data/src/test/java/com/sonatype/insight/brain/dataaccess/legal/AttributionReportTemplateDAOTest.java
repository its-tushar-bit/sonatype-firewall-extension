/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.legal;

import java.util.Date;
import java.util.List;

import com.sonatype.insight.brain.dataaccess.AbstractDbDAOTest;
import com.sonatype.insight.brain.dataaccess.JPA;
import com.sonatype.insight.brain.model.InvalidNameException;
import com.sonatype.insight.brain.model.legal.AttributionReportTemplate;
import com.sonatype.insight.error.exception.BadRequestException;

import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

public class AttributionReportTemplateDAOTest
    extends AbstractDbDAOTest
{
  private AttributionReportTemplateDAO dao;

  private AttributionReportTemplate createTemplate(
      String templateName,
      String title,
      String header,
      String footer,
      boolean includeAppendix,
      boolean includeTableOfContents,
      boolean includeStandardLicenseText,
      boolean includeInnerSource,
      boolean includeSonatypeSpecialLicenses)
  {
    Date now = new Date();
    AttributionReportTemplate attributionReportTemplate = new AttributionReportTemplate();
    attributionReportTemplate.setLastUpdatedAt(new Date(now.getTime() - 1));
    attributionReportTemplate.setTemplateName(templateName);
    attributionReportTemplate.setDocumentTitle(title);
    attributionReportTemplate.setDocumentHeader(header);
    attributionReportTemplate.setDocumentFooter(footer);
    attributionReportTemplate.setIncludeAppendix(includeAppendix);
    attributionReportTemplate.setIncludeTableOfContents(includeTableOfContents);
    attributionReportTemplate.setIncludeStandardLicenseTexts(includeStandardLicenseText);
    attributionReportTemplate.setIncludeInnerSource(includeInnerSource);
    attributionReportTemplate.setIncludeSonatypeSpecialLicenses(includeSonatypeSpecialLicenses);

    return attributionReportTemplate;
  }

  @Before
  @Override
  public void setup() {
    super.setup();
    dao = daoFactory.createAttributionReportTemplateDAO();
    List<AttributionReportTemplate> reports = dao.getAll();
    reports.forEach(report -> dao.delete(report));
  }

  @Test
  public void testCRUD() {
    // Create
    AttributionReportTemplate attributionReportTemplate =
        createTemplate("template name", "doc title", "header", "footer", false, false, false, false,
            false);
    dao.insert(attributionReportTemplate);
    assertThat(attributionReportTemplate.getId()).isNotNull();

    // Read
    assertThat(dao.getById(attributionReportTemplate.getId())).usingRecursiveComparison(JPA.RECURSIVE_COMPARISON_CONFIG)
        .isEqualTo(attributionReportTemplate);

    // Update
    Date now = new Date();
    attributionReportTemplate.setDocumentTitle("updated document title");
    attributionReportTemplate.setDocumentHeader("updated document header");
    attributionReportTemplate.setDocumentFooter("updated document footer");
    attributionReportTemplate.setIncludeAppendix(true);
    attributionReportTemplate.setIncludeTableOfContents(true);
    attributionReportTemplate.setIncludeStandardLicenseTexts(true);
    attributionReportTemplate.setIncludeInnerSource(true);
    attributionReportTemplate.setIncludeSonatypeSpecialLicenses(true);
    attributionReportTemplate.setLastUpdatedAt(now);
    dao.update(attributionReportTemplate);
    assertThat(dao.getById(attributionReportTemplate.getId())).usingRecursiveComparison()
        .ignoringFields(JPA.IGNORE_FIELDS)
        .usingOverriddenEquals()
        .isEqualTo(attributionReportTemplate);

    // Delete
    dao.delete(attributionReportTemplate);
    assertThat(dao.getById(attributionReportTemplate.getId())).isNull();
  }

  @Test
  public void testGetAll() {
    AttributionReportTemplate attributionReportTemplate1 =
        new AttributionReportTemplate(
            "template 1",
            "title1",
            "header1",
            "footer1",
            false,
            false,
            false,
            false,
            false);
    AttributionReportTemplate attributionReportTemplate2 =
        new AttributionReportTemplate(
            "template2",
            "title2",
            "header2",
            "footer2",
            false,
            false,
            false,
            false,
            false);
    dao.insert(attributionReportTemplate1);
    dao.insert(attributionReportTemplate2);

    List<AttributionReportTemplate> reports = dao.getAll();
    assertThat(reports.size()).isEqualTo(2);
  }

  @Test
  public void testGetByTemplateName() {
    AttributionReportTemplate attributionReportTemplate1 =
        new AttributionReportTemplate(
            "template 1",
            "title1",
            "header1",
            "footer1",
            false,
            false,
            false,
            false,
            false);

    AttributionReportTemplate attributionReportTemplate2 =
        new AttributionReportTemplate(
            "template 2",
            "title2",
            "header2",
            "footer2",
            false,
            false,
            false,
            false,
            false);
    dao.insert(attributionReportTemplate1);
    dao.insert(attributionReportTemplate2);

    assertThat(dao.getByTemplateName("template 2")).usingRecursiveComparison()
        .ignoringFields(JPA.IGNORE_FIELDS)
        .usingOverriddenEquals()
        .isEqualTo(attributionReportTemplate2);
  }

  @Test
  public void testInsert_SetsDateIfNull() {
    AttributionReportTemplate attributionReportTemplate =
        new AttributionReportTemplate(
            "template name",
            "title",
            "header",
            "footer",
            false,
            false,
            false,
            false,
            false);

    attributionReportTemplate.setLastUpdatedAt(null);
    Date now = new Date();

    dao.insert(attributionReportTemplate);

    assertThat(dao.getById(attributionReportTemplate.getId()).getLastUpdatedAt()).isAfterOrEqualTo(now);
  }

  @Test
  public void testUpdate_SetsDate() {
    Date now = new Date();
    AttributionReportTemplate attributionReportTemplate =
        new AttributionReportTemplate("template name", "title", "header", "footer", false, false, false, false, false);
    attributionReportTemplate.setLastUpdatedAt(new Date(now.getTime() - 1));
    dao.insert(attributionReportTemplate);
    assertThat(dao.getById(attributionReportTemplate.getId()).getLastUpdatedAt()).isBefore(now);

    dao.update(attributionReportTemplate);

    assertThat(dao.getById(attributionReportTemplate.getId()).getLastUpdatedAt()).isAfterOrEqualTo(now);
  }

  @Test
  public void testUpdate_DoesNotExist() {
    AttributionReportTemplate attributionReportTemplate =
        new AttributionReportTemplate("template name", "title", "header",
            "footer", false, false, false,
            false, false);
    attributionReportTemplate.setId("doesNotExist");

    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> dao.update(attributionReportTemplate))
        .withMessageContaining(
            "Cannot update attribution report template with id " + attributionReportTemplate.getId() +
                " because it does not exist.");
  }

  @Test
  public void testInsert_TemplateNameTooLong() {
    AttributionReportTemplate attributionReportTemplate =
        new AttributionReportTemplate("my templatemy templatemy templatemy templatemy templatemy " +
            "templatemy templatemy templatemy templatemy templatemy templatemy templatemy templatemy templatemy " +
            "templatemy templatemy templatemy templatemy templatemy templatemy templatemy templatemy templatemy " +
            "templatemy templatemy templatemy templatemy templatemy templatemy templatemy templatemy templatemy " +
            "templatemy templatemy templatemy templatemy templatemy templatemy templatemy templatemy templatemy " +
            "template", "title", "header",
            "footer", false, false, false,
            false, false);

    assertThatExceptionOfType(InvalidNameException.class)
        .isThrownBy(() -> dao.insert(attributionReportTemplate))
        .withMessageContaining(
            "Report template name is too long");
  }

  @Test
  public void testUpdate_TemplateNameTooLong() {
    AttributionReportTemplate attributionReportTemplate =
        new AttributionReportTemplate("my templatemy templatemy templatemy templatemy templatemy " +
            "templatemy templatemy templatemy templatemy templatemy templatemy templatemy templatemy templatemy " +
            "templatemy templatemy templatemy templatemy templatemy templatemy templatemy templatemy templatemy " +
            "templatemy templatemy templatemy templatemy templatemy templatemy templatemy templatemy templatemy " +
            "templatemy templatemy templatemy templatemy templatemy templatemy templatemy templatemy templatemy " +
            "template", "title", "header",
            "footer", false, false, false,
            false, false);

    assertThatExceptionOfType(InvalidNameException.class)
        .isThrownBy(() -> dao.update(attributionReportTemplate))
        .withMessageContaining(
            "Report template name is too long");
  }
}
