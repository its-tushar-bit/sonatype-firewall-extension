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
import router from 'MainRoot/router/routerInstance';

describe('LabelsTile', () => {
  let renderComponent,
    goToCreateLabelSpy,
    selectLabelsLoadingSpy,
    selectLabelsLoadErrorSpy,
    selectApplicableLabelsSpy,
    selectInheritedLabelsOpenSpy,
    selectHasEditIqPermissionSpy;

  beforeEach(() => {
    selectLabelsLoadingSpy = jest.spyOn(labelsSelectors, 'selectLabelsLoading').mockReturnValue(false);
    selectLabelsLoadErrorSpy = jest.spyOn(labelsSelectors, 'selectLabelsLoadError').mockReturnValue(null);
    selectApplicableLabelsSpy = jest.spyOn(labelsSelectors, 'selectApplicableLabels').mockReturnValue([]);
    selectInheritedLabelsOpenSpy = jest.spyOn(labelsSelectors, 'selectInheritedLabelsOpen').mockReturnValue({});
    selectHasEditIqPermissionSpy = jest
      .spyOn(ownerSummarySelectors, 'selectHasEditIqPermission')
      .mockReturnValue(false);
    jest.spyOn(orgsAndPoliciesSelectors, 'selectSelectedOwnerName').mockReturnValue('Owner Name');

    jest.spyOn(routerSelectors, 'selectRouterSlice').mockReturnValue(() => ({
      currentState: { name: 'applications' },
      currentParams: {
        applicationId: 'applicationId',
        labelId: 'labelId',
      },
    }));

    jest.spyOn(router.stateService, 'href').mockReturnValue('editLabelHref');
    jest.spyOn(router.stateService, 'includes').mockReturnValue(false);
    jest.spyOn(router.stateService, 'get').mockReturnValue(null);

    jest.spyOn(actions, 'loadApplicableLabels').mockReturnValue({
      type: 'labels/loadApplicableLabels/fulfilled',
      payload: [],
    });

    goToCreateLabelSpy = jest.spyOn(actions, 'goToCreateLabel');

    renderComponent = () => render(<LabelsTile />);
  });

  it('renders loading indicator', () => {
    selectLabelsLoadingSpy.mockReturnValue(true);
    renderComponent();
    expect(screen.getByText('Loading…')).toBeVisible();
  });

  it('renders error alert on load error', () => {
    selectLabelsLoadErrorSpy.mockReturnValue('Load Error');
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
      selectApplicableLabelsSpy.mockReturnValue([
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
      selectHasEditIqPermissionSpy.mockReturnValue(true);

      renderComponent();

      const listItems = screen.getAllByRole('listitem');

      expect(screen.getByText('Local to Owner Name')).toBeVisible();
      expect(listItems[0]).toBeVisible();
      expect(listItems[0]).toHaveTextContent('label title');

      expect(listItems[0].firstChild).toHaveAttribute('href', 'editLabelHref');
    });

    it('rendered correctly without links', () => {
      selectHasEditIqPermissionSpy.mockReturnValue(false);

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
      selectApplicableLabelsSpy.mockReturnValue([
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

      selectInheritedLabelsOpenSpy.mockReturnValue({
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
