/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service.legal.report;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;

import javax.inject.Inject;

import com.sonatype.insight.brain.api.v2.dto.ApiComponentDTOV2;
import com.sonatype.insight.brain.api.v2.dto.legal.ApiLicenseLegalApplicationReportDTO;
import com.sonatype.insight.brain.api.v2.dto.legal.ApiLicenseLegalComponentDTO;
import com.sonatype.insight.brain.api.v2.dto.legal.ApiLicenseLegalCopyrightDTO;
import com.sonatype.insight.brain.api.v2.dto.legal.ApiLicenseLegalDataDTO;
import com.sonatype.insight.brain.api.v2.dto.legal.ApiLicenseLegalFileDTO;
import com.sonatype.insight.brain.api.v2.dto.legal.ApiLicenseLegalMetadataDTO;
import com.sonatype.insight.brain.api.v2.dto.legal.ComponentObligationAttributionDTO;
import com.sonatype.insight.brain.api.v2.service.legal.ApiLicenseLegalService;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.legal.ComponentLegalPartStatus;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;
import com.sonatype.insight.brain.service.AbstractComponentTest;

import com.google.inject.Binder;
import org.apache.commons.io.IOUtils;
import org.assertj.core.util.Lists;
import org.junit.Test;
import org.mockito.Mock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

public class ApplicationAttributionReportBuilderTest
    extends AbstractComponentTest
{
  @Mock
  private ApiLicenseLegalService mockApiLicenseLegalService;

  @Inject
  private ApplicationAttributionReportBuilder reportBuilder;

  @Override
  public void configure(final Binder binder) {
    binder.bind(ApiLicenseLegalService.class).toInstance(mockApiLicenseLegalService);
    super.configure(binder);
  }

  @Test
  public void testSuccessfulReport() throws IOException {
    Application application = tempEntity.newApplicationWithParent("appId");
    ApiLicenseLegalApplicationReportDTO reportDTO = new ApiLicenseLegalApplicationReportDTO();

    //First Component
    ApiComponentDTOV2 component1 = new ApiComponentDTOV2();
    component1.displayName = "component 1";

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
    attributions
        .add(new ComponentObligationAttributionDTO("id", "owner", "Must State Changes", "myAttributionContent"));

    ApiLicenseLegalDataDTO licenseLegalData1 = new ApiLicenseLegalDataDTO();
    licenseLegalData1.copyrights = copyrights1;
    licenseLegalData1.licenseFiles = licenseFiles;
    licenseLegalData1.effectiveLicenses = new ArrayList<>();
    licenseLegalData1.effectiveLicenses.add("LicenseOneName");
    licenseLegalData1.effectiveLicenses.add("LicenseTwo");
    licenseLegalData1.attributions = attributions;

    reportDTO.components = new ArrayList<>();
    reportDTO.components.add(new ApiLicenseLegalComponentDTO(component1, licenseLegalData1, null));

    //Second Component
    ApiComponentDTOV2 component2 = new ApiComponentDTOV2();
    component2.displayName = "component 2";

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

    //Third Component - only contains standard license text
    ApiComponentDTOV2 component3 = new ApiComponentDTOV2();
    component3.displayName = "component 3";

    ApiLicenseLegalDataDTO licenseLegalData3 = new ApiLicenseLegalDataDTO();
    licenseLegalData3.effectiveLicenses = Lists.newArrayList("LicenseOneName");

    reportDTO.components = new ArrayList<>();
    reportDTO.components.add(new ApiLicenseLegalComponentDTO(component1, licenseLegalData1, null));
    reportDTO.components.add(new ApiLicenseLegalComponentDTO(component2, licenseLegalData2, null));
    reportDTO.components.add(new ApiLicenseLegalComponentDTO(component3, licenseLegalData3, null));

    when(mockApiLicenseLegalService.getLicenseLegalApplicationReport(application, BuildStageType.ID))
        .thenReturn(reportDTO);

    reportDTO.licenseLegalMetadata = new HashSet<>();
    ApiLicenseLegalMetadataDTO licenseLegalMetadataDTO =
        new ApiLicenseLegalMetadataDTO("LicenseOne", "LicenseOneName", "License One Standard License Text",
            new HashSet<>());
    reportDTO.licenseLegalMetadata.add(licenseLegalMetadataDTO);

    String content = reportBuilder.generateLegalApplicationAttributionReport(application, BuildStageType.ID);
    String expectedContent = IOUtils.toString(Objects.requireNonNull(getClass().getClassLoader()
            .getResource("ApplicationAttributionReportTest/expectedApplicationAttributionReport.html")),
        StandardCharsets.UTF_8);

    assertThat(content).isEqualToIgnoringWhitespace(expectedContent);
  }

  @Test
  public void testEmptyReport() {
    Application application = tempEntity.newApplicationWithParent("appId");
    ApiLicenseLegalApplicationReportDTO reportDTO = new ApiLicenseLegalApplicationReportDTO();

    when(mockApiLicenseLegalService.getLicenseLegalApplicationReport(application, BuildStageType.ID))
        .thenReturn(reportDTO);

    assertThat(reportBuilder.generateLegalApplicationAttributionReport(application, BuildStageType.ID))
        .isNotNull();
  }
}
