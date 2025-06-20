/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { selectOrgsAndPoliciesSlice } from 'MainRoot/OrgsAndPolicies/orgsAndPoliciesSelectors';
import {
  selectCpeConfiguration,
  selectPublicDataSourcesSlice,
} from 'MainRoot/OrgsAndPolicies/publicDataSources/publicDataSourcesSelectors';

describe('publicDataSourcesSelectors', () => {
  describe('selectPublicDataSourcesSlice', () => {
    it('is composed from the following selector', () => {
      expect(selectPublicDataSourcesSlice.dependencies).toEqual([selectOrgsAndPoliciesSlice]);
    });

    it('selects public data sources slice', () => {
      const orgsAndPoliciesSlice = {
        publicDataSources: 'public',
      };

      const selected = selectPublicDataSourcesSlice.resultFunc(orgsAndPoliciesSlice);

      expect(selected).toBe('public');
    });
  });

  describe('selectCpeConfiguration', () => {
    it('is composed from the following selector', () => {
      expect(selectCpeConfiguration.dependencies).toEqual([selectPublicDataSourcesSlice]);
    });

    it('returns the current state for the data property of the public data sources slice', () => {
      const orgsAndPoliciesSlice = {
        data: {
          enabled: true,
          enabledInParent: true,
          inheritedFromOrganizationName: 'Root Organization M',
          allowOverride: true,
          inheritedFromOrganizationAllowOverride: true,
        },
      };

      const selected = selectCpeConfiguration.resultFunc(orgsAndPoliciesSlice);

      expect(selected).not.toBeNull();
      expect(selected.enabled).toBe(true);
      expect(selected.enabledInParent).toBe(true);
      expect(selected.inheritedFromOrganizationName).toBe('Root Organization M');
      expect(selected.allowOverride).toBe(true);
      expect(selected.inheritedFromOrganizationAllowOverride).toBe(true);
    });
  });
});
