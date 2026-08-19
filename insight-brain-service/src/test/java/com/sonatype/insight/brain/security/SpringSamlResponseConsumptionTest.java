/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;

import com.sonatype.insight.brain.model.configuration.saml.SamlConfiguration;
import com.sonatype.insight.brain.security.certificate.CertificateFactory;
import com.sonatype.insight.brain.service.BaseUrl;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opensaml.core.xml.XMLObjectBuilderFactory;
import org.opensaml.core.xml.config.XMLObjectProviderRegistrySupport;
import org.opensaml.core.xml.io.Marshaller;
import org.opensaml.core.xml.schema.XSString;
import org.opensaml.core.xml.schema.impl.XSStringBuilder;
import org.opensaml.saml.common.SignableSAMLObject;
import org.opensaml.saml.saml2.core.Assertion;
import org.opensaml.saml.saml2.core.Attribute;
import org.opensaml.saml.saml2.core.AttributeStatement;
import org.opensaml.saml.saml2.core.AttributeValue;
import org.opensaml.saml.saml2.core.Audience;
import org.opensaml.saml.saml2.core.AudienceRestriction;
import org.opensaml.saml.saml2.core.AuthnContext;
import org.opensaml.saml.saml2.core.AuthnContextClassRef;
import org.opensaml.saml.saml2.core.AuthnStatement;
import org.opensaml.saml.saml2.core.Conditions;
import org.opensaml.saml.saml2.core.Issuer;
import org.opensaml.saml.saml2.core.NameID;
import org.opensaml.saml.saml2.core.Response;
import org.opensaml.saml.saml2.core.Status;
import org.opensaml.saml.saml2.core.StatusCode;
import org.opensaml.saml.saml2.core.Subject;
import org.opensaml.saml.saml2.core.SubjectConfirmation;
import org.opensaml.saml.saml2.core.SubjectConfirmationData;
import org.opensaml.security.x509.BasicX509Credential;
import org.opensaml.xmlsec.signature.Signature;
import org.opensaml.xmlsec.signature.support.SignatureConstants;
import org.opensaml.xmlsec.signature.support.Signer;
import org.springframework.security.core.Authentication;
import org.springframework.security.saml2.provider.service.authentication.OpenSaml5AuthenticationProvider;
import org.springframework.security.saml2.provider.service.authentication.Saml2AuthenticatedPrincipal;
import org.springframework.security.saml2.provider.service.authentication.Saml2AuthenticationException;
import org.springframework.security.saml2.provider.service.authentication.Saml2AuthenticationToken;
import org.springframework.security.saml2.provider.service.authentication.AbstractSaml2AuthenticationRequest;
import org.springframework.security.saml2.provider.service.authentication.Saml2PostAuthenticationRequest;
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistration;
import org.w3c.dom.Element;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Exercises the IdP&rarr;SP response-consumption half of SAML login (CLM-42790): an in-JVM IdP builds a
 * genuine RSA-SHA256 <em>signed</em> SAML {@code Response} which is validated by the real
 * {@link OpenSaml5AuthenticationProvider} using a {@link RelyingPartyRegistration} built by
 * {@link SamlRelyingPartyRegistrationResolver} from a {@link SamlConfiguration}. This complements the
 * outbound (SP-initiated AuthnRequest) coverage; no Docker or external IdP is required.
 *
 * <p>
 * It also covers the signature policy wired into {@link SpringSamlAuthenticatingFilter}: by default (no
 * explicit flags) both the response and the assertion must be signed when the IdP publishes a signing key,
 * and the {@code validateResponseSignature}/{@code validateAssertionSignature} flags relax those
 * requirements independently.
 */
public class SpringSamlResponseConsumptionTest
{
  private static final String SP_ENTITY_ID = "http://localhost:8072/api/v2/config/saml/metadata";

  private static final String ACS_URL = "http://localhost:8072/saml";

  private static final String IDP_ENTITY_ID = "http://idp.local/saml";

  private KeyPair idpKeyPair;

  private X509Certificate idpCertificate;

  @BeforeAll
  public static void initOpenSaml() {
    // Referencing the provider triggers Spring's OpenSAML initialization.
    new OpenSaml5AuthenticationProvider();
  }

  @BeforeEach
  public void generateIdpKeys() throws Exception {
    KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
    keyPairGenerator.initialize(2048);
    idpKeyPair = keyPairGenerator.generateKeyPair();
    idpCertificate = CertificateFactory.createCertificate(idpKeyPair)[0];
  }

  @Test
  public void testFullySignedResponse_IsConsumedAndMappedToPrincipal() throws Exception {
    String responseXml = buildResponse(true, true, defaultExpiry());

    Authentication authentication = authenticate(responseXml, null, null);
    Saml2AuthenticatedPrincipal principal = (Saml2AuthenticatedPrincipal) authentication.getPrincipal();

    assertThat(principal.getName()).isEqualTo("subject-name");

    SpringSamlPrincipal springPrincipal = new SpringSamlPrincipal(principal);
    assertThat(springPrincipal.getAttribute("username")).isEqualTo("jsmith");
    assertThat(springPrincipal.getAttributes("groups")).containsExactly("admins", "devs");
    // FriendlyName ("displayName") resolves even though the assertion keys it by a formal urn:oid Name.
    assertThat(springPrincipal.getAttribute("displayName")).isEqualTo("John Smith");
  }

  @Test
  public void testDefault_RejectsResponseWithUnsignedResponseElement() throws Exception {
    // Only the assertion is signed; with the default policy (IdP has a signing key) the response signature
    // is also required, matching the pre-migration behavior.
    String responseXml = buildResponse(false, true, defaultExpiry());

    assertThatThrownBy(() -> authenticate(responseXml, null, null))
        .isInstanceOf(Saml2AuthenticationException.class)
        .hasMessageContaining("response is not signed");
  }

  @Test
  public void testDefault_RejectsResponseWithUnsignedAssertion() throws Exception {
    // Only the response is signed; the default policy also requires the assertion signature.
    String responseXml = buildResponse(true, false, defaultExpiry());

    assertThatThrownBy(() -> authenticate(responseXml, null, null))
        .isInstanceOf(Saml2AuthenticationException.class)
        .hasMessageContaining("assertion is not signed");
  }

  @Test
  public void testDefault_RejectsFullyUnsignedResponse() throws Exception {
    String responseXml = buildResponse(false, false, defaultExpiry());

    assertThatThrownBy(() -> authenticate(responseXml, null, null))
        .isInstanceOf(Saml2AuthenticationException.class);
  }

  @Test
  public void testValidateResponseSignatureFalse_AcceptsAssertionOnlySigning() throws Exception {
    // Disabling response-signature validation lets an IdP that signs only the assertion authenticate.
    String responseXml = buildResponse(false, true, defaultExpiry());

    Authentication authentication = authenticate(responseXml, false, null);

    assertThat(authentication.getPrincipal()).isInstanceOf(Saml2AuthenticatedPrincipal.class);
  }

  @Test
  public void testValidateAssertionSignatureFalse_AcceptsResponseOnlySigning() throws Exception {
    // Disabling assertion-signature validation lets an IdP that signs only the response authenticate.
    String responseXml = buildResponse(true, false, defaultExpiry());

    Authentication authentication = authenticate(responseXml, null, false);

    assertThat(authentication.getPrincipal()).isInstanceOf(Saml2AuthenticatedPrincipal.class);
  }

  @Test
  public void testTamperedSignedResponse_IsRejected() throws Exception {
    // Mutating an attribute value inside the signed assertion invalidates the signature.
    String responseXml = buildResponse(true, true, defaultExpiry()).replace("jsmith", "attacker");

    assertThatThrownBy(() -> authenticate(responseXml, null, null))
        .isInstanceOf(Saml2AuthenticationException.class);
  }

  @Test
  public void testExpiredAssertion_IsRejected() throws Exception {
    // NotOnOrAfter well in the past (beyond the 5-minute clock skew) must fail condition validation.
    String responseXml = buildResponse(true, true, Instant.now().minus(10, ChronoUnit.MINUTES));

    assertThatThrownBy(() -> authenticate(responseXml, null, null))
        .isInstanceOf(Saml2AuthenticationException.class);
  }

  @Test
  public void testFriendlyNameAttribute_FoldedUnderBothFormalAndFriendlyNames() throws Exception {
    // foldFriendlyNames aliases a formal-Name-keyed attribute under its FriendlyName without dropping the formal
    // key, so an attribute mapping referencing either name resolves. This is the only path exercising that folding.
    Authentication authentication = authenticate(buildResponse(true, true, defaultExpiry()), null, null);
    SpringSamlPrincipal principal =
        new SpringSamlPrincipal((Saml2AuthenticatedPrincipal) authentication.getPrincipal());
    assertThat(principal.getAttribute("displayName")).isEqualTo("John Smith");
    assertThat(principal.getAttribute("urn:oid:2.16.840.1.113730.3.1.241")).isEqualTo("John Smith");
  }

  @Test
  public void testInResponseTo_MatchingSavedAuthnRequest_IsAccepted() throws Exception {
    // When the SP saved an AuthnRequest, a response whose InResponseTo matches that request id is accepted.
    String requestId = "_authn_request_id";
    Authentication authentication =
        authenticateWithSavedRequest(buildResponse(true, true, defaultExpiry(), requestId), requestId);
    assertThat(authentication.getPrincipal()).isInstanceOf(Saml2AuthenticatedPrincipal.class);
  }

  @Test
  public void testInResponseTo_MismatchedSavedAuthnRequest_IsRejected() throws Exception {
    // The response answers a different AuthnRequest id than the SP saved: the InResponseTo anti-replay/anti-CSRF
    // correlation must reject it (a forged or unsolicited response cannot satisfy the saved request).
    String responseXml = buildResponse(true, true, defaultExpiry(), "_some_other_request_id");
    assertThatThrownBy(() -> authenticateWithSavedRequest(responseXml, "_saved_request_id"))
        .isInstanceOf(Saml2AuthenticationException.class);
  }

  private Authentication authenticate(
      String responseXml,
      Boolean validateResponseSignature,
      Boolean validateAssertionSignature)
  {
    SamlConfiguration samlConfiguration = new SamlConfiguration();
    samlConfiguration.setEntityId(SP_ENTITY_ID);
    samlConfiguration.setIdentityProviderMetadataXml(idpMetadata(idpCertificate));
    samlConfiguration.setValidateResponseSignature(validateResponseSignature);
    samlConfiguration.setValidateAssertionSignature(validateAssertionSignature);

    SamlConfigurationCache cache = mock(SamlConfigurationCache.class);
    when(cache.get()).thenReturn(samlConfiguration);

    RelyingPartyRegistration registration =
        new SamlRelyingPartyRegistrationResolver(cache, mock(BaseUrl.class)).build(samlConfiguration, ACS_URL);
    Saml2AuthenticationToken token = new Saml2AuthenticationToken(registration, responseXml);
    return SpringSamlAuthenticatingFilter.createAuthenticationProvider(cache).authenticate(token);
  }

  private Authentication authenticateWithSavedRequest(String responseXml, String savedRequestId) {
    SamlConfiguration samlConfiguration = new SamlConfiguration();
    samlConfiguration.setEntityId(SP_ENTITY_ID);
    samlConfiguration.setIdentityProviderMetadataXml(idpMetadata(idpCertificate));

    SamlConfigurationCache cache = mock(SamlConfigurationCache.class);
    when(cache.get()).thenReturn(samlConfiguration);

    RelyingPartyRegistration registration =
        new SamlRelyingPartyRegistrationResolver(cache, mock(BaseUrl.class)).build(samlConfiguration, ACS_URL);
    AbstractSaml2AuthenticationRequest savedRequest = Saml2PostAuthenticationRequest
        .withRelyingPartyRegistration(registration)
        .samlRequest("PHNhbWxwOkF1dGhuUmVxdWVzdD48L3NhbWxwOkF1dGhuUmVxdWVzdD4=")
        .id(savedRequestId)
        .build();
    Saml2AuthenticationToken token = new Saml2AuthenticationToken(registration, responseXml, savedRequest);
    return SpringSamlAuthenticatingFilter.createAuthenticationProvider(cache).authenticate(token);
  }

  private static Instant defaultExpiry() {
    return Instant.now().plus(5, ChronoUnit.MINUTES);
  }

  private String buildResponse(boolean signResponse, boolean signAssertion, Instant notOnOrAfter) throws Exception {
    return buildResponse(signResponse, signAssertion, notOnOrAfter, null);
  }

  private String buildResponse(
      boolean signResponse,
      boolean signAssertion,
      Instant notOnOrAfter,
      String inResponseTo) throws Exception
  {
    XMLObjectBuilderFactory builders = XMLObjectProviderRegistrySupport.getBuilderFactory();
    Instant now = Instant.now();

    Assertion assertion = build(builders, Assertion.DEFAULT_ELEMENT_NAME);
    assertion.setID("_assertion_" + java.util.UUID.randomUUID());
    assertion.setIssueInstant(now);
    assertion.setIssuer(issuer(builders));

    Subject subject = build(builders, Subject.DEFAULT_ELEMENT_NAME);
    NameID nameId = build(builders, NameID.DEFAULT_ELEMENT_NAME);
    nameId.setValue("subject-name");
    nameId.setFormat("urn:oasis:names:tc:SAML:1.1:nameid-format:unspecified");
    subject.setNameID(nameId);
    SubjectConfirmation confirmation = build(builders, SubjectConfirmation.DEFAULT_ELEMENT_NAME);
    confirmation.setMethod("urn:oasis:names:tc:SAML:2.0:cm:bearer");
    SubjectConfirmationData confirmationData = build(builders, SubjectConfirmationData.DEFAULT_ELEMENT_NAME);
    confirmationData.setRecipient(ACS_URL);
    confirmationData.setNotOnOrAfter(notOnOrAfter);
    if (inResponseTo != null) {
      confirmationData.setInResponseTo(inResponseTo);
    }
    confirmation.setSubjectConfirmationData(confirmationData);
    subject.getSubjectConfirmations().add(confirmation);
    assertion.setSubject(subject);

    Conditions conditions = build(builders, Conditions.DEFAULT_ELEMENT_NAME);
    conditions.setNotBefore(now.minus(1, ChronoUnit.MINUTES));
    conditions.setNotOnOrAfter(notOnOrAfter);
    AudienceRestriction audienceRestriction = build(builders, AudienceRestriction.DEFAULT_ELEMENT_NAME);
    Audience audience = build(builders, Audience.DEFAULT_ELEMENT_NAME);
    audience.setURI(SP_ENTITY_ID);
    audienceRestriction.getAudiences().add(audience);
    conditions.getAudienceRestrictions().add(audienceRestriction);
    assertion.setConditions(conditions);

    AuthnStatement authnStatement = build(builders, AuthnStatement.DEFAULT_ELEMENT_NAME);
    authnStatement.setAuthnInstant(now);
    AuthnContext authnContext = build(builders, AuthnContext.DEFAULT_ELEMENT_NAME);
    AuthnContextClassRef classRef = build(builders, AuthnContextClassRef.DEFAULT_ELEMENT_NAME);
    classRef.setURI("urn:oasis:names:tc:SAML:2.0:ac:classes:unspecified");
    authnContext.setAuthnContextClassRef(classRef);
    authnStatement.setAuthnContext(authnContext);
    assertion.getAuthnStatements().add(authnStatement);

    AttributeStatement attributeStatement = build(builders, AttributeStatement.DEFAULT_ELEMENT_NAME);
    attributeStatement.getAttributes().add(attribute(builders, "username", "jsmith"));
    attributeStatement.getAttributes().add(attribute(builders, "groups", "admins", "devs"));
    attributeStatement.getAttributes()
        .add(attributeWithFriendlyName(builders, "urn:oid:2.16.840.1.113730.3.1.241", "displayName", "John Smith"));
    assertion.getAttributeStatements().add(attributeStatement);

    if (signAssertion) {
      sign(assertion);
    }

    Response response = build(builders, Response.DEFAULT_ELEMENT_NAME);
    response.setID("_response_" + java.util.UUID.randomUUID());
    if (inResponseTo != null) {
      response.setInResponseTo(inResponseTo);
    }
    response.setIssueInstant(now);
    response.setDestination(ACS_URL);
    response.setIssuer(issuer(builders));
    Status status = build(builders, Status.DEFAULT_ELEMENT_NAME);
    StatusCode statusCode = build(builders, StatusCode.DEFAULT_ELEMENT_NAME);
    statusCode.setValue(StatusCode.SUCCESS);
    status.setStatusCode(statusCode);
    response.setStatus(status);
    response.getAssertions().add(assertion);

    Element element;
    if (signResponse) {
      // sign() marshalls the response (reusing the already-signed assertion DOM) and applies the signature.
      sign(response);
      element = response.getDOM();
    }
    else {
      element = XMLObjectProviderRegistrySupport.getMarshallerFactory().getMarshaller(response).marshall(response);
    }
    return net.shibboleth.shared.xml.SerializeSupport.nodeToString(element);
  }

  private Issuer issuer(XMLObjectBuilderFactory builders) {
    Issuer issuer = build(builders, Issuer.DEFAULT_ELEMENT_NAME);
    issuer.setValue(IDP_ENTITY_ID);
    return issuer;
  }

  private Attribute attribute(XMLObjectBuilderFactory builders, String name, String... values) {
    Attribute attribute = build(builders, Attribute.DEFAULT_ELEMENT_NAME);
    attribute.setName(name);
    attribute.setNameFormat("urn:oasis:names:tc:SAML:2.0:attrname-format:basic");
    XSStringBuilder stringBuilder = (XSStringBuilder) builders.getBuilder(XSString.TYPE_NAME);
    for (String value : values) {
      XSString attributeValue = stringBuilder.buildObject(AttributeValue.DEFAULT_ELEMENT_NAME, XSString.TYPE_NAME);
      attributeValue.setValue(value);
      attribute.getAttributeValues().add(attributeValue);
    }
    return attribute;
  }

  private Attribute attributeWithFriendlyName(
      XMLObjectBuilderFactory builders,
      String name,
      String friendlyName,
      String... values)
  {
    Attribute attribute = attribute(builders, name, values);
    attribute.setFriendlyName(friendlyName);
    return attribute;
  }

  private void sign(SignableSAMLObject signable) throws Exception {
    BasicX509Credential credential = new BasicX509Credential(idpCertificate, idpKeyPair.getPrivate());
    Signature signature =
        (Signature) XMLObjectProviderRegistrySupport.getBuilderFactory()
            .getBuilder(Signature.DEFAULT_ELEMENT_NAME)
            .buildObject(Signature.DEFAULT_ELEMENT_NAME);
    signature.setSigningCredential(credential);
    signature.setSignatureAlgorithm(SignatureConstants.ALGO_ID_SIGNATURE_RSA_SHA256);
    signature.setCanonicalizationAlgorithm(SignatureConstants.ALGO_ID_C14N_EXCL_OMIT_COMMENTS);
    signable.setSignature(signature);
    Marshaller marshaller = XMLObjectProviderRegistrySupport.getMarshallerFactory().getMarshaller(signable);
    marshaller.marshall(signable);
    Signer.signObject(signature);
  }

  @SuppressWarnings("unchecked")
  private static <T> T build(XMLObjectBuilderFactory builders, javax.xml.namespace.QName elementName) {
    return (T) builders.getBuilder(elementName).buildObject(elementName);
  }

  private static String idpMetadata(X509Certificate signingCert) {
    String certificate;
    try {
      certificate = Base64.getEncoder().encodeToString(signingCert.getEncoded());
    }
    catch (Exception e) {
      throw new IllegalStateException(e);
    }
    return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
        + "<EntityDescriptor xmlns=\"urn:oasis:names:tc:SAML:2.0:metadata\" entityID=\"" + IDP_ENTITY_ID + "\">"
        + "<IDPSSODescriptor protocolSupportEnumeration=\"urn:oasis:names:tc:SAML:2.0:protocol\">"
        + "<KeyDescriptor use=\"signing\">"
        + "<dsig:KeyInfo xmlns:dsig=\"http://www.w3.org/2000/09/xmldsig#\">"
        + "<dsig:X509Data><dsig:X509Certificate>" + certificate + "</dsig:X509Certificate></dsig:X509Data>"
        + "</dsig:KeyInfo></KeyDescriptor>"
        + "<NameIDFormat>urn:oasis:names:tc:SAML:1.1:nameid-format:unspecified</NameIDFormat>"
        + "<SingleSignOnService Binding=\"urn:oasis:names:tc:SAML:2.0:bindings:HTTP-POST\" "
        + "Location=\"http://idp.local/sso\"/>"
        + "</IDPSSODescriptor></EntityDescriptor>";
  }
}
