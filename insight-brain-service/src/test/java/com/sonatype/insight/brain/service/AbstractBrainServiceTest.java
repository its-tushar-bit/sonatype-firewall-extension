/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import javax.ws.rs.core.UriBuilder;

import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.db.DataSourceFactory;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.mock.InsightMockServer;

import com.google.inject.AbstractModule;
import com.ning.http.client.Response;
import org.apache.shiro.web.filter.mgt.DefaultFilterChainManager;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.IOUtil;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.TestName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.assertEquals;

public abstract class AbstractBrainServiceTest
{
  static {
    System.setProperty("javax.net.ssl.trustStore", "src/test/resources/ssl/server-store");
  }

  private static final Logger log = LoggerFactory.getLogger(AbstractBrainServiceTest.class);

  private static int saasPort = findFreePort(8090);

  private static int brainPort = findFreePort(8070);

  private static int brainAdminPort = findFreePort(8071);

  private static File saasWork = new File("target/mock-saas-work/");

  protected InsightMockServer saas;

  protected TestInsightBrainService brain;

  protected Set<Application> applicationsToDelete = new LinkedHashSet<Application>();

  protected Set<Organization> organizationsToDelete = new LinkedHashSet<Organization>();

  @Rule
  public TestName testName = new TestName();

  private final boolean disableSecurity;

  public AbstractBrainServiceTest() {
    this(false /*disableSecurity*/);
  }

  // To be removed when we implement auth for clients
  public AbstractBrainServiceTest(boolean disableSecurity) {
    this.disableSecurity = disableSecurity;
  }

  @AfterClass
  public static void afterClass() {
    DataSourceFactory.clear_ForTestsOnly();
  }

  @Before
  public void startService() throws Exception {
    long start = System.currentTimeMillis();

    if (saas == null) {
      log.debug("Starting InsightMockServer on port {}", saasPort);
      saas = new InsightMockServer();
      saas.setHttpPort(saasPort);
      saas.setJsonResponseDirectory(getJsonResponseDirectory());
      saas.setZipResponseDirectory(getZipResponseDirectory());
      if (isProxyRequiredToReachSaas()) {
        saas.setKeyStore(System.getProperty("javax.net.ssl.trustStore"), "server-pwd");
        saas.setProxyAuthentication("proxyuser", "proxypass");
      }
      configureSaas(saas);
      saas.start();
    }
    log.debug("Started InsightMockServer in {}", System.currentTimeMillis() - start);

    start = System.currentTimeMillis();
    if (brain == null) {
      log.debug("Starting TestInsightBrainService on port {}, admin port {}", brainPort, brainAdminPort);
      brain = new TestInsightBrainService();
      brain.setHttpPort(brainPort);
      brain.setHttpAdminPort(brainAdminPort);
      brain.setSaasAddress(saas.getHttpUrl());
      if (isProxyRequiredToReachSaas()) {
        brain.setProxyConfig("127.0.0.1", saasPort, "proxyuser", "proxypass");
      }
      configureBrain(brain);
      brain.start();
    }

    log.debug("Started TestInsightBrainService in {}", System.currentTimeMillis() - start);
  }

  protected void configureBrain(final TestInsightBrainService brain) {
    if (disableSecurity) {
      brain.addModule(new AbstractModule()
      {
        @Override
        protected void configure() {
          DefaultFilterChainManager manager = new DefaultFilterChainManager();
          manager.createChain("/**", "anon");
          bind(DefaultFilterChainManager.class).toInstance(manager);
        }
      });
    }
  }

  protected void configureSaas(final InsightMockServer saas) {
    // hook for sub classes
  }

  protected boolean isProxyRequiredToReachSaas() {
    return getClass().getName().endsWith("ProxyTest");
  }

  @After
  public void stopService() throws Exception {
    long start = System.currentTimeMillis();

    cleanupApplications();
    cleanupOrganizations();

    if (brain != null) {
      brain.stop();
      brain = null;
    }
    if (saas != null) {
      saas.stop();
      saas = null;
    }

    log.debug("Stopped test servers in {}", System.currentTimeMillis() - start);
  }

  protected static File getJsonResponseDirectory() {
    return new File(saasWork, "json");
  }

  protected static File getZipResponseDirectory() {
    return new File(saasWork, "zip");
  }

  protected static File getScanResponseFile(final String licenseFingerprint) {
    return new File(getJsonResponseDirectory(), licenseFingerprint + ".json");
  }

  protected static File getReportResponseFile(final String licenseFingerprint, final String scanId) {
    return new File(getZipResponseDirectory(), licenseFingerprint + '-' + scanId + ".zip");
  }

  protected static int findFreePort(final int defaultPort) {
    int port = defaultPort;
    ServerSocket socket = null;
    try {
      socket = new ServerSocket(0);
      port = socket.getLocalPort();
    }
    catch (final IOException e) {
      e.printStackTrace();
    }
    finally {
      if (socket != null) {
        try {
          socket.close();
        }
        catch (final IOException e) {
          e.printStackTrace();
        }
      }
    }
    return port;
  }

  protected String getRestBaseUrl() {
    String restBaseUrl = brain.getClientConfiguration().getServerUrl();
    if (!restBaseUrl.endsWith("/")) {
      restBaseUrl = restBaseUrl + "/";
    }
    return restBaseUrl;
  }

  protected String expandRestUrl(String templateUrl, Object... paramValues) {
    return UriBuilder.fromPath(templateUrl).build(paramValues).toString();
  }

  protected void setSaasResponseForURI(String uri, int status, Object body) {
    saas.setResponseForURI(uri, body, status);
  }

  protected void setSaasResponseForURI(String uri, String body, int status) {
    saas.setResponseForURI(uri, body, status);
  }

  protected void setSaasResponseForURI(String uri, int status, String bodyResource) {
    setSaasResponseForURI(uri, toString(bodyResource), status);
  }

  protected void setSecurityAuditLog(String appId, String jsonResource) {
    setAuditLog(appId, "security.json", jsonResource);
  }

  private void setAuditLog(String appId, String jsonFile, String jsonResource) {
    File logFile = new File(brain.getAuditDir(appId), jsonFile);
    logFile.getAbsoluteFile().getParentFile().mkdirs();
    try {
      FileUtils.fileWrite(logFile, "UTF-8", toString(jsonResource));
    }
    catch (IOException e) {
      throw new IllegalStateException(e);
    }
  }

  private String toString(String resource) {
    try {
      return IOUtil.toString(getClass().getResourceAsStream(resource), "UTF-8");
    }
    catch (IOException e) {
      throw new IllegalStateException(e);
    }
  }

  protected Application createApplication(String publicId) {
    return createApplication(publicId, true /* createLicenseThreatGroups */);
  }

  protected Application createApplication(String publicId, boolean createLicenseThreatGroups) {
    // Application Name must be unique
    return createApplication(publicId, "DUMMY-NAME-" + UUID.randomUUID().toString(), createLicenseThreatGroups);
  }

  protected Application createApplication(String publicId, String name) {
    return createApplication(publicId, name, true /* createLicenseThreatGroups */);
  }

  protected Application createApplication(String publicId, String name, boolean createLicenseThreatGroups) {
    return createApplication(publicId, name, createLicenseThreatGroups, true);
  }

  protected Application createApplication(String publicId, String name, Organization organization) {
    return createApplication(publicId, name, true, false, organization);
  }

  protected Application createApplication(String publicId, String name, boolean createLicenseThreatGroups,
      boolean withOrg)
  {
    return createApplication(publicId, name, createLicenseThreatGroups, withOrg, null);
  }

  protected Application createApplication(String publicId, String name, boolean createLicenseThreatGroups,
      boolean withOrg, Organization organization)
  {
    if (withOrg) {
      organization = createOrganization(name, createLicenseThreatGroups);
    }

    ApplicationDAO applicationDAO = new ApplicationDAO();
    Application application = new Application();
    application.setPublicId(publicId);
    application.setName(name);
    application.setOrganizationId(organization != null ? organization.getId() : null);
    applicationDAO.insert(application);
    applicationsToDelete.add(application);
    return application;
  }

  protected Organization createOrganization(String name) {
    return createOrganization(name, true /* createLicenseThreatGroups */);
  }

  protected Organization createOrganization(String name, boolean createLicenseThreatGroups) {
    OrganizationDAO dao = new OrganizationDAO();
    Organization organization = new Organization();
    organization.setName(name);
    dao.insert(organization, createLicenseThreatGroups);
    organizationsToDelete.add(organization);
    return organization;
  }

  private void cleanupApplications() {
    ApplicationDAO applicationDAO = new ApplicationDAO();
    for (Application application : applicationsToDelete) {
      cleanupApplication(application);
      if (application.getId() != null) {
        application = applicationDAO.getById(application.getId());
        if (application != null) {
          applicationDAO.delete(application);
        }
      }
    }
    applicationsToDelete.clear();
  }

  private void cleanupOrganizations() {
    OrganizationDAO dao = new OrganizationDAO();
    for (Organization organization : organizationsToDelete) {
      cleanupOrganization(organization);
      dao.delete(organization);
    }
    organizationsToDelete.clear();
  }

  protected void cleanupApplication(Application application) {
    PolicyDAO policyDAO = new PolicyDAO(brain.getWorkDir());
    List<Policy> policies = policyDAO.getByOwnerId(application.getId());
    for (Policy policy : policies) {
      policyDAO.delete(application.getId(), policy.getId());
    }
  }

  protected void cleanupOrganization(Organization organization) {
    PolicyDAO policyDAO = new PolicyDAO(brain.getWorkDir());
    List<Policy> policies = policyDAO.getByOwnerId(organization.getId());
    for (Policy policy : policies) {
      policyDAO.delete(organization.getId(), policy.getId());
    }
  }

  protected static void assertResponseStatus(final int expectedStatus, final Response response) throws IOException {
    final int actualStatus = response.getStatusCode();
    assertEquals(
        "URI:" + response.getUri() + ", StatusText:" + response.getStatusText() + ", ResponseBody:"
            + response.getResponseBody(), expectedStatus, actualStatus);
  }
}
