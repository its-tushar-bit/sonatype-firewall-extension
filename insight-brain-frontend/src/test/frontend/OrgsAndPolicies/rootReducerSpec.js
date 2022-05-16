/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import reducer from 'MainRoot/OrgsAndPolicies/rootSlice';

describe('root reducer', () => {
  describe('orgsAndPolicies/setSelectedOwner', () => {
    it('sets owner object', () => {
      const state = Object.freeze({
        selectedOwner: {},
      });

      const newState = reducer(state, {
        type: 'orgsAndPolicies/setSelectedOwner',
        payload: {
          name: 'ownerName',
          id: 'ownerId',
        },
      });

      expect(newState.selectedOwner.name).toBe('ownerName');
      expect(newState.selectedOwner.id).toBe('ownerId');
    });
  });

  describe('orgsAndPolicies/setSelectedOwnerContact', () => {
    it('sets contact property to selectedOwner object', () => {
      const state = Object.freeze({
        selectedOwner: {},
      });

      const newState = reducer(state, {
        type: 'orgsAndPolicies/setSelectedOwnerContact',
        payload: 'contactValue',
      });

      expect(newState.selectedOwner.contact).toBe('contactValue');
    });
  });
});
