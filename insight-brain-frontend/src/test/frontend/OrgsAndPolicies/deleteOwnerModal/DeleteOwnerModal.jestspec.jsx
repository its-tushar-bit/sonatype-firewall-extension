/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { getOrganizationsUrl } from 'MainRoot/util/CLMLocation';
import DeleteOwnerModal from 'MainRoot/OrgsAndPolicies/deleteOwnerModal/DeleteOwnerModal';
import { actions } from 'MainRoot/OrgsAndPolicies/deleteOwnerModal/deleteOwnerSlice';
import { fireEvent, render, screen, axiosMockAdapter } from 'TestRoot/SpecUtil';

import 'TestRoot/SpecUtil';

const OWNER_ORG_NAME = 'Organization Two Name';
const OWNER_APP_NAME = 'Application One Name';

describe('DeleteOwnerModal', () => {
  let preloadedState, axiosMock, closeModalSpy;

  beforeAll(() => {
    axiosMock = axiosMockAdapter();
  });

  beforeEach(() => {
    jest.spyOn(actions, 'removeOwner');
    closeModalSpy = jest.spyOn(actions, 'closeModal');
  });

  const renderComponent = (preloadedState) => render(<DeleteOwnerModal />, { preloadedState });

  describe('Organization', () => {
    beforeEach(() => {
      preloadedState = {
        router: {
          currentState: {
            name: 'management.view.organization',
          },
        },
        orgsAndPolicies: {
          root: {
            selectedOwner: {
              id: 'organizationTwoID',
              name: OWNER_ORG_NAME,
              parentOrganizationId: 'ROOT_ORGANIZATION_ID',
            },
          },
          ownerSideNav: {
            ownersMap: {
              organizationTwoID: {
                type: 'organization',
                id: 'organizationTwoID',
                name: OWNER_ORG_NAME,
                synthetic: false,
                parentOrganizationId: null,
                applicationIds: [],
                organizationIds: [],
                subOrgs: 13,
                totalApps: 16,
              },
            },
          },
          ownerActions: {
            deleteOwner: {
              submitError: null,
              isModalOpen: true,
            },
          },
        },
      };
    });

    it("does not render modal if it's not triggered", () => {
      preloadedState.orgsAndPolicies.ownerActions.deleteOwner = {
        submitError: null,
        isModalOpen: false,
      };
      renderComponent(preloadedState);

      expect(screen.queryByText('Delete Organization')).not.toBeInTheDocument();
    });

    it('renders correct title for organization', () => {
      renderComponent(preloadedState);

      expect(screen.getByText('Delete Organization')).toBeVisible();
      expect(screen.getByRole('dialog')).toHaveTextContent(OWNER_ORG_NAME);
    });

    describe('renders correct modal content', () => {
      it('if organization has 0 descendants', () => {
        preloadedState.orgsAndPolicies.ownerSideNav.ownersMap.organizationTwoID.subOrgs = 0;
        preloadedState.orgsAndPolicies.ownerSideNav.ownersMap.organizationTwoID.totalApps = 0;
        renderComponent(preloadedState);

        expect(
          screen.getByText(
            `You are about to permanently remove ${OWNER_ORG_NAME} and 0 descendants. This action cannot be undone.`
          )
        ).toBeVisible();
      });

      it('if organization has only 1 descendant', () => {
        preloadedState.orgsAndPolicies.ownerSideNav.ownersMap.organizationTwoID.subOrgs = 1;
        preloadedState.orgsAndPolicies.ownerSideNav.ownersMap.organizationTwoID.totalApps = 0;
        renderComponent(preloadedState);

        expect(
          screen.getByText(
            `You are about to permanently remove ${OWNER_ORG_NAME} and 1 descendant. This action cannot be undone.`
          )
        ).toBeVisible();
      });

      // TODO repo test required

      it('if organization has >1 descendant', () => {
        preloadedState.orgsAndPolicies.ownerSideNav.ownersMap.organizationTwoID.subOrgs = 10;
        preloadedState.orgsAndPolicies.ownerSideNav.ownersMap.organizationTwoID.totalApps = 12;
        renderComponent(preloadedState);

        expect(
          screen.getByText(
            `You are about to permanently remove ${OWNER_ORG_NAME} and 22 descendants. This action cannot be undone.`
          )
        ).toBeVisible();
      });
    });

    it('handles submit error', async () => {
      const orgDeleteUrl = `${getOrganizationsUrl()}/organizationTwoID`;
      axiosMock.onDelete(orgDeleteUrl).replyOnce(502).onDelete(orgDeleteUrl).reply(204);

      renderComponent(preloadedState);

      const deleteBtn = screen.getByRole('button', { name: 'Delete' });
      fireEvent.click(deleteBtn);

      expect(await screen.findByRole('alert', /An error occurred saving data. Bad Gateway/i)).toBeVisible();

      //no errors
      fireEvent.click(screen.getByRole('button', { name: 'Retry' }));
      expect(await screen.findByText('Success!')).toBeVisible();
    });

    it('closes modal on cancel', () => {
      renderComponent(preloadedState);
      const closeButton = screen.getByRole('button', { name: 'Cancel' });
      expect(closeButton).toBeVisible();
      expect(closeButton).not.toHaveClass('disabled');
      fireEvent.click(closeButton);
      expect(closeModalSpy).toHaveBeenCalledTimes(1);
    });
  });

  describe('Application', () => {
    beforeEach(() => {
      preloadedState = {
        router: {
          currentState: {
            name: 'management.view.application',
          },
        },
        orgsAndPolicies: {
          root: {
            selectedOwner: {
              id: 'applicationOneID',
              publicId: 'applicationOnePublicID',
              organizationId: 'organizationOneID',
              name: OWNER_APP_NAME,
            },
          },
          ownerSideNav: {
            ownersMap: {
              applicationOnePublicID: {
                type: 'application',
                id: 'applicationOneID',
                name: OWNER_APP_NAME,
                publicId: 'applicationOnePublicID',
                organizationId: 'c610b4cc9e53468f9970ef3d5e8b72ce',
                provider: null,
                repositoryUrl: null,
              },
            },
          },
          ownerActions: {
            deleteOwner: {
              submitError: null,
              isModalOpen: true,
            },
          },
        },
      };
    });

    it('renders correct title for application', () => {
      renderComponent(preloadedState);

      expect(screen.getByText('Delete Application')).toBeVisible();
      expect(screen.getByRole('dialog')).toHaveTextContent(OWNER_APP_NAME);
    });

    it('renders correct modal content', () => {
      renderComponent(preloadedState);
      expect(
        screen.getByText(`You are about to permanently remove ${OWNER_APP_NAME}. This action cannot be undone.`)
      ).toBeVisible();
    });
  });

  describe('Repository', () => {
    const preloadedState = {
      router: {
        currentParams: { '#': null, repositoryManagerId: 'repositoryManagerId' },
        currentState: {
          name: 'management.view.repository_manager',
          url: '/repository_manager/{repositoryManagerId}',
        },
      },
      orgsAndPolicies: {
        ownerSideNav: {
          ownersMap: {
            '0b9a675da0a14deabe26ad90df74a0cf': {
              type: 'repositoryManager',
              id: '0b9a675da0a14deabe26ad90df74a0cf',
              name: '91D74F09-3FE2E0B7-DF2B86A6-969AE288-DE07E9B5',
              synthetic: false,
              parentOrganizationId: 'REPOSITORY_CONTAINER_ID',
              repositoryIds: ['repositoryId'],
            },
          },
        },
        root: {
          selectedOwner: {
            id: '0b9a675da0a14deabe26ad90df74a0cf',
            parentOrganizationId: 'REPOSITORY_CONTAINER_ID',
            name: '91D74F09-3FE2E0B7-DF2B86A6-969AE288-DE07E9B5',
          },
        },
        ownerActions: {
          deleteOwner: {
            submitError: null,
            isModalOpen: true,
          },
        },
      },
    };

    it('renders correct title for repository', () => {
      renderComponent(preloadedState);

      expect(screen.getByText('Delete Repository Manager')).toBeVisible();
      expect(screen.getByRole('dialog')).toHaveTextContent(preloadedState.orgsAndPolicies.root.selectedOwner.name);
    });

    it('renders correct modal content', () => {
      renderComponent(preloadedState);
      expect(
        screen.getByText(
          `You are about to permanently remove ${preloadedState.orgsAndPolicies.root.selectedOwner.name} and 1 descendant. This action cannot be undone.`
        )
      ).toBeVisible();
    });
  });
});
