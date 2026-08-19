/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { compose, last } from 'ramda';

import { render, screen, within, fireEvent } from 'TestRoot/SpecUtil';
import PolicyActionsEditor from 'MainRoot/OrgsAndPolicies/policyEditor/policyActionsEditor';
import { actions as policyActions, initialState } from 'MainRoot/OrgsAndPolicies/policySlice';
import { pathSet } from 'MainRoot/util/jsUtil';

import 'TestRoot/SpecUtil';

const actionStages = [
  { stageTypeId: 'proxy', shortName: 'proxy' },
  { stageTypeId: 'build', shortName: 'build' },
  { stageTypeId: 'operate', shortName: 'operate' },
];

const sbomStages = [{ stageTypeId: 'compliance', shortName: 'compliance' }];

describe('PolicyActionsEditor', () => {
  let renderComponent, state;

  beforeEach(() => {
    state = {
      orgsAndPolicies: {
        root: {
          policiesByOwner: [],
          selectedOwner: {
            id: 'ownerId',
          },
        },
        policy: {
          ...initialState,
          isInherited: false,
          overrideActionsFlag: false,
          hasEditIqPermission: true,
          currentPolicy: {
            actions: {},
            constraints: [],
          },
          notificationWebhooks: [],
        },
        stages: {
          action: { stageTypes: actionStages, loading: false, error: null },
          sbom: { stageTypes: sbomStages, loading: false, error: null },
        },
      },
      router: {
        currentParams: { organizationId: 'organizationId' },
        currentState: { name: 'organization' },
      },
      productFeatures: {
        productFeatures: {
          notifications: true,
          firewall: false,
          enforcement: false,
          'policy-monitoring': true,
        },
      },
    };
    renderComponent = (preloadedState = state) => render(<PolicyActionsEditor />, { preloadedState });
  });

  it('does not render actions disabled message when enforcement is supported', () => {
    const preloadedState = compose(pathSet(['productFeatures', 'productFeatures', 'enforcement'], true))(state);
    renderComponent(preloadedState);

    const actionsAreDisabledAlert = screen.queryByText('Actions are not supported by your product license.');
    const onlyProxyActionsAreSupportedAlert = screen.queryByText(
      'Only Proxy Actions are supported with your Firewall product license.'
    );

    expect(actionsAreDisabledAlert).toBeNull();
    expect(onlyProxyActionsAreSupportedAlert).toBeNull();
  });

  it('renders table with enabled radios when parent actions overriding is enabled', () => {
    const preloadedState = compose(
      pathSet(['productFeatures', 'productFeatures', 'firewall'], true),
      pathSet(['productFeatures', 'productFeatures', 'enforcement'], true),
      pathSet(['orgsAndPolicies', 'policy', 'overrideActionsFlag'], true),
      pathSet(['orgsAndPolicies', 'policy', 'hasEditIqPermission'], true)
    )(state);
    renderComponent(preloadedState);

    const table = screen.getByRole('table', { name: 'Edit policy actions table' });
    const radios = within(table).getAllByRole('radio');

    expect(radios.length).toBe(9);
    radios.forEach((radio) => expect(radio).toBeEnabled());
  });

  it('dispatches setActions action when a radio is clicked', () => {
    const spy = jest.spyOn(policyActions, 'setActions');
    const preloadedState = compose(
      pathSet(['productFeatures', 'productFeatures', 'firewall'], true),
      pathSet(['productFeatures', 'productFeatures', 'enforcement'], true),
      pathSet(['orgsAndPolicies', 'policy', 'isInherited'], false),
      pathSet(['orgsAndPolicies', 'policy', 'hasEditIqPermission'], true),
      pathSet(['orgsAndPolicies', 'policy', 'currentPolicy', 'actions'], {})
    )(state);

    renderComponent(preloadedState);

    const table = screen.getByRole('table', { name: 'Edit policy actions table' });
    const radios = within(table).getAllByRole('radio');

    expect(last(radios)).toBeEnabled();
    expect(last(radios)).not.toBeChecked();
    fireEvent.click(last(radios));

    expect(spy).toHaveBeenCalledTimes(1);
    expect(spy).toHaveBeenCalledWith({ operate: 'fail' });
  });

  it('does not render quarantine warning alert when quarantine warning flag is false', () => {
    renderComponent();

    const alert = screen.queryByText(
      'This will quarantine all new components that violate this policy going forward and may cause build failures.'
    );

    expect(alert).toBeNull();
  });

  it('does not render SBOM Manager\'s "Compliance" stage in Lifecycle when state supports SBOM Manager', () => {
    const stateSupportsSBOMManager = {
      ...state,
      productFeatures: {
        productFeatures: {
          notifications: true,
          firewall: false,
          enforcement: false,
          'policy-monitoring': true,
          'sbom-manager': true,
        },
      },
    };
    renderComponent(stateSupportsSBOMManager);
    const table = screen.getByRole('table', { name: 'Edit policy actions table' });
    expect(within(table).queryByRole('columnheader', { name: 'compliance' })).not.toBeInTheDocument();
  });

  describe('when quarantine warning flag is true', () => {
    it('renders quarantine warning alert', () => {
      const preloadedState = compose(
        pathSet(['orgsAndPolicies', 'policy', 'isRootOrg'], true),
        pathSet(['orgsAndPolicies', 'policy', 'originalProxyStageAction'], 'warn'),
        pathSet(['orgsAndPolicies', 'policy', 'currentPolicy', 'actions'], { proxy: 'fail' })
      )(state);

      renderComponent(preloadedState);

      const alert = screen.getByText(
        'This will quarantine all new components that violate this policy going forward and may cause build failures.'
      );

      expect(alert).toBeVisible();
    });
  });

  describe('when policy is inherited', () => {
    it('dispatches setOverrideParentActions action', () => {
      const spy = jest.spyOn(policyActions, 'setOverrideParentActions');
      const preloadedState = compose(
        pathSet(['productFeatures', 'productFeatures', 'firewall'], true),
        pathSet(['productFeatures', 'productFeatures', 'enforcement'], true),
        pathSet(['orgsAndPolicies', 'policy', 'isInherited'], true),
        pathSet(['orgsAndPolicies', 'policy', 'currentPolicy', 'policyActionsOverrideAllowed'], true)
      )(state);
      renderComponent(preloadedState);

      const overrideParentActionsRadio = screen.getByLabelText(/Override parent actions/i);
      expect(overrideParentActionsRadio).not.toBeChecked();
      const table = screen.getByRole('table', { name: 'Edit policy actions table' });
      const radios = within(table).getAllByRole('radio');

      expect(radios.length).toBe(9);
      radios.forEach((radio) => expect(radio).not.toBeEnabled());

      fireEvent.click(overrideParentActionsRadio);
      expect(overrideParentActionsRadio).toBeChecked();

      expect(radios.length).toBe(9);
      radios.forEach((radio) => expect(radio).toBeEnabled());

      expect(spy).toHaveBeenCalled();
      expect(overrideParentActionsRadio).toBeChecked();
    });

    it('dispatches unSetOverrideParentActions action', () => {
      const spy = jest.spyOn(policyActions, 'unSetOverrideParentActions');
      const preloadedState = compose(
        pathSet(['orgsAndPolicies', 'policy', 'isInherited'], true),
        pathSet(['orgsAndPolicies', 'policy', 'currentPolicy', 'policyActionsOverrideAllowed'], true),
        pathSet(['orgsAndPolicies', 'policy', 'overrideActionsFlag'], true)
      )(state);
      renderComponent(preloadedState);

      const inheritParentActionsRadio = screen.getByLabelText(/Inherit parent actions/i);
      fireEvent.click(inheritParentActionsRadio);

      const table = screen.getByRole('table', { name: 'Edit policy actions table' });
      const radios = within(table).getAllByRole('radio');

      expect(spy).toHaveBeenCalled();
      expect(inheritParentActionsRadio).toBeChecked();

      expect(radios.length).toBe(9);
      radios.forEach((radio) => expect(radio).not.toBeEnabled());
    });

    it('renders actions overrides disabled message when action overrides are not enabled', () => {
      const preloadedState = compose(
        pathSet(['orgsAndPolicies', 'policy', 'isInherited'], true),
        pathSet(['orgsAndPolicies', 'policy', 'currentPolicy', 'policyActionsOverrideAllowed'], false)
      )(state);
      renderComponent(preloadedState);

      const actionsOverridesDisabledMessage = screen.getByText(/Action overrides have been disabled for this policy./i);

      expect(actionsOverridesDisabledMessage).toBeVisible();
    });

    it('does not render actions overrides ensabled message when action overrides are enabled', () => {
      const preloadedState = compose(
        pathSet(['orgsAndPolicies', 'policy', 'isInherited'], true),
        pathSet(['orgsAndPolicies', 'policy', 'currentPolicy', 'policyActionsOverrideAllowed'], true)
      )(state);
      renderComponent(preloadedState);

      const actionsOverridesEnabledMessage = screen.getByText(
        /Action overrides have been enabled for this policy. Modifying actions will only affect this level./i
      );

      expect(actionsOverridesEnabledMessage).toBeVisible();
    });

    it('renders enabled radios when action overrides are enabled', () => {
      const preloadedState = compose(
        pathSet(['orgsAndPolicies', 'policy', 'isInherited'], true),
        pathSet(['orgsAndPolicies', 'policy', 'currentPolicy', 'policyActionsOverrideAllowed'], true),
        pathSet(['productFeatures', 'productFeatures', 'firewall'], true)
      )(state);
      renderComponent(preloadedState);

      const inheritParentActionsRadio = screen.getByLabelText(/Inherit parent actions/i);
      const overrideParentActionsRadio = screen.getByLabelText(/Override parent actions/i);

      expect(inheritParentActionsRadio).toBeVisible();
      expect(inheritParentActionsRadio).toBeEnabled();
      expect(overrideParentActionsRadio).toBeVisible();
      expect(overrideParentActionsRadio).toBeEnabled();
    });

    it('renders disabled radios if no permission', () => {
      const preloadedState = compose(
        pathSet(['orgsAndPolicies', 'policy', 'isInherited'], true),
        pathSet(['orgsAndPolicies', 'policy', 'hasEditIqPermission'], false),
        pathSet(['orgsAndPolicies', 'policy', 'currentPolicy', 'policyActionsOverrideAllowed'], true),
        pathSet(['productFeatures', 'productFeatures', 'firewall'], true)
      )(state);
      renderComponent(preloadedState);

      const inheritParentActionsRadio = screen.getByLabelText(/Inherit parent actions/i);
      const overrideParentActionsRadio = screen.getByLabelText(/Override parent actions/i);

      expect(inheritParentActionsRadio).toBeVisible();
      expect(inheritParentActionsRadio).toBeDisabled();
      expect(overrideParentActionsRadio).toBeVisible();
      expect(overrideParentActionsRadio).toBeDisabled();
    });

    it('renders disabled radios when action overrides are not enabled', () => {
      const preloadedState = compose(
        pathSet(['orgsAndPolicies', 'policy', 'isInherited'], true),
        pathSet(['orgsAndPolicies', 'policy', 'currentPolicy', 'policyActionsOverrideAllowed'], false)
      )(state);
      renderComponent(preloadedState);

      const inheritParentActionsRadio = screen.getByLabelText(/Inherit parent actions/i);
      const overrideParentActionsRadio = screen.getByLabelText(/Override parent actions/i);

      expect(inheritParentActionsRadio).toBeVisible();
      expect(inheritParentActionsRadio).toBeDisabled();
      expect(overrideParentActionsRadio).toBeVisible();
      expect(overrideParentActionsRadio).toBeDisabled();
    });

    it('renders table with disabled radios when policy actions overriding is not enabled', () => {
      const preloadedState = compose(
        pathSet(['productFeatures', 'productFeatures', 'firewall'], true),
        pathSet(['productFeatures', 'productFeatures', 'enforcement'], true),
        pathSet(['orgsAndPolicies', 'policy', 'isInherited'], true),
        pathSet(['orgsAndPolicies', 'policy', 'overrideActionsFlag'], true),
        pathSet(['orgsAndPolicies', 'policy', 'currentPolicy', 'policyActionsOverrideAllowed'], false)
      )(state);

      renderComponent(preloadedState);

      const table = screen.getByRole('table', { name: 'Edit policy actions table' });
      const radios = within(table).getAllByRole('radio');

      expect(radios.length).toBe(9);
      radios.forEach((radio) => expect(radio).toBeDisabled());
    });

    it('dispatches setActionsOverride action when actions overriding is enabled and a radio is clicked', () => {
      const spy = jest.spyOn(policyActions, 'setActionsOverride');
      const preloadedState = compose(
        pathSet(['productFeatures', 'productFeatures', 'firewall'], true),
        pathSet(['productFeatures', 'productFeatures', 'enforcement'], true),
        pathSet(['orgsAndPolicies', 'policy', 'isInherited'], true),
        pathSet(['orgsAndPolicies', 'policy', 'overrideActionsFlag'], true),
        pathSet(['orgsAndPolicies', 'policy', 'hasEditIqPermission'], true),
        pathSet(['orgsAndPolicies', 'policy', 'currentPolicy', 'policyActionsOverrideAllowed'], true)
      )(state);

      renderComponent(preloadedState);

      const table = screen.getByRole('table', { name: 'Edit policy actions table' });
      const radios = within(table).getAllByRole('radio');

      radios.forEach((radio) => expect(radio).toBeEnabled());

      expect(last(radios)).toBeEnabled();
      expect(last(radios)).not.toBeChecked();
      fireEvent.click(last(radios));

      expect(spy).toHaveBeenCalledTimes(1);
      expect(spy).toHaveBeenCalledWith({ ownerId: 'ownerId', actionsOverride: { operate: 'fail' } });
    });

    it('dispatches setActionsOverride action when actions overriding is enabled only for proxy stage for repository manager', () => {
      const spy = jest.spyOn(policyActions, 'setActionsOverride');
      const preloadedState = compose(
        pathSet(['productFeatures', 'productFeatures', 'firewall'], true),
        pathSet(['productFeatures', 'productFeatures', 'enforcement'], true),
        pathSet(['orgsAndPolicies', 'policy', 'isInherited'], true),
        pathSet(['orgsAndPolicies', 'policy', 'overrideActionsFlag'], true),
        pathSet(['orgsAndPolicies', 'policy', 'hasEditIqPermission'], true),
        pathSet(['orgsAndPolicies', 'policy', 'currentPolicy', 'policyActionsOverrideAllowed'], true),
        pathSet(['router', 'currentParams'], { repositoryManagerId: 'repositoryManagerId' }),
        pathSet(['router', 'currentState'], { name: 'management.view.repository_manager' })
      )(state);

      renderComponent(preloadedState);

      const table = screen.getByRole('table', { name: 'Edit policy actions table' });
      const radios = within(table).getAllByRole('radio');

      const proxyRadios = radios.filter((radio) => radio.name === 'action-for-proxy');
      const otherStatesRadios = radios.filter((radio) => radio.name !== 'action-for-proxy');

      proxyRadios.forEach((radio) => expect(radio).toBeEnabled());
      otherStatesRadios.forEach((radio) => expect(radio).toBeDisabled());

      expect(last(proxyRadios)).toBeEnabled();
      expect(last(proxyRadios)).not.toBeChecked();
      fireEvent.click(last(proxyRadios));

      expect(spy).toHaveBeenCalledTimes(1);
      expect(spy).toHaveBeenCalledWith({ ownerId: 'ownerId', actionsOverride: { proxy: 'fail' } });
    });

    describe('when enforcement is not supported', () => {
      it('renders actions not supported message when firewall is not supported', () => {
        const preloadedState = compose(
          pathSet(['orgsAndPolicies', 'policy', 'isInherited'], true),
          pathSet(['orgsAndPolicies', 'policy', 'currentPolicy', 'policyActionsOverrideAllowed'], false)
        )(state);
        renderComponent(preloadedState);

        const alert = screen.getByText('Actions are not supported by your product license.');

        expect(alert).toBeVisible();
      });

      it('renders only proxy actions are supported message when firewall is supported', () => {
        const preloadedState = compose(
          pathSet(['orgsAndPolicies', 'policy', 'isInherited'], true),
          pathSet(['orgsAndPolicies', 'policy', 'currentPolicy', 'policyActionsOverrideAllowed'], false),
          pathSet(['productFeatures', 'productFeatures', 'firewall'], true)
        )(state);
        renderComponent(preloadedState);

        const alert = screen.getByText('Only Proxy Actions are supported with your Firewall product license.');

        expect(alert).toBeVisible();
      });
    });
  });
});
