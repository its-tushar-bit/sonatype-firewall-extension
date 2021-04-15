/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import utilityModule from '../../../../main/frontend/utility/utility.module';

describe('threat.level.selector.directive.spec.js', function () {
  var $compile, element;

  beforeEach(angular.mock.module(utilityModule.name));
  beforeEach(inject(function (_$compile_, $rootScope) {
    var scope = $rootScope.$new();

    $compile = _$compile_;

    element = $compile(
      '<threat-level-selector ng-model="testLevel" threat-type="ltg" ' +
        'ng-disabled="disabled"></threat-level-selector>'
    )(scope);
    scope.disabled = false;
    scope.testLevel = 0;
  }));

  it('Directive creates full list of possible threat levels', function () {
    element.isolateScope().$digest();
    expect(element.find('.dropdown-menu li').length).toBe(11);
    element.find('.dropdown-menu li a').each(function (index) {
      // We convert classList in an array since it is a DOMTokentList. Jasmine allows array-like objects as of v2.3.4.
      expect(Array.prototype.slice.apply(this.classList, [0])).toContain(
        'ltg-threat-level-' + (10 - index)
      );
    });
  });

  it('Directive switches threat levels properly via selectLevel method', function () {
    var isolatedScope = element.isolateScope();

    for (var i = 0; i <= 10; i++) {
      isolatedScope.vm.selectLevel(i);
      isolatedScope.$apply();

      expect(
        Array.prototype.slice.apply(
          element.find('a.selected-threat-level').get(0).classList,
          [0]
        )
      ).toContain('ltg-threat-level-' + i);
    }
  });
});
