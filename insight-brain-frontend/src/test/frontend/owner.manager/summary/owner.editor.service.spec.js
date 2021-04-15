/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import ownerManagerModule from '../../../../main/frontend/owner.manager/owner.manager.module';

describe('owner.editor.service.spec.js', function () {
  beforeEach(
    angular.mock.module(ownerManagerModule.name, function ($provide) {
      $provide.value('$cookies', {
        get: angular.noop,
      });
    })
  );

  beforeEach(inject(function (Modal) {
    spyOn(Modal, 'open');
  }));

  it('open', inject(function (OwnerEditorService, Modal) {
    var owner = {
      id: 'foo',
      name: 'bar',
    };

    OwnerEditorService.open(owner, 'organization');
    expect(Modal.open).toHaveBeenCalled();

    expect(Modal.open.calls.mostRecent().args[0].resolve.owner()).toEqual(owner);
    expect(Modal.open.calls.mostRecent().args[0].resolve.ownerType()).toEqual('organization');
  }));
});
