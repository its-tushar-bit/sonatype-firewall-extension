/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import reducer, {
  initialState,
  actions,
} from 'MainRoot/firewall/renewWaiver/renewWaiverSlice';
import { axiosMockAdapter, configureStore } from 'TestRoot/SpecUtil';
import { getWaiverDetailsUrl, renewWaiverUrl, getPolicyWaiverReasonsUrl } from 'MainRoot/util/CLMLocation';

const LOAD_WAIVER_PENDING = 'firewallRenewWaiver/loadWaiverForRenewal/pending';
const LOAD_WAIVER_FULFILLED = 'firewallRenewWaiver/loadWaiverForRenewal/fulfilled';
const LOAD_WAIVER_REJECTED = 'firewallRenewWaiver/loadWaiverForRenewal/rejected';
const SUBMIT_RENEWAL_PENDING = 'firewallRenewWaiver/submitRenewal/pending';
const SUBMIT_RENEWAL_FULFILLED = 'firewallRenewWaiver/submitRenewal/fulfilled';
const SUBMIT_RENEWAL_REJECTED = 'firewallRenewWaiver/submitRenewal/rejected';
const LOAD_REASONS_PENDING = 'firewallRenewWaiver/loadWaiverReasons/pending';
const LOAD_REASONS_FULFILLED = 'firewallRenewWaiver/loadWaiverReasons/fulfilled';
const LOAD_REASONS_REJECTED = 'firewallRenewWaiver/loadWaiverReasons/rejected';

describe('renewWaiverSlice reducers', () => {
  describe(LOAD_WAIVER_PENDING, () => {
    it('sets loading to true and clears error', () => {
      const state = { ...initialState, loadError: 'previous error', loading: false };
      const newState = reducer(state, { type: LOAD_WAIVER_PENDING });
      expect(newState.loading).toBe(true);
      expect(newState.loadError).toBeNull();
    });
  });

  describe(LOAD_WAIVER_FULFILLED, () => {
    it('sets waiver and defaults newExpiryTime to 30 for waivers with existing expiry', () => {
      const mockWaiver = { id: 'waiver-456', expiryTime: '2026-12-31T23:59:59.999Z' };
      const newState = reducer(initialState, { type: LOAD_WAIVER_FULFILLED, payload: mockWaiver });
      expect(newState.loading).toBe(false);
      expect(newState.loadError).toBeNull();
      expect(newState.waiver).toEqual(mockWaiver);
      // Waivers with an existing expiry default to 30 days
      expect(newState.newExpiryTime).toBe('30');
    });

    it('defaults newExpiryTime to never for waivers with no expiry', () => {
      const mockWaiver = { id: 'waiver-456', expiryTime: null };
      const newState = reducer(initialState, { type: LOAD_WAIVER_FULFILLED, payload: mockWaiver });
      expect(newState.waiver).toEqual(mockWaiver);
      // Never-expiring waivers default to 'never'
      expect(newState.newExpiryTime).toBe('never');
    });
  });

  describe(LOAD_WAIVER_REJECTED, () => {
    it('clears loading and sets loadError', () => {
      const state = { ...initialState, loading: true };
      const newState = reducer(state, { type: LOAD_WAIVER_REJECTED, payload: 'Waiver not found' });
      expect(newState.loading).toBe(false);
      expect(newState.loadError).toBe('Waiver not found');
    });
  });

  describe(SUBMIT_RENEWAL_PENDING, () => {
    it('sets submitMaskState to false and clears submitError', () => {
      const state = { ...initialState, submitError: 'old error', isDirty: true };
      const newState = reducer(state, { type: SUBMIT_RENEWAL_PENDING });
      expect(newState.submitMaskState).toBe(false);
      expect(newState.submitError).toBeNull();
      expect(newState.isDirty).toBe(false);
    });
  });

  describe(SUBMIT_RENEWAL_FULFILLED, () => {
    it('sets submitMaskState to true and stores return navigation state', () => {
      const returnStateName = 'firewall.waiver.details';
      const returnParams = { ownerType: 'repository', ownerId: 'owner-123', waiverId: 'waiver-456' };
      const newState = reducer(initialState, {
        type: SUBMIT_RENEWAL_FULFILLED,
        payload: { returnStateName, returnParams },
      });
      expect(newState.submitMaskState).toBe(true);
      expect(newState.submitError).toBeNull();
      expect(newState.returnStateName).toBe(returnStateName);
      expect(newState.returnParams).toEqual(returnParams);
    });

    it('stores previous state for navigation when coming from non-details page', () => {
      const returnStateName = 'firewall.quarantine';
      const returnParams = { ownerType: 'root_organization', ownerId: 'org-123' };
      const newState = reducer(initialState, {
        type: SUBMIT_RENEWAL_FULFILLED,
        payload: { returnStateName, returnParams },
      });
      expect(newState.submitMaskState).toBe(true);
      expect(newState.returnStateName).toBe('firewall.quarantine');
      expect(newState.returnParams).toEqual({ ownerType: 'root_organization', ownerId: 'org-123' });
    });
  });

  describe(SUBMIT_RENEWAL_REJECTED, () => {
    it('clears submitMaskState and sets submitError', () => {
      const state = { ...initialState, submitMaskState: false };
      const newState = reducer(state, { type: SUBMIT_RENEWAL_REJECTED, payload: 'Server error' });
      expect(newState.submitMaskState).toBeNull();
      expect(newState.submitError).toBe('Server error');
    });
  });

  describe(LOAD_REASONS_PENDING, () => {
    it('sets waiverReasonsLoading to true and clears error', () => {
      const state = { ...initialState, waiverReasonsError: 'previous error' };
      const newState = reducer(state, { type: LOAD_REASONS_PENDING });
      expect(newState.waiverReasonsLoading).toBe(true);
      expect(newState.waiverReasonsError).toBeNull();
    });
  });

  describe(LOAD_REASONS_FULFILLED, () => {
    it('sets waiverReasons and clears loading', () => {
      const reasons = [{ id: 'r1', name: 'False Positive' }, { id: 'r2', name: 'Risk Accepted' }];
      const state = { ...initialState, waiverReasonsLoading: true };
      const newState = reducer(state, { type: LOAD_REASONS_FULFILLED, payload: reasons });
      expect(newState.waiverReasonsLoading).toBe(false);
      expect(newState.waiverReasons).toEqual(reasons);
    });
  });

  describe(LOAD_REASONS_REJECTED, () => {
    it('clears loading and sets error', () => {
      const state = { ...initialState, waiverReasonsLoading: true };
      const newState = reducer(state, { type: LOAD_REASONS_REJECTED, payload: 'Server error' });
      expect(newState.waiverReasonsLoading).toBe(false);
      expect(newState.waiverReasonsError).toBe('Server error');
    });
  });

  describe('state mutations', () => {
    it('setNewExpiryTime should update expiry time and mark dirty', () => {
      const newState = reducer(initialState, actions.setNewExpiryTime('30'));
      expect(newState.newExpiryTime).toBe('30');
      expect(newState.isDirty).toBe(true);
    });

    it('setComment should update comment and mark dirty', () => {
      const newState = reducer(initialState, actions.setComment('Test comment'));
      expect(newState.comment.value).toBe('Test comment');
      expect(newState.comment.isPristine).toBe(false);
      expect(newState.isDirty).toBe(true);
    });

    it('setReasonId should update reason and mark dirty', () => {
      const newState = reducer(initialState, actions.setReasonId('reason-123'));
      expect(newState.reasonId).toBe('reason-123');
      expect(newState.isDirty).toBe(true);
    });

    it('clearSubmitError should clear submit error', () => {
      const stateWithError = { ...initialState, submitError: 'Previous error' };
      const newState = reducer(stateWithError, actions.clearSubmitError());
      expect(newState.submitError).toBeNull();
    });

    it('resetRenewWaiverState should reset to initial state', () => {
      const dirtyState = {
        ...initialState,
        waiver: { id: 'waiver-123' },
        newExpiryTime: '30',
        isDirty: true,
        submitError: 'Error',
      };
      const newState = reducer(dirtyState, actions.resetRenewWaiverState());
      expect(newState).toEqual(initialState);
    });
  });

  describe('@@reduxUiRouter/onFinish', () => {
    it('should reset state when navigating away from renew waiver page', () => {
      const dirtyState = { ...initialState, waiver: { id: 'waiver-123' }, isDirty: true };
      const action = {
        type: '@@reduxUiRouter/onFinish',
        payload: { toState: { name: 'firewall.quarantine' } },
      };
      const newState = reducer(dirtyState, action);
      expect(newState).toEqual(initialState);
    });

    it('should not reset state when staying on renew waiver page', () => {
      const dirtyState = { ...initialState, waiver: { id: 'waiver-123' }, isDirty: true };
      const action = {
        type: '@@reduxUiRouter/onFinish',
        payload: { toState: { name: 'firewall.renewWaiver' } },
      };
      const newState = reducer(dirtyState, action);
      expect(newState).toEqual(dirtyState);
    });
  });
});

describe('renewWaiverSlice async thunks', () => {
  let axiosMock;

  const defaultReduxState = {
    firewallRenewWaiver: initialState,
    router: {
      currentParams: {
        ownerType: 'repository',
        ownerId: 'owner-123',
        waiverId: 'waiver-456',
      },
      prevState: { name: 'firewall.waiver.details' },
      prevParams: {},
    },
  };

  beforeAll(() => {
    axiosMock = axiosMockAdapter();
  });

  afterEach(() => {
    axiosMock.reset();
  });

  afterAll(() => {
    axiosMock.restore();
  });

  describe('submitRenewal thunk', () => {
    it('dispatches rejected action when API returns renewed=0 with errors', async () => {
      axiosMock.onPost(renewWaiverUrl()).reply(200, {
        renewed: 0,
        errors: ['Waiver waiver-456: A policy waiver for the same policy violation already exists.'],
      });

      const store = configureStore({ reducer: { firewallRenewWaiver: reducer, router: (s = defaultReduxState.router) => s } });
      await store.dispatch(actions.submitRenewal('waiver-456'));

      const state = store.getState().firewallRenewWaiver;
      expect(state.submitMaskState).toBeNull();
      expect(state.submitError).toBeTruthy();
    });

    it('dispatches fulfilled action when API returns renewed=1', async () => {
      axiosMock.onPost(renewWaiverUrl()).reply(200, { renewed: 1 });

      const store = configureStore({ reducer: { firewallRenewWaiver: reducer, router: (s = defaultReduxState.router) => s } });
      await store.dispatch(actions.submitRenewal('waiver-456'));

      const state = store.getState().firewallRenewWaiver;
      expect(state.submitMaskState).toBe(true);
      expect(state.submitError).toBeNull();
    });

    it('sends POST with null newExpiryTime when set to never', async () => {
      axiosMock.onPost(renewWaiverUrl()).reply(200, { renewed: 1 });

      const stateWithNever = {
        ...defaultReduxState,
        firewallRenewWaiver: {
          ...initialState,
          newExpiryTime: 'never',
          comment: { value: 'Renewal comment', isPristine: false },
          reasonId: 'reason-123',
        },
      };

      const store = SpecUtil.mockReduxStore(stateWithNever);
      await store.dispatch(actions.submitRenewal('waiver-456'));

      expect(axiosMock.history.post.length).toBe(1);
      const postData = JSON.parse(axiosMock.history.post[0].data);
      expect(postData.waiverIds).toEqual(['waiver-456']);
      expect(postData.newExpiryTime).toBeNull();
      expect(postData.comment).toBe('Renewal comment');
      expect(postData.reasonId).toBe('reason-123');
    });

    it('sends POST with ISO date string when using custom date', async () => {
      axiosMock.onPost(renewWaiverUrl()).reply(200, { renewed: 1 });

      const stateWithCustom = {
        ...defaultReduxState,
        firewallRenewWaiver: {
          ...initialState,
          newExpiryTime: 'custom',
          customExpiryTime: { value: '2027-06-15', isPristine: false },
          comment: { value: 'Custom renewal', isPristine: false },
        },
      };

      const store = SpecUtil.mockReduxStore(stateWithCustom);
      await store.dispatch(actions.submitRenewal('waiver-456'));

      expect(axiosMock.history.post.length).toBe(1);
      const postData = JSON.parse(axiosMock.history.post[0].data);
      // moment endOf('day').toISOString() converts local end-of-day to UTC, so the date may differ
      expect(postData.newExpiryTime).toMatch(/^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}\.\d{3}Z$/);
      // Verify the date is in year 2027
      expect(new Date(postData.newExpiryTime).getFullYear()).toBe(2027);
    });

    it('sends POST with future date when using days-based expiry', async () => {
      axiosMock.onPost(renewWaiverUrl()).reply(200, { renewed: 1 });

      const stateWithDays = {
        ...defaultReduxState,
        firewallRenewWaiver: {
          ...initialState,
          newExpiryTime: '30',
          comment: { value: '30-day renewal', isPristine: false },
        },
      };

      const store = SpecUtil.mockReduxStore(stateWithDays);
      await store.dispatch(actions.submitRenewal('waiver-456'));

      expect(axiosMock.history.post.length).toBe(1);
      const postData = JSON.parse(axiosMock.history.post[0].data);
      expect(postData.newExpiryTime).toMatch(/^\d{4}-\d{2}-\d{2}T/);

      const expiryDate = new Date(postData.newExpiryTime);
      const now = new Date();
      const daysDiff = Math.round((expiryDate - now) / (1000 * 60 * 60 * 24));
      expect(daysDiff).toBeGreaterThanOrEqual(29);
      expect(daysDiff).toBeLessThanOrEqual(31);
    });
  });
});
