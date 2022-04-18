/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { deriveEditRoute, deriveViewRoute, getOwnerName } from 'MainRoot/OrgsAndPolicies/utility/util';

describe('route derivation util', () => {
  let router;

  beforeEach(() => {
    router = {
      currentState: {
        name: 'management.view.organization',
      },
      currentParams: {
        organizationId: '123',
      },
    };
  });

  it('derives edit state with only to value provided', () => {
    const toMock = 'create-label';
    const actual = deriveEditRoute(router, toMock);

    expect(actual.to).toEqual(`management.edit.organization.${toMock}`);
    expect(actual.params).toEqual(router.currentParams);
  });

  it('derives edit state with to and params values provided', () => {
    const toMock = 'create-label';
    const paramsMock = { labelId: 'foo' };

    const actual = deriveEditRoute(router, toMock, paramsMock);

    expect(actual.to).toEqual(`management.edit.organization.${toMock}`);
    expect(actual.params).toEqual({ organizationId: '123', labelId: 'foo' });
  });

  it('derives view state with no options provided', () => {
    router.currentState = { name: 'management.edit.organization' };

    const actual = deriveViewRoute(router);

    expect(actual.to).toEqual('management.view.organization');
    expect(actual.params).toEqual(router.currentParams);
  });

  it('derives route with empty string as an input', () => {
    let actual = deriveEditRoute(router, '');

    expect(actual.to).toEqual('management.edit.organization');
    expect(actual.params).toEqual(router.currentParams);
  });

  it('derives route with no input', () => {
    const actual = deriveEditRoute(router);

    expect(actual.to).toEqual('management.edit.organization');
    expect(actual.params).toEqual(router.currentParams);
  });
});

describe('getOwnerName', () => {
  it('gets the owner Name', () => {
    const owners = [
      { publicId: 'owner1', name: 'owner 1' },
      { publicId: 'owner2', name: 'owner 2' },
    ];
    const ownerName = getOwnerName('owner1')(owners);

    expect(ownerName).toBe('owner 1');
  });
});
