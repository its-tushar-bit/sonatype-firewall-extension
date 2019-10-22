/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

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
import com.sonatype.insight.brain.dataaccess.configuration.saml.SamlConfigurationDAO;
import com.sonatype.insight.brain.model.configuration.saml.SamlConfiguration;
import com.sonatype.insight.brain.model.security.Group;
import com.sonatype.insight.brain.security.UserSessionResource.AuthenticationStatus;
import com.sonatype.insight.brain.service.AbstractResourceTest;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.keycloak.saml.BaseSAML2BindingBuilder;
import org.keycloak.saml.common.constants.JBossSAMLConstants;
import org.keycloak.saml.common.util.DocumentUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import static org.assertj.core.api.Assertions.assertThat;

public class SamlAuthcTest
    extends AbstractResourceTest
{
  private static final String SIGN_DOCUMENT = "signDocument";

  private static final String SIGN_ASSERTION = "signAssertion";

  private static final String ENCRYPT_ASSERTION = "encryptAssertion";

  private KeyPair idpSigningKeyPair;

  private String idpMetadata;

  @After
  public void exit() throws Exception {
    restRequest().path(PublicApiPaths.SAML_CONFIG_RESOURCE_PATH_V2).delete();
  }

  @Before
  public void init() throws Exception {
    SamlConfigurationDAO samlConfigDAO = new SamlConfigurationDAO();
    SamlConfiguration samlConfig = new SamlConfiguration();
    samlConfigDAO.insert(samlConfig);
    samlConfigDAO.delete(samlConfig);
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

  private Document fixSpUrls(Document document) throws Exception {
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
    builder.encrypt(new SamlConfigurationDAO().get().getSigningKeyPair().getPublic());

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

  protected Consumer<HttpRequest> samlResponse(Document document) throws Exception {
    String xml = DocumentUtil.getDocumentAsString(document);
    String value = URLEncoder.encode(Base64.getEncoder().encodeToString(xml.getBytes(StandardCharsets.UTF_8)), "UTF-8");
    return httpRequest -> httpRequest.body("SAMLResponse=" + value, "application/x-www-form-urlencoded");
  }

  private void configureSaml() throws Exception {
    assertResponseStatus(204,
        restRequest().path(PublicApiPaths.SAML_CONFIG_RESOURCE_PATH_V2).part("identityProviderXml", idpMetadata).put());
  }

  @Test
  public void testLoginResponse_FullySignedAndEncrypted() throws Exception {
    configureSaml();

    Document doc = loadMessage("login-response.xml");
    finishIdpMessage(doc, SIGN_DOCUMENT, SIGN_ASSERTION, ENCRYPT_ASSERTION);

    HttpResponse response = restRequest().path("saml").with(samlResponse(doc)).post();

    assertResponseStatus(302, response);
    assertThat(response.getHeader("Location")).isEqualTo(getRestBaseUrl());
    HttpCookie sessionCookie = response.getSessionCookie();
    assertThat(sessionCookie).isNotNull();

    response = restRequest().path(UserSessionResource.RESOURCE_PATH).anon().cookie(sessionCookie).get();
    assertResponseStatus(200, response);
    AuthenticationStatus authStatus = response.getBody(AuthenticationStatus.class);
    assertThat(authStatus.getUsername()).isEqualTo("username-attribute");
    assertThat(authStatus.getGroups()).containsExactlyInAnyOrder(Group.AUTHENTICATED_USERS_GROUP_ID, "group-attribute");
  }
}
