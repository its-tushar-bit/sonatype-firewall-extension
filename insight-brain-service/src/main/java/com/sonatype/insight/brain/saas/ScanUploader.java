package com.sonatype.insight.brain.saas;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.servlet.http.HttpServletRequest;

import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sonatype.clm.dto.model.ScanReceipt;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.report.ReportResource;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.scan.upload.BOMCheckScanUploadResult;

@Named
@Singleton
public class ScanUploader
{
    private static final Logger log = LoggerFactory.getLogger( ScanUploader.class );

    private final SaasClient client;

    private final InsightWork work;

    private ApplicationDAO applicationDAO = new ApplicationDAO();

    @Inject
    public ScanUploader( final SaasClient client, final InsightWork work )
    {
        this.client = client;
        this.work = work;
    }

    protected ScanReceipt upload( HttpServletRequest request, String applicationPublicId, String path,
                                               String... params )
        throws IOException
    {
        Application application = applicationDAO.getByPublicIdNotNull( applicationPublicId );
        String appId = application.getId();

        final File scanDir = work.getScanDir( appId );
        scanDir.mkdirs();

        final File scanFile = FileUtils.createTempFile( "temp-", ".xml.gz", scanDir );

        final FileOutputStream os = new FileOutputStream( scanFile );
        try
        {
            IOUtil.copy( request.getInputStream(), os );
        }
        finally
        {
            IOUtil.close( os );
        }

        request.setAttribute( SaasClient.UPLOAD_FILE_ATTRIBUTE, scanFile );

        final BOMCheckScanUploadResult uploadResult = client.get( request, BOMCheckScanUploadResult.class, path, params );

        if ( StringUtils.isNotBlank( uploadResult.getScanId() ) )
        {
            FileUtils.rename( scanFile, new File( scanDir, "scan-" + uploadResult.getScanId() + ".xml.gz" ) );
        }

        log.debug( "Successfully uploaded scan id {}", uploadResult.getScanId() );
        
        final ScanReceipt receipt = new ScanReceipt();
        receipt.setScanId( uploadResult.getScanId() );
        receipt.setTimeToReport( uploadResult.getTimeToReport() );
        receipt.setReportUrl( ReportResource.getReportPath( applicationPublicId, uploadResult.getScanId() ) );

        return receipt;
    }
}
