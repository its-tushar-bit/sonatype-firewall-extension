/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import cipModalModule from '../../../../main/frontend/applicationReport/results/cipModal/module';

describe('cipSimilar', function () {
  let controller, $scope;

  beforeEach(angular.mock.module(cipModalModule.name));

  beforeEach(inject(function ($componentController, $rootScope) {
    $scope = $rootScope.$new();
    controller = $componentController('cipSimilar', { $scope });
  }));

  it('sets vm.mostSimilarComponent and vm.otherSimilarComponents based on vm.similarComponents', function () {
    controller.similarComponents = [];
    $scope.$digest();

    expect(controller.mostSimilarComponent).toBe(undefined);
    expect(controller.otherSimilarComponents).toEqual([]);

    controller.similarComponents = ['foo'];
    $scope.$digest();

    expect(controller.mostSimilarComponent).toBe('foo');
    expect(controller.otherSimilarComponents).toEqual([]);

    controller.similarComponents = ['foo', 'bar', 'baz'];
    $scope.$digest();

    expect(controller.mostSimilarComponent).toBe('foo');
    expect(controller.otherSimilarComponents).toEqual(['bar', 'baz']);

    controller.similarComponents = undefined;
    $scope.$digest();

    expect(controller.mostSimilarComponent).toBe(undefined);
    expect(controller.otherSimilarComponents).toEqual([]);
  });
});
