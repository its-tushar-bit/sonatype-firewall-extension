/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import template from './retentionEditor.html';

export default {
  template: template,
  controllerAs: 'vm',
  controller: RetentionEditorController,
};

function RetentionEditorController(CLMContextLocations, retentionService, $q, Messages) {
  const DONT_PURGE = "don't purge";

  let originalRetention = {};

  const timeUnitMultipliers = {
    day: 1,
    week: 7,
    month: 30,
    year: 365,
  };

  const vm = this;

  Object.assign(vm, {
    MIN_APPLICATION_REPORTS: 1,
    MAX_APPLICATION_REPORTS: 9999,
    // min application report age in days of 1 is enforced by age.in.days.input.directive.html
    MAX_APPLICATION_REPORT_AGE_IN_DAYS: 18249,
    MIN_SUCCESS_METRICS_AGE_IN_YEARS: 1,
    MAX_SUCCESS_METRICS_AGE_IN_YEARS: 49,

    isRootOrganization: CLMContextLocations.isRootOrg(),

    parentApplicationReportsFromServer: undefined,
    applicationReportsFromServer: undefined,

    parentSuccessMetricsFromServer: undefined,
    successMetricsFromServer: undefined,

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
      return $q.all(promises).then(
        function (results) {
          vm.applicationReportsFromServer = results[0].applicationReports;
          vm.successMetricsFromServer = results[0].successMetrics;
          if (!vm.isRootOrganization) {
            vm.parentApplicationReportsFromServer = results[1].applicationReports;
            vm.parentSuccessMetricsFromServer = results[1].successMetrics;
          }
          setRetentionFormValue();
        },
        function (error) {
          vm.error = Messages.getHttpErrorMessage(error);
        }
      );
    },

    getParentMaxReportsAndMaxAge(stage) {
      const parentApplicationReport = vm.parentApplicationReportsFromServer.stages[stage];
      if (parentApplicationReport.enablePurging) {
        const prefix = 'keep at most ';
        if (parentApplicationReport.maxCount && parentApplicationReport.maxAge) {
          return (
            prefix +
            parentApplicationReport.maxAge +
            ', ' +
            parentApplicationReport.maxCount +
            ' report' +
            (parentApplicationReport.maxCount > 1 ? 's' : '')
          );
        }
        if (parentApplicationReport.maxCount) {
          return (
            prefix + parentApplicationReport.maxCount + ' report' + (parentApplicationReport.maxCount > 1 ? 's' : '')
          );
        }
        if (parentApplicationReport.maxAge) {
          return prefix + parentApplicationReport.maxAge;
        }
      } else {
        return DONT_PURGE;
      }
    },

    getParentSuccessMetricsMaxAge() {
      return vm.parentSuccessMetricsFromServer.enablePurging
        ? 'keep last ' + vm.parentSuccessMetricsFromServer.maxAge
        : DONT_PURGE;
    },

    save() {
      vm.submitError = undefined;
      const newApplicationReports = { stages: {} };
      for (const stage in vm.retention.stages) {
        newApplicationReports.stages[stage] = toServerRetention(vm.retention.stages[stage], false);
      }
      const newSuccessMetrics = toServerRetention(vm.retention.successMetrics, true);
      const payload = {
        applicationReports: newApplicationReports,
        successMetrics: newSuccessMetrics,
      };
      vm.retentionEditorMask.wrap(
        retentionService.setRetentionPolicies(payload).then(vm.load, function (error) {
          vm.submitError = Messages.getHttpErrorMessage(error);
        })
      );
    },

    isDirty() {
      for (const stage in vm.retention.stages) {
        if (isRetentionDirty(originalRetention.stages[stage], vm.retention.stages[stage])) {
          return true;
        }
      }
      return isRetentionDirty(originalRetention.successMetrics, vm.retention.successMetrics);
    },

    titleCase(name) {
      return name.replace(/\b\w+/g, function (txt) {
        return txt.charAt(0).toUpperCase() + txt.substr(1).toLowerCase();
      });
    },
  });

  function setRetentionFormValue() {
    vm.retention = {};
    try {
      setApplicationReportRetentionFormValues();
      setSuccessMetricsRetentionFormValue();
      originalRetention = angular.copy(vm.retention);
    } catch (e) {
      vm.error = e;
    }
  }

  function setApplicationReportRetentionFormValues() {
    vm.retention.stages = {};
    for (const stage in vm.applicationReportsFromServer.stages) {
      vm.retention.stages[stage] = toRetention(vm.applicationReportsFromServer.stages[stage], false);
    }
  }

  function setSuccessMetricsRetentionFormValue() {
    vm.retention.successMetrics = toRetention(vm.successMetricsFromServer, true);
  }

  function toRetention(serverRetention, isSuccessMetrics) {
    const retention = { formValue: getFormValue(serverRetention) };
    if (retention.formValue !== 'custom') {
      return retention;
    }
    if (isSuccessMetrics) {
      const parsedMaxAge = parseMaxAge(serverRetention.maxAge);
      if (parsedMaxAge.timeUnit !== 'year') {
        throw 'Unable to parse "' + parsedMaxAge.timeUnit + '" (expected years) for success metrics.';
      }
      retention.maxAgeInYears = parsedMaxAge.value;
    } else {
      retention.maxCount = serverRetention.maxCount || null;
      if (serverRetention.maxAge) {
        const parsedMaxAge = parseMaxAge(serverRetention.maxAge);
        retention.maxAgeInDays = (parsedMaxAge.value * timeUnitMultipliers[parsedMaxAge.timeUnit]).toString();
      } else {
        retention.maxAgeInDays = null;
      }
    }
    return retention;
  }

  function toServerRetention(retention, isSuccessMetrics) {
    const serverRetention = {};
    switch (retention.formValue) {
      case 'inherit': {
        serverRetention.inheritPolicy = true;
        serverRetention.enablePurging = true;
        serverRetention.maxCount = null;
        serverRetention.maxAge = null;
        break;
      }
      case 'dontPurge': {
        serverRetention.inheritPolicy = false;
        serverRetention.enablePurging = false;
        serverRetention.maxCount = null;
        serverRetention.maxAge = null;
        break;
      }
      case 'custom': {
        serverRetention.inheritPolicy = false;
        serverRetention.enablePurging = true;
        serverRetention.maxCount = retention.maxCount || null;
        serverRetention.maxAge = isSuccessMetrics
          ? retention.maxAgeInYears + ' year'
          : retention.maxAgeInDays
          ? retention.maxAgeInDays + ' day'
          : null;
        break;
      }
    }
    if (isSuccessMetrics) {
      delete serverRetention.maxCount;
    }
    return serverRetention;
  }

  function getFormValue(serverRetention) {
    if (serverRetention.inheritPolicy) {
      return 'inherit';
    }
    if (!serverRetention.enablePurging) {
      return 'dontPurge';
    }
    return 'custom';
  }

  function parseMaxAge(maxAge) {
    const splitMaxAge = maxAge.toLowerCase().split(/\s+/);
    const timeUnit = getTimeUnit(splitMaxAge[1]);
    if (splitMaxAge.length !== 2 || !/^\d+$/.test(splitMaxAge[0]) || timeUnit === undefined) {
      throw 'Unable to parse "' + maxAge + '".';
    }
    return { value: parseInt(splitMaxAge[0], 10), timeUnit: timeUnit };
  }

  function getTimeUnit(maxAgeTimeUnit) {
    for (const timeUnit in timeUnitMultipliers) {
      if (maxAgeTimeUnit.indexOf(timeUnit) >= 0) {
        return timeUnit;
      }
    }
    return undefined;
  }

  function isRetentionDirty(originalRetention, retention) {
    return (
      originalRetention !== retention &&
      (originalRetention.formValue !== retention.formValue ||
        (originalRetention.formValue === 'custom' && !angular.equals(originalRetention, retention)))
    );
  }

  vm.load();
}

RetentionEditorController.$inject = ['CLMContextLocations', 'retentionService', '$q', 'Messages'];
