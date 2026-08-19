/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import copyIdToClipboardAction from 'MainRoot/OrgsAndPolicies/copyIdToClipboardToast/copyIdToClipboardSlice';

xdescribe('CopyIdToClipboard actions', () => {
  let store, state;

  beforeEach(() => {
    state = {
      router: {
        currentParams: {},
      },
      toast: {
        toastIdInc: 0,
        toasts: [],
      },
    };
  });
  it('copyIdToClipboardAction on Root Organization level', (done) => {
    state.router.currentParams.organizationId = 'ROOT_ORGANIZATION_ID';
    store = SpecUtil.mockReduxStore(state);
    store.dispatch(copyIdToClipboardAction()).then(() => {
      const actions = store.getActions();
      expect(actions.length).toBe(3);
      expect(actions).toHaveActionTypesInOrder([
        'ownerActions/copyIdToClipboard/pending',
        'toast/addToast',
        'ownerActions/copyIdToClipboard/fulfilled',
      ]);

      done();
    });
  });

  it('copyIdToClipboardAction on Organization level', (done) => {
    state.router.currentParams.organizationId = '5d1f27a9bf12462da95436491a6050ea';
    store = SpecUtil.mockReduxStore(state);
    store.dispatch(copyIdToClipboardAction()).then(() => {
      const actions = store.getActions();
      expect(actions.length).toBe(3);
      expect(actions).toHaveActionTypesInOrder([
        'ownerActions/copyIdToClipboard/pending',
        'toast/addToast',
        'ownerActions/copyIdToClipboard/fulfilled',
      ]);

      done();
    });
  });

  it('copyIdToClipboardAction on Application level', (done) => {
    state.router.currentParams.applicationPublicId = '1';
    store = SpecUtil.mockReduxStore(state);
    store.dispatch(copyIdToClipboardAction()).then(() => {
      const actions = store.getActions();
      expect(actions.length).toBe(3);
      expect(actions).toHaveActionTypesInOrder([
        'ownerActions/copyIdToClipboard/pending',
        'toast/addToast',
        'ownerActions/copyIdToClipboard/fulfilled',
      ]);

      done();
    });
  });
});
