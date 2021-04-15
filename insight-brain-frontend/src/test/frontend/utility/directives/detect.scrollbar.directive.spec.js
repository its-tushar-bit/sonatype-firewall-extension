/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import utilityDirectivesModule from '../../../../main/frontend/utility/directives/utility.directives.module';

describe('detect.scrollbar.directive.js', function () {
  let scope, compile, element;

  const styleString = 'style="height:100px; overflow: auto;"';

  beforeEach(angular.mock.module(utilityDirectivesModule.name));

  beforeEach(inject(function ($rootScope, $compile) {
    scope = $rootScope.$new();
    compile = $compile;
  }));

  afterEach(function () {
    if (element && element.length) {
      document.body.removeChild(element[0]);
    }
  });

  it('adds .scrollbar-present class to the element if there is a scrollbar', function () {
    const children = '<div>child</div>'.repeat(100);
    const elementTemplate = angular.element(
      `<div ${styleString} detect-scrollbar="">
            ${children}
        </div>`
    );
    // Compile element and append to DOM so that CSS takes effect.
    element = compile(elementTemplate)(scope, function (elementInstance) {
      document.body.appendChild(elementInstance[0]);
    });
    scope.$digest();

    const domElement = element[0];
    expect(element).not.toBeNull();
    expect(domElement).not.toBeNull();
    expect(domElement.classList.length).toBeGreaterThan(0);
    expect(domElement.classList.contains('scrollbar-present')).toEqual(true);
  });

  it('does not add a class to the element if there is not a scrollbar', function () {
    const elementTemplate = angular.element(
      `<div ${styleString} detect-scrollbar=""></div>`
    );
    element = compile(elementTemplate)(scope, function (elementInstance) {
      document.body.appendChild(elementInstance[0]);
    });
    scope.$digest();

    const domElement = element[0];
    expect(element).not.toBeNull();
    expect(domElement).not.toBeNull();
    expect(domElement.classList.length).toBeGreaterThan(0);
    expect(domElement.classList.contains('scrollbar-present')).toEqual(false);
  });

  it('executes on state change', function () {
    const elementTemplate = angular.element(
      `<div ${styleString} detect-scrollbar="items">
          <div ng-repeat="item in items">{{item}}</div>
        <div>`
    );
    scope.items = [...Array(2).keys()];
    element = compile(elementTemplate)(scope, function (elementInstance) {
      document.body.appendChild(elementInstance[0]);
    });
    scope.$digest();

    expect(element).not.toBeNull();
    // No scrollbar
    expect(element[0].classList.contains('scrollbar-present')).toEqual(false);

    // State change
    scope.items = [...Array(30).keys()];
    scope.$digest();
    expect(element[0].classList.contains('scrollbar-present')).toEqual(true);
  });
});
