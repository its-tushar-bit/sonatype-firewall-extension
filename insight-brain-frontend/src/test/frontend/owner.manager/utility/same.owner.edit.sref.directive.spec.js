/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import ownerManagerModule from '../../../../main/frontend/owner.manager/owner.manager.module';

describe('same.owner.edit.sref.directive.spec.js', function () {
  var $compile, $rootScope, SameOwnerStateNavigationService;

  beforeEach(
    angular.mock.module(ownerManagerModule.name, function ($provide) {
      $provide.value('$cookies', {
        get: angular.noop,
      });
    })
  );

  beforeEach(inject(function (_$compile_, _$rootScope_, _SameOwnerStateNavigationService_) {
    $compile = _$compile_;
    $rootScope = _$rootScope_;
    SameOwnerStateNavigationService = _SameOwnerStateNavigationService_;
  }));

  it('Properly wrapping around ui-sref', function () {
    spyOn(SameOwnerStateNavigationService.refactorStateParams, 'edit').and.returnValue({
      to: 'management.edit.organization.label',
      params: { organizationId: '123', labelId: '123' },
    });

    var element = $compile('<button same-owner-edit-sref=label({&quot;labelId&quot;:&quot;123&quot;})></button>')(
      $rootScope
    );

    expect(element.attr('same-owner-edit-sref')).toBeUndefined();
    expect(element.attr('ui-sref')).toEqual(
      'management.edit.organization.label({"organizationId":"123","labelId":"123"})'
    );
  });
});
