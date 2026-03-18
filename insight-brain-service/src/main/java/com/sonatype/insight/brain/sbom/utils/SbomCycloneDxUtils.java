/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.sbom.utils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import com.sonatype.insight.SbomTaxonomy;
import com.sonatype.insight.brain.dataaccess.MigrationTrackerDAO;
import com.sonatype.insight.brain.migration.DisplayNameForFileCoordinateAsyncDbMigration;
import com.sonatype.insight.brain.model.HashHelper;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartySbomMetadata;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyScan;
import com.sonatype.insight.brain.sbom.components.BomPageMetadataDTO;
import com.sonatype.insight.brain.sbom.utils.SbomCreationDetails.Creator;
import com.sonatype.insight.brain.sbom.utils.SbomCreationDetails.CreatorType;
import com.sonatype.insight.json.store.JsonUtils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.cyclonedx.Version;
import org.cyclonedx.exception.ParseException;
import org.cyclonedx.model.Bom;
import org.cyclonedx.model.Component;
import org.cyclonedx.model.Metadata;
import org.cyclonedx.model.OrganizationalContact;
import org.cyclonedx.model.OrganizationalEntity;
import org.cyclonedx.model.Property;
import org.cyclonedx.model.Service;
import org.cyclonedx.model.Tool;
import org.cyclonedx.model.vulnerability.Vulnerability.Rating.Method;
import org.cyclonedx.parsers.BomParserFactory;
import org.cyclonedx.parsers.Parser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.sonatype.insight.brain.sbom.utils.SbomCreationDetails.CreatorType.parseCreatorType;

public class SbomCycloneDxUtils
{
  private static final Logger log = LoggerFactory.getLogger(SbomCycloneDxUtils.class);

  private static final Gson gson = new GsonBuilder().create();

  private static final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'");

  public static final String PROPERTY_SONATYPE_IDENTIFIER = "sonatypeIdentifier";

  public static final String PROPERTY_COMPONENT_REF = "componentRef";

  public static final String PROPERTY_COMPONENT_REFS = "componentRefs";

  public static final String VULNERABILITY_KEY = "vulnerabilities";

  private SbomCycloneDxUtils() {
    // no-op
  }

  public static String getApplicationNameSafely(Bom bomDocument) {
    Metadata metadata = getMetadata(bomDocument);
    if (metadata != null && metadata.getComponent() != null) {
      return metadata.getComponent().getName();
    }
    return null;
  }

  public static String getApplicationVersionSafely(Bom bomDocument) {
    Metadata metadata = getMetadata(bomDocument);
    if (metadata != null && metadata.getComponent() != null) {
      return metadata.getComponent().getVersion();
    }
    return null;
  }

  public static String getOrGenerateSerialNumber(Bom bomDocument) {
    final String serialNumber = bomDocument.getSerialNumber();
    if (serialNumber != null) {
      return serialNumber;
    }
    return "urn:uuid:" + UUID.randomUUID();
  }

  private static Metadata getMetadata(Bom bomDocument) {
    if (bomDocument != null) {
      return bomDocument.getMetadata();
    }
    return null;
  }

  public static SbomCreationDetails getSbomCreationDetails(Bom bom) {
    if (bom == null || bom.getMetadata() == null) {
      return null;
    }
    SbomCreationDetails extractedMetadata = new SbomCreationDetails();
    Metadata metadataFromSbomFile = bom.getMetadata();
    Component componentMetadata = metadataFromSbomFile.getComponent();
    if (componentMetadata != null) {
      extractedMetadata.type = componentMetadata.getType().getTypeName();
    }
    Date timestampFromSbomFile = metadataFromSbomFile.getTimestamp();
    if (timestampFromSbomFile != null) {
      extractedMetadata.created = dateTimeFormatter.withZone(ZoneOffset.UTC)
          .format(timestampFromSbomFile.toInstant());
    }
    buildCreators(extractedMetadata, metadataFromSbomFile);
    buildTools(extractedMetadata, metadataFromSbomFile);
    return extractedMetadata;
  }

  public static String getSbomCreationDetailsJson(Bom bom) {
    SbomCreationDetails sbomCreationDetails = getSbomCreationDetails(bom);
    return sbomCreationDetails != null ? gson.toJson(sbomCreationDetails) : null;
  }

  public static BomPageMetadataDTO buildBomPageMetadataDTO(
      ThirdPartySbomMetadata sbomMetadata,
      ThirdPartyScan scan,
      MigrationTrackerDAO migrationTrackerDAO)
  {
    String metadataJson = sbomMetadata.getMetadataJson();
    List<String> manufacturerList = new ArrayList<>();
    List<String> supplierList = new ArrayList<>();
    List<String> authorList = new ArrayList<>();
    List<String> personList = new ArrayList<>();
    List<String> organizationList = new ArrayList<>();
    if (metadataJson != null) {
      try {
        SbomCreationDetails creationDetails = JsonUtils.parse(metadataJson, SbomCreationDetails.class);
        if (creationDetails.creators != null) {
          for (Creator creator : creationDetails.creators) {
            switch (parseCreatorType(creator.type)) {
              case Manufacturer:
                if (!organizationList.contains(creator.name)) {
                  manufacturerList.add(creator.name);
                }
                break;
              case Supplier:
                if (!supplierList.contains(creator.name)) {
                  supplierList.add(creator.name);
                }
                break;
              case Author:
                if (!authorList.contains(creator.name)) {
                  authorList.add(creator.name);
                }
                break;
              case Person:
                if (!personList.contains(creator.name)) {
                  personList.add(creator.name);
                }
                break;
              case Organization:
                if (!organizationList.contains(creator.name)) {
                  organizationList.add(creator.name);
                }
                break;
              default:
                break;
            }
          }
        }
      }
      catch (IOException e) {
        throw new IllegalStateException("Can not read metadata json, incorrect format", e);
      }
    }
    return new BomPageMetadataDTO(
        authorList,
        manufacturerList,
        supplierList,
        personList,
        organizationList,
        sbomMetadata.getSpec(),
        sbomMetadata.getSpecVersion(),
        sbomMetadata.getSpecFormat(),
        sbomMetadata.getCreatedAt(),
        scan != null ? scan.getScanId() : null,
        sbomMetadata.getIsValid(),
        sbomMetadata.getOriginalBinaryFileName(),
        migrationTrackerDAO.isTrackerPresent(DisplayNameForFileCoordinateAsyncDbMigration.class.getSimpleName()));
  }

  private static void buildCreators(SbomCreationDetails extractedMetadata, Metadata metadataFromSbomFile) {
    extractedMetadata.creators = new ArrayList<>();
    buildCreatorsWithAuthors(extractedMetadata, metadataFromSbomFile);
    buildCreatorsWithManufacturer(extractedMetadata, metadataFromSbomFile);
    buildCreatorsWithSupplier(extractedMetadata, metadataFromSbomFile);
    if (extractedMetadata.creators.isEmpty()) {
      extractedMetadata.creators = null;
    }
  }

  private static void buildCreatorsWithAuthors(SbomCreationDetails extractedMetadata, Metadata metadataFromSbomFile) {
    List<OrganizationalContact> authorsFromSbomFile = metadataFromSbomFile.getAuthors();
    if (!CollectionUtils.isEmpty(authorsFromSbomFile)) {
      extractedMetadata.creators.addAll(authorsFromSbomFile.stream()
          .map(organizationalContact -> mapOrganizationalContactToCreator(organizationalContact,
              SbomCreationDetails.CreatorType.Author.name()))
          .toList());
    }
  }

  private static void buildCreatorsWithManufacturer(
      SbomCreationDetails extractedMetadata,
      Metadata metadataFromSbomFile)
  {
    OrganizationalEntity manufactureFromSbomFile = metadataFromSbomFile.getManufacture();
    if (manufactureFromSbomFile != null && CollectionUtils.isNotEmpty(manufactureFromSbomFile.getContacts())) {
      extractedMetadata.creators.addAll(manufactureFromSbomFile.getContacts()
          .stream()
          .map(contact -> mapOrganizationalContactToCreator(contact,
              SbomCreationDetails.CreatorType.Manufacturer.name(),
              manufactureFromSbomFile.getUrls()))
          .toList());
    }
    else if (manufactureFromSbomFile != null) {
      extractedMetadata.creators.add(
          mapOrganizationalEntityToCreator(manufactureFromSbomFile, CreatorType.Manufacturer.name()));
    }
  }

  private static void buildCreatorsWithSupplier(SbomCreationDetails extractedMetadata, Metadata metadataFromSbomFile) {
    OrganizationalEntity supplierFromSbomFile = metadataFromSbomFile.getSupplier();
    if (supplierFromSbomFile != null && CollectionUtils.isNotEmpty(supplierFromSbomFile.getContacts())) {
      extractedMetadata.creators.addAll(supplierFromSbomFile.getContacts()
          .stream()
          .map(contact -> mapOrganizationalContactToCreator(contact, CreatorType.Supplier.name(),
              supplierFromSbomFile.getUrls()))
          .toList());
    }
    else if (supplierFromSbomFile != null) {
      extractedMetadata.creators.add(
          mapOrganizationalEntityToCreator(supplierFromSbomFile, CreatorType.Supplier.name()));
    }
  }

  private static SbomCreationDetails.Creator mapOrganizationalContactToCreator(
      OrganizationalContact organizationalContact,
      String creatorType,
      List<String> urls)
  {
    SbomCreationDetails.Creator creator = mapOrganizationalContactToCreator(organizationalContact, creatorType);
    StringBuilder creatorUrls = new StringBuilder();
    if (!CollectionUtils.isEmpty(urls)) {
      for (int i = 0; i < urls.size(); i++) {
        if (i == urls.size() - 1) {
          creatorUrls.append(urls.get(i));
        }
        else {
          creatorUrls.append(urls.get(i)).append(",");
        }
      }
    }
    creator.url = creatorUrls.toString();
    return creator;
  }

  private static SbomCreationDetails.Creator mapOrganizationalContactToCreator(
      OrganizationalContact organizationalContact,
      String creatorType)
  {
    SbomCreationDetails.Creator creator = new SbomCreationDetails.Creator();
    creator.type = creatorType;
    if (organizationalContact.getName() != null) {
      creator.name = organizationalContact.getName();
    }
    if (organizationalContact.getEmail() != null) {
      creator.email = organizationalContact.getEmail();
    }
    if (organizationalContact.getPhone() != null) {
      creator.phone = organizationalContact.getPhone();
    }
    return creator;
  }

  private static SbomCreationDetails.Creator mapOrganizationalEntityToCreator(
      OrganizationalEntity organizationalEntity,
      String creatorType)
  {
    SbomCreationDetails.Creator creator = new SbomCreationDetails.Creator();
    creator.type = creatorType;
    if (organizationalEntity.getName() != null) {
      creator.name = organizationalEntity.getName();
    }
    if (!CollectionUtils.isEmpty(organizationalEntity.getUrls())) {
      creator.url = String.join(",", organizationalEntity.getUrls());
    }
    return creator;
  }

  private static void buildTools(SbomCreationDetails extractedMetadata, Metadata metadataFromSbomFile) {
    extractedMetadata.tools = new ArrayList<>();
    List<Tool> legacyToolsFromSbomFile = metadataFromSbomFile.getTools();
    if (metadataFromSbomFile.getToolChoice() != null) {
      log.debug("Using tools from tool choice in sbom");
      if (!CollectionUtils.isEmpty(metadataFromSbomFile.getToolChoice().getComponents())) {
        List<Component> toolChoiceComponentsFromSbomFile = metadataFromSbomFile.getToolChoice().getComponents();
        buildToolsWithToolChoiceComponents(extractedMetadata, toolChoiceComponentsFromSbomFile);
      }
      if (!CollectionUtils.isEmpty(metadataFromSbomFile.getToolChoice().getServices())) {
        List<Service> toolChoiceServicesFromSbomFile = metadataFromSbomFile.getToolChoice().getServices();
        buildToolsWithToolChoiceServices(extractedMetadata, toolChoiceServicesFromSbomFile);
      }
    }
    else if (!CollectionUtils.isEmpty(legacyToolsFromSbomFile)) {
      log.debug("Using tools from legacy tools in sbom");
      buildToolsWithLegacyTool(extractedMetadata, legacyToolsFromSbomFile);
    }
    if (extractedMetadata.tools.size() == 0) {
      extractedMetadata.tools = null;
    }
  }

  private static void buildToolsWithToolChoiceComponents(
      SbomCreationDetails extractedMetadata,
      List<Component> toolComponentsFromSbom)
  {
    extractedMetadata.tools.addAll(toolComponentsFromSbom.stream().map(bomTool -> {
      SbomCreationDetails.Tool tool = new SbomCreationDetails.Tool();
      if (bomTool.getName() != null) {
        tool.name = bomTool.getName();
      }
      if (bomTool.getVersion() != null) {
        tool.version = bomTool.getVersion();
      }
      if (bomTool.getType() != null) {
        tool.type = bomTool.getType().getTypeName();
      }
      if (!CollectionUtils.isEmpty(bomTool.getComponents())) {
        buildToolsWithToolChoiceComponents(extractedMetadata, bomTool.getComponents());
      }
      return tool;
    }).collect(Collectors.toList()));
  }

  private static void buildToolsWithToolChoiceServices(
      SbomCreationDetails extractedMetadata,
      List<Service> toolServicesFromSbom)
  {
    extractedMetadata.tools.addAll(toolServicesFromSbom.stream().map(bomService -> {
      SbomCreationDetails.Tool tool = new SbomCreationDetails.Tool();
      if (bomService.getName() != null) {
        tool.name = bomService.getName();
      }
      if (bomService.getVersion() != null) {
        tool.version = bomService.getVersion();
      }
      if (!CollectionUtils.isEmpty(bomService.getServices())) {
        buildToolsWithToolChoiceServices(extractedMetadata, bomService.getServices());
      }
      return tool;
    }).collect(Collectors.toList()));
  }

  private static void buildToolsWithLegacyTool(
      SbomCreationDetails extractedMetadata,
      List<Tool> legacyToolsFromSbomFile)
  {
    extractedMetadata.tools.addAll(legacyToolsFromSbomFile.stream().map(bomTool -> {
      SbomCreationDetails.Tool tool = new SbomCreationDetails.Tool();
      if (bomTool.getName() != null) {
        tool.name = bomTool.getName();
      }
      if (bomTool.getVersion() != null) {
        tool.version = bomTool.getVersion();
      }
      return tool;
    }).collect(Collectors.toList()));
  }

  public static Bom parseContentNoValidation(final String content) throws IOException, ParseException {
    return parseContentStreamNoValidation(IOUtils.toInputStream(content, StandardCharsets.UTF_8));
  }

  public static Bom parseContentStreamNoValidation(InputStream is) throws IOException, ParseException {
    byte[] bytes = IOUtils.toByteArray(is);
    Parser parser = BomParserFactory.createParser(bytes);
    return parser.parse(bytes);
  }

  /*
   * Currently used for backward compatibility SBOMs with a null componentRef
   *
   * @deprecated search for a component by component-ref instead.
   */
  @Deprecated
  public static Optional<Component> findComponentByPackageUrl(String packageUrl, Bom bom) {
    if (packageUrl == null) {
      return Optional.empty();
    }

    return bom.getComponents()
        .stream()
        .filter(
            bc -> packageUrl.equals(bc.getPurl()))
        .findFirst();
  }

  public static Method resolveRatingMethod(String text) {
    for (Method method : Method.values()) {
      if (StringUtils.equalsIgnoreCase(method.getMethodName(), text)) {
        return method;
      }
    }
    return null;
  }

  public static Method resolveRatingMethodFromSeveritySource(String severitySource) {
    return switch (severitySource) {
      case "cve_cvss_2" -> Method.CVSSV2;
      case "cve_cvss_3" -> Method.CVSSV3;
      case "cve_cvss_31" -> Method.CVSSV31;
      case "cve_cvss_4" -> Method.CVSSV4;
      default -> Method.OTHER;
    };
  }

  public static String getGenericSbomCreationDetailsAsString() {
    SbomCreationDetails sbomCreationDetails = new SbomCreationDetails();
    sbomCreationDetails.created = dateTimeFormatter.withZone(ZoneOffset.UTC).format(Instant.now());
    sbomCreationDetails.tools = new ArrayList<>();
    SbomCreationDetails.Tool sbomManagerTool = new SbomCreationDetails.Tool();
    sbomManagerTool.type = Component.Type.APPLICATION.getTypeName();
    sbomManagerTool.name = "Sonatype SBOM Manager";
    sbomCreationDetails.tools.add(sbomManagerTool);
    return gson.toJson(sbomCreationDetails);
  }

  public static String getFilteredPathname(String pathname) {
    return StringUtils.removeStart(pathname, "dependency:/");
  }

  public static Optional<Version> getVersionFromString(String versionString) {
    return Arrays.stream(Version.values())
        .filter(cycloneDxVersion -> cycloneDxVersion.getVersionString().equalsIgnoreCase(versionString))
        .findFirst();
  }

  public static void addSonatypeTruncatedSha1(String sha1, Component component) {
    Property property = new Property();
    property.setName(SbomTaxonomy.CDX_SONATYPE_SHA1_PROPERTY_NAME);
    property.setValue(StringUtils.truncate(sha1, 0, HashHelper.MAX_LENGTH));
    component.addProperty(property);
  }

  public static String getSonatypeTruncatedSha1(Component component) {
    if (component != null && CollectionUtils.isNotEmpty(component.getProperties())) {
      return component.getProperties()
          .stream()
          .filter(p -> SbomTaxonomy.CDX_SONATYPE_SHA1_PROPERTY_NAME.equals(p.getName()))
          .findFirst()
          .map(Property::getValue)
          .orElse(null);
    }
    return null;
  }

  public static void addSonatypeOriginalPurl(final String originalPurl, final Component component) {
    if (StringUtils.isNotBlank(originalPurl)) {
      Property property = new Property();
      property.setName(SbomTaxonomy.CDX_ORIGINAL_PURL_PROPERTY_NAME);
      property.setValue(originalPurl);
      component.addProperty(property);
    }
  }
}
