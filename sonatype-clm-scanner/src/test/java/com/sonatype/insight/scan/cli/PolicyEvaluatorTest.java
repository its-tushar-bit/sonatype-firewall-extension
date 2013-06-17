/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.scan.cli;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

import org.apache.http.client.HttpResponseException;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.rules.TestName;
import org.mockito.ArgumentCaptor;
import org.slf4j.LoggerFactory;
import org.sonatype.guice.bean.containers.InjectedTest;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.util.ContextInitializer;
import ch.qos.logback.core.OutputStreamAppender;
import ch.qos.logback.core.util.StatusPrinter;

import com.google.inject.Binder;
import com.sonatype.clm.dto.model.ScanReceipt;
import com.sonatype.clm.dto.model.policy.Action;
import com.sonatype.clm.dto.model.policy.PolicyAlert;
import com.sonatype.clm.dto.model.policy.PolicyEvaluationResult;
import com.sonatype.clm.dto.model.policy.PolicyFact;
import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.client.utils.HttpClientUtils.Configuration;
import com.sonatype.insight.scan.cli.RestClientFactory.RestClient;
import com.sonatype.insight.scan.model.Scan;
import com.sonatype.insight.scan.model.ScanConfiguration;
import com.sonatype.insight.scan.model.ScanItem;
import com.sonatype.insight.scan.model.ScanSummary;
import com.sonatype.insight.scan.model.io.ScanReader;

public class PolicyEvaluatorTest
    extends InjectedTest
{

    @Rule
    public TestName testName = new TestName();

    @Rule
    public TemporaryFolder tmpDir = new TemporaryFolder();

    private ByteArrayOutputStream log;

    @Inject
    private PolicyEvaluator evaluator;

    @Inject
    private ScanReader scanReader;

    private RestClient restClient;

    private ArgumentCaptor<Configuration> httpConfig;

    @Before
    public void setUp()
        throws Exception
    {
        System.out.println( "--- " + testName.getMethodName() + " ------------------------" );
        try
        {
            String outDir = tmpDir.newFolder( "scan" ).getAbsolutePath();
            String timestamp = "20130610-171959";
            System.setProperty( PolicyEvaluatorCli.PROP_OUTPUT_DIRECTORY, outDir );
            System.setProperty( PolicyEvaluatorCli.PROP_START_TIME, timestamp );
            log = new ByteArrayOutputStream( 1024 * 4 );
            resetLogback();
        }
        catch ( Exception e )
        {
            throw new IllegalStateException( e );
        }
        super.setUp();
    }

    @After
    public void resetLogger()
    {
        // close file appenders to allow deletion of tmp files
        LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();
        lc.reset();
    }

    private void resetLogback()
    {
        LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();
        try
        {
            lc.reset();
            new ContextInitializer( lc ).autoConfig();
            PatternLayoutEncoder encoder = new PatternLayoutEncoder();
            encoder.setContext( lc );
            encoder.setPattern( "[%level] %m%n" );
            encoder.start();
            OutputStreamAppender<ILoggingEvent> appender = new OutputStreamAppender<ILoggingEvent>();
            appender.setContext( lc );
            appender.setEncoder( encoder );
            appender.setOutputStream( log );
            appender.setName( "mem" );
            appender.start();
            lc.getLogger( org.slf4j.Logger.ROOT_LOGGER_NAME ).addAppender( appender );
        }
        catch ( Exception je )
        {
            je.printStackTrace();
        }
        StatusPrinter.printInCaseOfErrorsOrWarnings( lc );
    }

    private void assertLog( String line )
        throws Exception
    {
        List<String> logLines = Arrays.asList( log.toString( "UTF-8" ).split( "\r\n|\r|\n" ) );
        assertTrue( "Could not locate log: " + line, logLines.contains( line ) );
    }

    private ScanReceipt newReceipt()
    {
        ScanReceipt receipt = new ScanReceipt();
        receipt.setScanId( "the-scan-id" );
        receipt.setReportUrl( "the-report-url" );
        receipt.setTimeToReport( 0L );
        return receipt;
    }

    @Override
    public void configure( Binder binder )
    {
        RestClientFactory restClientFactory = mock( RestClientFactory.class );
        binder.bind( RestClientFactory.class ).toInstance( restClientFactory );
        httpConfig = ArgumentCaptor.forClass( Configuration.class );
        restClient = mock( RestClient.class );
        when( restClientFactory.newRestClient( httpConfig.capture() ) ).thenReturn( restClient );
    }

    @Test
    public void testServerDown()
        throws Exception
    {
        when( restClient.getApplications() ).thenThrow( new HttpResponseException( 503, "Maintenance" ) );
        Parameters params =
            new Parameters( "-s", "http://localhost:8070/", "-p", "localhost:8888", "-U", "proxyuser:proxypass", "-i",
                            "the-app-id", "src/test/data/artifact.jar" );
        try
        {
            evaluator.run( params );
            fail( "Expected error" );
        }
        catch ( ExitException e )
        {
            assertLog( "[ERROR] The CLM Server is down for maintenance, please try again later." );
            assertEquals( "http://localhost:8070/", httpConfig.getValue().getServerUrl() );
            assertEquals( "localhost", httpConfig.getValue().getProxyHost() );
            assertEquals( 8888, httpConfig.getValue().getProxyPort() );
            assertEquals( "proxyuser", httpConfig.getValue().getProxyAuth().getUsername() );
            assertEquals( "proxypass", new String( httpConfig.getValue().getProxyAuth().getPassword() ) );
        }
    }

    @Test
    public void testInvalidAppId()
        throws Exception
    {
        Parameters params =
            new Parameters( "-s", "http://localhost:8070/", "-i", "the-app-id", "src/test/data/artifact.jar" );
        try
        {
            evaluator.run( params );
            fail( "Expected error" );
        }
        catch ( ExitException e )
        {
            assertLog( "[ERROR] The application ID the-app-id is invalid." );
        }
    }

    @Test
    public void testNoViolations()
        throws Exception
    {
        when( restClient.getApplications() ).thenReturn( Collections.singletonMap( "the-app-id", "My App" ) );
        when( restClient.uploadScan( eq( "the-app-id" ), any( File.class ) ) ).thenReturn( newReceipt() );
        when( restClient.evaluatePolicy( eq( "the-app-id" ), eq( "the-scan-id" ), eq( Stage.ID_BUILD ) ) ).thenReturn( new PolicyEvaluationResult() );
        Parameters params =
            new Parameters( "-s", "http://localhost:8070/", "-i", "the-app-id", "src/test/data/artifact.jar" );
        evaluator.run( params );
        assertLog( "[INFO] Summary of policy violations: 0 critical, 0 severe, 0 moderate" );
    }

    @Test
    public void testSomeViolations()
        throws Exception
    {
        PolicyAlert alert =
            new PolicyAlert( new PolicyFact( "policyId", "Policy Name", 10 ),
                             Arrays.asList( new Action( Action.ID_WARN ) ) );
        PolicyEvaluationResult eval = new PolicyEvaluationResult();
        eval.setAffectedComponentCount( 6 );
        eval.setCriticalComponentCount( 1 );
        eval.setSevereComponentCount( 2 );
        eval.setModerateComponentCount( 3 );
        eval.setAlerts( Arrays.asList( alert ) );
        when( restClient.getApplications() ).thenReturn( Collections.singletonMap( "the-app-id", "My App" ) );
        when( restClient.uploadScan( eq( "the-app-id" ), any( File.class ) ) ).thenReturn( newReceipt() );
        when( restClient.evaluatePolicy( eq( "the-app-id" ), eq( "the-scan-id" ), eq( Stage.ID_BUILD ) ) ).thenReturn( eval );
        Parameters params =
            new Parameters( "-s", "http://localhost:8070/", "-i", "the-app-id", "src/test/data/artifact.jar" );
        evaluator.run( params );
        assertLog( "[INFO] Policy Action: Warning" );
        assertLog( "[INFO] Summary of policy violations: 1 critical, 2 severe, 3 moderate" );
        assertLog( "[WARN] Sonatype CLM reports policy warning due to " );
    }

    @Test
    public void testEffectiveActionIsMostSevere()
        throws Exception
    {
        PolicyAlert alert1 =
            new PolicyAlert( new PolicyFact( "policy1", "Policy 1", 10 ), Arrays.asList( new Action( Action.ID_WARN ) ) );
        PolicyAlert alert2 =
            new PolicyAlert( new PolicyFact( "policy2", "Policy 2", 10 ), Arrays.asList( new Action( Action.ID_FAIL ) ) );
        PolicyAlert alert3 =
            new PolicyAlert( new PolicyFact( "policy3", "Policy 3", 10 ), Arrays.asList( new Action( Action.ID_WARN ) ) );
        PolicyEvaluationResult eval = new PolicyEvaluationResult();
        eval.setAffectedComponentCount( 6 );
        eval.setCriticalComponentCount( 1 );
        eval.setSevereComponentCount( 2 );
        eval.setModerateComponentCount( 3 );
        eval.setAlerts( Arrays.asList( alert1, alert2, alert3 ) );
        when( restClient.getApplications() ).thenReturn( Collections.singletonMap( "the-app-id", "My App" ) );
        when( restClient.uploadScan( eq( "the-app-id" ), any( File.class ) ) ).thenReturn( newReceipt() );
        when( restClient.evaluatePolicy( eq( "the-app-id" ), eq( "the-scan-id" ), eq( Stage.ID_BUILD ) ) ).thenReturn( eval );
        Parameters params =
            new Parameters( "-s", "http://localhost:8070/", "-i", "the-app-id", "src/test/data/artifact.jar" );
        evaluator.run( params );
        assertLog( "[INFO] Policy Action: Failure" );
        assertLog( "[INFO] Summary of policy violations: 1 critical, 2 severe, 3 moderate" );
        assertLog( "[ERROR] Sonatype CLM reports policy failing due to " );
        assertLog( "[WARN] Sonatype CLM reports policy warning due to " );
    }

    @Test
    public void testReportUrl()
        throws Exception
    {
        when( restClient.getApplications() ).thenReturn( Collections.singletonMap( "the-app-id", "My App" ) );
        when( restClient.uploadScan( eq( "the-app-id" ), any( File.class ) ) ).thenReturn( newReceipt() );
        when( restClient.evaluatePolicy( eq( "the-app-id" ), eq( "the-scan-id" ), eq( Stage.ID_BUILD ) ) ).thenReturn( new PolicyEvaluationResult() );
        Parameters params =
            new Parameters( "-s", "http://localhost:8070/", "-i", "the-app-id", "src/test/data/artifact.jar" );
        evaluator.run( params );
        assertLog( "[INFO] The detailed report can be viewed online at http://localhost:8070/the-report-url" );
    }

    @Test
    public void testScan()
        throws Exception
    {
        ArgumentCaptor<File> scanFile = ArgumentCaptor.forClass( File.class );
        when( restClient.getApplications() ).thenReturn( Collections.singletonMap( "the-app-id", "My App" ) );
        when( restClient.uploadScan( eq( "the-app-id" ), scanFile.capture() ) ).thenReturn( newReceipt() );
        when( restClient.evaluatePolicy( eq( "the-app-id" ), eq( "the-scan-id" ), eq( Stage.ID_BUILD ) ) ).thenReturn( new PolicyEvaluationResult() );
        Parameters params =
            new Parameters( "-s", "http://localhost:8070/", "-i", "the-app-id", "-pp", "com.sonatype",
                            "src/test/data/artifact.jar" );
        evaluator.run( params );
        assertNotNull( scanFile.getValue() );
        Scan scan = scanReader.read( scanFile.getValue() );
        assertNotNull( scan );
        ScanSummary summary = scan.getSummary();
        assertNotNull( summary );
        assertNotNull( summary.getStartTime() );
        assertNotNull( summary.getEndTime() );
        assertNotNull( summary.getClientInfo() );
        assertNotNull( summary.getClientInfo().getProperty( "java.version" ) );
        ScanConfiguration config = scan.getConfiguration();
        assertNotNull( config );
        assertEquals( "com.sonatype", config.getString( "", "proprietaryPackages" ) );
        assertEquals( 1, scan.getItems().size() );
        ScanItem jar = scan.getItems().get( 0 );
        assertEquals( "artifact.jar", jar.getPath() );
        assertEquals( "87cf012929052d02c3f1", jar.getSha1() );
        for ( ScanItem item : jar.getItems() )
        {
            assertNull( item.getPath() );
            assertNotNull( item.getSha1() );
            assertNotNull( item.getSha1JA001() );
            assertEquals( "proprietaryPackages", item.getNoPathReason() );
        }
    }

}
