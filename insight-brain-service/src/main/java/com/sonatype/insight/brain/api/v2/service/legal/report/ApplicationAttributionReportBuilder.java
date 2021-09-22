/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service.legal.report;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;

import com.sonatype.insight.brain.api.v2.dto.legal.ApiLicenseLegalApplicationReportDTO;
import com.sonatype.insight.brain.api.v2.dto.legal.ApiLicenseLegalComponentDTO;
import com.sonatype.insight.brain.api.v2.service.legal.ApiLicenseLegalService;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.security.Authorize;
import com.sonatype.insight.brain.security.AuthzContext;
import com.sonatype.insight.brain.security.AuthzContext.Key;
import com.sonatype.insight.error.exception.BadRequestException;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import static org.apache.commons.lang3.StringUtils.isBlank;

@Named
public class ApplicationAttributionReportBuilder
{
  private final ApiLicenseLegalService apiLicenseLegalService;

  private final TemplateEngine templateEngine;

  @Inject
  public ApplicationAttributionReportBuilder(final ApiLicenseLegalService apiLicenseLegalService) {
    this.apiLicenseLegalService = apiLicenseLegalService;

    ClassLoaderTemplateResolver templateResolver = new ClassLoaderTemplateResolver(this.getClass().getClassLoader());
    templateResolver.setTemplateMode(TemplateMode.HTML);
    templateResolver.setPrefix("/com/sonatype/insight/brain/legal/templates/");
    templateResolver.setSuffix(".html");
    templateEngine = new TemplateEngine();
    templateEngine.setTemplateResolver(templateResolver);
  }

  @Authorize(permission = Permission.LEGAL_REVIEWER)
  public String generateCustomLegalApplicationAttributionReport(
      @AuthzContext(Key.OWNER) Owner application,
      String stageId,
      LegalCustomReportParameters reportParameters)
  {
    validateReportParameters(reportParameters);

    ApiLicenseLegalApplicationReportDTO applicationReportDTO =
        apiLicenseLegalService.getLicenseLegalApplicationReport(application, stageId);
    Map<String, Object> contextMap = buildContextMap(application, applicationReportDTO, reportParameters);
    return templateEngine.process("application_attribution_report", new Context(Locale.getDefault(), contextMap));
  }

  private Map<String, Object> buildContextMap(
      final Owner application,
      final ApiLicenseLegalApplicationReportDTO applicationReportDTO,
      final LegalCustomReportParameters reportParameters)
  {
    //Map obligation DB names to human readable names
    Map<String, String> obligationNameMap = new HashMap<>(4);
    obligationNameMap.put("Must State Changes", "Stated Changes");
    obligationNameMap.put("Inclusion of Install Instructions", "Install Instructions");
    obligationNameMap.put("Must Give Credit", "Give Credit");
    obligationNameMap.put(null, "Additional Attributions");

    //Map application data
    Map<String, Object> contextMap = new HashMap<>(3);
    contextMap.put("applicationReport", applicationReportDTO);
    contextMap.put("applicationPublicId", application.getPublicId());
    contextMap.put("obligationNameMap", obligationNameMap);

    //Map Custom Report data
    contextMap.put("reportParameters", reportParameters);

    Map<String, String> licenseIdToLicenseText = applicationReportDTO.licenseLegalMetadata == null ? new HashMap<>() :
        applicationReportDTO.licenseLegalMetadata.stream()
            .filter(l -> StringUtils.isNotEmpty(l.licenseText))
            .collect(Collectors.toMap(
                l -> l.licenseId,
                l -> l.licenseText
            ));

    //Map Component Purl --> List<Pair<licenseId, Standard License text>>
    Map<String, List<Pair<String, String>>> purlToStandardLicenseText = new HashMap<>();

    //Map ComponentPurl+'-'+licenseId --> LicenseLink
    // A license will either link to the standard text, or the license file. The logic is easier to determine here than
    // within the thymeleaf template.
    Map<String, String> purlLicenseToLicenseLink = new HashMap<>();

    //Map licenseId --> License Text. Not all licenses of the application will be present in this map, only
    // licenses for components which don't have license files. That is, when a component does not have license files
    // then we need to include standard license text to the report.
    Map<String, String> licenseIdToStandardLicenseText = new HashMap<>();

    if (applicationReportDTO.components != null) {
      for (ApiLicenseLegalComponentDTO componentDTO : applicationReportDTO.components) {
        boolean hasLicenseFiles = CollectionUtils.isNotEmpty(componentDTO.licenseLegalData.licenseFiles);

        boolean requiresStandardLicense = !hasLicenseFiles &&
            reportParameters.isIncludeStandardLicenseTexts() &&
            CollectionUtils.isNotEmpty(componentDTO.licenseLegalData.effectiveLicenses);

        for (String effectiveLicense : componentDTO.licenseLegalData.effectiveLicenses) {
          final String purlLicenseKey = componentDTO.packageUrl + "-" + effectiveLicense;

          if (requiresStandardLicense && licenseIdToLicenseText.containsKey(effectiveLicense)) {
            purlToStandardLicenseText.computeIfAbsent(componentDTO.packageUrl,
                k -> new ArrayList<>()).add(Pair.of(effectiveLicense, licenseIdToLicenseText.get(effectiveLicense)));
            purlLicenseToLicenseLink
                .put(purlLicenseKey,
                    reportParameters.isIncludeAppendix() ?
                        "standard-" + effectiveLicense : componentDTO.packageUrl + "-standard-" + effectiveLicense);
            licenseIdToStandardLicenseText.putIfAbsent(effectiveLicense, licenseIdToLicenseText.get(effectiveLicense));
          }
          else if (hasLicenseFiles) {
            purlLicenseToLicenseLink
                .put(purlLicenseKey,
                    componentDTO.packageUrl + "-license-files");
          }
          else {
            purlLicenseToLicenseLink
                .put(purlLicenseKey, componentDTO.packageUrl);
          }
        }
      }
    }

    contextMap.put("purlToStandardLicenseText", purlToStandardLicenseText);
    contextMap.put("purlLicenseToLicenseLink", purlLicenseToLicenseLink);
    contextMap.put("licenseIdToStandardLicenseText", licenseIdToStandardLicenseText);

    return contextMap;
  }

  private void validateReportParameters(final LegalCustomReportParameters reportParameters) {
    if (isBlank(reportParameters.getTitle())) {
      throw new BadRequestException("Report must have title");
    }
  }
}
