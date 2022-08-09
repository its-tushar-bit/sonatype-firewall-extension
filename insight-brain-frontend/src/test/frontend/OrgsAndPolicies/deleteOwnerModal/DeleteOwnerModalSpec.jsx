/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import DeleteOwnerModal from 'MainRoot/OrgsAndPolicies/deleteOwnerModal/DeleteOwnerModal';
import * as deleteOwnerSelectors from 'MainRoot/OrgsAndPolicies/deleteOwnerModal/deleteOwnerSelectors';
import * as orgsAndPoliciesSelectors from 'MainRoot/OrgsAndPolicies/orgsAndPoliciesSelectors';
import * as routerSelectors from 'MainRoot/reduxUiRouter/routerSelectors';
import { actions } from 'MainRoot/OrgsAndPolicies/deleteOwnerModal/deleteOwnerSlice';
import { fireEvent, render, screen } from 'TestRoot/SpecUtil';

describe('DeleteOwnerModal', () => {
  let renderComponent, deleteOwnerStateSpy, closeModalSpy, removeOwnerSpy;

  const OWNER_ORG_NAME = 'Organization Two Name';
  const OWNER_APP_NAME = 'Application One Name';

  beforeEach(() => {
    deleteOwnerStateSpy = spyOn(deleteOwnerSelectors, 'selectDeleteOwnerSlice').and.returnValue({
      submitError: null,
      isModalOpen: true,
    });

    removeOwnerSpy = spyOn(actions, 'removeOwner').and.callThrough();
    closeModalSpy = spyOn(actions, 'closeModal').and.callThrough();

    renderComponent = () => render(<DeleteOwnerModal />);
  });

  describe('Organization', () => {
    beforeEach(() => {
      spyOn(routerSelectors, 'selectIsApplication').and.returnValue(false);
      spyOn(orgsAndPoliciesSelectors, 'selectSelectedOwnerName').and.returnValue(OWNER_ORG_NAME);
      spyOn(orgsAndPoliciesSelectors, 'selectSelectedOwner').and.returnValue({
        id: 'organizationTwoID',
        name: OWNER_ORG_NAME,
        parentOrganizationId: 'ROOT_ORGANIZATION_ID',
      });
    });

    it('renders modal with the correct page title - Organization', () => {
      renderComponent();

      expect(screen.getByText('Delete Organization')).toBeVisible();
      expect(screen.getByRole('dialog')).toHaveTextContent(OWNER_ORG_NAME);
    });

    it('not renders modal without being open', () => {
      deleteOwnerStateSpy.and.returnValue({
        submitError: null,
        isModalOpen: false,
      });
      renderComponent();

      const initialTitle = screen.queryAllByText('Delete Organization');
      expect(initialTitle.length).toBe(0);
    });

    it('renders modal with correct content', () => {
      renderComponent();
      expect(
        screen.getByText(`You are about to permanently remove ${OWNER_ORG_NAME}. This action cannot be undone.`)
      ).toBeVisible();

      expect(screen.getByRole('button', { name: 'Cancel' })).toBeVisible();
      expect(screen.getByRole('button', { name: 'Delete' })).toBeVisible();
    });

    it('renders error on submitError', () => {
      deleteOwnerStateSpy.and.returnValue({
        submitError: 'Error 404',
        isModalOpen: true,
      });
      renderComponent();

      const error = screen.getByRole('alert');

      expect(error).toBeVisible();
      expect(screen.getByText('An error occurred saving data. Error 404')).toBeVisible();
    });

    it('triggers removeOwner', () => {
      renderComponent();

      const submitButton = screen.getByRole('button', { name: 'Delete' });
      expect(submitButton).toBeVisible();
      expect(submitButton).not.toHaveClassName('disabled');
      fireEvent.click(submitButton);
      expect(removeOwnerSpy).toHaveBeenCalledTimes(1);
    });

    it('close modal on cancel', () => {
      renderComponent();

      const closeButton = screen.getByRole('button', { name: 'Cancel' });
      expect(closeButton).toBeVisible();
      expect(closeButton).not.toHaveClassName('disabled');
      fireEvent.click(closeButton);
      expect(closeModalSpy).toHaveBeenCalledTimes(1);
    });
  });

  describe('Application', () => {
    beforeEach(() => {
      spyOn(routerSelectors, 'selectIsApplication').and.returnValue(true);
      spyOn(orgsAndPoliciesSelectors, 'selectSelectedOwnerName').and.returnValue(OWNER_APP_NAME);
      spyOn(orgsAndPoliciesSelectors, 'selectSelectedOwner').and.returnValue({
        id: 'applicationOneID',
        publicId: 'applicationOnePublicID',
        organizationId: 'organizationOneID',
        name: OWNER_APP_NAME,
      });
    });

    it('renders modal with the correct page title - Application', () => {
      renderComponent();

      expect(screen.getByText('Delete Application')).toBeVisible();
      expect(screen.getByRole('dialog')).toHaveTextContent(OWNER_APP_NAME);
    });

    it('not renders modal without being open', () => {
      deleteOwnerStateSpy.and.returnValue({
        submitError: null,
        isModalOpen: false,
      });
      renderComponent();

      const initialTitle = screen.queryAllByText('Delete Application');
      expect(initialTitle.length).toBe(0);
    });

    it('renders modal with correct content', () => {
      renderComponent();
      expect(
        screen.getByText(`You are about to permanently remove ${OWNER_APP_NAME}. This action cannot be undone.`)
      ).toBeVisible();

      expect(screen.getByRole('button', { name: 'Cancel' })).toBeVisible();
      expect(screen.getByRole('button', { name: 'Delete' })).toBeVisible();
    });
  });
});
