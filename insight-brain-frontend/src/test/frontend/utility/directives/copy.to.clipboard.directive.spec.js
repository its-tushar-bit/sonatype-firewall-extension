/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import utilityDirectivesModule from '../../../../main/frontend/utility/directives/utility.directives.module';

describe('copy.to.clipboard.directive.js', function () {
  var element, scope, $window;

  beforeEach(angular.mock.module(utilityDirectivesModule.name));

  beforeEach(inject(function ($rootScope, $compile, _$window_) {
    $window = _$window_;
    scope = $rootScope.$new();
    element = $compile(
      '<div copy-to-clipboard="vm.owner.publicId" copied-tooltip="successTooltip"></div>'
    )(scope);

    scope.vm = {
      owner: {
        publicId: 'test-app-id',
      },
    };

    spyOn(scope.successTooltip, 'showTooltip');
  }));

  describe('in non-safari browsers', function () {
    it('copies expression value and clears document selection', function () {
      spyOn($window.document, 'execCommand').and.callFake(function () {
        // the selections should contain expected text
        expect($window.getSelection().toString()).toBe('test-app-id');
        simulateCopy();
        // copy success
        return true;
      });

      element.trigger('click');
      expect($window.document.execCommand).toHaveBeenCalledWith('copy');
      // selection is cleared
      expect($window.getSelection().toString()).toBe('');

      expect(scope.successTooltip.showTooltip).toHaveBeenCalled();
    });
  });

  describe('in safari browsers', function () {
    var promptTooltip;

    beforeEach(function () {
      // simulate copy failure
      spyOn($window.document, 'execCommand').and.returnValue(false);

      // mock the tooltip
      promptTooltip = jasmine.createSpy('promptTooltip');
      spyOn($.fn, 'tooltip').and.returnValue({
        tooltip: promptTooltip,
      });
    });

    it('selects expression value and prompts user to copy', function () {
      element.trigger('click');

      expect($window.document.execCommand).toHaveBeenCalledWith('copy');
      expect($.fn.tooltip).toHaveBeenCalledWith({
        title: 'Press ⌘-C to copy',
        trigger: 'manual',
        placement: 'bottom',
      });
      expect(promptTooltip).toHaveBeenCalledWith('show');

      // selection is not cleared
      expect($window.getSelection().toString()).toBe('test-app-id');

      expect(scope.successTooltip.showTooltip).not.toHaveBeenCalled();
    });

    it('removes copy prompt and shows success on copy event', function () {
      element.trigger('click');

      simulateCopy();

      expect($.fn.tooltip).toHaveBeenCalledWith('destroy');
      expect(scope.successTooltip.showTooltip).toHaveBeenCalled();
    });
  });

  function simulateCopy() {
    $($window.document).find('body textarea').last().trigger('copy');
  }
});
