/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.report;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import com.sonatype.insight.brain.common.io.FileCleaner;
import com.sonatype.insight.brain.organization.ContactDTO;
import com.sonatype.insight.brain.policy.evaluator.ScanPolicyEvaluator;
import com.sonatype.insight.json.store.JsonUtils;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Strings;
import org.apache.batik.util.XMLResourceDescriptor;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.IOUtil;
import org.eclipse.birt.core.exception.BirtException;
import org.eclipse.birt.core.framework.Platform;
import org.eclipse.birt.core.framework.PlatformConfig;
import org.eclipse.birt.report.engine.api.EngineConfig;
import org.eclipse.birt.report.engine.api.IPDFRenderOption;
import org.eclipse.birt.report.engine.api.IRenderOption;
import org.eclipse.birt.report.engine.api.IReportEngine;
import org.eclipse.birt.report.engine.api.IReportEngineFactory;
import org.eclipse.birt.report.engine.api.IReportRunnable;
import org.eclipse.birt.report.engine.api.IRunAndRenderTask;
import org.eclipse.birt.report.engine.api.RenderOption;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.XMLReaderFactory;

public final class Pdf
{
  private static final Logger log = LoggerFactory.getLogger(Pdf.class);

  private static IReportEngine reportEngine;

  public static File getPdfFile(final File reportFile) {
    return new File(reportFile.getParentFile(), "report.pdf");
  }

  public static File generate(final File reportFile,
                              final File cacheDir,
                              final String applicationName,
                              final String stageName,
                              final ContactDTO contact) throws IOException
  {
    final File pdfFile = getPdfFile(reportFile);

    if (!pdfFile.isFile() || pdfFile.length() == 0) {
      final File templateDir = setupTemplateDir(reportFile, cacheDir, applicationName, stageName, contact);
      try {
        generate(pdfFile, templateDir);
        new FileCleaner().delete(templateDir);
      }
      catch (Exception e) {
        if (!pdfFile.delete() && pdfFile.exists()) {
          log.error("Could not delete broken PDF {}", pdfFile);
        }
        try {
          new FileCleaner().delete(templateDir);
        }
        catch (Exception suppressed) {
          e.addSuppressed(suppressed);
        }
        throw e;
      }
    }
    return pdfFile;
  }

  private static File setupTemplateDir(final File reportFile,
                                       final File cacheDir,
                                       final String applicationName,
                                       final String stageName,
                                       final ContactDTO contact) throws IOException
  {
    final File templateDir = new File(reportFile.getParentFile(), "pdf");

    try (final ZipFile archive = new ZipFile(reportFile)) {
      for (final Enumeration<? extends ZipEntry> en = archive.entries(); en.hasMoreElements();) {
        final ZipEntry entry = en.nextElement();
        if (entry.isDirectory()) {
          continue;
        }
        final String name = entry.getName();
        if (isPdfResource(name)) {
          final File extractedFile = new File(templateDir, name);
          final File cacheFile = new File(cacheDir, name);
          if (cacheFile.isFile()) {
            FileUtils.copyFile(cacheFile, extractedFile);
          }
          else {
            extractedFile.getParentFile().mkdirs();
            try (final FileOutputStream fos = new FileOutputStream(extractedFile)) {
              IOUtil.copy(archive.getInputStream(entry), fos);
            }
          }
          if ("summary.json".equals(name)) {
            final ObjectNode summary = JsonUtils.read(extractedFile);
            fillSummary(summary, applicationName, stageName, contact);
            JsonUtils.write(extractedFile, summary);
          }
        }
      }
      File policyAlerts = new File(cacheDir, ScanPolicyEvaluator.POLICY_ALERTS_FILENAME);
      if (policyAlerts.exists()) {
        FileUtils.copyFile(policyAlerts, new File(templateDir, policyAlerts.getName()));
      }
    }

    return templateDir;
  }

  private static boolean isPdfResource(final String pathname) {
    if (pathname.startsWith("public/")) {
      return true;
    }
    final String ext = FileUtils.getExtension(pathname);
    if ("json".equals(ext) || ext.startsWith("rpt")) {
      return true;
    }
    return false;
  }

  static void fillSummary(final ObjectNode summary,
                          final String applicationName,
                          final String stageName,
                          final ContactDTO contact)
  {
    summary.put("applicationName", applicationName);
    summary.put("stageName", stageName);
    // PDF templates before 1.12.1 (Sept/Oct 2014) had projectName and buildNumber, so for backwards compat...
    summary.put("projectName", applicationName);
    summary.put("buildNumber", stageName);
    if (contact != null) {
      if (!Strings.isNullOrEmpty(contact.getEmail())) {
        summary.put("applicationContactEmail", contact.getEmail());
      }

      if (!Strings.isNullOrEmpty(contact.getDisplayName())) {
        summary.put("applicationContactName", contact.getDisplayName());
      }
    }
  }

  private static File generate(final File pdfFile, final File templateDir) throws IOException {
    init();

    log.debug("Generating report PDF {}", pdfFile);
    long millis = System.currentTimeMillis();

    try {
      final File designFile = new File(templateDir, "detail.rptdesign");
      final IReportRunnable runnable = reportEngine.openReportDesign(designFile.getAbsolutePath());

      final IRunAndRenderTask task = reportEngine.createRunAndRenderTask(runnable);
      try {
        final IRenderOption options = new RenderOption();
        options.setOutputFormat("PDF");
        options.setOutputFileName(pdfFile.getAbsolutePath());
        options.setOption(IPDFRenderOption.PDF_TEXT_WRAPPING, Boolean.TRUE);
        options.setOption(IPDFRenderOption.PDF_HYPHENATION, Boolean.TRUE);

        task.setRenderOption(options);
        task.setLocale(Locale.ENGLISH);
        task.setParameterValue("reportDir", templateDir.getAbsolutePath());

        task.run();

        @SuppressWarnings("unchecked")
        final List<Throwable> errors = task.getErrors();
        if (errors != null && !errors.isEmpty()) {
          log.error("Got {} errors while generating report {}", errors.size(), pdfFile);
          for (final Throwable error : errors) {
            log.error(error.getMessage(), error);
          }
          throw new IOException("Could not generate report " + pdfFile, errors.get(0));
        }
        if (pdfFile.length() <= 0) {
          throw new IOException("Could not generate report " + pdfFile);
        }
      }
      finally {
        task.close();
      }
    }
    catch (final BirtException e) {
      throw new IOException(e.getMessage(), e);
    }

    millis = System.currentTimeMillis() - millis;
    log.debug("Generated report PDF {} in {} ms", pdfFile, millis);

    return pdfFile;
  }

  private static synchronized void init() throws IOException {
    if (reportEngine == null) {
      log.debug("Initializing BIRT engine");
      configureBatikToUseBundledSaxParser();
      try {
        final PlatformConfig platformConfig = new PlatformConfig();
        Platform.startup(platformConfig);

        final IReportEngineFactory reportEngineFactory = (IReportEngineFactory) Platform
            .createFactoryObject(IReportEngineFactory.EXTENSION_REPORT_ENGINE_FACTORY);

        final EngineConfig engineConfig = new EngineConfig();
        reportEngine = reportEngineFactory.createReportEngine(engineConfig);
      }
      catch (final BirtException e) {
        throw new IOException(e.getMessage(), e);
      }
    }
  }

  private static void configureBatikToUseBundledSaxParser() {
    // Batik defaults to org.apache.xerces.parsers.SAXParser from xercesImpl whose latest release in Central (2.11.0)
    // has vulnerabilities so we point to the fixed copy bundled with the JRE.
    String saxParserClassName;
    try {
      saxParserClassName = XMLReaderFactory.createXMLReader().getClass().getName();
    }
    catch (SAXException e) {
      log.warn("Could not retrieve SAXParser classname.  Setting manually.", e);
      saxParserClassName = "com.sun.org.apache.xerces.internal.parsers.SAXParser";
    }
    log.debug("Using SAXParser class {}", saxParserClassName);
    XMLResourceDescriptor.setXMLParserClassName(saxParserClassName);
  }

  /*
   * For embedded test purposes...
   */
  static synchronized void destroy() {
    if (reportEngine != null) {
      reportEngine.destroy();
      reportEngine = null;
    }
  }
}
