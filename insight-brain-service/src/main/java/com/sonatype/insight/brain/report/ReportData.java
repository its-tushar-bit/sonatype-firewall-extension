/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.report;

import java.util.ArrayList;
import java.util.List;

/**
 * DTO describing the data for an application composition report (at least the bits we expose as public API so far).
 * 
 * @since 1.9.1
 */
public class ReportData
{
  // components in app, in no particular order
  public List<Component> components = new ArrayList<>();

  public static class Component
  {
    public String hash;

    public Coordinates mavenCoordinates;

    public String matchState;
    public boolean proprietary;

    // occurrences of component, in no particular order
    public List<String> pathnames = new ArrayList<>();

    public LicenseData licenseData;

    public SecurityData securityData;
  }

  public static class Coordinates
  {
    public String groupId;
    public String artifactId;
    public String version;
  }

  public static class LicenseData
  {
    // licenses of component, in no particular order
    public List<License> declaredLicenses = new ArrayList<>();
    public List<License> observedLicenses = new ArrayList<>();
    public List<License> overriddenLicenses = new ArrayList<>();
    public String status;
  }

  public static class License
  {
    public String licenseId;
    public String licenseName;
  }

  public static class SecurityData
  {
    // SVs of component, in no particular order
    public List<SecurityIssue> securityIssues = new ArrayList<>();
  }

  public static class SecurityIssue
  {
    public String source;
    public String reference;
    public Float score;
    public String status;
  }
}
