/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.rm.scan;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import com.sonatype.clm.dto.model.ProprietaryConfig;
import com.sonatype.insight.brain.common.io.FileCleaner;
import com.sonatype.insight.brain.common.io.FileCleaner.FileDeletionException;
import com.sonatype.insight.scan.archive.Selector;
import com.sonatype.insight.scan.archive.Selector.Selection;
import com.sonatype.insight.scan.client.ClientScanRequest;
import com.sonatype.insight.scan.config.ScanPropertiesLoader;
import com.sonatype.insight.scan.file.Config;
import com.sonatype.insight.scan.file.FileScanRequest;
import com.sonatype.insight.scan.file.FileScanner;
import com.sonatype.insight.scan.file.ScanSession;
import com.sonatype.insight.scan.hash.SHA1;
import com.sonatype.insight.scan.model.Repository;
import com.sonatype.insight.scan.model.Scan;
import com.sonatype.insight.scan.model.ScanConfiguration;
import com.sonatype.insight.scan.model.ScanSummary;
import com.sonatype.insight.scan.model.io.ScanWriter;
import com.sonatype.insight.scan.util.HashUtils;

import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.ReaderFactory;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.util.xml.PrettyPrintXMLWriter;
import org.codehaus.plexus.util.xml.pull.MXParser;
import org.codehaus.plexus.util.xml.pull.XmlPullParser;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ScanFactory
{
  private static final Logger log = LoggerFactory.getLogger(ScanFactory.class);

  private static final String CONFIGURATION_RESOURCE = ScanFactory.class.getName().replace('.', '/')
      .replace(ScanFactory.class.getSimpleName(), "configuration.properties");

  public File forConfiguration(com.sonatype.insight.rm.scan.ScanConfiguration config) throws IOException {
    if (config == null) {
      throw new IllegalArgumentException("scan configuration missing");
    }

    File scanFile = File.createTempFile("sonatype-clm-scan-", ".xml.gz", config.getWorkDir());
    try (Writer writer = new OutputStreamWriter(
        new GZIPOutputStream(new BufferedOutputStream(new FileOutputStream(scanFile), 32 * 1024)), "UTF-8")) {
      scan(config, writer);
    }
    catch (RuntimeException | IOException e) {
      try {
        new FileCleaner().delete(scanFile);
      }
      catch (FileDeletionException fde) {
        log.error("Unable to delete scanFile: {}", scanFile, fde);
      }
      throw e;
    }
    return scanFile;
  }

  private void scan(com.sonatype.insight.rm.scan.ScanConfiguration config, Writer writer) throws IOException {
    Scan scan = new Scan();
    scan.setConfiguration(getConfiguration(config));
    ScanSummary summary = scan.getSummary();
    summary.setStartTime();

    Bindings.clientScanner().scan(new ClientScanRequest(scan));

    Set<String> moduleIds = new HashSet<>();
    Set<SHA1> componentHashes = new HashSet<>();
    Set<SHA1> scannedHashes = new HashSet<>();

    List<RepositoryItem> componentItems = config.getComponentItems();
    for (int i = componentItems.size() - 1; i >= 0; i--) {
      RepositoryItem item = componentItems.get(i);
      moduleIds.add(item.getCoordinates().getModuleId());
      String sha1 = item.getSha1();
      if (sha1 == null || sha1.isEmpty()) {
        try (InputStream is = item.newInputStream()) {
          sha1 = HashUtils.hash(is, HashUtils.SHA1);
        }
        componentItems.set(i, new HashedRepositoryItem(item, sha1));
      }
      componentHashes.add(normalizeSha1(sha1));
    }

    ScanWriter scanWriter = Bindings.scanWriterFactory().newWriter(writer);
    PrettyPrintXMLWriter xmlWriter = new PrettyPrintXMLWriter(writer);

    scanWriter.openScan(scan);
    scanWriter.writeRepository(new Repository(null, config.getRepositoryId(), config.getRepositoryName(), config
        .getRepositoryFormat(), null));
    scanWriter.writeConfiguration(scan.getConfiguration());

    Selector proprietarySelector = new Config(scan.getConfiguration()).hiddenResourceNamePathSelector;

    int archives = 0;
    int files = 0;
    int classFiles = 0;
    for (RepositoryItem item : config.getScanItems()) {
      if (!moduleIds.contains(item.getCoordinates().getModuleId())) {
        // no scan-worthy component exists for that module any more, ignore its scan (especially dependencies)
        continue;
      }

      try (Reader reader = ReaderFactory.newXmlReader(new GZIPInputStream(item.newInputStream()))) {
        MXParser parser = new MXParser();
        parser.setInput(reader);
        for (int event = parser.getEventType(); event != XmlPullParser.END_DOCUMENT; event = parser.next()) {
          if (event == XmlPullParser.START_TAG) {
            String tag = parser.getName();
            if (parser.getDepth() == 1) {
              if (!"scan".equals(tag)) {
                throw new XmlPullParserException("Unexpected root tag: " + tag, parser, null);
              }
              String version = parser.getAttributeValue(null, "version");
              if (!scan.getVersion().equals(version)) {
                log.warn("Unexpected file format in " + item.getPath() + ", scan might be inaccurate"
                    + ", please ensure the employed Nexus IQ client tools are compatible");
              }
            }
            else if (parser.getDepth() == 2 && "configuration".equals(tag)) {
              parser.skipSubTree();
            }
            else if (parser.getDepth() == 2 && "summary".equals(tag)) {
              parser.skipSubTree();
            }
            else if (parser.getDepth() == 3 && "dir".equals(tag)
                && !Boolean.parseBoolean(parser.getAttributeValue(null, "dependency"))
                && isNotPresentInRepo(parser.getAttributeValue(null, "sha1"), componentHashes)) {
              parser.skipSubTree();
            }
            else {
              xmlWriter.startElement(tag);

              boolean filterPath = "item".equals(tag);
              for (int i = 0, n = parser.getAttributeCount(); i < n; i++) {
                String name = parser.getAttributeName(i);
                String value = parser.getAttributeValue(i);
                if (filterPath && "path".equals(name) && value != null
                    && proprietarySelector.isSelected(value) != Selection.SELECTED) {
                  xmlWriter.addAttribute("noPathReason", proprietarySelector.getName());
                }
                else {
                  xmlWriter.addAttribute(name, value);
                }
              }

              if ("dir".equals(tag)) {
                archives++;
                if (!Boolean.parseBoolean(parser.getAttributeValue(null, "dependency"))) {
                  scannedHashes.add(normalizeSha1(parser.getAttributeValue(null, "sha1")));
                }
              }
              else if ("item".equals(tag)) {
                files++;
                String path = parser.getAttributeValue(null, "path");
                if (path != null && path.endsWith(".class")) {
                  classFiles++;
                }
              }
            }
          }
          else if (event == XmlPullParser.END_TAG) {
            if (parser.getDepth() > 1) {
              xmlWriter.endElement();
            }
          }
          else if (event == XmlPullParser.TEXT) {
            xmlWriter.writeText(parser.getText());
          }
        }
      }
      catch (XmlPullParserException e) {
        throw new IOException("Could not read scan file " + item.getPath(), e);
      }
    }
    summary.setArchives(archives);
    summary.setFiles(files);
    summary.setClassFiles(classFiles);

    FileScanner fileScanner = Bindings.fileScanner();
    ScanSession scanSession = new ScanSession(scan, scanWriter);
    for (RepositoryItem item : componentItems) {
      if (scannedHashes.contains(normalizeSha1(item.getSha1()))) {
        continue;
      }
      File file = item.getFile();
      File tmp = null;
      try {
        if (file == null) {
          // NOTE: We need to retain the proper file extension for TrueZIP to recognize the archive type
          String ext = new File(item.getPath()).getName();
          ext = ext.substring(ext.indexOf('.') + 1);
          file = tmp = File.createTempFile("sonatype-clm-file-", "." + ext, config.getWorkDir());
          try (InputStream is = item.newInputStream(); FileOutputStream fos = new FileOutputStream(file)) {
            IOUtil.copy(is, fos);
          }
        }
        FileScanRequest scanRequest = new FileScanRequest(scanSession);
        scanRequest.addFile(file, trimLeadingSlash(item.getPath()), item.getCoordinates().getId());
        fileScanner.scan(scanRequest);
      }
      finally {
        if (tmp != null) {
          try {
            new FileCleaner().delete(tmp);
          }
          catch (FileDeletionException fde) {
            log.error("Unable to delete temporary file: {}", tmp, fde);
          }
        }
      }
    }

    summary.setEndTime();
    scanWriter.writeSummary(summary);
    scanWriter.closeScan();
  }

  private boolean isNotPresentInRepo(String sha1, Set<SHA1> componentHashes) {
    return sha1 != null && !componentHashes.contains(normalizeSha1(sha1));
  }

  private SHA1 normalizeSha1(String sha1) {
    return (sha1 != null) ? SHA1.fromHexString(sha1) : null;
  }

  private String trimLeadingSlash(String path) {
    return (path != null && path.startsWith("/")) ? path.substring(1) : path;
  }

  private ScanConfiguration getConfiguration(com.sonatype.insight.rm.scan.ScanConfiguration config) throws IOException {
    final Properties properties = new Properties();
    if (config.getProprietaryConfig() != null) {
      ProprietaryConfig proprietaryConfig = config.getProprietaryConfig();
      properties.put("proprietaryPackages", StringUtils.join(proprietaryConfig.getPackages().iterator(), ","));
      properties.put("proprietaryRegexes", StringUtils.join(proprietaryConfig.getRegexes().iterator(), ":::"));
    }
    properties.putAll(config.getScanOptions());
    final ScanPropertiesLoader loader = new ScanPropertiesLoader();
    loader.resolveAliases(properties);
    loader.loadDefaults(properties, CONFIGURATION_RESOURCE);
    return new ScanConfiguration(properties);
  }

  private static class HashedRepositoryItem
      extends RepositoryItem
  {
    private final RepositoryItem delegate;

    private final String sha1;

    public HashedRepositoryItem(RepositoryItem delegate, String sha1) {
      this.delegate = delegate;
      this.sha1 = sha1;
    }

    @Override
    public String getSha1() {
      return sha1;
    }

    @Override
    public File getFile() {
      return delegate.getFile();
    }

    @Override
    public String getPath() {
      return delegate.getPath();
    }

    @Override
    public Coords getCoordinates() {
      return delegate.getCoordinates();
    }

    @Override
    public InputStream newInputStream() throws IOException {
      return delegate.newInputStream();
    }
  }
}
