package com.sonatype.insight.brain.saas;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Context;

import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.StringUtils;

import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.scan.upload.BOMCheckScanUploadResult;

public abstract class AbstractSaasResource
{
    @Context
    private InsightWork work;

    @Context
    private SaasClient client;

    private ApplicationDAO applicationDAO = new ApplicationDAO();

    protected BOMCheckScanUploadResult doScanUpload( HttpServletRequest request, String applicationPublicId,
                                                     String path, String... params )
        throws IOException
    {
        Application application = applicationDAO.getByPublicIdNotNull( applicationPublicId );
        String appId = application.getId();

        final File scanDir = work.getScanDir( appId );
        final File scanFile = FileUtils.createTempFile( "temp-", ".xml.gz", scanDir );

        scanDir.mkdirs();
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

        final BOMCheckScanUploadResult result = client.get( request, BOMCheckScanUploadResult.class, path, params );

        if ( StringUtils.isNotBlank( result.getScanId() ) )
        {
            FileUtils.rename( scanFile, new File( scanDir, "scan-" + result.getScanId() + ".xml.gz" ) );
        }

        return result;
    }
}
