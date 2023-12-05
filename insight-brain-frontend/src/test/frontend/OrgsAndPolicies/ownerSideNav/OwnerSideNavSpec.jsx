/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { render, screen, axiosMockAdapter, fireEvent, within, waitFor } from 'TestRoot/SpecUtil';
import { nxTextInputStateHelpers } from '@sonatype/react-shared-components';
import { getOwnerListUrl, getPermissionContextTestUrl, getRepositoriesUrl } from 'MainRoot/util/CLMLocation';
import { getOwnersMap } from './nLevelMockData';
import OwnerSideNav from 'MainRoot/OrgsAndPolicies/ownerSideNav/OwnerSideNav';
import RouterStateContext from 'MainRoot/react/RouterStateContext';
import { PERMISSION } from 'MainRoot/util/authorizationUtil';
import { fakeRouterState, verifyOwnersMenuSection } from './ownerSideNavTestingUtils';
import { FILTER_DEBOUNCE } from 'MainRoot/OrgsAndPolicies/ownerSideNav/ownerSideNavSlice';

const { initialState: rscInitialState } = nxTextInputStateHelpers;

describe('OwnerSideNav', () => {
  let mockAxiosCalls;
  let state;
  let routerContext;
  const organizationsDepth = 4;
  const ownersMap = getOwnersMap(organizationsDepth);
  const topParentOrganizationId = 'ROOT_ORGANIZATION_ID';
  const ownerListPayload = { topParentOrganizationId, ownersMap };
  const repositoriesList = {
    repositories: [
      {
        oldestEvalTimestamp: null,
        managerInstanceId: '54342D8A-8FBE62A7-98C2B285-C37C2DC0-5FDBFC12',
        repository: {
          id: 'c192fc00375948dfbe1e8702ef6a3e44',
          repositoryManagerId: '17ee89ffd86649c49ce32f7d0328072a',
          publicId: 'maven-central',
          enabled: true,
          quarantineEnabled: false,
          format: 'maven2',
          repositoryType: 'maven',
        },
      },
      {
        oldestEvalTimestamp: null,
        managerInstanceId: '54342D8A-8FBE62A7-98C2B285-C37C2DC0-5FDBFC12',
        repository: {
          id: 'c192fc00375948dfbe1e8702ef6a3e45',
          repositoryManagerId: '17ee89ffd86649c49ce32f7d0328072a',
          publicId: 'maven-central',
          enabled: true,
          quarantineEnabled: false,
          format: 'maven2',
          repositoryType: 'maven',
        },
      },
      {
        oldestEvalTimestamp: null,
        managerInstanceId: '54342D8A-8FBE62A7-98C2B285-C37C2DC0-5FDBFC12',
        repository: {
          id: 'c192fc00375948dfbe1e8702ef6a3e46',
          repositoryManagerId: '17ee89ffd86649c49ce32f7d0328072a',
          publicId: 'maven-central',
          enabled: true,
          quarantineEnabled: false,
          format: 'maven2',
          repositoryType: 'maven',
        },
      },
    ],
  };
  const ownerType = 'repository_container';
  const permissionsPayload = [PERMISSION.READ];
  const ownerListUrl = getOwnerListUrl();
  const permissionContextTestUrl = getPermissionContextTestUrl(ownerType);
  const addOrgsAndApps = (organization) => {
    const organizations = (organization.organizationIds || []).map((id) => ownersMap[id]);
    const applications = (organization.applicationIds || []).map((id) => ownersMap[id]);
    return { ...organization, organizations, applications };
  };

  beforeAll(() => {
    mockAxiosCalls = axiosMockAdapter();
  });

  beforeEach(() => {
    const selectedOrg = addOrgsAndApps(ownersMap[topParentOrganizationId]);
    state = {
      productFeatures: {
        productFeatures: {
          'orgs-and-apps': true,
        },
      },
      router: {
        currentParams: {},
        currentState: { name: 'management.view.organization' },
      },
      orgsAndPolicies: {
        ownerSideNav: {
          filterQuery: rscInitialState(''),
          filteredEntries: {
            applications: [],
            organizations: [],
          },
          displayedOrganization: {
            type: 'organization',
            id: selectedOrg.id,
            name: selectedOrg.name,
          },
        },
      },
    };

    routerContext = { href: null };
    spyOn(routerContext, 'href').and.callFake(fakeRouterState);

    mockAxiosCalls.onGet(ownerListUrl).reply(200, ownerListPayload);
    mockAxiosCalls.onPut(permissionContextTestUrl).reply(200, []);
    mockAxiosCalls.onGet(getRepositoriesUrl()).reply(200, repositoriesList);
  });

  const renderComponent = (preloadedState = state, router = routerContext) => {
    return render(
      <RouterStateContext.Provider value={router}>
        <OwnerSideNav />
      </RouterStateContext.Provider>,
      { preloadedState }
    );
  };

  it('renders loading indicator and handles error', async () => {
    mockAxiosCalls.reset();

    // ownerListUrl request error
    mockAxiosCalls.onGet(ownerListUrl).replyOnce(404).onGet(ownerListUrl).reply(200, ownerListPayload);
    renderComponent();
    expect(screen.getByText('Loading…')).toBeVisible();
    expect(await screen.findByRole('alert', /An error occurred loading data. Error 404/i)).toBeVisible();

    // no errors
    fireEvent.click(screen.getByRole('button', { name: 'Retry' }));
    const searchInput = await screen.findByRole('textbox');
    expect(searchInput).toBeVisible();
    expect(searchInput).toHaveTextContent('');
  });

  describe('Search', () => {
    it('renders empty search input', async () => {
      renderComponent();
      const searchInput = await screen.findByRole('textbox');
      expect(searchInput).toBeVisible();
      expect(searchInput).toHaveTextContent('');
    });

    it('renders clarification message if search term is less than 3 chars long', async () => {
      renderComponent();
      const searchInput = await screen.findByRole('textbox');

      fireEvent.change(searchInput, { target: { value: 'te' } });

      const message = screen.getByText('Enter three characters to begin filtering.');
      expect(message).toBeVisible();
    });

    it('does not render clarification message if search term is longer than 3 chars', async () => {
      renderComponent();
      const searchInput = await screen.findByRole('textbox');

      fireEvent.change(searchInput, { target: { value: 'term' } });

      const message = screen.queryByText('Enter three characters to begin filtering.');
      expect(message).not.toBeInTheDocument();
    });

    it('renders no results found if there is no entries found', async () => {
      jasmine.clock().install();
      jasmine.clock().mockDate();

      renderComponent();

      const searchInput = await screen.findByRole('textbox');

      fireEvent.change(searchInput, { target: { value: 'ababahalamaha' } });

      jasmine.clock().tick(FILTER_DEBOUNCE);
      jasmine.clock().uninstall();

      const noResults = await screen.findByText('No Results Found');
      expect(noResults).toBeVisible();
    });

    it('renders matching results', async () => {
      jasmine.clock().install();
      jasmine.clock().mockDate();

      const rootOrg = ownersMap[topParentOrganizationId];
      state = {
        productFeatures: {
          productFeatures: {
            'orgs-and-apps': true,
          },
        },
        router: {
          currentState: {
            name: 'management.view.organization',
          },
          currentParams: {
            organizationId: rootOrg.id,
          },
        },
        orgsAndPolicies: {
          ownerSideNav: {
            filterQuery: rscInitialState(''),
            filteredEntries: {
              applications: [],
              organizations: [],
            },
            displayedOrganization: {
              type: 'organization',
              id: rootOrg.id,
              name: rootOrg.name,
            },
          },
        },
      };
      renderComponent();

      const searchInput = await screen.findByRole('textbox');

      fireEvent.change(searchInput, { target: { value: 'name 3' } });

      const filteredHeader = screen.getByText('Filtered Results:');
      expect(filteredHeader).toBeVisible();

      jasmine.clock().tick(FILTER_DEBOUNCE);
      jasmine.clock().uninstall();

      const results = await screen.findAllByRole('menuitem');
      expect(results).toHaveSize(5);

      expect(results[0]).toHaveTextContent('organization name 3');
      expect(results[1]).toHaveTextContent('application name 3 at organization 1');
      expect(results[2]).toHaveTextContent('application name 3 at organization 2');
      expect(results[3]).toHaveTextContent('application name 3 at organization 3');
      expect(results[4]).toHaveTextContent('application name 3 at organization 4');
    });
  });

  describe('Current owner is Root Organization', () => {
    let selectedOrg;
    beforeEach(() => {
      selectedOrg = ownersMap[topParentOrganizationId];
      state = {
        productFeatures: {
          productFeatures: {
            'orgs-and-apps': true,
          },
        },
        router: {
          currentState: {
            name: 'management.view.organization',
          },
          currentParams: {
            organizationId: selectedOrg.id,
          },
        },
        orgsAndPolicies: {
          ownerSideNav: {
            filterQuery: rscInitialState(''),
            filteredEntries: {
              applications: [],
              organizations: [],
            },
            displayedOrganization: {
              type: 'organization',
              id: selectedOrg.id,
              name: selectedOrg.name,
            },
          },
        },
      };
    });

    it('renders no breadcrumb', async () => {
      renderComponent();
      const currentOrg = await screen.findByText(/ROOT_ORGANIZATION_NAME/i);
      expect(currentOrg).toBeVisible();

      const navigation = screen.queryByRole('navigation', {
        name: /breadcrumbs/i,
      });
      expect(navigation).toBe(null);
    });

    describe('owner sidenav header', () => {
      it("renders current organization's name as a paragraph with selected item class", async () => {
        renderComponent();
        const currentOrg = await screen.findByText(/ROOT_ORGANIZATION_NAME/i);
        expect(currentOrg).toBeVisible();
        expect(currentOrg).toHaveClassName('iq-navbar-item iq-selected-org active');
      });
    });

    describe('owner sidenav content', () => {
      describe('repositories menu item', () => {
        beforeEach(() => {
          mockAxiosCalls.onPut(permissionContextTestUrl).reply(200, permissionsPayload);
        });

        it('renders Repositories link with the correct counter value', async () => {
          renderComponent();
          const goToRepositoriesLink = await screen.findByRole('link', { name: /(Repositories)/ });
          expect(goToRepositoriesLink).toHaveClassName('iq-navbar-item iq-repositories-link');
          expect(goToRepositoriesLink).toBeVisible();
          expect(goToRepositoriesLink).toHaveTextContent('(3)');
        });

        it('contains correct href to navigate to repository configuration', async () => {
          renderComponent();
          const goToRepositoriesLink = await screen.findByRole('link', { name: /(Repositories)/ });
          expect(goToRepositoriesLink).toBeVisible();
          expect(goToRepositoriesLink).toHaveAttribute('href');
        });
      });

      describe('children menu items', () => {
        it('renders only child organizations collapsible menu sections', async () => {
          renderComponent();
          const childMenus = await screen.findAllByRole('menu');
          expect(childMenus).toHaveSize(1);
        });

        it('child organizations menu sections is not empty ', async () => {
          renderComponent();
          const childMenus = await screen.findAllByRole('group');
          const childOrganizations = ownersMap[topParentOrganizationId].organizationIds.map((id) => ownersMap[id]);
          verifyOwnersMenuSection(childMenus[0], childOrganizations, 'organization');
        });

        it('renders children counter', async () => {
          SpecUtil.requestIdleCallbackInvokeImmediate();
          renderComponent();
          const childMenus = await screen.findAllByRole('menu');
          const menuItems = within(childMenus[0]).getAllByRole('menuitem');
          expect(menuItems[0]).toHaveTextContent('organization name 1(5');
        });
      });
    });

    describe('owner sidenav footer', () => {
      it('renders tree view link', async () => {
        renderComponent();
        const goToTreeViewLink = await screen.findByRole('link', { name: 'Tree View' });
        expect(goToTreeViewLink).toHaveClassName('nx-btn nx-btn--tertiary');
        expect(goToTreeViewLink).toBeVisible();
      });

      it('contains correct href to navigate to tree view page', async () => {
        renderComponent();
        const goToTreeViewLink = await screen.findByRole('link', { name: 'Tree View' });
        expect(goToTreeViewLink).toHaveClassName('nx-btn nx-btn--tertiary');
        expect(goToTreeViewLink).toHaveAttribute('href', '#/management/view/tree');
      });
    });
  });

  describe('Current owner is Repositories', () => {
    let selectedOrg;
    beforeEach(() => {
      selectedOrg = ownersMap[topParentOrganizationId];
      state = {
        productFeatures: {
          productFeatures: {
            'orgs-and-apps': true,
          },
        },
        router: {
          currentState: {
            name: 'management.view.repositories',
          },
          currentParams: {},
        },
        orgsAndPolicies: {
          ownerSideNav: {
            filterQuery: rscInitialState(''),
            filteredEntries: {
              applications: [],
              organizations: [],
            },
            displayedOrganization: {
              type: 'organization',
              id: selectedOrg.id,
              name: selectedOrg.name,
            },
          },
        },
      };
      mockAxiosCalls.onPut(permissionContextTestUrl).reply(200, permissionsPayload);
    });

    it('renders breadcrumb', async () => {
      renderComponent();

      const navigation = await screen.findByRole('navigation', {
        name: /breadcrumbs/i,
      });

      const ancestorBreadcrumb = within(navigation).getByText('ROOT_ORGANIZATION_NAME');
      expect(ancestorBreadcrumb).toBeVisible();
      expect(ancestorBreadcrumb.closest('a')).toHaveAttribute(
        'href',
        '#/management/view/organization/ROOT_ORGANIZATION_ID'
      );

      const currentBreadcrumb = within(navigation).getByText('Repositories');
      expect(currentBreadcrumb).toBeVisible();
      expect(currentBreadcrumb.closest('a')).not.toHaveAttribute('href');
    });

    describe('owner sidenav header', () => {
      it("renders current organization's name as a link with displayed organization class", async () => {
        renderComponent();
        const navbarHeader = await screen.findByTestId('sidebar-header');
        const currentOrgLink = within(navbarHeader).getByRole('link', { name: selectedOrg.name });
        expect(currentOrgLink).toBeVisible();
        expect(currentOrgLink).toHaveClassName('iq-navbar-item iq-selected-org');
        expect(currentOrgLink).toHaveAttribute('href', `#/management/view/organization/${selectedOrg.id}`);
      });
    });

    describe('owner sidenav content', () => {
      describe('repositories menu item', () => {
        it('renders Repositories link with selected item class', async () => {
          renderComponent();
          const goToRepositoriesLink = await screen.findByRole('link', { name: /(Repositories)/ });
          expect(goToRepositoriesLink).toHaveClassName('iq-navbar-item iq-repositories-link active');
          expect(goToRepositoriesLink).toBeVisible();
        });

        it('contains correct href to navigate to repository configuration', async () => {
          renderComponent();
          const goToRepositoriesLink = await screen.findByRole('link', { name: /(Repositories)/ });
          expect(goToRepositoriesLink).toHaveAttribute('href');
        });
      });

      describe('children menu items', () => {
        it('renders only child organizations collapsible menu sections', async () => {
          renderComponent();
          const childMenus = await screen.findAllByRole('menu');
          expect(childMenus).toHaveSize(1);
        });

        it('child organizations menu sections is not empty ', async () => {
          renderComponent();
          const childMenus = await screen.findAllByRole('group');
          const childOrganizations = ownersMap[topParentOrganizationId].organizationIds.map((id) => ownersMap[id]);
          verifyOwnersMenuSection(childMenus[0], childOrganizations, 'organization');
        });
      });
    });

    describe('owner sidenav footer', () => {
      it('renders tree view link', async () => {
        renderComponent();
        const goToTreeViewLink = await screen.findByRole('link', { name: 'Tree View' });
        expect(goToTreeViewLink).toHaveClassName('nx-btn nx-btn--tertiary');
        expect(goToTreeViewLink).toBeVisible();
      });

      it('contains correct href to navigate to tree view page', async () => {
        renderComponent();
        const goToTreeViewLink = await screen.findByRole('link', { name: 'Tree View' });
        expect(goToTreeViewLink).toHaveClassName('nx-btn nx-btn--tertiary');
        expect(goToTreeViewLink).toHaveAttribute('href', '#/management/view/tree');
      });
    });
  });

  describe('Current owner is an Organization', () => {
    let selectedOrg;
    beforeEach(() => {
      selectedOrg = ownersMap[ownersMap[topParentOrganizationId].organizationIds[0]];
      state = {
        router: {
          currentState: {
            name: 'management.view.organization',
          },
          currentParams: {
            organizationId: selectedOrg.id,
          },
        },
        productFeatures: {
          productFeatures: {
            'orgs-and-apps': true,
            'saas-lifecycle-scm-enabled': true,
          },
        },
        orgsAndPolicies: {
          ownerSideNav: {
            filterQuery: rscInitialState(''),
            filteredEntries: {
              applications: [],
              organizations: [],
            },
            displayedOrganization: {
              type: 'organization',
              id: selectedOrg.id,
              name: selectedOrg.name,
            },
          },
        },
      };
    });

    it('renders breadcrumb', async () => {
      renderComponent();

      const navigation = await screen.findByRole('navigation', {
        name: /breadcrumbs/i,
      });

      const ancestorBreadcrumb = within(navigation).getByText('ROOT_ORGANIZATION_NAME');
      expect(ancestorBreadcrumb).toBeVisible();
      expect(ancestorBreadcrumb.closest('a')).toHaveAttribute(
        'href',
        '#/management/view/organization/ROOT_ORGANIZATION_ID'
      );

      const currentBreadcrumb = within(navigation).getByText(selectedOrg.name);
      expect(currentBreadcrumb).toBeVisible();
      expect(currentBreadcrumb.closest('a')).not.toHaveAttribute('href');
    });

    describe('owner sidenav header', () => {
      it("renders current organization's name as a paragraph with selected item class", async () => {
        renderComponent();

        const matchingOrgNames = await screen.findAllByText(selectedOrg.name);
        // first match is the breadcrumb.
        expect(matchingOrgNames[1]).toBeVisible();
        expect(matchingOrgNames[1]).toHaveClassName('iq-navbar-item iq-selected-org active');
      });
    });

    describe('Add Organization', () => {
      it('should open owner modal when user clicks on plus button in Organization section', async () => {
        renderComponent();

        const buttons = await screen.findAllByRole('button');
        const organizationPlusButton = buttons[2];

        fireEvent.click(organizationPlusButton);

        let ownerModal;
        await waitFor(() => {
          ownerModal = document.querySelector('.nx-modal');
          expect(ownerModal).toBeVisible();
        });

        expect(within(ownerModal).getByText('New Organization')).toBeVisible();
      });
    });

    describe('Add Application dropdown', () => {
      it('should render two options', async () => {
        renderComponent();

        const buttons = await screen.findAllByRole('button');
        const applicationPlusButton = buttons[4];

        fireEvent.click(applicationPlusButton);

        expect(await screen.findByRole('button', { name: 'New Application' })).toBeVisible();
        const importAppLink = await screen.findByRole('link', { name: 'Import Applications' });
        expect(importAppLink).toBeVisible();
        expect(importAppLink).toHaveAttribute('href', '#/onboarding/organization id 1');
      });

      it('should open owner modal when user clicks on New Application button', async () => {
        renderComponent();

        const buttons = await screen.findAllByRole('button');
        const applicationPlusButton = buttons[4];

        fireEvent.click(applicationPlusButton);

        const newApplicationBtn = await screen.findByRole('button', { name: 'New Application' });
        fireEvent.click(newApplicationBtn);

        let ownerModal;
        await waitFor(() => {
          ownerModal = document.querySelector('.nx-modal');
          expect(ownerModal).toBeVisible();
        });

        expect(within(ownerModal).getByText('New Application')).toBeVisible();
      });

      it('should not render the Import Application option when saas-lifecycle-scm-enabled is false', async function () {
        state.productFeatures.productFeatures['saas-lifecycle-scm-enabled'] = false;
        renderComponent();

        const applicationPlusButton = await screen.findByRole('button', { name: 'Add Application' });

        fireEvent.click(applicationPlusButton);

        expect(await screen.findByRole('button', { name: 'New Application' })).toBeVisible();
        expect(screen.queryByRole('link', { name: 'Import Applications' })).not.toBeInTheDocument();
      });

      it('should not render the Import Application option when saas-lifecycle-scm-enabled is missing', async function () {
        delete state.productFeatures.productFeatures['saas-lifecycle-scm-enabled'];
        renderComponent();

        const applicationPlusButton = await screen.findByRole('button', { name: 'Add Application' });

        fireEvent.click(applicationPlusButton);

        expect(await screen.findByRole('button', { name: 'New Application' })).toBeVisible();
        expect(screen.queryByRole('link', { name: 'Import Applications' })).not.toBeInTheDocument();
      });
    });

    describe('owner sidenav content', () => {
      describe('repositories menu item', () => {
        it('is not rendered', async () => {
          renderComponent();
          const goToRepositoriesLink = await screen.queryByRole('link', { name: 'Repositories' });
          expect(goToRepositoriesLink).toBeNull();
        });
      });

      describe('children menu items', () => {
        it('renders child organizations and applications collapsible menu sections', async () => {
          renderComponent();
          const childMenus = await screen.findAllByRole('menu');
          expect(childMenus).toHaveSize(2);
        });

        it('child organizations menu section contains links to each child', async () => {
          renderComponent();
          const childMenus = await screen.findAllByRole('group');
          const childOrganization = selectedOrg.organizationIds.map((id) => ownersMap[id]);
          verifyOwnersMenuSection(childMenus[0], childOrganization, 'organization');
        });

        it('child applications menu section contains links to each child', async () => {
          renderComponent();
          const childMenus = await screen.findAllByRole('group');
          const childApplications = selectedOrg.applicationIds.map((id) => ownersMap[id]);
          verifyOwnersMenuSection(childMenus[1], childApplications, 'application');
        });
      });
    });

    describe('owner sidenav footer', () => {
      it('renders tree view link', async () => {
        renderComponent();
        const goToTreeViewLink = await screen.findByRole('link', { name: 'Tree View' });
        expect(goToTreeViewLink).toHaveClassName('nx-btn nx-btn--tertiary');
        expect(goToTreeViewLink).toBeVisible();
      });

      it('contains correct href to navigate to tree view page', async () => {
        renderComponent();
        const goToTreeViewLink = await screen.findByRole('link', { name: 'Tree View' });
        expect(goToTreeViewLink).toHaveClassName('nx-btn nx-btn--tertiary');
        expect(goToTreeViewLink).toHaveAttribute('href', '#/management/view/tree');
      });
    });
  });

  describe('Current owner is an Application', () => {
    let selectedOrg, selectedApp;
    beforeEach(() => {
      selectedOrg = ownersMap[ownersMap[topParentOrganizationId].organizationIds[0]];
      selectedApp = ownersMap[selectedOrg.applicationIds[0]];
      state = {
        productFeatures: {
          productFeatures: {
            'orgs-and-apps': true,
          },
        },
        router: {
          currentState: {
            name: 'management.view.application',
          },
          currentParams: {
            applicationPublicId: selectedApp.publicId,
          },
        },
        orgsAndPolicies: {
          root: {
            selectedOwner: {
              id: selectedApp.id,
            },
          },
          ownerSideNav: {
            filterQuery: rscInitialState(''),
            filteredEntries: {
              applications: [],
              organizations: [],
            },
            displayedOrganization: {
              type: 'organization',
              id: selectedOrg.id,
              name: selectedOrg.name,
            },
          },
        },
      };
    });

    describe('owner sidenav header', () => {
      it("renders current organization's name as a link with displayed organization class", async () => {
        renderComponent();
        const navbarHeader = await screen.findByTestId('sidebar-header');
        const currentOrgLink = within(navbarHeader).getByRole('link', { name: selectedOrg.name });
        expect(currentOrgLink).toBeVisible();
        expect(currentOrgLink).toHaveClassName('iq-navbar-item iq-selected-org');
        expect(currentOrgLink).toHaveAttribute('href', `#/management/view/organization/${selectedOrg.id}`);
      });
    });

    describe('owner sidenav content', () => {
      describe('repositories menu item', () => {
        it('is not rendered', async () => {
          renderComponent();
          const goToRepositoriesLink = await screen.queryByRole('link', { name: 'Repositories' });
          expect(goToRepositoriesLink).toBeNull();
        });
      });

      describe('children menu items', () => {
        it('renders child organizations and applications collapsible menu sections', async () => {
          renderComponent();
          const childMenus = await screen.findAllByRole('menu');
          expect(childMenus).toHaveSize(2);
        });

        it('child organizations menu section contains links to each child', async () => {
          renderComponent();
          const childMenus = await screen.findAllByRole('group');
          const childOrganization = selectedOrg.organizationIds.map((id) => ownersMap[id]);
          verifyOwnersMenuSection(childMenus[0], childOrganization, 'organization');
        });

        it('child applications menu section contains links to each child and selected app is marked', async () => {
          renderComponent();
          const childMenus = await screen.findAllByRole('group');
          const childApplications = selectedOrg.applicationIds.map((id) => ownersMap[id]);
          verifyOwnersMenuSection(childMenus[1], childApplications, 'application', selectedApp);
        });
      });
    });

    describe('owner sidenav footer', () => {
      it('renders tree view link', async () => {
        renderComponent();
        const goToTreeViewLink = await screen.findByRole('link', { name: 'Tree View' });
        expect(goToTreeViewLink).toHaveClassName('nx-btn nx-btn--tertiary');
        expect(goToTreeViewLink).toBeVisible();
      });

      it('contains correct href to navigate to tree view page', async () => {
        renderComponent();
        const goToTreeViewLink = await screen.findByRole('link', { name: 'Tree View' });
        expect(goToTreeViewLink).toHaveClassName('nx-btn nx-btn--tertiary');
        expect(goToTreeViewLink).toHaveAttribute('href', '#/management/view/tree');
      });
    });
  });

  describe('Current owner is an Application, but parent org does not have child orgs', () => {
    let selectedOrg, selectedApp;
    beforeEach(() => {
      selectedOrg = ownersMap[ownersMap[topParentOrganizationId].organizationIds[0]];
      selectedOrg.organizationIds = [];
      selectedApp = ownersMap[selectedOrg.applicationIds[0]];
      state = {
        productFeatures: {
          productFeatures: {
            'orgs-and-apps': true,
          },
        },
        router: {
          currentState: {
            name: 'management.view.application',
          },
          currentParams: {
            applicationPublicId: selectedApp.publicId,
          },
        },
        orgsAndPolicies: {
          root: {
            selectedOwner: {
              id: selectedApp.id,
            },
          },
          ownerSideNav: {
            filterQuery: rscInitialState(''),
            filteredEntries: {
              applications: [],
              organizations: [],
            },
            displayedOrganization: {
              type: 'organization',
              id: selectedOrg.id,
              name: selectedOrg.name,
            },
          },
        },
      };

      mockAxiosCalls.reset();
      const newOwnerListPayload = { topParentOrganizationId, ownersMap };
      mockAxiosCalls.onGet(ownerListUrl).reply(200, newOwnerListPayload);
    });

    describe('owner sidenav header', () => {
      it("renders current organization's name as a link with displayed organization class", async () => {
        renderComponent();
        const navbarHeader = await screen.findByTestId('sidebar-header');
        const currentOrgLink = within(navbarHeader).getByRole('link', { name: selectedOrg.name });
        expect(currentOrgLink).toBeVisible();
        expect(currentOrgLink).toHaveClassName('iq-navbar-item iq-selected-org');
        expect(currentOrgLink).toHaveAttribute('href', `#/management/view/organization/${selectedOrg.id}`);
      });
    });

    describe('owner sidenav content', () => {
      describe('repositories menu item', () => {
        it('is not rendered', async () => {
          renderComponent();
          const goToRepositoriesLink = await screen.queryByRole('link', { name: 'Repositories' });
          expect(goToRepositoriesLink).toBeNull();
        });
      });

      describe('children menu items', () => {
        it('renders only applications collapsible menu sections', async () => {
          renderComponent();
          const childMenus = await screen.findAllByRole('menu');

          expect(within(childMenus[0]).queryAllByRole('menuitem')).toHaveSize(0);
          expect(within(childMenus[1]).getAllByRole('menuitem')).toHaveSize(4);
        });

        it('child applications menu section contains links to each child and selected app is marked', async () => {
          renderComponent();
          const childMenus = await screen.findAllByRole('group');
          const childApplications = selectedOrg.applicationIds.map((id) => ownersMap[id]);
          verifyOwnersMenuSection(childMenus[1], childApplications, 'application', selectedApp);
        });
      });
    });

    describe('owner sidenav footer', () => {
      it('renders tree view link', async () => {
        renderComponent();
        const goToTreeViewLink = await screen.findByRole('link', { name: 'Tree View' });
        expect(goToTreeViewLink).toHaveClassName('nx-btn nx-btn--tertiary');
        expect(goToTreeViewLink).toBeVisible();
      });

      it('contains correct href to navigate to tree view page', async () => {
        renderComponent();
        const goToTreeViewLink = await screen.findByRole('link', { name: 'Tree View' });
        expect(goToTreeViewLink).toHaveClassName('nx-btn nx-btn--tertiary');
        expect(goToTreeViewLink).toHaveAttribute('href', '#/management/view/tree');
      });
    });
  });

  describe('Current owner is an Organization with no children', () => {
    let selectedOrg;
    beforeEach(() => {
      selectedOrg = ownersMap[ownersMap[topParentOrganizationId].organizationIds[0]];
      selectedOrg.organizations = null;
      selectedOrg.applications = null;
      state = {
        productFeatures: {
          productFeatures: {
            'orgs-and-apps': true,
          },
        },
        router: {
          currentState: {
            name: 'management.view.organization',
          },
          currentParams: {
            organizationId: selectedOrg.id,
          },
        },
        orgsAndPolicies: {
          ownerSideNav: {
            filterQuery: rscInitialState(''),
            filteredEntries: {
              applications: [],
              organizations: [],
            },
            displayedOrganization: {
              type: 'organization',
              id: selectedOrg.id,
              name: selectedOrg.name,
            },
          },
        },
      };

      mockAxiosCalls.reset();
      const newOwnerListPayload = { topParentOrganizationId, ownersMap };
      mockAxiosCalls.onGet(ownerListUrl).reply(200, newOwnerListPayload);
    });

    describe('owner sidenav header', () => {
      it("renders current organization's name as a paragraph with selected item class", async () => {
        renderComponent();
        const matchingOrgNames = await screen.findAllByText(selectedOrg.name);
        // first match is the breadcrumb.
        expect(matchingOrgNames[1]).toBeVisible();
        expect(matchingOrgNames[1]).toHaveClassName('iq-navbar-item iq-selected-org active');
      });
    });

    describe('owner sidenav content', () => {
      describe('repositories menu item', () => {
        it('is not rendered', async () => {
          renderComponent();
          const goToRepositoriesLink = await screen.queryByRole('link', { name: 'Repositories' });
          expect(goToRepositoriesLink).toBeNull();
        });
      });

      it('first menu section is for child organizations, but renders only disabled trigger button', async () => {
        renderComponent();
        const goToTreeViewLink = await screen.findByRole('link', { name: 'Tree View' });
        expect(goToTreeViewLink).toHaveClassName('nx-btn nx-btn--tertiary');
        expect(goToTreeViewLink).toBeVisible();
      });

      it('contains correct href to navigate to tree view page', async () => {
        renderComponent();
        const goToTreeViewLink = await screen.findByRole('link', { name: 'Tree View' });
        expect(goToTreeViewLink).toHaveClassName('nx-btn nx-btn--tertiary');
        expect(goToTreeViewLink).toHaveAttribute('href', '#/management/view/tree');
      });
    });
  });

  describe('FirewallOnlyLicense', () => {
    it('renders components when not FirewallOnlyLicense', async () => {
      const rootOrg = ownersMap[topParentOrganizationId];
      state = {
        productFeatures: {
          productFeatures: {
            'orgs-and-apps': true,
          },
        },
        router: {
          currentState: {
            name: 'management.view.organization',
          },
          currentParams: {
            organizationId: rootOrg.id,
          },
        },
        orgsAndPolicies: {
          ownerSideNav: {
            filterQuery: rscInitialState(''),
            filteredEntries: {
              applications: [],
              organizations: [],
            },
            displayedOrganization: {
              type: 'organization',
              id: rootOrg.id,
              name: rootOrg.name,
            },
          },
        },
      };
      renderComponent();
      const currentOrg = await screen.findByText(/ROOT_ORGANIZATION_NAME/i);
      expect(currentOrg).toBeVisible();

      expect(screen.queryByPlaceholderText('Org or App Name')).not.toBeNull();
      expect(screen.queryByTestId('organizations-add')).not.toBeNull();
      expect(screen.queryByText('Tree View')).not.toBeNull();
    });
    it('does not render components when FirewallOnlyLicense', async () => {
      const rootOrg = ownersMap[topParentOrganizationId];
      state = {
        productFeatures: {
          productFeatures: {},
        },
        router: {
          currentState: {
            name: 'management.view.organization',
          },
          currentParams: {
            organizationId: rootOrg.id,
          },
        },
        orgsAndPolicies: {
          ownerSideNav: {
            filterQuery: rscInitialState(''),
            filteredEntries: {
              applications: [],
              organizations: [],
            },
            displayedOrganization: {
              type: 'organization',
              id: rootOrg.id,
              name: rootOrg.name,
            },
          },
        },
      };
      renderComponent();
      const currentOrg = await screen.findByText(/ROOT_ORGANIZATION_NAME/i);
      expect(currentOrg).toBeVisible();

      expect(screen.queryByPlaceholderText('Org or App Name')).toBeNull();
      expect(screen.queryByTestId('organizations-add')).toBeNull();
      expect(screen.queryByText('Tree View')).toBeNull();
    });
  });
});
