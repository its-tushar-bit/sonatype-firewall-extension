/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.report.pdf;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.sonatype.insight.brain.api.v2.dto.ApiLicenseDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiLicenseThreatDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiReportComponentDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiReportComponentPolicyViolationsDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiReportPolicyDataDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiReportPolicyViolationDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiReportRawDataDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiSecurityIssueDTO;
import com.sonatype.insight.brain.report.pdf.PdfData.PdfComponent.PdfComponentLicense;
import com.sonatype.insight.brain.report.pdf.PdfData.PdfComponent.PdfComponentLicenseThreat;
import com.sonatype.insight.brain.report.pdf.PdfData.PdfComponent.PdfComponentPolicyViolation;
import com.sonatype.insight.brain.report.pdf.PdfData.PdfComponent.PdfComponentSecurityIssue;
import com.sonatype.insight.brain.sbom.components.BomPageMetadataDTO;

public class PdfData
{
  public String baseUrl;

  public String title;

  public Date createdDate;

  public Date analyzedDate;

  public String commitHash;

  public String productVersion;

  public List<PdfComponent> components;

  public BomPageMetadataDTO sbomMetadata;

  public static class PdfComponent
  {
    public String displayName;

    public String matchState;

    public List<PdfComponentPolicyViolation> policyViolations;

    public List<PdfComponentSecurityIssue> securityIssues;

    public List<PdfComponentLicense> effectiveLicenses;

    public List<PdfComponentLicense> declaredLicenses;

    public List<PdfComponentLicense> observedLicenses;

    public List<PdfComponentLicense> overriddenLicenses;

    public List<PdfComponentLicenseThreat> effectiveLicenseThreats;

    public static class PdfComponentPolicyViolation
    {
      public int policyThreatLevel;

      public String policyName;

      public String policyThreatCategory;

      public boolean legacyViolation;

      public boolean waived;
    }

    public static class PdfComponentSecurityIssue
    {
      public String reference;

      public Float severity;

      public String analysisState;
    }

    public static class PdfComponentLicense
    {
      public String name;
    }

    public static class PdfComponentLicenseThreat
    {
      public int licenseThreatGroupLevel;
    }
  }

  public static PdfData createPdfData(
      String baseUrl,
      String productVersion,
      ApiReportPolicyDataDTOV2 policyData,
      ApiReportRawDataDTOV2 rawData)
  {
    PdfData pdfData = new PdfData();
    pdfData.baseUrl = baseUrl;
    pdfData.title = getTitle(policyData);
    pdfData.createdDate = new Date();
    pdfData.analyzedDate = policyData.reportTime;
    pdfData.commitHash = policyData.commitHash;
    pdfData.productVersion = productVersion;

    Map<String, ApiReportComponentPolicyViolationsDTOV2> componentPolicyViolationsByHash =
        policyData.components.stream().collect(Collectors.toMap(c -> c.hash, c -> c));
    Map<String, ApiReportComponentDTOV2> componentRawByHash =
        rawData.components.stream().collect(Collectors.toMap(c -> c.hash, c -> c));

    Set<String> componentHashes = new LinkedHashSet<>();
    componentHashes.addAll(componentPolicyViolationsByHash.keySet());
    componentHashes.addAll(componentRawByHash.keySet());

    pdfData.components = new ArrayList<>();
    for (String hash : componentHashes) {
      PdfComponent component = new PdfComponent();
      component.policyViolations = new ArrayList<>();
      component.securityIssues = new ArrayList<>();
      component.effectiveLicenses = new ArrayList<>();
      component.declaredLicenses = new ArrayList<>();
      component.observedLicenses = new ArrayList<>();
      component.overriddenLicenses = new ArrayList<>();
      component.effectiveLicenseThreats = new ArrayList<>();

      ApiReportComponentPolicyViolationsDTOV2 componentPolicyViolations = componentPolicyViolationsByHash.get(hash);
      if (componentPolicyViolations != null) {
        component.displayName = componentPolicyViolations.displayName;
        component.matchState = componentPolicyViolations.matchState;

        for (ApiReportPolicyViolationDTOV2 policyViolation : componentPolicyViolations.violations) {
          PdfComponentPolicyViolation componentPolicyViolation = new PdfComponentPolicyViolation();
          componentPolicyViolation.policyThreatLevel = policyViolation.policyThreatLevel;
          componentPolicyViolation.policyName = policyViolation.policyName;
          componentPolicyViolation.policyThreatCategory = policyViolation.policyThreatCategory;
          componentPolicyViolation.waived = policyViolation.waived;
          componentPolicyViolation.legacyViolation = policyViolation.legacyViolation;
          component.policyViolations.add(componentPolicyViolation);
        }
      }

      ApiReportComponentDTOV2 componentRaw = componentRawByHash.get(hash);
      if (componentRaw != null) {
        component.displayName = componentRaw.displayName;
        component.matchState = componentRaw.matchState;

        if (componentRaw.securityData != null) {
          for (ApiSecurityIssueDTO securityIssue : componentRaw.securityData.securityIssues) {
            PdfComponentSecurityIssue componentSecurityIssue = new PdfComponentSecurityIssue();
            componentSecurityIssue.reference = securityIssue.reference;
            componentSecurityIssue.severity = securityIssue.severity;
            component.securityIssues.add(componentSecurityIssue);
          }
        }

        if (componentRaw.licenseData != null) {
          for (ApiLicenseDTO license : componentRaw.licenseData.effectiveLicenses) {
            PdfComponentLicense componentLicense = new PdfComponentLicense();
            componentLicense.name = license.licenseName;
            component.effectiveLicenses.add(componentLicense);
          }

          for (ApiLicenseDTO license : componentRaw.licenseData.declaredLicenses) {
            PdfComponentLicense componentLicense = new PdfComponentLicense();
            componentLicense.name = license.licenseName;
            component.declaredLicenses.add(componentLicense);
          }

          for (ApiLicenseDTO license : componentRaw.licenseData.observedLicenses) {
            PdfComponentLicense componentLicense = new PdfComponentLicense();
            componentLicense.name = license.licenseName;
            component.observedLicenses.add(componentLicense);
          }

          for (ApiLicenseDTO license : componentRaw.licenseData.overriddenLicenses) {
            PdfComponentLicense componentLicense = new PdfComponentLicense();
            componentLicense.name = license.licenseName;
            component.overriddenLicenses.add(componentLicense);
          }

          for (ApiLicenseThreatDTOV2 licenseThreat : componentRaw.licenseData.effectiveLicenseThreats) {
            PdfComponentLicenseThreat componentLicenseThreat = new PdfComponentLicenseThreat();
            componentLicenseThreat.licenseThreatGroupLevel = licenseThreat.licenseThreatGroupLevel;
            component.effectiveLicenseThreats.add(componentLicenseThreat);
          }
        }
      }

      pdfData.components.add(component);
    }
    return pdfData;
  }

  public static PdfData createSbomPdfData(
      String baseUrl,
      String productVersion,
      ApiReportPolicyDataDTOV2 policyData,
      ApiReportRawDataDTOV2 rawData,
      BomPageMetadataDTO bomPageMetadata)
  {
    PdfData pdfData = createPdfData(baseUrl, productVersion, policyData, rawData);
    // Make modifications to the pdfData here and in PdfGeneration to control the pdf
    pdfData.sbomMetadata = bomPageMetadata;
    return pdfData;
  }

  private static String getTitle(ApiReportPolicyDataDTOV2 policyData) {
    List<String> parts = new ArrayList<>();
    if (policyData.application != null && policyData.application.name != null) {
      parts.add(policyData.application.name);
    }
    if (policyData.reportTitle != null) {
      parts.add(policyData.reportTitle);
    }
    return String.join(" ", parts);
  }
}
