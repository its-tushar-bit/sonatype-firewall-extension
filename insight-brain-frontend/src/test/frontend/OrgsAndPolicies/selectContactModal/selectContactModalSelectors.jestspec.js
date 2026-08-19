/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import { selectContactSlice } from 'MainRoot/OrgsAndPolicies/selectContactModal/selectContactModalSelectors';

const contact = {
  selectedUser: null,
  fetchedUsers: { data: [], loading: false, loadError: null, partialError: null },
  isContactModalOpen: false,
  isDirty: false,
  loadError: null,
  loading: false,
  query: '',
  submitError: null,
  submitMaskState: null,
  isFetched: null,
  selectedId: null,
  contact: null,
};

describe('selectContactModalSelectors', () => {
  let mockState;
  beforeEach(() => {
    mockState = {
      router: {
        currentParams: {
          '#': null,
          applicationPublicId: '4',
        },
        currentState: {
          data: { title: 'Application Management', viewportSized: true },
          name: 'management.view.application',
        },
      },
      orgsAndPolicies: {
        ownerActions: {
          contact: {
            selectedUser: null,
            fetchedUsers: { data: [], loading: false, loadError: null, partialError: null },
            isContactModalOpen: false,
            isDirty: false,
            loadError: null,
            loading: false,
            query: '',
            submitError: null,
            submitMaskState: null,
            isFetched: null,
            selectedId: null,
            contact: null,
          },
        },
      },
    };
  });
  describe('selectContactSlice', () => {
    it('selects correct state', () => {
      expect(selectContactSlice(mockState)).toEqual(contact);
    });
  });
});
