/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import axios from 'axios';
import { omit } from 'ramda';
import { actions } from 'MainRoot/OrgsAndPolicies/waivedComponentUpgradesSlice';
import { getWaivedComponentUpgradeConfigUrl } from 'MainRoot/util/CLMLocation';
import ownerConstant from 'MainRoot/utility/services/owner.constant';

const { saveUpgradeStage, loadUpgradeStage } = actions;

const SAVE_UPGRADE_STAGE_REQUESTED = 'waivedComponentUpgrades/saveUpgradeStage/pending';
const SAVE_UPGRADE_STAGE_FULFILLED = 'waivedComponentUpgrades/saveUpgradeStage/fulfilled';
const SAVE_UPGRADE_STAGE_FAILED = 'waivedComponentUpgrades/saveUpgradeStage/rejected';

const LOAD_UPGRADE_STAGE_REQUESTED = 'waivedComponentUpgrades/loadUpgradeStage/pending';
const LOAD_UPGRADE_STAGE_FULFILLED = 'waivedComponentUpgrades/loadUpgradeStage/fulfilled';
const LOAD_UPGRADE_STAGE_FAILED = 'waivedComponentUpgrades/loadUpgradeStage/rejected';

const removeExtraDataFromActions = (actions) => actions.map((action) => omit(['meta', 'error', 'payload'], action));

describe('waivedComponentUpgrades actions', function () {
  let store, state, mockAxiosCalls, stageTypes;

  beforeEach(() => {
    state = {
      loading: false,
      loadError: null,
      isDirty: false,
      submitMaskState: null,
      submitError: null,
      configuredStage: 'develop',
    };
    store = SpecUtil.mockReduxStore(state);
    mockAxiosCalls = SpecUtil.axiosMockerGenerator(axios);
  });

  describe('saveUpgradeStage', () => {
    it('dispatches saveUpgradeStage/pending and saveUpgradeStage/fulfilled after a successful response', (done) => {
      mockAxiosCalls({
        put: {
          [getWaivedComponentUpgradeConfigUrl()]: Promise.resolve({ data: {} }),
        },
      });

      store.dispatch(saveUpgradeStage()).then(() => {
        const actions = removeExtraDataFromActions(store.getActions());
        expect(actions.length).toBe(2);
        expect(actions).toHaveActionsInOrder([
          { type: SAVE_UPGRADE_STAGE_REQUESTED },
          { type: SAVE_UPGRADE_STAGE_FULFILLED },
        ]);
        done();
      });
    });

    it('dispatches saveUpgradeStage/pending and saveUpgradeStage/rejected after a failed reponse', (done) => {
      mockAxiosCalls({
        put: {
          [getWaivedComponentUpgradeConfigUrl()]: () => Promise.reject('error'),
        },
      });

      store.dispatch(saveUpgradeStage()).then(() => {
        const actions = removeExtraDataFromActions(store.getActions());
        expect(actions.length).toBe(2);
        expect(actions).toHaveActionsInOrder([
          { type: SAVE_UPGRADE_STAGE_REQUESTED },
          { type: SAVE_UPGRADE_STAGE_FAILED },
        ]);
        done();
      });
    });
  });

  describe('loadUpgradeStage', () => {
    beforeEach(() => {
      stageTypes = [
        {
          stageTypeId: 'develop',
          stageName: 'Develop',
        },
        {
          stageTypeId: 'source',
          stageName: 'Source',
        },
        {
          stageTypeId: 'build',
          stageName: 'Build',
        },
        {
          stageTypeId: 'stage-release',
          stageName: 'Stage Release',
        },
        {
          stageTypeId: 'release',
          stageName: 'Release',
        },
        {
          stageTypeId: 'operate',
          stageName: 'Operate',
        },
      ];
      state = {
        router: {
          currentParams: {
            organizationId: ownerConstant.ROOT_ORGANIZATION_ID,
          },
        },
        waivedComponentUpgrades: {
          loading: false,
          loadError: null,
          isDirty: false,
          submitMaskState: null,
          submitError: null,
          configuredStage: null,
        },
        orgsAndPolicies: {
          stages: {
            cli: { stageTypes },
          },
        },
      };
      store = SpecUtil.mockReduxStore(state);
    });

    it('dispatches loadUpgradeStage/pending and loadUpgradeStage/fulfilled after a successful response', (done) => {
      mockAxiosCalls({
        get: {
          [getWaivedComponentUpgradeConfigUrl()]: Promise.resolve({ data: { stage: null } }),
        },
      });

      store.dispatch(loadUpgradeStage()).then(() => {
        const actions = removeExtraDataFromActions(store.getActions());
        expect(actions.length).toBe(2);
        expect(actions).toHaveActionsInOrder([
          { type: LOAD_UPGRADE_STAGE_REQUESTED },
          { type: LOAD_UPGRADE_STAGE_FULFILLED },
        ]);
        done();
      });
    });

    it('dispatches loadUpgradeStage/pending and loadUpgradeStage/rejected after a failed reponse', (done) => {
      mockAxiosCalls({
        get: {
          [getWaivedComponentUpgradeConfigUrl()]: () => Promise.reject('error'),
        },
      });

      store.dispatch(loadUpgradeStage()).then(() => {
        const actions = removeExtraDataFromActions(store.getActions());
        expect(actions.length).toBe(2);
        expect(actions).toHaveActionsInOrder([
          { type: LOAD_UPGRADE_STAGE_REQUESTED },
          { type: LOAD_UPGRADE_STAGE_FAILED },
        ]);
        done();
      });
    });
  });
});
