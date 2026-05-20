/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service.consumption.dto;

import java.util.List;

/**
 * DTO for daily consumption history including cumulative totals, daily average, and peak day.
 *
 * @since 1.204
 */
public class ConsumptionDailyHistoryDTO
{
  private List<DailyEntry> dailyHistory;

  private double dailyAverage;

  private PeakDay peakDay;

  public ConsumptionDailyHistoryDTO() {
  }

  public ConsumptionDailyHistoryDTO(List<DailyEntry> dailyHistory, double dailyAverage, PeakDay peakDay) {
    this.dailyHistory = dailyHistory;
    this.dailyAverage = dailyAverage;
    this.peakDay = peakDay;
  }

  public List<DailyEntry> getDailyHistory() {
    return dailyHistory;
  }

  public void setDailyHistory(List<DailyEntry> dailyHistory) {
    this.dailyHistory = dailyHistory;
  }

  public double getDailyAverage() {
    return dailyAverage;
  }

  public void setDailyAverage(double dailyAverage) {
    this.dailyAverage = dailyAverage;
  }

  public PeakDay getPeakDay() {
    return peakDay;
  }

  public void setPeakDay(PeakDay peakDay) {
    this.peakDay = peakDay;
  }

  public static class DailyEntry
  {
    private String date;

    private long components;

    private long componentsCumulative;

    public DailyEntry() {
    }

    public DailyEntry(String date, long components, long componentsCumulative) {
      this.date = date;
      this.components = components;
      this.componentsCumulative = componentsCumulative;
    }

    public String getDate() {
      return date;
    }

    public void setDate(String date) {
      this.date = date;
    }

    public long getComponents() {
      return components;
    }

    public void setComponents(long components) {
      this.components = components;
    }

    public long getComponentsCumulative() {
      return componentsCumulative;
    }

    public void setComponentsCumulative(long componentsCumulative) {
      this.componentsCumulative = componentsCumulative;
    }
  }

  public static class PeakDay
  {
    private long count;

    private String date;

    public PeakDay() {
    }

    public PeakDay(long count, String date) {
      this.count = count;
      this.date = date;
    }

    public long getCount() {
      return count;
    }

    public void setCount(long count) {
      this.count = count;
    }

    public String getDate() {
      return date;
    }

    public void setDate(String date) {
      this.date = date;
    }
  }
}
