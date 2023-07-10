/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { last } from 'ramda';

import reduce, { REDUCER_NAME } from 'MainRoot/firewallOnboarding/firewallOnboardingSlice';
import { steps } from 'MainRoot/firewallOnboarding/firewallOnboardingUtils';

const LOAD_UNCONFIGURED_REPOMANAGER_REQUESTED = `${REDUCER_NAME}/loadUnconfiguredRepoManagers/pending`;
const LOAD_UNCONFIGURED_REPOMANAGER_FULFILLED = `${REDUCER_NAME}/loadUnconfiguredRepoManagers/fulfilled`;
const LOAD_UNCONFIGURED_REPOMANAGER_FAILED = `${REDUCER_NAME}/loadUnconfiguredRepoManagers/rejected`;
const SAVE_REPOSITORIES_REQUESTED = `${REDUCER_NAME}/saveRepositories/pending`;
const SAVE_REPOSITORIES_FULFILLED = `${REDUCER_NAME}/saveRepositories/fulfilled`;
const SAVE_REPOSITORIES_FAILED = `${REDUCER_NAME}/saveRepositories/rejected`;
const LOAD_REPOSITORIES_REQUESTED = `${REDUCER_NAME}/loadRepositories/pending`;
const LOAD_REPOSITORIES_FULFILLED = `${REDUCER_NAME}/loadRepositories/fulfilled`;
const LOAD_REPOSITORIES_FAILED = `${REDUCER_NAME}/loadRepositories/rejected`;
const CONFIGURE_PROTECTION_RULES_REQUESTED = `${REDUCER_NAME}/configureProtectionRules/pending`;
const CONFIGURE_PROTECTION_RULES_FULFILLED = `${REDUCER_NAME}/configureProtectionRules/fulfilled`;
const CONFIGURE_PROTECTION_RULES_FAILED = `${REDUCER_NAME}/configureProtectionRules/rejected`;

describe('FirewallOnboardingReducer', () => {
  describe('unknown action', () => {
    it('returns original state', () => {
      const state = Object.freeze({ foo: 'bar' });
      const newState = reduce(state, { type: 'UNKNOWN' });

      expect(newState).toBe(state);
    });
  });

  describe('openIncompleteConfigurationModal', () => {
    it('sets showModal to true and href to payload', () => {
      const state = Object.freeze({
        incompleteConfigurationModal: {
          showModal: false,
          href: null,
        },
      });
      const { incompleteConfigurationModal } = reduce(state, {
        type: 'firewallOnboarding/openIncompleteConfigurationModal',
        payload: 'abc',
      });
      expect(incompleteConfigurationModal).toEqual({
        showModal: true,
        href: 'abc',
      });
    });
  });

  describe('closeIncompleteConfigurationModal', () => {
    it('sets showModal to false and href to null', () => {
      const state = Object.freeze({
        incompleteConfigurationModal: {
          showModal: true,
          href: 'abc',
        },
      });
      const { incompleteConfigurationModal } = reduce(state, {
        type: 'firewallOnboarding/closeIncompleteConfigurationModal',
      });
      expect(incompleteConfigurationModal).toEqual({
        showModal: false,
        href: null,
      });
    });
  });

  describe('continueToNextStep', () => {
    it('updates currentStep', () => {
      const state = Object.freeze({ currentStep: steps[0] });
      const { currentStep } = reduce(state, { type: 'firewallOnboarding/continueToNextStep' });

      expect(currentStep).toEqual(steps[1]);
    });

    describe('when current step does not have next step', () => {
      it('does not update currentStep', () => {
        const state = Object.freeze({ currentStep: last(steps) });
        const { currentStep } = reduce(state, { type: 'firewallOnboarding/continueToNextStep' });

        expect(currentStep).toEqual(last(steps));
      });
    });
  });

  describe('goBackToPreviousStep', () => {
    it('updates currentStep', () => {
      const state = Object.freeze({ currentStep: steps[1] });
      const { currentStep } = reduce(state, { type: 'firewallOnboarding/goBackToPreviousStep' });

      expect(currentStep).toEqual(steps[0]);
    });

    describe('when current step does not have previous step', () => {
      it('does not update currentStep', () => {
        const state = Object.freeze({ currentStep: steps[0] });
        const { currentStep } = reduce(state, { type: 'firewallOnboarding/goBackToPreviousStep' });

        expect(currentStep).toEqual(steps[0]);
      });
    });
  });

  describe('loadUnconfiguredRepoManagers', () => {
    describe(LOAD_UNCONFIGURED_REPOMANAGER_REQUESTED, () => {
      it('sets loading to true and clears error', () => {
        const state = Object.freeze({
          unconfiguredRepoManagers: {
            repoManagers: [],
            loading: false,
            loadError: 'error',
          },
        });

        const newState = reduce(state, {
          type: LOAD_UNCONFIGURED_REPOMANAGER_REQUESTED,
        });

        expect(newState.unconfiguredRepoManagers.loading).toBe(true);
        expect(newState.unconfiguredRepoManagers.loadError).toBe(null);
      });
    });

    describe(LOAD_UNCONFIGURED_REPOMANAGER_FAILED, () => {
      it('clears loading state and sets a load error', () => {
        const state = Object.freeze({
          unconfiguredRepoManagers: {
            repoManagers: [],
            loading: true,
            loadError: null,
          },
        });

        const newState = reduce(state, {
          type: LOAD_UNCONFIGURED_REPOMANAGER_FAILED,
          payload: 'test error',
        });

        expect(newState.unconfiguredRepoManagers.loading).toBe(false);
        expect(newState.unconfiguredRepoManagers.loadError).toBe('test error');
      });
    });

    describe(LOAD_UNCONFIGURED_REPOMANAGER_FULFILLED, () => {
      it('sets loading to false, clears load error, and sets unconfigured repo managers repoManagers', () => {
        const state = Object.freeze({
          unconfiguredRepoManagers: {
            repoManagers: [],
            loading: true,
            loadError: 'previous error',
          },
        });

        const repoManagers = [1, 2, 3];

        const newState = reduce(state, {
          type: LOAD_UNCONFIGURED_REPOMANAGER_FULFILLED,
          payload: repoManagers,
        });

        expect(newState.unconfiguredRepoManagers.loading).toBe(false);
        expect(newState.unconfiguredRepoManagers.loadError).toBe(null);
        expect(newState.unconfiguredRepoManagers.repoManagers).toBe(repoManagers);
      });
    });
  });

  describe('saveRepositories', () => {
    describe(SAVE_REPOSITORIES_REQUESTED, () => {
      it('sets loading to true and clears error', () => {
        const state = Object.freeze({
          unconfiguredRepoManagers: {
            repoManagers: [],
            loading: false,
            loadError: null,
          },
          repositories: {
            loading: false,
            loadError: null,
            saving: false,
            saveError: null,
            list: [
              {
                id: 'id',
                repositoryManagerId: 'repoManagerId',
                publicId: 'publicId',
                repositoryType: 'proxy',
                auditEnabled: true,
                quarantineEnabled: true,
                policyCompliantComponentSelectionEnabled: false,
                namespaceConfusionProtectionEnabled: false,
                format: 'maven',
              },
            ],
          },
        });

        const newState = reduce(state, {
          type: SAVE_REPOSITORIES_REQUESTED,
        });

        expect(newState.repositories.saving).toBe(true);
        expect(newState.repositories.saveError).toBe(null);
      });
    });

    describe(SAVE_REPOSITORIES_FAILED, () => {
      it('clears loading state and sets a load error', () => {
        const state = Object.freeze({
          unconfiguredRepoManagers: {
            repoManagers: [],
            loading: false,
            loadError: null,
          },
          repositories: {
            loading: false,
            loadError: null,
            saving: false,
            saveError: null,
            list: [],
          },
        });

        const newState = reduce(state, {
          type: SAVE_REPOSITORIES_FAILED,
          payload: 'test error',
        });

        expect(newState.repositories.saving).toBe(false);
        expect(newState.repositories.saveError).toBe('test error');
      });
    });

    describe(SAVE_REPOSITORIES_FULFILLED, () => {
      it('sets loading to false and clears load error', () => {
        const state = Object.freeze({
          unconfiguredRepoManagers: {
            repoManagers: [],
            loading: false,
            loadError: null,
          },
          repositories: {
            loading: false,
            loadError: null,
            saving: false,
            saveError: null,
            list: [],
          },
        });

        const newState = reduce(state, {
          type: SAVE_REPOSITORIES_FULFILLED,
          payload: state.saveRepositories,
        });

        expect(newState.repositories.saving).toBe(false);
        expect(newState.repositories.saveError).toBe(null);
      });
    });
  });

  describe('loadRepositories', () => {
    describe(LOAD_REPOSITORIES_REQUESTED, () => {
      it('sets loading to true and clears error', () => {
        const state = Object.freeze({
          unconfiguredRepoManagers: {
            repoManagers: [],
            loading: false,
            loadError: null,
          },
          repositories: {
            loading: false,
            loadError: null,
            saving: false,
            saveError: null,
            list: [
              {
                id: 'id',
                repositoryManagerId: 'repoManagerId',
                publicId: 'publicId',
                repositoryType: 'proxy',
                auditEnabled: true,
                quarantineEnabled: true,
                policyCompliantComponentSelectionEnabled: false,
                namespaceConfusionProtectionEnabled: false,
                format: 'maven',
              },
            ],
          },
        });

        const newState = reduce(state, {
          type: LOAD_REPOSITORIES_REQUESTED,
        });

        expect(newState.repositories.loading).toBe(true);
        expect(newState.repositories.loadError).toBe(null);
      });
    });

    describe(LOAD_REPOSITORIES_FAILED, () => {
      it('clears loading state and sets a load error', () => {
        const state = Object.freeze({
          unconfiguredRepoManagers: {
            repoManagers: [],
            loading: false,
            loadError: null,
          },
          repositories: {
            loading: true,
            loadError: null,
            saving: false,
            saveError: null,
            list: null,
          },
        });

        const newState = reduce(state, {
          type: LOAD_REPOSITORIES_FAILED,
          payload: 'test error',
        });

        expect(newState.repositories.list).toBe(null);
        expect(newState.repositories.loading).toBe(false);
        expect(newState.repositories.loadError).toBe('test error');
      });
    });

    describe(LOAD_REPOSITORIES_FULFILLED, () => {
      it('sets loading to false, clears load error and filter repositories with format set to null', () => {
        const state = Object.freeze({
          unconfiguredRepoManagers: {
            repoManagers: [],
            loading: false,
            loadError: null,
          },
          supportedFormats: [],
          repositories: {
            loading: true,
            loadError: null,
            saving: false,
            saveError: 'error',
            list: [],
          },
        });

        const repositories = [
          { id: '1', repositoryType: 'proxy', format: 'format1' },
          { id: '2', repositoryType: 'proxy', format: 'format2' },
          { id: '3', repositoryType: 'proxy', format: 'format3' },
          { id: '5', repositoryType: 'proxy', format: null },
          { id: '5', repositoryType: 'proxy', format: null },
        ];
        const supportedFormats = {
          maven: [],
          npm: [],
          pypi: [],
          go: [],
        };
        const newState = reduce(state, {
          type: LOAD_REPOSITORIES_FULFILLED,
          payload: {
            repositories,
            supportedFormats,
          },
        });

        expect(newState.supportedFormats).toEqual(Object.keys(supportedFormats));
        expect(newState.repositories.list).toEqual([repositories[0], repositories[1], repositories[2]]);
        expect(newState.repositories.loading).toBe(false);
        expect(newState.repositories.loadError).toBe(null);
      });
    });
  });

  describe('toggleProtectionRule', () => {
    it('toggles protection rule', () => {
      const state = Object.freeze({
        protectionRules: {
          supplyChainAttacksProtectionEnabled: false,
          namespaceConfusionProtectionEnabled: false,
        },
      });
      const { protectionRules } = reduce(state, {
        type: 'firewallOnboarding/toggleProtectionRule',
        payload: 'namespaceConfusionProtectionEnabled',
      });

      expect(protectionRules).toEqual({
        supplyChainAttacksProtectionEnabled: false,
        namespaceConfusionProtectionEnabled: true,
      });
    });
  });

  describe('configureProtectionRules', () => {
    describe(CONFIGURE_PROTECTION_RULES_REQUESTED, () => {
      it('sets configuring to true and clears error', () => {
        const state = Object.freeze({
          protectionRules: {
            supplyChainAttacksProtectionEnabled: false,
            namespaceConfusionProtectionEnabled: false,
            configuring: false,
            configureError: null,
          },
        });

        const newState = reduce(state, {
          type: CONFIGURE_PROTECTION_RULES_REQUESTED,
        });

        expect(newState.protectionRules.configuring).toBe(true);
        expect(newState.protectionRules.configureError).toBe(null);
      });
    });

    describe(CONFIGURE_PROTECTION_RULES_FAILED, () => {
      it('clears configuring state and sets a configure error', () => {
        const state = Object.freeze({
          protectionRules: {
            supplyChainAttacksProtectionEnabled: false,
            namespaceConfusionProtectionEnabled: false,
            configuring: true,
            configureError: null,
          },
        });

        const newState = reduce(state, {
          type: CONFIGURE_PROTECTION_RULES_FAILED,
          payload: 'test error',
        });

        expect(newState.protectionRules.configuring).toBe(false);
        expect(newState.protectionRules.configureError).toBe('test error');
      });
    });

    describe(CONFIGURE_PROTECTION_RULES_FULFILLED, () => {
      it('sets configuring to false and clears configure error', () => {
        const state = Object.freeze({
          protectionRules: {
            supplyChainAttacksProtectionEnabled: false,
            namespaceConfusionProtectionEnabled: false,
            configuring: true,
            configureError: null,
          },
        });

        const newState = reduce(state, {
          type: CONFIGURE_PROTECTION_RULES_FULFILLED,
          payload: state.configureProtectionRules,
        });

        expect(newState.protectionRules.configuring).toBe(false);
        expect(newState.protectionRules.configureError).toBe(null);
      });
    });
  });
});
