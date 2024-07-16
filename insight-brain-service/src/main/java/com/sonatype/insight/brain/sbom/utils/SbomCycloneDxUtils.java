/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.sbom.utils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.io.IOUtils;
import org.cyclonedx.parsers.BomParserFactory;
import org.cyclonedx.exception.ParseException;
import org.cyclonedx.model.Bom;
import org.cyclonedx.model.Component;
import org.cyclonedx.model.Metadata;
import org.cyclonedx.model.OrganizationalContact;
import org.cyclonedx.model.OrganizationalEntity;
import org.cyclonedx.model.Property;
import org.cyclonedx.model.Service;
import org.cyclonedx.model.Tool;
import org.cyclonedx.model.vulnerability.Vulnerability.Rating;
import org.cyclonedx.parsers.Parser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SbomCycloneDxUtils
{
  private static final Logger log = LoggerFactory.getLogger(SbomCycloneDxUtils.class);

  private static final Gson gson = new GsonBuilder().create();

  private static final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'");

  public static final String PROPERTY_SONATYPE_IDENTIFIER = "sonatypeIdentifier";

  private SbomCycloneDxUtils() {
    //no-op
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

  private static void buildCreators(SbomCreationDetails extractedMetadata, Metadata metadataFromSbomFile) {
    extractedMetadata.creators = new ArrayList<>();
    buildCreatorsWithAuthors(extractedMetadata, metadataFromSbomFile);
    buildCreatorsWithManufacturer(extractedMetadata, metadataFromSbomFile);
    buildCreatorsWithSupplier(extractedMetadata, metadataFromSbomFile);
    if (extractedMetadata.creators.size() == 0) {
      extractedMetadata.creators = null;
    }
  }

  private static void buildCreatorsWithAuthors(SbomCreationDetails extractedMetadata, Metadata metadataFromSbomFile) {
    List<OrganizationalContact> authorsFromSbomFile = metadataFromSbomFile.getAuthors();
    if (!CollectionUtils.isEmpty(authorsFromSbomFile)) {
      extractedMetadata.creators.addAll(authorsFromSbomFile.stream()
          .map(organizationalContact ->
              mapOrganizationalContactToCreator(organizationalContact, SbomCreationDetails.CreatorType.Author.name()))
          .collect(Collectors.toList()));
    }
  }

  private static void buildCreatorsWithManufacturer(
      SbomCreationDetails extractedMetadata,
      Metadata metadataFromSbomFile)
  {
    OrganizationalEntity manufactureFromSbomFile = metadataFromSbomFile.getManufacture();
    if (manufactureFromSbomFile != null && CollectionUtils.isNotEmpty(manufactureFromSbomFile.getContacts())) {
      extractedMetadata.creators.addAll(manufactureFromSbomFile.getContacts().stream()
          .map(contact -> mapOrganizationalContactToCreator(contact,
              SbomCreationDetails.CreatorType.Manufacturer.name(),
              manufactureFromSbomFile.getUrls()))
          .collect(Collectors.toList()));
    }
  }

  private static void buildCreatorsWithSupplier(SbomCreationDetails extractedMetadata, Metadata metadataFromSbomFile) {
    OrganizationalEntity supplierFromSbomFile = metadataFromSbomFile.getSupplier();
    if (supplierFromSbomFile != null && CollectionUtils.isNotEmpty(supplierFromSbomFile.getContacts())) {
      extractedMetadata.creators.addAll(supplierFromSbomFile.getContacts().stream()
          .map(contact -> mapOrganizationalContactToCreator(contact, SbomCreationDetails.CreatorType.Supplier.name(),
              supplierFromSbomFile.getUrls()))
          .collect(Collectors.toList()));
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

  public static Optional<Component> findComponentByPackageUrl(String packageUrl, Bom bom) {
    if (packageUrl == null) {
      return Optional.empty();
    }

    return bom.getComponents().stream().filter(
        bc -> packageUrl.equals(bc.getPurl()) ).findFirst();
  }

  public static void addSonatypeIdentifierPropertyToComponent(
      final Pair<ComponentIdentifier, Component> resolvedComponent,
      final String fileCoordinateId)
  {
    Property prop = new Property();
    prop.setName(PROPERTY_SONATYPE_IDENTIFIER);
    prop.setValue(fileCoordinateId);
    resolvedComponent.getRight().addProperty(prop);
  }

  public static Rating.Method resolveRatingMethod(String text) {
    for (Rating.Method method : Rating.Method.values()) {
      if (StringUtils.equalsIgnoreCase(method.getMethodName(), text)) {
        return method;
      }
    }
    return null;
  }
}
