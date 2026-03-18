/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.telemetry;

import com.sonatype.insight.brain.dataaccess.tag.ApplicationTagDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.tag.ApplicationTag;
import com.sonatype.insight.brain.model.tag.ApplicationTagData;
import com.sonatype.insight.brain.model.tag.Tag;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.telemetry.model.TelemetryData;
import com.sonatype.insight.telemetry.model.TelemetryPurpose;
import org.junit.Before;
import org.junit.Test;

import jakarta.inject.Inject;

import java.util.List;

import static com.sonatype.insight.brain.telemetry.PaginatedTelemetryCollectorImpl.DATA_LIST;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

public class ApplicationCategoryTelemetryCollectorTest
    extends AbstractComponentTest
{
  @Inject
  private ApplicationCategoryTelemetryCollector applicationCategoryTelemetryCollector;

  @Inject
  private ApplicationTagDAO applicationTagDAO;

  private Organization organization;

  private Application application1;

  private Application application2;

  private Application application3;

  @Before
  public void before() {
    organization = tempEntity.newOrganization();
    application1 = tempEntity.newApplication(organization.getId());
    application2 = tempEntity.newApplication(organization.getId());
    application3 = tempEntity.newApplication(organization.getId());
  }

  @Test
  public void testIsClusterTelemetry() {
    assertThat(applicationCategoryTelemetryCollector.isClusterTelemetry()).isTrue();
  }

  @Test
  public void testCollectData_noResults() {
    assertThatExceptionOfType(UnsupportedOperationException.class)
        .isThrownBy(() -> applicationCategoryTelemetryCollector.collectData());
    assertThatExceptionOfType(UnsupportedOperationException.class)
        .isThrownBy(() -> applicationCategoryTelemetryCollector.collectAllData());
  }

  @Test
  public void testFetchFirstPage_multipleAppsAndTags() {
    // Given: page size of 25_000
    // Multiple records
    // 1 page
    Tag tag = insertTag(organization.getId());
    Tag tag1 = insertTag(organization.getId());
    Tag tag2 = insertTag(organization.getId());
    Tag tag3 = insertTag(organization.getId());

    insertApplicationTag(application1.getId(), tag.getId());
    insertApplicationTag(application1.getId(), tag1.getId());
    insertApplicationTag(application1.getId(), tag2.getId());
    insertApplicationTag(application2.getId(), tag2.getId());
    insertApplicationTag(application2.getId(), tag3.getId());
    insertApplicationTag(application2.getId(), tag.getId());

    // When
    TelemetryData telemetryData = applicationCategoryTelemetryCollector.firstPage();

    // Then
    List<ApplicationTagData> applicationTags =
        (List<ApplicationTagData>) telemetryData
            .getAttributes()
            .get(DATA_LIST);

    assertTelemetryAttributesAndSize(telemetryData, 2);

    assertThat(applicationTags)
        .extracting(ApplicationTagData::getAppId)
        .containsExactlyInAnyOrder(application1.getId(), application2.getId())
        .hasSize(2);

    assertApplicationTags(
        applicationTags,
        application1.getId(),
        List.of(tag.getName(), tag1.getName(), tag2.getName()));
    assertApplicationTags(
        applicationTags,
        application2.getId(),
        List.of(tag2.getName(), tag3.getName(), tag.getName()));
  }

  @Test
  public void testCollectData_firstPageNoData() {
    // --- Page #1 ---
    // Given no data
    // When
    TelemetryData telemetryData = applicationCategoryTelemetryCollector.firstPage();

    // Then
    assertTelemetryIsEmpty(telemetryData);

    // --- Page #2 ---
    // When
    assertThat(applicationCategoryTelemetryCollector.hasMoreData()).isFalse();
    assertThatExceptionOfType(IllegalStateException.class)
        .isThrownBy(() -> applicationCategoryTelemetryCollector.nextPage());
  }

  @Test
  public void testCollectData_partialFirstPage() {
    // Page --- #1 ---
    // Given: A page size of 2
    // 1 record
    // 1 partial page
    ApplicationCategoryTelemetryCollector telemetryCollector = mockPageSize(2);
    Tag tag = insertTag(organization.getId());
    insertApplicationTag(application1.getId(), tag.getId());

    // When
    TelemetryData telemetryData = telemetryCollector.firstPage();

    // Then
    assertTelemetryAttributesAndSize(telemetryData, 1);
    assertThat(applicationCategoryTelemetryCollector.hasMoreData()).isTrue();

    // when : try another page
    telemetryData = telemetryCollector.nextPage();

    // then
    assertThat(applicationCategoryTelemetryCollector.hasMoreData()).isFalse();

    // when trying fetch a page when more data is false then we should get an exception
    assertThatExceptionOfType(IllegalStateException.class)
        .isThrownBy(telemetryCollector::nextPage);
  }

  @Test
  public void testCollectData_1ExactPage() {
    // --- Page #1 ---
    // Given: a page size of 1
    // 1 record
    // 1 exact page
    ApplicationCategoryTelemetryCollector telemetryCollector = mockPageSize(1);
    Tag tag = insertTag(organization.getId());
    insertApplicationTag(application1.getId(), tag.getId());

    // When
    TelemetryData telemetryData = telemetryCollector.firstPage();

    // Then
    assertTelemetryAttributesAndSize(telemetryData, 1);
    assertThat(telemetryCollector.hasMoreData()).isTrue();

    // --- Page #2 ---
    // Given
    // When
    telemetryData = telemetryCollector.nextPage();

    // Then
    assertThat((List) telemetryData.getAttributes().get(DATA_LIST)).isEmpty();
    assertThat(telemetryCollector.hasMoreData()).isFalse();
  }

  @Test
  public void testCollectData_partialNextPage() {
    // Page --- #1 ---
    // Given: a page size of 2.
    // 4 records
    // 2 exact and 1 partial page
    ApplicationCategoryTelemetryCollector telemetryCollector = mockPageSize(2);
    Tag tag = insertTag(organization.getId());
    Tag tag1 = insertTag(organization.getId());
    insertApplicationTag(application1.getId(), tag.getId());
    insertApplicationTag(application2.getId(), tag1.getId());
    insertApplicationTag(application3.getId(), tag1.getId());

    // When
    TelemetryData telemetryData = telemetryCollector.firstPage();

    // Then
    assertTelemetryAttributesAndSize(telemetryData, 2);
    assertThat(telemetryCollector.hasMoreData()).isTrue();

    // --- Page #2 ---
    // When collecting page 2
    telemetryData = telemetryCollector.nextPage();

    // Then
    assertTelemetryAttributesAndSize(telemetryData, 1);
    assertThat(telemetryCollector.hasMoreData()).isTrue();

    // when trying to read another page
    telemetryData = telemetryCollector.nextPage();

    // then
    assertTelemetryAttributesAndSize(telemetryData, 0);
    assertThat(telemetryCollector.hasMoreData()).isFalse();

    // then expect an exception when trying to read another page
    assertThatExceptionOfType(IllegalStateException.class)
        .isThrownBy(telemetryCollector::nextPage);
  }

  @Test
  public void testCollectData_multipleExactPages() {
    // Page --- #1 ---
    // Given: page size of 1
    // 3 records
    // 3 exact pages.
    ApplicationCategoryTelemetryCollector telemetryCollector = mockPageSize(1);
    Tag tag = insertTag(organization.getId());
    Tag tag1 = insertTag(organization.getId());
    Tag tag2 = insertTag(organization.getId());
    insertApplicationTag(application1.getId(), tag.getId());
    insertApplicationTag(application1.getId(), tag1.getId());
    insertApplicationTag(application1.getId(), tag2.getId());

    // When
    TelemetryData telemetryData = telemetryCollector.firstPage();

    // Then
    assertTelemetryAttributesAndSize(telemetryData, 1);
    assertThat(telemetryCollector.hasMoreData()).isTrue();

    // --- Page #2 ---
    // When collecting page 2
    telemetryData = telemetryCollector.nextPage();

    // Then
    assertThat(telemetryCollector.hasMoreData()).isTrue();
    assertTelemetryAttributesAndSize(telemetryData, 1);

    // --- Page #3 ---
    // When collecting page 3
    telemetryData = telemetryCollector.nextPage();

    // Then
    assertTelemetryAttributesAndSize(telemetryData, 1);
    assertThat(telemetryCollector.hasMoreData()).isTrue();

    // --- Page #4 ---
    // Given no data for page 4
    // When
    telemetryData = telemetryCollector.nextPage();

    // Then
    assertTelemetryIsEmpty(telemetryData);
    assertThat(telemetryCollector.hasMoreData()).isFalse();

    // --- Another call to nextPage(...) ---
    // Given no data for page 4
    assertThatExceptionOfType(IllegalStateException.class)
        .isThrownBy(telemetryCollector::nextPage);
  }

  @Test
  public void testCollectData_collectExactPageData() {
    // --- Page #1 ---
    // Given: a page size of 1
    // 1 record
    // 1 exact page
    ApplicationCategoryTelemetryCollector telemetryCollector = mockPageSize(1);
    Tag tag = insertTag(organization.getId());
    insertApplicationTag(application1.getId(), tag.getId());

    // When
    TelemetryData telemetryData = telemetryCollector.firstPage();

    // Then
    assertTelemetryAttributesAndSize(telemetryData, 1);

    // --- Page #2 ---
    // When
    telemetryData = telemetryCollector.nextPage();

    // Then
    assertTelemetryIsEmpty(telemetryData);

    // --- Another call to nextPage(...) ---
    // When
    assertThatExceptionOfType(IllegalStateException.class)
        .isThrownBy(telemetryCollector::nextPage);
  }

  private ApplicationCategoryTelemetryCollector mockPageSize(int pageSize) {
    ApplicationCategoryTelemetryCollector telemetryCollector = spy(applicationCategoryTelemetryCollector);
    when(telemetryCollector.getPageSize()).thenReturn(pageSize);
    return telemetryCollector;
  }

  private void assertTelemetryAttributesAndSize(TelemetryData telemetryData, int size) {
    assertThat(telemetryData.getPurpose()).isEqualTo(TelemetryPurpose.APPLICATION_CATEGORY);
    assertThat(telemetryData.getAttributes()).containsKey(DATA_LIST);
    assertThat((List) telemetryData.getAttributes().get(DATA_LIST))
        .hasSize(size);
  }

  private void assertTelemetryIsEmpty(TelemetryData telemetryData) {
    List<ApplicationTagData> list =
        (List<ApplicationTagData>) telemetryData.getAttributes()
            .get(DATA_LIST);
    assertThat(list).isEmpty();
  }

  private void assertApplicationTags(
      List<ApplicationTagData> applicationTags,
      String appId,
      List<String> expectedTags)
  {
    assertThat(applicationTags)
        .filteredOn(applicationTagData -> applicationTagData.getAppId().equals(appId))
        .flatExtracting(ApplicationTagData::getCategories)
        .containsExactlyInAnyOrderElementsOf(expectedTags);
  }

  private Tag insertTag(String organizationId) {
    return tempEntity.newTag(organizationId);
  }

  private void insertApplicationTag(String applicationId, String tagId) {
    applicationTagDAO.insert(new ApplicationTag(applicationId, tagId));
  }
}
