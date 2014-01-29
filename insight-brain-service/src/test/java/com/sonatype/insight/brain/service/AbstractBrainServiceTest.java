/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
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
import com.sonatype.insight.brain.dataaccess.tag.PolicyTagDAO;
import com.sonatype.insight.brain.db.DataSourceFactory;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.tag.PolicyTag;
import com.sonatype.insight.mock.InsightMockServer;

import com.ning.http.client.Cookie;
import com.ning.http.client.Response;
import com.yammer.dropwizard.testing.JsonHelpers;
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
import static org.junit.Assert.fail;

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

  private String expandRestUrl(String templateUrl, Object... paramValues) {
    return UriBuilder.fromPath(templateUrl).build(paramValues).toString();
  }

  protected String getRestUrl(String templateUrl, Object... paramValues) {
    return getRestBaseUrl() + expandRestUrl(templateUrl, paramValues);
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
    Organization org = createOrganization(name);
    return createApplication(publicId, name, createLicenseThreatGroups, org);
  }

  protected Application createApplication(String publicId, String name, Organization organization) {
    return createApplication(publicId, name, true, organization);
  }

  protected Application createApplication(String publicId, String name, boolean createLicenseThreatGroups,
      Organization organization)
  {

    return createApplication(publicId, name, createLicenseThreatGroups, organization, null);
  }

  protected Application createApplication(String publicId, String name, boolean createLicenseThreatGroups,
      Organization organization, String contactInternalName)
  {
    ApplicationDAO applicationDAO = new ApplicationDAO();
    Application application = new Application();
    application.setPublicId(publicId);
    application.setName(name);
    application.setOrganizationId(organization.getId());
    application.setContactInternalName(contactInternalName);
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
      PolicyTagDAO policyTagDAO = new PolicyTagDAO();
      List<PolicyTag> policyTags = policyTagDAO.getByPolicyId(policy.getId());
      for(PolicyTag policyTag : policyTags) {
        policyTagDAO.delete(policyTag);
      }
      policyDAO.delete(organization.getId(), policy.getId());
    }
  }

  protected static void assertResponseStatus(final int expectedStatus, final Response response) throws IOException {
    final int actualStatus = response.getStatusCode();
    assertEquals(
        "URI:" + response.getUri() + ", StatusText:" + response.getStatusText() + ", ResponseBody:"
            + response.getResponseBody(), expectedStatus, actualStatus);
  }

  protected Cookie extractSessionCookie(final Response response) {
    for (final Cookie cookie : response.getCookies()) {
      if ("JSESSIONID".equals(cookie.getName())) {
        return cookie;
      }
    }

    fail("Missing session cookie");
    return null;
  }

  protected String toJson(Object object) {
    try {
      return JsonHelpers.asJson(object);
    }
    catch (IOException e) {
      throw new IllegalStateException(e);
    }
  }

  protected <T> T fromJson(Response response, Class<T> type) {
    try {
      return JsonHelpers.fromJson(response.getResponseBody(), type);
    }
    catch (IOException e) {
      throw new IllegalStateException(e);
    }
  }
}
