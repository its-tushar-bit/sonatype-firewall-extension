/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import utilityDirectivesModule from '../../../../main/frontend/utility/directives/utility.directives.module';

describe('copied.tooltip.directive.spec.js', function() {
  var element,
      scope;

  beforeEach(angular.mock.module(utilityDirectivesModule.name));

  beforeEach(inject(function($rootScope, $compile) {
    scope = $rootScope.$new();
    element = $compile('<div copied-tooltip="tooltip"></div>')(scope);
    spyOn($.fn, 'tooltip').and.returnValue(element);
  }));

  it('tooltip is not shown initially', function() {
    expect($.fn.tooltip).not.toHaveBeenCalled();
  });

  it('tooltip is shown when showTooltip is called', function() {
    expect(scope.tooltip).toBeDefined();
    expect(scope.tooltip.showTooltip).toBeDefined();

    scope.tooltip.showTooltip();
    expect($.fn.tooltip.calls.count()).toBe(2);
    expect($.fn.tooltip).toHaveBeenCalledWith('show');
  });

  it('tooltip is removed on mouseleave', function() {
    scope.tooltip.showTooltip();
    element.trigger('mouseleave');
    expect($.fn.tooltip).toHaveBeenCalledWith('destroy');
  });
});
