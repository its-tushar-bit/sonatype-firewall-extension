/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.report.pdf;

import java.awt.*;

import org.knowm.xchart.PieChart;
import org.knowm.xchart.PieSeries.PieSeriesRenderStyle;
import org.knowm.xchart.style.PieStyler;

class IdentifiedPercentDonutChart
    extends PieChart
{
  // Visible for testing
  static final Color IDENTIFIED_COLOR = new Color(0, 107, 191);

  // Visible for testing
  static final Color UNIDENTIFIED_COLOR = new Color(151, 203, 237);

  // Visible for testing
  static final double DEFAULT_THICKNESS = 0.6;

  // Visible for testing
  static final int DEFAULT_WIDTH = 100;

  // Visible for testing
  static final int DEFAULT_HEIGHT = 100;

  IdentifiedPercentDonutChart(double identifiedPercent) {
    super(DEFAULT_WIDTH, DEFAULT_HEIGHT);
    PieStyler pieStyler = getStyler();
    pieStyler.setDefaultSeriesRenderStyle(PieSeriesRenderStyle.Donut)
        .setChartTitleVisible(false)
        .setChartTitleBoxVisible(false)
        .setInfoPanelVisible(false)
        .setLegendVisible(false)
        .setPlotBorderVisible(false)
        .setToolTipsEnabled(false)
        .setToolTipsAlwaysVisible(false)
        .setHasAnnotations(false)
        .setChartPadding(0)
        .setPlotContentSize(1)
        .setSeriesColors(new Color[]{IDENTIFIED_COLOR, UNIDENTIFIED_COLOR});
    pieStyler.setDonutThickness(DEFAULT_THICKNESS);
    addSeries("identifiedPercent", identifiedPercent);
    addSeries("unidentifiedPercent", 100.0d - identifiedPercent);
  }
}
