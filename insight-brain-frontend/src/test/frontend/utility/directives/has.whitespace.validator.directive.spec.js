/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import utilityDirectivesModule from '../../../../main/frontend/utility/directives/utility.directives.module';

describe('has.whitespace.validator.directive.spec.js', function() {
  var scope;

  beforeEach(angular.mock.module(utilityDirectivesModule.name));

  beforeEach(inject(function($compile, $rootScope) {
    scope = $rootScope.$new();
  }));

  it('validates whitespace controls', inject(function($compile) {
    $compile('<ng-form name="form"><input name="control" type="text" ng-model="whitespace" has-whitespace-validator/>' +
        '</ng-form>')(scope);
    scope.$digest();

    scope.$apply(function() {
      scope.whitespace = '1234 ';
    });
    expect(scope.form.control.$error.spaces).not.toBeTruthy();

    scope.$apply(function() {
      scope.whitespace = ' 1234';
    });
    expect(scope.form.control.$error.spaces).not.toBeTruthy();

    scope.$apply(function() {
      scope.whitespace = '12  34';
    });
    expect(scope.form.control.$error.spaces).toBeTruthy();

    scope.$apply(function() {
      // tab
      scope.whitespace = '12	34';
    });
    expect(scope.form.control.$error.spaces).toBeTruthy();

    scope.$apply(function() {
      scope.whitespace = '1234';
    });
    expect(scope.form.control.$error.spaces).not.toBeTruthy();
  }));
});
