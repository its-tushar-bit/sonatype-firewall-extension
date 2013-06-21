/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.report;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.equalToIgnoringWhiteSpace;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.stringContainsInOrder;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collection;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.StringUtils;
import org.junit.Assert;
import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ContainerNode;
import com.ning.http.client.Response;
import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.dataaccess.component.HashGAVDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.component.HashGAV;
import com.sonatype.insight.brain.model.component.IdentificationSource;
import com.sonatype.insight.brain.model.component.MatchState;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;
import com.sonatype.insight.brain.policy.evaluator.PolicyEvaluateResource;
import com.sonatype.insight.brain.policy.evaluator.PolicyEvaluationLog;
import com.sonatype.insight.brain.service.AbstractResourceTest;
import com.sonatype.insight.client.utils.UrlUtils;
import com.sonatype.insight.json.store.JsonUtils;
import com.sonatype.insight.test.RestAccess;
import com.yammer.dropwizard.testing.JsonHelpers;

import eu.medsea.mimeutil.MimeType;
import eu.medsea.mimeutil.detector.MagicMimeMimeDetector;

public class ReportResourceTest
    extends AbstractResourceTest
{
    @Test
    public void testManuallyIdentifiedComponent()
        throws Exception
    {
        // The hash of commons-httpclient-3.1.SONATYPE.jar, similar match of commons-httpclient:commons-httpclient:3.1
        String hash = "f0776db1593e215146d2";
        String groupId = "testClaimedComponent_G";
        String artifactId = "testClaimedComponent_A";
        String version = "testClaimedComponent_V";
        String extension = "testClaimedComponent_E";
        String classifier = "testClaimedComponent_C";
        HashGAV hashGAV = new HashGAV( hash, groupId, artifactId, version, extension, classifier );
        HashGAVDAO hashGAVDAO = new HashGAVDAO();
        hashGAVDAO.insert( hashGAV );

        String applicationPublicId = "testClaimedComponent_AppId";
        createApplication( applicationPublicId );
        String scanId = "testClaimedComponent_ScanId";
        String licenseFingerprint = "ReportResourceTest_LicenseFingerprint";
        setLicenseFingerprint( licenseFingerprint );

        File saasReportFile = getReportResponseFile( licenseFingerprint, scanId );
        saasReportFile.delete();

        URL testReportResultUrl = getClass().getResource( "/ReportResourceTest/report.zip" );
        FileUtils.copyFile( new File( testReportResultUrl.getFile() ), saasReportFile );

        assertResponseStatus( 200,
                              RestAccess.get( getRestBaseUrl()
                                  + ReportResource.getReportPath( applicationPublicId, scanId ) ) );

        String resourcePrefix = getServiceURL( applicationPublicId, scanId );
        Response response = RestAccess.get( resourcePrefix + "/embedReport/bom.json" );
        assertResponseStatus( 200, response );
        boolean foundClaimedComponent = false;
        String bomJsonData = response.getResponseBody();
        for ( JsonNode bomJsonNode : JsonUtils.parse( bomJsonData ).get( "aaData" ) )
        {
            String bomJsonHash = bomJsonNode.get( "hash" ).asText();
            JsonNode identificationSource = bomJsonNode.get( "identificationSource" );
            if ( hash.equals( bomJsonHash ) )
            {
                assertEquals( IdentificationSource.MANUAL.getId(), identificationSource.asText() );
                assertEquals( groupId, bomJsonNode.get( "groupId" ).asText() );
                assertEquals( artifactId, bomJsonNode.get( "artifactId" ).asText() );
                assertEquals( version, bomJsonNode.get( "version" ).asText() );
                assertEquals( extension, bomJsonNode.get( "extension" ).asText() );
                assertEquals( classifier, bomJsonNode.get( "classifier" ).asText() );
                assertEquals( MatchState.EXACT.getId(), bomJsonNode.get( "matchState" ).asText() );
                foundClaimedComponent = true;
            }
            else
            {
                assertNull( identificationSource );
            }
        }
        assertTrue( foundClaimedComponent );

        response = RestAccess.get( resourcePrefix + "/embedReport/licenses.json" );
        assertResponseStatus( 200, response );
        String licensesJsonData = response.getResponseBody();
        assertNotNull( licensesJsonData );
        assertFalse( StringUtils.isEmpty( licensesJsonData ) );
        assertFalse( licensesJsonData.contains( hash ) );
        assertFalse( licensesJsonData.contains( "commons-httpclient" ) );

        response = RestAccess.get( resourcePrefix + "/embedReport/security.json" );
        assertResponseStatus( 200, response );
        String securityJsonData = response.getResponseBody();
        assertNotNull( securityJsonData );
        assertFalse( StringUtils.isEmpty( securityJsonData ) );
        assertFalse( securityJsonData.contains( hash ) );
        assertFalse( securityJsonData.contains( "commons-httpclient" ) );

        response = RestAccess.get( resourcePrefix + "/embedReport/partialmatched.json" );
        assertResponseStatus( 200, response );
        String partialmatched = response.getResponseBody();
        assertNotNull( partialmatched );
        assertFalse( StringUtils.isEmpty( partialmatched ) );
        assertFalse( partialmatched.contains( hash ) );
        assertFalse( partialmatched.contains( "commons-httpclient" ) );

        hashGAVDAO.delete( hashGAV );
    }

    @Test
    public void testEmbedReport()
        throws Exception
    {
        final String applicationPublicId = "ReportResourceTest_AppId";
        createApplication( applicationPublicId );
        final String scanId = "ReportResourceTest_ScanId";
        final String licenseFingerprint = "ReportResourceTest_LicenseFingerprint";
        setLicenseFingerprint( licenseFingerprint );

        final String resourcePrefix = getServiceURL( applicationPublicId, scanId );

        final File saasReportFile = getReportResponseFile( licenseFingerprint, scanId );
        saasReportFile.delete();

        final URL testReportResultUrl = getClass().getResource( "/ReportResourceTest/report.zip" );
        FileUtils.copyFile( new File( testReportResultUrl.getFile() ), saasReportFile );

        final ZipFile zipFile = new ZipFile( saasReportFile );
        final Enumeration<? extends ZipEntry> e = zipFile.entries();
        while ( e.hasMoreElements() )
        {
            final ZipEntry entry = e.nextElement();
            final Response response = RestAccess.get( resourcePrefix + "/embedReport/" + entry.getName() );
            final String contentType = response.getContentType();
            assertResponseStatus( 200, response );

            if ( "data.json".equals( entry.getName() ) )
            {
                String actual = response.getResponseBody();
                testDataJsonApplyChanges( actual );
            }
            else if ( "badges.json".equals( entry.getName() ) )
            {
                assertThat( JsonUtils.parse( response.getResponseBodyAsBytes(), int[].class ), equalTo( new int[] { 36,
                    8, 36 } ) );
            }
            else if ( "licenses.json".equals( entry.getName() ) )
            {
                String expected = IOUtil.toString( zipFile.getInputStream( entry ), "UTF-8" );
                String actual = response.getResponseBody();

                testLicensesJsonApplyChanges( actual );

                // embedded report processor modifies the effectiveLicenseThreat property type
                String alteredExpected = expected.replaceAll( ",\\s*\"effectiveLicenseThreat\" : \"[^\"]+\"", "" );
                String alteredActual = actual.replaceAll( ",\\s*\"effectiveLicenseThreat\" : [^,]+", "" );

                assertThat( alteredActual, equalToIgnoringWhiteSpace( alteredExpected ) );
            }
            else if ( "licensethreats.json".equals( entry.getName() ) )
            {
                String actual = response.getResponseBody();

                testLicenseThreatsJsonApplyChanges( actual );
            }
            else if ( "partialmatched.json".equals( entry.getName() ) )
            {
                String actual = response.getResponseBody();

                testPartialMatchedJsonApplyChanges( actual );
            }
            else if ( "index.html".equals( entry.getName() ) )
            {
                String actual = response.getResponseBody();
                assertTrue( "The app public id was not included in the report",
                            actual.contains( "applicationId = '" + applicationPublicId + "'" ) );
            }
            else if ( contentType.startsWith( "text" ) || contentType.endsWith( "json" ) )
            {
                assertThat( response.getResponseBody(),
                            equalToIgnoringWhiteSpace( IOUtil.toString( zipFile.getInputStream( entry ), "UTF-8" ) ) );
            }
            else
            {
                assertThat( IOUtil.toByteArray( response.getResponseBodyAsStream() ),
                            equalTo( IOUtil.toByteArray( zipFile.getInputStream( entry ) ) ) );
            }
        }

        zipFile.close();

        assertResponseStatus( 200,
                              RestAccess.get( getRestBaseUrl()
                                  + ReportResource.getReportPath( applicationPublicId, scanId ) ) );
    }

    @Test
    public void testPrintReport()
        throws Exception
    {
        final String applicationPublicId = "ReportResourceTest_AppId";
        createApplication( applicationPublicId );
        final String scanId = "ReportResourceTest_ScanId";
        final String licenseFingerprint = "ReportResourceTest_LicenseFingerprint";
        setLicenseFingerprint( licenseFingerprint );

        final String resourcePrefix = getServiceURL( applicationPublicId, scanId );

        final File saasReportFile = getReportResponseFile( licenseFingerprint, scanId );
        saasReportFile.delete();

        final URL testReportResultUrl = getClass().getResource( "/ReportResourceTest/report.zip" );
        FileUtils.copyFile( new File( testReportResultUrl.getFile() ), saasReportFile );

        final Response response;
        try
        {
            response = RestAccess.get( resourcePrefix + "/printReport?projectName=Test%20Project&buildNumber=8" );
            assertResponseStatus( 200, response );
            assertThat( response.getHeader( "Content-Disposition" ),
                        stringContainsInOrder( Arrays.asList( "attachment; filename=", "Test%20Project-8-", ".pdf" ) ) );
        }
        finally
        {
            Pdf.destroy();
        }

        // validate content type and check the actual content is really a PDF
        assertThat( response.getContentType(), equalTo( "application/pdf" ) );
        final Collection<?> mimeTypes = new MagicMimeMimeDetector().getMimeTypes( response.getResponseBodyAsStream() );
        assertThat( mimeTypes, contains( (Object) new MimeType( "application/pdf" ) ) );
    }

    @Test
    public void testPrintReport_AfterPreviousGenerationFailure()
        throws Exception
    {
        final String applicationPublicId = "ReportResourceTest_AppId";
        final String appId = createApplication( applicationPublicId ).getId();
        final String scanId = "ReportResourceTest_ScanId";
        final String licenseFingerprint = "ReportResourceTest_LicenseFingerprint";
        setLicenseFingerprint( licenseFingerprint );

        final String resourcePrefix = getServiceURL( applicationPublicId, scanId );

        final File saasReportFile = getReportResponseFile( licenseFingerprint, scanId );
        saasReportFile.delete();

        final URL testReportResultUrl = getClass().getResource( "/ReportResourceTest/report.zip" );
        FileUtils.copyFile( new File( testReportResultUrl.getFile() ), saasReportFile );

        Response response;
        try
        {
            response = RestAccess.get( resourcePrefix + "/printReport?projectName=Test%20Project&buildNumber=8" );
            assertResponseStatus( 200, response );

            // pretend the print attempt crashed with OOME, which usually leaves an empty PDF file around
            File pdfFile =
                new File( new File( new File( brain.getWorkDir(), "report/" + appId ), scanId ), "report.pdf" );
            assertTrue( pdfFile.getPath(), pdfFile.isFile() );
            new FileOutputStream( pdfFile ).close();

            // printing again after fixing the mem setting should produce a proper PDF
            response = RestAccess.get( resourcePrefix + "/printReport?projectName=Test%20Project&buildNumber=8" );
            assertResponseStatus( 200, response );
            assertThat( response.getHeader( "Content-Disposition" ),
                        stringContainsInOrder( Arrays.asList( "attachment; filename=", "Test%20Project-8-", ".pdf" ) ) );
            assertThat( Long.parseLong( response.getHeader( "Content-Length" ) ), greaterThan( 0L ) );
        }
        finally
        {
            Pdf.destroy();
        }

        // validate content type and check the actual content is really a PDF
        assertThat( response.getContentType(), equalTo( "application/pdf" ) );
        final Collection<?> mimeTypes = new MagicMimeMimeDetector().getMimeTypes( response.getResponseBodyAsStream() );
        assertThat( mimeTypes, contains( (Object) new MimeType( "application/pdf" ) ) );
    }

    @Test
    public void testReevaluateReport()
        throws Exception
    {
        final String applicationPublicId = "ReportResourceTest_AppId";
        final Application application = createApplication( applicationPublicId );
        final String scanId = "ReportResourceTest_ScanId";
        final String licenseFingerprint = "ReportResourceTest_LicenseFingerprint";
        setLicenseFingerprint( licenseFingerprint );

        final File saasReportFile = getReportResponseFile( licenseFingerprint, scanId );
        saasReportFile.delete();

        final URL testReportResultUrl = getClass().getResource( "/ReportResourceTest/report.zip" );
        FileUtils.copyFile( new File( testReportResultUrl.getFile() ), saasReportFile );

        PolicyEvaluationLog evalLog = new PolicyEvaluationLog( brain.getAuditDir( application.getId() ) );
        PolicyEvaluation policyEvaluation = evalLog.get( scanId );

        Assert.assertNull( policyEvaluation );

        final Stage stage = new Stage( BuildStageType.ID );

        // Evaluate policy
        Response response =
            RestAccess.post( getRestBaseUrl()
                                 + PolicyEvaluateResource.SERVICE_PATH.replace( "{applicationPublicId}",
                                                                                applicationPublicId ) + "?scanId="
                                 + scanId, JsonHelpers.asJson( stage ) );

        // ReEvaluate
        assertResponseStatus( 200, response );

        policyEvaluation = evalLog.get( scanId );
        Assert.assertNotNull( policyEvaluation );
        Assert.assertEquals( scanId, policyEvaluation.getScanId() );
        Assert.assertNotNull( policyEvaluation.getStage() );
        Assert.assertEquals( BuildStageType.ID, policyEvaluation.getStage().getStageTypeId() );
        assertTrue( System.currentTimeMillis() - policyEvaluation.getTime() < 60 * 1000 );
    }

    @Test
    public void testArtifactDetails()
        throws Exception
    {
        final String applicationPublicId = "ReportResourceTest_AppId";
        createApplication( applicationPublicId );
        final String scanId = "ReportResourceTest_ScanId";

        final String resourcePrefix = getServiceURL( applicationPublicId, scanId );
        final String query = "?groupId=org.springframework&artifactId=spring-core&version=2.5.6";
        final Response response = RestAccess.get( resourcePrefix + "/artifactDetails" + query );
        assertResponseStatus( 200, response );

        assertThat( response.getResponseBody(), stringContainsInOrder( Arrays.asList( "\"groupId\"",
                                                                                      "\"org.springframework\"",
                                                                                      "\"artifactId\"",
                                                                                      "\"spring-core\"", "\"version\"",
                                                                                      "\"2.5.6\"" ) ) );
    }

    @Test
    public void testAugmentDataAndAuditLog()
        throws Exception
    {
        final String applicationPublicId = "ReportResourceTest_AppId";
        createApplication( applicationPublicId );
        final String scanId = "ReportResourceTest_ScanId";
        final String licenseFingerprint = "ReportResourceTest_LicenseFingerprint";
        setLicenseFingerprint( licenseFingerprint );

        final String resourcePrefix = getServiceURL( applicationPublicId, scanId );

        final File saasReportFile = getReportResponseFile( licenseFingerprint, scanId );
        saasReportFile.delete();

        final URL testReportResultUrl = getClass().getResource( "/ReportResourceTest/report.zip" );
        FileUtils.copyFile( new File( testReportResultUrl.getFile() ), saasReportFile );

        final String query = "security.json?user=test&where=ReportResourceTest";

        // attempt a bad edit (no augmented data)
        Response response = RestAccess.post( resourcePrefix + "/augmentData/" + query, "" );
        assertResponseStatus( 400, response ); // bad request; no changes

        // verify nothing has changed
        response = RestAccess.get( resourcePrefix + "/embedReport/security.json" );
        assertResponseStatus( 200, response );
        assertThat( response.getResponseBody(), not( containsString( "\"state\" : \"accepted\"" ) ) );

        // edit the state
        String edit =
            "{ \"hash\" : \"1249e25aebb15358bedd\", \"reference\" : \"CVE-2007-5333\", \"state\" : \"accepted\" }";
        response = RestAccess.post( resourcePrefix + "/augmentData/" + query, edit );
        assertResponseStatus( 200, response );

        // verify the state has changed
        response = RestAccess.get( resourcePrefix + "/embedReport/security.json" );
        assertResponseStatus( 200, response );
        assertThat( response.getResponseBody(), containsString( "\"state\" : \"accepted\"" ) );

        // check the audit log reflects this change
        response =
            RestAccess.get( resourcePrefix + "/auditLog/security.json?key="
                + UrlUtils.encodeUrlComponent( "{\"hash\":\"1249e25aebb15358bedd\"}" ) );
        assertResponseStatus( 200, response );

        String feed =
            "{ \"aaData\" : [ { \"hash\" : \"1249e25aebb15358bedd\", \"reference\" : \"CVE-2007-5333\", \"state\" : \"accepted\", \"user\" : \"test\", \"ip\" : \"127.0.0.1\", \"where\" : \"ReportResourceTest\", \"filename\" : \"security.json\" } ] }";

        assertThat( response.getResponseBody().replaceFirst( "\"time\" : [0-9]+,", "" ),
                    equalToIgnoringWhiteSpace( feed ) );

        // edit the state again
        edit = "{ \"hash\" : \"1249e25aebb15358bedd\", \"reference\" : \"CVE-2007-5333\", \"state\" : \"confirmed\" }";
        response = RestAccess.post( resourcePrefix + "/augmentData/" + query, edit );
        assertResponseStatus( 200, response );

        // verify the state has changed again
        response = RestAccess.get( resourcePrefix + "/embedReport/security.json" );
        assertResponseStatus( 200, response );
        assertThat( response.getResponseBody(), containsString( "\"state\" : \"confirmed\"" ) );

        // check the audit log reflects this change
        response =
            RestAccess.get( resourcePrefix + "/auditLog/security.json?key="
                + UrlUtils.encodeUrlComponent( "{\"hash\":\"1249e25aebb15358bedd\"}" ) );
        assertResponseStatus( 200, response );

        feed =
            "{ \"aaData\" : [ { \"hash\" : \"1249e25aebb15358bedd\", \"reference\" : \"CVE-2007-5333\", \"state\" : \"confirmed\", \"user\" : \"test\", \"ip\" : \"127.0.0.1\", \"where\" : \"ReportResourceTest\", \"filename\" : \"security.json\" }, "
                + "{ \"hash\" : \"1249e25aebb15358bedd\", \"reference\" : \"CVE-2007-5333\", \"state\" : \"accepted\", \"user\" : \"test\", \"ip\" : \"127.0.0.1\", \"where\" : \"ReportResourceTest\", \"filename\" : \"security.json\" } ] }";

        assertThat( response.getResponseBody().replaceAll( "\"time\" : [0-9]+,", "" ), equalToIgnoringWhiteSpace( feed ) );

        // edit the license
        final String licenseEdit =
            "[{\"groupId\":\"commons-pool\",\"artifactId\":\"commons-pool\",\"version\":\"1.4\",\"status\":\"Overridden\",\"overriddenLicenses\":[\"GPL-3.0\"],\"overriddenLicenseThreat\":10,\"comment\":\"\"}]:";
        final String licenseQuery = "licenses.json?user=test&where=ReportResourceTest";

        response = RestAccess.post( resourcePrefix + "/augmentData/" + licenseQuery, licenseEdit );
        assertResponseStatus( 200, response );

        // verify the license change has processed
        response = RestAccess.get( resourcePrefix + "/embedReport/licenses.json" );
        assertResponseStatus( 200, response );

        // verify that the license is overridden correctly
        boolean found = false;
        final String licenseJsonString = response.getResponseBody();
        final JsonNode licenseJsonData = JsonUtils.parse( licenseJsonString ).get( "aaData" );
        for ( JsonNode licenseJsonNode : licenseJsonData )
        {
            if ( "commons-pool".equals( licenseJsonNode.get( "groupId" ).asText() )
                && "commons-pool".equals( licenseJsonNode.get( "artifactId" ).asText() )
                && "1.4".equals( licenseJsonNode.get( "version" ).asText() ) )
            {
                String overridenLicenseNamesStr = licenseJsonNode.get( "overriddenLicenses" ).toString();
                Assert.assertEquals( "[\"GPL-3.0\"]", overridenLicenseNamesStr );
                int threat = licenseJsonNode.get( "effectiveLicenseThreat" ).asInt();
                Assert.assertEquals( 9, threat );
                found = true;
                break;
            }
        }
        Assert.assertTrue( "Did not find expected overridden license", found );
    }

    @Test
    public void testRefreshOnlyOnChange()
        throws Exception
    {
        final String applicationPublicId = "ReportResourceTest_AppId";
        Application application = createApplication( applicationPublicId );
        String appId = application.getId();
        final String scanId = "ReportResourceTest_ScanId";
        final String licenseFingerprint = "ReportResourceTest_LicenseFingerprint";
        setLicenseFingerprint( licenseFingerprint );

        final String resourcePrefix = getServiceURL( applicationPublicId, scanId );

        final File saasReportFile = getReportResponseFile( licenseFingerprint, scanId );
        saasReportFile.delete();

        final URL testReportResultUrl = getClass().getResource( "/ReportResourceTest/report.zip" );
        FileUtils.copyFile( new File( testReportResultUrl.getFile() ), saasReportFile );

        final String query = "security.json?user=test&where=ReportResourceTest";

        // verify nothing has changed
        Response response = RestAccess.get( resourcePrefix + "/embedReport/security.json" );
        assertResponseStatus( 200, response );
        assertThat( response.getResponseBody(), not( containsString( "\"state\" : \"accepted\"" ) ) );

        // edit the state
        final String edit =
            "{ \"hash\" : \"1249e25aebb15358bedd\", \"reference\" : \"CVE-2007-5333\", \"state\" : \"accepted\" }";
        response = RestAccess.post( resourcePrefix + "/augmentData/" + query, edit );
        assertResponseStatus( 200, response );

        // check the audit log reflects this change
        response =
            RestAccess.get( resourcePrefix + "/auditLog/security.json?key="
                + UrlUtils.encodeUrlComponent( "{\"hash\":\"1249e25aebb15358bedd\"}" ) );
        assertResponseStatus( 200, response );

        final String feed =
            "{ \"aaData\" : [ { \"hash\" : \"1249e25aebb15358bedd\", \"reference\" : \"CVE-2007-5333\", \"state\" : \"accepted\", \"user\" : \"test\", \"ip\" : \"127.0.0.1\", \"where\" : \"ReportResourceTest\", \"filename\" : \"security.json\" } ] }";

        assertThat( response.getResponseBody().replaceFirst( "\"time\" : [0-9]+,", "" ),
                    equalToIgnoringWhiteSpace( feed ) );

        // force the internal modification count to make it look like we're already up-to-date
        int oldModCount = ReportResource.MODIFICATION_COUNTS.put( appId + '-' + scanId, 888 );

        // verify nothing has changed
        response = RestAccess.get( resourcePrefix + "/embedReport/security.json" );
        assertResponseStatus( 200, response );
        assertThat( response.getResponseBody(), not( containsString( "\"state\" : \"accepted\"" ) ) );

        // put back the accurate modification count, which should lead to a refresh
        ReportResource.MODIFICATION_COUNTS.put( appId + '-' + scanId, oldModCount );

        // verify the state has changed
        response = RestAccess.get( resourcePrefix + "/embedReport/security.json" );
        assertResponseStatus( 200, response );
        assertThat( response.getResponseBody(), containsString( "\"state\" : \"accepted\"" ) );
    }

    @Test
    public void testCanAuditNonReportData()
        throws Exception
    {
        final String applicationPublicId = "ReportResourceTest_AppId";
        createApplication( applicationPublicId );
        final String scanId = "ReportResourceTest_ScanId";
        final String licenseFingerprint = "ReportResourceTest_LicenseFingerprint";
        setLicenseFingerprint( licenseFingerprint );

        final String resourcePrefix = getServiceURL( applicationPublicId, scanId );

        final File saasReportFile = getReportResponseFile( licenseFingerprint, scanId );
        saasReportFile.delete();

        final URL testReportResultUrl = getClass().getResource( "/ReportResourceTest/report.zip" );
        FileUtils.copyFile( new File( testReportResultUrl.getFile() ), saasReportFile );

        final String query = "extra.json?user=test&where=ReportResourceTest";

        // audit non-report data
        final String extra = "{ \"policy\" : \"TEST\", \"result\" : \"OK\" }";
        Response response = RestAccess.post( resourcePrefix + "/augmentData/" + query, extra );
        assertResponseStatus( 200, response );

        // verify can still access report
        response = RestAccess.get( resourcePrefix + "/embedReport/security.json" );
        assertResponseStatus( 200, response );
        assertThat( response.getResponseBody(), not( containsString( "\"state\" : \"accepted\"" ) ) );

        // check the audit log reflects this change
        response = RestAccess.get( resourcePrefix + "/auditLog/extra.json" );
        assertResponseStatus( 200, response );

        final String feed =
            "{ \"aaData\" : [ { \"policy\" : \"TEST\", \"result\" : \"OK\", \"user\" : \"test\", \"ip\" : \"127.0.0.1\", \"where\" : \"ReportResourceTest\", \"filename\" : \"extra.json\" } ] }";

        assertThat( response.getResponseBody().replaceFirst( "\"time\" : [0-9]+,", "" ),
                    equalToIgnoringWhiteSpace( feed ) );
    }

    @Test
    public void testRedirection()
        throws Exception
    {
        String path = "index.html?x=y&a=b";
        String url = getServiceURL( "appId", "scanId" );
        Response response = RestAccess.get( url + "/brain/" + path );
        assertResponseStatus( 307, response );
        Assert.assertEquals( getRestBaseUrl() + path, response.getHeader( "Location" ) );
    }

    private void testDataJsonApplyChanges( String json )
        throws IOException
    {
        final ContainerNode<?> data = JsonUtils.parse( json );

        // keyFindings must not have duplicates
        JsonNode keyFindings = data.get( "keyFindings" );
        Assert.assertNotNull( keyFindings );
        Assert.assertTrue( keyFindings.size() > 0 );
        Set<String> uniqueKeyFindings = new LinkedHashSet<String>();
        for ( int i = 0; i < keyFindings.size(); i++ )
        {
            String keyFinding = keyFindings.get( i ).get( "text" ).asText();
            uniqueKeyFindings.add( keyFinding );
        }
        Assert.assertEquals( keyFindings.toString(), uniqueKeyFindings.size(), keyFindings.size() );

        Assert.assertEquals( 2, data.get( "weakcopyleftLicenseCount" ).asInt() );
        Assert.assertEquals( 2, data.get( "nonStandardLicenseCount" ).asInt() );
        Assert.assertEquals( 3, data.get( "copyleftLicenseCount" ).asInt() );
        Assert.assertEquals( 20, data.get( "liberalLicenseCount" ).asInt() );
        Assert.assertEquals( 1, data.get( "notProvidedLicenseCount" ).asInt() );
        Assert.assertEquals( "[19,0,2,0,0,0,2,0,0,4,0]", data.get( "effectiveLicenseCounts" ).toString() );

        Assert.assertEquals( 7, data.get( "insecureArtifactCount" ).asInt() );
        Assert.assertEquals( "[0,4,0,0,2,12,15,2,0,1]", data.get( "securityCounts" ).toString() );

        Assert.assertEquals( "[0,0,0,0,0,0,0,0,0,0,0]", data.get( "policyCounts" ).toString() );
        Assert.assertEquals( 0, data.get( "policyComponentCount" ).asInt() );

        Assert.assertEquals( "[[4,11,3],[0,18,0],[0,12,0],[0,6,0],[0,6,0]]", data.get( "securityPunchCard" ).toString() );
        Assert.assertEquals( "[[2,1,2],[2,1,0],[1,0,0],[0,1,0],[0,1,0]]", data.get( "licensePunchCard" ).toString() );
    }

    private void testLicensesJsonApplyChanges( String json )
        throws IOException
    {
        final ContainerNode<?> licenses = JsonUtils.parse( json );
        final JsonNode aaData = licenses.get( "aaData" );
        int countNotZero = 0;
        for ( JsonNode license : aaData )
        {
            JsonNode effectiveLicenseThreat = license.get( "effectiveLicenseThreat" );
            Assert.assertNotNull( effectiveLicenseThreat );
            Integer threat = effectiveLicenseThreat.asInt();
            Assert.assertTrue( "Effective license threat between null and 10.", threat == null
                || ( threat >= 0 && threat <= 10 ) );
            if ( threat != null && threat > 0 )
            {
                countNotZero++;
            }
        }
        Assert.assertTrue( countNotZero > 0 );
    }

    private void testLicenseThreatsJsonApplyChanges( String json )
        throws IOException
    {
        final ContainerNode<?> licenseThreats = JsonUtils.parse( json );
        final JsonNode aaData = licenseThreats.get( "aaData" );
        int countNotZero = testLicenseThreatsApplyChanges( aaData );
        Assert.assertTrue( countNotZero > 0 );
    }

    private void testPartialMatchedJsonApplyChanges( String json )
        throws IOException
    {
        final ContainerNode<?> partialMatched = JsonUtils.parse( json );
        final JsonNode aaNode = partialMatched.get( "aaData" );
        for ( JsonNode license : aaNode )
        {
            final JsonNode matchedComponentNodes = license.get( "matchDetails" );
            Assert.assertTrue( matchedComponentNodes.size() > 0 );
            testLicenseThreatsApplyChanges( matchedComponentNodes );
        }
    }

    private int testLicenseThreatsApplyChanges( JsonNode licenses )
    {
        int countNotZero = 0;
        for ( JsonNode licenseThreat : licenses )
        {
            Integer threat = licenseThreat.asInt();
            Assert.assertTrue( "Effective license threat between null and 10.", threat == null
                || ( threat >= 0 && threat <= 10 ) );
            if ( threat != null && threat > 0 )
            {
                countNotZero++;
            }
        }
        return countNotZero;
    }

    private String getServiceURL( final String appId, final String scanId )
    {
        return getRestBaseUrl()
            + ReportResource.SERVICE_PATH.replace( "{applicationPublicId}", appId ).replace( "{scanId}", scanId );
    }
}
