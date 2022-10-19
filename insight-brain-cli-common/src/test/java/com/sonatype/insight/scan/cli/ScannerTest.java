/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.scan.cli;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import javax.inject.Inject;

import com.sonatype.insight.scan.file.FileScanner;
import com.sonatype.insight.scan.file.ModuleScanRequest;
import com.sonatype.insight.scan.file.ScanSession;
import com.sonatype.insight.scan.model.ClientScanResult;
import com.sonatype.insight.test.InjectedTest;
import com.sonatype.insight.test.LogOutput;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class ScannerTest
    extends InjectedTest
{
  @Rule
  public TemporaryFolder tmpDir = new TemporaryFolder();

  @Rule
  public LogOutput logOutput = new LogOutput(Scanner.class);

  @Inject
  private Scanner scanner;

  @Test
  public void testScan_FingerprintPerformanceLoggingMessage() throws Exception {
    List<File> targets = Collections.singletonList(new File("src/test/resources/ScannerTest/app.ear"));

    scanner.scan(tmpDir.newFile("scan-test.xml.gz"), targets, new Properties());

    assertThat(logOutput).atInfoLevel()
        .containsPattern("Fingerprinting completed in \\d+ seconds for 4 archives, 60 total files");
  }

  @Test
  public void testScan_ReturnResult() throws Exception {
    List<File> targets = Collections.singletonList(new File("src/test/resources/ScannerTest/app.ear"));

    ClientScanResult clientScanResult = scanner.scan(tmpDir.newFile("scan-test.xml.gz"), targets, new Properties());

    assertThat(clientScanResult).isNotNull();
    assertThat(clientScanResult.getScanFile()).isNotNull();
    assertThat(clientScanResult.hasThirdPartyScanContent()).isFalse();
  }

  @Test
  public void testScan_ScansModules() throws Exception {
    Scanner scannerSpy = spy(scanner);
    File baseDirectory = new File("baseDirectory");
    List<File> targets = Collections.singletonList(new File("src/test/resources/ScannerTest/app.ear"));
    List<File> moduleIndices = Arrays.asList(
        new File("src/test/resources/ScannerTest/test1/module1.xml"),
        new File("src/test/resources/ScannerTest/test1/module2.xml")
    );

    scannerSpy.scan(tmpDir.newFile("scan-test.xml.gz"), baseDirectory, targets, moduleIndices, new Properties(), null,
        null);

    verify(scannerSpy).scanModules(eq(moduleIndices), any(ScanSession.class), eq(baseDirectory),
        any(FileScanner.class));
  }

  @Test
  public void testScanModules() throws Exception {
    Scanner scannerSpy = spy(scanner);
    List<File> moduleIndices = Arrays.asList(
        new File("src/test/resources/ScannerTest/test1/module1.xml"),
        new File("src/test/resources/ScannerTest/test1/module2.xml")
    );
    ScanSession scanSession = new ScanSession(null, null);
    File baseDirectory = new File("baseDirectory");
    FileScanner mockFileScanner = mock(FileScanner.class);
    List<ModuleScanRequest> scanRequests = new ArrayList<>();
    doAnswer(invocationOnMock -> {
      ModuleScanRequest moduleScanRequestSpy = spy(new ModuleScanRequest(invocationOnMock.getArgument(0)));
      scanRequests.add(moduleScanRequestSpy);
      return moduleScanRequestSpy;
    }).when(scannerSpy).createModuleScanRequest(any(ScanSession.class));

    scannerSpy.scanModules(moduleIndices, scanSession, baseDirectory, mockFileScanner);

    assertThat(scanRequests).hasSize(2);
    ModuleScanRequest moduleScanRequest1Spy = scanRequests.get(0);
    assertThat(moduleScanRequest1Spy.getScanSession()).isEqualTo(scanSession);
    verify(moduleScanRequest1Spy).setBasedir(baseDirectory);
    verify(moduleScanRequest1Spy).setModule("org.example:test1:jar:1.0-SNAPSHOT", "maven", "modulePath1");
    verify(moduleScanRequest1Spy).addConsumedFile(
        new File("path2\\org\\apache\\httpcomponents\\httpclient\\4.5.13\\httpclient-4.5.13.jar"),
        "org.apache.httpcomponents:httpclient:jar:4.5.13"
    );
    verify(moduleScanRequest1Spy).addConsumedFile(
        new File("path2\\org\\apache\\httpcomponents\\httpcore\\4.4.13\\httpcore-4.4.13.jar"),
        "org.apache.httpcomponents:httpcore:jar:4.4.13"
    );
    verify(moduleScanRequest1Spy).addConsumedFile(
        new File("path2\\commons-logging\\commons-logging\\1.2\\commons-logging-1.2.jar"),
        "commons-logging:commons-logging:jar:1.2"
    );
    verify(moduleScanRequest1Spy).addConsumedFile(
        new File("path2\\commons-codec\\commons-codec\\1.11\\commons-codec-1.11.jar"),
        "commons-codec:commons-codec:jar:1.11"
    );
    verify(moduleScanRequest1Spy, times(4)).addConsumedFile(any(), any());
    verify(moduleScanRequest1Spy).addDependency(
        "org.apache.httpcomponents:httpclient:jar:4.5.13",
        true,
        Arrays.asList("org.apache.httpcomponents:httpcore:jar:4.4.13", "commons-logging:commons-logging:jar:1.2")
    );
    verify(moduleScanRequest1Spy).addDependency(
        "org.apache.httpcomponents:httpcore:jar:4.4.13",
        false,
        Collections.emptyList()
    );
    verify(moduleScanRequest1Spy).addDependency(
        "commons-logging:commons-logging:jar:1.2",
        false,
        Collections.emptyList()
    );
    verify(moduleScanRequest1Spy).addDependency(
        "commons-codec:commons-codec:jar:1.11",
        true,
        Collections.emptyList()
    );

    verify(mockFileScanner).scan(moduleScanRequest1Spy);
    ModuleScanRequest moduleScanRequest2Spy = scanRequests.get(1);
    assertThat(moduleScanRequest2Spy.getScanSession()).isEqualTo(scanSession);
    verify(moduleScanRequest2Spy).setBasedir(baseDirectory);
    verify(moduleScanRequest2Spy).setModule("org.example:test2:jar:1.0-SNAPSHOT", "maven", "modulePath2");
    verify(moduleScanRequest2Spy).addConsumedFile(
        new File("path2\\com\\novell\\ldap\\jldap\\2009-10-07\\jldap-2009-10-07.jar"),
        "com.novell.ldap:jldap:jar:2009-10-07"
    );
    verify(moduleScanRequest2Spy, times(1)).addConsumedFile(any(), any());
    verify(moduleScanRequest2Spy).addDependency(
        "com.novell.ldap:jldap:jar:2009-10-07",
        true,
        Collections.emptyList()
    );
    verify(mockFileScanner).scan(moduleScanRequest2Spy);
  }

  @Test
  public void testScanModules_multimodule() throws Exception {
    Scanner scannerSpy = spy(scanner);
    List<File> moduleIndices = Arrays.asList(
        new File("src/test/resources/ScannerTest/test2/test/target-test/module.xml"),
        new File("src/test/resources/ScannerTest/test2/target-test/module.xml")
    );
    ScanSession scanSession = new ScanSession(null, null);
    File baseDirectory = new File("baseDirectory");
    FileScanner mockFileScanner = mock(FileScanner.class);
    List<ModuleScanRequest> scanRequests = new ArrayList<>();
    doAnswer(invocationOnMock -> {
      ModuleScanRequest moduleScanRequestSpy = spy(new ModuleScanRequest(invocationOnMock.getArgument(0)));
      scanRequests.add(moduleScanRequestSpy);
      return moduleScanRequestSpy;
    }).when(scannerSpy).createModuleScanRequest(any(ScanSession.class));

    scannerSpy.scanModules(moduleIndices, scanSession, baseDirectory, mockFileScanner);

    assertThat(scanRequests).hasSize(2);
    ModuleScanRequest moduleScanRequest1Spy = scanRequests.get(0);
    assertThat(moduleScanRequest1Spy.getScanSession()).isEqualTo(scanSession);
    verify(moduleScanRequest1Spy).setBasedir(baseDirectory);
    verify(moduleScanRequest1Spy).setModule("org.example:test:jar:1.0-SNAPSHOT", "maven", "test2/modulePath1");

    verify(mockFileScanner).scan(moduleScanRequest1Spy);
    ModuleScanRequest moduleScanRequest2Spy = scanRequests.get(1);
    assertThat(moduleScanRequest2Spy.getScanSession()).isEqualTo(scanSession);
    verify(moduleScanRequest2Spy).setBasedir(baseDirectory);
    verify(moduleScanRequest2Spy).setModule("org.example:test2:jar:1.0-SNAPSHOT", "maven", "test2/test/modulePath2");
    verify(moduleScanRequest2Spy).addConsumedFile(
        new File("path2\\com\\novell\\ldap\\jldap\\2009-10-07\\jldap-2009-10-07.jar"),
        "com.novell.ldap:jldap:jar:2009-10-07"
    );
    verify(moduleScanRequest2Spy, times(1)).addConsumedFile(any(), any());
    verify(moduleScanRequest2Spy).addDependency(
        "com.novell.ldap:jldap:jar:2009-10-07",
        true,
        Collections.emptyList()
    );
    verify(mockFileScanner).scan(moduleScanRequest2Spy);
  }

  @Test
  public void testScanModules_multimodule_order() throws Exception {
    Scanner scannerSpy = spy(scanner);
    List<File> moduleIndices = Arrays.asList(
        new File("src/test/resources/ScannerTest/test2/target-test/module.xml"),
        new File("src/test/resources/ScannerTest/test2/test/target-test/module.xml")
    );
    ScanSession scanSession = new ScanSession(null, null);
    File baseDirectory = new File("baseDirectory");
    FileScanner mockFileScanner = mock(FileScanner.class);
    List<ModuleScanRequest> scanRequests = new ArrayList<>();
    doAnswer(invocationOnMock -> {
      ModuleScanRequest moduleScanRequestSpy = spy(new ModuleScanRequest(invocationOnMock.getArgument(0)));
      scanRequests.add(moduleScanRequestSpy);
      return moduleScanRequestSpy;
    }).when(scannerSpy).createModuleScanRequest(any(ScanSession.class));

    scannerSpy.scanModules(moduleIndices, scanSession, baseDirectory, mockFileScanner);

    assertThat(scanRequests).hasSize(2);
    ModuleScanRequest moduleScanRequest1Spy = scanRequests.get(0);
    assertThat(moduleScanRequest1Spy.getScanSession()).isEqualTo(scanSession);
    verify(moduleScanRequest1Spy).setBasedir(baseDirectory);
    verify(moduleScanRequest1Spy).setModule("org.example:test:jar:1.0-SNAPSHOT", "maven", "test2/modulePath1");

    verify(mockFileScanner).scan(moduleScanRequest1Spy);
    ModuleScanRequest moduleScanRequest2Spy = scanRequests.get(1);
    assertThat(moduleScanRequest2Spy.getScanSession()).isEqualTo(scanSession);
    verify(moduleScanRequest2Spy).setBasedir(baseDirectory);
    verify(moduleScanRequest2Spy).setModule("org.example:test2:jar:1.0-SNAPSHOT", "maven", "test2/test/modulePath2");
    verify(moduleScanRequest2Spy).addConsumedFile(
        new File("path2\\com\\novell\\ldap\\jldap\\2009-10-07\\jldap-2009-10-07.jar"),
        "com.novell.ldap:jldap:jar:2009-10-07"
    );
    verify(moduleScanRequest2Spy, times(1)).addConsumedFile(any(), any());
    verify(moduleScanRequest2Spy).addDependency(
        "com.novell.ldap:jldap:jar:2009-10-07",
        true,
        Collections.emptyList()
    );
    verify(mockFileScanner).scan(moduleScanRequest2Spy);
  }
}
