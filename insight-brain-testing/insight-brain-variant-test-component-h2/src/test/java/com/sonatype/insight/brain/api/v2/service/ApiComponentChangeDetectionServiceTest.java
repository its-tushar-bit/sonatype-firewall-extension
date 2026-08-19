/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import java.time.Instant;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import com.sonatype.insight.brain.dataaccess.ComponentChangeDetectionConfigurationDAO;
import com.sonatype.insight.brain.dataaccess.ComponentChangeDetectionEventDAO;
import com.sonatype.insight.brain.model.ComponentChangeDetectionConfiguration;
import com.sonatype.insight.brain.model.ComponentChangeDetectionEvent;
import com.sonatype.insight.brain.product.license.ProductLicense;
import com.sonatype.insight.brain.variant.AbstractComponentH2Test;
import com.sonatype.insight.brain.service.Configuration;
import com.sonatype.insight.brain.variant.ComponentH2Test;

@ComponentH2Test
public class ApiComponentChangeDetectionServiceTest
    extends AbstractComponentH2Test
{
  @Mock
  private Configuration configuration;

  @Inject
  private ProductLicense productLicense;

  @Inject
  private ComponentChangeDetectionConfigurationDAO configurationDAO;

  @Inject
  private ComponentChangeDetectionEventDAO eventDAO;

  @Inject
  private ApiComponentChangeDetectionService underTest;

  @Test
  public void getConfiguration_returnsCorrectPage() {
    List<ComponentChangeDetectionConfiguration> actual = Arrays.asList(
        new ComponentChangeDetectionConfiguration("1.0", "purl1", null, null, new Date()),
        new ComponentChangeDetectionConfiguration("1.0", "purl2", null, null, nowPlusSeconds(1)),
        new ComponentChangeDetectionConfiguration("1.0", "purl3", null, null, nowPlusSeconds(2)),
        new ComponentChangeDetectionConfiguration("1.0", "purl4", null, null, nowPlusSeconds(3)),
        new ComponentChangeDetectionConfiguration("1.0", "purl5", null, null, nowPlusSeconds(4)),
        new ComponentChangeDetectionConfiguration("1.0", "purl6", null, null, nowPlusSeconds(5)));
    when(configuration.getComponentChangeDetectionMaxComponents()).thenReturn(10);
    underTest.addItemsToConfiguration(actual);

    List<ComponentChangeDetectionConfiguration> result = underTest.getConfiguration(2, 2);

    assertEquals(List.of("purl3", "purl4"),
        result.stream().map(ComponentChangeDetectionConfiguration::getPurl).collect(Collectors.toList()));
  }

  private Date nowPlusSeconds(int seconds) {
    return Date.from(Instant.now().plusSeconds(seconds));
  }

  @Test
  public void updateHashForComponent_updatesHashCorrectly() {
    List<ComponentChangeDetectionConfiguration> actual = Arrays.asList(
        new ComponentChangeDetectionConfiguration("1.0", "purl1", null, null, new Date()),
        new ComponentChangeDetectionConfiguration("1.0", "purl2", null, null, nowPlusSeconds(1)),
        new ComponentChangeDetectionConfiguration("1.0", "purl3", null, null, nowPlusSeconds(2)));
    underTest.addItemsToConfiguration(actual);

    underTest.updateHashForComponent("purl1", "newHash");

    List<ComponentChangeDetectionConfiguration> items = underTest.getConfiguration(1, 3);

    items.forEach(item -> {
      if (item.getPurl().equals("purl1")) {
        assertThat(item.getComparisonHash()).isEqualTo("newHash");
      }
      else {
        assertThat(item.getComparisonHash()).isNull();
      }
    });
  }

  @Test
  public void addEvent_addsEventCorrectly() {
    ComponentChangeDetectionEvent event = new ComponentChangeDetectionEvent("purl1", "data", new Date());

    underTest.addEvent(event);

    assertThat(eventDAO.getAll().size()).isEqualTo(1);
    assertThat(eventDAO.getAll().stream().findFirst().get()).isEqualTo(event);
  }

  @Test
  public void removeExcessEvents() throws Exception {
    when(configuration.getComponentChangeDetectionMaxEvents()).thenReturn(2);
    underTest.addEvent(new ComponentChangeDetectionEvent("purl1", "data", new Date()));
    Thread.sleep(2);

    underTest.addEvent(new ComponentChangeDetectionEvent("purl2", "data", new Date()));
    Thread.sleep(2);

    underTest.addEvent(new ComponentChangeDetectionEvent("purl3", "data", new Date()));

    assertThat(eventDAO.getAll().size()).isEqualTo(3);

    underTest.removeExcessEvents();
    assertThat(eventDAO.getAll().size()).isEqualTo(2);
    assertThat(eventDAO.getAll().stream().map(ComponentChangeDetectionEvent::getPurl).collect(Collectors.toList()))
        .isEqualTo(List.of("purl2", "purl3"));
  }

  @Test
  public void acknowledgeEventsOlderThan_deletesOldEvents() throws Exception {
    Date time = new Date();
    // Allow a small amount of time to pass to ensure the events are added at a unique to that tested against
    Thread.sleep(2);

    underTest.addEvent(new ComponentChangeDetectionEvent("purl1", "data", new Date()));
    Thread.sleep(2);
    Date middleAdded = new Date();
    underTest.addEvent(new ComponentChangeDetectionEvent("purl2", "data", new Date()));
    Thread.sleep(2);

    underTest.acknowledgeEventsOlderThan(time);

    assertThat(eventDAO.getAll().size()).isEqualTo(2);

    underTest.acknowledgeEventsOlderThan(middleAdded);
    assertThat(eventDAO.getAll().size()).isEqualTo(1);

    underTest.acknowledgeEventsOlderThan(new Date());
    assertThat(eventDAO.getAll().size()).isEqualTo(0);
  }
}
