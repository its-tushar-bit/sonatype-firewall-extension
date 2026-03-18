/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service.legal.report;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.stream.Collectors;

import jakarta.inject.Inject;
import jakarta.inject.Named;

import com.sonatype.insight.brain.api.v2.dto.legal.ApiLicenseLegalApplicationReportDTO;
import com.sonatype.insight.brain.api.v2.dto.legal.ApiLicenseLegalComponentDTO;
import com.sonatype.insight.brain.api.v2.dto.legal.ApiLicenseLegalMetadataDTO;
import com.sonatype.insight.brain.api.v2.dto.legal.AttributionReportApplicationDTO;
import com.sonatype.insight.brain.api.v2.dto.legal.LegalSourceLinkDTO;
import com.sonatype.insight.brain.api.v2.service.legal.ApiLicenseLegalService;
import com.sonatype.insight.brain.filter.AdvancedLegalPackDashboardFilter;
import com.sonatype.insight.brain.filter.UserFilterDTO;
import com.sonatype.insight.brain.filter.UserFilterService;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.filter.UserFilterType;
import com.sonatype.insight.brain.model.legal.ComponentLegalPartStatus;
import com.sonatype.insight.brain.model.policy.StageType;
import com.sonatype.insight.brain.model.policy.stages.StageTypes;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.organization.ApplicationService;
import com.sonatype.insight.brain.security.Authorize;
import com.sonatype.insight.brain.security.AuthzContext;
import com.sonatype.insight.brain.security.AuthzContext.Key;
import com.sonatype.insight.brain.security.AuthzFilter;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.NotAuthorizedException;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.shiro.authz.UnauthorizedException;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import static com.sonatype.insight.brain.api.v2.service.legal.LicenseLegalComparators.LEGAL_SOURCE_LINK_COMPARATOR;
import static org.apache.commons.lang3.StringUtils.isBlank;

@Named
public class ApplicationAttributionReportBuilder
{
  private final ApiLicenseLegalService apiLicenseLegalService;

  private final TemplateEngine templateEngine;

  private final ApplicationService applicationService;

  private final UserFilterService userFilterService;

  private static final String APPLICATION_ATTRIBUTION_REPORT = "application_attribution_report";

  @Inject
  public ApplicationAttributionReportBuilder(
      final ApiLicenseLegalService apiLicenseLegalService,
      final ApplicationService applicationService,
      final UserFilterService userFilterService)
  {
    this.apiLicenseLegalService = apiLicenseLegalService;
    this.applicationService = applicationService;
    this.userFilterService = userFilterService;

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

    ApiLicenseLegalApplicationReportDTO applicationReportDTO = apiLicenseLegalService
        .getLicenseLegalApplicationReport(application, stageId, reportParameters.isIncludeInnerSource(),
            reportParameters.isIncludeSonatypeSpecialLicenses());
    Map<String, Object> contextMap = buildContextMap(application, applicationReportDTO, reportParameters);
    return templateEngine.process(APPLICATION_ATTRIBUTION_REPORT, new Context(Locale.getDefault(), contextMap));
  }

  public String generateCustomLegalMultiApplicationAttributionReport(
      Set<AttributionReportApplicationDTO> applicationsAndStages,
      LegalCustomReportParameters reportParameters)
  {
    Set<String> applicationPublicIds =
        applicationsAndStages.stream().map(n -> n.applicationPublicId).collect(Collectors.toSet());
    List<Application> applicationsAuthz = getApplicationsByPublicIds(applicationPublicIds);
    validateAuthorizedApplicationsByPublicId(applicationPublicIds, applicationsAuthz);
    validateReportParameters(reportParameters);
    Map<String, Application> applicationMap =
        applicationsAuthz.stream().collect(Collectors.toMap(Application::getPublicId, Function.identity()));

    List<Owner> applications = new ArrayList<>();
    List<String> stageIds = new ArrayList<>();
    for (AttributionReportApplicationDTO report : applicationsAndStages) {
      applications.add(applicationMap.get(report.applicationPublicId));
      stageIds.add(report.stageTypeName);
    }
    Set<Optional<ApiLicenseLegalApplicationReportDTO>> applicationReportDTOSet = apiLicenseLegalService
        .getLicenseLegalMultiApplicationReport(applications, stageIds, reportParameters.isIncludeInnerSource(),
            reportParameters.isIncludeSonatypeSpecialLicenses());

    ApiLicenseLegalApplicationReportDTO applicationReportDTO = mergeApplicationReports(applicationReportDTOSet);
    Map<String, Object> contextMap = buildContextMap(null, applicationReportDTO, reportParameters);
    return templateEngine.process(APPLICATION_ATTRIBUTION_REPORT, new Context(Locale.getDefault(), contextMap));
  }

  public String generateLegalMultiApplicationAttributionReportFromActiveUserFilter(
      LegalCustomReportParameters reportParameters)
  {
    validateReportParameters(reportParameters);

    UserFilterDTO filterDto;
    Set<String> stagesFilter = new HashSet<>();
    List<Application> applicationsAuthz;
    Set<AttributionReportApplicationDTO> applicationsAndStages;
    Set<String> applicationIds = new LinkedHashSet<>();

    filterDto = userFilterService.getActiveUserFilterForCurrentUser(UserFilterType.ADVANCED_LEGAL_PACK_DASHBOARD);
    if (filterDto.getFilter() != null) {
      AdvancedLegalPackDashboardFilter filter = (AdvancedLegalPackDashboardFilter) filterDto.getFilter();
      applicationIds = new HashSet<>(filter.getApplicationFilters());
      stagesFilter = new HashSet<>(filter.getStageTypeFilters());
    }
    if (stagesFilter.isEmpty()) {
      stagesFilter = StageTypes.getAll().stream().map(StageType::getId).collect(Collectors.toSet());
    }

    applicationsAuthz = getApplicationsByIds(applicationIds);

    if (applicationsAuthz.isEmpty()) {
      String applicationsText = "";
      if (!applicationIds.isEmpty()) {
        List<String> applicationIdsSort = new ArrayList<>(applicationIds);
        Collections.sort(applicationIdsSort);
        applicationsText = String.join(", ", applicationIdsSort) + " ";
      }
      throw new UnauthorizedException("Not authorized to generate report for " + applicationsText + "applications.");
    }

    applicationIds = applicationsAuthz.stream().map(Application::getId).collect(Collectors.toSet());
    applicationsAndStages = resourceDTOFromSet(applicationIds, stagesFilter);

    Map<String, Application> applicationMap =
        applicationsAuthz.stream().collect(Collectors.toMap(Application::getId, Function.identity()));

    List<Owner> applications = new ArrayList<>();
    List<String> stageIds = new ArrayList<>();
    for (AttributionReportApplicationDTO report : applicationsAndStages) {
      applications.add(applicationMap.get(report.applicationId));
      stageIds.add(report.stageTypeName);
    }
    Set<Optional<ApiLicenseLegalApplicationReportDTO>> applicationReportDTOSet =
        apiLicenseLegalService.getLicenseLegalMultiApplicationReport(applications, stageIds,
            reportParameters.isIncludeInnerSource(), reportParameters.isIncludeSonatypeSpecialLicenses());

    ApiLicenseLegalApplicationReportDTO applicationReportDTO = mergeApplicationReports(applicationReportDTOSet);
    Map<String, Object> contextMap = buildContextMap(null, applicationReportDTO, reportParameters);
    return templateEngine.process(APPLICATION_ATTRIBUTION_REPORT, new Context(Locale.getDefault(), contextMap));
  }

  private Set<AttributionReportApplicationDTO> resourceDTOFromSet(Set<String> applicationIds, Set<String> stageIds) {
    Set<AttributionReportApplicationDTO> legalReportResourceApplicationDTO = new HashSet<>();
    applicationIds.forEach(applicationId -> stageIds.forEach(
        stageId -> legalReportResourceApplicationDTO
            .add(new AttributionReportApplicationDTO(applicationId, null, stageId))));
    return legalReportResourceApplicationDTO;
  }

  private ApiLicenseLegalApplicationReportDTO mergeApplicationReports(
      Set<Optional<ApiLicenseLegalApplicationReportDTO>> applicationReportDTOS)
  {
    List<ApiLicenseLegalComponentDTO> components = new ArrayList<>();
    Set<ApiLicenseLegalMetadataDTO> licenseLegalMetadata = new HashSet<>();
    for (Optional<ApiLicenseLegalApplicationReportDTO> apiLicenseLegalApplicationReportDTO : applicationReportDTOS) {
      if (apiLicenseLegalApplicationReportDTO.isPresent()) {
        components.addAll(apiLicenseLegalApplicationReportDTO.get().components.stream()
            .filter(Objects::nonNull)
            .collect(Collectors.toList()));
        licenseLegalMetadata.addAll(apiLicenseLegalApplicationReportDTO.get().licenseLegalMetadata.stream()
            .filter(Objects::nonNull)
            .collect(Collectors.toSet()));
      }
    }
    return new ApiLicenseLegalApplicationReportDTO(components, licenseLegalMetadata);
  }

  private Map<String, Object> buildContextMap(
      final Owner application,
      final ApiLicenseLegalApplicationReportDTO applicationReportDTO,
      final LegalCustomReportParameters reportParameters)
  {
    // Map obligation DB names to human readable names
    Map<String, String> obligationNameMap = new HashMap<>(4);
    obligationNameMap.put("Must State Changes", "Stated Changes");
    obligationNameMap.put("Inclusion of Install Instructions", "Install Instructions");
    obligationNameMap.put("Must Give Credit", "Give Credit");
    obligationNameMap.put(null, "Additional Attributions");

    // Map application data
    Map<String, Object> contextMap = new HashMap<>(3);
    contextMap.put("applicationReport", applicationReportDTO);
    if (application != null) {
      contextMap.put("applicationPublicId", application.getPublicId());
    }
    contextMap.put("obligationNameMap", obligationNameMap);

    // Map Custom Report data
    contextMap.put("reportParameters", reportParameters);

    Map<String, String> licenseIdToLicenseText = applicationReportDTO.licenseLegalMetadata == null
        ? new HashMap<>()
        : applicationReportDTO.licenseLegalMetadata.stream()
            .filter(l -> StringUtils.isNotEmpty(l.licenseText))
            .collect(Collectors.toMap(
                l -> l.licenseId,
                l -> l.licenseText));

    // Map Component Purl --> List<Pair<licenseId, Standard License text>>
    Map<String, List<Pair<String, String>>> purlToStandardLicenseText = new HashMap<>();

    // Map ComponentPurl+'-'+licenseId --> LicenseLink
    // A license will either link to the standard text, or the license file. The logic is easier to determine here than
    // within the thymeleaf template.
    Map<String, String> purlLicenseToLicenseLink = new HashMap<>();

    // Map licenseId --> License Text. Not all licenses of the application will be present in this map, only
    // licenses for components which don't have license files. That is, when a component does not have license files
    // then we need to include standard license text to the report.
    Map<String, String> licenseIdToStandardLicenseText = new HashMap<>();

    // Map Component Purl --> String (formatted link)
    Map<String, String> purlToFormattedLinks = new HashMap<>();
    Map<String, LegalSourceLinkDTO[]> purlToEnabledLinks = new HashMap<>();
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
                    reportParameters.isIncludeAppendix()
                        ? "standard-" + effectiveLicense
                        : componentDTO.packageUrl + "-standard-" + effectiveLicense);
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

          Set<LegalSourceLinkDTO> enabledLinks = getEnabledLinks(componentDTO.licenseLegalData.sourceLinks);
          purlToFormattedLinks.putIfAbsent(componentDTO.packageUrl, formatSourceLink(enabledLinks));
          purlToEnabledLinks.putIfAbsent(componentDTO.packageUrl, enabledLinks.stream()
              .toArray(LegalSourceLinkDTO[]::new));
        }
      }
    }

    contextMap.put("purlToStandardLicenseText", purlToStandardLicenseText);
    contextMap.put("purlLicenseToLicenseLink", purlLicenseToLicenseLink);
    contextMap.put("licenseIdToStandardLicenseText", licenseIdToStandardLicenseText);
    contextMap.put("formattedSourceLinks", purlToFormattedLinks);
    contextMap.put("enabledSourceLinks", purlToEnabledLinks);

    return contextMap;
  }

  private static Set<LegalSourceLinkDTO> getEnabledLinks(Set<LegalSourceLinkDTO> sourceLinks) {
    if (CollectionUtils.isEmpty(sourceLinks)) {
      return new HashSet<>();
    }
    return sourceLinks.stream()
        .filter(l -> l.status.equals(ComponentLegalPartStatus.ENABLED))
        .collect(Collectors.toCollection(() -> new TreeSet<>(LEGAL_SOURCE_LINK_COMPARATOR)));
  }

  public static String formatSourceLink(Set<LegalSourceLinkDTO> sourceLinks) {
    if (CollectionUtils.isNotEmpty(sourceLinks)) {
      LegalSourceLinkDTO[] links = sourceLinks.stream().toArray(LegalSourceLinkDTO[]::new);
      if (links.length == 0 || links[0] == null || links[0].content == null) {
        return "";
      }

      String firstLink = links[0].content;
      if (sourceLinks.size() > 1) {
        String linksCommaSeparated = sourceLinks.stream()
            .map(o -> o.content)
            .collect(Collectors.joining(", "));
        return linksCommaSeparated.length() > 55
            ? linksCommaSeparated.substring(0, 55) + "..., "
            : firstLink + ", ";
      }
      else {
        return firstLink.length() > 55 ? firstLink.substring(0, 55) + "..." : firstLink;
      }
    }
    else {
      return "";
    }
  }

  private void validateReportParameters(final LegalCustomReportParameters reportParameters) {
    if (isBlank(reportParameters.getTitle())) {
      throw new BadRequestException("Report must have title");
    }
  }

  @AuthzFilter(permission = Permission.LEGAL_REVIEWER, context = AuthzFilter.Context.APPLICATION)
  protected List<Application> getApplicationsByPublicIds(Set<String> applicationPublicIds) {
    return applicationService.getByPublicIdsNoAuthz(applicationPublicIds);
  }

  @AuthzFilter(permission = Permission.LEGAL_REVIEWER, context = AuthzFilter.Context.APPLICATION)
  protected List<Application> getApplicationsByIds(Set<String> applicationIds) {
    return applicationService.getApplicationsByIdsAndOrganizationIdsAndTagIdsNoAuthz(null, applicationIds, null);
  }

  private void validateAuthorizedApplicationsByPublicId(
      Set<String> applicationIds,
      List<Application> applicationsAuthz)
  {
    List<String> authorizedApplicationIds =
        applicationsAuthz.stream().map(Application::getPublicId).collect(Collectors.toList());
    Collection<String> unauthorizedApplicationIds = CollectionUtils.subtract(applicationIds, authorizedApplicationIds);
    if (!unauthorizedApplicationIds.isEmpty()) {
      throw new NotAuthorizedException("Insufficient permissions to generate reports for applications: "
          + (String.join(", ", unauthorizedApplicationIds)));
    }
  }
}
