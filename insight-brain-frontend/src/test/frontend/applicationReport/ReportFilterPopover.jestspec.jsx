/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import userEvent from '@testing-library/user-event';
import ReportFilterPopover from 'MainRoot/applicationReport/ReportFilterPopover';
import * as applicationReportSelectors from 'MainRoot/applicationReport/applicationReportSelectors';

import { render, screen, fireEvent, within, getAllByRole } from 'TestRoot/SpecUtil';

describe('ReportFilterPopover', () => {
  let renderComponent, selectShowFilterPopoverSpy;
  beforeEach(() => {
    selectShowFilterPopoverSpy = jest
      .spyOn(applicationReportSelectors, 'selectShowFilterPopover')
      .mockReturnValue(true);
    jest.spyOn(applicationReportSelectors, 'selectIsPolicyTypeFilterEnabled').mockReturnValue(true);
    renderComponent = (props) => render(<ReportFilterPopover {...props} />);
  });

  it('does not render the tree when selectShowFilterPopover is false', () => {
    selectShowFilterPopoverSpy.mockReturnValue(false);
    renderComponent();
    expect(screen.queryByText('Filter')).toBeNull();
  });

  it('renders the tree with the correct title', () => {
    renderComponent();
    expect(screen.getByText('Filter')).toBeVisible();
  });

  it('enables policy types filter by default', () => {
    renderComponent();
    expect(screen.getByRole('button', { name: /policy types/i, exact: false })).toBeEnabled();
  });

  it('enables policy types filter when isPolicyTypeFilterEnabled flag is true', () => {
    applicationReportSelectors.selectIsPolicyTypeFilterEnabled.mockReturnValue(true);
    renderComponent();
    expect(screen.getByRole('button', { name: /policy types/i, exact: false })).toBeEnabled();
  });

  it('disables policy types filter when isPolicyTypeFilterEnabled flag is false', () => {
    applicationReportSelectors.selectIsPolicyTypeFilterEnabled.mockReturnValue(false);
    renderComponent();
    expect(screen.getByRole('button', { name: /policy types/i, exact: false })).toBeDisabled();
  });

  it('renders tooltip when policy types filter is disabled', async () => {
    applicationReportSelectors.selectIsPolicyTypeFilterEnabled.mockReturnValue(false);
    SpecUtil.requestIdleCallbackInvokeImmediateJest();
    renderComponent();
    fireEvent.mouseOver(screen.getByRole('button', { name: /policy types/i, exact: false }));
    const tooltip = await screen.findByRole('tooltip');

    expect(
      within(tooltip).getByText('Reevaluate the report in order to enable Policy Types filter')
    ).toBeInTheDocument();
  });

  describe('handles clicks on the checkboxes', () => {
    it('Handles the click in the proprietary items', async () => {
      const user = userEvent.setup();

      renderComponent();
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

      renderComponent();
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

      renderComponent();
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

      renderComponent();
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

      renderComponent();
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

      renderComponent();
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

      renderComponent();
      await user.click(screen.getByRole('button', { name: /Policy Threat Level/ }));

      const policyThreat = screen.getAllByRole('list')[0];
      const policyThreatSliders = getAllByRole(policyThreat, 'slider');
      expect(policyThreatSliders[0]).toHaveTextContent('0');
      expect(policyThreatSliders[1]).toHaveTextContent('10');
    });
  });
});
