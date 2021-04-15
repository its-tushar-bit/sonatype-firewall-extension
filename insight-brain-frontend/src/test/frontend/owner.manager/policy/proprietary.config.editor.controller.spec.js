/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import ownerManagerModule from '../../../../main/frontend/owner.manager/owner.manager.module';

describe('proprietary.config.editor.controller.spec.js', function () {
  beforeEach(
    angular.mock.module(ownerManagerModule.name, function ($provide) {
      $provide.value('$cookies', {
        get: angular.noop,
      });
    })
  );

  var vm,
    $q,
    scope,
    $httpBackend,
    $timeout,
    $rootScope,
    CLMContextLocations,
    mockProprietaryConfig = ResourceUtils().createMockResource();

  beforeEach(inject(function (
    _$rootScope_,
    $injector,
    _$q_,
    $controller,
    _$timeout_,
    _$httpBackend_,
    _CLMContextLocations_
  ) {
    $rootScope = _$rootScope_;
    scope = $rootScope.$new();
    $q = _$q_;
    $httpBackend = _$httpBackend_;
    $timeout = _$timeout_;
    CLMContextLocations = _CLMContextLocations_;

    vm = $controller('proprietary.config.editor.controller', {
      $scope: scope,
    });

    vm.proprietaryConfigEditorMask = { wrap: SpecUtil.promiseWrapper($q) };
  }));

  afterEach(function () {
    $httpBackend.verifyNoOutstandingExpectation();
    $httpBackend.verifyNoOutstandingRequest();
  });

  describe('Page Changes', function () {
    beforeEach(inject(function () {
      $httpBackend
        .expectGET(CLMContextLocations.getProprietaryConfigUrl())
        .respond(ProprietaryMockData.getProprietaryConfiguration());
      $httpBackend.flush();
      vm.dirtyProprietaryConfig = mockProprietaryConfig;
      vm.dirtyProprietaryConfig.isDirty = angular.noop;
    }));

    it('clean', function () {
      spyOn(vm.dirtyProprietaryConfig, 'isDirty').and.returnValue(false);

      SpecUtil.expectStateChangeNotPrevented(scope);
      expect(vm.dirtyProprietaryConfig.isDirty).toHaveBeenCalled();
    });

    it('dirty', function () {
      spyOn(vm.dirtyProprietaryConfig, 'isDirty').and.returnValue(true);

      SpecUtil.expectStateChangePrevented(scope);
      expect(vm.dirtyProprietaryConfig.isDirty).toHaveBeenCalled();
    });
  });

  it('proprietary config editor loads properly', function () {
    $httpBackend
      .expectGET(CLMContextLocations.getProprietaryConfigUrl())
      .respond(ProprietaryMockData.getProprietaryConfiguration());
    $httpBackend.flush();

    expect(vm.dirtyProprietaryConfig.$new).toBeFalsy();
    expect(vm.localMatchers.length).toEqual(3);
    expect(vm.proprietaryConfigs.length).toEqual(2);

    expect(vm.proprietaryConfigs[0].proprietaryConfig[0].packages).toEqual(['com.sonatype', 'com.local']);
    expect(vm.proprietaryConfigs[0].proprietaryConfig[0].regexes).toEqual(['.*/test\\.zip']);

    expect(vm.proprietaryConfigs[1].proprietaryConfig[0].packages).toEqual([]);
    expect(vm.proprietaryConfigs[1].proprietaryConfig[0].regexes).toEqual(['.*/foo\\.zip']);

    expect(vm.dirtyProprietaryConfig.packages.length).toEqual(2);
    expect(vm.dirtyProprietaryConfig.regexes.length).toEqual(1);

    expect(vm.localMatchers[0].type).toEqual(vm.matcherTypes.PACKAGE);
    expect(vm.localMatchers[0].matcher).toEqual('com.sonatype');
    expect(vm.localMatchers[1].type).toEqual(vm.matcherTypes.PACKAGE);
    expect(vm.localMatchers[1].matcher).toEqual('com.local');
    expect(vm.localMatchers[2].type).toEqual(vm.matcherTypes.REGEX);
    expect(vm.localMatchers[2].matcher).toEqual('.*/test\\.zip');
  });

  it('Save fails with successful retry', function () {
    $httpBackend
      .expectGET(CLMContextLocations.getProprietaryConfigUrl())
      .respond(ProprietaryMockData.getProprietaryConfiguration());
    $httpBackend.flush();

    vm.dirtyProprietaryConfig = mockProprietaryConfig;
    vm.save();
    vm.dirtyProprietaryConfig.rejectSave('config error');

    $timeout.flush();
    expect(vm.submitError).toBe('config error');

    vm.save();
    expect(vm.submitError).toBeUndefined();
  });

  it('proprietary config editor fails to load data', function () {
    $httpBackend.expectGET(CLMContextLocations.getProprietaryConfigUrl()).respond(500, 'foo');
    $httpBackend.flush();
    $timeout.flush();
    expect(vm.loadError).toEqual('foo');
  });

  it('removes package matcher from local array and updates vm.dirtyProprietaryConfig.packages', function () {
    $httpBackend
      .expectGET(CLMContextLocations.getProprietaryConfigUrl())
      .respond(ProprietaryMockData.getProprietaryConfiguration());
    $httpBackend.flush();

    expect(vm.localMatchers.length).toBe(3);
    expect(vm.dirtyProprietaryConfig.packages).toEqual(['com.sonatype', 'com.local']);
    expect(vm.dirtyProprietaryConfig.regexes).toEqual(['.*/test\\.zip']);
    expect(vm.localMatchers[0].type).toBe(vm.matcherTypes.PACKAGE);
    expect(vm.localMatchers[1].type).toBe(vm.matcherTypes.PACKAGE);
    expect(vm.localMatchers[1].matcher).toBe('com.local');

    vm.removeMatcher(vm.localMatchers[1]);
    expect(vm.localMatchers.length).toBe(2);
    expect(vm.dirtyProprietaryConfig.packages).toEqual(['com.sonatype']);
    expect(vm.dirtyProprietaryConfig.regexes).toEqual(['.*/test\\.zip']);
  });

  it('removes regex matcher from local array and updates vm.dirtyProprietaryConfig.regexes', function () {
    $httpBackend
      .expectGET(CLMContextLocations.getProprietaryConfigUrl())
      .respond(ProprietaryMockData.getProprietaryConfiguration());
    $httpBackend.flush();

    expect(vm.localMatchers.length).toBe(3);
    expect(vm.dirtyProprietaryConfig.packages.length).toBe(2);
    expect(vm.dirtyProprietaryConfig.regexes.length).toBe(1);
    expect(vm.localMatchers[2].type).toBe(vm.matcherTypes.REGEX);

    vm.removeMatcher(vm.localMatchers[2]);
    expect(vm.localMatchers.length).toBe(2);
    expect(vm.dirtyProprietaryConfig.packages.length).toBe(2);
    expect(vm.dirtyProprietaryConfig.regexes.length).toBe(0);
  });

  it('removes same regex and package name', function () {
    $httpBackend
      .expectGET(CLMContextLocations.getProprietaryConfigUrl())
      .respond(ProprietaryMockData.getProprietaryConfiguration());
    $httpBackend.flush();

    vm.proprietaryConfigEditor = jasmine.createSpyObj('proprietaryConfigEditor', ['$setPristine']);

    // add a regex that matches the string of an existing package name
    vm.matcherType = vm.matcherTypes.REGEX;
    vm.regexMatcher = 'com.sonatype';
    vm.addMatcher();

    expect(vm.localMatchers.length).toBe(4);
    expect(vm.dirtyProprietaryConfig.packages.length).toBe(2);
    expect(vm.dirtyProprietaryConfig.regexes.length).toBe(2);
    expect(vm.localMatchers[0]).toEqual({
      type: vm.matcherTypes.PACKAGE,
      matcher: 'com.sonatype',
    });
    expect(vm.localMatchers[3]).toEqual({
      type: vm.matcherTypes.REGEX,
      matcher: 'com.sonatype',
    });

    vm.removeMatcher(vm.localMatchers[3]);
    expect(vm.localMatchers.length).toBe(3);
    expect(vm.dirtyProprietaryConfig.packages.length).toBe(2);
    // only the regex to be deleted
    expect(vm.dirtyProprietaryConfig.regexes.length).toBe(1);
  });

  it('Expect field to be cleared on add', function () {
    $httpBackend
      .expectGET(CLMContextLocations.getProprietaryConfigUrl())
      .respond(ProprietaryMockData.getProprietaryConfiguration());
    $httpBackend.flush();

    vm.matcherType = vm.matcherTypes.PACKAGE;
    vm.packageMatcher = 'foo';
    vm.proprietaryConfigEditor = jasmine.createSpyObj('proprietaryConfigEditor', ['$setPristine']);

    vm.addMatcher();
    expect(vm.packageMatcher).toBeUndefined();

    vm.matcherType = vm.matcherTypes.REGEX;
    vm.regexMatcher = 'bar';
    vm.addMatcher();
    expect(vm.regexMatcher).toBeUndefined();
  });

  it('Add package updates arrays', function () {
    $httpBackend
      .expectGET(CLMContextLocations.getProprietaryConfigUrl())
      .respond(ProprietaryMockData.getProprietaryConfiguration());
    $httpBackend.flush();

    vm.proprietaryConfigEditor = jasmine.createSpyObj('proprietaryConfigEditor', ['$setPristine']);

    expect(vm.localMatchers.length).toBe(3);
    expect(vm.dirtyProprietaryConfig.packages.length).toBe(2);
    expect(vm.dirtyProprietaryConfig.regexes.length).toBe(1);
    vm.matcherType = vm.matcherTypes.PACKAGE;
    vm.packageMatcher = 'foo';
    vm.addMatcher();
    expect(vm.localMatchers.length).toBe(4);
    expect(vm.dirtyProprietaryConfig.packages.length).toBe(3);
    expect(vm.dirtyProprietaryConfig.regexes.length).toBe(1);
  });

  it('Add regex updates arrays', function () {
    $httpBackend
      .expectGET(CLMContextLocations.getProprietaryConfigUrl())
      .respond(ProprietaryMockData.getProprietaryConfiguration());
    $httpBackend.flush();

    vm.proprietaryConfigEditor = jasmine.createSpyObj('proprietaryConfigEditor', ['$setPristine']);

    expect(vm.localMatchers.length).toBe(3);
    expect(vm.dirtyProprietaryConfig.packages.length).toBe(2);
    expect(vm.dirtyProprietaryConfig.regexes.length).toBe(1);
    vm.matcherType = vm.matcherTypes.REGEX;
    vm.regexMatcher = 'foo';
    vm.addMatcher();
    expect(vm.localMatchers.length).toBe(4);
    expect(vm.dirtyProprietaryConfig.packages.length).toBe(2);
    expect(vm.dirtyProprietaryConfig.regexes.length).toBe(2);
  });

  it('Add button disabled toggles properly with regex', function () {
    $httpBackend
      .expectGET(CLMContextLocations.getProprietaryConfigUrl())
      .respond(ProprietaryMockData.getProprietaryConfiguration());
    $httpBackend.flush();

    vm.matcherType = vm.matcherTypes.REGEX;
    expect(vm.regexMatcher).toBeUndefined();
    expect(vm.isAddButtonDisabled()).toBe(true);
    vm.regexMatcher = 'foo';
    expect(vm.isAddButtonDisabled()).toBeFalsy();
    vm.regexMatcher = undefined;
    expect(vm.isAddButtonDisabled()).toBe(true);
  });

  it('Add button disabled toggles properly with package', function () {
    $httpBackend
      .expectGET(CLMContextLocations.getProprietaryConfigUrl())
      .respond(ProprietaryMockData.getProprietaryConfiguration());
    $httpBackend.flush();

    vm.matcherType = vm.matcherTypes.PACKAGE;
    expect(vm.packageMatcher).toBe('');
    expect(vm.isAddButtonDisabled()).toBe(true);
    vm.packageMatcher = 'foo';
    expect(vm.isAddButtonDisabled()).toBeFalsy();
    vm.packageMatcher = undefined;
    expect(vm.isAddButtonDisabled()).toBe(true);
  });

  describe('Validation of Inputs', function () {
    beforeEach(function () {
      $httpBackend
        .expectGET(CLMContextLocations.getProprietaryConfigUrl())
        .respond(ProprietaryMockData.getProprietaryConfiguration());
      $httpBackend.flush();
    });

    it('Good package inputs', function () {
      expect(vm.validatePackage('com.sonatype')).toEqual({
        invalidPrefix: true,
        wildcards: true,
      });
    });

    //see CLM-1097
    it('Should treat an empty entry as valid', function () {
      expect(vm.validatePackage('')).toEqual({
        invalidPrefix: true,
        wildcards: true,
      });
    });

    it('Bad package inputs', function () {
      expect(vm.validatePackage('com sonatype')).toEqual({
        invalidPrefix: false,
        wildcards: true,
      });
      expect(vm.validatePackage('com/sonatype')).toEqual({
        invalidPrefix: false,
        wildcards: true,
      });
      expect(vm.validatePackage('com.sonatype.')).toEqual({
        invalidPrefix: false,
        wildcards: true,
      });
      expect(vm.validatePackage('.com.sonatype')).toEqual({
        invalidPrefix: false,
        wildcards: true,
      });
      expect(vm.validatePackage('com.sonatype.*')).toEqual({
        invalidPrefix: true,
        wildcards: false,
      });
      expect(vm.validatePackage('com.sonatype.**')).toEqual({
        invalidPrefix: true,
        wildcards: false,
      });
      expect(vm.validatePackage('com.sona*')).toEqual({
        invalidPrefix: true,
        wildcards: false,
      });
      expect(vm.validatePackage('*.sonatype')).toEqual({
        invalidPrefix: true,
        wildcards: false,
      });
    });
  });
});
