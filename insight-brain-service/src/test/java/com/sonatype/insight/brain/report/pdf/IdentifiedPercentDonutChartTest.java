/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.report.pdf;

import java.awt.*;

import org.junit.jupiter.api.Test;
import org.knowm.xchart.style.PieStyler;

import static org.assertj.core.api.Assertions.assertThat;

public class IdentifiedPercentDonutChartTest
{
  @Test
  public void testIdentifiedPercentDonutChart() {
    IdentifiedPercentDonutChart identifiedPercentDonutChart = new IdentifiedPercentDonutChart(75);
    assertThat(identifiedPercentDonutChart.getWidth()).isEqualTo(IdentifiedPercentDonutChart.DEFAULT_WIDTH);
    assertThat(identifiedPercentDonutChart.getHeight()).isEqualTo(IdentifiedPercentDonutChart.DEFAULT_HEIGHT);
    PieStyler pieStyler = identifiedPercentDonutChart.getStyler();
    assertThat(pieStyler.isChartTitleVisible()).isFalse();
    assertThat(pieStyler.isChartTitleBoxVisible()).isFalse();
    assertThat(pieStyler.isInfoPanelVisible()).isFalse();
    assertThat(pieStyler.isLegendVisible()).isFalse();
    assertThat(pieStyler.isPlotBorderVisible()).isFalse();
    assertThat(pieStyler.isToolTipsEnabled()).isFalse();
    assertThat(pieStyler.isToolTipsAlwaysVisible()).isFalse();
    assertThat(pieStyler.hasAnnotations()).isFalse();
    assertThat(pieStyler.getChartPadding()).isEqualTo(0);
    assertThat(pieStyler.getPlotContentSize()).isEqualTo(1);
    assertThat(pieStyler.getSeriesColors()).isEqualTo(
        new Color[]{IdentifiedPercentDonutChart.IDENTIFIED_COLOR, IdentifiedPercentDonutChart.UNIDENTIFIED_COLOR});
    assertThat(pieStyler.getDonutThickness()).isEqualTo(IdentifiedPercentDonutChart.DEFAULT_THICKNESS);
    assertThat(identifiedPercentDonutChart.getSeriesMap().get("identifiedPercent").getValue()).isEqualTo(75.0);
    assertThat(identifiedPercentDonutChart.getSeriesMap().get("unidentifiedPercent").getValue()).isEqualTo(25.0);
  }
}
