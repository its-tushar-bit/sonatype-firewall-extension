/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import ReportFilterPopover from 'MainRoot/applicationReport/ReportFilterPopover';
import * as applicationReportSelectors from 'MainRoot/applicationReport/applicationReportSelectors';

import { render, screen, fireEvent, within, getAllByRole } from 'TestRoot/SpecUtil';

describe('ReportFilterPopover', () => {
  let renderComponent, selectShowFilterPopoverSpy;
  beforeEach(() => {
    selectShowFilterPopoverSpy = spyOn(applicationReportSelectors, 'selectShowFilterPopover').and.returnValue(true);
    spyOn(applicationReportSelectors, 'selectIsPolicyTypeFilterEnabled').and.returnValue(true);
    renderComponent = (props) => render(<ReportFilterPopover {...props} />);
  });

  it('does not render the tree when selectShowFilterPopover is false', () => {
    selectShowFilterPopoverSpy.and.returnValue(false);
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
    applicationReportSelectors.selectIsPolicyTypeFilterEnabled.and.returnValue(true);
    renderComponent();
    expect(screen.getByRole('button', { name: /policy types/i, exact: false })).toBeEnabled();
  });

  it('disables policy types filter when isPolicyTypeFilterEnabled flag is false', () => {
    applicationReportSelectors.selectIsPolicyTypeFilterEnabled.and.returnValue(false);
    renderComponent();
    expect(screen.getByRole('button', { name: /policy types/i, exact: false })).toBeDisabled();
  });

  it('renders tooltip when policy types filter is disabled', async () => {
    applicationReportSelectors.selectIsPolicyTypeFilterEnabled.and.returnValue(false);
    renderComponent();
    fireEvent.mouseOver(screen.getByRole('button', { name: /policy types/i, exact: false }));
    const tooltip = await screen.findByRole('tooltip');

    expect(
      within(tooltip).getByText('Reevaluate the report in order to enable Policy Types filter')
    ).toBeInTheDocument();
  });

  describe('handles clicks on teh checkboxes', () => {
    const checkedClass = 'nx-collapsible-items__child tm-checked';
    const uncheckedClass = 'nx-collapsible-items__child tm-unchecked';

    it('Handles the click in the proprietary items', () => {
      renderComponent();
      const proprietaryList = screen.getAllByRole('list')[0];
      const proprietaryOptions = getAllByRole(proprietaryList, 'listitem');

      expect(proprietaryOptions[0]).toHaveClassName(uncheckedClass);
      expect(proprietaryOptions[1]).toHaveClassName(uncheckedClass);
      expect(proprietaryOptions[2]).toHaveClassName(uncheckedClass);

      fireEvent.click(proprietaryOptions[1]);
      expect(proprietaryOptions[0]).toHaveClassName(uncheckedClass);
      expect(proprietaryOptions[1]).toHaveClassName(checkedClass);
      expect(proprietaryOptions[2]).toHaveClassName(uncheckedClass);

      fireEvent.click(proprietaryOptions[2]);
      expect(proprietaryOptions[0]).toHaveClassName(checkedClass);
      expect(proprietaryOptions[1]).toHaveClassName(checkedClass);
      expect(proprietaryOptions[2]).toHaveClassName(checkedClass);

      fireEvent.click(proprietaryOptions[0]);
      expect(proprietaryOptions[0]).toHaveClassName(uncheckedClass);
      expect(proprietaryOptions[1]).toHaveClassName(uncheckedClass);
      expect(proprietaryOptions[2]).toHaveClassName(uncheckedClass);
    });

    it('Handles the click in the InnerSource items', () => {
      renderComponent();
      const innerSourceList = screen.getAllByRole('list')[1];
      const innerSourceOptions = getAllByRole(innerSourceList, 'listitem');

      expect(innerSourceOptions[0]).toHaveClassName(uncheckedClass);
      expect(innerSourceOptions[1]).toHaveClassName(uncheckedClass);
      expect(innerSourceOptions[2]).toHaveClassName(uncheckedClass);

      fireEvent.click(innerSourceOptions[1]);
      expect(innerSourceOptions[0]).toHaveClassName(uncheckedClass);
      expect(innerSourceOptions[1]).toHaveClassName(checkedClass);
      expect(innerSourceOptions[2]).toHaveClassName(uncheckedClass);

      fireEvent.click(innerSourceOptions[2]);
      expect(innerSourceOptions[0]).toHaveClassName(checkedClass);
      expect(innerSourceOptions[1]).toHaveClassName(checkedClass);
      expect(innerSourceOptions[2]).toHaveClassName(checkedClass);

      fireEvent.click(innerSourceOptions[0]);
      expect(innerSourceOptions[0]).toHaveClassName(uncheckedClass);
      expect(innerSourceOptions[1]).toHaveClassName(uncheckedClass);
      expect(innerSourceOptions[2]).toHaveClassName(uncheckedClass);
    });

    it('Handles the click in the component match state items', () => {
      renderComponent();
      const matchStateList = screen.getAllByRole('list')[2];
      const matchStateOptions = getAllByRole(matchStateList, 'listitem');

      expect(matchStateOptions[0]).toHaveClassName(uncheckedClass);
      expect(matchStateOptions[1]).toHaveClassName(uncheckedClass);
      expect(matchStateOptions[2]).toHaveClassName(uncheckedClass);
      expect(matchStateOptions[3]).toHaveClassName(uncheckedClass);

      fireEvent.click(matchStateOptions[1]);
      expect(matchStateOptions[0]).toHaveClassName(uncheckedClass);
      expect(matchStateOptions[1]).toHaveClassName(checkedClass);
      expect(matchStateOptions[2]).toHaveClassName(uncheckedClass);
      expect(matchStateOptions[3]).toHaveClassName(uncheckedClass);

      fireEvent.click(matchStateOptions[2]);
      expect(matchStateOptions[0]).toHaveClassName(uncheckedClass);
      expect(matchStateOptions[1]).toHaveClassName(checkedClass);
      expect(matchStateOptions[2]).toHaveClassName(checkedClass);
      expect(matchStateOptions[3]).toHaveClassName(uncheckedClass);

      fireEvent.click(matchStateOptions[3]);
      expect(matchStateOptions[0]).toHaveClassName(checkedClass);
      expect(matchStateOptions[1]).toHaveClassName(checkedClass);
      expect(matchStateOptions[2]).toHaveClassName(checkedClass);
      expect(matchStateOptions[3]).toHaveClassName(checkedClass);

      fireEvent.click(matchStateOptions[0]);
      expect(matchStateOptions[0]).toHaveClassName(uncheckedClass);
      expect(matchStateOptions[1]).toHaveClassName(uncheckedClass);
      expect(matchStateOptions[2]).toHaveClassName(uncheckedClass);
      expect(matchStateOptions[3]).toHaveClassName(uncheckedClass);
    });

    it('Handles the click in the violation state items', () => {
      renderComponent();
      const violationList = screen.getAllByRole('list')[3];
      const violationOptions = getAllByRole(violationList, 'listitem');

      expect(violationOptions[0]).toHaveClassName(uncheckedClass);
      expect(violationOptions[1]).toHaveClassName(uncheckedClass);
      expect(violationOptions[2]).toHaveClassName(uncheckedClass);
      expect(violationOptions[3]).toHaveClassName(uncheckedClass);
      expect(violationOptions[4]).toHaveClassName(uncheckedClass);

      fireEvent.click(violationOptions[1]);
      expect(violationOptions[0]).toHaveClassName(uncheckedClass);
      expect(violationOptions[1]).toHaveClassName(checkedClass);
      expect(violationOptions[2]).toHaveClassName(uncheckedClass);
      expect(violationOptions[3]).toHaveClassName(uncheckedClass);
      expect(violationOptions[4]).toHaveClassName(uncheckedClass);

      fireEvent.click(violationOptions[2]);
      expect(violationOptions[0]).toHaveClassName(uncheckedClass);
      expect(violationOptions[1]).toHaveClassName(checkedClass);
      expect(violationOptions[2]).toHaveClassName(checkedClass);
      expect(violationOptions[3]).toHaveClassName(uncheckedClass);
      expect(violationOptions[4]).toHaveClassName(uncheckedClass);

      fireEvent.click(violationOptions[3]);
      expect(violationOptions[0]).toHaveClassName(uncheckedClass);
      expect(violationOptions[1]).toHaveClassName(checkedClass);
      expect(violationOptions[2]).toHaveClassName(checkedClass);
      expect(violationOptions[3]).toHaveClassName(checkedClass);
      expect(violationOptions[4]).toHaveClassName(uncheckedClass);

      fireEvent.click(violationOptions[4]);
      expect(violationOptions[0]).toHaveClassName(checkedClass);
      expect(violationOptions[1]).toHaveClassName(checkedClass);
      expect(violationOptions[2]).toHaveClassName(checkedClass);
      expect(violationOptions[3]).toHaveClassName(checkedClass);
      expect(violationOptions[4]).toHaveClassName(checkedClass);

      fireEvent.click(violationOptions[0]);
      expect(violationOptions[0]).toHaveClassName(uncheckedClass);
      expect(violationOptions[1]).toHaveClassName(uncheckedClass);
      expect(violationOptions[2]).toHaveClassName(uncheckedClass);
      expect(violationOptions[3]).toHaveClassName(uncheckedClass);
      expect(violationOptions[4]).toHaveClassName(uncheckedClass);
    });

    it('Handles the click in the dependency type items', () => {
      renderComponent();
      const dependencyTypeList = screen.getAllByRole('list')[4];
      const dependencyTypeOptions = getAllByRole(dependencyTypeList, 'listitem');

      expect(dependencyTypeOptions[0]).toHaveClassName(uncheckedClass);
      expect(dependencyTypeOptions[1]).toHaveClassName(uncheckedClass);
      expect(dependencyTypeOptions[2]).toHaveClassName(uncheckedClass);
      expect(dependencyTypeOptions[3]).toHaveClassName(uncheckedClass);

      fireEvent.click(dependencyTypeOptions[1]);
      expect(dependencyTypeOptions[0]).toHaveClassName(uncheckedClass);
      expect(dependencyTypeOptions[1]).toHaveClassName(checkedClass);
      expect(dependencyTypeOptions[2]).toHaveClassName(uncheckedClass);
      expect(dependencyTypeOptions[3]).toHaveClassName(uncheckedClass);

      fireEvent.click(dependencyTypeOptions[2]);
      expect(dependencyTypeOptions[0]).toHaveClassName(uncheckedClass);
      expect(dependencyTypeOptions[1]).toHaveClassName(checkedClass);
      expect(dependencyTypeOptions[2]).toHaveClassName(checkedClass);
      expect(dependencyTypeOptions[3]).toHaveClassName(uncheckedClass);

      fireEvent.click(dependencyTypeOptions[3]);
      expect(dependencyTypeOptions[0]).toHaveClassName(checkedClass);
      expect(dependencyTypeOptions[1]).toHaveClassName(checkedClass);
      expect(dependencyTypeOptions[2]).toHaveClassName(checkedClass);
      expect(dependencyTypeOptions[3]).toHaveClassName(checkedClass);

      fireEvent.click(dependencyTypeOptions[0]);
      expect(dependencyTypeOptions[0]).toHaveClassName(uncheckedClass);
      expect(dependencyTypeOptions[1]).toHaveClassName(uncheckedClass);
      expect(dependencyTypeOptions[2]).toHaveClassName(uncheckedClass);
      expect(dependencyTypeOptions[3]).toHaveClassName(uncheckedClass);
    });

    it('Handles the click in the policy types items', () => {
      renderComponent();
      const policyList = screen.getAllByRole('list')[5];
      const policyOptions = getAllByRole(policyList, 'listitem');

      expect(policyOptions[0]).toHaveClassName(uncheckedClass);
      expect(policyOptions[1]).toHaveClassName(uncheckedClass);
      expect(policyOptions[2]).toHaveClassName(uncheckedClass);
      expect(policyOptions[3]).toHaveClassName(uncheckedClass);
      expect(policyOptions[4]).toHaveClassName(uncheckedClass);

      fireEvent.click(policyOptions[1]);
      expect(policyOptions[0]).toHaveClassName(uncheckedClass);
      expect(policyOptions[1]).toHaveClassName(checkedClass);
      expect(policyOptions[2]).toHaveClassName(uncheckedClass);
      expect(policyOptions[3]).toHaveClassName(uncheckedClass);
      expect(policyOptions[4]).toHaveClassName(uncheckedClass);

      fireEvent.click(policyOptions[2]);
      expect(policyOptions[0]).toHaveClassName(uncheckedClass);
      expect(policyOptions[1]).toHaveClassName(checkedClass);
      expect(policyOptions[2]).toHaveClassName(checkedClass);
      expect(policyOptions[3]).toHaveClassName(uncheckedClass);
      expect(policyOptions[4]).toHaveClassName(uncheckedClass);

      fireEvent.click(policyOptions[3]);
      expect(policyOptions[0]).toHaveClassName(uncheckedClass);
      expect(policyOptions[1]).toHaveClassName(checkedClass);
      expect(policyOptions[2]).toHaveClassName(checkedClass);
      expect(policyOptions[3]).toHaveClassName(checkedClass);
      expect(policyOptions[4]).toHaveClassName(uncheckedClass);

      fireEvent.click(policyOptions[4]);
      expect(policyOptions[0]).toHaveClassName(checkedClass);
      expect(policyOptions[1]).toHaveClassName(checkedClass);
      expect(policyOptions[2]).toHaveClassName(checkedClass);
      expect(policyOptions[3]).toHaveClassName(checkedClass);
      expect(policyOptions[4]).toHaveClassName(checkedClass);

      fireEvent.click(policyOptions[0]);
      expect(policyOptions[0]).toHaveClassName(uncheckedClass);
      expect(policyOptions[1]).toHaveClassName(uncheckedClass);
      expect(policyOptions[2]).toHaveClassName(uncheckedClass);
      expect(policyOptions[3]).toHaveClassName(uncheckedClass);
      expect(policyOptions[4]).toHaveClassName(uncheckedClass);
    });

    it('Handles the click in the policy threat level', () => {
      renderComponent();
      const policyThreat = screen.getAllByRole('list')[6];
      const policyThreatSliders = getAllByRole(policyThreat, 'slider');
      expect(policyThreatSliders[0]).toHaveTextContent('0');
      expect(policyThreatSliders[1]).toHaveTextContent('10');
    });
  });
});
