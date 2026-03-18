/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import org.junit.experimental.categories.Category;
import com.sonatype.insight.brain.common.test.SlowTest;

import java.net.HttpCookie;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.cert.Certificate;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.configuration.saml.SamlConfigurationService;
import com.sonatype.insight.brain.model.configuration.saml.SamlConfiguration;
import com.sonatype.insight.brain.model.security.Group;
import com.sonatype.insight.brain.security.UserSessionResource.AuthenticationStatus;
import com.sonatype.insight.brain.service.AbstractResourceTest;
import com.sonatype.insight.test.LogOutput;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.keycloak.saml.BaseSAML2BindingBuilder;
import org.keycloak.saml.SignatureAlgorithm;
import org.keycloak.saml.common.constants.JBossSAMLConstants;
import org.keycloak.saml.common.util.DocumentUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import static org.assertj.core.api.Assertions.assertThat;

@Category(SlowTest.class)
public class SamlAuthcTest
    extends AbstractResourceTest
{
  private static final String SIGN_DOCUMENT = "signDocument";

  private static final String SIGN_ASSERTION = "signAssertion";

  private static final String ENCRYPT_ASSERTION = "encryptAssertion";

  private SamlConfigurationService samlConfigurationService;

  private KeyPair idpSigningKeyPair;

  private String idpMetadata;

  @Rule
  public LogOutput logOutput = new LogOutput("org.keycloak", SamlFilter.class.getName());

  @After
  public void exit() throws Exception {
    restRequest().path(PublicApiPaths.SAML_CONFIG_RESOURCE_PATH_V2).delete();
  }

  @Before
  public void init() throws Exception {
    samlConfigurationService = lookup(SamlConfigurationService.class);
    SamlConfiguration samlConfig = new SamlConfiguration();
    samlConfigurationService.insert(samlConfig);
    samlConfigurationService.delete();
    idpSigningKeyPair = samlConfig.getSigningKeyPair();
    idpMetadata = newIdpMetadata(samlConfig.getCertificate());
  }

  private String newIdpMetadata(Certificate signingCert) throws Exception {
    String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>";
    xml += "<EntitiesDescriptor xmlns=\"urn:oasis:names:tc:SAML:2.0:metadata\" Name=\"urn:test\">";
    xml += "<EntityDescriptor entityID=\"http://idp.local/saml\">";
    xml += "<IDPSSODescriptor WantAuthnRequestsSigned=\"true\" "
        + "protocolSupportEnumeration=\"urn:oasis:names:tc:SAML:2.0:protocol\">";
    xml += "<KeyDescriptor>";
    xml += "<dsig:KeyInfo xmlns:dsig=\"http://www.w3.org/2000/09/xmldsig#\">";
    xml += "<dsig:X509Data>";
    xml += "<dsig:X509Certificate>";
    xml += Base64.getEncoder().encodeToString(signingCert.getEncoded());
    xml += "</dsig:X509Certificate>";
    xml += "</dsig:X509Data>";
    xml += "</dsig:KeyInfo>";
    xml += "</KeyDescriptor>";
    xml += "<NameIDFormat>urn:oasis:names:tc:SAML:1.1:nameid-format:unspecified</NameIDFormat>";
    xml += "<SingleSignOnService Binding=\"urn:oasis:names:tc:SAML:2.0:bindings:HTTP-POST\" "
        + "Location=\"http://idp.local/saml\"/>";
    xml += "</IDPSSODescriptor>";
    xml += "</EntityDescriptor>";
    xml += "</EntitiesDescriptor>";
    return xml;
  }

  private Document loadMessage(String resourceName) throws Exception {
    DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
    documentBuilderFactory.setNamespaceAware(true);
    Document document = documentBuilderFactory.newDocumentBuilder()
        .parse(getClass().getResource("/SamlAuthcTest/" + resourceName).toString());
    fixSpUrls(document);
    return document;
  }

  private Document fixSpUrls(Document document) {
    String samlEndpoint = getRestBaseUrl() + "saml";
    String entityId = getRestBaseUrl() + PublicApiPaths.SAML_CONFIG_RESOURCE_PATH_V2 + "/metadata";
    document.getDocumentElement().setAttribute("Destination", samlEndpoint);
    NodeList nodes = document.getElementsByTagNameNS(JBossSAMLConstants.AUDIENCE.getNsUri().get(),
        JBossSAMLConstants.AUDIENCE.get());
    for (int i = 0; i < nodes.getLength(); i++) {
      nodes.item(i).setTextContent(entityId);
    }
    return document;
  }

  private void finishIdpMessage(Document document, String... options) throws Exception {
    Set<String> flags = new HashSet<>(Arrays.asList(options));
    BaseSAML2BindingBuilder<BaseSAML2BindingBuilder<?>> builder = new BaseSAML2BindingBuilder<>();
    builder.signWith(null, idpSigningKeyPair);
    builder.signatureAlgorithm(SignatureAlgorithm.RSA_SHA256);
    builder.encrypt(samlConfigurationService.get().getSigningKeyPair().getPublic());

    if (flags.contains(SIGN_ASSERTION)) {
      Element assertion = DocumentUtil.getElement(document, JBossSAMLConstants.ASSERTION.getAsQName());
      if (assertion.getPrefix() != null) {
        // ensure the assertion's namespace is declared again on the assertion itself or signAssertion() will misbehave
        String namespaceAttribute = XMLConstants.XMLNS_ATTRIBUTE + ":" + assertion.getPrefix();
        if (assertion.getAttribute(namespaceAttribute).isEmpty()) {
          assertion.setAttributeNS(XMLConstants.XMLNS_ATTRIBUTE_NS_URI, namespaceAttribute,
              assertion.getNamespaceURI());
        }
      }
      builder.signAssertion(document);
    }

    if (flags.contains(ENCRYPT_ASSERTION)) {
      builder.encryptDocument(document);
    }

    if (flags.contains(SIGN_DOCUMENT)) {
      builder.signDocument(document);
    }
  }

  private HttpRequest samlRequest() {
    return restRequest().path("saml").anon();
  }

  private Consumer<HttpRequest> samlResponse(Document document) throws Exception {
    String xml = DocumentUtil.getDocumentAsString(document);
    String value = URLEncoder.encode(Base64.getEncoder().encodeToString(xml.getBytes(StandardCharsets.UTF_8)), "UTF-8");
    return httpRequest -> httpRequest.body("SAMLResponse=" + value, "application/x-www-form-urlencoded");
  }

  private void configureSaml() throws Exception {
    assertResponseStatus(204,
        restRequest().path(PublicApiPaths.SAML_CONFIG_RESOURCE_PATH_V2).part("identityProviderXml", idpMetadata).put());
  }

  @Test
  public void testLoginRequest_SessionCookieUseableForLoginResponseFromIdp() throws Exception {
    configureSaml();

    HttpResponse response = samlRequest().path("login").get();

    assertResponseStatus(200, response);
    assertThat(response.getSessionCookie()).isNotNull();
    // To redirect back to the originally requested page upon receiving the login response from the IdP, the original
    // URL is stored in a session when the SAML flow starts. To find/continue that session later, the corresponding
    // session cookie must support the cross-site POST request from the IdP though.
    assertThat(response.getHeader("Set-Cookie")).contains(response.getSessionCookie().getName())
        .doesNotContainIgnoringCase("SameSite=Lax", "SameSite=Strict");
  }

  @Test
  public void testLoginResponse_FullySignedAndEncrypted() throws Exception {
    configureSaml();

    Document doc = loadMessage("login-response.xml");
    finishIdpMessage(doc, SIGN_DOCUMENT, SIGN_ASSERTION, ENCRYPT_ASSERTION);

    HttpResponse response = samlRequest().with(samlResponse(doc)).post();

    assertResponseStatus(302, response);
    assertThat(response.getHeader("Location")).isEqualTo(getRestBaseUrl());
    HttpCookie sessionCookie = response.getSessionCookie();
    assertThat(sessionCookie).isNotNull();

    response = restRequest().path(UserSessionResource.RESOURCE_PATH).anon().cookie(sessionCookie).get();
    assertResponseStatus(200, response);
    AuthenticationStatus authStatus = response.getBody(AuthenticationStatus.class);
    assertThat(authStatus.getUsername()).isEqualTo("username-attribute");
    assertThat(authStatus.isInternalUser()).isFalse();
    assertThat(authStatus.getGroups()).containsExactlyInAnyOrder(Group.AUTHENTICATED_USERS_GROUP_ID, "group-attribute");
  }

  @Test
  public void testLoginResponse_InvalidDestinationUri() throws Exception {
    configureSaml();

    Document doc = loadMessage("login-response.xml");
    doc.getDocumentElement().setAttribute("Destination", "\"invalid-destination-uri");
    finishIdpMessage(doc, SIGN_DOCUMENT, SIGN_ASSERTION);

    HttpResponse response = samlRequest().with(samlResponse(doc)).post();

    assertResponseStatus(500, response);
    assertThat(response.getBodyText()).isEqualTo(SamlFilter.MSG_SAML_FAILURE);
    assertThat(logOutput).atErrorLevel().containsPattern("Request URI .* does not match SAML request destination");
  }

  @Test
  public void testLoginResponse_MismatchingDestination() throws Exception {
    configureSaml();

    Document doc = loadMessage("login-response.xml");
    doc.getDocumentElement().setAttribute("Destination", "mismatching-destination");
    finishIdpMessage(doc, SIGN_DOCUMENT, SIGN_ASSERTION);

    HttpResponse response = samlRequest().with(samlResponse(doc)).post();

    assertResponseStatus(500, response);
    assertThat(response.getBodyText()).isEqualTo(SamlFilter.MSG_SAML_FAILURE);
    assertThat(logOutput).atErrorLevel().containsPattern("Request URI .* does not match SAML request destination");
  }

  @Test
  public void testLoginResponse_InvalidAudienceUri() throws Exception {
    configureSaml();

    Document doc = loadMessage("login-response.xml");
    Element audience = DocumentUtil.getElement(doc, JBossSAMLConstants.AUDIENCE.getAsQName());
    audience.setTextContent("\"invalid-audience-uri");
    finishIdpMessage(doc, SIGN_DOCUMENT, SIGN_ASSERTION);

    HttpResponse response = samlRequest().with(samlResponse(doc)).post();

    assertResponseStatus(500, response);
    assertThat(response.getBodyText()).isEqualTo(SamlFilter.MSG_SAML_FAILURE);
    assertThat(logOutput).atErrorLevel().containsPattern("Invalid SAML message.*invalid-audience-uri");
  }

  @Test
  public void testLoginResponse_MismatchingAudience() throws Exception {
    configureSaml();

    Document doc = loadMessage("login-response.xml");
    Element audience = DocumentUtil.getElement(doc, JBossSAMLConstants.AUDIENCE.getAsQName());
    audience.setTextContent("mismatching-audience");
    finishIdpMessage(doc, SIGN_DOCUMENT, SIGN_ASSERTION);

    HttpResponse response = samlRequest().with(samlResponse(doc)).post();

    assertResponseStatus(200, response);
    assertThat(response.getBodyText()).contains("<INPUT TYPE=\"HIDDEN\" NAME=\"SAMLRequest\"");
    assertThat(logOutput).atInfoLevel().containsPattern("Assertion .* is not addressed to this SP");
  }

  @Test
  public void testLoginResponse_MissingResponseSignature() throws Exception {
    configureSaml();

    Document doc = loadMessage("login-response.xml");
    finishIdpMessage(doc);

    HttpResponse response = samlRequest().with(samlResponse(doc)).post();

    assertResponseStatus(500, response);
    assertThat(response.getBodyText()).isEqualTo(SamlFilter.MSG_SAML_FAILURE);
    assertThat(logOutput).atErrorLevel().contains("Failed to verify saml response signature");
  }

  @Test
  public void testLoginResponse_MissingAssertionSignature() throws Exception {
    configureSaml();

    Document doc = loadMessage("login-response.xml");
    finishIdpMessage(doc, SIGN_DOCUMENT);

    HttpResponse response = samlRequest().with(samlResponse(doc)).post();

    assertResponseStatus(500, response);
    assertThat(response.getBodyText()).isEqualTo(SamlFilter.MSG_SAML_FAILURE);
    assertThat(logOutput).atErrorLevel().contains("Failed to verify saml assertion signature");
  }

  @Test
  public void testLoginResponse_AssertionOutOfTimeBounds() throws Exception {
    configureSaml();

    Document doc = loadMessage("login-response.xml");
    Element conditions = DocumentUtil.getElement(doc, JBossSAMLConstants.CONDITIONS.getAsQName());
    conditions.setAttribute("NotOnOrAfter", "2019-10-18T14:18:08.123Z");
    finishIdpMessage(doc, SIGN_DOCUMENT, SIGN_ASSERTION);

    HttpResponse response = samlRequest().with(samlResponse(doc)).post();

    assertResponseStatus(200, response);
    assertThat(response.getBodyText()).contains("<INPUT TYPE=\"HIDDEN\" NAME=\"SAMLRequest\"");
    assertThat(logOutput).atInfoLevel().containsPattern("Assertion .* expired");
    assertThat(logOutput).atDebugLevel()
        .containsPattern(
            "Conditions of Assertion .* notBefore=2019-10-17T14:17:08\\.098Z.*notOnOrAfter=2019-10-18T14:18:08\\.123Z");
  }
}
