/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { useEffect } from 'react';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { OwnerAdapterProvider } from 'GuideRoot/components/navigation/context-picker/OwnerAdapterProvider';
import {
  PolicyContextProvider,
  usePolicyContext,
} from 'GuideRoot/components/navigation/context-picker/PolicyContext';
import type { OwnerAdapter } from 'GuideRoot/components/navigation/context-picker/OwnerAdapter';
import type { Owner } from 'GuideRoot/components/navigation/context-picker/types';
import { PolicyScopeBoundary } from 'GuideRoot/layout/PolicyScopeBoundary';
import { _resetOwnerScopeForTests } from 'GuideRoot/api/ownerScope';

const STORAGE_KEY = 'guide.policyOwner';

const paymentsOrg: Owner = {
  id: 'payments', publicId: 'payments', name: 'Payments', type: 'org', ancestorPath: [],
};

function stubAdapter(resolve: (id: string) => Promise<Owner | null>): OwnerAdapter {
  return {
    getTopOrgs: jest.fn(), getAppsForOrg: jest.fn(), searchOwners: jest.fn(),
    cancelSearch: jest.fn(), resolveOwner: jest.fn(resolve),
  } as unknown as OwnerAdapter;
}

let mountCount = 0;
function Child() {
  useEffect(() => {
    mountCount += 1;
  }, []);
  return <div>child-content</div>;
}

function Selector() {
  const { setActiveOwner } = usePolicyContext();
  return <button onClick={() => setActiveOwner(paymentsOrg)}>select-payments</button>;
}

function renderBoundary(adapter: OwnerAdapter) {
  return render(
    <OwnerAdapterProvider adapter={adapter}>
      <PolicyContextProvider>
        <Selector />
        <PolicyScopeBoundary>
          <Child />
        </PolicyScopeBoundary>
      </PolicyContextProvider>
    </OwnerAdapterProvider>
  );
}

describe('PolicyScopeBoundary', () => {
  beforeEach(() => {
    mountCount = 0;
  });
  afterEach(() => {
    localStorage.clear();
    _resetOwnerScopeForTests();
  });

  it('renders children immediately when there is no stored selection', () => {
    renderBoundary(stubAdapter(async () => null));
    expect(screen.getByText('child-content')).toBeInTheDocument();
  });

  it('withholds children until a stored selection resolves, then renders them', async () => {
    localStorage.setItem(STORAGE_KEY, JSON.stringify('payments'));
    renderBoundary(stubAdapter(async () => paymentsOrg));
    // Not hydrated yet on the synchronous first render.
    expect(screen.queryByText('child-content')).not.toBeInTheDocument();
    await screen.findByText('child-content');
  });

  it('remounts children when the owner changes', async () => {
    renderBoundary(stubAdapter(async () => null));
    await screen.findByText('child-content');
    expect(mountCount).toBe(1);
    await userEvent.click(screen.getByRole('button', { name: 'select-payments' }));
    await waitFor(() => expect(mountCount).toBe(2));
  });
});
