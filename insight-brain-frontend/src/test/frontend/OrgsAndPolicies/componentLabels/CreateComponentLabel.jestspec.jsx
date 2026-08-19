/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import CreateComponentLabel from 'MainRoot/OrgsAndPolicies/componentLabels/CreateComponentLabel';
import * as productFeaturesSelectors from 'MainRoot/productFeatures/productFeaturesSelectors';
import * as labelsSelectors from 'MainRoot/OrgsAndPolicies/labelsSelectors';
import * as routerSelectors from 'MainRoot/reduxUiRouter/routerSelectors';
import { actions } from 'MainRoot/OrgsAndPolicies/labelsSlice';
import { render, screen, fireEvent } from 'TestRoot/SpecUtil';

import 'TestRoot/SpecUtil';

describe('CreateComponentLabel create', () => {
  let renderComponent, selectLabelsLoadingSpy, selectLabelsLoadErrorSpy, saveLabelSpy, setLabelNameSpy;

  beforeEach(() => {
    jest.spyOn(productFeaturesSelectors, 'selectHasCustomComponentLabels').mockReturnValue(true);
    selectLabelsLoadingSpy = jest.spyOn(labelsSelectors, 'selectLabelsLoading').mockReturnValue(false);
    selectLabelsLoadErrorSpy = jest.spyOn(labelsSelectors, 'selectLabelsLoadError').mockReturnValue(null);

    saveLabelSpy = jest.spyOn(actions, 'saveLabel');
    setLabelNameSpy = jest.spyOn(actions, 'setLabelName');

    renderComponent = () => render(<CreateComponentLabel />);
  });

  it('renders tile with the correct page title', () => {
    renderComponent();

    expect(screen.getByText('Component Label Settings')).toBeVisible();
  });

  it('renders loading indicator', () => {
    selectLabelsLoadingSpy.mockReturnValue(true);
    renderComponent();

    expect(screen.getByText('Loading…')).toBeVisible();
  });

  it('initial create label not trigger', () => {
    renderComponent();

    const createButton = screen.getByText('Create');
    expect(createButton).toBeVisible();

    fireEvent.click(createButton);
    expect(saveLabelSpy).not.toHaveBeenCalled();
  });

  it('does create label can trigger', () => {
    renderComponent();

    const labelInputValue = 'labelValue';

    const labelInput = screen.getAllByRole('textbox')[0];
    fireEvent.change(labelInput, { target: { value: labelInputValue } });

    expect(setLabelNameSpy).toHaveBeenCalled();
    expect(setLabelNameSpy).toHaveBeenCalledWith(labelInputValue);
    expect(screen.getByDisplayValue(labelInputValue)).toBeVisible();

    const createButton = screen.getByText('Create');
    expect(createButton).toBeVisible();
    expect(createButton).not.toHaveClass('disabled');

    fireEvent.click(createButton);
    expect(saveLabelSpy).toHaveBeenCalled();
  });

  it('shows error message on error', () => {
    selectLabelsLoadErrorSpy.mockReturnValue('Error');
    renderComponent();

    const error = screen.getByRole('alert');

    expect(error).toBeVisible();
  });
});

describe('CreateComponentLabel edit', () => {
  let renderComponent, saveLabelSpy, setLabelNameSpy, removeLabelSpy;

  beforeEach(() => {
    jest.spyOn(productFeaturesSelectors, 'selectHasCustomComponentLabels').mockReturnValue(true);
    jest.spyOn(labelsSelectors, 'selectLabelsLoadError').mockReturnValue(null);
    jest.spyOn(labelsSelectors, 'selectLabelsLoading').mockReturnValue(false);
    jest.spyOn(routerSelectors, 'selectRouterCurrentParams').mockReturnValue({
      labelId: '353653653653',
    });

    jest.spyOn(actions, 'loadLabelsEditor').mockReturnValue({
      type: 'labels/loadLabelsEditor/fulfilled',
      payload: {
        currentLabel: {
          color: 'light-red',
          description: 'initialDesc',
          label: 'initialLabel',
        },
        sublings: [],
      },
    });

    saveLabelSpy = jest.spyOn(actions, 'saveLabel');
    removeLabelSpy = jest.spyOn(actions, 'removeLabel');
    setLabelNameSpy = jest.spyOn(actions, 'setLabelName');

    renderComponent = () => render(<CreateComponentLabel />);
  });

  it('renders tile with the correct page title', () => {
    renderComponent();

    expect(screen.getByText('Component Label Settings')).toBeVisible();
  });

  it('initial update label not trigger', () => {
    renderComponent();

    const updateButton = screen.getByText('Update');
    const deleteButton = screen.getByText('Delete');

    expect(updateButton).toBeVisible();
    expect(deleteButton).toBeVisible();

    fireEvent.click(updateButton);
    expect(saveLabelSpy).not.toHaveBeenCalled();
  });

  it('does update label can trigger', () => {
    renderComponent();

    const labelInputValue = 'labelValue';

    const labelInput = screen.getAllByRole('textbox')[0];
    fireEvent.change(labelInput, { target: { value: labelInputValue } });

    expect(setLabelNameSpy).toHaveBeenCalled();
    expect(setLabelNameSpy).toHaveBeenCalledWith(labelInputValue);
    expect(screen.getByDisplayValue(labelInputValue)).toBeVisible();

    const updateButton = screen.getByText('Update');
    expect(updateButton).toBeVisible();
    expect(updateButton).not.toHaveClass('disabled');

    fireEvent.click(updateButton);
    expect(saveLabelSpy).toHaveBeenCalled();
  });

  it('if same label values, than button is disabled', () => {
    renderComponent();

    const labelInputValue = 'labelValue';

    const labelInput = screen.getAllByRole('textbox')[0];

    fireEvent.change(labelInput, { target: { value: labelInputValue } });

    expect(setLabelNameSpy).toHaveBeenCalled();
    expect(screen.getByDisplayValue(labelInputValue)).toBeVisible();

    const updateButton = screen.getByText('Update');
    expect(updateButton).not.toHaveClass('disabled');
    fireEvent.change(labelInput, { target: { value: 'initialLabel' } });

    const updateButtonReinitialize = screen.getByText('Update');

    fireEvent.click(updateButtonReinitialize);
    expect(saveLabelSpy).not.toHaveBeenCalled();
  });

  it('delete label working', () => {
    renderComponent();

    const deleteButton = screen.getByText('Delete');
    expect(deleteButton).toBeVisible();
    fireEvent.click(deleteButton);

    expect(screen.getByText('Delete Label')).toBeVisible();

    const modalDeleteButton = screen.getAllByText('Delete')[1];
    fireEvent.click(modalDeleteButton);

    expect(removeLabelSpy).toHaveBeenCalled();
  });
});

describe('CreateComponentLabel Pro Tier Gating', () => {
  beforeEach(() => {
    jest.spyOn(productFeaturesSelectors, 'selectHasCustomComponentLabels').mockReturnValue(false);
    jest.spyOn(labelsSelectors, 'selectLabelsLoadError').mockReturnValue(null);
    jest.spyOn(labelsSelectors, 'selectLabelsLoading').mockReturnValue(false);
    jest.spyOn(labelsSelectors, 'selectLabelsIsEditMode').mockReturnValue(true);
    jest.spyOn(routerSelectors, 'selectRouterCurrentParams').mockReturnValue({ labelId: '123' });
    jest.spyOn(actions, 'loadLabelsEditor').mockReturnValue({
      type: 'labels/loadLabelsEditor/fulfilled',
      payload: {
        currentLabel: { color: 'light-red', description: 'desc', label: 'test' },
        sublings: [],
      },
    });
  });

  it('shows mode switch with Default and Custom buttons when editing', () => {
    render(<CreateComponentLabel />);
    expect(screen.getByText('Default')).toBeVisible();
    expect(screen.getByText('Custom')).toBeVisible();
  });

  it('does not show Delete button for Pro tier user', () => {
    render(<CreateComponentLabel />);
    expect(screen.queryByText('Delete')).not.toBeInTheDocument();
  });

  it('shows Enterprise Feature banner in create mode', () => {
    jest.spyOn(labelsSelectors, 'selectLabelsIsEditMode').mockReturnValue(false);
    jest.spyOn(routerSelectors, 'selectRouterCurrentParams').mockReturnValue({});
    render(<CreateComponentLabel />);
    expect(screen.getByText('Component Label Settings')).toBeVisible();
  });
});
