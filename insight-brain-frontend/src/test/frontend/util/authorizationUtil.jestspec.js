/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import axios from 'axios';
import { getPermissionContextTestUrl, getProductFeaturesUrl } from 'MainRoot/util/CLMLocation';

import {
  getPermissions,
  checkPermissions,
  getFeatures,
  checkFeatures,
  authErrorMessage,
  featureNotEnableErrorMessage,
} from 'MainRoot/util/authorizationUtil';

import 'TestRoot/SpecUtil';

const mockAxiosCalls = SpecUtil.axiosMockerGenerator(axios);

describe('authorizationUtil', () => {
  beforeEach(() => {
    mockAxiosCalls({
      put: {
        [getPermissionContextTestUrl('ownerType', 'ownerId')]: Promise.resolve({
          data: ['permission1', 'permission2'],
        }),
      },
      get: {
        [getProductFeaturesUrl()]: Promise.resolve({
          data: ['feature1', 'feature2'],
        }),
      },
    });
  });

  describe('checkPermissions', () => {
    it('returns resolved empty promise if authorized for all permissions', (done) => {
      checkPermissions(['permission1', 'permission2'], 'ownerType', 'ownerId')
        .then((result) => {
          expect(result).toBeUndefined();
          done();
        })
        .catch(() => {
          done.fail('Promise should have been resolved');
        });
    });

    it('returns rejected promise if not authorized for at least one permission', (done) => {
      checkPermissions(['permission2'], 'ownerType', 'ownerId')
        .then(() => {
          done.fail('Promise should have been rejected');
        })
        .catch((message) => {
          expect(message).toBe(authErrorMessage);
          done();
        });
    });
  });

  describe('getPermissions', () => {
    it('returns resolved array of authorized permissions', (done) => {
      getPermissions(['permission1', 'permission2', 'permission3'], 'ownerType', 'ownerId')
        .then((result) => {
          expect(result).toEqual(['permission1', 'permission2']);
          done();
        })
        .catch(() => {
          done.fail('Promise should have been resolved');
        });
    });
  });

  describe('checkFeatures', () => {
    it('returns resolved empty promise if all features are supported', (done) => {
      checkFeatures(['feature1', 'feature2'])
        .then((result) => {
          expect(result).toBeUndefined();
          done();
        })
        .catch(() => {
          done.fail('Promise should have been resolved');
        });
    });

    it('returns rejected promise if at least one feature is not supported', (done) => {
      checkFeatures(['feature1', 'feature2', 'feature3'])
        .then(() => {
          done.fail('Promise should have been rejected');
        })
        .catch((message) => {
          expect(message).toBe(featureNotEnableErrorMessage);
          done();
        });
    });
  });

  describe('getFeatures', () => {
    it('returns resolved array of authorized permissions', (done) => {
      getFeatures()
        .then((result) => {
          expect(result).toEqual(['feature1', 'feature2']);
          done();
        })
        .catch(() => {
          done.fail('Promise should have been resolved');
        });
    });
  });
});
