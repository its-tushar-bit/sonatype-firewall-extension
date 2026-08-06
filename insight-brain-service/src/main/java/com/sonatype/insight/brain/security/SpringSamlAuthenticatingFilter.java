/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.core.UriBuilder;

import com.sonatype.insight.brain.landing.LandingService;
import com.sonatype.insight.brain.model.configuration.saml.SamlConfiguration;
import com.sonatype.insight.jaxrs.error.ErrorResponse;

import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.web.filter.authc.AuthenticatingFilter;
import org.apache.commons.lang3.StringUtils;
import org.opensaml.saml.saml2.core.Assertion;
import org.opensaml.saml.saml2.core.Attribute;
import org.opensaml.saml.saml2.core.AttributeStatement;
import org.opensaml.saml.saml2.assertion.SAML2AssertionValidationParameters;
import org.opensaml.saml.saml2.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.saml2.provider.service.authentication.DefaultSaml2AuthenticatedPrincipal;
import org.springframework.security.saml2.provider.service.authentication.Saml2Authentication;
import org.springframework.security.saml2.core.Saml2ParameterNames;
import org.springframework.security.saml2.core.Saml2Error;
import org.springframework.security.saml2.core.Saml2ErrorCodes;
import org.springframework.security.saml2.core.Saml2ResponseValidatorResult;
import org.springframework.security.saml2.provider.service.authentication.AbstractSaml2AuthenticationRequest;
import org.springframework.security.saml2.provider.service.authentication.OpenSaml5AuthenticationProvider;
import org.springframework.security.saml2.provider.service.authentication.Saml2AuthenticatedPrincipal;
import org.springframework.security.saml2.provider.service.authentication.Saml2AuthenticationToken;
import org.springframework.security.saml2.provider.service.authentication.Saml2PostAuthenticationRequest;
import org.springframework.security.saml2.provider.service.authentication.Saml2RedirectAuthenticationRequest;
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistration;
import org.springframework.security.saml2.provider.service.web.RelyingPartyRegistrationResolver;
import org.springframework.security.saml2.provider.service.web.Saml2AuthenticationRequestRepository;
import org.springframework.security.saml2.provider.service.web.Saml2AuthenticationTokenConverter;
import org.springframework.security.saml2.provider.service.web.authentication.OpenSaml5AuthenticationRequestResolver;
import org.springframework.security.saml2.provider.service.web.authentication.Saml2AuthenticationRequestResolver;
import org.springframework.web.util.HtmlUtils;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.util.UriUtils;

/**
 * Shiro authenticating filter that drives SAML login via the Spring Security SAML2 (OpenSAML5) engine.
 *
 * <p>
 * It only initiates a SAML challenge for the {@code /saml} endpoint and otherwise passes the request
 * through to the rest of the Shiro chain (so unauthenticated API calls get a 401 from
 * {@code MissingAuthenticationFilter} rather than an IdP redirect). On a successful assertion it
 * establishes the Shiro subject via {@link SamlAuthenticationToken} carrying a
 * {@link SpringSamlPrincipal}, consumed by {@link SamlRealm}.
 *
 * @see org.springframework.security.saml2.provider.service.web.authentication.Saml2WebSsoAuthenticationFilter
 */
@Named
@Singleton
public class SpringSamlAuthenticatingFilter
    extends AuthenticatingFilter
{
  private static final Logger log = LoggerFactory.getLogger(SpringSamlAuthenticatingFilter.class);

  static final String MSG_NO_SAML_CONFIG = "SAML Realm Enabled but no IDP configured";

  // Separates the encoded SPA landing hints (origin | hash) carried through the IdP round-trip in RelayState.
  private static final char RELAY_STATE_SEPARATOR = '|';

  // Generates per-response CSP nonces for the auto-POST binding form.
  private static final SecureRandom NONCE_RANDOM = new SecureRandom();

  // Preserves the pre-migration SAML assertion clock-skew tolerance (CLM-13628). OpenSAML5's default is 5 minutes;
  // this restores the deliberately tight 1s window. Replay is further bounded by InResponseTo correlation and the
  // single-use AuthnRequest cookie.
  private static final Duration ALLOWED_CLOCK_SKEW = Duration.ofSeconds(1);

  private final RelyingPartyRegistrationResolver relyingPartyRegistrationResolver;

  private final LandingService landingService;

  private final Saml2AuthenticationRequestResolver authRequestResolver;

  private final Saml2AuthenticationTokenConverter authTokenConverter;

  private final Saml2AuthenticationRequestRepository<AbstractSaml2AuthenticationRequest> authenticationRequestRepository;

  private final OpenSaml5AuthenticationProvider authenticationProvider;

  @Inject
  public SpringSamlAuthenticatingFilter(
      SamlRelyingPartyRegistrationResolver relyingPartyRegistrationResolver,
      LandingService landingService,
      SamlConfigurationCache samlConfigurationCache,
      EncryptionKeyStore encryptionKeyStore)
  {
    this.relyingPartyRegistrationResolver = relyingPartyRegistrationResolver;
    this.landingService = landingService;
    OpenSaml5AuthenticationRequestResolver authnRequestResolver =
        new OpenSaml5AuthenticationRequestResolver(relyingPartyRegistrationResolver);
    authnRequestResolver.setRequestMatcher(SAML_REQUEST_MATCHER);
    // Carry the SPA's post-login landing hints (origin/hash) through the IdP round-trip so the user returns
    // to the page originally requested rather than always the default landing page.
    authnRequestResolver.setRelayStateResolver(SpringSamlAuthenticatingFilter::resolveRelayState);
    this.authRequestResolver = authnRequestResolver;
    this.authenticationRequestRepository = new CookieSaml2AuthenticationRequestRepository(encryptionKeyStore);
    Saml2AuthenticationTokenConverter tokenConverter =
        new Saml2AuthenticationTokenConverter(relyingPartyRegistrationResolver);
    // Load the saved AuthnRequest (from the cookie) so the provider can validate the response's
    // InResponseTo against the SP-initiated request.
    tokenConverter.setAuthenticationRequestRepository(authenticationRequestRepository);
    this.authTokenConverter = tokenConverter;
    this.authenticationProvider = createAuthenticationProvider(samlConfigurationCache);
  }

  /**
   * Builds the OpenSAML5 authentication provider used to validate the IdP response.
   *
   * <p>
   * It augments the default response converter so that SAML attribute {@code FriendlyName}s are folded into
   * the principal's attribute map (keyed by formal {@code Name}), letting {@link SamlRealm} resolve attribute
   * mappings that reference either the formal name or the friendly name.
   *
   * <p>
   * It also reproduces the pre-migration signature policy: a response/assertion signature is required
   * according to the tenant's {@code validateResponseSignature}/{@code validateAssertionSignature} flags,
   * each defaulting to "required when the identity provider published a signing key". The provider's built-in
   * validators still verify any signature that is present; these added checks only enforce that one must be
   * present. (Spring's own baseline additionally rejects a fully unsigned response.)
   */
  static OpenSaml5AuthenticationProvider createAuthenticationProvider(SamlConfigurationCache samlConfigurationCache) {
    OpenSaml5AuthenticationProvider provider = new OpenSaml5AuthenticationProvider();

    Converter<OpenSaml5AuthenticationProvider.ResponseToken, Saml2ResponseValidatorResult> defaultResponseValidator =
        OpenSaml5AuthenticationProvider.createDefaultResponseValidator();
    provider.setResponseValidator(responseToken -> {
      Saml2ResponseValidatorResult result = defaultResponseValidator.convert(responseToken);
      if (signatureRequired(samlConfigurationCache, responseToken.getToken(),
          SamlConfiguration::getValidateResponseSignature) && !responseToken.getResponse().isSigned())
      {
        result = result.concat(new Saml2Error(Saml2ErrorCodes.INVALID_SIGNATURE,
            "SAML response is not signed but response signature validation is required"));
      }
      return result;
    });

    Converter<OpenSaml5AuthenticationProvider.AssertionToken, Saml2ResponseValidatorResult> defaultAssertionValidator =
        OpenSaml5AuthenticationProvider.createDefaultAssertionValidatorWithParameters(
            params -> params.put(SAML2AssertionValidationParameters.CLOCK_SKEW, ALLOWED_CLOCK_SKEW));
    provider.setAssertionValidator(assertionToken -> {
      Saml2ResponseValidatorResult result = defaultAssertionValidator.convert(assertionToken);
      if (signatureRequired(samlConfigurationCache, assertionToken.getToken(),
          SamlConfiguration::getValidateAssertionSignature) && !assertionToken.getAssertion().isSigned())
      {
        result = result.concat(new Saml2Error(Saml2ErrorCodes.INVALID_SIGNATURE,
            "SAML assertion is not signed but assertion signature validation is required"));
      }
      return result;
    });

    Converter<OpenSaml5AuthenticationProvider.ResponseToken, Saml2Authentication> defaultConverter =
        OpenSaml5AuthenticationProvider.createDefaultResponseAuthenticationConverter();
    provider.setResponseAuthenticationConverter(
        responseToken -> foldFriendlyNames(responseToken.getResponse(), defaultConverter.convert(responseToken)));
    return provider;
  }

  /**
   * Whether a signature must be present on the response/assertion. An explicit {@code validate*Signature}
   * flag wins; otherwise a signature is required whenever the identity provider published a signing key.
   */
  private static boolean signatureRequired(
      SamlConfigurationCache samlConfigurationCache,
      Saml2AuthenticationToken token,
      Function<SamlConfiguration, Boolean> flag)
  {
    boolean hasSigningKey = !token.getRelyingPartyRegistration()
        .getAssertingPartyMetadata()
        .getVerificationX509Credentials()
        .isEmpty();
    SamlConfiguration samlConfiguration = samlConfigurationCache.get();
    Boolean explicit = (samlConfiguration == null) ? null : flag.apply(samlConfiguration);
    return (explicit != null) ? explicit : hasSigningKey;
  }

  /**
   * Alias each attribute's values under its {@code FriendlyName} (when present and not already keyed), so a
   * lookup by friendly name resolves. OpenSAML5's default converter keys attributes only by formal
   * {@code Name}.
   */
  private static Saml2Authentication foldFriendlyNames(Response response, Saml2Authentication authentication) {
    if (authentication == null) {
      return null;
    }
    Saml2AuthenticatedPrincipal principal = (Saml2AuthenticatedPrincipal) authentication.getPrincipal();
    Map<String, List<Object>> attributes = new LinkedHashMap<>(principal.getAttributes());
    boolean folded = false;
    for (Assertion assertion : response.getAssertions()) {
      for (AttributeStatement statement : assertion.getAttributeStatements()) {
        for (Attribute attribute : statement.getAttributes()) {
          String friendlyName = attribute.getFriendlyName();
          String name = attribute.getName();
          if (StringUtils.isNotBlank(friendlyName)
              && !attributes.containsKey(friendlyName)
              && attributes.containsKey(name))
          {
            attributes.put(friendlyName, attributes.get(name));
            folded = true;
          }
        }
      }
    }
    if (!folded) {
      return authentication;
    }
    DefaultSaml2AuthenticatedPrincipal merged =
        new DefaultSaml2AuthenticatedPrincipal(principal.getName(), attributes, principal.getSessionIndexes());
    if (principal.getRelyingPartyRegistrationId() != null) {
      merged.setRelyingPartyRegistrationId(principal.getRelyingPartyRegistrationId());
    }
    Saml2Authentication result =
        new Saml2Authentication(merged, authentication.getSaml2Response(), authentication.getAuthorities());
    result.setDetails(authentication.getDetails());
    return result;
  }

  /**
   * Matches IQ's fixed {@code /saml} endpoint and supplies the single {@code saml} registration id, so
   * {@link OpenSaml5AuthenticationRequestResolver} builds the AuthnRequest there (its default matcher
   * expects {@code /saml2/authenticate/{registrationId}}).
   */
  private static final RequestMatcher SAML_REQUEST_MATCHER = new RequestMatcher()
  {
    @Override
    public boolean matches(HttpServletRequest request) {
      return isSamlEndpoint(request);
    }

    @Override
    public MatchResult matcher(HttpServletRequest request) {
      return isSamlEndpoint(request)
          ? MatchResult.match(Map.of("registrationId", SamlRelyingPartyRegistrationResolver.REGISTRATION_ID))
          : MatchResult.notMatch();
    }
  };

  /**
   * Routes every {@code /saml} request through {@link #onAccessDenied} (returning {@code false} here) and applies
   * standard Shiro semantics elsewhere. This is kept a pure query: performing the already-authenticated home
   * redirect here as well would commit the response and then let {@code onPreHandle} run {@code onAccessDenied},
   * which would attempt a second redirect on the committed response and throw {@link IllegalStateException}.
   */
  @Override
  protected boolean isAccessAllowed(ServletRequest request, ServletResponse response, Object mappedValue) {
    if (isSamlEndpoint((HttpServletRequest) request)) {
      return false;
    }
    return super.isAccessAllowed(request, response, mappedValue);
  }

  @Override
  protected boolean onAccessDenied(ServletRequest request, ServletResponse response) throws Exception {
    HttpServletRequest httpRequest = (HttpServletRequest) request;
    HttpServletResponse httpResponse = (HttpServletResponse) response;

    // Only the /saml endpoint initiates SAML; other chained paths pass through so requireAuth can 401.
    if (!isSamlEndpoint(httpRequest)) {
      return true;
    }

    // Already-authenticated users hitting the SAML endpoint are redirected home rather than re-challenged.
    if (getSubject(request, response).isAuthenticated()) {
      redirectToHome(httpRequest, httpResponse);
      return false;
    }

    RelyingPartyRegistration registration = relyingPartyRegistrationResolver.resolve(httpRequest, null);
    if (registration == null) {
      log.debug("SAML endpoint hit but SAML is not configured");
      LoginErrorResponseHandler.sendError(httpResponse,
          new ErrorResponse(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, MSG_NO_SAML_CONFIG));
      return false;
    }

    if (isLoginAttempt(httpRequest)) {
      return executeLogin(request, response);
    }
    return sendChallenge(httpRequest, httpResponse);
  }

  /**
   * Redirect the user to the configured SAML IDP (SP-initiated).
   */
  private boolean sendChallenge(HttpServletRequest request, HttpServletResponse response) throws IOException {
    log.debug("Initiating SAML authentication via identity provider");
    AbstractSaml2AuthenticationRequest authenticationRequest = authRequestResolver.resolve(request);
    if (authenticationRequest == null) {
      LoginErrorResponseHandler.sendError(response,
          new ErrorResponse(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, MSG_NO_SAML_CONFIG));
      return false;
    }
    authenticationRequestRepository.saveAuthenticationRequest(authenticationRequest, request, response);

    if (authenticationRequest instanceof Saml2PostAuthenticationRequest postRequest) {
      log.debug("Rendering auto-POST form to IDP");
      renderPostForm(response, postRequest);
    }
    else if (authenticationRequest instanceof Saml2RedirectAuthenticationRequest redirectRequest) {
      log.debug("Redirecting user to IDP via redirect binding");
      response.sendRedirect(createRedirectBindingUrl(redirectRequest));
    }
    else {
      log.error("Unknown SAML request type {}", authenticationRequest.getClass());
      LoginErrorResponseHandler.sendError(response,
          new ErrorResponse(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Unknown SAML request type"));
    }
    return false;
  }

  @Override
  protected boolean executeLogin(ServletRequest request, ServletResponse response) throws Exception {
    HttpServletRequest httpRequest = (HttpServletRequest) request;
    HttpServletResponse httpResponse = (HttpServletResponse) response;
    try {
      if (super.executeLogin(request, response)) {
        httpResponse.sendRedirect(resolveDestination(httpRequest));
        return false;
      }
      LoginErrorResponseHandler.sendError(httpResponse,
          new ErrorResponse(HttpServletResponse.SC_BAD_REQUEST,
              "An unknown login error occurred. Check with administrator."));
    }
    catch (AuthenticationException e) {
      log.warn("An error occurred executing SAML login: {}", e.getMessage(), log.isDebugEnabled() ? e : null);
      LoginErrorResponseHandler.sendError(httpResponse, new ErrorResponse(HttpServletResponse.SC_BAD_REQUEST,
          "Authentication failed due to SAML error. Please contact your IT administrator."));
    }
    return false;
  }

  @Override
  protected AuthenticationToken createToken(ServletRequest request, ServletResponse response) {
    HttpServletRequest httpRequest = (HttpServletRequest) request;
    Saml2AuthenticationToken token = authTokenConverter.convert(httpRequest);
    if (token == null) {
      throw new BadCredentialsException("Missing SAML response");
    }
    Authentication authentication = authenticationProvider.authenticate(token);
    if (!(authentication.getPrincipal() instanceof Saml2AuthenticatedPrincipal samlPrincipal)) {
      throw new BadCredentialsException(
          "Unexpected SAML principal type: " + authentication.getPrincipal());
    }
    // Clear the single-use AuthnRequest cookie only on success. On a transient failure (e.g. XML parse or
    // key-store error) the cookie is left intact so the InResponseTo correlation survives for a retry; the
    // HMAC signature and the assertion's own replay protection prevent misuse. Mirrors Spring's
    // Saml2WebSsoAuthenticationFilter, which clears the saved request only on the success path.
    authenticationRequestRepository.removeAuthenticationRequest(httpRequest, (HttpServletResponse) response);
    return new SamlAuthenticationToken(new SpringSamlPrincipal(samlPrincipal));
  }

  private String createRedirectBindingUrl(Saml2RedirectAuthenticationRequest authenticationRequest) {
    UriComponentsBuilder uriBuilder =
        UriComponentsBuilder.fromUriString(authenticationRequest.getAuthenticationRequestUri());
    addParameter(Saml2ParameterNames.SAML_REQUEST, authenticationRequest.getSamlRequest(), uriBuilder);
    addParameter(Saml2ParameterNames.RELAY_STATE, authenticationRequest.getRelayState(), uriBuilder);
    addParameter(Saml2ParameterNames.SIG_ALG, authenticationRequest.getSigAlg(), uriBuilder);
    addParameter(Saml2ParameterNames.SIGNATURE, authenticationRequest.getSignature(), uriBuilder);
    return uriBuilder.build(true).toUriString();
  }

  private static void addParameter(String name, String value, UriComponentsBuilder builder) {
    if (value != null && !value.isEmpty()) {
      builder.queryParam(UriUtils.encode(name, StandardCharsets.ISO_8859_1),
          UriUtils.encode(value, StandardCharsets.ISO_8859_1));
    }
  }

  /**
   * Render an auto-submitting HTML form that POSTs the SAML request to the IdP (POST binding).
   */
  private static void renderPostForm(
      HttpServletResponse response,
      Saml2PostAuthenticationRequest request) throws IOException
  {
    String location = HtmlUtils.htmlEscape(request.getAuthenticationRequestUri());
    String samlRequest = HtmlUtils.htmlEscape(request.getSamlRequest());
    String relayState = request.getRelayState() == null ? "" : HtmlUtils.htmlEscape(request.getRelayState());
    // The form must auto-submit via script. Emit it as a nonce'd <script> element (not an inline event handler)
    // and set a tight per-response CSP, so a strict deployment-wide policy (e.g. from LegacyWebHeaderFilter on
    // /*) cannot block the submit and silently break POST-binding SSO. form-action is pinned to the IdP origin
    // so the SAMLRequest cannot be re-targeted.
    byte[] nonceBytes = new byte[16];
    NONCE_RANDOM.nextBytes(nonceBytes);
    String nonce = Base64.getEncoder().encodeToString(nonceBytes);
    response.setHeader("Content-Security-Policy",
        "default-src 'none'; script-src 'nonce-" + nonce + "'; form-action " + idpFormActionSource(request) + ";");
    String html = "<!DOCTYPE html><html lang=\"en\"><head><meta charset=\"utf-8\"></head>"
        + "<body>"
        + "<noscript><p>Your browser must support JavaScript to continue.</p></noscript>"
        + "<form method=\"post\" action=\"" + location + "\">"
        + "<input type=\"hidden\" name=\"SAMLRequest\" value=\"" + samlRequest + "\"/>"
        + "<input type=\"hidden\" name=\"RelayState\" value=\"" + relayState + "\"/>"
        + "<noscript><input type=\"submit\" value=\"Continue\"/></noscript>"
        + "</form>"
        + "<script nonce=\"" + nonce + "\">document.forms[0].submit()</script>"
        + "</body></html>";
    response.setContentType("text/html;charset=UTF-8");
    response.getWriter().write(html);
  }

  /**
   * The IdP {@code scheme://authority} used to pin the auto-POST form's CSP {@code form-action}, so the browser
   * will only submit the SAMLRequest to the identity provider. Falls back to {@code 'self'} if the endpoint URI
   * cannot be parsed.
   */
  private static String idpFormActionSource(Saml2PostAuthenticationRequest request) {
    try {
      URI uri = URI.create(request.getAuthenticationRequestUri());
      if (uri.getScheme() != null && uri.getAuthority() != null) {
        return uri.getScheme() + "://" + uri.getAuthority();
      }
    }
    catch (IllegalArgumentException e) {
      log.debug("Could not parse IdP endpoint for CSP form-action; falling back to 'self'");
    }
    return "'self'";
  }

  private void redirectToHome(HttpServletRequest request, HttpServletResponse response) {
    try {
      String home = request.getContextPath();
      if (!home.endsWith("/")) {
        home += "/";
      }
      response.sendRedirect(home);
    }
    catch (IOException e) {
      log.warn("Failed to redirect already-authenticated user to home", e);
    }
  }

  /**
   * Encodes the SPA's post-login landing hints ({@code origin}, {@code hash}) from the initiating request
   * into the SAML {@code RelayState} so they survive the IdP round-trip. Returns {@code null} when there is
   * nothing to preserve.
   *
   * <p>
   * SAML 2.0 limits {@code RelayState} to 80 bytes. A deep link whose {@code origin|hash} exceeds that (e.g. a
   * long filtered route like {@code #/vulnerabilities?severities=critical&...}) can be truncated or dropped by a
   * strict IdP, in which case the user lands on the default page after login rather than the deep link. The
   * origin/hash carry only presentation state, so this degrades gracefully and never affects authentication.
   */
  private static String resolveRelayState(HttpServletRequest request) {
    String origin = request.getParameter("origin");
    String hash = request.getParameter("hash");
    if (StringUtils.isBlank(origin) && StringUtils.isBlank(hash)) {
      return null;
    }
    return (origin == null ? "" : origin) + RELAY_STATE_SEPARATOR + (hash == null ? "" : hash);
  }

  /**
   * Rebuilds the post-login redirect target from the returned {@code RelayState}. The destination is always
   * reconstructed from the trusted {@link LandingService} base (only the SPA {@code origin}/{@code hash} are
   * taken from RelayState), so a tampered RelayState cannot produce an open redirect.
   */
  // Package-private for testing (open-redirect defense).
  String resolveDestination(HttpServletRequest request) {
    String origin = null;
    String hash = "";
    String relayState = request.getParameter(Saml2ParameterNames.RELAY_STATE);
    if (StringUtils.isNotBlank(relayState)) {
      int separator = relayState.indexOf(RELAY_STATE_SEPARATOR);
      if (separator >= 0) {
        origin = relayState.substring(0, separator);
        hash = relayState.substring(separator + 1);
      }
    }
    if (hash.startsWith("#")) {
      hash = hash.substring(1);
    }
    URI base = LandingService.ORIGIN_GUIDE.equals(origin)
        ? landingService.getGuideDestination()
        : landingService.getDestination();
    URI uri = UriBuilder.fromUri(base).replaceQuery("").fragment(hash).build();
    // UriBuilder.fragment() over-encodes '/' and '?' that RFC 3986 permits in a fragment; restore them within the
    // fragment only (never the path or query) so the SPA sees its original hash route
    // (e.g. "#/vulnerabilities?severities=critical").
    String result = uri.toString();
    int fragmentStart = result.indexOf('#');
    if (fragmentStart < 0) {
      return result;
    }
    String fragment = result.substring(fragmentStart + 1).replace("%2F", "/").replace("%3F", "?");
    return URI.create(result.substring(0, fragmentStart + 1) + fragment).toString();
  }

  private static boolean isSamlEndpoint(HttpServletRequest request) {
    String requestPath = request.getRequestURI().substring(request.getContextPath().length());
    return requestPath.equals(SamlConstants.SAML_REQUEST_PATH)
        || requestPath.startsWith(SamlConstants.SAML_REQUEST_PATH + "/");
  }

  private static boolean isLoginAttempt(HttpServletRequest request) {
    return "POST".equals(request.getMethod()) && request.getParameter("SAMLResponse") != null;
  }
}
