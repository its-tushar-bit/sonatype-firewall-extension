/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.scan.cli;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;

import com.sonatype.insight.scan.client.ClientScanRequest;
import com.sonatype.insight.scan.client.ClientScanner;
import com.sonatype.insight.scan.config.ScanPropertiesLoader;
import com.sonatype.insight.scan.file.FileScanRequest;
import com.sonatype.insight.scan.file.FileScanner;
import com.sonatype.insight.scan.file.ModuleScanRequest;
import com.sonatype.insight.scan.file.ScanSession;
import com.sonatype.insight.scan.model.ClientScanResult;
import com.sonatype.insight.scan.model.Scan;
import com.sonatype.insight.scan.model.ScanConfiguration;
import com.sonatype.insight.scan.model.ScanMetadata;
import com.sonatype.insight.scan.model.io.ScanWriter;
import com.sonatype.insight.scan.model.io.ScanWriterFactory;
import com.sonatype.insight.scan.module.model.Dependency;
import com.sonatype.insight.scan.module.model.Module;
import com.sonatype.insight.scan.module.model.io.ModuleIoManager;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
public class Scanner
{
  private static final Logger log = LoggerFactory.getLogger(Scanner.class);

  private static final String MODULE_XML_FORMAT = new Module().getFormatVersion();

  private final ScanPropertiesLoader configLoader;

  private final ClientScanner clientScanner;

  private final FileScanner fileScanner;

  private final ScanWriterFactory writerFactory;

  @Inject
  public Scanner(ScanPropertiesLoader configLoader,
                 ClientScanner clientScanner,
                 FileScanner fileScanner,
                 ScanWriterFactory writerFactory)
  {
    this.configLoader = configLoader;
    this.clientScanner = clientScanner;
    this.fileScanner = fileScanner;
    this.writerFactory = writerFactory;
  }

  public ClientScanResult scan(File scanFile, List<File> targets, Properties config) throws IOException {
    return this.scan(scanFile, targets, config, null);
  }

  public ClientScanResult scan(
      final File scanFile,
      final List<File> targets,
      final Properties config,
      final ScanMetadata metadata) throws IOException
  {
    return this.scan(scanFile, null, targets, config, metadata, null);
  }

  public ClientScanResult scan(
      final File scanFile,
      final File baseDir,
      final List<File> targets,
      final Properties config,
      final ScanMetadata metadata,
      final Set<String> licensedFeatures) throws IOException
  {
    return scan(scanFile, baseDir, targets, Collections.emptyList(), config, metadata, licensedFeatures);
  }

  public ClientScanResult scan(
      final File scanFile,
      final File baseDir,
      final List<File> targets,
      final List<File> moduleIndices,
      final Properties config,
      final ScanMetadata metadata,
      final Set<String> licensedFeatures) throws IOException
  {
    log.info("Starting scan...");

    Scan scan = new Scan();
    scan.setConfiguration(new ScanConfiguration(getScanConfigProps(config)));
    scan.setMetadata(metadata);
    try (ScanWriter writer = writerFactory.newWriter(scanFile)) {
      writer.openScan(scan);
      writer.writeConfiguration(scan.getConfiguration());
      writer.writeMetadata(metadata);
      scan.getSummary().setStartTime();
      ScanSession scanSession = new ScanSession(scan, writer);
      scanSession.setLicensedFeatures(licensedFeatures);
      clientScanner.scan(new ClientScanRequest(scan));
      FileScanRequest fileScanRequest = new FileScanRequest(scanSession, targets);
      fileScanRequest.setBasedir(baseDir);
      fileScanner.scan(fileScanRequest);
      scanModules(moduleIndices, scanSession, baseDir, fileScanner);
      scan.getSummary().setEndTime();
      writer.writeSummary(scan.getSummary());
      writer.closeScan();
    }
    log.info("Fingerprinting completed in {} seconds for {} archives, {} total files",
        scan.getSummary().getElapsedSeconds(), scan.getSummary().getArchives(), scan.getSummary().getFiles());
    return new ClientScanResult(scanFile, scan.hasThirdPartyScanContent());
  }

  // Visible for testing
  void scanModules(
      List<File> moduleIndices,
      ScanSession scanSession,
      File baseDirectory,
      FileScanner fileScanner) throws IOException
  {
    List<Module> modules = getModules(moduleIndices);

    for (Module module : modules) {
      ModuleScanRequest scanRequest = createModuleScanRequest(scanSession);
      if (baseDirectory != null) {
        scanRequest.setBasedir(baseDirectory);
      }
      scanRequest.setModule(module.getId(), module.getIdKind(), module.getPathname());

      module.getConsumedArtifacts().forEach(artifact -> {
        if (artifact.isMonitored()) {
          String id = StringUtils.defaultIfBlank(artifact.getId(), "unknown:unknown:unknown");
          File file = new File(artifact.getPathname());
          scanRequest.addConsumedFile(file, id);
        }
      });

      Map<String, List<String>> childrenByDependencyId = new HashMap<>();
      for (Dependency dependency : module.getDependencies()) {
        List<String> childIds = getChildDependencyIds(scanRequest, dependency, childrenByDependencyId);
        List<String> existingChildIds = childrenByDependencyId.get(dependency.getId());
        if (existingChildIds == null || existingChildIds.size() < childIds.size()) {
          scanRequest.addDependency(dependency.getId(), dependency.isDirect(), childIds);
          childrenByDependencyId.put(dependency.getId(), childIds);
        }
      }
      fileScanner.scan(scanRequest);
    }
  }

  // Visible for testing
  ModuleScanRequest createModuleScanRequest(ScanSession scanSession) {
    return new ModuleScanRequest(scanSession);
  }

  private List<Module> getModules(List<File> moduleIndexes) throws IOException {
    List<Module> modules = new ArrayList<>();
    ModuleIoManager moduleIoManager = new ModuleIoManager();
    Set<String> moduleIds = new HashSet<>();
    for (File moduleIndex : moduleIndexes) {
      Module module = moduleIoManager.readModule(moduleIndex);
      if (!MODULE_XML_FORMAT.equals(module.getFormatVersion())) {
        log.warn("Unexpected file format in {}, scan might be inaccurate," +
            " please ensure the employed IQ client tools are compatible", moduleIndex);
      }
      modules.add(module);
      if (StringUtils.isNotBlank(module.getId())) {
        moduleIds.add(module.getId());
      }
    }
    List<Integer> rootIndexes = new ArrayList<>();
    for (int i = 0; i < modules.size(); i++) {
      Module module = modules.get(i);
      if (!moduleIds.contains(module.getParentId())) {
        log.debug(
            "Module {} has parent {} which matches no other modules in this project and so may be the root module.",
            module.getId(), module.getParentId());
        rootIndexes.add(i);
      }
    }
    if (rootIndexes.size() == 1) {
      Collections.swap(modules, 0, rootIndexes.get(0));
    }
    return modules;
  }

  private List<String> getChildDependencyIds(
      ModuleScanRequest scanRequest,
      Dependency dependency,
      Map<String, List<String>> childrenByDependencyId)
  {
    List<String> ids = new ArrayList<>();
    for (Dependency child : dependency.getDependencies()) {
      ids.add(child.getId());
      List<String> childIds = getChildDependencyIds(scanRequest, child, childrenByDependencyId);
      List<String> existingChildIds = childrenByDependencyId.get(child.getId());
      if (existingChildIds == null || existingChildIds.size() < childIds.size()) {
        scanRequest.addDependency(child.getId(), false, childIds);
        childrenByDependencyId.put(child.getId(), childIds);
      }
    }
    return ids;
  }

  private Properties getScanConfigProps(Properties properties) throws IOException {
    Properties props = new Properties();
    props.putAll(properties);
    configLoader.loadDefaults(props, "configuration.properties");
    configLoader.resolveAliases(props);
    return props;
  }
}
