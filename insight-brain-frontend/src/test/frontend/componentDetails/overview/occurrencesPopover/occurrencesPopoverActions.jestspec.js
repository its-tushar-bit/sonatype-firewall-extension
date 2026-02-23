/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { occurrencesPopoverActions } from '../../../../../main/frontend/componentDetails/overview/occurrencesPopover/occurrencesPopoverSlice';

import 'TestRoot/SpecUtil';

describe('occurrencesPopoverActions', () => {
  let store, state;

  beforeEach(function () {
    state = {
      occurrencesPopover: {
        showOccurrencesPopover: false,
      },
    };
    store = SpecUtil.mockReduxStore(state);
  });

  describe('toggleShowOccurrencesPopover action', () => {
    it('dispatches `occurrencesPopover/toggleShowOccurrencesPopover` as action', () => {
      store.dispatch(occurrencesPopoverActions.toggleShowOccurrencesPopover());

      const storeActions = store.getActions();
      expect(storeActions.length).toEqual(1);
      expect(storeActions).toHaveActionType('occurrencesPopover/toggleShowOccurrencesPopover');
    });
  });
});
