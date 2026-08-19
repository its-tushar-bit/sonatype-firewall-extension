/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.configuration;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import com.sonatype.insight.brain.dataaccess.configuration.AnnouncementBannerDAO;
import com.sonatype.insight.brain.model.configuration.AnnouncementBanner;
import com.sonatype.insight.error.exception.BadRequestException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class AnnouncementBannerServiceTest
{
  @Mock
  private AnnouncementBannerDAO dao;

  @InjectMocks
  private AnnouncementBannerService service;

  @Test
  public void testGetBanner_returnsDaoValueWhenPresent() {
    AnnouncementBanner row = enabledBanner();
    when(dao.get()).thenReturn(row);

    assertThat(service.getBanner()).isSameAs(row);
  }

  @Test
  public void testGetBanner_returnsDisabledDefaultWhenDaoReturnsNull() {
    when(dao.get()).thenReturn(null);

    AnnouncementBanner result = service.getBanner();

    assertThat(result).isNotNull();
    assertThat(result.isEnabled()).isFalse();
    assertThat(result.getSeverity()).isEqualTo("info");
    assertThat(result.getId()).isEqualTo(AnnouncementBannerDAO.SINGLETON_ENTITY_ID);
  }

  @Test
  public void testUpdateBanner_disabledBannerNeedsNoRequiredFields_returnsInputObject() {
    AnnouncementBanner disabled = new AnnouncementBanner();
    disabled.setEnabled(false);

    AnnouncementBanner result = service.updateBanner(disabled);

    assertThat(result).isSameAs(disabled);
    verify(dao).update(disabled);
  }

  @Test
  public void testUpdateBanner_defaultsNullSeverityToInfo() {
    AnnouncementBanner banner = enabledBanner();
    banner.setSeverity(null);

    AnnouncementBanner result = service.updateBanner(banner);

    ArgumentCaptor<AnnouncementBanner> captor = ArgumentCaptor.forClass(AnnouncementBanner.class);
    verify(dao).update(captor.capture());
    assertThat(captor.getValue().getSeverity()).isEqualTo("info");
    assertThat(result.getSeverity()).isEqualTo("info");
  }

  @Test
  public void testUpdateBanner_acceptsCriticalSeverity() {
    AnnouncementBanner banner = enabledBanner();
    banner.setSeverity("critical");

    service.updateBanner(banner);

    ArgumentCaptor<AnnouncementBanner> captor = ArgumentCaptor.forClass(AnnouncementBanner.class);
    verify(dao).update(captor.capture());
    assertThat(captor.getValue().getSeverity()).isEqualTo("critical");
  }

  @Test
  public void testUpdateBanner_rejectsUnknownSeverity() {
    AnnouncementBanner banner = enabledBanner();
    banner.setSeverity("ugly-green");

    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> service.updateBanner(banner))
        .withMessageContaining("severity");
  }

  @Test
  public void testUpdateBanner_rejectsEnabledWithoutMessage() {
    AnnouncementBanner banner = enabledBanner();
    banner.setMessage(null);

    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> service.updateBanner(banner))
        .withMessageContaining("message");
  }

  @Test
  public void testUpdateBanner_generatesWindowIdWhenBlank() {
    AnnouncementBanner banner = enabledBanner();
    banner.setWindowId(null);

    AnnouncementBanner persisted = service.updateBanner(banner);

    ArgumentCaptor<AnnouncementBanner> captor = ArgumentCaptor.forClass(AnnouncementBanner.class);
    verify(dao).update(captor.capture());
    assertThat(captor.getValue().getWindowId()).isNotBlank();
    assertThatCode(() -> UUID.fromString(captor.getValue().getWindowId())).doesNotThrowAnyException();
    assertThat(persisted.getWindowId()).isEqualTo(captor.getValue().getWindowId());
  }

  @Test
  public void testUpdateBanner_generatesWindowIdWhenEmpty() {
    AnnouncementBanner banner = enabledBanner();
    banner.setWindowId("");

    AnnouncementBanner persisted = service.updateBanner(banner);

    ArgumentCaptor<AnnouncementBanner> captor = ArgumentCaptor.forClass(AnnouncementBanner.class);
    verify(dao).update(captor.capture());
    assertThat(captor.getValue().getWindowId()).isNotBlank();
    assertThatCode(() -> UUID.fromString(captor.getValue().getWindowId())).doesNotThrowAnyException();
    assertThat(persisted.getWindowId()).isEqualTo(captor.getValue().getWindowId());
  }

  @Test
  public void testUpdateBanner_rejectsDisplayFromAfterDisplayUntil() {
    AnnouncementBanner banner = enabledBanner();
    banner.setDisplayFrom(OffsetDateTime.of(2026, 6, 1, 0, 0, 0, 0, ZoneOffset.UTC));
    banner.setDisplayUntil(OffsetDateTime.of(2026, 5, 1, 0, 0, 0, 0, ZoneOffset.UTC));

    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> service.updateBanner(banner))
        .withMessageContaining("displayFrom");
  }

  @Test
  public void testUpdateBanner_rejectsMissingDisplayFromWhenEnabled() {
    AnnouncementBanner banner = enabledBanner();
    banner.setDisplayFrom(null);

    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> service.updateBanner(banner))
        .withMessageContaining("displayFrom")
        .withMessageContaining("displayUntil");
  }

  @Test
  public void testUpdateBanner_rejectsMissingDisplayUntilWhenEnabled() {
    AnnouncementBanner banner = enabledBanner();
    banner.setDisplayUntil(null);

    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> service.updateBanner(banner))
        .withMessageContaining("displayFrom")
        .withMessageContaining("displayUntil");
  }

  @Test
  public void testUpdateBanner_rejectsNullInputWithBadRequest() {
    // Service-level precondition: if the resource layer ever hands down a null body, fail fast as 400,
    // not as an NPE deep in the validator.
    assertThatExceptionOfType(BadRequestException.class).isThrownBy(() -> service.updateBanner(null));
  }

  @Test
  public void testUpdateBanner_doesNotStampUpdatedAt_becauseDaoOwnsThatInvariant() {
    // DAO owns the updatedAt stamp; service must leave the caller's value untouched.
    AnnouncementBanner banner = enabledBanner();
    OffsetDateTime caller = OffsetDateTime.of(2020, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);
    banner.setUpdatedAt(caller);

    service.updateBanner(banner);

    ArgumentCaptor<AnnouncementBanner> captor = ArgumentCaptor.forClass(AnnouncementBanner.class);
    verify(dao).update(captor.capture());
    assertThat(captor.getValue().getUpdatedAt()).isEqualTo(caller);
  }

  private AnnouncementBanner enabledBanner() {
    AnnouncementBanner banner = new AnnouncementBanner();
    banner.setEnabled(true);
    banner.setWindowId("2026-05-26-us");
    banner.setSeverity("info");
    banner.setMessage("Scheduled maintenance: May 26, 6-10 PM EDT.");
    banner.setDisplayFrom(OffsetDateTime.of(2026, 5, 20, 0, 0, 0, 0, ZoneOffset.UTC));
    banner.setDisplayUntil(OffsetDateTime.of(2026, 5, 26, 23, 0, 0, 0, ZoneOffset.UTC));
    return banner;
  }
}
