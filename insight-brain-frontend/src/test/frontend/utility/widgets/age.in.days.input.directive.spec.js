/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import utilityModule from '../../../../main/frontend/utility/utility.module';
import legacyConfigurationModule from '../../../../main/frontend/LegacyConfigurationModule';

describe('age.in.days.input.directive.spec.js', function () {
  var element, scope, isolatedScope, vm, $compile, $httpBackend;

  beforeEach(angular.mock.module(utilityModule.name, legacyConfigurationModule.name));

  afterEach(function () {
    $httpBackend.verifyNoOutstandingExpectation();
    $httpBackend.verifyNoOutstandingRequest();
  });

  beforeEach(inject(function (_$compile_, $rootScope, _$httpBackend_) {
    scope = $rootScope.$new();
    $httpBackend = _$httpBackend_;
    $compile = _$compile_;

    scope = angular.extend(scope, {
      ageModel: '3',
    });

    element = $compile('<form name="testform"><age-in-days-input ng-model="ageModel">' + '</age-in-days-input></form>')(
      scope
    ).children();

    isolatedScope = element.isolateScope();
    vm = isolatedScope.vm;
  }));

  it('Directive formats days into the correct age and parses age into days', function () {
    scope.$digest();
    expect(element.find('input[type="number"]').val()).toEqual('3');

    expect(vm.modifierTypes.length).toBe(4);
    vm.modifierTypes.forEach(function (modifierType) {
      vm.modifier = modifierType.modifier;
      isolatedScope.$digest();
      scope.$digest();
      expect(scope.ageModel).toEqual((3 * vm.modifier).toString());
      expect(element.find('input[type="number"]').val()).toEqual('3');
    });
  });

  it('Directive parses age into correct number of days after changing input', function () {
    expect(scope.ageModel).toEqual('3');
    vm.modifier = 30;
    isolatedScope.$digest();
    element.find('input[type="number"]').val(10).trigger('input');
    expect(element.find('input[type="number"]').val()).toEqual('10');
    expect(scope.ageModel).toEqual('300');
  });

  it('Directive formats days into the correct age after changing model', function () {
    vm.modifier = 30;
    isolatedScope.$digest();
    scope.ageModel = '3000';
    scope.$digest();
    expect(element.find('input[type="number"]').val()).toEqual('100');
    expect(vm.ageInDaysModel).toEqual('3000');
  });

  describe('isRequired', function () {
    it('returns true if vm.ageInDaysRequired is undefined', function () {
      vm.ageInDaysRequired = undefined;
      expect(vm.isRequired()).toBe(true);
    });

    it('returns vm.ageInDaysRequired if it is defined', function () {
      vm.ageInDaysRequired = false;
      expect(vm.isRequired()).toBe(false);
      vm.ageInDaysRequired = true;
      expect(vm.isRequired()).toBe(true);
    });
  });

  describe('formatMax', function () {
    it('returns undefined if vm.max is undefined', function () {
      vm.max = undefined;
      expect(vm.formatMax()).toBeUndefined();
    });

    it('returns the maximum for the current modifier if vm.max is defined', function () {
      vm.max = 18249;
      vm.modifier = 365;
      expect(vm.formatMax()).toBe(49);
      vm.modifier = 30;
      expect(vm.formatMax()).toBe(608);
      vm.modifier = 7;
      expect(vm.formatMax()).toBe(2607);
      vm.modifier = 1;
      expect(vm.formatMax()).toBe(18249);
    });
  });
});
