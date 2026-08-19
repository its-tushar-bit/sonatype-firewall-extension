/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import com.sonatype.clm.dto.model.policy.ConditionFact;
import com.sonatype.clm.dto.model.policy.ConstraintFact;
import com.sonatype.insight.brain.dataaccess.roi.RoiConfigurationDAO;
import com.sonatype.insight.brain.dataaccess.roi.RoiConfigurationDefaultValuesDAO;
import com.sonatype.insight.brain.roi.dto.RoiFirewallMetricsDTO;
import com.sonatype.insight.brain.dataaccess.successmetrics.FirewallMetricsDAO;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.component.SecurityVulnerabilityCategory;
import com.sonatype.insight.brain.model.policy.ProxyRepositoryPolicyViolation;
import com.sonatype.insight.brain.model.policy.conditions.ProprietaryNameConflictConditionType;
import com.sonatype.insight.brain.model.policy.conditions.SecurityVulnerabilityCategoryConditionType;
import com.sonatype.insight.brain.model.repository.RepositoryContainer;
import com.sonatype.insight.brain.model.roi.CurrencyTypes;
import com.sonatype.insight.brain.model.roi.RoiConfiguration;
import com.sonatype.insight.brain.model.roi.RoiConfigurationDefaultValues;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.model.successmetrics.ApiFirewallMetricsResultDTO;
import com.sonatype.insight.brain.model.successmetrics.FirewallMetrics;
import com.sonatype.insight.brain.model.successmetrics.FirewallMetricsName;
import com.sonatype.insight.brain.product.license.InvalidLicenseException;
import com.sonatype.insight.brain.product.license.ProductLicense;
import com.sonatype.insight.brain.security.Authorize;
import com.sonatype.insight.brain.security.AuthzContext;
import com.sonatype.insight.license.model.LicensedFeature;

import com.google.common.collect.ImmutableSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.sonatype.insight.brain.model.successmetrics.FirewallMetricsName.NAMESPACE_ATTACKS_BLOCKED;
import static com.sonatype.insight.brain.model.successmetrics.FirewallMetricsName.SAFE_VERSIONS_SELECTED_AUTOMATICALLY;
import static com.sonatype.insight.brain.model.successmetrics.FirewallMetricsName.SUPPLY_CHAIN_ATTACKS_BLOCKED;
import static com.sonatype.insight.brain.utils.DateConverter.toLocalDate;

@Named
public class ApiFirewallMetricsService
{
  private static final Logger log = LoggerFactory.getLogger(ApiFirewallMetricsService.class);

  private static final Set<LicensedFeature> FIREWALL_METRICS_LICENSED_FEATURES =
      ImmutableSet.of(LicensedFeature.FIREWALL_AUTO_UNQUARANTINE, LicensedFeature.RELEASE_INTEGRITY);

  private final String maliciousCodeSummarySuffix;

  private final FirewallMetricsDAO firewallMetricsDAO;

  private final ProductLicense productLicense;

  private final RoiConfigurationDAO roiConfigurationDAO;

  private final RoiConfigurationDefaultValuesDAO roiConfigurationDefaultValuesDAO;

  private final FirewallPermissionGate firewallPermissionGate;

  @Inject
  public ApiFirewallMetricsService(
      final FirewallMetricsDAO firewallMetricsDAO,
      final ProductLicense productLicense,
      final SecurityVulnerabilityCategoryConditionType securityVulnerabilityCategoryConditionType,
      final RoiConfigurationDAO roiConfigurationDAO,
      final RoiConfigurationDefaultValuesDAO roiConfigurationDefaultValuesDAO,
      final FirewallPermissionGate firewallPermissionGate)
  {
    this.firewallMetricsDAO = firewallMetricsDAO;
    this.productLicense = productLicense;
    maliciousCodeSummarySuffix = securityVulnerabilityCategoryConditionType.getSupportedOperators().get(0) + " "
        + SecurityVulnerabilityCategory.MALICIOUS_CODE.getName();
    this.roiConfigurationDAO = roiConfigurationDAO;
    this.roiConfigurationDefaultValuesDAO = roiConfigurationDefaultValuesDAO;
    this.firewallPermissionGate = firewallPermissionGate;
  }

  @Authorize(permission = Permission.READ)
  void checkReadPermission(@SuppressWarnings("unused") @AuthzContext(AuthzContext.Key.OWNER) Owner owner) {
  }

  Map<FirewallMetricsName, ApiFirewallMetricsResultDTO> getFirewallMetrics() {
    checkProductLicense();
    firewallPermissionGate.resolvePermittedRepositoryIds(); // gate check - metrics values remain global
    Map<FirewallMetricsName, ApiFirewallMetricsResultDTO> resultMap = firewallMetricsDAO.getMetricsValueByName();
    for (FirewallMetricsName firewallMetricsName : FirewallMetricsName.values()) {
      resultMap.putIfAbsent(firewallMetricsName, new ApiFirewallMetricsResultDTO(0, null));
    }
    return resultMap;
  }

  public void incrementFirewallMetrics(FirewallMetrics firewallMetrics) {
    firewallMetricsDAO.insertUpdateFirewallMetrics(firewallMetrics);
  }

  public boolean isValidProductLicense() {
    try {
      checkProductLicense();
      return true;
    }
    catch (InvalidLicenseException e) {
      return false;
    }
  }

  public void checkProductLicense() {
    checkLicensedFeatures(productLicense::hasFeature);
  }

  public void checkLicensedFeatures(Set<LicensedFeature> licensedFeatures) {
    checkLicensedFeatures(licensedFeatures::contains);
  }

  private void checkLicensedFeatures(Predicate<LicensedFeature> predicate) {
    if (!FIREWALL_METRICS_LICENSED_FEATURES.stream().allMatch(predicate)) {
      throw new InvalidLicenseException();
    }
  }

  public void checkFirewallMetricsInRepositoryPolicyViolation(
      ProxyRepositoryPolicyViolation proxyRepositoryPolicyViolation,
      Map<LocalDate, FirewallMetrics> namespaceAttacksBlockedMetrics,
      Map<LocalDate, FirewallMetrics> supplyChainAttacksBlockedMetrics)
  {
    try {
      boolean hasProprietaryNameConflict = false;
      boolean hasSecurityVulnerabilityCategoryMaliciousCode = false;

      for (Iterator<ConstraintFact> iterator = proxyRepositoryPolicyViolation.getConstraintFacts()
          .iterator(); (!hasProprietaryNameConflict || !hasSecurityVulnerabilityCategoryMaliciousCode)
              && iterator.hasNext();)
      {
        ConstraintFact constraintFact = iterator.next();

        for (ConditionFact conditionFact : constraintFact.getConditionFacts()) {

          if (conditionFact.getConditionTypeId().equals(ProprietaryNameConflictConditionType.ID)
              && conditionFact.getSummary().endsWith(ProprietaryNameConflictConditionType.OP_IS_PRESENT))
          {
            hasProprietaryNameConflict = true;
          }
          else if (conditionFact.getConditionTypeId().equals(SecurityVulnerabilityCategoryConditionType.ID)
              && conditionFact.getSummary().endsWith(maliciousCodeSummarySuffix))
          {
            hasSecurityVulnerabilityCategoryMaliciousCode = true;
          }

          if (hasProprietaryNameConflict && hasSecurityVulnerabilityCategoryMaliciousCode) {
            break;
          }
        }
      }

      LocalDate violationLocalDate =
          proxyRepositoryPolicyViolation.getTime().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();

      if (hasProprietaryNameConflict) {
        FirewallMetrics firewallMetrics = namespaceAttacksBlockedMetrics.computeIfAbsent(violationLocalDate,
            key -> new FirewallMetrics(toLocalDate(proxyRepositoryPolicyViolation.getTime()), NAMESPACE_ATTACKS_BLOCKED,
                0));
        firewallMetrics.incrementMetricsValue(1);
      }
      if (hasSecurityVulnerabilityCategoryMaliciousCode) {
        FirewallMetrics firewallMetrics = supplyChainAttacksBlockedMetrics.computeIfAbsent(violationLocalDate,
            key -> new FirewallMetrics(toLocalDate(proxyRepositoryPolicyViolation.getTime()),
                SUPPLY_CHAIN_ATTACKS_BLOCKED,
                0));
        firewallMetrics.incrementMetricsValue(1);
      }
    }
    catch (Exception e) {
      log.error("Error checking Firewal Metrics in repository policy violation with ID {}",
          proxyRepositoryPolicyViolation.getId(), e);
    }
  }

  @Authorize(permission = Permission.CONFIGURE_SYSTEM)
  public RoiFirewallMetricsDTO getRoiFirewallMetrics(CurrencyTypes currencyType) {
    checkProductLicense();
    checkReadPermission(RepositoryContainer.SINGLETON);
    Map<FirewallMetricsName, ApiFirewallMetricsResultDTO> firewallMetrics = getFirewallMetrics();
    RoiConfiguration roiConfiguration = roiConfigurationDAO.getByCurrencyType(currencyType);
    RoiFirewallMetricsDTO roiMetricsDTO = new RoiFirewallMetricsDTO();
    if (roiConfiguration != null) {
      roiMetricsDTO.setCurrency(currencyType);
      roiMetricsDTO.setNamespaceAttacksPrevented(roiConfiguration.getNamespaceAttacksPrevented()
          .multiply(BigDecimal.valueOf(firewallMetrics.get(NAMESPACE_ATTACKS_BLOCKED).getFirewallMetricsValue())));
      roiMetricsDTO.setSafeComponentsAutoSelected(roiConfiguration.getSafeComponentsAutoSelected()
          .multiply(
              BigDecimal.valueOf(firewallMetrics.get(SAFE_VERSIONS_SELECTED_AUTOMATICALLY).getFirewallMetricsValue())));
      roiMetricsDTO.setMalwareAttacksPrevented(roiConfiguration.getMalwareAttacksPrevented()
          .multiply(BigDecimal.valueOf(firewallMetrics.get(SUPPLY_CHAIN_ATTACKS_BLOCKED).getFirewallMetricsValue())));
    }
    else {
      RoiConfigurationDefaultValues roiConfigurationDefaultValues =
          roiConfigurationDefaultValuesDAO.getByCurrencyType(currencyType);
      roiMetricsDTO.setCurrency(currencyType);
      roiMetricsDTO.setNamespaceAttacksPrevented(roiConfigurationDefaultValues.getNamespaceAttacksPreventedDefault()
          .multiply(BigDecimal.valueOf(firewallMetrics.get(NAMESPACE_ATTACKS_BLOCKED).getFirewallMetricsValue())));
      roiMetricsDTO.setSafeComponentsAutoSelected(roiConfigurationDefaultValues.getSafeComponentsAutoSelectedDefault()
          .multiply(
              BigDecimal.valueOf(firewallMetrics.get(SAFE_VERSIONS_SELECTED_AUTOMATICALLY).getFirewallMetricsValue())));
      roiMetricsDTO.setMalwareAttacksPrevented(roiConfigurationDefaultValues.getMalwareAttacksPreventedDefault()
          .multiply(BigDecimal.valueOf(firewallMetrics.get(SUPPLY_CHAIN_ATTACKS_BLOCKED).getFirewallMetricsValue())));
    }
    roiMetricsDTO.setTotalSaved(
        roiMetricsDTO.getNamespaceAttacksPrevented()
            .add(roiMetricsDTO.getSafeComponentsAutoSelected())
            .add(roiMetricsDTO.getMalwareAttacksPrevented()));
    return roiMetricsDTO;
  }
}
