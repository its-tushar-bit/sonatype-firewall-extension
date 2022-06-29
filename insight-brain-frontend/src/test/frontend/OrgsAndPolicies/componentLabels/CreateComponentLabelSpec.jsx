/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import CreateComponentLabel from 'MainRoot/OrgsAndPolicies/componentLabels/CreateComponentLabel';
import * as labelsSelectors from 'MainRoot/OrgsAndPolicies/labelsSelectors';
import * as routerSelectors from 'MainRoot/reduxUiRouter/routerSelectors';
import { actions } from 'MainRoot/OrgsAndPolicies/labelsSlice';
import { render, screen, fireEvent } from 'TestRoot/SpecUtil';

describe('CreateComponentLabel create', () => {
  let renderComponent, selectLabelsLoadingSpy, selectLabelsLoadErrorSpy, saveLabelSpy, setLabelNameSpy;

  beforeEach(() => {
    selectLabelsLoadingSpy = spyOn(labelsSelectors, 'selectLabelsLoading').and.returnValue(false);
    selectLabelsLoadErrorSpy = spyOn(labelsSelectors, 'selectLabelsLoadError').and.returnValue(null);

    saveLabelSpy = spyOn(actions, 'saveLabel').and.callThrough();
    setLabelNameSpy = spyOn(actions, 'setLabelName').and.callThrough();

    renderComponent = () => render(<CreateComponentLabel />);
  });

  it('renders tile with the correct page title', () => {
    renderComponent();

    expect(screen.getByText('New Component Label')).toBeVisible();
  });

  it('renders loading indicator', () => {
    selectLabelsLoadingSpy.and.returnValue(true);
    renderComponent();

    expect(screen.getByText('Loading…')).toBeVisible();
  });

  it('initial create label not trigger', () => {
    renderComponent();

    const createButton = screen.getByText('Create');
    expect(createButton).toBeVisible();
    expect(createButton).toHaveClassName('disabled');

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
    expect(createButton).not.toHaveClassName('disabled');

    fireEvent.click(createButton);
    expect(saveLabelSpy).toHaveBeenCalled();
  });

  it('shows error message on error', () => {
    selectLabelsLoadErrorSpy.and.returnValue('Error');
    renderComponent();

    const error = screen.getByRole('alert');

    expect(error).toBeVisible();
  });
});

describe('CreateComponentLabel edit', () => {
  let renderComponent, saveLabelSpy, setLabelNameSpy, removeLabelSpy;

  beforeEach(() => {
    spyOn(labelsSelectors, 'selectLabelsLoadError').and.returnValue(null);
    spyOn(labelsSelectors, 'selectLabelsLoading').and.returnValue(false);
    spyOn(routerSelectors, 'selectRouterCurrentParams').and.returnValue({
      labelId: '353653653653',
    });

    spyOn(actions, 'loadLabelsEditor').and.returnValue({
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

    saveLabelSpy = spyOn(actions, 'saveLabel').and.callThrough();
    removeLabelSpy = spyOn(actions, 'removeLabel').and.callThrough();
    setLabelNameSpy = spyOn(actions, 'setLabelName').and.callThrough();

    renderComponent = () => render(<CreateComponentLabel />);
  });

  it('renders tile with the correct page title', () => {
    renderComponent();

    expect(screen.getByText('Edit Component Label')).toBeVisible();
  });

  it('initial update label not trigger', () => {
    renderComponent();

    const updateButton = screen.getByText('Update');
    const deleteButton = screen.getByText('Delete');

    expect(updateButton).toBeVisible();
    expect(deleteButton).toBeVisible();
    expect(updateButton).toHaveClassName('disabled');

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
    expect(updateButton).not.toHaveClassName('disabled');

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
    expect(updateButton).not.toHaveClassName('disabled');
    fireEvent.change(labelInput, { target: { value: 'initialLabel' } });

    const updateButtonReinitialize = screen.getByText('Update');
    expect(updateButtonReinitialize).toHaveClassName('disabled');

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
