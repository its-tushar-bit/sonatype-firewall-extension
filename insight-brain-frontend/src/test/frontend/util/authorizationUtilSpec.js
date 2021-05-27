/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import axios from 'axios';

import { checkPermissions, authErrorMessage } from '../../../main/frontend/util/authorizationUtil';
import { getGlobalPermissionTestUrl } from '../../../main/frontend/util/CLMContextLocation';

const mockAxiosCalls = SpecUtil.axiosMockerGenerator(axios);

describe('authorizationUtil', () => {
  beforeEach(() => {
    mockAxiosCalls({
      put: {
        [getGlobalPermissionTestUrl()]: Promise.resolve({ data: ['permission1', 'permission2'] }),
      },
    });
  });

  describe('checkPermissions', () => {
    it('returns resolved empty promise if authorized for all permissions', (done) => {
      checkPermissions(['permission1', 'permission2'])
        .then((result) => {
          expect(result).toBeUndefined();
          done();
        })
        .catch(() => {
          done.fail('Promise should have been resolved');
        });
    });

    it('returns rejected promise if not authorized for at least one permission', (done) => {
      checkPermissions(['permission2'])
        .then(() => {
          done.fail('Promise should have been rejected');
        })
        .catch((message) => {
          expect(message).toBe(authErrorMessage);
          done();
        });
    });
  });
});
