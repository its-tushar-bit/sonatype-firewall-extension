/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import {
  isAuthorized,
  isContextAuthorized,
  getValidPermissions,
  isFeatureEnabled,
} from 'MainRoot/util/permissionService';
import { getPermissionContextTestUrl, getGlobalPermissionTestUrl } from 'MainRoot/util/CLMContextLocation';
import { getProductFeaturesUrl } from 'MainRoot/util/CLMLocation';
import { axiosMockAdapter } from 'TestRoot/SpecUtil';

describe('PermissionService.js', function () {
  let mock;

  beforeAll(function () {
    mock = axiosMockAdapter();
  });

  describe('isAuthorized', function () {
    it('Single Perm, Allowed', async function () {
      mock.onPut(getGlobalPermissionTestUrl(), ['ADMIN']).reply(200, ['ADMIN']);

      const result = await isAuthorized(['ADMIN']);

      expect(result).toBe(true);
    });

    it('Single Perm, Disallowed', async function () {
      mock.onPut(getGlobalPermissionTestUrl(), ['ADMIN']).reply(200, []);

      const result = await isAuthorized(['ADMIN']);

      expect(result).toBe(false);
    });

    it('Multiple Perms, Allowed', async function () {
      mock.onPut(getGlobalPermissionTestUrl(), ['ADMIN', 'ADMIN2']).reply(200, ['ADMIN', 'ADMIN2']);

      const result = await isAuthorized(['ADMIN', 'ADMIN2']);

      expect(result).toBe(true);
    });

    it('Multiple Perms, Disallowed', async function () {
      mock.onPut(getGlobalPermissionTestUrl(), ['ADMIN', 'ADMIN2']).reply(200, ['ADMIN2']);

      const result = await isAuthorized(['ADMIN', 'ADMIN2']);

      expect(result).toBe(false);
    });

    it('Multiple Perms in different order, Allowed', async function () {
      mock.onPut(getGlobalPermissionTestUrl(), ['ADMIN', 'ADMIN2']).reply(200, ['ADMIN2', 'ADMIN']);

      const result = await isAuthorized(['ADMIN', 'ADMIN2']);

      expect(result).toBe(true);
    });

    it('Server Error', async function () {
      mock.onPut(getGlobalPermissionTestUrl(), ['ADMIN', 'ADMIN2']).reply(500, 'foo');

      await expect(isAuthorized(['ADMIN', 'ADMIN2'])).rejects.toMatchObject({
        response: { data: 'foo', status: 500 },
      });
    });
  });

  describe('isContextAuthorized', function () {
    it('Single Perm, Allowed', async function () {
      mock.onPut(getPermissionContextTestUrl('repository_container'), ['ADMIN']).reply(200, ['ADMIN']);

      const result = await isContextAuthorized(['ADMIN'], 'repository_container');

      expect(result).toBe(true);
    });

    it('Single Perm, Disallowed', async function () {
      mock.onPut(getPermissionContextTestUrl('repository_container'), ['ADMIN']).reply(200, []);

      const result = await isContextAuthorized(['ADMIN'], 'repository_container');

      expect(result).toBe(false);
    });

    it('Multiple Perms, Allowed', async function () {
      mock
        .onPut(getPermissionContextTestUrl('repository_container'), ['ADMIN', 'ADMIN2'])
        .reply(200, ['ADMIN', 'ADMIN2']);

      const result = await isContextAuthorized(['ADMIN', 'ADMIN2'], 'repository_container');

      expect(result).toBe(true);
    });

    it('Multiple Perms, Disallowed', async function () {
      mock.onPut(getPermissionContextTestUrl('repository_container'), ['ADMIN', 'ADMIN2']).reply(200, ['ADMIN2']);

      const result = await isContextAuthorized(['ADMIN', 'ADMIN2'], 'repository_container');

      expect(result).toBe(false);
    });

    it('Multiple Perms in different order, Allowed', async function () {
      mock
        .onPut(getPermissionContextTestUrl('repository_container'), ['ADMIN', 'ADMIN2'])
        .reply(200, ['ADMIN2', 'ADMIN']);

      const result = await isContextAuthorized(['ADMIN', 'ADMIN2'], 'repository_container');

      expect(result).toBe(true);
    });

    it('Server Error', async function () {
      mock.onPut(getPermissionContextTestUrl('repository_container'), ['ADMIN', 'ADMIN2']).reply(500, 'foo');

      await expect(isContextAuthorized(['ADMIN', 'ADMIN2'], 'repository_container')).rejects.toMatchObject({
        response: { data: 'foo', status: 500 },
      });
    });
  });

  describe('getValidPermissions', function () {
    it('Single Perm, Returns valid permissions', async function () {
      mock.onPut(getGlobalPermissionTestUrl(), ['ADMIN']).reply(200, ['ADMIN']);

      const result = await getValidPermissions(['ADMIN']);

      expect(result).toEqual(['ADMIN']);
    });

    it('Multiple Perms, Returns valid permissions', async function () {
      mock.onPut(getGlobalPermissionTestUrl(), ['ADMIN', 'ADMIN2']).reply(200, ['ADMIN', 'ADMIN2']);

      const result = await getValidPermissions(['ADMIN', 'ADMIN2']);

      expect(result).toEqual(['ADMIN', 'ADMIN2']);
    });

    it('Partial Perms, Returns only valid permissions', async function () {
      mock.onPut(getGlobalPermissionTestUrl(), ['ADMIN', 'ADMIN2']).reply(200, ['ADMIN']);

      const result = await getValidPermissions(['ADMIN', 'ADMIN2']);

      expect(result).toEqual(['ADMIN']);
    });

    it('No Valid Perms, Returns empty array', async function () {
      mock.onPut(getGlobalPermissionTestUrl(), ['ADMIN']).reply(200, []);

      const result = await getValidPermissions(['ADMIN']);

      expect(result).toEqual([]);
    });

    it('Server Error, Should throw', async function () {
      mock.onPut(getGlobalPermissionTestUrl(), ['ADMIN']).reply(500, 'Server Error');

      await expect(getValidPermissions(['ADMIN'])).rejects.toMatchObject({
        response: { data: 'Server Error', status: 500 },
      });
    });
  });

  describe('isFeatureEnabled', () => {
    it('Feature is enabled: should return true', async function () {
      mock.onGet(getProductFeaturesUrl()).reply(200, ['feature1', 'feature2', 'feature3']);

      const result = await isFeatureEnabled('feature1');

      expect(result).toBe(true);
    });

    it('Feature is not enabled: should return false', async function () {
      mock.onGet(getProductFeaturesUrl()).reply(200, ['feature1', 'feature2', 'feature3']);

      const result = await isFeatureEnabled('feature4');

      expect(result).toBe(false);
    });

    it('HTTP request error: should return false', async function () {
      mock.onGet(getProductFeaturesUrl()).reply(500);

      const result = await isFeatureEnabled('feature1');

      expect(result).toBe(false);
    });
  });
});
