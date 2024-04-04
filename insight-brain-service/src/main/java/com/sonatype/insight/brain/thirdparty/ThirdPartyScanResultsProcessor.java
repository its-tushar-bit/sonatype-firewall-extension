/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.thirdparty;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.zip.GZIPInputStream;
import javax.inject.Inject;
import javax.inject.Named;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventFactory;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.XMLEvent;
import javax.xml.stream.util.EventReaderDelegate;

import com.sonatype.insight.brain.audit.AuditData;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyFileDAO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartySbomMetadataDAO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyScanDAO;
import com.sonatype.insight.brain.model.policy.stages.ReleaseStageType;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyFile;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartySbomMetadata;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyScan;
import com.sonatype.insight.brain.product.license.ProductLicense;
import com.sonatype.insight.brain.sbom.utils.SbomDetectionResult;
import com.sonatype.insight.brain.sbom.utils.SbomFileDetector;
import com.sonatype.insight.brain.sbom.utils.SbomMetadataUtils;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.brain.telemetry.TelemetrySender;
import com.sonatype.insight.brain.utils.Xpp3Util;
import com.sonatype.insight.license.model.LicensedFeature;
import com.sonatype.insight.scan.file.InvalidSbomException;
import com.sonatype.insight.scan.file.UnsupportedSbomException;
import com.sonatype.insight.scan.model.ItemContentType;
import com.sonatype.insight.scan.model.ProjectScanItem;
import com.sonatype.insight.scan.model.io.XStreamFactory;
import com.sonatype.insight.telemetry.model.TelemetryData;

import com.thoughtworks.xstream.XStream;
import io.dropwizard.logback.shaded.guava.annotations.VisibleForTesting;
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.codehaus.plexus.util.FileUtils;
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
          ItemContentType.SPDX.name(), ItemContentType.IAC_FILE.name());

  private static final XMLEventFactory EVENT_FACTORY = XMLEventFactory.newInstance();

  private final ThirdPartyScanDAO thirdPartyScanDAO;

  private final ThirdPartyFileDAO thirdPartyFileDAO;

  private final ThirdPartySbomMetadataDAO thirdPartySbomMetadataDAO;

  private final TelemetrySender telemetrySender;

  private final ThirdPartyResultHandlerFactory thirdPartyResultHandlerFactory;

  private final InsightWork insightWork;

  private final ProductLicense productLicense;

  private final SbomFileDetector sbomFileDetector;

  private final DateTimeFormatter dtFormatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");

  private final SbomMetadataUtils sbomMetadataUtils;

  @Inject
  public ThirdPartyScanResultsProcessor(
      ThirdPartyScanDAO thirdPartyScanDAO,
      ThirdPartyFileDAO thirdPartyFileDAO,
      ThirdPartySbomMetadataDAO thirdPartySbomMetadataDAO,
      TelemetrySender telemetrySender,
      ThirdPartyResultHandlerFactory thirdPartyResultHandlerFactory,
      InsightWork insightWork,
      ProductLicense productLicense,
      SbomFileDetector sbomFileDetector, final SbomMetadataUtils sbomMetadataUtils)
  {
    this.telemetrySender = telemetrySender;
    this.thirdPartyFileDAO = thirdPartyFileDAO;
    this.thirdPartyScanDAO = thirdPartyScanDAO;
    this.thirdPartySbomMetadataDAO =  thirdPartySbomMetadataDAO;
    this.thirdPartyResultHandlerFactory = thirdPartyResultHandlerFactory;
    this.insightWork = insightWork;
    this.productLicense = productLicense;
    this.sbomFileDetector = sbomFileDetector;
    this.sbomMetadataUtils = sbomMetadataUtils;
  }

  public String filterAndSaveData(
      File scanFile,
      File tempScanFile,
      File scanDir,
      TelemetryData thirdPartyScanTelemetryData,
      String applicationId,
      String stageTypeId)
  {
    String scanRequestId = UUID.randomUUID().toString().replace("-", "");
    log.info("Processing third party content with scanRequestId: {}", scanRequestId);
    try {
      File filteredFile = FileUtils.createTempFile("tmp-", ".xml", scanDir);
      ThirdPartyScanContext scanContext = new ThirdPartyScanContext(scanRequestId,
          applicationId,
          scanFile,
          stageTypeId
          );
      try (GZIPInputStream gis = new GZIPInputStream(new FileInputStream(scanFile));
           OutputStream out = new FileOutputStream(filteredFile)) {

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
        compressScanFile(filteredFile, tempScanFile);
        log.info("Completed processing third party content in file {}", scanFile.getName());
        return scanRequestId;
      }
      finally {
        filteredFile.delete();
      }
    }
    catch (Exception e) {
      throw new IllegalArgumentException("Error reading/processing third party scan content from scan file", e);
    }
  }

  public void postHandle(String scanId, String scanRequestId) {
    thirdPartyScanDAO.getByScanRequestId(scanRequestId).forEach(thirdPartyScan -> {
      thirdPartyScan.setScanId(scanId);
      thirdPartyScanDAO.update(thirdPartyScan);
    });
  }

  private void processEvent(
      ThirdPartyScanContext scanContext,
      XmlPullParser parser,
      XMLEventWriter writer,
      TelemetryData thirdPartyScanTelemetryData)
  {
    try {
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
    catch (Exception e) {
      log.error("Error parsing third party scan file", e);
    }
  }

  private void processItemElement(
      ThirdPartyScanContext scanContext,
      XmlPullParser parser,
      XMLEventWriter writer,
      TelemetryData thirdPartyScanTelemetryData
  ) throws XMLStreamException, IOException, XmlPullParserException
  {
    String contentType = parser.getAttributeValue(null, "contentType");
    if (contentType != null && thirdPartyItemContentTypes.contains(contentType)) {
      if (thirdPartyScanTelemetryData != null) {
        // add the content type to telemetry data
        thirdPartyScanTelemetryData.getAttributes().put("content_type", contentType);
        telemetrySender.send(thirdPartyScanTelemetryData);
      }
      List<ProjectScanItem> moduleDependencies = new ArrayList<>();
      if (!contentType.equals(ItemContentType.IAC_FILE.name())) {
        Xpp3Dom itemElement = Xpp3Util.loadElement("item", parser);
        Xpp3Dom contentElement = itemElement.getChild("content");
        if (contentElement != null) {
          FilteredThirdPartyContent filteredThirdPartyContent =
              handleContent(itemElement, contentElement.getValue(), contentType, scanContext);
          writeFilteredInformation(writer, filteredThirdPartyContent.getContent());
          Optional.of(filteredThirdPartyContent.getModuleDependencies())
              .ifPresent(moduleDependencies::addAll);
          storeSbomFileIfApplicable(contentType, itemElement, contentElement, scanContext);
        }
        else {
          log.error("scan file {} contained a third party scan item {} without any content",
              scanContext.getScanFile().getName(), contentType);
        }
        writer.add(EVENT_FACTORY.createEndElement(new QName(parser.getName()), null));
        writeDependencyGraph(writer, moduleDependencies);
      }
    }
  }

  private FilteredThirdPartyContent handleContent(
      Xpp3Dom itemElement,
      String contentElement,
      String contentType,
      ThirdPartyScanContext scanContext)
  {
    String path = itemElement.getAttribute("path");
    String lastModified = itemElement.getAttribute("lastModified");
    String sha1 = itemElement.getAttribute("sha1");

    ThirdPartyFile thirdPartyFile = saveFile(path);
    scanContext.setThirdPartyFileId(thirdPartyFile.getId());
    saveScan(thirdPartyFile, scanContext.getScanRequestId());

    ItemContentType contentItemType = ItemContentType.valueOf(contentType);
    ThirdPartyScanResultHandler handler = createHandler(contentItemType);
    return handler.handleAndFilterContents(
        new ThirdPartyScanContent(path, contentItemType, lastModified, sha1, contentElement), thirdPartyFile);
  }

  private ThirdPartyFile saveFile(String path) {
    ThirdPartyFile thirdPartyFile = new ThirdPartyFile(path, new Date());
    thirdPartyFileDAO.insert(thirdPartyFile);
    return thirdPartyFile;
  }

  private void saveScan(ThirdPartyFile thirdPartyFile, String scanRequestId) {
    ThirdPartyScan thirdPartyScan = new ThirdPartyScan(thirdPartyFile.getId(), scanRequestId, new Date());
    thirdPartyScanDAO.insert(thirdPartyScan);
  }

  private void compressScanFile(File filteredFile, File scanFile) throws IOException {
    try (FileInputStream inputStream = new FileInputStream(filteredFile);
         GzipCompressorOutputStream outStream = new GzipCompressorOutputStream(new FileOutputStream(scanFile))) {
      IOUtils.copy(inputStream, outStream);
    }
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

  private void writeFilteredInformation(XMLEventWriter writer, String filteredInformation) throws XMLStreamException {
    writer.add(EVENT_FACTORY.createCharacters("\n"));
    QName name = new QName("content");
    writer.add(EVENT_FACTORY.createStartElement(name, null, null));
    XMLEvent contentEvent;
    contentEvent = EVENT_FACTORY.createCData(filteredInformation);
    writer.add(contentEvent);
    writer.add(EVENT_FACTORY.createEndElement(name, null));
    writer.add(EVENT_FACTORY.createCharacters("\n"));
  }

  private void writeDependencyGraph(final XMLEventWriter writer, final List<ProjectScanItem> moduleDependencies)
      throws XMLStreamException
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

  ThirdPartyScanResultHandler createHandler(ItemContentType contentItemType) {
    return thirdPartyResultHandlerFactory.newHandler(contentItemType);
  }

  private void storeSbomFileIfApplicable(
      String contentType,
      Xpp3Dom itemElement,
      Xpp3Dom contentElement,
      ThirdPartyScanContext scanContext)
  {
    if (canStoreSbom(contentType, scanContext)) {
      String sbomContent = storeSbom(scanContext, itemElement, contentElement);
      if (scanContext.getSbomFileName() != null && sbomContent != null) {
        saveThirdPartySbomMetadata(scanContext, sbomContent);
      }
    }
  }

  private boolean canStoreSbom(String contentType, ThirdPartyScanContext scanContext) {
    ItemContentType contentItemType = ItemContentType.valueOf(contentType);

    //for now we persist only the first sbom for sbom manager support when there are multiple sboms in the scan.
    return productLicense.hasFeature(LicensedFeature.SBOM_MANAGER)
        && !sbomMetadataUtils.hasMaxSbomLimitBeenReached()
        && isStageTypeSupported(scanContext)
        && (ItemContentType.SBOM.equals(contentItemType) || ItemContentType.SPDX.equals(contentItemType))
        && !scanContext.isSbomSavedForScan();
  }

  private boolean isStageTypeSupported(ThirdPartyScanContext scanContext) {
    return scanContext.getStageType().equalsIgnoreCase(ReleaseStageType.ID)
        && productLicense.getStageTypes().stream().anyMatch(
            stageType -> stageType.getId().equalsIgnoreCase( ReleaseStageType.ID));
  }

  private String storeSbom(
      ThirdPartyScanContext scanContext,
      Xpp3Dom itemElement,
      Xpp3Dom contentElement)
  {
    StringWriter stringWriter = new StringWriter();
    try {
      File sbomDir = insightWork.getSbomDir(scanContext.getApplicationId());
      Files.createDirectories(sbomDir.toPath().normalize());
      final Path tempFilePath =
          Files.createTempFile(sbomDir.toPath().normalize(), UUID.randomUUID().toString().replace("-", ""),
              "." + FilenameUtils.getExtension(itemElement.getAttribute("path")) + ".gz");
      try (InputStream sbomContent = IOUtils.toInputStream(contentElement.getValue(), Charset.defaultCharset());
           GzipCompressorOutputStream outputStream = new GzipCompressorOutputStream(
               Files.newOutputStream(tempFilePath))) {
        IOUtils.copy(sbomContent, outputStream);
        scanContext.markSbomSavedForScan();
        scanContext.setSbomFileName(tempFilePath.getFileName().toString());
        sbomContent.reset();
        IOUtils.copy(sbomContent, stringWriter, StandardCharsets.UTF_8);
        return stringWriter.toString();
      }
    }
    catch (IOException e) {
      log.error("there was an error while trying to store sbom file {}", scanContext.getScanFile().getName(), e);
    }
    return null;
  }

  private void saveThirdPartySbomMetadata(
      ThirdPartyScanContext scanContext, String sbomContent)
  {
    try {
      SbomDetectionResult sbomResult = sbomFileDetector.getSbomDetectionResult(sbomContent);
      if (sbomResult == null || sbomResult.summary == null) {
        throw new InvalidSbomException("SBOM metadata could not be identified.");
      }
      ThirdPartySbomMetadata thirdPartySbomMetadata = getSbomMetadataEntity(scanContext, sbomResult);
      thirdPartySbomMetadataDAO.insert(thirdPartySbomMetadata);
      AuditData.get().setSbomVersion(thirdPartySbomMetadata, SbomAction.CREATE);
    }
    catch (InvalidSbomException | UnsupportedSbomException ex) {
      log.debug("there was an error while trying to save sbom metadata", ex);
    }
  }

  @VisibleForTesting
  ThirdPartySbomMetadata getSbomMetadataEntity(
      ThirdPartyScanContext scanContext,
      SbomDetectionResult sbomDetectionResult)
  {
    ThirdPartySbomMetadata sbomMetadata = new ThirdPartySbomMetadata();
    sbomMetadata.setApplicationId(scanContext.getApplicationId());
    sbomMetadata.setThirdPartyFileId(scanContext.getThirdPartyFileId());
    sbomMetadata.setFilename(scanContext.getSbomFileName());
    sbomMetadata.setSbomVersion(getApplicationVersion(scanContext.getApplicationId(), sbomDetectionResult));
    sbomMetadata.setSerialNumber(sbomDetectionResult.summary.serialNumber);
    sbomMetadata.setSpec(sbomDetectionResult.summary.specification);
    sbomMetadata.setSpecFormat(sbomDetectionResult.summary.format);
    sbomMetadata.setSpecVersion(sbomDetectionResult.summary.version);
    sbomMetadata.setMetadataJson(sbomDetectionResult.summary.creationDetails);
    sbomMetadata.setCreatedAt(new Date());
    sbomMetadata.setStatus(SbomStatus.PENDING.name());
    return sbomMetadata;
  }

  private String getApplicationVersion(String applicationId, SbomDetectionResult sbomDetectionResult) {
    Optional<String> applicationVersion = Optional.ofNullable(sbomDetectionResult.summary.applicationVersion)
        .map(version -> version.isEmpty() ? null : version);

    return applicationVersion
        .map(version ->
            thirdPartySbomMetadataDAO.getByApplicationId(applicationId).stream()
                .filter(sbom -> version.equals(sbom.getSbomVersion()))
                .findFirst()
                .map(existingSbomApplicationVersion -> String.join("_",
                    existingSbomApplicationVersion.getSbomVersion(), dtFormatter.format(LocalDateTime.now())))
                .orElse(version))
        .orElseGet(() -> dtFormatter.format(LocalDateTime.now()));
  }
}
