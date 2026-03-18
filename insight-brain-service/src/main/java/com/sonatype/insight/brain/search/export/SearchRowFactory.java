/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search.export;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.sonatype.insight.brain.search.index.ItemType;
import com.sonatype.insight.brain.search.results.SearchResultItemDTO;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.sonatype.insight.brain.landing.UserInterfaceLinksHelper.getItemManagementPathEdit;
import static com.sonatype.insight.brain.landing.UserInterfaceLinksHelper.getManagementPath;
import static com.sonatype.insight.brain.landing.UserInterfaceLinksHelper.getReportUrl;
import static com.sonatype.insight.brain.landing.UserInterfaceLinksHelper.getVulnerabilityDetailsUrl;
import static com.sonatype.insight.brain.search.export.SearchPaths.APPLICATION_CATEGORY_PATH_VARIABLE;
import static com.sonatype.insight.brain.search.export.SearchPaths.APPLICATION_PATH_VARIABLE;
import static com.sonatype.insight.brain.search.export.SearchPaths.LABEL_PATH_VARIABLE;
import static com.sonatype.insight.brain.search.export.SearchPaths.ORGANIZATION_PATH_VARIABLE;
import static com.sonatype.insight.brain.search.export.SearchPaths.POLICY_PATH_VARIABLE;
import static com.sonatype.insight.brain.search.export.SearchRowFactory.Header.*;

public abstract class SearchRowFactory
{
  public enum Header
  {
    ITEM_TYPE("Item Type"),
    ORGANIZATION("Organization"),
    ORGANIZATION_LINK("Organization Link"),
    APPLICATION("Application"),
    APPLICATION_LINK("Application Link"),
    APPLICATION_CATEGORY("Application Category"),
    APPLICATION_CATEGORY_LINK("Application Category Link"),
    APPLICATION_VERSION("Application Version"),
    COMPONENT_LABEL("Component Label"),
    COMPONENT_LABEL_LINK("Component Label Link"),
    POLICY("Policy"),
    THREAT("Threat"),
    POLICY_LINK("Policy Link"),
    COMPONENT_NAME("Component Name"),
    REPORT("Report"),
    SECURITY_ISSUE("Security Issue"),
    SECURITY_ISSUE_ID("Security Issue ID"),
    STAGE("Stage"),
    SBOM_SPECIFICATION("SBOM Specification");

    private final String header;

    Header(String header) {
      this.header = header;
    }

    public String getHeader() {
      return header;
    }
  }

  private static final Logger log = LoggerFactory.getLogger(SearchRowFactory.class);

  private final Map<Header, Integer> exportSearchHeadersMap;

  public SearchRowFactory(List<Header> exportSearchHeaders) {
    this.exportSearchHeadersMap = createHeadersMap(exportSearchHeaders);
  }

  public String[] getHeaders() {
    return exportSearchHeadersMap.keySet().stream().map(Header::getHeader).toArray(String[]::new);
  }

  public List<String> create(SearchResultItemDTO searchResultItemDTO, String baseUrl) {
    List<String> row = new ArrayList<>(Collections.nCopies(exportSearchHeadersMap.size(), ""));

    addColumn(row, ITEM_TYPE, searchResultItemDTO, baseUrl);

    switch (ItemType.valueOf(searchResultItemDTO.itemType)) {
      case ORGANIZATION:
        addColumns(row, searchResultItemDTO, baseUrl, ORGANIZATION, ORGANIZATION_LINK);
        break;
      case APPLICATION:
        addColumns(row, searchResultItemDTO, baseUrl, ORGANIZATION, ORGANIZATION_LINK, APPLICATION, APPLICATION_LINK);
        break;
      case APPLICATION_CATEGORY:
        addColumns(row, searchResultItemDTO, baseUrl,
            ORGANIZATION, ORGANIZATION_LINK, APPLICATION_CATEGORY, APPLICATION_CATEGORY_LINK);
        break;
      case COMPONENT_LABEL:
        if (searchResultItemDTO.organizationId != null) {
          addColumns(row, searchResultItemDTO, baseUrl, ORGANIZATION, ORGANIZATION_LINK);
        }
        else {
          addColumns(row, searchResultItemDTO, baseUrl, APPLICATION, APPLICATION_LINK);
        }
        addColumns(row, searchResultItemDTO, baseUrl, COMPONENT_LABEL, COMPONENT_LABEL_LINK);
        break;
      case POLICY:
        if (searchResultItemDTO.organizationId != null) {
          addColumns(row, searchResultItemDTO, baseUrl, ORGANIZATION, ORGANIZATION_LINK);
        }
        else {
          addColumns(row, searchResultItemDTO, baseUrl, APPLICATION, APPLICATION_LINK);
        }
        addColumns(row, searchResultItemDTO, baseUrl, POLICY, THREAT, POLICY_LINK);
        break;
      case SECURITY_VULNERABILITY:
        if (searchResultItemDTO.organizationName != null) {
          addColumns(row, searchResultItemDTO, baseUrl, ORGANIZATION, ORGANIZATION_LINK);
        }
        addColumns(row, searchResultItemDTO, baseUrl,
            APPLICATION, APPLICATION_LINK, COMPONENT_NAME, REPORT, SECURITY_ISSUE, SECURITY_ISSUE_ID, STAGE,
            APPLICATION_VERSION, SBOM_SPECIFICATION);
        break;
      case NON_VULNERABLE_COMPONENT:
        if (searchResultItemDTO.organizationName != null) {
          addColumns(row, searchResultItemDTO, baseUrl, ORGANIZATION, ORGANIZATION_LINK);
        }
        addColumns(row, searchResultItemDTO, baseUrl,
            APPLICATION, APPLICATION_LINK, COMPONENT_NAME, REPORT, STAGE, APPLICATION_VERSION, SBOM_SPECIFICATION);
        break;
      case SBOM_METADATA:
        if (searchResultItemDTO.organizationName != null) {
          addColumns(row, searchResultItemDTO, baseUrl, ORGANIZATION, ORGANIZATION_LINK);
        }
        addColumns(row, searchResultItemDTO, baseUrl,
            APPLICATION, APPLICATION_LINK, APPLICATION_VERSION, SBOM_SPECIFICATION);
        break;
      default:
        log.error("Unexpected row in advanced search export, item type: {}", searchResultItemDTO.itemType);
        Collections.fill(row, "");
        break;
    }

    return row;
  }

  private void addColumns(
      List<String> row,
      SearchResultItemDTO searchResultItemDTO,
      String baseUrl,
      Header... headers)
  {
    for (Header header : headers) {
      addColumn(row, header, searchResultItemDTO, baseUrl);
    }
  }

  protected void addColumn(
      List<String> row,
      Header header,
      SearchResultItemDTO searchResultItemDTO,
      String baseUrl)
  {
    int colIdx = exportSearchHeadersMap.getOrDefault(header, -1);
    if (colIdx < 0) {
      // Supported columns depend on advanced search mode Lifecycle vs SBOM Manager
      return;
    }

    String columnValue = valueMapper(header, searchResultItemDTO, baseUrl);
    row.set(colIdx, columnValue);
  }

  private String valueMapper(Header header, SearchResultItemDTO searchResultItemDTO, String baseUrl) {
    String value;
    switch (header) {
      case ITEM_TYPE:
        value = ItemType.valueOf(searchResultItemDTO.itemType).name();
        break;
      case ORGANIZATION:
        value = searchResultItemDTO.organizationName;
        break;
      case ORGANIZATION_LINK:
        value = baseUrl +
            getManagementPath(ORGANIZATION_PATH_VARIABLE, searchResultItemDTO.organizationId, isSbomManager());
        break;
      case APPLICATION:
        value = searchResultItemDTO.applicationName;
        break;
      case APPLICATION_LINK:
        value = baseUrl +
            getManagementPath(APPLICATION_PATH_VARIABLE, searchResultItemDTO.applicationPublicId, isSbomManager());
        break;
      case APPLICATION_CATEGORY:
        value = searchResultItemDTO.applicationCategoryName;
        break;
      case APPLICATION_CATEGORY_LINK:
        value = baseUrl + getItemManagementPathEdit(ORGANIZATION_PATH_VARIABLE, searchResultItemDTO.organizationId,
            APPLICATION_CATEGORY_PATH_VARIABLE, searchResultItemDTO.applicationCategoryId);
        break;
      case APPLICATION_VERSION:
        value = searchResultItemDTO.applicationVersion;
        break;
      case COMPONENT_NAME:
        value = searchResultItemDTO.componentName;
        break;
      case COMPONENT_LABEL:
        value = searchResultItemDTO.componentLabelName;
        break;
      case COMPONENT_LABEL_LINK:
        value = getComponentLabelLink(searchResultItemDTO, baseUrl);
        break;
      case POLICY:
        value = searchResultItemDTO.policyName;
        break;
      case POLICY_LINK:
        value = getPolicyLink(searchResultItemDTO, baseUrl);
        break;
      case THREAT:
        value = String.valueOf(searchResultItemDTO.policyThreatLevel);
        break;
      case REPORT:
        value = baseUrl + getReportUrl(searchResultItemDTO.applicationPublicId, searchResultItemDTO.reportId);
        break;
      case SECURITY_ISSUE:
        value = baseUrl + getVulnerabilityDetailsUrl(searchResultItemDTO.vulnerabilityId);
        break;
      case SECURITY_ISSUE_ID:
        value = searchResultItemDTO.vulnerabilityId;
        break;
      case STAGE:
        value = searchResultItemDTO.policyEvaluationStage;
        break;
      case SBOM_SPECIFICATION:
        value = searchResultItemDTO.sbomSpecification;
        break;
      default:
        log.error("Unexpected header in advanced search export row: {}", header);
        value = "";
    }

    return value;
  }

  private String getPolicyLink(SearchResultItemDTO searchResultItemDTO, String baseUrl) {
    if (searchResultItemDTO.organizationId != null) {
      return baseUrl + getItemManagementPathEdit(ORGANIZATION_PATH_VARIABLE, searchResultItemDTO.organizationId,
          POLICY_PATH_VARIABLE, searchResultItemDTO.policyId);
    }
    return baseUrl + getItemManagementPathEdit(APPLICATION_PATH_VARIABLE, searchResultItemDTO.applicationPublicId,
        POLICY_PATH_VARIABLE, searchResultItemDTO.policyId);
  }

  private String getComponentLabelLink(SearchResultItemDTO searchResultItemDTO, String baseUrl) {
    if (Objects.isNull(searchResultItemDTO.organizationId)) {
      return baseUrl + getItemManagementPathEdit(APPLICATION_PATH_VARIABLE, searchResultItemDTO.applicationPublicId,
          LABEL_PATH_VARIABLE, searchResultItemDTO.componentLabelId);
    }
    return baseUrl + getItemManagementPathEdit(ORGANIZATION_PATH_VARIABLE, searchResultItemDTO.organizationId,
        LABEL_PATH_VARIABLE, searchResultItemDTO.componentLabelId);
  }

  private Map<Header, Integer> createHeadersMap(List<Header> headers) {
    Map<Header, Integer> exportSearchHeadersMap = new LinkedHashMap<>();

    for (int i = 0; i < headers.size(); i++) {
      exportSearchHeadersMap.put(headers.get(i), i);
    }

    return exportSearchHeadersMap;
  }

  protected boolean isSbomManager() {
    return false;
  }
}
