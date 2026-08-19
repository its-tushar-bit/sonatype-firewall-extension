/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import SelectContactModal from 'MainRoot/OrgsAndPolicies/selectContactModal/SelectContactModal';
import { render, screen, axiosMockAdapter } from 'TestRoot/SpecUtil';
import { getUsersRoleMappingUrl } from 'MainRoot/util/CLMLocation';
import { fireEvent, waitFor, within } from '@testing-library/react';
import { NX_STANDARD_DEBOUNCE_TIME } from '@sonatype/react-shared-components';

import 'TestRoot/SpecUtil';

xdescribe('SelectContactModal', () => {
  let renderComponent, axiosMock;

  beforeAll(() => {
    axiosMock = axiosMockAdapter();
  });

  beforeEach(() => {
    const defaultPreloadedState = {
      router: {
        currentParams: { '#': null, applicationPublicId: '4' },
      },
      orgsAndPolicies: {
        root: {
          selectedOwner: {
            contact: null,
            id: 'a284da8572fd4e4eb2097cab965560aa',
            name: '4App',
            organizationId: 'cb53d63023c44a429d3d539821bdbd06',
            organizationName: '4 org',
            publicId: '4',
          },
        },
        ownerActions: {
          contact: {
            selectedUser: null,
            query: '',
            fetchedUsers: { data: [], loading: false, loadError: null, partialError: null },
            isContactModalOpen: true,
            isDirty: false,
            loadError: null,
            loading: false,
            submitError: null,
            submitMaskState: null,
          },
        },
      },
    };

    renderComponent = (preloadedState) =>
      render(<SelectContactModal />, { preloadedState: preloadedState || defaultPreloadedState });
  });

  it('doesn"t show modal without being open', () => {
    renderComponent({
      orgsAndPolicies: {
        ownerActions: {
          contact: {
            fetchedUsers: { data: [], loading: false, loadError: null, partialError: null },
            isContactModalOpen: false,
          },
        },
      },
    });
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
  });

  it('shows modal with the correct title', () => {
    renderComponent();
    const contactModalTitle = screen.getByText('Select Contact');
    expect(contactModalTitle).toBeVisible();
  });

  it("didn't fetch all available users, if the user didn't provide any actions", () => {
    const usersRequestSpy = jest.fn(() => [200, { users: [{ id: 1, name: 'John Smith' }] }]);
    axiosMock.onGet('/users').reply(usersRequestSpy);
    renderComponent();
    expect(usersRequestSpy).not.toHaveBeenCalled();
  });

  it('shows warning message, if there are no available users', (done) => {
    axiosMock.onGet(getUsersRoleMappingUrl('application', '4', '1*', false)).reply(200, {
      members: [],
      error: null,
      query: '1',
    });
    renderComponent({
      router: {
        currentParams: { '#': null, applicationPublicId: '4' },
      },
      orgsAndPolicies: {
        root: {
          selectedOwner: {
            contact: null,
            id: 'a284da8572fd4e4eb2097cab965560aa',
            name: '4App',
            organizationId: 'cb53d63023c44a429d3d539821bdbd06',
            organizationName: '4 org',
            publicId: '4',
          },
        },
        ownerActions: {
          contact: {
            selectedUser: null,
            query: '',
            fetchedUsers: {
              data: [],
              loading: false,
              loadError: null,
              partialError: null,
            },
            isContactModalOpen: true,
            isDirty: false,
            loadError: null,
            loading: false,
            submitError: null,
            submitMaskState: null,
          },
        },
      },
    });
    const combobox = screen.getByRole('combobox');
    fireEvent.change(combobox, { target: { value: '1' } });
    setTimeout(() => {
      const alert = screen.getAllByRole('alert');
      const message = within(alert[0]).getByText('No Results Found');
      expect(message).toBeVisible();
      done();
    }, NX_STANDARD_DEBOUNCE_TIME);
  });

  it('set partialError when there is an error', (done) => {
    axiosMock.onGet(getUsersRoleMappingUrl('application', '4', '1*', false)).reply(200, {
      members: [
        {
          type: 'USER',
          internalName: '12',
          displayName: '12 12',
          email: 'my@gmail.com',
          realm: 'IQ Server',
        },
      ],
      error: 'partialError message',
      query: '1',
    });
    renderComponent({
      router: {
        currentParams: { '#': null, applicationPublicId: '4' },
      },
      orgsAndPolicies: {
        root: {
          selectedOwner: {
            contact: null,
            id: 'a284da8572fd4e4eb2097cab965560aa',
            name: '4App',
            organizationId: 'cb53d63023c44a429d3d539821bdbd06',
            organizationName: '4 org',
            publicId: '4',
          },
        },
        ownerActions: {
          contact: {
            selectedUser: null,
            query: '',
            fetchedUsers: {
              data: [],
              loading: false,
              loadError: null,
              partialError: null,
            },
            isContactModalOpen: true,
            isDirty: false,
            loadError: null,
            loading: false,
            submitError: null,
            submitMaskState: null,
          },
        },
      },
    });
    const combobox = screen.getByRole('combobox');
    fireEvent.change(combobox, { target: { value: '1' } });
    setTimeout(() => {
      const [alert] = screen.getAllByRole('alert');
      const partialErrorMessage = within(alert).getByText('partialError message');
      expect(partialErrorMessage).toBeVisible();
      done();
    }, NX_STANDARD_DEBOUNCE_TIME);
  });

  it('shows Submit and Cancel buttons', async () => {
    renderComponent();
    const submitButton = await screen.findByRole('button', { name: 'Save' }),
      cancelButton = await screen.findByRole('button', { name: 'Cancel' });

    expect(submitButton).toBeVisible();
    expect(cancelButton).toBeVisible();
  });

  it('saveContact successfully', (done) => {
    axiosMock.onGet(getUsersRoleMappingUrl('application', '4', '12 12*', false)).reply(200, {
      members: [
        {
          type: 'USER',
          internalName: '12',
          displayName: '12 12',
          email: 'my@gmail.com',
          realm: 'IQ Server',
        },
      ],
      error: null,
      query: '12 12*',
    });
    renderComponent({
      router: {
        currentParams: { '#': null, applicationPublicId: '4' },
      },
      orgsAndPolicies: {
        root: {
          selectedOwner: {
            contact: null,
            id: 'a284da8572fd4e4eb2097cab965560aa',
            name: '4App',
            organizationId: 'cb53d63023c44a429d3d539821bdbd06',
            organizationName: '4 org',
            publicId: '4',
          },
        },
        ownerActions: {
          contact: {
            selectedUser: null,
            query: '',
            fetchedUsers: {
              data: [],
              loading: false,
              loadError: null,
              partialError: null,
            },
            isContactModalOpen: true,
            isDirty: false,
            loadError: null,
            loading: false,
            submitError: null,
            submitMaskState: null,
          },
        },
      },
    });
    const combobox = screen.getByRole('combobox');
    fireEvent.change(combobox, { target: { value: '12 12' } });

    // Use a timeout to test debounced function call
    setTimeout(() => {
      const listOfCombobox = screen.getByRole('listbox');
      expect(listOfCombobox).toBeVisible();
      const selectedContact = within(listOfCombobox).getByRole('option', { name: '12 12' });
      fireEvent.click(selectedContact);
      const saveButton = screen.getByRole('button', { name: 'Save' });
      expect(saveButton).not.toHaveClass('disabled');
      expect(saveButton).toBeVisible();
      fireEvent.click(saveButton);
      expect(axiosMock.history.put.length).toBe(1);
      done();
    }, NX_STANDARD_DEBOUNCE_TIME);
  });

  it('removeContact successfully', async () => {
    renderComponent({
      router: {
        currentParams: { '#': null, applicationPublicId: '4' },
      },
      orgsAndPolicies: {
        root: {
          selectedOwner: {
            contact: {
              internalName: 'ad',
              displayName: 'admin second',
              email: 'my@gmaim.com',
              realm: 'IQ Server',
              error: null,
            },
            id: 'a284',
            name: '44App',
            organizationId: 'cb53',
            organizationName: '4 org',
            publicId: '4',
          },
        },
        ownerActions: {
          contact: {
            selectedContact: {
              displayName: 'admin second',
              email: 'my@gmaim.com',
              error: null,
              internalName: 'ad',
              realm: 'IQ Server',
            },
            fetchedUsers: {
              data: [
                {
                  displayName: 'admin second',
                  email: 'my@gmaim.com',
                  error: null,
                  id: 'adundefined',
                  internalName: 'ad',
                  realm: 'IQ Server',
                },
              ],
              loading: false,
              loadError: null,
              partialError: null,
            },
            savedContact: {
              displayName: 'admin second',
              email: 'my@gmaim.com',
              error: null,
              internalName: 'ad',
              realm: 'IQ Server',
            },
            isContactModalOpen: true,
            isDirty: false,
            loadError: null,
            submitError: null,
            submitMaskState: null,
            query: 'admin second',
          },
        },
      },
    });
    const combobox = screen.getByRole('combobox');
    fireEvent.click(combobox);
    fireEvent.change(combobox, { target: { value: '' } });
    const [cancelButton, saveButton] = screen.getAllByRole('button');
    expect(saveButton).toBeVisible();
    expect(cancelButton).toBeVisible();
    expect(saveButton).not.toHaveClass('disabled');
    fireEvent.click(saveButton);
    await waitFor(() => expect(axiosMock.history.put.length).toBe(1));
  });

  it('closes modal when clicking on "Cancel" Button', () => {
    renderComponent();
    const cancelButton = screen.getByRole('button', { name: 'Cancel' });
    fireEvent.click(cancelButton);
    const contactTitle = screen.queryByText('Select Contact');
    expect(contactTitle).toBeNull();
  });

  describe('check appearing UnsavedChangesModal when clicking on "Cancel" Button', () => {
    it('no already-saved contact, and the user has entered text into the combobox', () => {
      renderComponent();
      const combobox = screen.getByRole('combobox');
      fireEvent.change(combobox, { target: { value: '12 12' } });
      const cancelButton = screen.getByRole('button', { name: 'Cancel' });
      fireEvent.click(cancelButton);
      const unsavedChangesModal = screen.getByText('Unsaved Changes');
      expect(unsavedChangesModal).toBeInTheDocument();
      const continueButton = screen.getByRole('button', { name: 'Continue' });
      fireEvent.click(continueButton);
      const contactTitle = screen.queryByText('Select Contact');
      expect(contactTitle).toBeNull();
    });

    it('no already-saved contact, and the user has selected a contact in the combobox dropdown', (done) => {
      // it('saveContact successfully', (done) => {
      axiosMock.onGet(getUsersRoleMappingUrl('application', '4', '12 12*', false)).reply(200, {
        members: [
          {
            type: 'USER',
            internalName: '12',
            displayName: '12 12',
            email: 'my@gmail.com',
            realm: 'IQ Server',
          },
        ],
        error: null,
        query: '12 12*',
      });
      renderComponent({
        router: {
          currentParams: { '#': null, applicationPublicId: '4' },
        },
        orgsAndPolicies: {
          root: {
            selectedOwner: {
              contact: null,
              id: 'a284da8572fd4e4eb2097cab965560aa',
              name: '4App',
              organizationId: 'cb53d63023c44a429d3d539821bdbd06',
              organizationName: '4 org',
              publicId: '4',
            },
          },
          ownerActions: {
            contact: {
              selectedUser: null,
              query: '',
              fetchedUsers: {
                data: [],
                loading: false,
                loadError: null,
                partialError: null,
              },
              isContactModalOpen: true,
              isDirty: false,
              loadError: null,
              loading: false,
              submitError: null,
              submitMaskState: null,
            },
          },
        },
      });
      const combobox = screen.getByRole('combobox');
      fireEvent.change(combobox, { target: { value: '12 12' } });

      // Use a timeout to test debounced function call
      setTimeout(() => {
        const listOfCombobox = screen.getByRole('listbox');
        expect(listOfCombobox).toBeVisible();
        const selectedContact = within(listOfCombobox).getByRole('option', { name: '12 12' });
        fireEvent.click(selectedContact);
        const cancelButton = screen.getByRole('button', { name: 'Cancel' });
        fireEvent.click(cancelButton);
        const unsavedChangesModal = screen.getByText('Unsaved Changes');
        expect(unsavedChangesModal).toBeInTheDocument();
        const continueButton = screen.getByRole('button', { name: 'Continue' });
        fireEvent.click(continueButton);
        const contactTitle = screen.queryByText('Select Contact');
        expect(contactTitle).toBeNull();
        done();
      }, NX_STANDARD_DEBOUNCE_TIME);
    });

    it('There is an already-saved contact, but the user has selected a different one in the combobox dropdown', (done) => {
      axiosMock.onGet(getUsersRoleMappingUrl('application', '4', '12 12*', false)).reply(200, {
        members: [
          {
            type: 'USER',
            internalName: '12',
            displayName: '12 12',
            email: 'my@gmail.com',
            realm: 'IQ Server',
          },
        ],
        error: null,
        query: '12 12*',
      });
      renderComponent({
        router: {
          currentParams: { '#': null, applicationPublicId: '4' },
        },
        orgsAndPolicies: {
          root: {
            selectedOwner: {
              contact: {
                internalName: 'ad',
                displayName: 'admin second',
                email: 'my@gmaim.com',
                realm: 'IQ Server',
                error: null,
              },
              id: 'a284',
              name: '44App',
              organizationId: 'cb53',
              organizationName: '4 org',
              publicId: '4',
            },
          },
          ownerActions: {
            contact: {
              selectedContact: {
                displayName: 'admin second',
                email: 'my@gmaim.com',
                error: null,
                internalName: 'ad',
                realm: 'IQ Server',
              },
              fetchedUsers: {
                data: [
                  {
                    displayName: 'admin second',
                    email: 'my@gmaim.com',
                    type: 'USER',
                    internalName: 'ad',
                    realm: 'IQ Server',
                    id: 'adundefined',
                  },
                  {
                    displayName: '12 12',
                    email: 'my@gmail.com',
                    type: 'USER',
                    internalName: '12',
                    realm: 'IQ Server',
                    id: '12USER',
                  },
                ],
                loading: false,
                loadError: null,
                partialError: null,
              },
              savedContact: {
                displayName: 'admin second',
                email: 'my@gmaim.com',
                error: null,
                internalName: 'ad',
                realm: 'IQ Server',
              },
              isContactModalOpen: true,
              isDirty: false,
              loadError: null,
              submitError: null,
              submitMaskState: null,
              query: 'admin second',
            },
          },
        },
      });
      const combobox = screen.getByRole('combobox');
      fireEvent.click(combobox);
      fireEvent.change(combobox, { target: { value: '12 12' } });

      // Use a timeout to test debounced function call
      setTimeout(() => {
        const listOfCombobox = screen.getByRole('listbox');
        expect(listOfCombobox).toBeVisible();
        const selectedContact = within(listOfCombobox).getByRole('option', { name: '12 12' });
        fireEvent.click(selectedContact);
        const cancelButton = screen.getByRole('button', { name: 'Cancel' });
        fireEvent.click(cancelButton);
        const unsavedChangesModal = screen.getByText('Unsaved Changes');
        expect(unsavedChangesModal).toBeInTheDocument();
        const continueButton = screen.getByRole('button', { name: 'Continue' });
        fireEvent.click(continueButton);
        const contactTitle = screen.queryByText('Select Contact');
        expect(contactTitle).toBeNull();
        done();
      }, NX_STANDARD_DEBOUNCE_TIME);
    });

    it('there is an already-saved contact, and the user has cleared the text in the dropdown', async () => {
      renderComponent({
        router: {
          currentParams: { '#': null, applicationPublicId: '4' },
        },
        orgsAndPolicies: {
          root: {
            selectedOwner: {
              contact: {
                internalName: 'ad',
                displayName: 'admin second',
                email: 'my@gmaim.com',
                realm: 'IQ Server',
                error: null,
              },
              id: 'a284',
              name: '44App',
              organizationId: 'cb53',
              organizationName: '4 org',
              publicId: '4',
            },
          },
          ownerActions: {
            contact: {
              selectedContact: {
                displayName: 'admin second',
                email: 'my@gmaim.com',
                error: null,
                internalName: 'ad',
                realm: 'IQ Server',
              },
              fetchedUsers: {
                data: [
                  {
                    displayName: 'admin second',
                    email: 'my@gmaim.com',
                    error: null,
                    id: 'adundefined',
                    internalName: 'ad',
                    realm: 'IQ Server',
                  },
                ],
                loading: false,
                loadError: null,
                partialError: null,
              },
              savedContact: {
                displayName: 'admin second',
                email: 'my@gmaim.com',
                error: null,
                internalName: 'ad',
                realm: 'IQ Server',
              },
              isContactModalOpen: true,
              isDirty: false,
              loadError: null,
              submitError: null,
              submitMaskState: null,
              query: 'admin second',
            },
          },
        },
      });
      const combobox = screen.getByRole('combobox');
      fireEvent.click(combobox);
      fireEvent.change(combobox, { target: { value: '' } });
      const cancelButton = screen.getByRole('button', { name: 'Cancel' });
      expect(cancelButton).toBeVisible();
      fireEvent.click(cancelButton);
      const unsavedChangesModal = screen.getByText('Unsaved Changes');
      expect(unsavedChangesModal).toBeInTheDocument();
      const continueButton = screen.getByRole('button', { name: 'Continue' });
      fireEvent.click(continueButton);
      const contactTitle = screen.queryByText('Select Contact');
      expect(contactTitle).toBeNull();
    });
  });

  it('renders error message, retry and ok button when loading orgs fails', (done) => {
    axiosMock.onGet(getUsersRoleMappingUrl('application', '4', '1*', false)).reply(500, 'Error Messages');
    renderComponent({
      router: {
        currentParams: { '#': null, applicationPublicId: '4' },
      },
      orgsAndPolicies: {
        root: {
          selectedOwner: {
            contact: null,
            id: 'a284da8572fd4e4eb2097cab965560aa',
            name: '4App',
            organizationId: 'cb53d63023c44a429d3d539821bdbd06',
            organizationName: '4 org',
            publicId: '4',
          },
        },
        ownerActions: {
          contact: {
            selectedUser: null,
            query: '',
            fetchedUsers: {
              data: [],
              loading: false,
              loadError: null,
              partialError: null,
            },
            isContactModalOpen: true,
            isDirty: false,
            loadError: null,
            loading: false,
            submitError: null,
            submitMaskState: null,
          },
        },
      },
    });

    // Use a timeout to test debounced function call
    const combobox = screen.getByRole('combobox');
    fireEvent.change(combobox, { target: { value: '1' } });
    setTimeout(() => {
      expect(axiosMock.history.get.length).toBe(1);
      const alerts = screen.getAllByRole('alert');
      const message = within(alerts[0]).getByText('An error occurred loading data. Error Messages');
      expect(message).toBeVisible();
      done();
    }, NX_STANDARD_DEBOUNCE_TIME);
  });
});
