/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.rm.rest;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.io.File;

import org.apache.http.client.HttpResponseException;
import org.junit.Test;

import com.sonatype.insight.brain.client.ScanClient;
import com.sonatype.insight.client.utils.HttpClientUtils.Configuration;

public class RestClientFactoryTest
{

    @Test
    public void testSaasUnreachable()
        throws Exception
    {
        HttpResponseException hre = new HttpResponseException( 504, "nobody there" );
        ScanClient scanClient = mock( ScanClient.class );
        when( scanClient.uploadRepoManScan( any( File.class ) ) ).thenThrow( hre );
        RestClientFactory factory = spy( new RestClientFactory() );
        doReturn( scanClient ).when( factory ).newScanClient( any( Configuration.class ), any( String.class ) );
        try
        {
            RestClient.App client = factory.forConfiguration( new RestClientConfiguration() ).forApplication( "appId" );
            client.uploadScan( new File( "" ) );
            fail( "Expected HttpException" );
        }
        catch ( HttpException e )
        {
            assertEquals( hre.getStatusCode(), e.getStatus() );
            assertEquals( hre.getMessage(), e.getReason() );
        }
    }

}
