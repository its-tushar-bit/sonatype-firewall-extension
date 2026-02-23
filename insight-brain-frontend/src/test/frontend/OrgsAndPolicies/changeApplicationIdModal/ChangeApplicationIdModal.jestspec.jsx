/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import ChangeApplicationIdModal from 'MainRoot/OrgsAndPolicies/changeApplicationIdModal/ChangeApplicationIdModal';
import * as changeApplicationIdSelectors from 'MainRoot/OrgsAndPolicies/changeApplicationIdModal/changeApplicationIdSelectors';
import * as orgsAndPoliciesSelectors from 'MainRoot/OrgsAndPolicies/orgsAndPoliciesSelectors';
import * as ownerSideNavSelectors from 'MainRoot/OrgsAndPolicies/ownerSideNav/ownerSideNavSelectors';
import * as routerSelectors from 'MainRoot/reduxUiRouter/routerSelectors';
import { actions } from 'MainRoot/OrgsAndPolicies/changeApplicationIdModal/changeApplicationIdSlice';
import { fireEvent, render, screen } from 'TestRoot/SpecUtil';
import { nxTextInputStateHelpers } from '@sonatype/react-shared-components';

// Import SpecUtil for jasmine compatibility layer
import 'TestRoot/SpecUtil';

const { initialState: rscInitialState } = nxTextInputStateHelpers;

describe('ChangeApplicationIdModal', () => {
  let renderComponent, changeApplicationIdStateSpy, closeModalSpy, changeApplicationIdSpy, newPublicIdSpy;
  const OWNER_APP_ID = 'applicationOnePublicID';
  const APP = {
    id: 'applicationOneID',
    publicId: OWNER_APP_ID,
    organizationId: 'organizationOneID',
    name: 'Application One Name',
  };

  beforeEach(() => {
    changeApplicationIdStateSpy = jest
      .spyOn(changeApplicationIdSelectors, 'selectChangeApplicationIdSlice')
      .mockReturnValue({
        submitError: null,
        isModalOpen: true,
        newPublicId: rscInitialState(''),
      });
    jest.spyOn(routerSelectors, 'selectIsApplication').mockReturnValue(true);
    jest.spyOn(orgsAndPoliciesSelectors, 'selectSelectedOwner').mockReturnValue(APP);
    jest.spyOn(ownerSideNavSelectors, 'selectOwnersFlattenEntries').mockReturnValue({ applications: [APP] });

    changeApplicationIdSpy = jest.spyOn(actions, 'changeApplicationId');
    closeModalSpy = jest.spyOn(actions, 'closeModal');
    newPublicIdSpy = jest.spyOn(actions, 'setNewPublicIdValue');

    renderComponent = () => render(<ChangeApplicationIdModal />);
  });

  it('renders modal with the correct page title', () => {
    renderComponent();

    expect(screen.getByText('Change Application ID')).toBeVisible();
    expect(screen.getByRole('dialog')).toHaveTextContent(OWNER_APP_ID);
  });

  it('does not render modal without being open', () => {
    changeApplicationIdStateSpy.mockReturnValue({
      submitError: null,
      isModalOpen: false,
    });
    renderComponent();

    const initialTitle = screen.queryAllByText('Change Application ID');
    expect(initialTitle.length).toBe(0);
  });

  it('renders modal with correct content', () => {
    renderComponent();
    expect(
      screen.getByText(
        `Changing the Application ID will break existing integration points. They will have to be re-configured.`
      )
    ).toBeVisible();
    expect(screen.getByText(`Current Application ID`)).toBeVisible();
    expect(screen.getByText(`New Application ID`)).toBeVisible();

    expect(screen.getByRole('button', { name: 'Cancel' })).toBeVisible();
    expect(screen.getByRole('button', { name: 'Change' })).toBeVisible();
  });

  it('renders error on submitError', () => {
    changeApplicationIdStateSpy.mockReturnValue({
      submitError: 'Error 404',
      isModalOpen: true,
    });
    renderComponent();

    const errors = screen.getAllByRole('alert');

    // main alert
    expect(errors.length).toBe(1);
    expect(screen.getByText('An error occurred saving data. Error 404')).toBeVisible();
  });

  it('triggers changeApplicationId', () => {
    renderComponent();

    const newIdValue = 'qwerty';

    const idInput = screen.getByRole('textbox');
    fireEvent.change(idInput, { target: { value: newIdValue } });

    expect(newPublicIdSpy).toHaveBeenCalled();
    expect(newPublicIdSpy).toHaveBeenCalledWith({ value: newIdValue, appsList: [APP] });
    expect(screen.getByDisplayValue(newIdValue)).toBeVisible();

    const submitButton = screen.getByRole('button', { name: 'Change' });
    expect(submitButton).toHaveTextContent('Change');
    expect(submitButton).not.toHaveClass('disabled');
    fireEvent.click(submitButton);
    expect(changeApplicationIdSpy).toHaveBeenCalledTimes(1);
  });

  it('close modal on cancel', () => {
    renderComponent();

    const closeButton = screen.getByRole('button', { name: 'Cancel' });
    expect(closeButton).toBeVisible();
    expect(closeButton).not.toHaveClass('disabled');
    fireEvent.click(closeButton);
    expect(closeModalSpy).toHaveBeenCalledTimes(1);
  });

  describe('input validation', () => {
    it('can not trigger change Id when there is incorrect symbols', () => {
      renderComponent();

      const idInput = screen.getByRole('textbox');
      fireEvent.change(idInput, { target: { value: 'some text' } });

      const submitButton = screen.getByRole('button', { name: 'Change' });
      expect(submitButton).toHaveTextContent('Change');
      fireEvent.click(submitButton);
      expect(changeApplicationIdSpy).not.toHaveBeenCalled();
      const errorText = screen.getByText('Use valid characters: alphanumeric, "_", "." or "-"');
      expect(errorText).toBeVisible();
    });

    it('can not trigger change Id when there is duplicate', () => {
      renderComponent();

      const idInput = screen.getByRole('textbox');
      fireEvent.change(idInput, { target: { value: OWNER_APP_ID } });

      const submitButton = screen.getByRole('button', { name: 'Change' });
      expect(newPublicIdSpy).toHaveBeenCalled();
      expect(newPublicIdSpy).toHaveBeenCalledWith({ value: OWNER_APP_ID, appsList: [APP] });
      fireEvent.click(submitButton);
      expect(changeApplicationIdSpy).not.toHaveBeenCalled();
      const errorText = screen.getByText('Name is already in use');
      expect(errorText).toBeVisible();
    });

    it('can not trigger change Id when characters are more than 200', () => {
      renderComponent();

      const idInput = screen.getByRole('textbox');
      fireEvent.change(idInput, { target: { value: 'a'.repeat(201) } });
      const submitButton = screen.getByRole('button', { name: 'Change' });

      expect(submitButton).toHaveTextContent('Change');
      fireEvent.click(submitButton);
      expect(changeApplicationIdSpy).not.toHaveBeenCalled();
      const errorText = screen.getByText('Please enter less than 200 characters');
      expect(errorText).toBeVisible();
    });

    it('can not trigger change Id when input is empty', () => {
      renderComponent();

      const idInput = screen.getByRole('textbox');
      fireEvent.change(idInput, { target: { value: 'some text' } });
      fireEvent.change(idInput, { target: { value: '' } });

      const submitButton = screen.getByRole('button', { name: 'Change' });
      expect(newPublicIdSpy).toHaveBeenCalled();
      fireEvent.click(submitButton);
      expect(changeApplicationIdSpy).not.toHaveBeenCalled();
      const errorText = screen.getByText('Must be non-empty');
      expect(errorText).toBeVisible();
    });
  });
});
