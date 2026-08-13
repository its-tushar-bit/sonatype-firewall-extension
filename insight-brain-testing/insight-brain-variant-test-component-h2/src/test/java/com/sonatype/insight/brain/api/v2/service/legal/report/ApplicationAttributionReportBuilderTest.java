/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service.legal.report;

import static com.sonatype.insight.brain.api.v2.service.legal.LicenseLegalComparators.LEGAL_SOURCE_LINK_COMPARATOR;
import static com.sonatype.insight.brain.model.filter.UserFilter.ACTIVE_FILTER_NAME;
import static com.sonatype.insight.brain.model.filter.UserFilterType.ADVANCED_LEGAL_PACK_DASHBOARD;
import static com.sonatype.insight.brain.model.license.License.UNSPECIFIED_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.booleanThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.api.v2.dto.ApiComponentDTOV2;
import com.sonatype.insight.brain.api.v2.dto.legal.ApiLicenseLegalApplicationReportDTO;
import com.sonatype.insight.brain.api.v2.dto.legal.ApiLicenseLegalComponentDTO;
import com.sonatype.insight.brain.api.v2.dto.legal.ApiLicenseLegalCopyrightDTO;
import com.sonatype.insight.brain.api.v2.dto.legal.ApiLicenseLegalDataDTO;
import com.sonatype.insight.brain.api.v2.dto.legal.ApiLicenseLegalFileDTO;
import com.sonatype.insight.brain.api.v2.dto.legal.ApiLicenseLegalMetadataDTO;
import com.sonatype.insight.brain.api.v2.dto.legal.AttributionReportApplicationDTO;
import com.sonatype.insight.brain.api.v2.dto.legal.ComponentObligationAttributionDTO;
import com.sonatype.insight.brain.api.v2.dto.legal.LegalSourceLinkDTO;
import com.sonatype.insight.brain.api.v2.service.legal.ApiLicenseLegalService;
import com.sonatype.insight.brain.filter.AdvancedLegalPackDashboardFilter;
import com.sonatype.insight.brain.filter.UserFilterDTO;
import com.sonatype.insight.brain.filter.UserFilterService;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.filter.UserFilter;
import com.sonatype.insight.brain.model.filter.UserFilterType;
import com.sonatype.insight.brain.model.legal.ComponentLegalPartStatus;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;
import com.sonatype.insight.brain.organization.ApplicationService;
import com.sonatype.insight.brain.security.InternalRealm;
import com.sonatype.insight.brain.variant.AbstractComponentH2Test;
import com.sonatype.insight.brain.variant.ComponentH2Test;
import com.sonatype.insight.json.store.JsonUtils;
import com.sonatype.insight.purl.PackageUrlIdentifier;
import jakarta.inject.Inject;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import org.apache.commons.io.IOUtils;
import org.assertj.core.util.Lists;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

@ComponentH2Test
public class ApplicationAttributionReportBuilderTest
    extends AbstractComponentH2Test
{
  @Mock
  private ApiLicenseLegalService mockApiLicenseLegalService;

  @Mock
  private ApplicationService mockApplicationService;

  @Mock
  UserFilterService mockUserFilterService;

  @Inject
  private ApplicationAttributionReportBuilder reportBuilder;

  @Test
  public void testDefaultSuccessfulReport() throws IOException {
    Application application = tempEntity.newApplicationWithParent("appId");
    generateReportDataAndMocks(application, true);

    String content = reportBuilder.generateCustomLegalApplicationAttributionReport(application, BuildStageType.ID,
        LegalCustomReportParameters.builder().buildWithDefaults(application.getPublicId()));
    Document doc = Jsoup.parse(content);

    String bodyContent = doc.select("body").first().toString();

    String expectedContent = IOUtils.toString(Objects.requireNonNull(getClass().getClassLoader()
        .getResource("ApplicationAttributionReportTest/expectedApplicationAttributionReport.html")),
        StandardCharsets.UTF_8);

    assertThat(bodyContent).isEqualToIgnoringWhitespace(expectedContent);

    assertThat(doc.select("#table-of-contents")).isNotEmpty();
    assertThat(doc.select("h1").first()).hasToString("<h1>Attribution Report for appId</h1>");
    assertThat(doc.select("#appendix")).isNotEmpty();
    assertThat(doc.select("#header")).isEmpty();
    assertThat(doc.select("#footer")).isEmpty();
    assertThat(doc.select("#additional-notices")).isEmpty();

    // Ensures that the appendix contains this license
    assertThat(doc.select("#standard-LicenseOne")).isNotEmpty();
  }

  @Test
  public void testDefaultSuccessfulReportMultiApp() throws IOException {
    Set<AttributionReportApplicationDTO> applicationsAndStages = new HashSet<>();
    Application application = tempEntity.newApplicationWithParent("appId");
    AttributionReportApplicationDTO app1 = new AttributionReportApplicationDTO();
    app1.applicationPublicId = application.getPublicId();
    app1.stageTypeName = BuildStageType.ID;
    applicationsAndStages.add(app1);
    Application application2 = tempEntity.newApplicationWithParent("appId2");
    AttributionReportApplicationDTO app2 = new AttributionReportApplicationDTO();
    app2.applicationPublicId = application2.getPublicId();
    app2.stageTypeName = BuildStageType.ID;
    applicationsAndStages.add(app2);
    generateMultiReportDataAndMocks(Lists.newArrayList(application, application2), true);
    when(mockApplicationService
        .getByPublicIdsNoAuthz(new HashSet<>(Arrays.asList(application.getPublicId(), application2.getPublicId()))))
            .thenReturn(Arrays.asList(application, application2));
    LegalCustomReportParameters reportParameters =
        LegalCustomReportParameters.builder()
            .buildMultiApplicationWithDefaults(
                new LinkedHashSet<>(Arrays.asList(application.getPublicId(), application2.getPublicId())));
    String content =
        reportBuilder.generateCustomLegalMultiApplicationAttributionReport(applicationsAndStages, reportParameters);
    Document doc = Jsoup.parse(content);
    String bodyContent = doc.select("body").first().toString();
    String expectedContent = IOUtils.toString(
        Objects.requireNonNull(getClass().getClassLoader()
            .getResource("ApplicationAttributionReportTest/expectedMultiApplicationAttributionReport.html")),
        StandardCharsets.UTF_8);

    assertThat(bodyContent).isEqualToIgnoringWhitespace(expectedContent);
    assertThat(doc.select("#table-of-contents")).isNotEmpty();
    assertThat(doc.select("h1").first()).hasToString("<h1>Attribution Report for appId, appId2</h1>");
    assertThat(doc.select("#appendix")).isNotEmpty();
    assertThat(doc.select("#header")).isEmpty();
    assertThat(doc.select("#footer")).isEmpty();
    assertThat(doc.select("#additional-notices")).isEmpty();

    // Ensures that the appendix contains this license
    assertThat(doc.select("#standard-LicenseOne")).isNotEmpty();
  }

  @Test
  public void testDefaultSuccessfulReportFromActiveFilter() throws IOException {
    String filterName = "test filter";
    Application application = tempEntity.newApplicationWithParent("appId");
    Application application2 = tempEntity.newApplicationWithParent("appId2");
    AdvancedLegalPackDashboardFilter advancedLegalPackDashboardFilter = new AdvancedLegalPackDashboardFilter(null,
        Arrays.asList(application.getId(), application2.getId()), null, Collections.singletonList(BuildStageType.ID),
        null);
    when(mockUserFilterService.getActiveUserFilterForCurrentUser(UserFilterType.ADVANCED_LEGAL_PACK_DASHBOARD))
        .thenReturn(newUserFilterDTO(tempEntity.newUserFilter("Test User", InternalRealm.ID, ACTIVE_FILTER_NAME,
            ADVANCED_LEGAL_PACK_DASHBOARD, JsonUtils.format(advancedLegalPackDashboardFilter), filterName)));
    generateMultiReportDataAndMocks(Lists.newArrayList(application, application2), true);

    List<Application> appIdList = Arrays.asList(application, application2);
    when(mockApplicationService.getApplicationsByIdsAndOrganizationIdsAndTagIdsNoAuthz(null,
        new LinkedHashSet<>(Arrays.asList(application.getId(), application2.getId())), null)).thenReturn(appIdList);
    String title = String.join(", ",
        appIdList.stream().map(Application::getPublicId).sorted().collect(Collectors.toCollection(LinkedHashSet::new)));
    String content = reportBuilder.generateLegalMultiApplicationAttributionReportFromActiveUserFilter(
        LegalCustomReportParameters.builder().buildWithDefaults(title));
    Document doc = Jsoup.parse(content);
    String bodyContent = doc.select("body").first().toString();
    String expectedContent = IOUtils.toString(
        Objects.requireNonNull(getClass().getClassLoader()
            .getResource("ApplicationAttributionReportTest/expectedMultiApplicationAttributionReport.html")),
        StandardCharsets.UTF_8);
    assertThat(bodyContent).isEqualToIgnoringWhitespace(expectedContent);
    assertThat(doc.select("#table-of-contents")).isNotEmpty();
    assertThat(doc.select("h1").first()).hasToString("<h1>Attribution Report for " + title + "</h1>");
    assertThat(doc.select("#appendix")).isNotEmpty();
    assertThat(doc.select("#header")).isEmpty();
    assertThat(doc.select("#footer")).isEmpty();
    assertThat(doc.select("#additional-notices")).isEmpty();
    // Ensures that the appendix contains this license
    assertThat(doc.select("#standard-LicenseOne")).isNotEmpty();
  }

  @Test
  public void testDefaultSuccessfulReportMultiAppWithNoticeFile() throws IOException {
    Set<AttributionReportApplicationDTO> applicationsAndStages = new HashSet<>();
    Application application = tempEntity.newApplicationWithParent("appId");
    AttributionReportApplicationDTO app1 = new AttributionReportApplicationDTO();
    app1.applicationPublicId = application.getPublicId();
    app1.stageTypeName = BuildStageType.ID;
    applicationsAndStages.add(app1);
    Application application2 = tempEntity.newApplicationWithParent("appId2");
    AttributionReportApplicationDTO app2 = new AttributionReportApplicationDTO();
    app2.applicationPublicId = application2.getPublicId();
    app2.stageTypeName = BuildStageType.ID;
    applicationsAndStages.add(app2);
    generateMultiReportDataAndMocks(Lists.newArrayList(application, application2), true);
    when(mockApplicationService
        .getByPublicIdsNoAuthz(new HashSet<>(Arrays.asList(application.getPublicId(), application2.getPublicId()))))
            .thenReturn(Arrays.asList(application, application2));

    LegalCustomReportParameters reportParameters = LegalCustomReportParameters.builder()
        .withNoticeFiles(Lists.newArrayList("First Notice File Content", "Second Notice File Content"))
        .buildMultiApplicationWithDefaults(
            new LinkedHashSet<>(Arrays.asList(application.getPublicId(), application2.getPublicId())));
    String content =
        reportBuilder.generateCustomLegalMultiApplicationAttributionReport(applicationsAndStages, reportParameters);
    Document doc = Jsoup.parse(content);
    String bodyContent = doc.select("body").first().toString();
    String expectedContent = IOUtils.toString(
        Objects.requireNonNull(getClass().getClassLoader()
            .getResource("ApplicationAttributionReportTest/expectedMultiApplicationAttributionReportWithNotice.html")),
        StandardCharsets.UTF_8);
    assertThat(bodyContent).isEqualToIgnoringWhitespace(expectedContent);
    assertThat(doc.select("#table-of-contents")).isNotEmpty();
    assertThat(doc.select("h1").first()).hasToString("<h1>Attribution Report for appId, appId2</h1>");
    assertThat(doc.select("#appendix")).isNotEmpty();
    assertThat(doc.select("#header")).isEmpty();
    assertThat(doc.select("#footer")).isEmpty();
    assertThat(doc.select("#additional-notices")).isNotEmpty();
    assertThat(doc.select("#additional-notices").first().toString()).contains("First Notice File Content");
    assertThat(doc.select("#additional-notices").first().toString()).contains("Second Notice File Content");
    // Ensures that the appendix contains this license
    assertThat(doc.select("#standard-LicenseOne")).isNotEmpty();
  }

  @Test
  public void testNoTableOfContent() {
    Application application = tempEntity.newApplicationWithParent("appId");
    generateReportDataAndMocks(application, true);

    String content = reportBuilder.generateCustomLegalApplicationAttributionReport(application, BuildStageType.ID,
        LegalCustomReportParameters.builder()
            .withTitle("My Report")
            .withIncludeToc(false)
            .build());

    Document doc = Jsoup.parse(content);

    assertThat(doc.select("#table-of-contents")).isEmpty();
    assertThat(doc.select("h1").first()).hasToString("<h1>My Report</h1>");
    assertThat(doc.select("#appendix")).isNotEmpty();
    assertThat(doc.select("#header")).isEmpty();
    assertThat(doc.select("#footer")).isEmpty();
    assertThat(doc.select("#additional-notices")).isEmpty();

    // Ensures that the appendix contains this license
    assertThat(doc.select("#standard-LicenseOne")).isNotEmpty();
  }

  @Test
  public void testNoStandardLicenseTextNoAppendix() {
    Application application = tempEntity.newApplicationWithParent("appId");
    generateReportDataAndMocks(application, true);

    String content = reportBuilder.generateCustomLegalApplicationAttributionReport(application, BuildStageType.ID,
        LegalCustomReportParameters.builder()
            .withIncludeStandardLicenseTexts(false)
            .withIncludeAppendix(false)
            .withTitle("My Report")
            .build());

    Document doc = Jsoup.parse(content);

    assertThat(doc.select("#table-of-contents")).isNotEmpty();
    assertThat(doc.select("h1").first()).hasToString("<h1>My Report</h1>");
    assertThat(doc.select("#appendix")).isEmpty();
    assertThat(doc.select("#header")).isEmpty();
    assertThat(doc.select("#footer")).isEmpty();
    assertThat(doc.select("#additional-notices")).isEmpty();

    // Ensures that the appendix doesn't contain this license
    assertThat(doc.select("#standard-LicenseOne")).isEmpty();
  }

  @Test
  public void testNoAppendix() {
    Application application = tempEntity.newApplicationWithParent("appId");
    generateReportDataAndMocks(application, true);

    String content = reportBuilder.generateCustomLegalApplicationAttributionReport(application, BuildStageType.ID,
        LegalCustomReportParameters.builder()
            .withIncludeAppendix(false)
            .withTitle("My Report")
            .build());

    Document doc = Jsoup.parse(content);

    assertThat(doc.select("#table-of-contents")).isNotEmpty();
    assertThat(doc.select("h1").first()).hasToString("<h1>My Report</h1>");
    assertThat(doc.select("#appendix")).isEmpty();
    assertThat(doc.select("#header")).isEmpty();
    assertThat(doc.select("#footer")).isEmpty();
    assertThat(doc.select("#additional-notices")).isEmpty();

    // Ensures that the appendix doesn't contain this license
    assertThat(doc.select("#standard-LicenseOne")).isEmpty();

    // component 1 contains LicenseOne but has license files, therefore no license text
    assertThat(doc.select("#purl1-license-files")).isNotEmpty();
    assertThat(doc.select("#purl1-standard-LicenseOne")).isEmpty();

    // Component 3 contains LicenseOne with no License files, therefore we should show Standard License Text in the
    // component box
    assertThat(doc.select("#purl3-standard-LicenseOne")).isNotEmpty();
    assertThat(doc.select("#purl3-license-files")).isEmpty();
  }

  @Test
  public void testAppendixDontIncludeStandardLicenseText() {
    Application application = tempEntity.newApplicationWithParent("appId");
    generateReportDataAndMocks(application, true);

    String content = reportBuilder.generateCustomLegalApplicationAttributionReport(application, BuildStageType.ID,
        LegalCustomReportParameters.builder()
            .withIncludeAppendix(true)
            .withIncludeStandardLicenseTexts(false)
            .withTitle("My Report")
            .build());

    Document doc = Jsoup.parse(content);

    assertThat(doc.select("#table-of-contents")).isNotEmpty();
    assertThat(doc.select("h1").first()).hasToString("<h1>My Report</h1>");
    assertThat(doc.select("#appendix")).isEmpty();
    assertThat(doc.select("#header")).isEmpty();
    assertThat(doc.select("#footer")).isEmpty();
    assertThat(doc.select("#additional-notices")).isEmpty();

    assertThat(doc.select("#standard-LicenseOne")).isEmpty();
    assertThat(doc.select("#purl1-standard-LicenseOne")).isEmpty();
    assertThat(doc.select("#purl3-standard-LicenseOne")).isEmpty();
  }

  @Test
  public void testAppendixEmptyStandardLicenseText() {
    Application application = tempEntity.newApplicationWithParent("appId");
    generateReportDataAndMocks(application, false);

    String content = reportBuilder.generateCustomLegalApplicationAttributionReport(application, BuildStageType.ID,
        LegalCustomReportParameters.builder()
            .withIncludeAppendix(true)
            .withIncludeStandardLicenseTexts(false)
            .withTitle("My Report")
            .build());

    Document doc = Jsoup.parse(content);

    assertThat(doc.select("#table-of-contents")).isNotEmpty();
    assertThat(doc.select("h1").first()).hasToString("<h1>My Report</h1>");
    assertThat(doc.select("#appendix")).isEmpty();
    assertThat(doc.select("#header")).isEmpty();
    assertThat(doc.select("#footer")).isEmpty();
    assertThat(doc.select("#additional-notices")).isEmpty();

    assertThat(doc.select("#standard-LicenseOne")).isEmpty();
    assertThat(doc.select("#purl1-standard-LicenseOne")).isEmpty();
    assertThat(doc.select("#purl3-standard-LicenseOne")).isEmpty();
  }

  @Test
  public void testWithHeaderAndFooter() {
    Application application = tempEntity.newApplicationWithParent("appId");
    generateReportDataAndMocks(application, true);

    String content = reportBuilder.generateCustomLegalApplicationAttributionReport(application, BuildStageType.ID,
        LegalCustomReportParameters.builder()
            .withIncludeAppendix(false)
            .withTitle("My Report")
            .withHeader("My Header Content")
            .withFooter("My Footer Content")
            .build());

    Document doc = Jsoup.parse(content);

    assertThat(doc.select("#header")).isNotEmpty();
    assertThat(doc.select("#header")).hasToString("<p id=\"header\">My Header Content</p>");
    assertThat(doc.select("#additional-notices")).isEmpty();

    assertThat(doc.select("#footer")).isNotEmpty();
    assertThat(doc.select("#footer")).hasToString("<p id=\"footer\">My Footer Content</p>");
  }

  @Test
  public void testWithNoticeFiles() {
    Application application = tempEntity.newApplicationWithParent("appId");
    generateReportDataAndMocks(application, true);

    String content = reportBuilder.generateCustomLegalApplicationAttributionReport(application, BuildStageType.ID,
        LegalCustomReportParameters.builder()
            .withIncludeAppendix(false)
            .withTitle("My Report")
            .withNoticeFiles(Lists.newArrayList(
                "First Notice File Content",
                "Second Notice File Content"))
            .build());

    Document doc = Jsoup.parse(content);

    assertThat(doc.select("#additional-notices")).isNotEmpty();
    assertThat(doc.select("#additional-notices").first().toString())
        .contains("First Notice File Content");
    assertThat(doc.select("#additional-notices").first().toString())
        .contains("Second Notice File Content");
  }

  private void generateReportDataAndMocks(final Application application, boolean addStandardLicenseTextToMetadata) {
    generateSingleReportDataAndMocks(application, addStandardLicenseTextToMetadata);
  }

  private ApiLicenseLegalApplicationReportDTO generateMockReportData(boolean addStandardLicenseTextToMetadata) {

    ApiLicenseLegalApplicationReportDTO reportDTO = new ApiLicenseLegalApplicationReportDTO();

    // First Component
    ApiComponentDTOV2 component1 = new ApiComponentDTOV2();
    component1.displayName = "component 1";
    component1.packageUrl = "purl1";

    List<ApiLicenseLegalCopyrightDTO> copyrights1 = new ArrayList<>();
    copyrights1
        .add(new ApiLicenseLegalCopyrightDTO("id1", "Copyright 2020", "hash1", ComponentLegalPartStatus.ENABLED));
    copyrights1.add(new ApiLicenseLegalCopyrightDTO("id2", "DISABLED", "hash2", ComponentLegalPartStatus.DISABLED));

    List<ApiLicenseLegalFileDTO> licenseFiles = new ArrayList<>();
    licenseFiles
        .add(new ApiLicenseLegalFileDTO("id1", "path1", "LICENSE CONTENT", "hash3", ComponentLegalPartStatus.ENABLED));
    licenseFiles
        .add(new ApiLicenseLegalFileDTO("id2", "path2", "DISABLED", "hash4", ComponentLegalPartStatus.DISABLED));

    List<ComponentObligationAttributionDTO> attributions = new ArrayList<>();
    attributions.add(
        new ComponentObligationAttributionDTO("id", "owner", "Must State Changes", "myAttributionContent"));
    attributions.add(
        new ComponentObligationAttributionDTO("id", "owner", "Inclusion of Install Instructions", "attribution 2"));
    attributions.add(new ComponentObligationAttributionDTO("id", "owner", "Must Give Credit", "attribution 3"));
    attributions.add(new ComponentObligationAttributionDTO("id", "owner", null, "attribution 4"));

    ApiLicenseLegalDataDTO licenseLegalData1 = new ApiLicenseLegalDataDTO();
    licenseLegalData1.copyrights = copyrights1;
    licenseLegalData1.licenseFiles = licenseFiles;
    licenseLegalData1.effectiveLicenses = new ArrayList<>();
    licenseLegalData1.effectiveLicenses.add("LicenseOne");
    licenseLegalData1.effectiveLicenses.add("LicenseTwo");
    licenseLegalData1.attributions = attributions;

    reportDTO.components = new ArrayList<>();
    reportDTO.components.add(new ApiLicenseLegalComponentDTO(component1, licenseLegalData1, null));

    // Second Component
    ApiComponentDTOV2 component2 = new ApiComponentDTOV2();
    component2.displayName = "component 2";
    component2.packageUrl = "purl2";

    List<ApiLicenseLegalCopyrightDTO> copyrights2 = new ArrayList<>();
    copyrights1
        .add(new ApiLicenseLegalCopyrightDTO("id3", "Copyright 2021", "hash5", ComponentLegalPartStatus.ENABLED));

    List<ApiLicenseLegalFileDTO> noticeFiles = new ArrayList<>();
    noticeFiles.add(
        new ApiLicenseLegalFileDTO("id1", "noticePath1", "NOTICE CONTENT", "hash6", ComponentLegalPartStatus.ENABLED));
    noticeFiles.add(new ApiLicenseLegalFileDTO("id2", "path2", "DISABLED", "hash7", ComponentLegalPartStatus.DISABLED));

    ApiLicenseLegalDataDTO licenseLegalData2 = new ApiLicenseLegalDataDTO();
    licenseLegalData2.copyrights = copyrights2;
    licenseLegalData2.noticeFiles = noticeFiles;
    licenseLegalData2.effectiveLicenses = new ArrayList<>();
    licenseLegalData2.effectiveLicenses.add("License Three");

    //// Only 1 source link less than 56 characters
    LegalSourceLinkDTO linkDTOShortLink = new LegalSourceLinkDTO();
    linkDTOShortLink.id = "1";
    linkDTOShortLink.content = "http://localhost";
    linkDTOShortLink.status = ComponentLegalPartStatus.ENABLED;
    HashSet<LegalSourceLinkDTO> sourceLinkDTOS = new HashSet<>();
    sourceLinkDTOS.add(linkDTOShortLink);
    licenseLegalData2.sourceLinks = sourceLinkDTOS;

    // Third Component - only contains standard license text
    ApiComponentDTOV2 component3 = new ApiComponentDTOV2();
    component3.displayName = "component 3";
    component3.packageUrl = "purl3";
    ApiLicenseLegalDataDTO licenseLegalData3 = new ApiLicenseLegalDataDTO();
    licenseLegalData3.effectiveLicenses = Lists.newArrayList("LicenseOne");

    //// Only 1 source link more than 56 characters
    LegalSourceLinkDTO linkDTOLongLink = new LegalSourceLinkDTO();
    linkDTOLongLink.id = "1";
    linkDTOLongLink.content = "http://test/more/than/56/characters/test/test/test/test/test/test/test/test";
    linkDTOLongLink.status = ComponentLegalPartStatus.ENABLED;
    HashSet<LegalSourceLinkDTO> sourceLinkDTOSLongLink = new HashSet<>();
    sourceLinkDTOSLongLink.add(linkDTOLongLink);
    licenseLegalData3.sourceLinks = sourceLinkDTOSLongLink;

    // Fourth Component - only contains standard license text
    ApiComponentDTOV2 component4 = new ApiComponentDTOV2();
    component4.displayName = "component 4";
    component4.packageUrl = "purl4";
    ApiLicenseLegalDataDTO licenseLegalData4 = new ApiLicenseLegalDataDTO();
    licenseLegalData4.effectiveLicenses = Lists.newArrayList("LicenseOne");

    //// More than 1 source link, first link shorter than  56 characters
    LegalSourceLinkDTO linkDTOMultiShortLink1 = new LegalSourceLinkDTO();
    linkDTOMultiShortLink1.id = "1";
    linkDTOMultiShortLink1.content = "http://abc";
    linkDTOMultiShortLink1.status = ComponentLegalPartStatus.ENABLED;

    LegalSourceLinkDTO linkDTOMultiShortLink2 = new LegalSourceLinkDTO();
    linkDTOMultiShortLink2.id = "2";
    linkDTOMultiShortLink2.content = "http://abcd";
    linkDTOMultiShortLink2.status = ComponentLegalPartStatus.ENABLED;

    Set<LegalSourceLinkDTO> sourceLinkDTOSMultiShortLink = new TreeSet<>(LEGAL_SOURCE_LINK_COMPARATOR);
    sourceLinkDTOSMultiShortLink.add(linkDTOMultiShortLink1);
    sourceLinkDTOSMultiShortLink.add(linkDTOMultiShortLink2);
    licenseLegalData4.sourceLinks = sourceLinkDTOSMultiShortLink;

    // Fifth Component - only contains standard license text
    ApiComponentDTOV2 component5 = new ApiComponentDTOV2();
    component5.displayName = "component 5";
    component5.packageUrl = "purl5";
    ApiLicenseLegalDataDTO licenseLegalData5 = new ApiLicenseLegalDataDTO();
    licenseLegalData5.effectiveLicenses = Lists.newArrayList("LicenseOne");
    //// More than 1 source link, first link longer than  56 characters
    LegalSourceLinkDTO linkDTOMultiLongLink1 = new LegalSourceLinkDTO();
    linkDTOMultiLongLink1.id = "1";
    linkDTOMultiLongLink1.content = "http://test/more/than/56/characters/test/test/test/test/test/test/test/test/1";
    linkDTOMultiLongLink1.status = ComponentLegalPartStatus.DISABLED;

    LegalSourceLinkDTO linkDTOMultiLongLink2 = new LegalSourceLinkDTO();
    linkDTOMultiLongLink2.id = "2";
    linkDTOMultiLongLink2.content = "http://test/more/than/56/characters/test/test/test/test/test/test/test/test/2";
    linkDTOMultiLongLink2.status = ComponentLegalPartStatus.ENABLED;

    HashSet<LegalSourceLinkDTO> sourceLinkDTOSMultiLongLink = new HashSet<>();
    sourceLinkDTOSMultiLongLink.add(linkDTOMultiLongLink1);
    sourceLinkDTOSMultiLongLink.add(linkDTOMultiLongLink2);
    licenseLegalData5.sourceLinks = sourceLinkDTOSMultiLongLink;

    reportDTO.components = new ArrayList<>();
    reportDTO.components.add(new ApiLicenseLegalComponentDTO(component1, licenseLegalData1, null));
    reportDTO.components.add(new ApiLicenseLegalComponentDTO(component2, licenseLegalData2, null));
    reportDTO.components.add(new ApiLicenseLegalComponentDTO(component3, licenseLegalData3, null));
    reportDTO.components.add(new ApiLicenseLegalComponentDTO(component4, licenseLegalData4, null));
    reportDTO.components.add(new ApiLicenseLegalComponentDTO(component5, licenseLegalData5, null));

    reportDTO.licenseLegalMetadata = new HashSet<>();
    ApiLicenseLegalMetadataDTO licenseLegalMetadataDTO =
        new ApiLicenseLegalMetadataDTO("LicenseOne", "LicenseOneName",
            addStandardLicenseTextToMetadata ? "License One Standard License Text" : null,
            new HashSet<>(), null);
    reportDTO.licenseLegalMetadata.add(licenseLegalMetadataDTO);
    return reportDTO;
  }

  private void generateSingleReportDataAndMocks(
      final Application application,
      boolean addStandardLicenseTextToMetadata)
  {
    ApiLicenseLegalApplicationReportDTO reportDTO = generateMockReportData(addStandardLicenseTextToMetadata);
    when(mockApiLicenseLegalService.getLicenseLegalApplicationReport(application, BuildStageType.ID,
        false, false)).thenReturn(reportDTO);
  }

  private void generateMultiReportDataAndMocks(
      final List<Owner> applications,
      boolean addStandardLicenseTextToMetadata)
  {
    Set<Optional<ApiLicenseLegalApplicationReportDTO>> reportDTOs =
        applications.stream()
            .map(app -> Optional.of(generateMockReportData(addStandardLicenseTextToMetadata)))
            .collect(Collectors.toSet());
    doReturn(reportDTOs).when(mockApiLicenseLegalService)
        .getLicenseLegalMultiApplicationReport(
            argThat(matcher -> matcher.containsAll(applications)),
            argThat(matcher -> matcher.containsAll(Collections.nCopies(applications.size(), BuildStageType.ID))),
            booleanThat(matcher -> !matcher),
            booleanThat(matcher -> !matcher));
  }

  @Test
  public void testEmptyReport() {
    Application application = tempEntity.newApplicationWithParent("appId");
    ApiLicenseLegalApplicationReportDTO reportDTO = new ApiLicenseLegalApplicationReportDTO();

    when(mockApiLicenseLegalService.getLicenseLegalApplicationReport(application, BuildStageType.ID,
        false, false)).thenReturn(reportDTO);

    assertThat(reportBuilder.generateCustomLegalApplicationAttributionReport(application, BuildStageType.ID,
        LegalCustomReportParameters.builder().buildWithDefaults(application.getPublicId())))
            .isNotNull();
  }

  @Test
  public void testHtmlEscaping() {
    Application application = tempEntity.newApplicationWithParent("appId");
    generateReportDataAndMocks(application, true);

    String content = reportBuilder.generateCustomLegalApplicationAttributionReport(application, BuildStageType.ID,
        LegalCustomReportParameters.builder()
            .withIncludeAppendix(false)
            .withTitle("User's Legal template in français é î ü & company <script>alert('test');</script>")
            .withHeader("!@#$%^&*())_")
            .withFooter("<h2>title</h2>")
            .build());

    Document doc = Jsoup.parse(content);
    assertThat(doc.select("h1").first()).hasToString(
        "<h1>User's Legal template in français é î ü &amp; company &lt;script&gt;alert('test');&lt;/script&gt;</h1>");
    assertThat(doc.select("#header")).isNotEmpty();
    assertThat(doc.select("#header")).hasToString("<p id=\"header\">!@#$%^&amp;*())_</p>");
    assertThat(doc.select("#footer")).isNotEmpty();
    assertThat(doc.select("#footer")).hasToString("<p id=\"footer\">&lt;h2&gt;title&lt;/h2&gt;</p>");
  }

  @Test
  public void testReportWithInnerSourceComponent() {
    Application application = tempEntity.newApplicationWithParent("appId");

    ApiComponentDTOV2 component = new ApiComponentDTOV2();
    component.displayName = "InnerSource component";
    component.packageUrl =
        PackageUrlIdentifier.fromComponentIdentifier(ComponentIdentifier.createNpmCoordinates("p", "v")).toString();

    ApiLicenseLegalDataDTO licenseLegalData = new ApiLicenseLegalDataDTO();
    licenseLegalData.effectiveLicenses = Collections.emptyList();

    tempEntity.newInnerSourceApplication(component.packageUrl, application);

    ApiLicenseLegalApplicationReportDTO reportDTO = new ApiLicenseLegalApplicationReportDTO();
    reportDTO.components =
        Collections.singletonList(new ApiLicenseLegalComponentDTO(component, licenseLegalData, null));

    when(mockApiLicenseLegalService.getLicenseLegalApplicationReport(application, BuildStageType.ID,
        true, false)).thenReturn(reportDTO);

    String content = reportBuilder.generateCustomLegalApplicationAttributionReport(application, BuildStageType.ID,
        LegalCustomReportParameters
            .builder()
            .withTitle("test")
            .withIncludeInnerSource(true)
            .build());

    Document doc = Jsoup.parse(content);
    assertThat(doc.select(".componentBox h2").first().toString()).contains(component.displayName);
    assertThat(doc.select("ul li").first()).hasToString("<li>No licenses detected.</li>");
  }

  @Test
  public void testReportIncludingSonatypeSpecialLicenses() {
    Application application = tempEntity.newApplicationWithParent("appId");

    ApiComponentDTOV2 component1 = new ApiComponentDTOV2();
    component1.displayName = "Sonatype Special licences component";
    component1.packageUrl = PackageUrlIdentifier.fromComponentIdentifier(ComponentIdentifier
        .createNpmCoordinates("p", "v")).toString();

    ApiComponentDTOV2 component2 = new ApiComponentDTOV2();
    component2.displayName = "MIT component";
    component2.packageUrl = PackageUrlIdentifier.fromComponentIdentifier(ComponentIdentifier
        .createNpmCoordinates("p1", "v1")).toString();

    ApiLicenseLegalDataDTO licenseLegalData1 = new ApiLicenseLegalDataDTO();
    licenseLegalData1.effectiveLicenses = Collections.singletonList(UNSPECIFIED_ID);

    ApiLicenseLegalDataDTO licenseLegalData2 = new ApiLicenseLegalDataDTO();
    licenseLegalData2.effectiveLicenses = Collections.singletonList("MIT");

    ApiLicenseLegalApplicationReportDTO reportDTO = new ApiLicenseLegalApplicationReportDTO();
    reportDTO.components = Arrays.asList(new ApiLicenseLegalComponentDTO(component1, licenseLegalData1, null),
        new ApiLicenseLegalComponentDTO(component2, licenseLegalData2, null));

    when(mockApiLicenseLegalService.getLicenseLegalApplicationReport(application, BuildStageType.ID, true,
        true)).thenReturn(reportDTO);

    String content = reportBuilder.generateCustomLegalApplicationAttributionReport(application, BuildStageType.ID,
        LegalCustomReportParameters
            .builder()
            .withTitle("test")
            .withIncludeInnerSource(true)
            .withIncludeIncludeSonatypeSpecialLicenses(true)
            .build());

    Document doc = Jsoup.parse(content);
    assertThat(doc.select(".componentBox h2").first().toString()).contains(component1.displayName);
    assertThat(doc.select("ul li").first()).hasToString("<li>UNSPECIFIED</li>");
    assertThat(reportDTO.components).hasSize(2);
  }

  private UserFilterDTO newUserFilterDTO(UserFilter userFilter) throws IOException {
    ObjectMapper mapper = new ObjectMapper();
    JsonNode node = mapper.valueToTree(JsonUtils.parse(userFilter.getFilter(), Map.class));
    UserFilterDTO userFilterDTO = new UserFilterDTO(userFilter.getName(), null, userFilter.getType(), node);
    return userFilterDTO;
  }
}
