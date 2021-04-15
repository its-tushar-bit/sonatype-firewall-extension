/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import componentsModule from '../../../main/frontend/components/module';

/* global HealthCheck */
describe('coverageDonut', function () {
  let element, scope;

  beforeEach(angular.mock.module(componentsModule.name));

  beforeEach(inject(function ($compile, $rootScope) {
    spyOn(HealthCheck, 'artifactsChart');

    scope = $rootScope.$new();
    element = angular.element(
      `<div coverage-donut="25"
              donut-width="12"
              donut-height="14"
              fill-colors="['foo', 'bar']"
              stroke-color="'baz'"
              line-width="1"
              inner-radius="3"
              outer-radius="4">`
    );

    $compile(element)(scope);
    scope.$digest();
  }));

  it('passes the complement of the percentage, as a value out of 1, to artifactChart', function () {
    expect(HealthCheck.artifactsChart).toHaveBeenCalledWith(
      0.75,
      jasmine.anything()
    );
  });

  it('passes the element to artifactsChart', function () {
    const passedElement = HealthCheck.artifactsChart.calls.argsFor(0)[1]
      .element;

    expect(passedElement).toBe(element[0]);
  });

  it('passes the width and height from the donut-width and donut-height attributes', function () {
    const passedConfig = HealthCheck.artifactsChart.calls.argsFor(0)[1],
      { width, height } = passedConfig;

    expect(width).toBe(12);
    expect(height).toBe(14);
  });

  it('passes the fillColors, strokeColor, lineWidth, innerRadius, and outerRadius from the corresponding attributes', function () {
    const passedConfig = HealthCheck.artifactsChart.calls.argsFor(0)[1],
      {
        fillColors,
        strokeColor,
        lineWidth,
        innerRadius,
        outerRadius,
      } = passedConfig;

    expect(fillColors).toEqual(['foo', 'bar']);
    expect(strokeColor).toBe('baz');
    expect(lineWidth).toBe(1);
    expect(innerRadius).toBe(3);
    expect(outerRadius).toBe(4);
  });
});
