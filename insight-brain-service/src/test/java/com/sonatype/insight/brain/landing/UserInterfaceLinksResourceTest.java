/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.landing;

import java.io.IOException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.mail.MessagingException;
import javax.mail.util.ByteArrayDataSource;

import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.hds.HdsClientAnalytics;
import com.sonatype.insight.brain.service.AbstractResourceTest;
import com.sonatype.insight.brain.service.BaseUrl;
import com.sonatype.insight.brain.telemetry.TelemetrySender;
import com.sonatype.insight.mock.hds.HttpResponseProcessor;
import com.sonatype.insight.telemetry.model.TelemetryData;
import com.sonatype.insight.telemetry.model.TelemetryPurpose;

import org.apache.shiro.subject.Subject;
import org.apache.shiro.util.ThreadContext;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import static java.util.stream.Collectors.groupingBy;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

public class UserInterfaceLinksResourceTest
    extends AbstractResourceTest
{
  private void assertRedirect(HttpResponse response, String expected) throws Exception {
    assertResponseStatus(307, response);
    assertThat(response.getHeader("Location")).isEqualTo(getRestBaseUrl() + expected);
  }

  private HttpResponse get(String path, Object... params) throws Exception {
    return restRequest().path(UserInterfaceLinksResource.RESOURCE_PATH, path).parameter(params).anon().get();
  }

  @Test
  public void testLinkToManagement_App() throws Exception {
    HttpResponse response = get(UserInterfaceLinksResource.MANAGEMENT_PATH, "application", "test id");
    assertRedirect(response, "assets/index.html#/management/view/application/test%20id");
  }

  @Test
  public void testLinkToManagement_Org() throws Exception {
    HttpResponse response = get(UserInterfaceLinksResource.MANAGEMENT_PATH, "organization", "test id");
    assertRedirect(response, "assets/index.html#/management/view/organization/test%20id");
  }

  @Test
  public void testLinkToReport() throws Exception {
    assertThat(UserInterfaceLinksResource.getReportUrl("app id", "scan id"))
        .isEqualTo(UserInterfaceLinksResource.RESOURCE_PATH + "/application/app%20id/report/scan%20id");
    HttpResponse response = get(UserInterfaceLinksResource.REPORT_PATH, "app id", "scan id");
    assertRedirect(response, "assets/index.html#/applicationReport/app%20id/scan%20id/policy");
  }

  @Test
  @ManualServerInit
  public void testLinkToReport_WithSourceQuery() throws Exception {
    final Map<ByteArrayDataSource, Integer> responses = Collections.synchronizedMap(new LinkedHashMap<>());
    initServer(config -> {
      getHdsServer().respondWith((HttpResponseProcessor) (request, response) -> {
        responses.put(new ByteArrayDataSource(request.getInputStream(), "multipart/form-data"), response.getStatus());
      }).andStatus(204).atUri(TelemetrySender.RESOURCE_PATH);
    });

    assertThat(UserInterfaceLinksResource.getReportUrl("app id", "scan id"))
        .isEqualTo(UserInterfaceLinksResource.RESOURCE_PATH + "/application/app%20id/report/scan%20id");
    HttpResponse redirect = restRequest()
        .path(UserInterfaceLinksResource.RESOURCE_PATH, UserInterfaceLinksResource.REPORT_PATH)
        .parameter("app id", "scan id").query("source=Foo").anon().get();
    assertRedirect(redirect, "assets/index.html?source=Foo#/applicationReport/app%20id/scan%20id/policy");

    Map<TelemetryPurpose, List<TelemetryItem>> telemetryItemsByPurpose = getTelemetryItemsByPurpose(responses);

    List<TelemetryItem> telemetryItems = telemetryItemsByPurpose.get(TelemetryPurpose.SOURCE_CONTROL_REPORT_LINK);
    assertThat(telemetryItems.size()).isEqualTo(1);
    TelemetryData telemetryData = telemetryItems.get(0).getTelemetryData();
    assertThat(telemetryData.getPurpose()).isEqualTo(TelemetryPurpose.SOURCE_CONTROL_REPORT_LINK);
    assertThat(telemetryData.getAttributes().get("source")).isEqualTo("Foo".toLowerCase(Locale.ENGLISH));
    assertThat(telemetryData.getAttributes().get("application_id")).isEqualTo(HdsClientAnalytics.obfuscate("app id"));
    assertThat(telemetryData.getAttributes().get("scan_id")).isEqualTo(HdsClientAnalytics.obfuscate("scan id"));
    assertThat(telemetryData.getAttributes().get("is_logged_in")).isEqualTo(false);
  }

  @Test
  public void testSendSourceTelemetryData_WhenUserIsLoggedIn() {
    Subject subject = mock(Subject.class);
    doReturn("principal").when(subject).getPrincipal();
    ThreadContext.bind(subject);

    TelemetrySender telemetrySender = mock(TelemetrySender.class);
    ArgumentCaptor<TelemetryData> telemetryDataArgumentCaptor = ArgumentCaptor.forClass(TelemetryData.class);
    doNothing().when(telemetrySender).send(telemetryDataArgumentCaptor.capture());
    ApplicationDAO applicationDao = mock(ApplicationDAO.class);
    new UserInterfaceLinksResource(mock(BaseUrl.class), telemetrySender, applicationDao)
        .sendSourceTelemetryData("appId", "scanId", "source");

    TelemetryData telemetryData = telemetryDataArgumentCaptor.getValue();
    assertThat(telemetryData.getPurpose()).isEqualTo(TelemetryPurpose.SOURCE_CONTROL_REPORT_LINK);
    assertThat(telemetryData.getAttributes().get("application_id")).isEqualTo(HdsClientAnalytics.obfuscate("appId"));
    assertThat(telemetryData.getAttributes().get("is_logged_in")).isEqualTo(true);
  }

  @Test
  @ManualServerInit
  public void testLinkToReport_WithoutSourceQuery() throws Exception {
    final Map<ByteArrayDataSource, Integer> responses = Collections.synchronizedMap(new LinkedHashMap<>());
    initServer(config -> {
      getHdsServer().respondWith((HttpResponseProcessor) (request, response) -> {
        responses.put(new ByteArrayDataSource(request.getInputStream(), "multipart/form-data"), response.getStatus());
      }).andStatus(204).atUri(TelemetrySender.RESOURCE_PATH);
    });

    assertThat(UserInterfaceLinksResource.getReportUrl("app id", "scan id"))
        .isEqualTo(UserInterfaceLinksResource.RESOURCE_PATH + "/application/app%20id/report/scan%20id");
    HttpResponse response = get(UserInterfaceLinksResource.REPORT_PATH, "app id", "scan id");
    assertRedirect(response, "assets/index.html#/applicationReport/app%20id/scan%20id/policy");

    Map<TelemetryPurpose, List<TelemetryItem>> telemetryDataByPurpose = getTelemetryItemsByPurpose(responses);
    assertThat(telemetryDataByPurpose.get(TelemetryPurpose.SOURCE_CONTROL_REPORT_LINK)).isNull();
  }

  @Test
  public void testLinkToEmbeddableReport() throws Exception {
    assertThat(UserInterfaceLinksResource.getEmbeddableReportUrl("app id", "scan id"))
        .isEqualTo(UserInterfaceLinksResource.RESOURCE_PATH + "/application/app%20id/report/scan%20id/embeddable");
    HttpResponse response = get(UserInterfaceLinksResource.EMBEDDABLE_REPORT_PATH, "app id", "scan id");
    assertRedirect(response, "assets/index.html#/applicationReport/app%20id/scan%20id/policy?embeddable");
  }

  @Test
  public void testLinkToPdf() throws Exception {
    assertThat(UserInterfaceLinksResource.getPdfUrl("app id", "scan id"))
        .isEqualTo(UserInterfaceLinksResource.RESOURCE_PATH + "/application/app%20id/report/scan%20id/pdf");
    HttpResponse response = get(UserInterfaceLinksResource.PDF_PATH, "app id", "scan id");
    assertRedirect(response, "rest/report/app%20id/scan%20id/printReport");
  }

  @Test
  public void testLinkToRepositoryReport() throws Exception {
    String url = UserInterfaceLinksResource.getRepositoryReportUrl("repo id");
    assertThat(url).isEqualTo(UserInterfaceLinksResource.RESOURCE_PATH + "/repository/repo%20id/result");
    HttpResponse response = get(UserInterfaceLinksResource.REPO_RESULT_PATH, "repo id");
    assertRedirect(response, "assets/index.html#/repository/repo%20id/result");
  }

  private Map<TelemetryPurpose, List<TelemetryItem>> getTelemetryItemsByPurpose(
      final Map<ByteArrayDataSource, Integer> responses)
      throws MessagingException, IOException
  {
    return getTelemetryItems(responses).stream().collect(groupingBy(TelemetryItem::getTelemetryPurpose));
  }
}
