/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import userEvent from '@testing-library/user-event';
import { axiosMockAdapter, render, screen, act } from 'TestRoot/SpecUtil';
import * as repositoriesSelectors from 'MainRoot/OrgsAndPolicies/repositories/repositoriesConfigurationSelectors';
import * as ownerSideNavSelectors from 'MainRoot/OrgsAndPolicies/ownerSideNav/ownerSideNavSelectors';
import * as ownerSummarySelectors from 'MainRoot/OrgsAndPolicies/ownerSummarySelectors';
import {
  actions as repositoriesActions,
  VIEW_TYPES,
} from 'MainRoot/OrgsAndPolicies/repositories/repositoriesConfigurationSlice';
import RepositoriesConfigurationTile from 'MainRoot/OrgsAndPolicies/repositories/RepositoriesConfigurationTile';
import { getRepositoriesUrl, getRepositoryInfoUrl, getRepositoryListUrl } from 'MainRoot/util/CLMLocation';
import { fireEvent, within } from '@testing-library/react';
import { groupBy, prop } from 'ramda';
import * as routerSelectors from 'MainRoot/reduxUiRouter/routerSelectors';
import * as orgsAndPoliciesSelectors from 'MainRoot/OrgsAndPolicies/orgsAndPoliciesSelectors';

describe('RepositoriesConfigurationTile', () => {
  let renderComponent,
    repos,
    reposAtManagerLevel,
    axiosMock,
    openDeleteModalSpy,
    openEditRepositoryManagerNameModalSpy,
    deleteRepositorySpy,
    loadRepositoriesSpy,
    editRepositoryManagerNameSpy,
    loadRepositoriesByManagerIdSpy,
    goToRepositorySummaryViewSpy;

  repos = [
    {
      oldestEvalTimestamp: null,
      managerInstanceId: 'managerInstanceIdA',
      managerName: 'managerNameA',
      proxyUrl: 'http://localhost/api/v2/firewall/enterprise/managerInstanceIdA/repositoryNameA/maven',
      repository: {
        id: 'repositoryA',
        repositoryManagerId: 'repositoryManagerIdA',
        publicId: 'repositoryNameA',
        auditEnabled: true,
        quarantineEnabled: true,
        format: 'maven',
        repositoryType: 'proxy',
      },
    },
    {
      oldestEvalTimestamp: null,
      managerInstanceId: 'managerInstanceIdB',
      proxyUrl: 'http://localhost/api/v2/firewall/enterprise/managerInstanceIdB/repositoryNameB/npm',
      repository: {
        id: 'repositoryB',
        repositoryManagerId: 'repositoryManagerIdB',
        publicId: 'repositoryNameB',
        auditEnabled: false,
        quarantineEnabled: false,
        namespaceConfusionProtectionEnabled: true,
        format: 'npm',
        repositoryType: 'hosted',
      },
    },
  ];

  reposAtManagerLevel = [
    {
      oldestEvalTimestamp: null,
      managerInstanceId: 'managerInstanceId',
      managerName: 'managerName',
      proxyUrl: 'http://localhost/api/v2/firewall/enterprise/managerInstanceId/repositoryName/maven',
      repository: {
        id: 'repository',
        repositoryManagerId: 'repositoryManagerId',
        publicId: 'repositoryName',
        auditEnabled: true,
        quarantineEnabled: true,
        format: 'maven',
        repositoryType: 'proxy',
      },
    },
  ];

  const repoManagerOwnersEntries = [
    {
      type: 'repository_manager',
      id: '134f25b4e96548a0b112941da77183b9',
      name: '37243988-2A3E4AF9-BE920718-EF863E4B-703AE4FBCD',
      instanceId: 'managerInstanceId',
      repositoryIds: ['repository'],
      parentId: 'REPOSITORY_CONTAINER_ID',
    },
  ];

  beforeEach(() => {
    axiosMock = axiosMockAdapter();

    jest.spyOn(ownerSideNavSelectors, 'selectRepoManagerOwnersEntries').mockReturnValue(repoManagerOwnersEntries);

    deleteRepositorySpy = jest.spyOn(repositoriesActions, 'deleteRepository');
    loadRepositoriesSpy = jest.spyOn(repositoriesActions, 'loadRepositories');
    editRepositoryManagerNameSpy = jest.spyOn(repositoriesActions, 'editRepositoryManagerName');
    openDeleteModalSpy = jest.spyOn(repositoriesActions, 'openDeleteModal');
    openEditRepositoryManagerNameModalSpy = jest.spyOn(repositoriesActions, 'openEditRepositoryManagerNameModal');
    loadRepositoriesByManagerIdSpy = jest.spyOn(repositoriesActions, 'loadRepositoriesByManagerId');
    goToRepositorySummaryViewSpy = jest.spyOn(repositoriesActions, 'goToRepositorySummaryView');

    axiosMock.onGet(getRepositoriesUrl()).reply(200, repos);
    axiosMock.onDelete(getRepositoryInfoUrl('repositoryA')).reply(204);
    axiosMock.onGet(getRepositoryListUrl('repositoryManagerId')).reply(200, reposAtManagerLevel);

    renderComponent = (props = {}) => render(<RepositoriesConfigurationTile {...props} />);
  });

  describe('when data are being loaded', () => {
    beforeEach(() => {
      jest.spyOn(repositoriesSelectors, 'selectRepositoriesLoading').mockReturnValue(true);
    });

    it('renders loading message', () => {
      renderComponent();

      expect(screen.queryByText('Loading…')).toBeInTheDocument();
    });
  });

  describe('when the page has a loading error', () => {
    beforeEach(() => {
      jest.spyOn(repositoriesSelectors, 'selectRepositoriesLoading').mockReturnValue(false);
      jest.spyOn(repositoriesSelectors, 'selectRepositoriesLoadError').mockReturnValue('Test loading error');
    });

    it('renders error section', () => {
      renderComponent();

      const retryButton = screen.queryByText('Retry');
      expect(screen.getByText('An error occurred loading data. Test loading error')).toBeVisible();
      fireEvent.click(retryButton);
      expect(loadRepositoriesSpy).toHaveBeenCalled();
    });
  });

  describe('when there are no repositories ', () => {
    beforeEach(() => {
      jest.spyOn(repositoriesSelectors, 'selectRepositories').mockReturnValue([]);
      jest.spyOn(repositoriesSelectors, 'selectRepositoriesLoading').mockReturnValue(false);
    });

    it('renders default empty message', () => {
      renderComponent();

      expect(screen.queryByText('Loading…')).not.toBeInTheDocument();
      expect(screen.getByText('There are no repositories registered with the server.')).toBeInTheDocument();
    });
  });

  describe('when repositories exist', () => {
    beforeEach(() => {
      jest
        .spyOn(repositoriesSelectors, 'selectRepositoriesByManagerInstanceId')
        .mockReturnValue(groupBy(prop('managerInstanceId'))(repos));
      jest.spyOn(repositoriesSelectors, 'selectRepositoriesLoading').mockReturnValue(false);
      jest.spyOn(repositoriesSelectors, 'selectRepositoriesLoading').mockReturnValue(false);
      jest.spyOn(routerSelectors, 'selectIsRepositoryManager').mockReturnValue(false);
      jest.spyOn(ownerSideNavSelectors, 'selectNonVirtualRepoManagerOwnersEntriesSorted').mockReturnValue([
        {
          type: 'repository_manager',
          id: 'repositoryManagerIdA',
          name: 'managerNameA',
          instanceId: 'managerInstanceIdA',
          repositoryIds: ['repositoryA'],
          parentId: 'REPOSITORY_CONTAINER_ID',
        },
        {
          type: 'repository_manager',
          id: 'repositoryManagerIdB',
          name: 'managerNameB',
          instanceId: 'managerInstanceIdB',
          repositoryIds: ['repositoryB'],
          parentId: 'REPOSITORY_CONTAINER_ID',
        },
      ]);
    });

    it('renders page with all elements on the table', () => {
      renderComponent();
      const repositories = [
        {
          managerLabel: screen.getByText('managerNameA'),
          publicId: screen.getByText('repositoryNameA'),
          format: screen.getByText('maven'),
          repositoryType: screen.getByText('proxy'),
          enablement: screen.getByText('Audit, Quarantine'),
        },
        {
          managerLabel: screen.getByText('managerNameB'),
          publicId: screen.getByText('repositoryNameB'),
          format: screen.getByText('npm'),
          repositoryType: screen.getByText('hosted'),
          enablement: screen.getByText('Namespace Scanning'),
        },
      ];

      expect(repositories[0].managerLabel).toBeVisible();
      expect(repositories[0].publicId).toBeVisible();
      expect(repositories[0].publicId.parentElement).toHaveClass('nx-text-link');
      expect(repositories[0].format).toBeVisible();
      expect(repositories[0].repositoryType).toBeVisible();
      expect(repositories[0].enablement).toBeVisible();

      expect(repositories[1].managerLabel).toBeVisible();
      expect(repositories[1].publicId).toBeVisible();
      expect(repositories[1].publicId.parentElement).not.toHaveClass('nx-text-link');
      expect(repositories[1].format).toBeVisible();
      expect(repositories[1].repositoryType).toBeVisible();
      expect(repositories[1].enablement).toBeVisible();
    });

    it('renders hosted repo as plain text when showHostedRepoLink is false (default)', () => {
      renderComponent();
      expect(screen.queryByTestId('repositories_configuration-hosted-link')).not.toBeInTheDocument();
      expect(screen.getByText('repositoryNameB')).toBeVisible();
    });

    it('renders hosted repo as a link when showHostedRepoLink is true and owner has instanceId', () => {
      jest.spyOn(orgsAndPoliciesSelectors, 'selectSelectedOwner').mockReturnValue({
        id: 'repositoryManagerIdB',
        instanceId: 'managerInstanceIdB',
        name: 'managerNameB',
      });
      renderComponent({ showHostedRepoLink: true });
      const link = screen.getByTestId('repositories_configuration-hosted-link');
      expect(link).toBeVisible();
      expect(link).toHaveTextContent('repositoryNameB');
    });

    it('renders hosted repo as plain text when showHostedRepoLink is true but owner has no instanceId (SaaS)', () => {
      jest.spyOn(orgsAndPoliciesSelectors, 'selectSelectedOwner').mockReturnValue({
        id: 'repositoryManagerIdB',
        instanceId: undefined,
        name: 'managerNameB',
      });
      renderComponent({ showHostedRepoLink: true });
      expect(screen.queryByTestId('repositories_configuration-hosted-link')).not.toBeInTheDocument();
      expect(screen.getByText('repositoryNameB')).toBeVisible();
    });

    describe('Delete modal', () => {
      it('renders delete modal warning for corresponding repository', () => {
        const REPOSITORY_A_WARNING =
          'Are you sure you want to remove the Repository with ID "repositoryNameA"? This action is not reversible.';
        renderComponent();
        const deleteIcons = screen.queryAllByTestId('repository-delete-button');

        expect(deleteIcons.length).toBe(2);
        fireEvent.click(deleteIcons[0]);

        const deleteModal = screen.getByTestId('delete-modal');

        expect(openDeleteModalSpy).toHaveBeenCalled();
        expect(deleteModal).toBeVisible();
        expect(screen.getByText(REPOSITORY_A_WARNING)).toBeInTheDocument();
      });
    });

    describe('Edit repository manager name modal', () => {
      it('renders edit repository manager instance id and name for corresponding repository', async () => {
        renderComponent();
        const row = screen.queryByText('managerNameA').closest('tr');
        expect(row).toBeVisible();
        const button = within(row).getByRole('button');
        expect(button).toBeVisible();
        fireEvent.click(button);

        const editRepositoryManagerNameModal = screen.getByTestId('edit-repository-manager-name-modal');
        expect(editRepositoryManagerNameModal).toBeVisible();
        expect(openEditRepositoryManagerNameModalSpy).toHaveBeenCalled();

        expect(screen.getByText('managerInstanceIdA')).toBeVisible();
        expect(screen.getByText('managerNameA')).toBeVisible();
        expect(
          screen.getByText('Any changes made will apply to all repositories for this repository manager.')
        ).toBeInTheDocument();
      });
    });

    describe('Edit repository button at repository managers level', () => {
      it('navigates user to corresponding repository page on click', () => {
        renderComponent();
        const editIcons = screen.queryAllByTestId('repository-edit-button');

        expect(editIcons.length).toBe(2);
        fireEvent.click(editIcons[0]);

        expect(goToRepositorySummaryViewSpy).toHaveBeenCalledWith('repositoryA');
      });
    });

    describe('Copy proxy URL button at repository managers level', () => {
      it('copy button is hidden for non-virtual manager repos', () => {
        renderComponent();
        const copyButtons = screen.queryAllByTestId('repository-copy-url-button');

        // Both managers are non-virtual (no managerType: 'virtual'), so all copy buttons should be hidden
        expect(copyButtons.length).toBe(2);
        copyButtons.forEach((btn) => {
          expect(btn).toHaveClass('iq-copy-url-button--hidden');
        });
      });

      it('copy button is visible for virtual manager proxy repos on virtual page', () => {
        jest.spyOn(ownerSideNavSelectors, 'selectVirtualRepoManagerOwnersEntriesSorted').mockReturnValue([
          {
            type: 'repository_manager',
            id: 'virtualManagerIdA',
            name: 'Virtual Manager A',
            instanceId: 'managerInstanceIdA',
            repositoryIds: ['repositoryA'],
            parentId: 'REPOSITORY_CONTAINER_ID',
            managerType: 'virtual',
          },
          {
            type: 'repository_manager',
            id: 'virtualManagerIdB',
            name: 'Virtual Manager B',
            instanceId: 'managerInstanceIdB',
            repositoryIds: ['repositoryB'],
            parentId: 'REPOSITORY_CONTAINER_ID',
            managerType: 'virtual',
          },
        ]);

        renderComponent({ virtualOnly: true });
        const copyButtons = screen.queryAllByTestId('repository-copy-url-button');

        // repositoryA is proxy on a virtual manager => visible
        // repositoryB is hosted on a virtual manager => hidden
        expect(copyButtons[0]).not.toHaveClass('iq-copy-url-button--hidden');
        expect(copyButtons[1]).toHaveClass('iq-copy-url-button--hidden');
      });

      it('copy button is hidden for virtual manager hosted repos on virtual page', () => {
        jest.spyOn(ownerSideNavSelectors, 'selectVirtualRepoManagerOwnersEntriesSorted').mockReturnValue([
          {
            type: 'repository_manager',
            id: 'virtualManagerId',
            name: 'Virtual Manager',
            instanceId: 'managerInstanceIdB',
            repositoryIds: ['repositoryB'],
            parentId: 'REPOSITORY_CONTAINER_ID',
            managerType: 'virtual',
          },
        ]);

        renderComponent({ virtualOnly: true });
        const copyButtons = screen.queryAllByTestId('repository-copy-url-button');

        // repositoryB is hosted even though manager is virtual => still hidden
        expect(copyButtons[0]).toHaveClass('iq-copy-url-button--hidden');
      });

      it('copies the correct proxy URL to the clipboard when clicked', () => {
        const clipboardWriteText = jest.fn().mockResolvedValue(undefined);
        Object.assign(navigator, { clipboard: { writeText: clipboardWriteText } });

        jest.spyOn(ownerSideNavSelectors, 'selectVirtualRepoManagerOwnersEntriesSorted').mockReturnValue([
          {
            type: 'repository_manager',
            id: 'virtualManagerId',
            name: 'Virtual Manager',
            instanceId: 'managerInstanceIdA',
            repositoryIds: ['repositoryA'],
            parentId: 'REPOSITORY_CONTAINER_ID',
            managerType: 'virtual',
          },
        ]);

        renderComponent({ virtualOnly: true });
        const copyButtons = screen.queryAllByTestId('repository-copy-url-button');

        fireEvent.click(copyButtons[0]);

        expect(clipboardWriteText).toHaveBeenCalledWith(
          'http://localhost/api/v2/firewall/enterprise/managerInstanceIdA/repositoryNameA/maven'
        );
      });
    });
  });

  describe('handles deleting error', () => {
    beforeEach(() => {
      jest.spyOn(repositoriesSelectors, 'selectDeleteModal').mockReturnValue(true);
      jest.spyOn(repositoriesSelectors, 'selectRepositoriesDeleteError').mockReturnValue('Test deleting error');
    });

    it('shows deleting error section', () => {
      renderComponent();
      const retryButton = screen.queryByText('Retry');

      expect(screen.getByText(/An error occurred saving data. Test deleting error/i)).toBeInTheDocument();
      fireEvent.click(retryButton);
      expect(deleteRepositorySpy).toHaveBeenCalled();
    });
  });

  describe('handles edit repository manager name error', () => {
    beforeEach(() => {
      jest.spyOn(repositoriesSelectors, 'selectShowEditRepositoryManagerNameModal').mockReturnValue(true);
      jest
        .spyOn(repositoriesSelectors, 'selectEditRepositoryManagerNameError')
        .mockReturnValue('Test edit repository manager name error');
    });

    it('shows edit repository manager name error section', () => {
      renderComponent();
      const retryButton = screen.queryByText('Retry');

      expect(
        screen.getByText(/An error occurred saving data. Test edit repository manager name error/i)
      ).toBeInTheDocument();
      fireEvent.click(retryButton);
      expect(editRepositoryManagerNameSpy).toHaveBeenCalled();
    });
  });

  describe('deleting repository', () => {
    beforeEach(() => {
      jest.spyOn(repositoriesSelectors, 'selectDeleteModal').mockReturnValue(true);
      jest
        .spyOn(repositoriesSelectors, 'selectDeleteModalInfo')
        .mockReturnValue({ id: 'repositoryA', publicId: 'repositoryNameA' });
    });

    it('deletes repository from the table', () => {
      renderComponent();

      const deleteButton = screen.getByRole('button', { name: 'Continue' });
      fireEvent.click(deleteButton);

      expect(deleteRepositorySpy).toHaveBeenCalledTimes(1);
    });
  });

  describe('editing a repository manager name', () => {
    beforeEach(() => {
      jest.spyOn(repositoriesSelectors, 'selectShowEditRepositoryManagerNameModal').mockReturnValue(true);
      jest.spyOn(repositoriesSelectors, 'selectEditRepositoryManagerNameModalInfo').mockReturnValue({
        managerInstanceId: 'someManagerInstanceId',
        managerName: 'someManagerName',
      });
    });

    it('validates empty input', () => {
      renderComponent();

      const editRepositoryManagerNameModal = screen.getByTestId('edit-repository-manager-name-modal');
      const editRepositoryManagerNameTextBox = within(editRepositoryManagerNameModal).getByRole('textbox');
      fireEvent.change(editRepositoryManagerNameTextBox, { target: { value: '' } });

      expect(screen.getByText('Must be non-empty')).toBeVisible();
    });

    it('edits a repository manager name', () => {
      renderComponent();

      const editRepositoryManagerNameModal = screen.getByTestId('edit-repository-manager-name-modal');
      const editRepositoryManagerNameButton = within(editRepositoryManagerNameModal).getByRole('button', {
        name: 'Update',
      });
      fireEvent.click(editRepositoryManagerNameButton);

      expect(editRepositoryManagerNameSpy).toHaveBeenCalledTimes(1);
    });
  });

  describe('when there are no repositories at repository manager level', () => {
    beforeEach(() => {
      jest.spyOn(repositoriesSelectors, 'selectRepositoriesLoading').mockReturnValue(false);
      jest.spyOn(repositoriesSelectors, 'selectRepositories').mockReturnValue([]);
      jest.spyOn(routerSelectors, 'selectIsRepositoryManager').mockReturnValue(true);
      jest.spyOn(orgsAndPoliciesSelectors, 'selectSelectedOwner').mockReturnValue({
        id: 'repositoryManagerId',
        instanceId: 'managerInstanceId',
        name: 'managerName',
      });
    });

    it('renders default empty message', () => {
      renderComponent();

      expect(screen.queryByText('Loading…')).not.toBeInTheDocument();
      expect(screen.getByText('There are no repositories registered with the server.')).toBeInTheDocument();
      expect(loadRepositoriesByManagerIdSpy).toHaveBeenCalled();
    });
  });

  describe('when repositories exist at repository manager level', () => {
    beforeEach(() => {
      jest.spyOn(repositoriesSelectors, 'selectRepositoriesLoading').mockReturnValue(false);
      jest.spyOn(repositoriesSelectors, 'selectRepositories').mockReturnValue([]);
      jest.spyOn(routerSelectors, 'selectIsRepositoryManager').mockReturnValue(true);
      jest.spyOn(orgsAndPoliciesSelectors, 'selectSelectedOwner').mockReturnValue({
        id: 'repositoryManagerId',
        instanceId: 'managerInstanceId',
        name: 'managerName',
      });
      jest
        .spyOn(repositoriesSelectors, 'selectRepositoriesByManagerInstanceId')
        .mockReturnValue(groupBy(prop('managerInstanceId'))(reposAtManagerLevel));
      jest.spyOn(ownerSideNavSelectors, 'selectRepoManagerOwnersEntries').mockReturnValue([]);
    });

    it('renders page with all elements on the table', () => {
      renderComponent();

      expect(screen.queryByText('managerName')).not.toBeInTheDocument();
      expect(screen.getByText('repositoryName')).toBeVisible();
      expect(screen.getByText('repositoryName').parentElement).toHaveClass('nx-text-link');
      expect(screen.getByText('maven')).toBeVisible();
      expect(screen.getByText('proxy')).toBeVisible();
      expect(screen.getByText('Audit, Quarantine')).toBeVisible();
      expect(loadRepositoriesByManagerIdSpy).toHaveBeenCalled();
    });

    describe('Delete modal at repository manager level', () => {
      it('renders delete modal warning for corresponding repository', () => {
        const REPOSITORY_A_WARNING =
          'Are you sure you want to remove the Repository with ID "repositoryName"? This action is not reversible.';
        renderComponent();
        const deleteIcons = screen.queryAllByTestId('repository-delete-button');

        expect(deleteIcons.length).toBe(1);
        fireEvent.click(deleteIcons[0]);

        const deleteModal = screen.getByTestId('delete-modal');

        expect(openDeleteModalSpy).toHaveBeenCalled();
        expect(deleteModal).toBeVisible();
        expect(screen.getByText(REPOSITORY_A_WARNING)).toBeInTheDocument();
      });
    });

    describe('Edit repository button at repository manager level', () => {
      it('navigates user to corresponding repository page on click', () => {
        renderComponent();
        const editIcons = screen.queryAllByTestId('repository-edit-button');

        expect(editIcons.length).toBe(1);
        fireEvent.click(editIcons[0]);

        expect(goToRepositorySummaryViewSpy).toHaveBeenCalledWith('repository');
      });
    });

    describe('Copy proxy URL button at repository manager level', () => {
      it('copy button is hidden when the owner is not a virtual manager', () => {
        renderComponent();
        const copyButtons = screen.queryAllByTestId('repository-copy-url-button');

        expect(copyButtons.length).toBe(1);
        expect(copyButtons[0]).toHaveClass('iq-copy-url-button--hidden');
      });
    });
  });

  describe('Filter reset behavior', () => {
    let resetViewFiltersSpy;
    let selectSelectedOwnerSpy;

    beforeEach(() => {
      resetViewFiltersSpy = jest.spyOn(repositoriesActions, 'resetViewFilters');
      jest.spyOn(repositoriesSelectors, 'selectRepositoriesLoading').mockReturnValue(false);
      jest.spyOn(repositoriesSelectors, 'selectRepositories').mockReturnValue([]);
      jest
        .spyOn(repositoriesSelectors, 'selectRepositoriesByManagerInstanceId')
        .mockReturnValue(groupBy(prop('managerInstanceId'))(repos));
    });

    describe('at container level', () => {
      beforeEach(() => {
        jest.spyOn(routerSelectors, 'selectIsRepositoryManager').mockReturnValue(false);
        jest.spyOn(routerSelectors, 'selectPrevStateIsRepositorySection').mockReturnValue(true);
        selectSelectedOwnerSpy = jest
          .spyOn(orgsAndPoliciesSelectors, 'selectSelectedOwner')
          .mockReturnValue({ id: 'containerIdA', instanceId: 'containerInstanceIdA', name: 'Container A' });
      });

      it('does not reset CONTAINER filter when unmounting with same owner (e.g. navigating to a repository page)', () => {
        const { unmount } = renderComponent();
        resetViewFiltersSpy.mockClear();

        unmount();

        expect(resetViewFiltersSpy).not.toHaveBeenCalled();
      });

      it('does not reset CONTAINER filter when remounting with same owner (navigating to a repo page and back)', () => {
        // Simulate: mount tile → navigate to repository page (unmount) → navigate back (remount)
        // prevStateIsRepositorySection stays true throughout (came from within repo section)
        const { unmount } = renderComponent();
        resetViewFiltersSpy.mockClear();

        unmount();

        // Remount — same owner, still within repository section
        renderComponent();

        expect(resetViewFiltersSpy).not.toHaveBeenCalled();
      });

      it('resets CONTAINER filter when owner changes (navigating to a different container)', () => {
        const { store } = renderComponent();
        resetViewFiltersSpy.mockClear();

        selectSelectedOwnerSpy.mockReturnValue({
          id: 'containerIdB',
          instanceId: 'containerInstanceIdB',
          name: 'Container B',
        });
        act(() => {
          store.dispatch({ type: 'orgsAndPolicies/loadSelectedOwner/fulfilled', payload: { id: 'containerIdB' } });
        });

        expect(resetViewFiltersSpy).toHaveBeenCalledWith(VIEW_TYPES.CONTAINER);
      });
    });

    describe('at repository manager level', () => {
      beforeEach(() => {
        jest.spyOn(routerSelectors, 'selectIsRepositoryManager').mockReturnValue(true);
        jest.spyOn(routerSelectors, 'selectPrevStateIsRepositorySection').mockReturnValue(true);
        selectSelectedOwnerSpy = jest
          .spyOn(orgsAndPoliciesSelectors, 'selectSelectedOwner')
          .mockReturnValue({ id: 'managerIdA', instanceId: 'managerInstanceIdA', name: 'Manager A' });
        jest
          .spyOn(repositoriesSelectors, 'selectRepositoriesByManagerInstanceId')
          .mockReturnValue(groupBy(prop('managerInstanceId'))(reposAtManagerLevel));
      });

      it('does not reset MANAGER filter when unmounting with same owner (e.g. navigating to a repository page)', () => {
        const { unmount } = renderComponent();
        resetViewFiltersSpy.mockClear();

        unmount();

        expect(resetViewFiltersSpy).not.toHaveBeenCalled();
      });

      it('resets MANAGER filter when owner changes (navigating to a different manager)', () => {
        const { store } = renderComponent();
        resetViewFiltersSpy.mockClear();

        selectSelectedOwnerSpy.mockReturnValue({
          id: 'managerIdB',
          instanceId: 'managerInstanceIdB',
          name: 'Manager B',
        });
        act(() => {
          store.dispatch({ type: 'orgsAndPolicies/loadSelectedOwner/fulfilled', payload: { id: 'managerIdB' } });
        });

        expect(resetViewFiltersSpy).toHaveBeenCalledWith(VIEW_TYPES.MANAGER);
      });
    });

    describe('when navigating from outside the repository section (e.g. Dashboard)', () => {
      beforeEach(() => {
        jest.spyOn(routerSelectors, 'selectIsRepositoryManager').mockReturnValue(false);
        jest
          .spyOn(orgsAndPoliciesSelectors, 'selectSelectedOwner')
          .mockReturnValue({ id: 'containerIdA', instanceId: 'containerInstanceIdA', name: 'Container A' });
        jest.spyOn(routerSelectors, 'selectPrevStateIsRepositorySection').mockReturnValue(false);
      });

      it('resets CONTAINER filter when arriving from a non-repository page (same owner)', () => {
        renderComponent();

        expect(resetViewFiltersSpy).toHaveBeenCalledWith(VIEW_TYPES.CONTAINER);
      });

      it('resets CONTAINER filter on cold deep-link even when owner loads async (ownerJustLoaded does not suppress non-repo entry)', () => {
        // prevStateIsRepositorySection is false (arriving from outside repo section)
        // Owner is initially undefined (async load) — ownerJustLoaded must NOT suppress the reset
        const ownerSpy = jest.spyOn(orgsAndPoliciesSelectors, 'selectSelectedOwner').mockReturnValue(undefined);
        const { store } = renderComponent();
        resetViewFiltersSpy.mockClear();

        ownerSpy.mockReturnValue({ id: 'containerIdA', instanceId: 'containerInstanceIdA', name: 'Container A' });
        act(() => {
          store.dispatch({ type: 'orgsAndPolicies/loadSelectedOwner/fulfilled', payload: { id: 'containerIdA' } });
        });

        expect(resetViewFiltersSpy).toHaveBeenCalledWith(VIEW_TYPES.CONTAINER);
      });

      it('resets CONTAINER filter when navigating away to a non-repository page without owner or view change', () => {
        // Start within repo section, then prevStateIsRepositorySection becomes false
        // (user navigated away to Dashboard) without owner or isRepositoryManager changing.
        // The main effect's deps don't change so only the secondary effect handles this.
        jest.spyOn(routerSelectors, 'selectPrevStateIsRepositorySection').mockReturnValue(true);
        const { store } = renderComponent();
        resetViewFiltersSpy.mockClear();

        // Simulate router updating prevState to a non-repo route (user navigated to Dashboard)
        jest.spyOn(routerSelectors, 'selectPrevStateIsRepositorySection').mockReturnValue(false);
        act(() => {
          store.dispatch(repositoriesActions.setRepositoryPublicIdFilter(''));
        });

        expect(resetViewFiltersSpy).toHaveBeenCalledWith(VIEW_TYPES.CONTAINER);
      });

      it('does not reset filter when owner resolves from undefined to defined (async data load, not navigation)', () => {
        jest.spyOn(routerSelectors, 'selectPrevStateIsRepositorySection').mockReturnValue(true);
        const ownerSpy = jest.spyOn(orgsAndPoliciesSelectors, 'selectSelectedOwner').mockReturnValue(undefined);
        const { store } = renderComponent();
        resetViewFiltersSpy.mockClear();

        ownerSpy.mockReturnValue({ id: 'containerIdA', instanceId: 'containerInstanceIdA', name: 'Container A' });
        act(() => {
          store.dispatch({ type: 'orgsAndPolicies/loadSelectedOwner/fulfilled', payload: { id: 'containerIdA' } });
        });

        expect(resetViewFiltersSpy).not.toHaveBeenCalled();
      });
    });

    describe('when navigating to a repository results page and back', () => {
      beforeEach(() => {
        jest.spyOn(routerSelectors, 'selectIsRepositoryManager').mockReturnValue(false);
        jest
          .spyOn(orgsAndPoliciesSelectors, 'selectSelectedOwner')
          .mockReturnValue({ id: 'containerIdA', instanceId: 'containerInstanceIdA', name: 'Container A' });
      });

      it.each([
        ['firewall.repository-report', 'non-docker repo results'],
        ['firewall.containerRepositoryResults', 'docker container repo results'],
        ['hostedRepoComponents', 'hosted repo components'],
      ])('does not reset filter when returning from %s (%s)', (prevRouteName) => {
        jest
          .spyOn(routerSelectors, 'selectPrevStateIsRepositorySection')
          .mockReturnValue(
            prevRouteName.includes('repository_container') ||
              prevRouteName.includes('repository_manager') ||
              prevRouteName.includes('management.view.repository') ||
              prevRouteName.includes('firewall.repository-report') ||
              prevRouteName.includes('firewall.containerRepositoryResults') ||
              prevRouteName.includes('hostedRepoComponents')
          );
        renderComponent();
        resetViewFiltersSpy.mockClear();

        // Remount (coming back from the results page)
        renderComponent();

        expect(resetViewFiltersSpy).not.toHaveBeenCalled();
      });
    });
  });

  describe('when the selected owner is a virtual repository manager (FIRE-665)', () => {
    const VRM_ID = 'vrm-1';

    beforeEach(() => {
      jest.spyOn(routerSelectors, 'selectIsRepositoryManager').mockReturnValue(true);
      jest.spyOn(routerSelectors, 'selectPrevStateIsRepositorySection').mockReturnValue(true);
      jest.spyOn(routerSelectors, 'selectIncludesManagementView').mockReturnValue(false);
      jest.spyOn(orgsAndPoliciesSelectors, 'selectSelectedOwner').mockReturnValue({
        id: VRM_ID,
        name: 'my-vrm',
        managerType: 'virtual',
        type: 'repository_manager',
      });
    });

    const renderVrmTile = ({ canEdit = true } = {}) => {
      jest.spyOn(ownerSummarySelectors, 'selectHasEditIqPermission').mockReturnValue(canEdit);
      return render(<RepositoriesConfigurationTile />);
    };

    it('renders the tile title as "Proxy Repositories" instead of "Configuration"', () => {
      renderVrmTile();
      expect(screen.getByRole('heading', { name: 'Proxy Repositories' })).toBeVisible();
      expect(screen.queryByRole('heading', { name: 'Configuration' })).toBeNull();
    });

    it('renders the "+ Add Proxy Repository" header button when the user has edit permission', () => {
      renderVrmTile({ canEdit: true });
      expect(screen.getByRole('button', { name: '+ Add Proxy Repository' })).toBeVisible();
    });

    it('does not render the Add button when the user lacks edit permission', () => {
      renderVrmTile({ canEdit: false });
      expect(screen.queryByRole('button', { name: /Add Proxy Repository/i })).toBeNull();
    });

    it('opens the AddProxyRepositoryModal when the header Add button is clicked', async () => {
      const user = userEvent.setup();
      renderVrmTile({ canEdit: true });
      await user.click(screen.getByRole('button', { name: '+ Add Proxy Repository' }));
      expect(screen.getByRole('heading', { name: 'Add Proxy Repository' })).toBeVisible();
    });

    it('renders the PCCS column header and applies the --with-pccs class to the table', () => {
      const { container } = renderVrmTile();
      expect(screen.getByText('PCCS')).toBeVisible();
      const table = container.querySelector('#iq-repositories-configuration-table');
      expect(table.classList.contains('iq-repositories-configuration-table--with-pccs')).toBe(true);
    });
  });

  describe('when the current route is the Virtual Repository Managers container (FIRE-665)', () => {
    beforeEach(() => {
      jest.spyOn(routerSelectors, 'selectIsRepositoryManager').mockReturnValue(false);
      jest.spyOn(routerSelectors, 'selectIsVirtualRepositoryContainer').mockReturnValue(true);
      jest.spyOn(routerSelectors, 'selectPrevStateIsRepositorySection').mockReturnValue(true);
      jest.spyOn(routerSelectors, 'selectIncludesManagementView').mockReturnValue(false);
      jest.spyOn(orgsAndPoliciesSelectors, 'selectSelectedOwner').mockReturnValue({
        id: 'virtual-container',
        name: 'Virtual Repository Managers',
        type: 'repository_container',
      });
    });

    const renderContainerTile = () => render(<RepositoriesConfigurationTile virtualOnly />);

    it('renders the tile title as "Proxy Repositories"', () => {
      renderContainerTile();
      expect(screen.getByRole('heading', { name: 'Proxy Repositories' })).toBeVisible();
    });

    it('renders the PCCS column header on the container listing', () => {
      renderContainerTile();
      expect(screen.getByText('PCCS')).toBeVisible();
    });

    it('does not render the info caption on the container listing', () => {
      renderContainerTile();
      expect(
        screen.queryByText('Policies applied to this Virtual Repository Manager govern all proxy repositories below.')
      ).toBeNull();
    });

    it('does not render the "+ Add Proxy Repository" button on the container listing', () => {
      renderContainerTile();
      expect(screen.queryByRole('button', { name: /Add Proxy Repository/i })).toBeNull();
    });
  });
});
