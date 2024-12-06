/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import { selectAutomatedWaiversRevocationSlice } from 'MainRoot/OrgsAndPolicies/automatedWaviersRevocationsSelector';

describe('automatedWaiversRevocationsSelectors', () => {
  describe('automatedWaiversRevocationsSlice', () => {
    it('selects autoWaiverRevocations', () => {
      const appState = {
        autoWaiverRevocations: {
          loading: false,
          loadError: null,
          data: null,
          serverData: null,
          isDirty: false,
          submitMaskState: null,
          submitError: null,
        },
      };

      const emptyAppState = {
        autoWaiverRevocations: null,
      };

      const selected = selectAutomatedWaiversRevocationSlice(appState);

      expect(selected).toEqual({
        loading: false,
        loadError: null,
        data: null,
        serverData: null,
        isDirty: false,
        submitMaskState: null,
        submitError: null,
      });

      const emptySelected = selectAutomatedWaiversRevocationSlice(emptyAppState);
      expect(emptySelected).toEqual(null);
    });
  });
});
