/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import InnerSourceRepositoryBaseConfigurations from 'MainRoot/innerSourceRepositoryConfiguration/InnerSourceRepositoryBaseConfigurations';
import { render, screen, fireEvent, within, axiosMockAdapter } from 'TestRoot/SpecUtil';
import { getRepositoryConnectionUrl } from 'MainRoot/util/CLMLocation';
import {
  actions,
  MUST_UPDATE_ENABLED_ADD_MESSAGE,
  MUST_UPDATE_ENABLED_EDIT_MESSAGE,
  PARENT_ORGANIZATIONS_MUST_ALLOW_OVERRIDE,
} from 'MainRoot/innerSourceRepositoryConfiguration/innerSourceRepositoryBaseConfigurationsSlice';

describe('InnerSourceRepositoryBaseConfigurations', () => {
  let axiosMock, preloadedState;
  const ownerId = 'e270271429f747ef9bebf4ca88f5e6c0';
  const ownerType = 'organization';

  const renderComponent = (preloadedState) => render(<InnerSourceRepositoryBaseConfigurations />, { preloadedState });

  beforeAll(() => {
    axiosMock = axiosMockAdapter();
  });

  beforeEach(() => {
    spyOn(actions, 'load').and.callThrough();
    spyOn(actions, 'save').and.callThrough();
    spyOn(actions, 'cancel').and.callThrough();

    preloadedState = {
      orgsAndPolicies: {
        root: {
          selectedOwner: {
            id: ownerId,
            name: 'broadcast',
          },
        },
      },
      router: {
        currentState: {
          name: 'management.view.organization',
          url: '/organization/{organizationId}',
          data: {
            title: 'Organization Management',
            viewportSized: true,
          },
        },
        currentParams: {
          organizationId: ownerId,
        },
      },
    };

    axiosMock.onGet(getRepositoryConnectionUrl(ownerType, ownerId, null)).reply(200, {
      repositoryConnectionStatus: {
        allowChange: true,
        allowOverride: true,
        enabled: null,
        inheritedFromOrgEnabled: false,
        inheritedFromOrganizationId: 'ROOT_ORGANIZATION_ID',
        inheritedFromOrganizationName: 'Root Organization',
      },
    });
    axiosMock.onPut(getRepositoryConnectionUrl(ownerType, ownerId, null)).reply(200, {});
  });

  it('renders a loading indicator', () => {
    renderComponent(preloadedState);
    expect(screen.getByText('Loading…')).toBeVisible();
  });

  it('renders load error', async () => {
    axiosMock.onGet(getRepositoryConnectionUrl(ownerType, ownerId, null)).reply(502);
    renderComponent(preloadedState);

    let failureAlert = await screen.findByRole('alert');
    expect(failureAlert).toBeVisible();
    expect(failureAlert).toHaveTextContent('An error occurred loading data. Bad Gateway');
  });

  describe('saving', () => {
    beforeEach(() => {
      axiosMock.onGet(getRepositoryConnectionUrl(ownerType, ownerId, null)).reply(200, {
        repositoryConnections: [],
        ownerDTO: {
          ownerId,
        },
        repositoryConnectionStatus: {
          allowChange: true,
          allowOverride: true,
          enabled: null,
          inheritedFromOrgEnabled: false,
          inheritedFromOrganizationId: 'ROOT_ORGANIZATION_ID',
          inheritedFromOrganizationName: 'Root Organization',
        },
      });
    });

    it('disables the update button if no changes have been made', async () => {
      SpecUtil.requestIdleCallbackInvokeImmediate();

      renderComponent(preloadedState);

      const updateButton = await screen.findByText('Update');
      expect(updateButton).toBeVisible();

      fireEvent.click(updateButton);
      const alert = await screen.findByRole('alert');
      expect(alert).toHaveTextContent('There were validation errors. No changes have been made.');
    });

    it('renders an alert with retry button if something goes wrong on save', async () => {
      axiosMock.onPut(getRepositoryConnectionUrl(ownerType, ownerId, null)).reply(502);

      renderComponent(preloadedState);

      const radios = await screen.findAllByRole('radio');
      const disableRadio = radios[1];

      fireEvent.click(disableRadio);
      expect(disableRadio).toBeEnabled();

      let updateBtn = await screen.findByText('Update');
      expect(updateBtn).not.toHaveClassName('disabled');
      fireEvent.click(updateBtn);

      let failureAlert = await screen.findByRole('alert');
      expect(failureAlert).toBeVisible();
      expect(failureAlert).toHaveTextContent('An error occurred saving data. Bad Gateway');

      let retryButton = await within(failureAlert).findByRole('button');

      expect(retryButton).toBeVisible();
      fireEvent.click(retryButton);

      failureAlert = await screen.findByRole('alert');
      expect(failureAlert).toBeVisible();
      expect(failureAlert).toHaveTextContent('An error occurred saving data. Bad Gateway');
    });
  });

  describe('configuration load', () => {
    describe('actions buttons', () => {
      beforeEach(() => {
        axiosMock.onGet(getRepositoryConnectionUrl(ownerType, ownerId, null)).reply(200, {
          repositoryConnectionStatus: {
            allowChange: true,
            allowOverride: true,
            enabled: true,
            inheritedFromOrgEnabled: true,
            inheritedFromOrganizationId: 'ROOT_ORGANIZATION_ID',
            inheritedFromOrganizationName: 'someOrganizationName',
          },
        });
      });

      it('renders cancel button', async () => {
        renderComponent(preloadedState);
        let cancelButton = await screen.findByText('Cancel');
        expect(cancelButton).toBeVisible();
        expect(cancelButton).toBeDisabled();

        const allowOverride = await screen.findByLabelText('Allow Override');
        fireEvent.click(allowOverride);

        cancelButton = await screen.findByText('Cancel');
        expect(cancelButton).toBeVisible();
        expect(cancelButton).not.toBeDisabled();
      });

      it('renders update button', async () => {
        SpecUtil.requestIdleCallbackInvokeImmediate();

        renderComponent(preloadedState);

        let updateButton = await screen.findByText('Update');
        expect(updateButton).toBeVisible();
        fireEvent.click(updateButton);
        const alert = await screen.findByRole('alert');
        expect(alert).toHaveTextContent('There were validation errors. No changes have been made.');

        const allowOverride = await screen.findByLabelText('Allow Override');
        fireEvent.click(allowOverride);

        updateButton = await screen.findByText('Update');

        expect(updateButton).toBeVisible();
        expect(updateButton).not.toHaveClassName('disabled');
      });
    });

    describe('back button behavior', () => {
      it('has the correct back link for an org', async () => {
        axiosMock.onGet(getRepositoryConnectionUrl(ownerType, ownerId, null)).reply(200, {
          repositoryConnectionStatus: {
            allowChange: true,
            allowOverride: true,
            enabled: null,
            inheritedFromOrgEnabled: true,
            inheritedFromOrganizationId: 'ROOT_ORGANIZATION_ID',
            inheritedFromOrganizationName: 'someOrganizationName',
          },
        });

        renderComponent(preloadedState);
        const back = await screen.findByText('Back');

        expect(back.closest('a').href.split('#')[1]).toEqual(
          '/management/view/organization/e270271429f747ef9bebf4ca88f5e6c0'
        );
      });

      it('has the correct back link for an app', async () => {
        preloadedState = {
          orgsAndPolicies: {
            root: {
              selectedOwner: {
                id: ownerId,
                name: 'broadcast',
                publicId: 'appPublicId',
              },
            },
          },
          router: {
            currentState: {
              name: 'management.view.application',
              url: '/application/{applicationId}',
              data: {
                title: 'Application Management',
                viewportSized: true,
              },
            },
            currentParams: {
              applicationPublicId: 'appPublicId',
              applicationId: ownerId,
            },
          },
        };
        axiosMock.onGet(getRepositoryConnectionUrl('application', ownerId, null)).reply(200, {
          ownerDTO: {
            ownerPublicId: 'appPublicId',
            ownerId,
            ownerName: 'appPublicId',
            ownerType: 'APPLICATION',
          },
          repositoryConnectionStatus: {
            allowChange: true,
            allowOverride: true,
            enabled: null,
            inheritedFromOrgEnabled: true,
            inheritedFromOrganizationId: 'ROOT_ORGANIZATION_ID',
            inheritedFromOrganizationName: 'someOrganizationName',
          },
        });

        renderComponent(preloadedState);
        const back = await screen.findByText('Back');

        expect(back.closest('a').href.split('#')[1]).toEqual('/management/view/application/appPublicId');
      });
    });

    it('renders repository status as enabled and inherited', async () => {
      axiosMock.onGet(getRepositoryConnectionUrl(ownerType, ownerId, null)).reply(200, {
        repositoryConnectionStatus: {
          allowChange: true,
          allowOverride: true,
          enabled: null,
          inheritedFromOrgEnabled: true,
          inheritedFromOrganizationId: 'ROOT_ORGANIZATION_ID',
          inheritedFromOrganizationName: 'someOrganizationName',
        },
      });

      renderComponent(preloadedState);
      const status = await screen.findByText('Enabled (inherited from someOrganizationName)');
      expect(status).toBeVisible();
    });

    it('renders repository status as disabled and inherited', async () => {
      axiosMock.onGet(getRepositoryConnectionUrl(ownerType, ownerId, null)).reply(200, {
        repositoryConnectionStatus: {
          allowChange: true,
          allowOverride: true,
          enabled: null,
          inheritedFromOrgEnabled: false,
          inheritedFromOrganizationId: 'ROOT_ORGANIZATION_ID',
          inheritedFromOrganizationName: 'someOrganizationName',
        },
      });

      renderComponent(preloadedState);
      const status = await screen.findByText('Disabled (inherited from someOrganizationName)');
      expect(status).toBeVisible();
    });

    it('renders repository status as enabled and not inherited', async () => {
      axiosMock.onGet(getRepositoryConnectionUrl(ownerType, ownerId, null)).reply(200, {
        repositoryConnectionStatus: {
          allowChange: true,
          allowOverride: true,
          enabled: true,
          inheritedFromOrgEnabled: null,
          inheritedFromOrganizationId: null,
          inheritedFromOrganizationName: null,
        },
      });

      renderComponent(preloadedState);
      const status = await screen.findByText('Enabled');
      expect(status).toBeVisible();
    });

    it('renders repository status as disabled and not inherited', async () => {
      axiosMock.onGet(getRepositoryConnectionUrl(ownerType, ownerId, null)).reply(200, {
        repositoryConnectionStatus: {
          allowChange: true,
          allowOverride: true,
          enabled: false,
          inheritedFromOrgEnabled: null,
          inheritedFromOrganizationId: null,
          inheritedFromOrganizationName: null,
        },
      });

      renderComponent(preloadedState);
      const status = await screen.findByText('Disabled');
      expect(status).toBeVisible();
    });

    it('renders allow override checkbox correctly', async () => {
      axiosMock.onGet(getRepositoryConnectionUrl(ownerType, ownerId, null)).reply(200, {
        repositoryConnectionStatus: {
          allowChange: true,
          allowOverride: true,
          enabled: null,
          inheritedFromOrgEnabled: true,
          inheritedFromOrganizationId: 'ROOT_ORGANIZATION_ID',
          inheritedFromOrganizationName: 'someOrganizationName',
        },
      });

      renderComponent(preloadedState);
      let allowOverride = await screen.findByLabelText('Allow Override');
      expect(allowOverride).toBeChecked();

      fireEvent.click(allowOverride);

      expect(await screen.findByLabelText('Allow Override')).not.toBeChecked();
    });

    it('renders repository connections options', async () => {
      axiosMock.onGet(getRepositoryConnectionUrl(ownerType, ownerId, null)).reply(200, {
        repositoryConnectionStatus: {
          allowChange: true,
          allowOverride: true,
          enabled: null,
          inheritedFromOrgEnabled: true,
          inheritedFromOrganizationId: 'ROOT_ORGANIZATION_ID',
          inheritedFromOrganizationName: 'someOrganizationName',
        },
      });

      renderComponent(preloadedState);

      expect(await screen.findByLabelText('Inherit')).toBeChecked();
      const disable = await screen.findByLabelText('Disable');
      expect(disable).not.toBeChecked();
      const enable = await screen.findByLabelText('Enable and Override Repository Connections');
      expect(enable).not.toBeChecked();

      expect(screen.queryByText('LOCAL')).not.toBeInTheDocument();
      expect(screen.queryByText('Add a Repository')).not.toBeInTheDocument();
      expect(screen.queryByText('No InnerSource repository connections are configured')).not.toBeInTheDocument();

      fireEvent.click(disable);

      expect(await screen.findByLabelText('Inherit')).not.toBeChecked();
      expect(await screen.findByLabelText('Disable')).toBeChecked();
      expect(await screen.findByLabelText('Enable and Override Repository Connections')).not.toBeChecked();

      expect(screen.queryByText('LOCAL')).not.toBeInTheDocument();
      expect(screen.queryByText('Add a Repository')).not.toBeInTheDocument();
      expect(screen.queryByText('No InnerSource repository connections are configured')).not.toBeInTheDocument();

      fireEvent.click(enable);

      expect(await screen.findByLabelText('Inherit')).not.toBeChecked();
      expect(await screen.findByLabelText('Disable')).not.toBeChecked();
      expect(await screen.findByLabelText('Enable and Override Repository Connections')).toBeChecked();

      expect(await screen.findByText('LOCAL')).toBeVisible();
      expect(await screen.findByText('Add a Repository')).toBeVisible();
      expect(await screen.findByText('No InnerSource repository connections are configured')).toBeVisible();
    });

    it('renders an info alert and disables the checkbox and radio buttons if `allowChange` is false', async () => {
      axiosMock.onGet(getRepositoryConnectionUrl(ownerType, ownerId, null)).reply(200, {
        repositoryConnectionStatus: {
          allowChange: false,
          allowOverride: true,
          enabled: true,
          inheritedFromOrgEnabled: true,
          inheritedFromOrganizationId: 'ROOT_ORGANIZATION_ID',
          inheritedFromOrganizationName: 'someOrganizationName',
        },
      });

      renderComponent(preloadedState);

      const infoAlert = await screen.findByText('The inherited configuration cannot be overridden.');
      expect(infoAlert).toBeVisible();
      const allowOverride = await screen.findByLabelText('Allow Override');
      expect(allowOverride).toBeVisible();
      expect(allowOverride).toBeDisabled();
      const inherit = await screen.findByLabelText('Inherit');
      expect(inherit).toBeVisible();
      expect(inherit).toBeDisabled();
      const disable = await screen.findByLabelText('Disable');
      expect(disable).toBeVisible();
      expect(disable).toBeDisabled();
      const enable = await screen.findByLabelText('Enable and Override Repository Connections');
      expect(enable).toBeVisible();
      expect(enable).toBeDisabled();
    });

    it('does not render an info alert and enables the checkbox and radio buttons if `allowChange` is true', async () => {
      axiosMock.onGet(getRepositoryConnectionUrl(ownerType, ownerId, null)).reply(200, {
        repositoryConnectionStatus: {
          allowChange: true,
          allowOverride: true,
          enabled: true,
          inheritedFromOrgEnabled: true,
          inheritedFromOrganizationId: 'ROOT_ORGANIZATION_ID',
          inheritedFromOrganizationName: 'someOrganizationName',
        },
      });

      renderComponent(preloadedState);

      const infoAlert = screen.queryByText('The inherited configuration cannot be overridden.');
      expect(infoAlert).not.toBeInTheDocument();
      const allowOverride = await screen.findByLabelText('Allow Override');
      expect(allowOverride).toBeVisible();
      expect(allowOverride).toBeEnabled();
      const inherit = await screen.findByLabelText('Inherit');
      expect(inherit).toBeVisible();
      expect(inherit).toBeEnabled();
      const disable = await screen.findByLabelText('Disable');
      expect(disable).toBeVisible();
      expect(disable).toBeEnabled();
      const enable = await screen.findByLabelText('Enable and Override Repository Connections');
      expect(enable).toBeVisible();
      expect(enable).toBeEnabled();
    });

    it('does not show inherit for the root organization and omits override text', async () => {
      preloadedState = {
        orgsAndPolicies: {
          root: {
            selectedOwner: {
              id: 'ROOT_ORGANIZATION_ID',
              name: 'Root Organization',
            },
          },
        },
        router: {
          currentState: {
            name: 'repositoryBaseConfigurations.organization',
            url: '/organization/{organizationId}/repositoryBaseConfigurations',
            data: {},
          },
          currentParams: {
            organizationId: 'ROOT_ORGANIZATION_ID',
          },
        },
      };

      axiosMock.onGet(getRepositoryConnectionUrl(ownerType, 'ROOT_ORGANIZATION_ID', null)).reply(200, {
        repositoryConnectionStatus: {
          allowChange: true,
          allowOverride: true,
          enabled: true,
          inheritedFromOrgEnabled: null,
          inheritedFromOrganizationId: null,
          inheritedFromOrganizationName: null,
        },
      });

      renderComponent(preloadedState);
      const inherit = screen.queryByLabelText('Inherit');
      expect(inherit).not.toBeInTheDocument();
      const disable = await screen.findByLabelText('Disable');
      expect(disable).toBeVisible();
      const enable = await screen.findByLabelText('Enable');
      expect(enable).toBeVisible();
    });

    it('does not render allow override checkbox for an application', async () => {
      preloadedState = {
        orgsAndPolicies: {
          root: {
            selectedOwner: {
              id: ownerId,
              name: 'broadcast',
            },
          },
        },
        router: {
          currentState: {
            name: 'repositoryBaseConfigurations.application',
            url: '/application/{applicationId}/repositoryBaseConfigurations',
            data: {},
          },
          currentParams: {
            applicationPublicId: 'appPublicId',
            applicationId: ownerId,
          },
        },
      };
      axiosMock.onGet(getRepositoryConnectionUrl('application', ownerId, null)).reply(200, {
        ownerDTO: {
          ownerPublicId: 'appPublicId',
          ownerId,
          ownerName: 'appPublicId',
          ownerType: 'APPLICATION',
        },
        repositoryConnectionStatus: {
          allowChange: true,
          allowOverride: true,
          enabled: null,
          inheritedFromOrgEnabled: true,
          inheritedFromOrganizationId: 'ROOT_ORGANIZATION_ID',
          inheritedFromOrganizationName: 'someOrganizationName',
        },
      });

      renderComponent(preloadedState);

      const allowOverride = screen.queryByLabelText('Allow Override');
      expect(allowOverride).not.toBeInTheDocument();
    });

    it('enables adding/editing repository connections if changes are allowed and enabled is saved', async () => {
      axiosMock.onGet(getRepositoryConnectionUrl(ownerType, ownerId, null)).reply(200, {
        repositoryConnections: [
          { repositoryConnectionId: 'someRepositoryConnectionId', baseUrl: 'someBaseUrl', format: 'someFormat' },
        ],
        repositoryConnectionStatus: {
          allowChange: true,
          allowOverride: true,
          enabled: true,
          inheritedFromOrgEnabled: true,
          inheritedFromOrganizationId: 'ROOT_ORGANIZATION_ID',
          inheritedFromOrganizationName: 'someOrganizationName',
        },
      });

      renderComponent(preloadedState);

      const addRepo = await screen.findByText('Add a Repository');
      const addButton = addRepo.closest('button');

      expect(addButton).toBeVisible();
      expect(addButton).not.toHaveClassName('disabled');

      const buttons = await screen.findAllByRole('button');
      const editButton = buttons.find((b) => b.getAttribute('class').includes('nx-btn--icon-only'));
      expect(editButton).toBeVisible();
      expect(editButton).not.toHaveClassName('disabled');
    });

    describe('add repository button', () => {
      beforeEach(() => {
        SpecUtil.requestIdleCallbackInvokeImmediate();
        preloadedState = {
          orgsAndPolicies: {
            root: {
              selectedOwner: {
                id: 'ROOT_ORGANIZATION_ID',
                name: 'Root Organization',
              },
            },
          },
          router: {
            currentState: {
              name: 'repositoryBaseConfigurations.organization',
              url: '/organization/{organizationId}/repositoryBaseConfigurations',
              data: {},
            },
            currentParams: {
              organizationId: 'ROOT_ORGANIZATION_ID',
            },
          },
        };
      });

      it('disables adding repository connections if enabled is not saved', async () => {
        axiosMock.onGet(getRepositoryConnectionUrl(ownerType, 'ROOT_ORGANIZATION_ID', null)).reply(200, {
          repositoryConnections: [
            { repositoryConnectionId: 'someRepositoryConnectionId', baseUrl: 'someBaseUrl', format: 'someFormat' },
          ],
          repositoryConnectionStatus: {
            allowChange: true,
            allowOverride: true,
            enabled: false,
            inheritedFromOrgEnabled: null,
            inheritedFromOrganizationId: null,
            inheritedFromOrganizationName: null,
          },
        });
        renderComponent(preloadedState);

        const enable = await screen.findByLabelText('Enable');
        fireEvent.click(enable);

        const addRepo = await screen.findByText('Add a Repository');
        const addButton = addRepo.closest('button');
        expect(addButton).toBeVisible();
        expect(addButton).toHaveClassName('disabled');

        fireEvent.mouseOver(addButton);
        const tooltip = await screen.findByRole('tooltip');
        expect(within(tooltip).getByText(MUST_UPDATE_ENABLED_ADD_MESSAGE)).toBeInTheDocument();
      });

      it('disables editing repository connections if enabled is not saved', async () => {
        axiosMock.onGet(getRepositoryConnectionUrl(ownerType, 'ROOT_ORGANIZATION_ID', null)).reply(200, {
          repositoryConnections: [
            { repositoryConnectionId: 'someRepositoryConnectionId', baseUrl: 'someBaseUrl', format: 'someFormat' },
          ],
          repositoryConnectionStatus: {
            allowChange: true,
            allowOverride: true,
            enabled: false,
            inheritedFromOrgEnabled: null,
            inheritedFromOrganizationId: null,
            inheritedFromOrganizationName: null,
          },
        });

        renderComponent(preloadedState);

        const enable = await screen.findByLabelText('Enable');
        fireEvent.click(enable);

        const buttons = await screen.findAllByRole('button');
        const editButton = buttons.find((b) => b.getAttribute('class').includes('nx-btn--icon-only'));
        expect(editButton).toBeVisible();
        expect(editButton).toHaveClassName('disabled');
        fireEvent.mouseOver(editButton);
        const tooltip = await screen.findByRole('tooltip');
        expect(within(tooltip).getByText(MUST_UPDATE_ENABLED_EDIT_MESSAGE)).toBeInTheDocument();
      });

      it('disables adding repository connections if changes are not allowed', async () => {
        axiosMock.onGet(getRepositoryConnectionUrl(ownerType, 'ROOT_ORGANIZATION_ID', null)).reply(200, {
          repositoryConnections: [
            { repositoryConnectionId: 'someRepositoryConnectionId', baseUrl: 'someBaseUrl', format: 'someFormat' },
          ],
          repositoryConnectionStatus: {
            allowChange: false,
            allowOverride: true,
            enabled: true,
            inheritedFromOrgEnabled: null,
            inheritedFromOrganizationId: null,
            inheritedFromOrganizationName: null,
          },
        });

        renderComponent(preloadedState);

        const addRepo = await screen.findByText('Add a Repository');
        const addButton = addRepo.closest('button');
        expect(addButton).toBeVisible();
        expect(addButton).toHaveClassName('disabled');

        fireEvent.mouseOver(addButton);
        const tooltip = await screen.findByRole('tooltip');
        expect(within(tooltip).getByText(PARENT_ORGANIZATIONS_MUST_ALLOW_OVERRIDE)).toBeInTheDocument();
      });
    });
  });
});
