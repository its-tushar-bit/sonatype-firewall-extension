/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import utilityModule from '../../../../main/frontend/utility/utility.module';
import legacyConfigurationModule from '../../../../main/frontend/LegacyConfigurationModule';

describe('dropdown.selector.directive.spec.js', function () {
  var element;

  beforeEach(angular.mock.module(utilityModule.name, legacyConfigurationModule.name));

  describe('with Object Options', function () {
    beforeEach(inject(function ($compile, $rootScope) {
      var scope = $rootScope.$new();

      scope = angular.extend(scope, {
        form: null,
        testModel: null,
        optionNameParam: 'name',
        options: [{ name: 'cherry' }, { name: 'orange' }, { name: 'raspberry' }],
        emptyOptionString: 'Nothing is Selected',
        noOptionsString: 'No Berries Available',
        onSelect: jasmine.createSpy(),
        disabled: false,
      });

      element = $compile(
        '<form name="form"><dropdown-selector name="dropdown" ng-model="testModel" ' +
          'options="options" ng-disabled="disabled" ng-change="onSelect(testModel)" ' +
          'empty-option-string="{{emptyOptionString}}" option-name-param="{{optionNameParam}}" ' +
          'no-options-string="{{noOptionsString}}"></dropdown-selector>' +
          '</form>'
      )(scope).children();

      scope.$digest();
    }));

    it('Directive creates full list of options', function () {
      var scope = element.scope();

      expect(element.find('.iq-form-select__item').text()).toEqual('Nothing is Selected');

      expect(element.find('.dropdown-menu li').length).toBe(scope.options.length);
      element.find('.dropdown-menu li a').each(function (index) {
        expect(this.text).toEqual(scope.options[index].name);
      });
    });

    it('Directive properly selects items', function () {
      var scope = element.scope(),
        isolatedScope = element.isolateScope(),
        vm = isolatedScope.vm;

      for (var i = 0; i < vm.options.length; i++) {
        vm.selectItem(vm.options[i]);
        isolatedScope.$apply();

        expect(element.find('.iq-form-select__item').text()).toEqual(vm.options[i].name);
        expect(scope.onSelect).toHaveBeenCalledWith(vm.options[i]);
      }
    });

    it('Directive properly re-parses option maps when options change', function () {
      var scope = element.scope(),
        vm = element.isolateScope().vm;

      expect(vm.optionViewMap['testOption']).toBeUndefined();
      scope.options.push({ name: 'testOption' });
      scope.$digest();
      expect(angular.equals(vm.optionViewMap['testOption'], { name: 'testOption' })).toBeTruthy();
    });

    it('Directive properly adds no-options class', function () {
      var scope = element.scope();

      expect(element.attr('class').split(' ')).not.toContain('no-options');
      scope.options = [];
      scope.$digest();
      expect(element.attr('class').split(' ')).toContain('no-options');
      expect(element.text().trim()).toEqual('No Berries Available');
    });

    it('Directive does not add no-options class when options are undefined', function () {
      var scope = element.scope();

      expect(element.attr('class').split(' ')).not.toContain('no-options');
      scope.options = undefined;
      scope.$digest();
      expect(element.attr('class').split(' ')).not.toContain('no-options');
      expect(element.find('span.iq-alert').length).toBe(0);
    });

    it('Directive displays disabled dropdown when options are undefined', function () {
      var scope = element.scope();
      scope.options = undefined;
      scope.$digest();

      expect(element.find('.iq-form-select__item').text().trim()).toBe('No Options Set');
      expect(element.attr('class').split(' ')).toContain('disabled');
    });

    it('Directive sets ngModelController to pristine when options change', function () {
      var scope = element.scope(),
        dropdownCtrl = scope.form.dropdown,
        vm = element.isolateScope().vm;

      expect(dropdownCtrl.$pristine).toBeTruthy();
      vm.selectItem({ name: 'orange' });
      scope.$digest();
      expect(dropdownCtrl.$pristine).toBeFalsy();
      scope.options = [{ name: 'blackberry' }, { name: 'strawberry' }];
      scope.$digest();
      expect(dropdownCtrl.$pristine).toBeTruthy();
    });
  });

  describe('with String Options', function () {
    beforeEach(inject(function ($compile, $rootScope) {
      var scope = $rootScope.$new();

      scope = angular.extend(scope, {
        testModel: null,
        options: ['cherry', 'orange', 'raspberry'],
      });

      element = $compile(
        '<form><dropdown-selector ng-model="testModel" options="options"></dropdown-selector>' + '</form>'
      )(scope).children();

      scope.$digest();
    }));

    it('Directive creates full list of string options', function () {
      var scope = element.scope();

      expect(element.find('.iq-form-select__item').text()).toEqual('None Selected');

      expect(element.find('.dropdown-menu li').length).toBe(scope.options.length);
      element.find('.dropdown-menu li a').each(function (index) {
        expect(this.text).toEqual(scope.options[index]);
      });
    });

    it('Directive properly selects string options', function () {
      var isolatedScope = element.isolateScope(),
        vm = isolatedScope.vm;

      expect(vm.optionNameParam).toBeUndefined();

      for (var i = 0; i < vm.options.length; i++) {
        vm.selectItem(vm.options[i]);
        isolatedScope.$apply();

        expect(element.find('.iq-form-select__item').text()).toEqual(vm.options[i]);
      }
    });
  });

  describe('with Track By', function () {
    beforeEach(inject(function ($compile, $rootScope) {
      var scope = $rootScope.$new();

      scope = angular.extend(scope, {
        testModel: 1,
        optionNameParam: 'name',
        options: [
          { id: 1, name: 'cherry' },
          { id: 2, name: 'orange' },
          { id: 3, name: 'raspberry' },
        ],
        optionValueParam: 'id',
      });

      element = $compile(
        '<form><dropdown-selector ng-model="testModel" options="options" ' +
          'option-value-param="{{optionValueParam}}" option-name-param="{{optionNameParam}}">' +
          '</dropdown-selector></form>'
      )(scope).children();

      scope.$digest();
    }));

    it('Directive set view Model, based on option value param', function () {
      var scope = element.scope(),
        isolatedScope = element.isolateScope(),
        vm = isolatedScope.vm;

      expect(element.find('.iq-form-select__item').text()).toEqual('cherry');

      for (var i = 0; i < vm.options.length; i++) {
        vm.selectItem(vm.options[i]);
        isolatedScope.$apply();

        expect(element.find('.iq-form-select__item').text()).toEqual(vm.options[i].name);
        expect(scope.testModel).toEqual(vm.options[i].id);
      }
    });
  });
});
