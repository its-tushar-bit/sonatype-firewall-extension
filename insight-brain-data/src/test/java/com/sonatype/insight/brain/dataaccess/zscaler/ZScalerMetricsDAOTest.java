/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.zscaler;

import java.util.Date;

import com.sonatype.insight.brain.dataaccess.AbstractDbDAOTest;
import com.sonatype.insight.brain.model.zscaler.ZScalerMetrics;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ZScalerMetricsDAOTest
    extends AbstractDbDAOTest
{
  private ZScalerMetricsDAO zScalerMetricsDAO;

  @BeforeEach
  @Override
  public void setup() {
    super.setup();
    zScalerMetricsDAO = daoFactory.createZScalerMetricsDAO();
  }

  @AfterEach
  public void tearDown() {
    // Clean up the database after each test
    ZScalerMetrics zScalerMetrics = zScalerMetricsDAO.get();
    if (zScalerMetrics != null) {
      zScalerMetricsDAO.delete(zScalerMetrics);
    }
  }

  @Test
  public void testCrud() {
    // Create a new ZScalerMetrics object
    ZScalerMetrics metrics = new ZScalerMetrics();
    metrics.setMavenUrlsFromHds(1);
    metrics.setMavenUrlsToZscaler(2);
    metrics.setNpmUrlsFromHds(3);
    metrics.setNpmUrlsToZscaler(4);
    metrics.setPypiUrlsFromHds(5);
    metrics.setPypiUrlsToZscaler(6);
    metrics.setNugetUrlsFromHds(7);
    metrics.setNugetUrlsToZscaler(8);

    // Insert the object into the database
    zScalerMetricsDAO.insert(metrics);

    // Retrieve the object from the database
    ZScalerMetrics retrievedMetrics = zScalerMetricsDAO.get();

    assertThat(retrievedMetrics).isNotNull();
    assertThat(retrievedMetrics.getId()).isEqualTo("zscaler-metrics");
    assertThat(retrievedMetrics.getUpdatedAt()).isNotNull();
    assertThat(retrievedMetrics.getMavenUrlsFromHds()).isEqualTo(1);
    assertThat(retrievedMetrics.getMavenUrlsToZscaler()).isEqualTo(2);
    assertThat(retrievedMetrics.getNpmUrlsFromHds()).isEqualTo(3);
    assertThat(retrievedMetrics.getNpmUrlsToZscaler()).isEqualTo(4);
    assertThat(retrievedMetrics.getPypiUrlsFromHds()).isEqualTo(5);
    assertThat(retrievedMetrics.getPypiUrlsToZscaler()).isEqualTo(6);
    assertThat(retrievedMetrics.getNugetUrlsFromHds()).isEqualTo(7);
    assertThat(retrievedMetrics.getNugetUrlsToZscaler()).isEqualTo(8);

    Date updatedAt = metrics.getUpdatedAt();

    metrics.setMavenUrlsFromHds(10);
    metrics.setMavenUrlsToZscaler(11);
    metrics.setNpmUrlsFromHds(12);
    metrics.setNpmUrlsToZscaler(13);
    metrics.setPypiUrlsFromHds(14);
    metrics.setPypiUrlsToZscaler(15);
    metrics.setNugetUrlsFromHds(16);
    metrics.setNugetUrlsToZscaler(17);

    // Update the object in the database
    zScalerMetricsDAO.update(metrics);

    // Retrieve the updated object from the database
    ZScalerMetrics updatedMetrics = zScalerMetricsDAO.get();
    assertThat(updatedMetrics).isNotNull();
    assertThat(updatedMetrics.getId()).isEqualTo("zscaler-metrics");
    assertThat(updatedMetrics.getUpdatedAt()).isNotNull();
    assertThat(updatedMetrics.getMavenUrlsFromHds()).isEqualTo(10);
    assertThat(updatedMetrics.getMavenUrlsToZscaler()).isEqualTo(11);
    assertThat(updatedMetrics.getNpmUrlsFromHds()).isEqualTo(12);
    assertThat(updatedMetrics.getNpmUrlsToZscaler()).isEqualTo(13);
    assertThat(updatedMetrics.getPypiUrlsFromHds()).isEqualTo(14);
    assertThat(updatedMetrics.getPypiUrlsToZscaler()).isEqualTo(15);
    assertThat(updatedMetrics.getNugetUrlsFromHds()).isEqualTo(16);
    assertThat(updatedMetrics.getNugetUrlsToZscaler()).isEqualTo(17);

    // Verify that the updatedAt timestamp has changed
    assertThat(updatedMetrics.getUpdatedAt()).isAfter(updatedAt);
  }
}
