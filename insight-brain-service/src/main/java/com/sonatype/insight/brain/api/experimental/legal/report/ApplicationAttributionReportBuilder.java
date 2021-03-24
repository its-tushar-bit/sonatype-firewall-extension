/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.experimental.legal.report;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;

import com.sonatype.insight.brain.api.experimental.legal.ApiLicenseLegalService;
import com.sonatype.insight.brain.api.v2.dto.legal.ApiLicenseLegalApplicationReportDTO;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.security.Authorize;
import com.sonatype.insight.brain.security.AuthzContext;

import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

@Named
public class ApplicationAttributionReportBuilder
{
  private final ApiLicenseLegalService apiLicenseLegalService;

  private final TemplateEngine templateEngine;

  @Inject
  public ApplicationAttributionReportBuilder(
      final ApiLicenseLegalService apiLicenseLegalService)
  {
    this.apiLicenseLegalService = apiLicenseLegalService;

    ClassLoaderTemplateResolver templateResolver = new ClassLoaderTemplateResolver(this.getClass().getClassLoader());
    templateResolver.setTemplateMode(TemplateMode.HTML);
    templateResolver.setPrefix("/com/sonatype/insight/brain/legal/templates/");
    templateResolver.setSuffix(".html");
    templateEngine = new TemplateEngine();
    templateEngine.setTemplateResolver(templateResolver);
  }

  @Authorize(permission = Permission.LEGAL_REVIEWER)
  public String generateLegalApplicationAttributionReport(
      @AuthzContext(AuthzContext.Key.APPLICATION_PUBLIC_ID) String applicationPublicId)
  {
    ApiLicenseLegalApplicationReportDTO applicationReportDTO =
        apiLicenseLegalService.getLicenseLegalApplicationReport(applicationPublicId);
    Map<String, Object> contextMap = new HashMap<>(2);
    contextMap.put("applicationReport", applicationReportDTO);
    contextMap.put("applicationPublicId", applicationPublicId);
    return templateEngine.process("application_attribution_report", new Context(Locale.getDefault(), contextMap));
  }
}
