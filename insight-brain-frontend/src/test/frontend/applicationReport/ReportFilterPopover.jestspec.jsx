/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import userEvent from '@testing-library/user-event';
import ReportFilterPopover from 'MainRoot/applicationReport/ReportFilterPopover';
import * as applicationReportSelectors from 'MainRoot/applicationReport/applicationReportSelectors';

import { render, screen, fireEvent, within, getAllByRole, setupPortalContainer } from 'TestRoot/SpecUtil';
import * as routerSelectors from 'MainRoot/reduxUiRouter/routerSelectors';

describe('ReportFilterPopover', () => {
  let renderComponent, selectShowFilterPopoverSpy, renderPopoverAndWaitForAnimation, selectIsBulkWaivePageSpy;

  beforeAll(() => setupPortalContainer());

  beforeEach(() => {
    selectShowFilterPopoverSpy = jest
      .spyOn(applicationReportSelectors, 'selectShowFilterPopover')
      .mockReturnValue(true);
    jest.spyOn(applicationReportSelectors, 'selectIsPolicyTypeFilterEnabled').mockReturnValue(true);
    selectIsBulkWaivePageSpy = jest.spyOn(routerSelectors, 'selectIsBulkWaivePage').mockReturnValue(false);
    renderComponent = (props) => render(<ReportFilterPopover {...props} />);
    renderPopoverAndWaitForAnimation = () => {
      renderComponent();
      const drawer = screen.getByRole('dialog', { hidden: true });
      fireEvent.animationEnd(drawer);
    };
  });

  it('does not render the tree when selectShowFilterPopover is false', () => {
    selectShowFilterPopoverSpy.mockReturnValue(false);
    renderComponent();
    expect(screen.queryByText('Filter')).toBeNull();
  });

  it('renders the filter popover', () => {
    renderPopoverAndWaitForAnimation();
    expect(screen.getByText('Filter')).toBeVisible();
  });

  it('enables policy types filter by default', () => {
    renderPopoverAndWaitForAnimation();
    expect(screen.getByRole('button', { name: /policy types/i, exact: false })).toBeEnabled();
  });

  it('enables policy types filter when isPolicyTypeFilterEnabled flag is true', () => {
    applicationReportSelectors.selectIsPolicyTypeFilterEnabled.mockReturnValue(true);
    renderPopoverAndWaitForAnimation();
    expect(screen.getByRole('button', { name: /policy types/i, exact: false })).toBeEnabled();
  });

  it('disables policy types filter when isPolicyTypeFilterEnabled flag is false', () => {
    applicationReportSelectors.selectIsPolicyTypeFilterEnabled.mockReturnValue(false);
    renderPopoverAndWaitForAnimation();
    expect(screen.getByRole('button', { name: /policy types/i, exact: false })).toBeDisabled();
  });

  it('renders tooltip when policy types filter is disabled', async () => {
    applicationReportSelectors.selectIsPolicyTypeFilterEnabled.mockReturnValue(false);
    SpecUtil.requestIdleCallbackInvokeImmediateJest();
    renderPopoverAndWaitForAnimation();
    fireEvent.mouseOver(screen.getByRole('button', { name: /policy types/i, exact: false }));
    const tooltip = await screen.findByRole('tooltip');

    expect(
      within(tooltip).getByText('Reevaluate the report in order to enable Policy Types filter')
    ).toBeInTheDocument();
  });

  describe('handles clicks on the checkboxes', () => {
    it('Handles the click in the proprietary items', async () => {
      const user = userEvent.setup();

      renderPopoverAndWaitForAnimation();
      await user.click(screen.getByRole('button', { name: /Proprietary/ }));

      const proprietaryList = screen.getAllByRole('menu')[0];
      const proprietaryOptions = getAllByRole(proprietaryList, 'menuitemcheckbox');

      expect(proprietaryOptions[0]).not.toBeChecked();
      expect(proprietaryOptions[1]).not.toBeChecked();
      expect(proprietaryOptions[2]).not.toBeChecked();

      fireEvent.click(proprietaryOptions[1]);
      expect(proprietaryOptions[0]).not.toBeChecked();
      expect(proprietaryOptions[1]).toBeChecked();
      expect(proprietaryOptions[2]).not.toBeChecked();

      fireEvent.click(proprietaryOptions[2]);
      expect(proprietaryOptions[0]).toBeChecked();
      expect(proprietaryOptions[1]).toBeChecked();
      expect(proprietaryOptions[2]).toBeChecked();

      fireEvent.click(proprietaryOptions[0]);
      expect(proprietaryOptions[0]).not.toBeChecked();
      expect(proprietaryOptions[1]).not.toBeChecked();
      expect(proprietaryOptions[2]).not.toBeChecked();
    });

    it('Handles the click in the InnerSource items', async () => {
      const user = userEvent.setup();

      renderPopoverAndWaitForAnimation();
      await user.click(screen.getByRole('button', { name: /InnerSource/ }));

      const innerSourceList = screen.getAllByRole('menu')[1];
      const innerSourceOptions = getAllByRole(innerSourceList, 'menuitemcheckbox');

      expect(innerSourceOptions[0]).not.toBeChecked();
      expect(innerSourceOptions[1]).not.toBeChecked();
      expect(innerSourceOptions[2]).not.toBeChecked();

      fireEvent.click(innerSourceOptions[1]);
      expect(innerSourceOptions[0]).not.toBeChecked();
      expect(innerSourceOptions[1]).toBeChecked();
      expect(innerSourceOptions[2]).not.toBeChecked();

      fireEvent.click(innerSourceOptions[2]);
      expect(innerSourceOptions[0]).toBeChecked();
      expect(innerSourceOptions[1]).toBeChecked();
      expect(innerSourceOptions[2]).toBeChecked();

      fireEvent.click(innerSourceOptions[0]);
      expect(innerSourceOptions[0]).not.toBeChecked();
      expect(innerSourceOptions[1]).not.toBeChecked();
      expect(innerSourceOptions[2]).not.toBeChecked();
    });

    it('Handles the click in the component match state items', async () => {
      const user = userEvent.setup();

      renderPopoverAndWaitForAnimation();
      await user.click(screen.getByRole('button', { name: /Match State/ }));

      const matchStateList = screen.getAllByRole('menu')[2];
      const matchStateOptions = getAllByRole(matchStateList, 'menuitemcheckbox');

      expect(matchStateOptions[0]).not.toBeChecked();
      expect(matchStateOptions[1]).not.toBeChecked();
      expect(matchStateOptions[2]).not.toBeChecked();
      expect(matchStateOptions[3]).not.toBeChecked();

      fireEvent.click(matchStateOptions[1]);
      expect(matchStateOptions[0]).not.toBeChecked();
      expect(matchStateOptions[1]).toBeChecked();
      expect(matchStateOptions[2]).not.toBeChecked();
      expect(matchStateOptions[3]).not.toBeChecked();

      fireEvent.click(matchStateOptions[2]);
      expect(matchStateOptions[0]).not.toBeChecked();
      expect(matchStateOptions[1]).toBeChecked();
      expect(matchStateOptions[2]).toBeChecked();
      expect(matchStateOptions[3]).not.toBeChecked();

      fireEvent.click(matchStateOptions[3]);
      expect(matchStateOptions[0]).toBeChecked();
      expect(matchStateOptions[1]).toBeChecked();
      expect(matchStateOptions[2]).toBeChecked();
      expect(matchStateOptions[3]).toBeChecked();

      fireEvent.click(matchStateOptions[0]);
      expect(matchStateOptions[0]).not.toBeChecked();
      expect(matchStateOptions[1]).not.toBeChecked();
      expect(matchStateOptions[2]).not.toBeChecked();
      expect(matchStateOptions[3]).not.toBeChecked();
    });

    it('Handles the click in the violation state items', async () => {
      const user = userEvent.setup();

      renderPopoverAndWaitForAnimation();
      await user.click(screen.getByRole('button', { name: /Violation State/ }));

      const violationList = screen.getAllByRole('menu')[3];
      const violationOptions = getAllByRole(violationList, 'menuitemcheckbox');

      expect(violationOptions[0]).not.toBeChecked();
      expect(violationOptions[1]).not.toBeChecked();
      expect(violationOptions[2]).not.toBeChecked();
      expect(violationOptions[3]).not.toBeChecked();
      expect(violationOptions[4]).not.toBeChecked();

      fireEvent.click(violationOptions[1]);
      expect(violationOptions[0]).not.toBeChecked();
      expect(violationOptions[1]).toBeChecked();
      expect(violationOptions[2]).not.toBeChecked();
      expect(violationOptions[3]).not.toBeChecked();
      expect(violationOptions[4]).not.toBeChecked();

      fireEvent.click(violationOptions[2]);
      expect(violationOptions[0]).not.toBeChecked();
      expect(violationOptions[1]).toBeChecked();
      expect(violationOptions[2]).toBeChecked();
      expect(violationOptions[3]).not.toBeChecked();
      expect(violationOptions[4]).not.toBeChecked();

      fireEvent.click(violationOptions[3]);
      expect(violationOptions[0]).not.toBeChecked();
      expect(violationOptions[1]).toBeChecked();
      expect(violationOptions[2]).toBeChecked();
      expect(violationOptions[3]).toBeChecked();
      expect(violationOptions[4]).not.toBeChecked();

      fireEvent.click(violationOptions[4]);
      expect(violationOptions[0]).toBeChecked();
      expect(violationOptions[1]).toBeChecked();
      expect(violationOptions[2]).toBeChecked();
      expect(violationOptions[3]).toBeChecked();
      expect(violationOptions[4]).toBeChecked();

      fireEvent.click(violationOptions[0]);
      expect(violationOptions[0]).not.toBeChecked();
      expect(violationOptions[1]).not.toBeChecked();
      expect(violationOptions[2]).not.toBeChecked();
      expect(violationOptions[3]).not.toBeChecked();
      expect(violationOptions[4]).not.toBeChecked();
    });

    it('Handles the click in the dependency type items', async () => {
      const user = userEvent.setup();

      renderPopoverAndWaitForAnimation();
      await user.click(screen.getByRole('button', { name: /Dependency Type/ }));

      const dependencyTypeList = screen.getAllByRole('menu')[4];
      const dependencyTypeOptions = getAllByRole(dependencyTypeList, 'menuitemcheckbox');

      expect(dependencyTypeOptions[0]).not.toBeChecked();
      expect(dependencyTypeOptions[1]).not.toBeChecked();
      expect(dependencyTypeOptions[2]).not.toBeChecked();
      expect(dependencyTypeOptions[3]).not.toBeChecked();

      fireEvent.click(dependencyTypeOptions[1]);
      expect(dependencyTypeOptions[0]).not.toBeChecked();
      expect(dependencyTypeOptions[1]).toBeChecked();
      expect(dependencyTypeOptions[2]).not.toBeChecked();
      expect(dependencyTypeOptions[3]).not.toBeChecked();

      fireEvent.click(dependencyTypeOptions[2]);
      expect(dependencyTypeOptions[0]).not.toBeChecked();
      expect(dependencyTypeOptions[1]).toBeChecked();
      expect(dependencyTypeOptions[2]).toBeChecked();
      expect(dependencyTypeOptions[3]).not.toBeChecked();

      fireEvent.click(dependencyTypeOptions[3]);
      expect(dependencyTypeOptions[0]).toBeChecked();
      expect(dependencyTypeOptions[1]).toBeChecked();
      expect(dependencyTypeOptions[2]).toBeChecked();
      expect(dependencyTypeOptions[3]).toBeChecked();

      fireEvent.click(dependencyTypeOptions[0]);
      expect(dependencyTypeOptions[0]).not.toBeChecked();
      expect(dependencyTypeOptions[1]).not.toBeChecked();
      expect(dependencyTypeOptions[2]).not.toBeChecked();
      expect(dependencyTypeOptions[3]).not.toBeChecked();
    });

    it('Handles the click in the policy types items', async () => {
      const user = userEvent.setup();

      renderPopoverAndWaitForAnimation();
      await user.click(screen.getByRole('button', { name: /Policy Type/ }));

      const policyList = screen.getAllByRole('menu')[5];
      const policyOptions = getAllByRole(policyList, 'menuitemcheckbox');

      expect(policyOptions[0]).not.toBeChecked();
      expect(policyOptions[1]).not.toBeChecked();
      expect(policyOptions[2]).not.toBeChecked();
      expect(policyOptions[3]).not.toBeChecked();
      expect(policyOptions[4]).not.toBeChecked();

      fireEvent.click(policyOptions[1]);
      expect(policyOptions[0]).not.toBeChecked();
      expect(policyOptions[1]).toBeChecked();
      expect(policyOptions[2]).not.toBeChecked();
      expect(policyOptions[3]).not.toBeChecked();
      expect(policyOptions[4]).not.toBeChecked();

      fireEvent.click(policyOptions[2]);
      expect(policyOptions[0]).not.toBeChecked();
      expect(policyOptions[1]).toBeChecked();
      expect(policyOptions[2]).toBeChecked();
      expect(policyOptions[3]).not.toBeChecked();
      expect(policyOptions[4]).not.toBeChecked();

      fireEvent.click(policyOptions[3]);
      expect(policyOptions[0]).not.toBeChecked();
      expect(policyOptions[1]).toBeChecked();
      expect(policyOptions[2]).toBeChecked();
      expect(policyOptions[3]).toBeChecked();
      expect(policyOptions[4]).not.toBeChecked();

      fireEvent.click(policyOptions[4]);
      expect(policyOptions[0]).toBeChecked();
      expect(policyOptions[1]).toBeChecked();
      expect(policyOptions[2]).toBeChecked();
      expect(policyOptions[3]).toBeChecked();
      expect(policyOptions[4]).toBeChecked();

      fireEvent.click(policyOptions[0]);
      expect(policyOptions[0]).not.toBeChecked();
      expect(policyOptions[1]).not.toBeChecked();
      expect(policyOptions[2]).not.toBeChecked();
      expect(policyOptions[3]).not.toBeChecked();
      expect(policyOptions[4]).not.toBeChecked();
    });

    it('Handles the click in the policy threat level', async () => {
      const user = userEvent.setup();

      renderPopoverAndWaitForAnimation();
      await user.click(screen.getByRole('button', { name: /Policy Threat Level/ }));

      const policyThreat = screen.getAllByRole('list')[0];
      const policyThreatSliders = getAllByRole(policyThreat, 'slider');
      expect(policyThreatSliders).toHaveLength(2);
      expect(policyThreatSliders[0]).toHaveAttribute('aria-valuemin', '0');
      expect(policyThreatSliders[1]).toHaveAttribute('aria-valuemax', '10');
      expect(policyThreat).toHaveTextContent('0');
      expect(policyThreat).toHaveTextContent('10');
    });
  });

  describe('bulk waive page context', () => {
    it('does not display violation state filter when on bulk waive page', () => {
      selectIsBulkWaivePageSpy.mockReturnValue(true);
      renderPopoverAndWaitForAnimation();

      // Verify that violation state filter is not present
      expect(screen.queryByRole('button', { name: /Violation State/ })).not.toBeInTheDocument();

      // Verify other filters are still present
      expect(screen.getByRole('button', { name: /Proprietary/ })).toBeInTheDocument();
      expect(screen.getByRole('button', { name: /InnerSource/ })).toBeInTheDocument();
      expect(screen.getByRole('button', { name: /Match State/ })).toBeInTheDocument();
      expect(screen.getByRole('button', { name: /Dependency Type/ })).toBeInTheDocument();
      expect(screen.getByRole('button', { name: /Policy Type/ })).toBeInTheDocument();
      expect(screen.getByRole('button', { name: /Policy Threat Level/ })).toBeInTheDocument();
    });

    it('displays violation state filter when not on bulk waive page', () => {
      selectIsBulkWaivePageSpy.mockReturnValue(false);
      renderPopoverAndWaitForAnimation();

      // Verify that violation state filter is present
      expect(screen.getByRole('button', { name: /Violation State/ })).toBeInTheDocument();

      // Verify other filters are also present
      expect(screen.getByRole('button', { name: /Proprietary/ })).toBeInTheDocument();
      expect(screen.getByRole('button', { name: /InnerSource/ })).toBeInTheDocument();
      expect(screen.getByRole('button', { name: /Match State/ })).toBeInTheDocument();
      expect(screen.getByRole('button', { name: /Dependency Type/ })).toBeInTheDocument();
      expect(screen.getByRole('button', { name: /Policy Type/ })).toBeInTheDocument();
      expect(screen.getByRole('button', { name: /Policy Threat Level/ })).toBeInTheDocument();
    });

    it('cannot access violation state filter options when on bulk waive page', () => {
      selectIsBulkWaivePageSpy.mockReturnValue(true);
      renderPopoverAndWaitForAnimation();

      // Verify that violation state filter button is not present, so we can't click it
      const violationStateButton = screen.queryByRole('button', { name: /Violation State/ });
      expect(violationStateButton).not.toBeInTheDocument();

      // Should not find any violation state options like "Open", "Waived", "Not Violating", "Legacy"
      // These would normally be accessible if the violation state filter was present
      const violationStateOptionTexts = ['Open', 'Waived', 'Not Violating', 'Legacy'];
      violationStateOptionTexts.forEach((optionText) => {
        expect(screen.queryByText(optionText)).not.toBeInTheDocument();
      });
    });
  });
});
