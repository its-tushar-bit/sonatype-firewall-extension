/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { defaultTo, join, pipe } from 'ramda';

import template from './applicationReportRawData.html';

export default {
  template: template,
  controllerAs: 'vm',
  controller: ApplicationReportRawController
};

function ApplicationReportRawController($ngRedux, applicationReportActions, VulnerabilityDetails, SelectedComponent) {
  const vm = this;

  Object.assign(vm, {
    $onInit() {
      vm.unsubscribe = $ngRedux.connect(mapStateToThis, applicationReportActions)(vm);
      vm.load();
    },

    $onDestroy() {
      vm.unsubscribe();
    },

    load() {
      vm.loadReportRawData();
    },

    onRawDataComponentNameFilterChange() {
      vm.setRawDataStringFieldFilter('derivedComponentName', vm.derivedComponentNameSubstringFilter);
    },

    onRawDataLicenseFilterChange() {
      vm.setRawDataStringFieldFilter('licenseSortKey', vm.licenseSortKeySubstringFilter);
    },

    onRawDataSecurityCodeFilterChange() {
      vm.setRawDataStringFieldFilter('securityCode', vm.securityCodeSubstringFilter);
    },

    onRawDataCVSSMinFilterChange() {
      vm.setRawDataNumericMinFilter('cvssScore', vm.cvssMinNumericFilter);
    },

    onRawDataCVSSMaxFilterChange() {
      vm.setRawDataNumericMaxFilter('cvssScore', vm.cvssMaxNumericFilter);
    },

    getLicenseTooltip(rawDataEntry) {
      const placeholder = '-',
          joiner = pipe(defaultTo([]), join(', ')),
          license = rawDataEntry.license || {},
          declaredLicenses = joiner(license.declaredLicenses),
          observedLicenses = joiner(license.observedLicenses);

      return `
        <dl class="iq-license-table">
          <dt>Declared:</dt><dd>${declaredLicenses || placeholder}</dd>
          <dt>Observed:</dt><dd>${observedLicenses || placeholder}</dd>
        </dl>
      `;
    },

    openVulnerabilitiesModal(rawDataEntry) {
      const { source, securityCode } = rawDataEntry;

      SelectedComponent.toggle(rawDataEntry);
      VulnerabilityDetails.open(source, securityCode);
    }
  });
}

export function mapStateToThis({applicationReport}) {
  const { derivedComponentName, licenseSortKey, securityCode } = applicationReport.rawDataSubstringFilters;
  const { cvssScore } = applicationReport.rawDataNumericFilters;
  let cvssScoreMin, cvssScoreMax;

  if (cvssScore && cvssScore.length) {
    cvssScoreMin = cvssScore[0];
    cvssScoreMax = cvssScore[1];
  }

  return {
    ...applicationReport,
    loading: !!applicationReport.pendingLoads.size,
    derivedComponentNameSubstringFilter: derivedComponentName,
    licenseSortKeySubstringFilter: licenseSortKey,
    securityCodeSubstringFilter: securityCode,
    cvssMinNumericFilter: cvssScoreMin,
    cvssMaxNumericFilter: cvssScoreMax
  };
}

ApplicationReportRawController.$inject = [
  '$ngRedux',
  'applicationReportActions',
  'VulnerabilityDetails',
  'SelectedComponent'
];
