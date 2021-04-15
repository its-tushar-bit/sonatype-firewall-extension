/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import applicationReportModule from '../../../../main/frontend/applicationReport/module';

describe('rawLicenseDisplay', function () {
  let getVm;

  beforeEach(angular.mock.module(applicationReportModule.name));

  beforeEach(inject(function ($componentController) {
    getVm = (license) => {
      const vm = $componentController('rawLicenseDisplay', null, { license });
      vm.$onInit();
      return vm;
    };
  }));

  it('handles null license parameter', () => {
    const vm = getVm(null);
    expect(vm.declaredLicenses).toBeUndefined();
    expect(vm.observedLicenses).toBeUndefined();
  });

  it('handles empty licenses', () => {
    const vm = getVm({
      declaredLicenses: [],
      observedLicenses: [],
    });

    expect(vm.declaredLicenses).toBe('Not Declared');
    expect(vm.observedLicenses).toBe(null);
  });

  it('sets declaredLicenses', () => {
    const vm = getVm({
      declaredLicenses: ['foo', 'bar', 'baz'],
      observedLicenses: [],
    });

    expect(vm.declaredLicenses).toBe('foo, bar, baz');
  });

  it('sets observedLicenses', () => {
    const vm = getVm({
      declaredLicenses: [],
      observedLicenses: ['foo', 'bar', 'baz'],
    });

    expect(vm.observedLicenses).toBe('foo, bar, baz');
  });

  it('dedupes Licenses', () => {
    const vm = getVm({
      declaredLicenses: ['bar'],
      observedLicenses: ['foo', 'bar', 'baz'],
    });

    expect(vm.declaredLicenses).toBe('bar');
    expect(vm.observedLicenses).toBe('foo, baz');
  });

  it('does not dedupe "Not Provided" value', () => {
    const vm = getVm({
      declaredLicenses: ['Not Provided'],
      observedLicenses: ['Not Provided'],
    });

    expect(vm.declaredLicenses).toBe('Not Provided');
    expect(vm.observedLicenses).toBe('Not Provided');
  });
});
