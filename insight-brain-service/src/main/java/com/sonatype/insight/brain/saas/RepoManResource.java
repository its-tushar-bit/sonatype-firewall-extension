/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.saas;

import java.io.IOException;

import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

import com.sonatype.clm.dto.model.ScanReceipt;
import com.sonatype.insight.brain.product.license.ProductLicenseEnforcementPoint;
import com.sonatype.insight.brain.report.ReportResource;
import com.sonatype.insight.license.model.CLMEnforcementPoint;
import com.sonatype.insight.scan.upload.BOMCheckScanUploadResult;

@Path( RepoManResource.SERVICE_PATH )
@ProductLicenseEnforcementPoint( { CLMEnforcementPoint.StageRelease } )
@Named
public class RepoManResource
{
    public static final String SERVICE_PATH = "rest/rm";

    private final ScanUploader uploader;

    @Inject
    public RepoManResource( final ScanUploader uploader )
    {
        this.uploader = uploader;
    }

    @PUT
    @Path( "scan/{applicationPublicId}" )
    @Produces( MediaType.APPLICATION_JSON )
    public ScanReceipt uploadScan( @PathParam( "applicationPublicId" )
    final String applicationPublicId, @Context
    HttpServletRequest req )
        throws IOException
    {
        final BOMCheckScanUploadResult result = uploader.upload( req, applicationPublicId, "rest/rm/scan" );

        final ScanReceipt receipt = new ScanReceipt();
        receipt.setScanId( result.getScanId() );
        receipt.setTimeToReport( result.getTimeToReport() );
        receipt.setReportUrl( ReportResource.getReportPath( applicationPublicId, result.getScanId() ) );

        return receipt;
    }
}
