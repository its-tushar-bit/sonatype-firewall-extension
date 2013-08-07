package com.sonatype.insight.brain.model;

//The full GAV is required as the report could conceivably contain multiple versions
public class GAVPopularity
{
  private String artifactId;

  private long[] catalogDates;

  private int currentVersionIndex;

  private String groupId;

  private int[] popularity;

  private String version;

  public String getArtifactId() {
    return artifactId;
  }

  public long[] getCatalogDates() {
    return catalogDates;
  }

  public int getCurrentVersionIndex() {
    return currentVersionIndex;
  }

  public String getGroupId() {
    return groupId;
  }

  public int[] getPopularity() {
    return popularity;
  }

  public String getVersion() {
    return version;
  }

  public void setArtifactId(String artifactId) {
    this.artifactId = artifactId;
  }

  public void setCatalogDates(long[] catalogDates) {
    this.catalogDates = catalogDates;
  }

  public void setCurrentVersionIndex(int currentVersionIndex) {
    this.currentVersionIndex = currentVersionIndex;
  }

  public void setGroupId(String groupId) {
    this.groupId = groupId;
  }

  public void setPopularity(int[] popularity) {
    this.popularity = popularity;
  }

  public void setVersion(String version) {
    this.version = version;
  }
}
