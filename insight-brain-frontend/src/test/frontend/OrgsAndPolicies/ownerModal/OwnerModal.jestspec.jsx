/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { getApplicationsUrl, getOrganizationsUrl, getNLevelOrgUrl, getAddIconUrl } from 'MainRoot/util/CLMLocation';
import OwnerModal from 'MainRoot/OrgsAndPolicies/ownerModal/OwnerModal';
import { fireEvent, render, screen, axiosMockAdapter, waitFor } from 'TestRoot/SpecUtil';
import { nxTextInputStateHelpers, nxFileUploadStateHelpers } from '@sonatype/react-shared-components';
import { validateNonEmpty } from 'MainRoot/util/validationUtil';
import router from 'MainRoot/router/routerInstance';

const { initialState: rscInitialState } = nxTextInputStateHelpers;
const { initialState: rscInitialFileUploadState } = nxFileUploadStateHelpers;

const APPS = [
  {
    id: 'applicationOneID',
    publicId: 'applicationOnePublicID',
    organizationId: 'organizationOneID',
    name: 'ApplicationOneName',
  },
  {
    id: 'applicationTwoID',
    publicId: 'applicationTwoPublicID',
    organizationId: 'organizationOneID',
    name: 'ApplicationTwoName',
  },
  {
    id: 'applicationThreeID',
    publicId: 'applicationThreePublicID',
    organizationId: 'organizationTwoID',
    name: 'ApplicationThreeName',
  },
];

const ORGS = [
  {
    id: 'organizationOneID',
    name: 'OrganizationOneName',
  },
  {
    id: 'organizationTwoID',
    name: 'OrganizationTwoName',
  },
];

const ownersMap = {
  applicationOnePublicID: {
    id: 'applicationOneID',
    publicId: 'applicationOnePublicID',
    organizationId: 'organizationOneID',
    name: 'ApplicationOneName',
    type: 'application',
  },
  applicationTwoPublicID: {
    id: 'applicationTwoID',
    publicId: 'applicationTwoPublicID',
    organizationId: 'organizationOneID',
    name: 'ApplicationTwoName',
    type: 'application',
  },
  applicationThreePublicID: {
    id: 'applicationThreeID',
    publicId: 'applicationThreePublicID',
    organizationId: 'organizationTwoID',
    name: 'ApplicationThreeName',
    type: 'application',
  },

  organizationOneID: {
    id: 'organizationOneID',
    name: 'OrganizationOneName',
    type: 'organization',
  },
  organizationTwoID: {
    id: 'organizationTwoID',
    name: 'OrganizationTwoName',
    type: 'organization',
  },
};

const defaultPreloadedState = {
  router: {
    currentState: {
      name: 'management.view.organization',
    },
    currentParams: {
      organizationId: 'ROOT_ORGANIZATION_ID',
    },
  },
  orgsAndPolicies: {
    applications: {
      applications: APPS,
    },
    organizations: {
      organizations: ORGS,
    },
    root: {
      selectedOwner: {
        id: 'ROOT_ORGANIZATION_ID',
        name: 'Root Organization',
        type: 'organization',
      },
    },
    ownerSideNav: {
      displayedOrganization: {
        type: 'organization',
        id: 'ROOT_ORGANIZATION_ID',
        name: 'Root Organization',
      },
      ownersMap,
    },
    ownerActions: {
      ownerModal: {
        submitError: null,
        submitMaskState: null,
        isModalOpen: true,
        isEditMode: false,
        isApplication: null,
        ownerIconType: '',
        ownerIcon: rscInitialFileUploadState(null),
        robotHash: '',
        validationErrors: [null],
        ownerName: rscInitialState('', validateNonEmpty),
        appId: rscInitialState('', validateNonEmpty),
        isDirty: false,
        isUnsavedChangesModalOpen: false,
      },
    },
  },
};

const createAppState = {
  router: {
    currentState: {
      name: 'management.view.organization',
    },
    currentParams: {
      organizationId: 'organizationOneID',
    },
  },
  orgsAndPolicies: {
    applications: {
      applications: APPS,
    },
    organizations: {
      organizations: ORGS,
    },
    root: {
      selectedOwner: { type: 'organization', ...ORGS[0] },
    },
    ownerSideNav: {
      displayedOrganization: {
        type: 'organization',
        id: ORGS[0].id,
        name: ORGS[0].name,
      },
      ownersMap,
    },
    ownerActions: {
      ownerModal: {
        submitError: null,
        submitMaskState: null,
        isModalOpen: true,
        isEditMode: false,
        isApplication: true,
        ownerIconType: '',
        ownerIcon: rscInitialFileUploadState(null),
        robotHash: '',
        validationErrors: [null],
        ownerName: rscInitialState('', validateNonEmpty),
        appId: rscInitialState('', validateNonEmpty),
        isDirty: false,
        isUnsavedChangesModalOpen: false,
      },
    },
  },
};

const sbomManagerCreateAppState = {
  ...createAppState,
  router: {
    ...createAppState.router,
    currentState: {
      name: 'sbomManager.management.view.organization',
    },
  },
};

const editOrgState = {
  router: {
    currentState: {
      name: 'management.view.organization',
    },
    currentParams: {
      organizationId: 'organizationOneID',
    },
  },
  orgsAndPolicies: {
    applications: {
      applications: APPS,
    },
    organizations: {
      organizations: ORGS,
    },
    root: {
      selectedOwner: { type: 'organization', ...ORGS[0] },
    },
    ownerSideNav: {
      displayedOrganization: {
        type: 'organization',
        id: ORGS[0].id,
        name: ORGS[0].name,
      },
      ownersMap,
    },
    ownerActions: {
      ownerModal: {
        submitError: null,
        submitMaskState: null,
        isModalOpen: true,
        isEditMode: true,
        isApplication: false,
        ownerIconType: '',
        ownerIcon: rscInitialFileUploadState(null),
        robotHash: '',
        validationErrors: [null],
        ownerName: rscInitialState('OrganizationOneName', validateNonEmpty),
        appId: rscInitialState('', validateNonEmpty),
        isDirty: false,
        isUnsavedChangesModalOpen: false,
      },
    },
  },
};

const editAppState = {
  router: {
    currentState: {
      name: 'management.view.application',
    },
    currentParams: {
      applicationPublicId: 'applicationOneID',
    },
  },
  orgsAndPolicies: {
    applications: {
      applications: APPS,
    },
    organizations: {
      organizations: ORGS,
    },
    root: {
      selectedOwner: { type: 'application', ...APPS[0] },
    },
    ownerSideNav: {
      displayedOrganization: {
        type: 'organization',
        id: ORGS[0].id,
        name: ORGS[0].name,
      },
      ownersMap,
    },
    ownerActions: {
      ownerModal: {
        submitError: null,
        submitMaskState: null,
        isModalOpen: true,
        isApplication: true,
        isEditMode: true,
        ownerIconType: '',
        ownerIcon: rscInitialFileUploadState(null),
        robotHash: '',
        validationErrors: [null],
        ownerName: rscInitialState('ApplicationOneName', validateNonEmpty),
        appId: rscInitialState('applicationOnePublicID', validateNonEmpty),
        isDirty: false,
        isUnsavedChangesModalOpen: false,
      },
    },
  },
};

const sbomManagerEditOrgState = {
  router: {
    currentState: {
      name: 'sbomManager.management.view.application',
    },
    currentParams: {
      organizationId: 'organizationOneID',
    },
  },
  orgsAndPolicies: {
    applications: {
      applications: APPS,
    },
    organizations: {
      organizations: ORGS,
    },
    root: {
      selectedOwner: { type: 'organization', ...ORGS[0] },
    },
    ownerSideNav: {
      displayedOrganization: {
        type: 'organization',
        id: ORGS[0].id,
        name: ORGS[0].name,
      },
      ownersMap,
    },
    ownerActions: {
      ownerModal: {
        submitError: null,
        submitMaskState: null,
        isModalOpen: true,
        isEditMode: true,
        isApplication: false,
        ownerIconType: '',
        ownerIcon: rscInitialFileUploadState(null),
        robotHash: '',
        validationErrors: [null],
        ownerName: rscInitialState('OrganizationOneName', validateNonEmpty),
        appId: rscInitialState('', validateNonEmpty),
        isDirty: false,
        isUnsavedChangesModalOpen: false,
      },
    },
  },
};

describe('OwnerModal', () => {
  let mock, renderComponent;

  beforeEach(() => {
    mock = axiosMockAdapter();

    jest.spyOn(router.stateService, 'href').mockImplementation((url, params) => {
      if (url.includes('scmOnboardingOrg')) {
        const organizationId = params?.organizationId;
        return `#/onboarding/${organizationId}`;
      }
      return '#';
    });
    jest.spyOn(router.stateService, 'includes').mockReturnValue(false);
    jest.spyOn(router.stateService, 'get').mockReturnValue(null);

    renderComponent = (preloadedState) =>
      render(<OwnerModal />, { preloadedState: preloadedState || defaultPreloadedState });
  });

  it('does not render modal without being open', () => {
    const state = defaultPreloadedState;
    state.orgsAndPolicies.ownerActions.ownerModal.isModalOpen = false;
    renderComponent(state);

    expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
  });

  it('close modal on cancel', () => {
    const state = defaultPreloadedState;
    state.orgsAndPolicies.ownerActions.ownerModal.isModalOpen = true;
    renderComponent();

    const closeButton = screen.getByRole('button', { name: 'Cancel' });
    expect(closeButton).toBeVisible();
    expect(closeButton).not.toHaveClass('disabled');
    fireEvent.click(closeButton);
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
  });

  describe('Create Organization', () => {
    it('renders modal with the correct page title', () => {
      renderComponent();

      expect(screen.getByText('New Organization')).toBeVisible();
    });

    it('renders modal with correct content', () => {
      renderComponent();
      expect(screen.getByText(`Organization Name`)).toBeVisible();
      expect(screen.getByText(`Use a default icon`)).toBeVisible();
      expect(screen.getByText(`Upload a custom icon`)).toBeVisible();
      expect(screen.getByText(`Get a robot`)).toBeVisible();

      expect(screen.getByRole('button', { name: 'Cancel' })).toBeVisible();
      expect(screen.getByRole('button', { name: 'Create' })).toBeVisible();
    });

    it('triggers createNewOwner', async () => {
      const newOwnerNameValue = 'qwerty';
      mock.onPost(getNLevelOrgUrl()).reply(200, {
        id: 'organizationThreeID',
        name: newOwnerNameValue,
      });
      renderComponent();

      const ownerNameInput = screen.getByRole('textbox');
      fireEvent.change(ownerNameInput, { target: { value: newOwnerNameValue } });

      expect(ownerNameInput.value).toBe(newOwnerNameValue);

      const submitButton = screen.getByRole('button', { name: 'Create' });
      expect(submitButton).toHaveTextContent('Create');
      expect(submitButton).not.toHaveClass('disabled');
      fireEvent.click(submitButton);
      await waitFor(() => expect(mock.history.post.length).toBe(1));
      expect(mock.history.put.length).toBe(0);
    });

    it('can not trigger createNewOwner when there are invalid characters in name input', () => {
      renderComponent();

      const ownerNameInput = screen.getByRole('textbox');
      fireEvent.change(ownerNameInput, { target: { value: 'some%text' } });

      const submitButton = screen.getByRole('button', { name: 'Create' });
      fireEvent.click(submitButton);
      expect(mock.history.post.length).toBe(0);
      expect(mock.history.put.length).toBe(0);
      const errorText = screen.getByText('Use valid characters: alphanumeric, "_", ".", "-", or spaces');
      expect(errorText).toBeVisible();
    });

    it('can not trigger createNewOwner when there is duplicate organization', () => {
      renderComponent();

      const ownerNameInput = screen.getByRole('textbox');
      fireEvent.change(ownerNameInput, { target: { value: ORGS[0].name } });

      const submitButton = screen.getByRole('button', { name: 'Create' });
      expect(ownerNameInput.value).toBe(ORGS[0].name);
      fireEvent.click(submitButton);
      expect(mock.history.post.length).toBe(0);
      expect(mock.history.put.length).toBe(0);
      const errorText = screen.getByText('Name is already in use');
      expect(errorText).toBeVisible();
    });

    it('can not trigger createNewOwner when there are more than 200 characters in name input', () => {
      renderComponent();

      const ownerNameInput = screen.getByRole('textbox');
      fireEvent.change(ownerNameInput, { target: { value: 'a'.repeat(201) } });
      const submitButton = screen.getByRole('button', { name: 'Create' });

      fireEvent.click(submitButton);
      expect(mock.history.post.length).toBe(0);
      expect(mock.history.put.length).toBe(0);
      const errorText = screen.getByText('Please enter less than 200 characters');
      expect(errorText).toBeVisible();
    });

    it('can not trigger createNewOwner when name input is empty', () => {
      renderComponent();

      const ownerNameInput = screen.getByRole('textbox');
      fireEvent.change(ownerNameInput, { target: { value: 'someText' } });
      fireEvent.change(ownerNameInput, { target: { value: '' } });

      const submitButton = screen.getByRole('button', { name: 'Create' });
      expect(ownerNameInput.value).toBe('');
      fireEvent.click(submitButton);
      expect(mock.history.post.length).toBe(0);
      expect(mock.history.put.length).toBe(0);
      const errorText = screen.getByText('Must be non-empty');
      expect(errorText).toBeVisible();
    });

    it('can not trigger createNewOwner when custom file input is empty', () => {
      const newNameValue = 'someText';
      renderComponent();

      const ownerNameInput = screen.getByRole('textbox');
      fireEvent.change(ownerNameInput, { target: { value: newNameValue } });

      const ownerIconRadio = screen.getByRole('radio', { name: 'Upload a custom icon' });
      fireEvent.click(ownerIconRadio);
      expect(ownerIconRadio).toBeChecked();

      const submitButton = screen.getByRole('button', { name: 'Create' });
      fireEvent.click(submitButton);
      expect(mock.history.post.length).toBe(0);
      expect(mock.history.put.length).toBe(0);
      const errorText = screen.getByText('No file selected');
      expect(errorText).toBeVisible();
    });
  });

  describe('Create Application', () => {
    it('renders modal with the correct page title', () => {
      renderComponent(createAppState);

      expect(screen.getByText('New Application')).toBeVisible();
    });

    it('renders without Import Apps button when it is SBOM Manager', () => {
      renderComponent(sbomManagerCreateAppState);
      expect(screen.queryByRole('link', { name: 'Import Apps' })).not.toBeInTheDocument();
      expect(screen.getByRole('button', { name: 'Cancel' })).toBeVisible();
      expect(screen.getByRole('button', { name: 'Create' })).toBeVisible();
    });

    it('renders modal with correct content', () => {
      renderComponent(createAppState);
      expect(screen.getByText(`Application Name`)).toBeVisible();
      expect(screen.getByText(`Application ID`)).toBeVisible();
      expect(screen.getByText(`Use a default icon`)).toBeVisible();
      expect(screen.getByText(`Upload a custom icon`)).toBeVisible();
      expect(screen.getByText(`Get a robot`)).toBeVisible();
      const importAppsButton = screen.getByRole('link', { name: 'Import Apps' });
      expect(importAppsButton).toBeVisible();
      expect(importAppsButton).toHaveAttribute('href', '#/onboarding/organizationOneID');

      expect(screen.getByRole('button', { name: 'Cancel' })).toBeVisible();
      expect(screen.getByRole('button', { name: 'Create' })).toBeVisible();
    });

    it('triggers createNewOwner', async () => {
      const newOwnerNameValue = 'qwerty';
      const newOwnerIdValue = '12345';
      mock.onPost(getApplicationsUrl()).reply(200, {
        id: 'applicationFourID',
        name: newOwnerNameValue,
        publicId: newOwnerIdValue,
        organizationId: 'organizationOneID',
      });
      renderComponent(createAppState);

      const ownerNameInput = screen.getAllByRole('textbox')[0];
      fireEvent.change(ownerNameInput, { target: { value: newOwnerNameValue } });

      expect(ownerNameInput.value).toBe(newOwnerNameValue);

      const ownerIdInput = screen.getAllByRole('textbox')[1];
      fireEvent.change(ownerIdInput, { target: { value: newOwnerIdValue } });

      expect(ownerIdInput.value).toBe(newOwnerIdValue);

      const submitButton = screen.getByRole('button', { name: 'Create' });
      expect(submitButton).toHaveTextContent('Create');
      expect(submitButton).not.toHaveClass('disabled');
      fireEvent.click(submitButton);
      await waitFor(() => expect(mock.history.post.length).toBe(1));
      expect(mock.history.put.length).toBe(0);
    });

    it('can not trigger createNewOwner when there are invalid characters in id input', () => {
      renderComponent(createAppState);

      const ownerIdInput = screen.getAllByRole('textbox')[1];
      fireEvent.change(ownerIdInput, { target: { value: 'some text' } });

      const submitButton = screen.getByRole('button', { name: 'Create' });
      fireEvent.click(submitButton);
      expect(mock.history.post.length).toBe(0);
      expect(mock.history.put.length).toBe(0);
      const errorText = screen.getByText('Use valid characters: alphanumeric, "_", "." or "-"');
      expect(errorText).toBeVisible();
    });

    it('can not trigger createNewOwner when there is duplicate application', () => {
      renderComponent(createAppState);

      const ownerIdInput = screen.getAllByRole('textbox')[1];
      fireEvent.change(ownerIdInput, { target: { value: APPS[0].publicId } });

      const submitButton = screen.getByRole('button', { name: 'Create' });

      expect(ownerIdInput.value).toBe(APPS[0].publicId);
      fireEvent.click(submitButton);
      expect(mock.history.post.length).toBe(0);
      expect(mock.history.put.length).toBe(0);

      const errorText = screen.getByText('ID is already in use');
      expect(errorText).toBeVisible();
    });

    it('can not trigger createNewOwner when there are more than 200 characters in id input', () => {
      renderComponent(createAppState);

      const ownerIdInput = screen.getAllByRole('textbox')[1];
      fireEvent.change(ownerIdInput, { target: { value: 'a'.repeat(201) } });
      const submitButton = screen.getByRole('button', { name: 'Create' });

      fireEvent.click(submitButton);
      expect(mock.history.post.length).toBe(0);
      expect(mock.history.put.length).toBe(0);
      const errorText = screen.getByText('Please enter less than 200 characters');
      expect(errorText).toBeVisible();
    });

    it('can not trigger createNewOwner when id input is empty', () => {
      renderComponent(createAppState);

      const errorText = 'Must be non-empty';
      const ownerIdInput = screen.getAllByRole('textbox')[1];
      fireEvent.change(ownerIdInput, { target: { value: 'someText' } });
      fireEvent.change(ownerIdInput, { target: { value: '' } });
      expect(screen.getByText(errorText)).toBeVisible();

      const submitButton = screen.getByRole('button', { name: 'Create' });
      expect(ownerIdInput.value).toBe('');

      fireEvent.click(submitButton);

      expect(mock.history.post.length).toBe(0);
      expect(mock.history.put.length).toBe(0);
      expect(screen.getAllByText(errorText).length).toBe(2);
    });
  });

  describe('Edit Organization', () => {
    it('renders modal with the correct page title', () => {
      renderComponent(editOrgState);

      expect(screen.getByText('Edit Organization')).toBeVisible();
    });

    it('renders the correct field label when it is SBOM Manager', () => {
      renderComponent(sbomManagerEditOrgState);
      expect(screen.getByText(`Organization Name`)).toBeVisible();
    });

    it('renders modal with correct content', () => {
      renderComponent(editOrgState);
      expect(screen.getByText(`Organization Name`)).toBeVisible();
      expect(screen.getByText(`Use a default icon`)).toBeVisible();
      expect(screen.getByText(`Upload a custom icon`)).toBeVisible();
      expect(screen.getByText(`Get a robot`)).toBeVisible();
      expect(screen.queryByRole('link', { name: 'Import Apps' })).not.toBeInTheDocument();

      const addingTo = screen.queryByRole('heading', { name: /Adding to:/i });
      expect(addingTo).not.toBeInTheDocument();

      expect(screen.getByRole('button', { name: 'Cancel' })).toBeVisible();
      expect(screen.getByText(`Update`)).toBeVisible();
    });

    it('triggers editCurrentOwner', async () => {
      const newOwnerNameValue = 'qwerty';

      mock.onPost(getAddIconUrl(false, 'organizationOneID')).reply(200, {});
      mock.onPut(getOrganizationsUrl()).reply(200, {
        id: 'organizationOneID',
        name: newOwnerNameValue,
      });
      renderComponent(editOrgState);

      const ownerNameInput = screen.getByRole('textbox');
      fireEvent.change(ownerNameInput, { target: { value: newOwnerNameValue } });

      expect(ownerNameInput.value).toBe(newOwnerNameValue);

      const submitButton = screen.getByRole('button', { name: 'Update' });
      expect(submitButton).toHaveTextContent('Update');
      expect(submitButton).not.toHaveClass('disabled');
      fireEvent.click(submitButton);
      await waitFor(() => expect(mock.history.put.length).toBe(1));
      expect(mock.history.post.length).toBe(1);
    });

    it('triggers editCurrentOwner on same value as was before in current name value, and request only icon change, not owner', async () => {
      const newOwnerNameValue = 'qwerty';
      const oldOwnerNameValue = 'OrganizationOneName';
      mock.onPost(getAddIconUrl(false, 'organizationOneID')).reply(200, {});
      renderComponent(editOrgState);

      const ownerNameInput = screen.getByRole('textbox');
      fireEvent.change(ownerNameInput, { target: { value: newOwnerNameValue } });
      fireEvent.change(ownerNameInput, { target: { value: oldOwnerNameValue } });

      expect(ownerNameInput.value).toBe(oldOwnerNameValue);

      const submitButton = screen.getByRole('button', { name: 'Update' });
      expect(submitButton).toHaveTextContent('Update');
      expect(submitButton).not.toHaveClass('disabled');
      fireEvent.click(submitButton);
      expect(mock.history.put.length).toBe(0);
      await waitFor(() => expect(mock.history.post.length).toBe(1));
    });

    it('can not trigger editCurrentOwner when there is duplicate organization', () => {
      renderComponent(editOrgState);

      const ownerNameInput = screen.getByRole('textbox');
      fireEvent.change(ownerNameInput, { target: { value: ORGS[1].name } });

      const submitButton = screen.getByRole('button', { name: 'Update' });
      expect(ownerNameInput.value).toBe(ORGS[1].name);
      fireEvent.click(submitButton);
      expect(mock.history.post.length).toBe(0);
      expect(mock.history.put.length).toBe(0);
      const errorText = screen.getByText('Name is already in use');
      expect(errorText).toBeVisible();
    });

    it('can not trigger editCurrentOwner when name input is empty', () => {
      renderComponent(editOrgState);

      const ownerNameInput = screen.getByRole('textbox');
      fireEvent.change(ownerNameInput, { target: { value: '' } });

      expect(ownerNameInput.value).toBe('');

      const submitButton = screen.getByRole('button', { name: 'Update' });
      fireEvent.click(submitButton);
      expect(mock.history.post.length).toBe(0);
      expect(mock.history.put.length).toBe(0);
      const errorText = screen.getByText('Must be non-empty');
      expect(errorText).toBeVisible();
    });

    it('can not trigger editCurrentOwner when custom file input is empty', () => {
      renderComponent(editOrgState);

      const ownerIconRadio = screen.getByRole('radio', { name: 'Upload a custom icon' });
      fireEvent.click(ownerIconRadio);

      const submitButton = screen.getByRole('button', { name: 'Update' });
      expect(ownerIconRadio).toBeChecked();

      fireEvent.click(submitButton);

      expect(mock.history.post.length).toBe(0);
      expect(mock.history.put.length).toBe(0);
      const errorText = screen.getByText('No file selected');
      expect(errorText).toBeVisible();
    });
  });

  describe('Edit Application', () => {
    it('renders modal with the correct page title', () => {
      renderComponent(editAppState);

      expect(screen.getByText('Edit Application')).toBeVisible();
    });

    it('renders modal with correct content', () => {
      renderComponent(editAppState);
      expect(screen.getByText(`Application Name`)).toBeVisible();
      expect(screen.queryAllByText(`Application ID`).length).toBe(0);
      expect(screen.getByText(`Use a default icon`)).toBeVisible();
      expect(screen.getByText(`Upload a custom icon`)).toBeVisible();
      expect(screen.getByText(`Get a robot`)).toBeVisible();
      expect(screen.queryByRole('link', { name: 'Import Apps' })).not.toBeInTheDocument();

      const addingTo = screen.queryByRole('heading', { name: /Adding to:/i });
      expect(addingTo).not.toBeInTheDocument();

      expect(screen.getByRole('button', { name: 'Cancel' })).toBeVisible();
      expect(screen.getByText(`Update`)).toBeVisible();
    });

    it('triggers editCurrentOwner', async () => {
      const newOwnerNameValue = 'qwerty';
      mock.onPost(getAddIconUrl(true, 'applicationOneID')).reply(200, {});
      mock.onPut(getApplicationsUrl()).reply(200, {
        id: 'applicationOneID',
        name: newOwnerNameValue,
        publicId: 'applicationOnePublicID',
        organizationId: 'organizationOneID',
      });
      renderComponent(editAppState);

      const ownerNameInput = screen.getByRole('textbox');
      fireEvent.change(ownerNameInput, { target: { value: newOwnerNameValue } });

      expect(ownerNameInput.value).toBe(newOwnerNameValue);

      const submitButton = screen.getByRole('button', { name: 'Update' });

      fireEvent.click(submitButton);
      await waitFor(() => expect(mock.history.put.length).toBe(1));
      expect(mock.history.post.length).toBe(1);
    });

    it('triggers editCurrentOwner on same value as was before in current name value, and request only icon change, not owner', async () => {
      const newOwnerNameValue = 'qwerty';
      const oldOwnerNameValue = 'ApplicationOneName';
      mock.onPost(getAddIconUrl(true, 'applicationOneID')).reply(200, {});
      renderComponent(editAppState);

      const ownerNameInput = screen.getByRole('textbox');
      fireEvent.change(ownerNameInput, { target: { value: newOwnerNameValue } });
      fireEvent.change(ownerNameInput, { target: { value: oldOwnerNameValue } });

      expect(ownerNameInput.value).toBe(oldOwnerNameValue);

      const submitButton = screen.getByRole('button', { name: 'Update' });
      fireEvent.click(submitButton);
      expect(mock.history.put.length).toBe(0);
      await waitFor(() => expect(mock.history.post.length).toBe(1));
    });

    it('can not trigger editCurrentOwner when there is duplicate application', () => {
      renderComponent(editAppState);

      const ownerNameInput = screen.getByRole('textbox');
      fireEvent.change(ownerNameInput, { target: { value: APPS[1].name } });

      const submitButton = screen.getByRole('button', { name: 'Update' });

      expect(ownerNameInput.value).toBe(APPS[1].name);
      fireEvent.click(submitButton);
      expect(mock.history.post.length).toBe(0);
      expect(mock.history.put.length).toBe(0);

      const errorText = screen.getByText('Name is already in use');
      expect(errorText).toBeVisible();
    });
  });
});
