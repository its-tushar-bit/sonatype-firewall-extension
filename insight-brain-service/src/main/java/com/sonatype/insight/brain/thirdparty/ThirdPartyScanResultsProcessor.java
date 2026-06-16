/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.thirdparty;

import java.io.IOException;
import java.io.OutputStream;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.zip.GZIPInputStream;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventFactory;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.XMLEvent;
import javax.xml.stream.util.EventReaderDelegate;

import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyFileDAO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartySbomMetadataDAO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyScanDAO;
import com.sonatype.insight.brain.model.policy.stages.ComplianceStageType;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyFile;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartySbomMetadata;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyScan;
import com.sonatype.insight.brain.product.license.ProductLicense;
import com.sonatype.insight.brain.sbom.utils.SbomDetectionResult;
import com.sonatype.insight.brain.sbom.utils.SbomFileDetector;
import com.sonatype.insight.brain.sbom.utils.SbomMetadataUtils;
import com.sonatype.insight.brain.scan.datastore.ScanEntity;
import com.sonatype.insight.brain.telemetry.TelemetrySender;
import com.sonatype.insight.brain.utils.CheckedIllegalArgumentException;
import com.sonatype.insight.brain.utils.Xpp3Util;
import com.sonatype.insight.license.model.LicensedFeature;
import com.sonatype.insight.scan.model.ItemContentType;
import com.sonatype.insight.scan.model.ProjectScanItem;
import com.sonatype.insight.scan.model.io.XStreamFactory;
import com.sonatype.insight.telemetry.model.TelemetryData;

import com.thoughtworks.xstream.XStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.codehaus.plexus.util.xml.XmlStreamReader;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.codehaus.plexus.util.xml.pull.MXParser;
import org.codehaus.plexus.util.xml.pull.XmlPullParser;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.util.Arrays.asList;

@Named
public class ThirdPartyScanResultsProcessor
{
  private static final Logger log = LoggerFactory.getLogger(ThirdPartyScanResultsProcessor.class);

  private static final XStream xstream = XStreamFactory.newInstance();

  private static final List<String> thirdPartyItemContentTypes =
      asList(ItemContentType.CLAIR_SCANNER.name(), ItemContentType.SBOM.name(), ItemContentType.CONTAINER_URI.name(),
          ItemContentType.CONTAINER_URI_SONATYPE.name(), ItemContentType.SPDX.name(), ItemContentType.IAC_FILE.name());

  private static final XMLEventFactory EVENT_FACTORY = XMLEventFactory.newInstance();

  private static final String CONTENT_TYPE_LIST_ATTRIBUTE = "content_type_list";

  private final ThirdPartyScanDAO thirdPartyScanDAO;

  private final ThirdPartyFileDAO thirdPartyFileDAO;

  private final ThirdPartySbomMetadataDAO thirdPartySbomMetadataDAO;

  private final TelemetrySender telemetrySender;

  private final ThirdPartyResultHandlerFactory thirdPartyResultHandlerFactory;

  private final ProductLicense productLicense;

  private final SbomFileDetector sbomFileDetector;

  private final SbomMetadataUtils sbomMetadataUtils;

  private final ThirdPartyPersistenceService thirdPartyPersistenceService;

  @Inject
  public ThirdPartyScanResultsProcessor(
      ThirdPartyScanDAO thirdPartyScanDAO,
      ThirdPartyFileDAO thirdPartyFileDAO,
      ThirdPartySbomMetadataDAO thirdPartySbomMetadataDAO,
      TelemetrySender telemetrySender,
      ThirdPartyResultHandlerFactory thirdPartyResultHandlerFactory,
      ProductLicense productLicense,
      SbomFileDetector sbomFileDetector,
      SbomMetadataUtils sbomMetadataUtils,
      ThirdPartyPersistenceService thirdPartyPersistenceService)
  {
    this.telemetrySender = telemetrySender;
    this.thirdPartyFileDAO = thirdPartyFileDAO;
    this.thirdPartySbomMetadataDAO = thirdPartySbomMetadataDAO;
    this.thirdPartyScanDAO = thirdPartyScanDAO;
    this.thirdPartyResultHandlerFactory = thirdPartyResultHandlerFactory;
    this.productLicense = productLicense;
    this.sbomFileDetector = sbomFileDetector;
    this.sbomMetadataUtils = sbomMetadataUtils;
    this.thirdPartyPersistenceService = thirdPartyPersistenceService;
  }

  public String filterAndSaveData(
      ScanEntity scanEntity,
      ScanEntity tempScanEntity,
      ThirdPartyScanContext scanContext,
      TelemetryData thirdPartyScanTelemetryData)
  {
    String scanRequestId = scanContext.getScanRequestId();
    log.info("Processing third party content with scanRequestId: {}", scanRequestId);

    try (GZIPInputStream gis = new GZIPInputStream(scanEntity.getInputStream());
        OutputStream out = new GzipCompressorOutputStream(tempScanEntity.getOutputStream()))
    {

      XmlPullParser parser = new MXParser();
      parser.setInput(new XmlStreamReader(gis));

      XMLOutputFactory outputFactory = XMLOutputFactory.newInstance();
      XMLEventWriter writer = outputFactory.createXMLEventWriter(out);

      parser.next();
      while (parser.getEventType() != XmlPullParser.END_DOCUMENT) {
        processEvent(scanContext, parser, writer, thirdPartyScanTelemetryData);
      }
      writer.flush();
      writer.close();
      telemetrySender.send(thirdPartyScanTelemetryData);
      log.info("Completed processing third party content in file {}", scanEntity.getLocation());
      return scanRequestId;
    }
    catch (Exception e) {
      throw new RuntimeException("Error reading/processing third party scan content from scan file", e);
    }
  }

  private void processEvent(
      ThirdPartyScanContext scanContext,
      XmlPullParser parser,
      XMLEventWriter writer,
      TelemetryData thirdPartyScanTelemetryData) throws XmlPullParserException, XMLStreamException, IOException
  {
    int eventType = parser.getEventType();
    boolean foundEndTag = false;
    while (!foundEndTag) {
      String elementName = parser.getName();
      if (eventType == XmlPullParser.START_TAG) {
        writer.add(EVENT_FACTORY.createStartElement(new QName(parser.getName()), null, null));
        addElementAttributes(parser, writer);
        if ("item".equals(elementName)) {
          processItemElement(scanContext, parser, writer, thirdPartyScanTelemetryData);
        }
      }
      else if (eventType == XmlPullParser.END_TAG) {
        if (!elementName.equals(parser.getName())) {
          throw new XmlPullParserException(
              "End tag '" + parser.getName() + "' does not match start tag '" + elementName + "'.");
        }
        writer.add(EVENT_FACTORY.createEndElement(new QName(parser.getName()), null));
        foundEndTag = true;
      }
      else if (eventType == XmlPullParser.TEXT) {
        writer.add(EVENT_FACTORY.createCharacters(parser.getText()));
      }
      eventType = parser.next();
    }
  }

  private void processItemElement(
      ThirdPartyScanContext scanContext,
      XmlPullParser parser,
      XMLEventWriter writer,
      TelemetryData thirdPartyScanTelemetryData) throws XMLStreamException, IOException, XmlPullParserException
  {
    String contentType = parser.getAttributeValue(null, "contentType");
    if (contentType != null && thirdPartyItemContentTypes.contains(contentType)) {
      addContentTypeToTelemetry(thirdPartyScanTelemetryData, contentType);
      List<ProjectScanItem> moduleDependencies = new ArrayList<>();
      if (!contentType.equals(ItemContentType.IAC_FILE.name())) {
        Xpp3Dom itemElement = Xpp3Util.loadElement("item", parser);
        Xpp3Dom contentElement = itemElement.getChild("content");
        if (contentElement != null) {
          FilteredThirdPartyContent filteredThirdPartyContent =
              handleContent(itemElement, contentElement, contentType, scanContext);
          writeFilteredInformation(writer, filteredThirdPartyContent);
          Optional.of(filteredThirdPartyContent.getModuleDependencies())
              .ifPresent(moduleDependencies::addAll);
        }
        else {
          log.error("scan file {} contained a third party scan item {} without any content",
              scanContext.getScanEntity().getName(), contentType);
        }
        writer.add(EVENT_FACTORY.createEndElement(new QName(parser.getName()), null));
        writeDependencyGraph(writer, moduleDependencies);
      }
    }
  }

  private static void addContentTypeToTelemetry(
      final TelemetryData tpScanTelemetryData,
      final String contentType)
  {
    if (tpScanTelemetryData != null) {
      Object contentTypeList = tpScanTelemetryData.getAttributes().get(CONTENT_TYPE_LIST_ATTRIBUTE);
      if (contentTypeList instanceof List) {
        // noinspection unchecked
        ((List<String>) contentTypeList).add(contentType);
      }
      else {
        tpScanTelemetryData.getAttributes().put(CONTENT_TYPE_LIST_ATTRIBUTE, new ArrayList<>(asList(contentType)));
      }
    }
  }

  private FilteredThirdPartyContent handleContent(
      Xpp3Dom itemElement,
      Xpp3Dom contentElement,
      String contentType,
      ThirdPartyScanContext scanContext)
  {
    String path = itemElement.getAttribute("path");
    String lastModified = itemElement.getAttribute("lastModified");
    String sha1 = itemElement.getAttribute("sha1");

    var thirdPartyFile = storeSbom(itemElement, contentElement, scanContext);

    ItemContentType contentItemType = ItemContentType.valueOf(contentType);
    if (ItemContentType.CONTAINER_URI.equals(contentItemType)
        || ItemContentType.CONTAINER_URI_SONATYPE.equals(contentItemType))
    {
      scanContext.addContainerUriPath(path);
    }
    ThirdPartyScanResultHandler handler = createHandler(contentItemType, scanContext);
    return handler.handleAndFilterContents(
        new ThirdPartyScanContent(path, contentItemType, lastModified, sha1, contentElement.getValue()),
        thirdPartyFile);
  }

  private ThirdPartyScan ensureThirdPartyScanIsSaved(ThirdPartyFile thirdPartyFile, ThirdPartyScanContext scanContext) {
    ThirdPartyScan thirdPartyScan = thirdPartyScanDAO.getByThirdPartyFileId(thirdPartyFile.getId());
    if (thirdPartyScan == null) {
      thirdPartyScan = thirdPartyPersistenceService.associateWithScan(thirdPartyFile, scanContext.getScanRequestId());
    }
    else {
      var scanContextScanId = scanContext.getThirdPartyScanId();
      if (scanContextScanId != null && !scanContextScanId.equals(thirdPartyScan.getId())) {
        throw new IllegalStateException("""
            Already-saved ThirdPartyScan does not match information being processed in ThirdPartyScanResultsProcessor: \
            %s != %s""".formatted(thirdPartyScan.getId(), scanContextScanId));
      }
    }

    scanContext.setThirdPartyScanId(thirdPartyScan.getId());
    return thirdPartyScan;
  }

  private void addElementAttributes(XmlPullParser parser, XMLEventWriter writer) throws XMLStreamException {
    Map<String, String> attributes = Xpp3Util.loadAttributes(parser);
    for (Map.Entry<String, String> attribute : attributes.entrySet()) {
      // the SPDX content is converted to CycloneDx during content handling and filtering,
      // so the contentType attribute has to be set to SBOM in this case
      if (attribute.getKey().equals("contentType") && attribute.getValue().equals(ItemContentType.SPDX.name())) {
        writer.add(EVENT_FACTORY.createAttribute(attribute.getKey(), ItemContentType.SBOM.name()));
      }
      else {
        writer.add(EVENT_FACTORY.createAttribute(attribute.getKey(), attribute.getValue()));
      }
    }
  }

  private void writeFilteredInformation(
      XMLEventWriter writer,
      FilteredThirdPartyContent filteredThirdPartyContent) throws XMLStreamException
  {
    if (filteredThirdPartyContent.hasErrors()) {
      try {
        writer.add(EVENT_FACTORY.createAttribute("hasError", "true"));
      }
      catch (XMLStreamException e) {
        log.debug("Could not add the hasError attribute to the item tag", e);
      }
    }

    String filteredInformation = filteredThirdPartyContent.getContent();
    writer.add(EVENT_FACTORY.createCharacters("\n"));
    QName name = new QName("content");
    writer.add(EVENT_FACTORY.createStartElement(name, null, null));
    XMLEvent contentEvent;
    contentEvent = EVENT_FACTORY.createCData(filteredInformation);
    writer.add(contentEvent);
    writer.add(EVENT_FACTORY.createEndElement(name, null));
    writer.add(EVENT_FACTORY.createCharacters("\n"));
  }

  private void writeDependencyGraph(
      final XMLEventWriter writer,
      final List<ProjectScanItem> moduleDependencies) throws XMLStreamException
  {
    for (ProjectScanItem moduleDependency : moduleDependencies) {
      String xml = xstream.toXML(moduleDependency);
      XMLEventReader reader = new EventReaderDelegate(getXmlInputFactorySafely(xml))
      {
        @Override
        public boolean hasNext() {
          if (!super.hasNext()) {
            return false;
          }
          try {
            return !super.peek().isEndDocument();
          }
          catch (XMLStreamException ignored) {
            return true;
          }
        }
      };

      if (reader.peek().isStartDocument()) {
        reader.nextEvent();
      }
      writer.add(reader);
    }
  }

  private XMLEventReader getXmlInputFactorySafely(String xml) throws XMLStreamException {
    XMLInputFactory factory = XMLInputFactory.newFactory();
    factory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);
    factory.setProperty(XMLInputFactory.IS_REPLACING_ENTITY_REFERENCES, false);
    factory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
    return factory.createXMLEventReader(new StringReader(xml));
  }

  ThirdPartyScanResultHandler createHandler(
      ItemContentType contentItemType,
      ThirdPartyScanContext thirdPartyScanContext)
  {
    return thirdPartyResultHandlerFactory.newHandler(contentItemType, thirdPartyScanContext);
  }

  /**
   * Stores the SBOM contents and high-level database records (ThirdPartySbomMetadata, ThirdPartyFile, and
   * ThirdPartyScan) as appropriate for this type of scan. Specifically, the ThirdPartySbomMetadata will only
   * be saved for COMPLIANCE stage scans (i.e. SBOM Manager uploads) and not for Lifecycle scans. Additionally,
   * records that have already been saved will not be duplicated, in order to facilitate the two-step
   * workflow in SBOM Manager where the ThirdPartySbomMetadata and ThirdPartyFile records are saved in one REST
   * call and the evaluation is done in a second REST call (this class is involved only in the second REST call).
   * ThirdPartySbomMetadata records that are created here will have a PENDING status.
   *
   * @return the saved ThirdPartyFile, or null if an error occurs.
   *         The ThirdPartySbomMetadata will be null if this is a Lifecycle scan for which the SBOM metadata should not
   *         be stored.
   */
  private ThirdPartyFile storeSbom(
      Xpp3Dom itemElement,
      Xpp3Dom contentElement,
      ThirdPartyScanContext scanContext)
  {
    boolean shouldStoreSbomMetadata = shouldStoreAsSbom(scanContext);
    ThirdPartyFile thirdPartyFile = null;
    ThirdPartySbomMetadata sbomMetadata = null;
    var sbomContent = contentElement.getValue();
    var filename = itemElement.getAttribute("path");

    try {
      if (shouldStoreSbomMetadata) {
        // SBOM Manager upload
        if (scanContext.getSbomMetadataId() == null) {
          SbomDetectionResult sbomDetectionResult =
              sbomFileDetector.getSbomDetectionResult(sbomContent, filename, true);
          ImmutablePair<ThirdPartySbomMetadata, ThirdPartyFile> entities;

          // Capture the user-preferred version before the save, because line 409 overwrites
          // scanContext.applicationVersion with the persisted (possibly collision-suffixed) version.
          String userPreferredVersion = scanContext.getApplicationVersion();

          if (sbomDetectionResult.isSbom) {
            entities = thirdPartyPersistenceService.saveSbomManagerSbomFromScan(
                sbomContent,
                filename,
                scanContext.getApplicationId(),
                userPreferredVersion,
                sbomDetectionResult);
          }
          else {
            entities = thirdPartyPersistenceService.saveSbomManagerBinaryFromScan(
                sbomContent,
                filename,
                scanContext.getApplicationId(),
                userPreferredVersion,
                sbomDetectionResult);
          }

          sbomMetadata = entities.getLeft();
          thirdPartyFile = entities.getRight();

          thirdPartyPersistenceService.setSbomMetadataStatusToPending(sbomMetadata);

          scanContext.markSbomSavedForScan();
          scanContext.setSbomMetadataId(sbomMetadata.getId());
          scanContext.setThirdPartyFileId(thirdPartyFile.getId());
          scanContext.setApplicationVersion(sbomMetadata.getSbomVersion());

          // it will be null if SbomFileDetector determines that the file isn't an SBOM at all
          if (sbomDetectionResult.isValid != null) {
            scanContext.setIsValid(sbomDetectionResult.isValid);
          }
        }
        else {
          sbomMetadata = thirdPartySbomMetadataDAO.getByIdNotNull(scanContext.getSbomMetadataId());
          thirdPartyFile = thirdPartyFileDAO.getByIdNotNull(sbomMetadata.getThirdPartyFileId());
          if (scanContext.getThirdPartyFileId() == null) {
            scanContext.setThirdPartyFileId(thirdPartyFile.getId());
          }
        }
      }
      else {
        // Lifecycle scan

        thirdPartyFile = thirdPartyPersistenceService.saveLifecycleSbomFromScan(filename);

        // Note: in LC multiple SBOMs present in the scan will be saved separately, so there may be multiple
        // ThirdPartyFiles. The scan context just holds the id of the most recently saved one
        scanContext.setThirdPartyFileId(thirdPartyFile.getId());
      }

      if (scanContext.getThirdPartyFileId() != null &&
          !scanContext.getThirdPartyFileId().equals(thirdPartyFile.getId()))
      {
        throw new IllegalStateException("""
            Already-saved ThirdPartyFile does not match information being processed in ThirdPartyScanResultsProcessor: \
            %s != %s""".formatted(scanContext.getThirdPartyFileId(), thirdPartyFile.getId()));
      }

      ensureThirdPartyScanIsSaved(thirdPartyFile, scanContext);

      return thirdPartyFile;
    }
    catch (IOException | CheckedIllegalArgumentException e) {
      log.error("there was an error while trying to store sbom file {}", scanContext.getScanEntity().getName(), e);
      return null;
    }
  }

  /**
   * @return whether or not the ThirdPartySbomMetadata and actual SBOM file contents should be persisted for this
   *         scan.
   */
  private boolean shouldStoreAsSbom(ThirdPartyScanContext scanContext) {
    if (!productLicense.hasFeature(LicensedFeature.SBOM_MANAGER)) {
      return false;
    }
    if (sbomMetadataUtils.hasMaxSbomLimitBeenReached()) {
      log.warn("SBOM cap reached for application {} (license max={}); skipping SBOM persistence for this scan.",
          scanContext.getApplicationId(), productLicense.getMaxSboms());
      return false;
    }
    return isStageTypeSupported(scanContext);
  }

  private boolean isStageTypeSupported(ThirdPartyScanContext scanContext) {
    return scanContext.getStageType().equalsIgnoreCase(ComplianceStageType.ID)
        && productLicense.getStageTypes()
            .stream()
            .anyMatch(stageType -> stageType.getId().equalsIgnoreCase(ComplianceStageType.ID));
  }
}
