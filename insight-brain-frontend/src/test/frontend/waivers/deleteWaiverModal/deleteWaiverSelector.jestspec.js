/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { selectDeleteWaiver, selectWaiverToDelete } from 'MainRoot/waivers/deleteWaiverModal/deleteWaiverSelector';

describe('deleteWaiverSelector', () => {
  const minState = {
    deleteWaiver: {
      waiverToDelete: null,
      deleteWaiverSaving: null,
      deleteWaiverError: null,
    },
  };

  it('get deleteWaiver from state', () => {
    expect(selectDeleteWaiver(minState)).toEqual(minState.deleteWaiver);
  });
  it('get waiverToDelete from deleteWaiver', () => {
    expect(selectWaiverToDelete(minState)).toEqual(minState.deleteWaiver.waiverToDelete);
  });
});
