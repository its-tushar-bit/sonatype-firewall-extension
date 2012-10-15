/**
 * Copyright (c) 2011-2012 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/insight/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.ci.client;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import javax.servlet.http.HttpServletResponse;

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

import com.fasterxml.jackson.databind.node.ObjectNode;

final class Pdf
{

    private static IReportEngine reportEngine;

    private static File getPdfFile( final File reportFile )
    {
        return new File( reportFile.getParentFile(), "report.pdf" );
    }

    public static void delete( final Logger log, final File reportFile )
    {
        final File pdfFile = getPdfFile( reportFile );
        log.debug( "Deleting report PDF {}", pdfFile );
        if ( !pdfFile.delete() && pdfFile.exists() )
        {
            log.warn( "Could not delete obsolete report PDF {}", pdfFile );
        }
    }

    public static void generate( final Logger log, final File reportFile, final File cacheDir, final boolean sample,
                                 final String projectName, final int buildNumber, final HttpServletResponse rsp )
        throws IOException
    {
        final File pdfFile = getPdfFile( reportFile );

        if ( !pdfFile.isFile() )
        {
            ClassLoader tccl = Thread.currentThread().getContextClassLoader();
            final File templateDir = setupTemplateDir( reportFile, cacheDir, projectName, buildNumber );
            try
            {
                /*
                 * At least Hudson 2.2.0 doesn't set the plugin class loader as TCCL automatically so we do or
                 * BIRT/Batik would fail to load its Xerces parser.
                 */
                Thread.currentThread().setContextClassLoader( Pdf.class.getClassLoader() );

                generate( log, pdfFile, templateDir, sample );

            }
            finally
            {
                Thread.currentThread().setContextClassLoader( tccl );
                FileUtils.deleteDirectory( templateDir );
            }
        }

        final FileInputStream fis = new FileInputStream( pdfFile );
        try
        {
            long now = System.currentTimeMillis();
            String timestamp = new SimpleDateFormat( "yyyyMMdd-HHmmss" ).format( new Date() );
            String filename = projectName + "-" + buildNumber + "-" + timestamp + ".pdf";
            rsp.setDateHeader( "Last-Modified", now );
            rsp.setDateHeader( "Expires", now );
            rsp.setContentLength( (int) pdfFile.length() );
            rsp.setContentType( "application/pdf" );
            rsp.setHeader( "Content-Disposition", "attachment; filename=" + filename );
            IOUtil.copy( fis, rsp.getOutputStream() );
        }
        finally
        {
            fis.close();
        }
    }

    private static File setupTemplateDir( final File reportFile, final File cacheDir, final String projectName,
                                          final int buildNumber )
        throws IOException
    {
        final File templateDir = new File( reportFile.getParentFile(), "pdf" );

        final ZipFile archive = new ZipFile( reportFile );
        try
        {
            for ( final Enumeration<? extends ZipEntry> en = archive.entries(); en.hasMoreElements(); )
            {
                final ZipEntry entry = en.nextElement();
                if ( entry.isDirectory() )
                {
                    continue;
                }
                final String name = entry.getName();
                if ( isPdfResource( name ) )
                {
                    final File extractedFile = new File( templateDir, name );
                    final File cacheFile = new File( cacheDir, name );
                    if ( cacheFile.isFile() )
                    {
                        FileUtils.copyFile( cacheFile, extractedFile );
                    }
                    else
                    {
                        extractedFile.getParentFile().mkdirs();
                        FileOutputStream fos = new FileOutputStream( extractedFile );
                        try
                        {
                            IOUtil.copy( archive.getInputStream( entry ), fos );
                        }
                        finally
                        {
                            fos.close();
                        }
                    }
                    if ( "summary.json".equals( name ) )
                    {
                        ObjectNode summary = DataStore.loadData( extractedFile );
                        summary.put( "projectName", projectName );
                        summary.put( "buildNumber", Integer.toString( buildNumber ) );
                        DataStore.saveData( extractedFile, summary );
                    }
                }
            }
        }
        finally
        {
            archive.close();
        }

        return templateDir;
    }

    private static boolean isPdfResource( String pathname )
    {
        if ( pathname.startsWith( "public/" ) )
        {
            return true;
        }
        String ext = FileUtils.getExtension( pathname );
        if ( "json".equals( ext ) || ext.startsWith( "rpt" ) )
        {
            return true;
        }
        return false;
    }

    private static File generate( final Logger log, final File pdfFile, final File templateDir, final boolean sample )
        throws IOException
    {
        init( log );

        log.debug( "Generating report PDF {}", pdfFile );
        long millis = System.currentTimeMillis();

        try
        {
            final File designFile = new File( templateDir, "detail.rptdesign" );
            IReportRunnable runnable = reportEngine.openReportDesign( designFile.getAbsolutePath() );

            IRunAndRenderTask task = reportEngine.createRunAndRenderTask( runnable );
            try
            {
                IRenderOption options = new RenderOption();
                options.setOutputFormat( "PDF" );
                options.setOutputFileName( pdfFile.getAbsolutePath() );
                options.setOption( IPDFRenderOption.PDF_TEXT_WRAPPING, Boolean.TRUE );
                options.setOption( IPDFRenderOption.PDF_HYPHENATION, Boolean.TRUE );

                task.setRenderOption( options );
                task.setLocale( Locale.ENGLISH );
                task.setParameterValue( "reportDir", templateDir.getAbsolutePath() );
                task.setParameterValue( "paid", false );
                task.setParameterValue( "freemium", sample );

                task.run();

                @SuppressWarnings( "unchecked" )
                List<Throwable> errors = task.getErrors();
                if ( errors != null && !errors.isEmpty() )
                {
                    log.error( "Got {} errors while generating report {}", errors.size(), pdfFile );
                    for ( Throwable error : errors )
                    {
                        log.error( error.getMessage(), error );
                    }
                    throw new IOException( "Could not generate report " + pdfFile, errors.get( 0 ) );
                }
                if ( pdfFile.length() <= 0 )
                {
                    throw new IOException( "Could not generate report " + pdfFile );
                }
            }
            finally
            {
                task.close();
            }
        }
        catch ( BirtException e )
        {
            throw new IOException( e.getMessage(), e );
        }

        millis = System.currentTimeMillis() - millis;
        log.debug( "Generated report PDF {} in {} ms", pdfFile, millis );

        return pdfFile;
    }

    private static synchronized void init( final Logger log )
        throws IOException
    {
        if ( reportEngine == null )
        {
            log.debug( "Initializing BIRT engine" );
            try
            {
                PlatformConfig platformConfig = new PlatformConfig();
                Platform.startup( platformConfig );

                IReportEngineFactory reportEngineFactory =
                    (IReportEngineFactory) Platform.createFactoryObject( IReportEngineFactory.EXTENSION_REPORT_ENGINE_FACTORY );

                EngineConfig engineConfig = new EngineConfig();
                reportEngine = reportEngineFactory.createReportEngine( engineConfig );
            }
            catch ( BirtException e )
            {
                throw new IOException( e.getMessage(), e );
            }
        }
    }

}
