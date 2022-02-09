/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import { selectOrgsAndPoliciesSlice } from 'MainRoot/OrgsAndPolicies/orgsAndPoliciesSelectors';

describe('orgsAndPoliciesSelectors', () => {
  describe('selectOrgsAndPoliciesSlice', () => {
    it('selects orgsAndPolicies', () => {
      const appState = {
        orgsAndPolicies: null,
      };

      const selected = selectOrgsAndPoliciesSlice(appState);

      expect(selected).toBeNull();
    });
  });
});
