/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import {isEmpty, isNil} from 'ramda';
import template from './rawLicenseDisplay.html';

export default {
  template,
  controller: rawLicenseDisplayController,
  controllerAs: 'vm',
  bindings: {
    license: '<'
  }
};

function rawLicenseDisplayController() {
  const vm = this;

  Object.assign(vm, {
    $onInit() {
      if (vm.license) {
        vm.declaredLicenses = getDeclaredLicenses(vm.license);
        vm.observedLicenses = getObservedLicenses(vm.license);
      }
    }
  });
}

function getDeclaredLicenses({declaredLicenses}) {
  return isEmpty(declaredLicenses) ? 'Not Declared' : renderLicenses(declaredLicenses);
}

function getObservedLicenses({declaredLicenses, observedLicenses}) {
  const observed = renderLicenses(dedupLicenses(declaredLicenses, observedLicenses));
  return observed || null;
}

function renderLicenses(licenses) {
  return isNil(licenses) ? '' : licenses.join(', ');
}

/**
 * Returns the list containing all values from `licenses2` that aren't in `licenses1`
 * or which are the value 'Not Provided'
 */
function dedupLicenses(licenses1, licenses2) {
  const deduped = [];

  for (let i = 0; i < licenses2.length; i++) {
    if ('Not Provided' === licenses2[i] || licenses1.indexOf(licenses2[i]) === -1) {
      deduped.push(licenses2[i]);
    }
  }

  return deduped;
}
