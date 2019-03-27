/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import template from './retentionEditor.html';

export default {
  template: template,
  controllerAs: 'vm',
  controller: RetentionEditorController
};

function RetentionEditorController(CLMContextLocations, retentionService, $q, Messages) {
  let originalRetention = undefined;

  const timeUnitMultipliers = {
    'day': 1,
    'week': 7,
    'month': 30,
    'year': 365
  };

  const vm = this;

  Object.assign(vm, {
    MIN_REPORTS: 1,
    MAX_REPORTS: 9999,
    // min age in days of 1 is enforced by age.in.days.input.directive.html
    MAX_AGE_IN_DAYS: 18249,

    isRootOrganization: CLMContextLocations.isRootOrg(),

    parentApplicationReports: undefined,
    applicationReports: undefined,

    retention: {},

    error: undefined,
    submitError: undefined,

    retentionEditorMask: undefined,

    load() {
      vm.error = undefined;
      const promises = [];
      promises.push(retentionService.getRetentionPolicies());
      if (!vm.isRootOrganization) {
        promises.push(retentionService.getRootOrganizationRetentionPolicies());
      }
      $q.all(promises).then(function(results) {
        vm.applicationReports = results[0].applicationReports;
        if (!vm.isRootOrganization) {
          vm.parentApplicationReports = results[1].applicationReports;
        }
        setRetentionFormValue();
      }, function(error) {
        vm.error = Messages.getHttpErrorMessage(error);
      });
    },

    getParentMaxReportsAndMaxAge(stage) {
      const parentApplicationReport = vm.parentApplicationReports.stages[stage];
      if (parentApplicationReport.enablePurging) {
        const prefix = 'keep at most ';
        if (parentApplicationReport.maxCount && parentApplicationReport.maxAge) {
          return prefix + parentApplicationReport.maxAge + ', ' + parentApplicationReport.maxCount + ' report' +
              (parentApplicationReport.maxCount > 1 ? 's' : '');
        }
        if (parentApplicationReport.maxCount) {
          return prefix + parentApplicationReport.maxCount + ' report' +
              (parentApplicationReport.maxCount > 1 ? 's' : '');
        }
        if (parentApplicationReport.maxAge) {
          return prefix + parentApplicationReport.maxAge;
        }
      }
      else {
        return 'don\'t purge';
      }
    },

    save() {
      vm.submitError = undefined;
      const newApplicationReports = {stages: {}};
      for (const stage in vm.retention) {
        const retention = vm.retention[stage];
        const newApplicationReport = {};
        switch (retention.formValue) {
          case 'inherit': {
            newApplicationReport.inheritPolicy = true;
            newApplicationReport.enablePurging = true;
            newApplicationReport.maxCount = null;
            newApplicationReport.maxAge = null;
            break;
          }
          case 'dontPurge': {
            newApplicationReport.inheritPolicy = false;
            newApplicationReport.enablePurging = false;
            newApplicationReport.maxCount = null;
            newApplicationReport.maxAge = null;
            break;
          }
          case 'custom': {
            newApplicationReport.inheritPolicy = false;
            newApplicationReport.enablePurging = true;
            newApplicationReport.maxCount = retention.maxCount || null;
            newApplicationReport.maxAge = retention.maxAgeInDays ? retention.maxAgeInDays + ' day' : null;
            break;
          }
        }
        newApplicationReports.stages[stage] = newApplicationReport;
      }
      const payload = {applicationReports: newApplicationReports};
      vm.retentionEditorMask.wrap(retentionService.setRetentionPolicies(payload)).then(vm.load, function(error) {
        vm.submitError = Messages.getHttpErrorMessage(error);
      });
    },

    isDirty() {
      for (const stage in vm.retention) {
        if (originalRetention[stage].formValue !== vm.retention[stage].formValue ||
            (originalRetention[stage].formValue === 'custom' &&
                !angular.equals(originalRetention[stage], vm.retention[stage]))) {
          return true;
        }
      }
      return false;
    }
  });

  function setRetentionFormValue() {
    // for each stage (e.g. build) set the value to select the button representing its data retention configuration
    // (vm.applicationReports corresponds to ApiDataRetentionPoliciesDTO.applicationReports)
    vm.retention = {};
    for (const stage in vm.applicationReports.stages) {
      const retention = vm.applicationReports.stages[stage];
      vm.retention[stage] = {};
      const stageRetention = vm.retention[stage];
      if (retention.inheritPolicy) {
        stageRetention.formValue = 'inherit';
        continue;
      }
      if (!retention.enablePurging) {
        stageRetention.formValue = 'dontPurge';
        continue;
      }
      stageRetention.formValue = 'custom';
      // set the maxCount for this stage from retention.maxCount if it exists, otherwise default to null
      // (cannot be zero)
      stageRetention.maxCount = retention.maxCount || null;
      // set the maxAgeInDays for this stage from retention.maxAge (which is a String e.g. '4 days') if it exists,
      // otherwise default to null
      if (retention.maxAge) {
        const splitMaxAge = retention.maxAge.toLowerCase().split(/\s+/);
        let invalid = !/^\d+$/.test(splitMaxAge[0]);
        if (!invalid) {
          invalid = true;
          for (const timeUnit in timeUnitMultipliers) {
            const multiplier = timeUnitMultipliers[timeUnit];
            if (splitMaxAge[1].indexOf(timeUnit) >= 0) {
              stageRetention.maxAgeInDays = (splitMaxAge[0] * multiplier).toString();
              invalid = false;
              break;
            }
          }
        }
        if (invalid) {
          vm.error = 'Unable to parse the received maximum age of "' + retention.maxAge + '" for ' + stage +
              ' application reports.';
          return;
        }
      }
      else {
        stageRetention.maxAgeInDays = null;
      }
    }
    originalRetention = angular.copy(vm.retention);
  }

  vm.load();
}

RetentionEditorController.$inject = ['CLMContextLocations', 'retentionService', '$q', 'Messages'];
