/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { render, screen, fireEvent } from 'TestRoot/SpecUtil';
import LabelsTile from 'MainRoot/OrgsAndPolicies/ownerSummary/labelsTile/LabelsTile';
import * as orgsAndPoliciesSelectors from 'MainRoot/OrgsAndPolicies/orgsAndPoliciesSelectors';
import * as labelsSelectors from 'MainRoot/OrgsAndPolicies/labelsSelectors';
import * as routerSelectors from 'MainRoot/reduxUiRouter/routerSelectors';
import * as ownerSummarySelectors from 'MainRoot/OrgsAndPolicies/ownerSummarySelectors';
import { actions } from 'MainRoot/OrgsAndPolicies/labelsSlice';
import * as routerContext from 'MainRoot/react/RouterStateContext';

describe('LabelsTile', () => {
  let renderComponent,
    goToCreateLabelSpy,
    selectLabelsLoadingSpy,
    selectLabelsLoadErrorSpy,
    selectApplicableLabelsSpy,
    selectInheritedLabelsOpenSpy,
    selectHasEditIqPermissionSpy;

  beforeEach(() => {
    selectLabelsLoadingSpy = spyOn(labelsSelectors, 'selectLabelsLoading').and.returnValue(false);
    selectLabelsLoadErrorSpy = spyOn(labelsSelectors, 'selectLabelsLoadError').and.returnValue(null);
    selectApplicableLabelsSpy = spyOn(labelsSelectors, 'selectApplicableLabels').and.returnValue([]);
    selectInheritedLabelsOpenSpy = spyOn(labelsSelectors, 'selectInheritedLabelsOpen').and.returnValue({});
    selectHasEditIqPermissionSpy = spyOn(ownerSummarySelectors, 'selectHasEditIqPermission').and.returnValue(false);
    spyOn(orgsAndPoliciesSelectors, 'selectSelectedOwnerName').and.returnValue('Owner Name');

    spyOn(routerSelectors, 'selectRouterSlice').and.returnValue(() => ({
      currentState: { name: 'applications' },
      currentParams: {
        applicationId: 'applicationId',
        labelId: 'labelId',
      },
    }));

    spyOn(routerContext, 'useRouterState').and.returnValue({
      href: jasmine.createSpy('href').and.returnValue(`editLabelHref`),
    });

    spyOn(actions, 'loadApplicableLabels').and.returnValue({
      type: 'labels/loadApplicableLabels/fulfilled',
      payload: [],
    });

    goToCreateLabelSpy = spyOn(actions, 'goToCreateLabel').and.callThrough();

    renderComponent = () => render(<LabelsTile />);
  });

  it('renders loading indicator', () => {
    selectLabelsLoadingSpy.and.returnValue(true);
    renderComponent();
    expect(screen.getByText('Loading…')).toBeVisible();
  });

  it('renders error alert on load error', () => {
    selectLabelsLoadErrorSpy.and.returnValue('Load Error');
    renderComponent();

    const error = screen.getByRole('alert');

    expect(error).toBeVisible();
  });

  it('renders tile with the correct title and subtitle', () => {
    renderComponent();
    expect(screen.getByText('Component Labels')).toBeVisible();
    expect(screen.getByText('available to Owner Name policies')).toBeVisible();
  });

  it('navigates to component label create page', () => {
    renderComponent();

    const addButton = screen.getByRole('button', { name: 'Add a Label' });
    fireEvent.click(addButton);

    expect(goToCreateLabelSpy).toHaveBeenCalled();
  });

  it('renders empty message if no local labels', () => {
    renderComponent();
    expect(screen.getByText('No local component labels defined')).toBeVisible();
  });

  it('does not render inherited section if no inherited labels', () => {
    renderComponent();
    expect(screen.queryByText('Inherited from')).not.toBeInTheDocument();
  });

  describe('local labels', () => {
    beforeEach(() => {
      selectApplicableLabelsSpy.and.returnValue([
        {
          ownerId: '202',
          ownerName: 'ownerNameLocal',
          inherited: false,
          labels: [
            {
              id: 'labelId',
              label: 'label title',
              description: 'description',
              color: 'dark-blue',
              ownerId: '202',
              ownerType: 'APPLICATION',
            },
          ],
        },
      ]);
    });

    it('rendered correctly with links', () => {
      selectHasEditIqPermissionSpy.and.returnValue(true);

      renderComponent();

      const listItems = screen.getAllByRole('listitem');

      expect(screen.getByText('Local to Owner Name')).toBeVisible();
      expect(listItems[0]).toBeVisible();
      expect(listItems[0]).toHaveTextContent('label title');

      expect(listItems[0].firstChild).toHaveAttribute('href', 'editLabelHref');
    });

    it('rendered correctly without links', () => {
      selectHasEditIqPermissionSpy.and.returnValue(false);

      renderComponent();

      const listItems = screen.getAllByRole('listitem');

      expect(screen.getByText('Local to Owner Name')).toBeVisible();
      expect(listItems[0]).toBeVisible();
      expect(listItems[0]).toHaveTextContent('label title');

      expect(listItems[0].firstChild).not.toHaveAttribute('href');
    });
  });

  describe('inherited labels', () => {
    beforeEach(() => {
      selectApplicableLabelsSpy.and.returnValue([
        {
          ownerId: '203',
          ownerName: 'ownerNameInherited',
          inherited: true,
          labels: [
            {
              id: 'inheritedLabelId',
              label: 'inherited title',
              description: 'inherited description',
              color: 'dark-green',
              ownerId: '203',
              ownerType: 'APPLICATION',
            },
          ],
        },
      ]);

      selectInheritedLabelsOpenSpy.and.returnValue({
        203: true,
      });
    });

    it('are rendered correctly', () => {
      renderComponent();

      const listItems = screen.getAllByRole('listitem');

      expect(screen.getByText('Inherited from ownerNameInherited')).toBeVisible();
      expect(listItems[1]).toHaveTextContent('inherited title');
      expect(listItems[1]).not.toHaveAttribute('href');
    });
  });
});
