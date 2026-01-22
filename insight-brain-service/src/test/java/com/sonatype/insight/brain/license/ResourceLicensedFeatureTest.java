/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.license;
import org.junit.experimental.categories.Category;
import com.sonatype.insight.brain.common.test.SlowTest;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;

import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.api.experimental.ApiCallFlowAnalysisConfigResource;
import com.sonatype.insight.brain.api.experimental.ApiComponentNearestFixedVersionsResource;
import com.sonatype.insight.brain.api.experimental.ApiSourceControlEventResource;
import com.sonatype.insight.brain.api.experimental.ApiSourceControlResource;
import com.sonatype.insight.brain.api.experimental.ApiVulnerabilityAnalysisDataResource;
import com.sonatype.insight.brain.api.experimental.ApiVulnerabilityCustomDataResource;
import com.sonatype.insight.brain.api.experimental.ApiVulnerabilityGroupResource;
import com.sonatype.insight.brain.api.experimental.ApiVulnerabilitySignatureResource;
import com.sonatype.insight.brain.api.experimental.resultsview.RepositoryResultsResource;
import com.sonatype.insight.brain.api.v2.ApiApplicationCategoryResource;
import com.sonatype.insight.brain.api.v2.ApiComponentDetailsResourceV2;
import com.sonatype.insight.brain.api.v2.ApiComponentLabelResourceV2;
import com.sonatype.insight.brain.api.v2.ApiComponentReleaseQuarantineResource;
import com.sonatype.insight.brain.api.v2.ApiComponentRemediationResource;
import com.sonatype.insight.brain.api.v2.ApiComponentsInQuarantineReportingResource;
import com.sonatype.insight.brain.api.v2.ApiComponentsWithWaiversReportingResource;
import com.sonatype.insight.brain.api.v2.ApiCompositeSourceControlConfigValidatorResource;
import com.sonatype.insight.brain.api.v2.ApiCompositeSourceControlResource;
import com.sonatype.insight.brain.api.v2.ApiCycloneDxResourceV2;
import com.sonatype.insight.brain.api.v2.ApiDataRetentionPolicyResource;
import com.sonatype.insight.brain.api.v2.ApiEndpointsResource;
import com.sonatype.insight.brain.api.v2.ApiEvaluationResourceV2;
import com.sonatype.insight.brain.api.v2.ApiLabelResource;
import com.sonatype.insight.brain.api.v2.ApiMetricsReportingResourceV2;
import com.sonatype.insight.brain.api.v2.ApiPolicyResourceV2;
import com.sonatype.insight.brain.api.v2.ApiPolicyViolationResourceV2;
import com.sonatype.insight.brain.api.v2.ApiPolicyViolationWaiverResource;
import com.sonatype.insight.brain.api.v2.ApiPolicyWaiverResource;
import com.sonatype.insight.brain.api.v2.ApiReportDataResourceV2;
import com.sonatype.insight.brain.api.v2.ApiReportResourceV2;
import com.sonatype.insight.brain.api.v2.ApiRepositoryConnectionResource;
import com.sonatype.insight.brain.api.v2.ApiRepositoryIdentifiedComponentResource;
import com.sonatype.insight.brain.api.v2.ApiRepositoryPathResource;
import com.sonatype.insight.brain.api.v2.ApiSearchResourceV2;
import com.sonatype.insight.brain.api.v2.ApiSourceControlConfigurationResource;
import com.sonatype.insight.brain.api.v2.ApiSourceControlMetricsResource;
import com.sonatype.insight.brain.api.v2.ApiSpdxResource;
import com.sonatype.insight.brain.api.v2.ApiStaleWaiversReportingResource;
import com.sonatype.insight.brain.api.v2.ApiThirdPartyScanResource;
import com.sonatype.insight.brain.api.v2.dto.ApiComponentEvaluationRequestDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiPromoteScanRequestDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiSourceControlEvaluationRequestDTO;
import com.sonatype.insight.brain.component.ComponentDetailResource;
import com.sonatype.insight.brain.git.ApiScmOnboardingResource;
import com.sonatype.insight.brain.git.ScmOnboardingResource;
import com.sonatype.insight.brain.hds.ComponentInfoResource;
import com.sonatype.insight.brain.hds.RepoManComponentInfoResource;
import com.sonatype.insight.brain.ide.IDEComponentInfoResource;

import com.sonatype.insight.brain.integration.PolicyEvaluationSummaryResource;
import com.sonatype.insight.brain.label.ComponentLabelResource;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.vulnerability.SecurityVulnerabilityOverride;
import com.sonatype.insight.brain.policy.PolicyMonitoringResource;
import com.sonatype.insight.brain.policy.PolicyResource;
import com.sonatype.insight.brain.policy.PolicyWaiverResource;
import com.sonatype.insight.brain.policy.evaluator.PolicyEvaluateResource;
import com.sonatype.insight.brain.product.license.ProductLicenseEnforcementPoint;
import com.sonatype.insight.brain.report.ReportResource;
import com.sonatype.insight.brain.repository.RepositoryReportResource;
import com.sonatype.insight.brain.repository.component.QuarantinedComponentResource;
import com.sonatype.insight.brain.scan.ScanResource;
import com.sonatype.insight.brain.service.AbstractResourceTest;
import com.sonatype.insight.brain.successmetrics.SuccessMetricsReportResource;
import com.sonatype.insight.brain.successmetrics.SuccessMetricsResource;
import com.sonatype.insight.brain.tag.PolicyTagResource;
import com.sonatype.insight.brain.vulnerability.SecurityVulnerabilityOverrideResource;
import com.sonatype.insight.license.model.LicensedFeature;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

@Category(SlowTest.class)
public class ResourceLicensedFeatureTest
    extends AbstractResourceTest
{
  private static final Map<Class<?>, LicensedFeature> EXPECTED_RESOURCE_CLASS_TO_LICENSED_FEATURE;

  static {
    Map<Class<?>, LicensedFeature> map = new HashMap<>();

    map.put(ApiEndpointsResource.class, LicensedFeature.API_PAGE);

    map.put(ApiThirdPartyScanResource.class, LicensedFeature.APPLICATION_EVALUATION);
    map.put(PolicyEvaluateResource.class, LicensedFeature.APPLICATION_EVALUATION);
    map.put(ScanResource.class, LicensedFeature.APPLICATION_EVALUATION);

    map.put(ApiReportResourceV2.class, LicensedFeature.APPLICATION_REPORTS);
    map.put(PolicyEvaluationSummaryResource.class, LicensedFeature.APPLICATION_REPORTS);

    map.put(ApiCallFlowAnalysisConfigResource.class, LicensedFeature.CALL_FLOW_ANALYSIS);
    map.put(ApiVulnerabilitySignatureResource.class, LicensedFeature.CALL_FLOW_ANALYSIS);

    map.put(ApiComponentDetailsResourceV2.class, LicensedFeature.COMPONENT_EVALUATION);
    map.put(ApiComponentNearestFixedVersionsResource.class, LicensedFeature.COMPONENT_EVALUATION);
    map.put(ApiComponentRemediationResource.class, LicensedFeature.COMPONENT_EVALUATION);
    map.put(ComponentInfoResource.class, LicensedFeature.COMPONENT_EVALUATION);
    map.put(IDEComponentInfoResource.class, LicensedFeature.COMPONENT_EVALUATION);
    map.put(RepoManComponentInfoResource.class, LicensedFeature.COMPONENT_EVALUATION);

    map.put(ApiComponentLabelResourceV2.class, LicensedFeature.COMPONENT_LABELS);
    map.put(ApiLabelResource.class, LicensedFeature.COMPONENT_LABELS);
    map.put(ComponentLabelResource.class, LicensedFeature.COMPONENT_LABELS);

    map.put(ApiSearchResourceV2.class, LicensedFeature.COMPONENT_SEARCH);

    map.put(ApiDataRetentionPolicyResource.class, LicensedFeature.DATA_RETENTION);

    map.put(ApiComponentsInQuarantineReportingResource.class, LicensedFeature.FIREWALL);
    map.put(ApiComponentReleaseQuarantineResource.class, LicensedFeature.FIREWALL);
    map.put(ApiRepositoryPathResource.class, LicensedFeature.FIREWALL);
    map.put(QuarantinedComponentResource.class, LicensedFeature.FIREWALL);
    map.put(RepositoryResultsResource.class, LicensedFeature.FIREWALL);

    map.put(ApiRepositoryConnectionResource.class, LicensedFeature.INNER_SOURCE_REPOSITORIES);
    map.put(ApiRepositoryIdentifiedComponentResource.class, LicensedFeature.INNER_SOURCE_REPOSITORIES);

    map.put(ApiApplicationCategoryResource.class, LicensedFeature.POLICY_MANAGEMENT);
    map.put(ApiPolicyResourceV2.class, LicensedFeature.POLICY_MANAGEMENT);
    map.put(LicenseThreatGroupResource.class, LicensedFeature.POLICY_MANAGEMENT);
    map.put(PolicyResource.class, LicensedFeature.POLICY_MANAGEMENT);
    map.put(PolicyTagResource.class, LicensedFeature.POLICY_MANAGEMENT);

    map.put(PolicyMonitoringResource.class, LicensedFeature.POLICY_MONITORING);

    map.put(ApiPolicyViolationResourceV2.class, LicensedFeature.POLICY_VIOLATIONS);

    map.put(ApiPolicyViolationWaiverResource.class, LicensedFeature.POLICY_WAIVERS);
    map.put(ApiPolicyWaiverResource.class, LicensedFeature.POLICY_WAIVERS);
    map.put(PolicyWaiverResource.class, LicensedFeature.POLICY_WAIVERS);

    map.put(RepositoryReportResource.class, LicensedFeature.REPOSITORY_REPORTS);

    map.put(ApiVulnerabilityAnalysisDataResource.class, LicensedFeature.SBOM_EVALUATION);

    map.put(ApiCycloneDxResourceV2.class, LicensedFeature.SBOM_REPORTS);
    map.put(ApiSpdxResource.class, LicensedFeature.SBOM_REPORTS);

    map.put(ApiCompositeSourceControlConfigValidatorResource.class, LicensedFeature.SOURCE_CONTROL);
    map.put(ApiCompositeSourceControlResource.class, LicensedFeature.SOURCE_CONTROL);
    map.put(ApiScmOnboardingResource.class, LicensedFeature.SOURCE_CONTROL);
    map.put(ApiSourceControlEventResource.class, LicensedFeature.SOURCE_CONTROL);
    map.put(ApiSourceControlConfigurationResource.class, LicensedFeature.SOURCE_CONTROL);
    map.put(ApiSourceControlMetricsResource.class, LicensedFeature.SOURCE_CONTROL);
    map.put(ApiSourceControlResource.class, LicensedFeature.SOURCE_CONTROL);
    map.put(com.sonatype.insight.brain.api.v2.ApiSourceControlResource.class, LicensedFeature.SOURCE_CONTROL);
    map.put(ScmOnboardingResource.class, LicensedFeature.SOURCE_CONTROL);

    map.put(ApiMetricsReportingResourceV2.class, LicensedFeature.SUCCESS_METRICS);
    map.put(SuccessMetricsReportResource.class, LicensedFeature.SUCCESS_METRICS);
    map.put(SuccessMetricsResource.class, LicensedFeature.SUCCESS_METRICS);

    map.put(ApiVulnerabilityCustomDataResource.class, LicensedFeature.VULNERABILITY_CUSTOMIZATION);
    map.put(ApiVulnerabilityGroupResource.class, LicensedFeature.VULNERABILITY_CUSTOMIZATION);

    map.put(ApiComponentsWithWaiversReportingResource.class, LicensedFeature.WAIVER_REPORTS);
    map.put(ApiStaleWaiversReportingResource.class, LicensedFeature.WAIVER_REPORTS);

    EXPECTED_RESOURCE_CLASS_TO_LICENSED_FEATURE = Collections.unmodifiableMap(map);
  }

  private static final Map<Method, LicensedFeature> EXPECTED_RESOURCE_CLASS_METHOD_TO_LICENSED_FEATURE;

  static {
    Map<Method, LicensedFeature> map = new HashMap<>();

    try {
      map.put(ApiEvaluationResourceV2.class.getDeclaredMethod(
          "evaluateComponents",
          String.class,
          ApiComponentEvaluationRequestDTOV2.class
      ), LicensedFeature.COMPONENT_EVALUATION);
      map.put(ApiEvaluationResourceV2.class.getDeclaredMethod(
          "getComponentEvaluation",
          String.class,
          String.class
      ), LicensedFeature.COMPONENT_EVALUATION);
      map.put(ApiEvaluationResourceV2.class.getDeclaredMethod(
          "promoteScan",
          String.class,
          ApiPromoteScanRequestDTOV2.class,
          HttpServletRequest.class
      ), LicensedFeature.APPLICATION_EVALUATION);
      map.put(ApiEvaluationResourceV2.class.getDeclaredMethod(
          "evaluateSourceControl",
          String.class,
          ApiSourceControlEvaluationRequestDTO.class,
          HttpServletRequest.class
      ), LicensedFeature.SOURCE_CONTROL);
      map.put(ApiEvaluationResourceV2.class.getDeclaredMethod(
          "getApplicationEvaluationStatus",
          String.class,
          String.class
      ), LicensedFeature.APPLICATION_EVALUATION);

      map.put(ApiReportDataResourceV2.class.getDeclaredMethod(
          "getData",
          String.class,
          String.class
      ), LicensedFeature.APPLICATION_REPORTS);
      map.put(ApiReportDataResourceV2.class.getDeclaredMethod(
          "getRawData",
          String.class,
          String.class
      ), LicensedFeature.APPLICATION_REPORTS);
      map.put(ApiReportDataResourceV2.class.getDeclaredMethod(
          "getPolicyViolations",
          String.class,
          String.class,
          boolean.class
      ), LicensedFeature.POLICY_VIOLATIONS);
      map.put(ApiReportDataResourceV2.class.getDeclaredMethod(
          "getDependencyTree",
          String.class,
          String.class
      ), LicensedFeature.APPLICATION_REPORTS);
      map.put(ApiReportDataResourceV2.class.getDeclaredMethod(
          "getPolicyViolationDiff",
          String.class,
          String.class,
          String.class,
          String.class,
          String.class,
          boolean.class
      ), LicensedFeature.POLICY_VIOLATIONS);

      map.put(ComponentDetailResource.class.getDeclaredMethod(
          "getApplicationDetailsByHash",
          String.class
      ), LicensedFeature.APPLICATION_REPORTS);

      map.put(ReportResource.class.getDeclaredMethod(
          "browseReport",
          String.class,
          String.class,
          String.class,
          HttpServletRequest.class
      ), LicensedFeature.APPLICATION_REPORTS);
      map.put(ReportResource.class.getDeclaredMethod(
          "getReportMetadata",
          String.class,
          String.class
      ), LicensedFeature.APPLICATION_REPORTS);
      map.put(ReportResource.class.getDeclaredMethod(
          "reevaluatePolicy",
          String.class,
          String.class,
          Boolean.class,
          HttpServletRequest.class
      ), LicensedFeature.APPLICATION_EVALUATION);
      map.put(ReportResource.class.getDeclaredMethod(
          "printReport",
          String.class,
          String.class
      ), LicensedFeature.APPLICATION_REPORTS);
      map.put(ReportResource.class.getDeclaredMethod(
          "downloadBundle",
          String.class,
          String.class
      ), LicensedFeature.APPLICATION_REPORTS);

      map.put(SecurityVulnerabilityOverrideResource.class.getDeclaredMethod(
          "applyOverride",
          OwnerType.class,
          String.class,
          SecurityVulnerabilityOverride.class,
          HttpServletRequest.class
      ), LicensedFeature.POLICY_MANAGEMENT);
    }
    catch (NoSuchMethodException e) {
      throw new RuntimeException(e);
    }

    EXPECTED_RESOURCE_CLASS_METHOD_TO_LICENSED_FEATURE = Collections.unmodifiableMap(map);
  }

  @Test
  public void testResourceClassesHaveProductLicenseEnforcementPointsWithLicensedFeatures() {
    for (Entry<Class<?>, LicensedFeature> entry : EXPECTED_RESOURCE_CLASS_TO_LICENSED_FEATURE.entrySet()) {
      Class<?> clazz = entry.getKey();
      ProductLicenseEnforcementPoint productLicenseEnforcementPoint =
          clazz.getAnnotation(ProductLicenseEnforcementPoint.class);
      assertThat(productLicenseEnforcementPoint)
          .withFailMessage("Resource class " + clazz.getName() + " is missing expected annotation @"
              + ProductLicenseEnforcementPoint.class.getSimpleName()
              + "(" + LicensedFeature.class.getSimpleName() + "." + entry.getValue().name() + ")")
          .isNotNull()
          .extracting(ProductLicenseEnforcementPoint::value).isEqualTo(entry.getValue());
    }
  }

  @Test
  public void testResourceClassMethodsHaveProductLicenseEnforcementPointsWithLicensedFeatures() {
    for (Entry<Method, LicensedFeature> entry : EXPECTED_RESOURCE_CLASS_METHOD_TO_LICENSED_FEATURE.entrySet()) {
      Method method = entry.getKey();
      assertThat(method).isNotNull();
      ProductLicenseEnforcementPoint productLicenseEnforcementPoint =
          method.getAnnotation(ProductLicenseEnforcementPoint.class);
      assertThat(productLicenseEnforcementPoint)
          .withFailMessage("Resource class method " + method.getDeclaringClass().getName() + "#" + method.getName()
              + " is missing expected annotation @"
              + ProductLicenseEnforcementPoint.class.getSimpleName()
              + "(" + LicensedFeature.class.getSimpleName() + "." + entry.getValue().name() + ")")
          .isNotNull()
          .extracting(ProductLicenseEnforcementPoint::value).isEqualTo(entry.getValue());
    }
  }

  @Test
  public void testProductLicenseEnforcementPoint_ClassLevel_MissingFeature() throws Exception {
    setFeatures();

    HttpResponse httpResponse1 = restRequest().path("test/class/productLicenseEnforcementPoint/endpoint1").post();
    HttpResponse httpResponse2 = restRequest().path("test/class/productLicenseEnforcementPoint/endpoint2").post();

    assertResponseStatus(402, httpResponse1);
    assertThat(httpResponse1.getBodyText()).isEqualTo("Your IQ Server license does not enable this feature.");
    assertResponseStatus(402, httpResponse2);
    assertThat(httpResponse2.getBodyText()).isEqualTo("Your IQ Server license does not enable this feature.");
  }

  @Test
  public void testProductLicenseEnforcementPoint_ClassLevel_HasFeature() throws Exception {
    setFeatures(LicensedFeature.AUTOMATION);

    HttpResponse httpResponse1 = restRequest().path("test/class/productLicenseEnforcementPoint/endpoint1").post();
    HttpResponse httpResponse2 = restRequest().path("test/class/productLicenseEnforcementPoint/endpoint2").post();

    assertResponseStatus(204, httpResponse1);
    assertResponseStatus(204, httpResponse2);
  }

  @Test
  public void testProductLicenseEnforcementPoint_MethodLevel_MissingFeature() throws Exception {
    setFeatures();

    HttpResponse httpResponse1 = restRequest().path("test/method/productLicenseEnforcementPoint/endpoint1").post();
    HttpResponse httpResponse2 = restRequest().path("test/method/productLicenseEnforcementPoint/endpoint2").post();

    assertResponseStatus(402, httpResponse1);
    assertThat(httpResponse1.getBodyText()).isEqualTo("Your IQ Server license does not enable this feature.");
    assertResponseStatus(204, httpResponse2);
  }

  @Test
  public void testProductLicenseEnforcementPoint_MethodLevel_HasFeature() throws Exception {
    setFeatures(LicensedFeature.AUTOMATION);

    HttpResponse httpResponse1 = restRequest().path("test/method/productLicenseEnforcementPoint/endpoint1").post();
    HttpResponse httpResponse2 = restRequest().path("test/method/productLicenseEnforcementPoint/endpoint2").post();

    assertResponseStatus(204, httpResponse1);
    assertResponseStatus(204, httpResponse2);
  }

  @Test
  public void testProductLicenseEnforcementPoint_MixedLevel_MissingBothFeatures() throws Exception {
    setFeatures();

    HttpResponse httpResponse1 = restRequest().path("test/mixed/productLicenseEnforcementPoint/endpoint1").post();
    HttpResponse httpResponse2 = restRequest().path("test/mixed/productLicenseEnforcementPoint/endpoint2").post();

    assertResponseStatus(402, httpResponse1);
    assertThat(httpResponse1.getBodyText()).isEqualTo("Your IQ Server license does not enable this feature.");
    assertResponseStatus(402, httpResponse2);
    assertThat(httpResponse2.getBodyText()).isEqualTo("Your IQ Server license does not enable this feature.");
  }

  @Test
  public void testProductLicenseEnforcementPoint_MixedLevel_OnlyClassFeature() throws Exception {
    setFeatures(LicensedFeature.AUTOMATION);

    HttpResponse httpResponse1 = restRequest().path("test/mixed/productLicenseEnforcementPoint/endpoint1").post();
    HttpResponse httpResponse2 = restRequest().path("test/mixed/productLicenseEnforcementPoint/endpoint2").post();

    assertResponseStatus(402, httpResponse1);
    assertThat(httpResponse1.getBodyText()).isEqualTo("Your IQ Server license does not enable this feature.");
    assertResponseStatus(204, httpResponse2);
  }

  @Test
  public void testProductLicenseEnforcementPoint_MixedLevel_OnlyMethodFeature() throws Exception {
    setFeatures(LicensedFeature.ENFORCEMENT);

    HttpResponse httpResponse1 = restRequest().path("test/mixed/productLicenseEnforcementPoint/endpoint1").post();
    HttpResponse httpResponse2 = restRequest().path("test/mixed/productLicenseEnforcementPoint/endpoint2").post();

    assertResponseStatus(204, httpResponse1);
    assertResponseStatus(402, httpResponse2);
    assertThat(httpResponse2.getBodyText()).isEqualTo("Your IQ Server license does not enable this feature.");
  }

  @Test
  public void testProductLicenseEnforcementPoint_MixedLevel_BothFeatures() throws Exception {
    setFeatures(LicensedFeature.AUTOMATION, LicensedFeature.ENFORCEMENT);

    HttpResponse httpResponse1 = restRequest().path("test/mixed/productLicenseEnforcementPoint/endpoint1").post();
    HttpResponse httpResponse2 = restRequest().path("test/mixed/productLicenseEnforcementPoint/endpoint2").post();

    assertResponseStatus(204, httpResponse1);
    assertResponseStatus(204, httpResponse2);
  }

  @Named
  @Singleton
  @Path("test/class/productLicenseEnforcementPoint")
  @ProductLicenseEnforcementPoint(LicensedFeature.AUTOMATION)
  public static final class TestClassLevelProductLicenseEnforcementPointResource
  {
    @POST
    @Path("endpoint1")
    public void endpoint1() {
      // no-op
    }

    @POST
    @Path("endpoint2")
    public void endpoint2() {
      // no-op
    }
  }

  @Named
  @Singleton
  @Path("test/method/productLicenseEnforcementPoint")
  public static final class TestMethodLevelProductLicenseEnforcementPointResource
  {
    @POST
    @Path("endpoint1")
    @ProductLicenseEnforcementPoint(LicensedFeature.AUTOMATION)
    public void endpoint1() {
      // no-op
    }

    @POST
    @Path("endpoint2")
    public void endpoint2() {
      // no-op
    }
  }

  @Named
  @Singleton
  @Path("test/mixed/productLicenseEnforcementPoint")
  @ProductLicenseEnforcementPoint(LicensedFeature.AUTOMATION)
  public static final class TestMixedLevelProductLicenseEnforcementPointResource
  {
    @POST
    @Path("endpoint1")
    @ProductLicenseEnforcementPoint(LicensedFeature.ENFORCEMENT)
    public void endpoint1() {
      // no-op
    }

    @POST
    @Path("endpoint2")
    public void endpoint2() {
      // no-op
    }
  }
}
