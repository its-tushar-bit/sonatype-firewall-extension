/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import ownerManagerModule from 'MainRoot/owner.manager/owner.manager.module';
import { mapStateToThis } from 'MainRoot/owner.manager/policy/proprietary.config.editor.controller';
import * as proprietarySelectors from 'MainRoot/OrgsAndPolicies/orgsAndPoliciesProprietarySelectors';
import { matcherTypes } from 'MainRoot/OrgsAndPolicies/orgsAndPoliciesProprietarySlice';

describe('proprietary.config.editor.controller', () => {
  beforeEach(
    angular.mock.module(ownerManagerModule.name, ($provide) => {
      SpecUtil.mockNgRedux($provide);
    })
  );

  let vm, scope, $rootScope;

  beforeEach(inject((_$rootScope_, $controller) => {
    $rootScope = _$rootScope_;
    scope = $rootScope.$new();

    vm = $controller('proprietary.config.editor.controller', {
      $scope: scope,
    });

    scope.vm = vm;
    vm.$onInit();
  }));

  describe('mapStateToThis', () => {
    it('maps redux properties to component', () => {
      spyOn(proprietarySelectors, 'selectLoadError').and.returnValue(null);
      spyOn(proprietarySelectors, 'selectSubmitError').and.returnValue(null);
      spyOn(proprietarySelectors, 'selectIsLoading').and.returnValue(false);
      spyOn(proprietarySelectors, 'selectLocalMatchers').and.returnValue([
        {
          type: 'Package',
          matcher: 'first',
        },
        {
          type: 'Package',
          matcher: 'second',
        },
        {
          type: 'Regular Expression',
          matcher: 'cuatro',
        },
      ]);

      spyOn(proprietarySelectors, 'selectProprietaryConfigs').and.returnValue({
        ownerId: '6b365e8a8000449aa924f194a7ed0d27',
        ownerName: 'dfgdf',
        ownerType: 'application',
        proprietaryConfig: {
          id: 'f977bcf69fcb464b84837f643d8f93b7',
          ownerId: '6b365e8a8000449aa924f194a7ed0d27',
          packages: ['first', 'second'],
          regexes: ['cuatro'],
        },
      });
      spyOn(proprietarySelectors, 'selectCurrentConfigs').and.returnValue({
        id: 'f977bcf69fcb464b84837f643d8f93b7',
        ownerId: '6b365e8a8000449aa924f194a7ed0d27',
        packages: ['first', 'second'],
        regexes: ['cuatro'],
      });
      spyOn(proprietarySelectors, 'selectPackageMatcher').and.returnValue('packageMatcher');
      spyOn(proprietarySelectors, 'selectRegexMatcher').and.returnValue('regexMatcher');
      spyOn(proprietarySelectors, 'selectMatcherType').and.returnValue(matcherTypes.PACKAGE);
      spyOn(proprietarySelectors, 'selectIsDirty').and.returnValue(false);

      const output = mapStateToThis({});

      expect(output.loadError).toBeNull();
      expect(output.submitError).toBeNull();
      expect(output.loading).toBeFalse();
      expect(output.isDirty).toBeFalse();

      expect(output.matcherType).toBe(matcherTypes.PACKAGE);
      expect(output.regexMatcher).toBe('regexMatcher');
      expect(output.packageMatcher).toBe('packageMatcher');
      expect(output.dirtyProprietaryConfig).toEqual({
        id: 'f977bcf69fcb464b84837f643d8f93b7',
        ownerId: '6b365e8a8000449aa924f194a7ed0d27',
        packages: ['first', 'second'],
        regexes: ['cuatro'],
      });
      expect(output.localMatchers).toEqual([
        {
          type: 'Package',
          matcher: 'first',
        },
        {
          type: 'Package',
          matcher: 'second',
        },
        {
          type: 'Regular Expression',
          matcher: 'cuatro',
        },
      ]);
      expect(output.proprietaryConfigs).toEqual({
        ownerId: '6b365e8a8000449aa924f194a7ed0d27',
        ownerName: 'dfgdf',
        ownerType: 'application',
        proprietaryConfig: {
          id: 'f977bcf69fcb464b84837f643d8f93b7',
          ownerId: '6b365e8a8000449aa924f194a7ed0d27',
          packages: ['first', 'second'],
          regexes: ['cuatro'],
        },
      });
    });
  });

  describe('$onInit()', () => {
    it('subscribes to the redux store', () => {
      expect(vm.unsubscribe).toBeDefined();
    });

    it('calls loadProprietaryConfig', () => {
      expect(vm.load).toHaveBeenCalledTimes(1);
    });
  });

  describe('$onDestroy()', () => {
    it('unsubscribes from redux store', () => {
      expect(vm.unsubscribe).not.toHaveBeenCalled();
      vm.$onDestroy();
      expect(vm.unsubscribe).toHaveBeenCalledTimes(1);
    });
  });

  describe('on pageChangeStarted', () => {
    it('navigates away if form is not dirty', () => {
      vm.isDirty = false;
      SpecUtil.expectStateChangeNotPrevented(scope);
    });

    it('does not navigate away if form is dirty', () => {
      vm.isDirty = true;
      SpecUtil.expectStateChangePrevented(scope);
    });
  });

  describe('on remove matcher', () => {
    it('calls removeMatcher on matcher remove', () => {
      expect(vm.removeMatcher).not.toHaveBeenCalled();
      vm.removeMatcher({ type: matcherTypes.PACKAGE, matcher: 'match' });
      expect(vm.removeMatcher).toHaveBeenCalledOnceWith({ type: matcherTypes.PACKAGE, matcher: 'match' });
    });
  });

  describe('on add matcher', () => {
    beforeEach(() => {
      vm.proprietaryConfigEditor = {
        $setPristine: jasmine.createSpy('$setPristine'),
      };
      vm.matcherType = matcherTypes.REGEX;
      vm.regexMatcher = '';
    });

    it('does not call addLocalMatcher is there is no matcher to add', () => {
      expect(vm.addLocalMatcher).not.toHaveBeenCalled();
    });

    it('calls addLocalMatcher, resetMatcher on matcher add', () => {
      expect(vm.addLocalMatcher).not.toHaveBeenCalled();

      vm.regexMatcher = 'value';

      vm.addMatcher();

      expect(vm.addLocalMatcher).toHaveBeenCalledOnceWith({ type: matcherTypes.REGEX, matcher: 'value' });
      expect(vm.resetMatcher).toHaveBeenCalledTimes(1);
      expect(vm.proprietaryConfigEditor.$setPristine).toHaveBeenCalledTimes(1);
    });
  });

  describe('on save', () => {
    it('calls saveConfig on configuration save', () => {
      vm.proprietaryConfigEditorMask = {
        wrap: jasmine.createSpy('wrap'),
      };

      vm.save();
      expect(vm.saveConfig).toHaveBeenCalledTimes(1);
    });
  });

  describe('on edit', () => {
    beforeEach(() => {
      vm.packageMatcher = 'packageMatcher';
      vm.regexMatcher = 'regexMatcher';
      vm.matcherType = matcherTypes.REGEX;
    });

    it('calls setMatcherRegexValue on regex field change', () => {
      expect(vm.setMatcherRegexValue).not.toHaveBeenCalled();
      vm.setRegexValue();
      expect(vm.setMatcherRegexValue).toHaveBeenCalledOnceWith('regexMatcher');
    });

    it('calls setMatcherPackageValue on package field change', () => {
      expect(vm.setMatcherPackageValue).not.toHaveBeenCalled();
      vm.setPackageValue();
      expect(vm.setMatcherPackageValue).toHaveBeenCalledOnceWith('packageMatcher');
    });

    it('calls setMatcherTypeValue on dropdown field change', () => {
      expect(vm.setMatcherTypeValue).not.toHaveBeenCalled();
      vm.setMatcherType();
      expect(vm.setMatcherTypeValue).toHaveBeenCalledOnceWith(matcherTypes.REGEX);
    });
  });

  describe('isAddButtonDisabled', () => {
    beforeEach(() => {
      vm.proprietaryConfigEditor = {
        $valid: true,
      };
    });

    it('returns true if form is valid and no value', () => {
      vm.matcherType = matcherTypes.REGEX;
      vm.regexMatcher = '';

      expect(vm.isAddButtonDisabled()).toBeTrue();
    });

    it('returns false if form and field value are both valid', () => {
      vm.matcherType = matcherTypes.REGEX;
      vm.regexMatcher = 'value';

      expect(vm.isAddButtonDisabled()).toBeFalse();
    });

    it('returns true if field has a value but form is invalid', () => {
      vm.proprietaryConfigEditor = {
        $valid: false,
      };

      vm.matcherType = matcherTypes.REGEX;
      vm.regexMatcher = 'c';

      expect(vm.isAddButtonDisabled()).toBeTrue();
    });
  });

  describe('validatePackage', () => {
    it('good package inputs', () => {
      expect(vm.validatePackage('com.sonatype')).toEqual({
        invalidPrefix: true,
        wildcards: true,
      });
    });

    //see CLM-1097
    it('should treat an empty entry as valid', () => {
      expect(vm.validatePackage('')).toEqual({
        invalidPrefix: true,
        wildcards: true,
      });
    });

    it('bad package inputs', () => {
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
