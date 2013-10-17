/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.report;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.StreamingOutput;

import com.sonatype.insight.client.utils.UrlUtils;
import com.sonatype.insight.json.store.JsonUtils;

import com.fasterxml.jackson.databind.node.ObjectNode;
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

final class Pdf
{
  private static final Logger log = LoggerFactory.getLogger(Pdf.class);

  private static IReportEngine reportEngine;

  private static File getPdfFile(final File reportFile) {
    return new File(reportFile.getParentFile(), "report.pdf");
  }

  public static void delete(final File reportFile) {
    final File pdfFile = getPdfFile(reportFile);
    log.debug("Deleting report PDF {}", pdfFile);
    if (!pdfFile.delete() && pdfFile.exists()) {
      log.warn("Could not delete obsolete report PDF {}", pdfFile);
    }
  }

  public static void generate(final File reportFile, final File cacheDir, final boolean sample,
      final String projectName, final int buildNumber, final ResponseBuilder response) throws IOException
  {
    final File pdfFile = getPdfFile(reportFile);

    if (!pdfFile.isFile() || pdfFile.length() == 0) {
      final File templateDir = setupTemplateDir(reportFile, cacheDir, projectName, buildNumber);
      try {
        generate(pdfFile, templateDir, sample);
      }
      finally {
        FileUtils.deleteDirectory(templateDir);
      }
    }

    final Date now = new Date();

    response.lastModified(now);
    response.expires(now);
    response.header(HttpHeaders.CONTENT_LENGTH, pdfFile.length());
    response.type("application/pdf");

    final String timestamp = new SimpleDateFormat("yyyyMMdd-HHmmss").format(now);
    final String filename = projectName + "-" + buildNumber + "-" + timestamp + ".pdf";
    response.header("Content-Disposition", "attachment; filename=" + UrlUtils.encodeUrlComponent(filename));

    response.entity(new StreamingOutput()
    {
      @Override
      public void write(final OutputStream os) throws IOException {
        final FileInputStream fis = new FileInputStream(pdfFile);
        try {
          IOUtil.copy(fis, os);
        }
        finally {
          fis.close();
        }
      }
    });
  }

  private static File setupTemplateDir(final File reportFile, final File cacheDir, final String projectName,
      final int buildNumber) throws IOException
  {
    final File templateDir = new File(reportFile.getParentFile(), "pdf");

    final ZipFile archive = new ZipFile(reportFile);
    try {
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
            final FileOutputStream fos = new FileOutputStream(extractedFile);
            try {
              IOUtil.copy(archive.getInputStream(entry), fos);
            }
            finally {
              fos.close();
            }
          }
          if ("summary.json".equals(name)) {
            final ObjectNode summary = JsonUtils.read(extractedFile);
            summary.put("projectName", projectName);
            summary.put("buildNumber", Integer.toString(buildNumber));
            JsonUtils.write(extractedFile, summary);
          }
        }
      }
      File policyAlerts = new File(cacheDir, "policyalerts.json");
      if(policyAlerts.exists()){
        FileUtils.copyFile(policyAlerts, new File(templateDir, policyAlerts.getName()));
      }
    }
    finally {
      archive.close();
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

  private static File generate(final File pdfFile, final File templateDir, final boolean sample) throws IOException {
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
        task.setParameterValue("paid", false);
        task.setParameterValue("freemium", sample);

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
