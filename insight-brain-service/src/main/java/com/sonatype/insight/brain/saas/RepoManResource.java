/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.saas;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sonatype.clm.dto.model.ScanReceipt;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.report.ReportResource;
import com.sonatype.insight.brain.service.InsightProxy;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.scan.upload.BOMCheckScanUploadResult;
import com.sonatype.insight.scan.upload.DefaultScanUploader;
import com.sonatype.insight.scan.upload.RepoManScanUploadRequest;
import com.sonatype.insight.scan.upload.ScanUploader;

@Path( RepoManResource.SERVICE_PATH )
public class RepoManResource
{
    public static final String SERVICE_PATH = "rest/rm";

    private static final String SCAN_EXT = ".xml.gz";

    private static final Logger log = LoggerFactory.getLogger( RepoManResource.class );

    private final ScanUploader uploader = new DefaultScanUploader( log, false );

    @Context
    private InsightWork work;

    @Context
    private InsightProxy proxy;

    private ApplicationDAO applicationDAO = new ApplicationDAO();

    @PUT
    @Path( "scan/{applicationPublicId}" )
    @Produces( MediaType.APPLICATION_JSON )
    public ScanReceipt uploadScan( @PathParam( "applicationPublicId" ) final String applicationPublicId,
                                   @QueryParam( "instanceId" ) final String instanceId, final InputStream data )
        throws IOException
    {
        Application application = applicationDAO.getByPublicIdNotNull( applicationPublicId );
        String appId = application.getId();

        final File scanFile = FileUtils.createTempFile( "temp-", SCAN_EXT, work.getScanDir( appId ) );
        final File scanDir = scanFile.getParentFile();

        scanDir.mkdirs();
        final FileOutputStream os = new FileOutputStream( scanFile );
        try
        {
            IOUtil.copy( data, os );
        }
        finally
        {
            IOUtil.close( os );
        }

        final RepoManScanUploadRequest request = new RepoManScanUploadRequest( applicationPublicId, scanFile, null );
        request.setInstanceId( instanceId );

        final BOMCheckScanUploadResult result = uploader.upload( proxy.contextualize( request ) );
        if ( StringUtils.isNotBlank( result.getScanId() ) )
        {
            FileUtils.rename( scanFile, new File( scanDir, "scan-" + result.getScanId() + SCAN_EXT ) );
        }

        final ScanReceipt receipt = new ScanReceipt();
        receipt.setScanId( result.getScanId() );
        receipt.setTimeToReport( result.getTimeToReport() );
        receipt.setReportUrl( ReportResource.getReportPath( applicationPublicId, result.getScanId() ) );

        return receipt;
    }
}
