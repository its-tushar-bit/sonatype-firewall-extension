/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import utilityModule from '../../../../main/frontend/utility/utility.module';

describe('sort.column.directive.spec.js', function () {
  var $compile, scope, isolatedScope, vm, element;

  beforeEach(angular.mock.module(utilityModule.name));
  beforeEach(inject(function (_$compile_, $rootScope) {
    scope = $rootScope.$new();

    $compile = _$compile_;
  }));

  afterEach(function () {
    scope.$destroy();
  });

  it('Test initial sort with no default', function () {
    scope.currentSortFields = [];
    element = $compile(
      '<table sort="currentSortFields" class="policy-list" cols="8"><tr class="simple">' +
        '<th sort-column="foo"><span>COL HEADER</span></th></tr></table>'
    )(scope).find('th');

    isolatedScope = element.isolateScope();
    isolatedScope.$digest();

    vm = isolatedScope.vm;

    spyOn(vm, 'updateHeader');
    expect(vm.isUp()).toBeFalsy();
    expect(vm.isDown()).toBeFalsy();
    expect(vm.field).toBe('foo');
    expect(vm.inverted).toBeFalsy();

    expect(vm.updateHeader).not.toHaveBeenCalled();
    expect(element.hasClass('selected-column')).toBe(false);
  });

  it('Test initial sort with default', function () {
    scope.currentSortFields = ['foo'];
    element = $compile(
      '<table sort="currentSortFields"><tr>' + '<th sort-column="foo"><span>COL HEADER</span></th></tr></table>'
    )(scope).find('th');

    isolatedScope = element.isolateScope();
    isolatedScope.$digest();

    vm = isolatedScope.vm;

    spyOn(vm, 'updateHeader');
    expect(vm.isUp()).toBeTruthy();
    expect(vm.isDown()).toBeFalsy();
    expect(vm.field).toBe('foo');
    expect(vm.inverted).toBeFalsy();

    expect(vm.updateHeader).not.toHaveBeenCalled();
    expect(element.hasClass('selected-column')).toBe(true);
  });

  it('Check initial sort with inverted', function () {
    scope.currentSortFields = ['foo'];
    element = $compile(
      '<table sort="currentSortFields"><tr>' +
        '<th sort-column="foo" sort-inverted="true"><span>COL HEADER</span></th></tr></table>'
    )(scope).find('th');

    isolatedScope = element.isolateScope();
    isolatedScope.$digest();

    vm = isolatedScope.vm;

    spyOn(vm, 'updateHeader');
    expect(vm.isUp()).toBeFalsy();
    expect(vm.isDown()).toBeTruthy();
    expect(vm.field).toBe('foo');
    expect(vm.inverted).toBeTruthy();

    expect(vm.updateHeader).not.toHaveBeenCalled();
    expect(element.hasClass('selected-column')).toBe(true);
  });

  it('Test sort calls', function () {
    scope.currentSortFields = [];
    element = $compile(
      '<table sort="currentSortFields" on-sort-change="currentSortFields = sortFields" ><tr>' +
        '<th sort-column="foo"><span>COL HEADER</span></th></tr></table>'
    )(scope).find('th');

    isolatedScope = element.isolateScope();
    isolatedScope.$digest();

    vm = isolatedScope.vm;

    expect(vm.isUp()).toBeFalsy();
    expect(vm.isDown()).toBeFalsy();
    expect(vm.field).toBe('foo');
    expect(vm.inverted).toBeFalsy();
    expect(element.hasClass('selected-column')).toBe(false);

    // simulate click
    vm.setSort();
    scope.$digest();

    expect(element.hasClass('selected-column')).toBe(true);
    expect(vm.isUp()).toBeTruthy();
    expect(vm.isDown()).toBeFalsy();
    expect(vm.field).toBe('foo');
    expect(vm.inverted).toBeFalsy();

    // simulate click
    vm.setSort();
    scope.$digest();

    expect(element.hasClass('selected-column')).toBe(true);
    expect(vm.isUp()).toBeFalsy();
    expect(vm.isDown()).toBeTruthy();
    expect(vm.field).toBe('foo');
    expect(vm.inverted).toBeFalsy();
  });
});
