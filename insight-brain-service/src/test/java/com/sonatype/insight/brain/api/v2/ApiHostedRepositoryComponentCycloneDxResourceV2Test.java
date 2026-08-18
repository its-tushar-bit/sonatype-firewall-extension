/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import java.util.List;

import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import com.sonatype.insight.brain.api.v2.service.ApiCycloneDxServiceV2;
import com.sonatype.insight.brain.dataaccess.repository.HostedRepositoryComponentDAO;
import com.sonatype.insight.brain.model.repository.HostedRepositoryComponent;
import com.sonatype.insight.brain.security.SecurityAspectControl;
import com.sonatype.insight.scan.file.ThirdPartyUtils;

import org.cyclonedx.Version;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link ApiHostedRepositoryComponentCycloneDxResourceV2}. Each handler is the
 * HRC-scoped sibling of {@link ApiCycloneDxResourceV2}; it resolves the HRC through the DAO and
 * delegates to the same {@link ApiCycloneDxServiceV2} method as the App path with the requested
 * CycloneDX version and media type.
 */
@ExtendWith(MockitoExtension.class)
public class ApiHostedRepositoryComponentCycloneDxResourceV2Test
{
  private static final String HRC_ID = "hrc-1";

  private static final String STAGE_ID = "build";

  private static final String REPORT_ID = "report-1";

  @Mock
  private ApiCycloneDxServiceV2 apiCycloneDxService;

  @Mock
  private HostedRepositoryComponentDAO hostedRepositoryComponentDAO;

  @InjectMocks
  private ApiHostedRepositoryComponentCycloneDxResourceV2 resource;

  private HostedRepositoryComponent hrc;

  @BeforeEach
  public void setUp() {
    // AspectJ compile-time weaving inserts a @HasFeature aspect on the resource class and an
    // @Authorize aspect on its service-call sites. Both fire during Mockito unit tests that
    // bypass the Spring proxy. Disabling enforcement short-circuits both to the mocked service
    // call — see SecurityAspectControl's javadoc for the intended use. This also covers
    // @HasFeature(HOSTED_REPOSITORY_EVALUATION), so the feature must not be toggled here:
    // SystemConfigurationPropertyFeature.setEnabled reaches for a statically injected
    // SystemConfigurationPropertyDAO that a plain MockitoJUnitRunner never wires.
    SecurityAspectControl.disableEnforcement();
    hrc = new HostedRepositoryComponent("repo-1", "path/lib.jar", "hash-abc");
    hrc.setId(HRC_ID);
    when(hostedRepositoryComponentDAO.getByIdNotNull(HRC_ID)).thenReturn(hrc);
  }

  @AfterEach
  public void tearDown() {
    SecurityAspectControl.enableEnforcement();
  }

  // ---- getLatest (default XML/1.1) ----

  @Test
  public void getLatest_delegatesWithXmlAndVersion11() {
    Response expected = mock(Response.class);
    when(apiCycloneDxService.getLatest(hrc, STAGE_ID, MediaType.APPLICATION_XML, Version.VERSION_11))
        .thenReturn(expected);

    Response actual = resource.getLatest(HRC_ID, STAGE_ID);

    assertThat(actual).isSameAs(expected);
    verify(hostedRepositoryComponentDAO).getByIdNotNull(HRC_ID);
    verify(apiCycloneDxService).getLatest(hrc, STAGE_ID, MediaType.APPLICATION_XML, Version.VERSION_11);
  }

  // ---- getLatestWithVersion ----

  @Test
  public void getLatestWithVersion_xmlHeader_delegatesWithXml() {
    HttpHeaders headers = mock(HttpHeaders.class);
    when(headers.getAcceptableMediaTypes()).thenReturn(List.of(MediaType.APPLICATION_XML_TYPE));
    Version version = ThirdPartyUtils.getCycloneDxSchemaVersion("1.3");
    Response expected = mock(Response.class);
    when(apiCycloneDxService.getLatest(hrc, STAGE_ID, MediaType.APPLICATION_XML, version)).thenReturn(expected);

    Response actual = resource.getLatestWithVersion(HRC_ID, STAGE_ID, "1.3", headers);

    assertThat(actual).isSameAs(expected);
    verify(apiCycloneDxService).getLatest(hrc, STAGE_ID, MediaType.APPLICATION_XML, version);
  }

  @Test
  public void getLatestWithVersion_jsonHeader_delegatesWithJson() {
    HttpHeaders headers = mock(HttpHeaders.class);
    when(headers.getAcceptableMediaTypes()).thenReturn(List.of(MediaType.APPLICATION_JSON_TYPE));
    Version version = ThirdPartyUtils.getCycloneDxSchemaVersion("1.4");
    Response expected = mock(Response.class);
    when(apiCycloneDxService.getLatest(hrc, STAGE_ID, MediaType.APPLICATION_JSON, version)).thenReturn(expected);

    Response actual = resource.getLatestWithVersion(HRC_ID, STAGE_ID, "1.4", headers);

    assertThat(actual).isSameAs(expected);
    verify(apiCycloneDxService).getLatest(hrc, STAGE_ID, MediaType.APPLICATION_JSON, version);
  }

  // ---- getByReportId (default XML/1.1) ----

  @Test
  public void getByReportId_delegatesWithXmlAndVersion11() {
    Response expected = mock(Response.class);
    when(apiCycloneDxService.getByScanId(hrc, REPORT_ID, MediaType.APPLICATION_XML, Version.VERSION_11))
        .thenReturn(expected);

    Response actual = resource.getByReportId(HRC_ID, REPORT_ID);

    assertThat(actual).isSameAs(expected);
    verify(hostedRepositoryComponentDAO).getByIdNotNull(HRC_ID);
    verify(apiCycloneDxService).getByScanId(hrc, REPORT_ID, MediaType.APPLICATION_XML, Version.VERSION_11);
  }

  // ---- getByReportIdWithVersion ----

  @Test
  public void getByReportIdWithVersion_xmlHeader_delegatesWithXml() {
    HttpHeaders headers = mock(HttpHeaders.class);
    when(headers.getAcceptableMediaTypes()).thenReturn(List.of(MediaType.APPLICATION_XML_TYPE));
    Version version = ThirdPartyUtils.getCycloneDxSchemaVersion("1.2");
    Response expected = mock(Response.class);
    when(apiCycloneDxService.getByScanId(hrc, REPORT_ID, MediaType.APPLICATION_XML, version)).thenReturn(expected);

    Response actual = resource.getByReportIdWithVersion(HRC_ID, REPORT_ID, "1.2", headers);

    assertThat(actual).isSameAs(expected);
    verify(apiCycloneDxService).getByScanId(hrc, REPORT_ID, MediaType.APPLICATION_XML, version);
  }

  @Test
  public void getByReportIdWithVersion_jsonHeader_delegatesWithJson() {
    HttpHeaders headers = mock(HttpHeaders.class);
    when(headers.getAcceptableMediaTypes()).thenReturn(List.of(MediaType.APPLICATION_JSON_TYPE));
    Version version = ThirdPartyUtils.getCycloneDxSchemaVersion("1.5");
    Response expected = mock(Response.class);
    when(apiCycloneDxService.getByScanId(hrc, REPORT_ID, MediaType.APPLICATION_JSON, version)).thenReturn(expected);

    Response actual = resource.getByReportIdWithVersion(HRC_ID, REPORT_ID, "1.5", headers);

    assertThat(actual).isSameAs(expected);
    verify(apiCycloneDxService).getByScanId(hrc, REPORT_ID, MediaType.APPLICATION_JSON, version);
  }

  @Test
  public void getByReportIdWithVersion_nullHeaders_defaultsToXml() {
    Version version = ThirdPartyUtils.getCycloneDxSchemaVersion("1.6");
    Response expected = mock(Response.class);
    when(apiCycloneDxService.getByScanId(hrc, REPORT_ID, MediaType.APPLICATION_XML, version)).thenReturn(expected);

    Response actual = resource.getByReportIdWithVersion(HRC_ID, REPORT_ID, "1.6", null);

    assertThat(actual).isSameAs(expected);
    verify(apiCycloneDxService).getByScanId(hrc, REPORT_ID, MediaType.APPLICATION_XML, version);
  }
}
