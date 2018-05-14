/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.tools.common;

import java.util.List;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.annotation.JsonRawValue;

public class PerfTestConfig
{
  private List<TestUrl> urls;

  public List<TestUrl> getUrls() {
    return urls;
  }

  public void setUrls(List<TestUrl> val) {
    urls = val;
  }

  public static class RepeatConfig
  {
    private long ifLongerThan = 0L;

    private int minRuns = 1;

    private int maxRuns = 2;

    public long getIfLongerThan() {
      return ifLongerThan;
    }

    public void setIfLongerThan(long val) {
      ifLongerThan = val;
    }

    public int getMinRuns() {
      return minRuns;
    }

    public void setMinRuns(int val) {
      minRuns = val;
    }

    public int getMaxRuns() {
      return maxRuns;
    }

    public void setMaxRuns(int val) {
      maxRuns = val;
    }
  }

  public static class TestUrl
      implements Cloneable
  {
    private String url;

    private String type;

    private String payload;

    private RepeatConfig repeat;

    public String getUrl() {
      return url;
    }

    public void setUrl(String val) {
      url = val;
    }

    public String getType() {
      return type;
    }

    public void setType(String val) {
      type = val;
    }

    @JsonRawValue
    public String getPayload() {
      return payload;
    }

    @JsonDeserialize(using = RawJsonDeserializer.class)
    public void setPayload(String val) {
      payload = val;
    }

    public RepeatConfig getRepeat() {
      return repeat;
    }

    public void setRepeat(RepeatConfig val) {
      repeat = val;
    }

    @Override
    public Object clone() {
      try {
        return super.clone();
      }
      catch (CloneNotSupportedException e) {
        throw new RuntimeException(e);
      }
    }
  }
}
