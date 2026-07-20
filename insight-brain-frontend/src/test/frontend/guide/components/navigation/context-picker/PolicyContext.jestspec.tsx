/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import type { ReactNode } from 'react';
import { OwnerAdapterProvider } from 'GuideRoot/components/navigation/context-picker/OwnerAdapterProvider';
import {
  PolicyContextProvider,
  usePolicyContext,
} from 'GuideRoot/components/navigation/context-picker/PolicyContext';
import type { OwnerAdapter } from 'GuideRoot/components/navigation/context-picker/OwnerAdapter';
import type { Owner } from 'GuideRoot/components/navigation/context-picker/types';
import { getOwnerScope, _resetOwnerScopeForTests } from 'GuideRoot/api/ownerScope';

const STORAGE_KEY = 'guide.policyOwner';

const paymentsOrg: Owner = {
  id: 'payments',
  publicId: 'payments',
  name: 'Payments',
  type: 'org',
  ancestorPath: [],
};

function stubAdapter(resolve: (id: string) => Promise<Owner | null>): OwnerAdapter {
  return {
    getTopOrgs: jest.fn(),
    getAppsForOrg: jest.fn(),
    searchOwners: jest.fn(),
    cancelSearch: jest.fn(),
    resolveOwner: jest.fn(resolve),
  } as unknown as OwnerAdapter;
}

function Probe() {
  const { activeOwner, hydrated, setActiveOwner } = usePolicyContext();
  return (
    <div>
      <div>hydrated:{String(hydrated)}</div>
      <div>owner:{activeOwner?.id ?? 'root'}</div>
      <button onClick={() => setActiveOwner(paymentsOrg)}>select-payments</button>
      <button onClick={() => setActiveOwner(null)}>select-root</button>
    </div>
  );
}

function renderWithAdapter(adapter: OwnerAdapter, children: ReactNode = <Probe />) {
  return render(
    <OwnerAdapterProvider adapter={adapter}>
      <PolicyContextProvider>{children}</PolicyContextProvider>
    </OwnerAdapterProvider>
  );
}

describe('PolicyContext', () => {
  afterEach(() => {
    localStorage.clear();
    _resetOwnerScopeForTests();
    jest.restoreAllMocks();
  });

  it('is hydrated immediately and stays root when nothing is stored', async () => {
    renderWithAdapter(stubAdapter(async () => null));
    expect(screen.getByText('hydrated:true')).toBeInTheDocument();
    expect(screen.getByText('owner:root')).toBeInTheDocument();
    expect(getOwnerScope()).toBeNull();
  });

  it('selecting an owner writes localStorage and updates ownerScope synchronously', async () => {
    renderWithAdapter(stubAdapter(async () => null));
    await userEvent.click(screen.getByRole('button', { name: 'select-payments' }));
    expect(screen.getByText('owner:payments')).toBeInTheDocument();
    expect(getOwnerScope()).toBe('payments');
    expect(JSON.parse(localStorage.getItem(STORAGE_KEY) as string)).toBe('payments');
  });

  it('selecting root clears localStorage and ownerScope', async () => {
    localStorage.setItem(STORAGE_KEY, JSON.stringify('payments'));
    renderWithAdapter(stubAdapter(async () => paymentsOrg));
    await screen.findByText('owner:payments');
    await userEvent.click(screen.getByRole('button', { name: 'select-root' }));
    expect(screen.getByText('owner:root')).toBeInTheDocument();
    expect(getOwnerScope()).toBeNull();
    expect(localStorage.getItem(STORAGE_KEY)).toBeNull();
  });

  it('rehydrates a stored owner: hydrated is false until resolveOwner settles, then true', async () => {
    localStorage.setItem(STORAGE_KEY, JSON.stringify('payments'));
    const adapter = stubAdapter(async () => paymentsOrg);
    renderWithAdapter(adapter);
    // Synchronous first render: a stored id exists, so not yet hydrated.
    expect(screen.getByText('hydrated:false')).toBeInTheDocument();
    await screen.findByText('hydrated:true');
    expect(screen.getByText('owner:payments')).toBeInTheDocument();
    expect(getOwnerScope()).toBe('payments');
    expect(adapter.resolveOwner).toHaveBeenCalledWith('payments');
  });

  it('stale selection (resolveOwner returns null) clears localStorage and falls back to root', async () => {
    localStorage.setItem(STORAGE_KEY, JSON.stringify('deleted-org'));
    renderWithAdapter(stubAdapter(async () => null));
    await screen.findByText('hydrated:true');
    expect(screen.getByText('owner:root')).toBeInTheDocument();
    expect(localStorage.getItem(STORAGE_KEY)).toBeNull();
    expect(getOwnerScope()).toBeNull();
  });

  it('becomes hydrated even if resolveOwner rejects (transient error), keeping the stored value', async () => {
    localStorage.setItem(STORAGE_KEY, JSON.stringify('payments'));
    renderWithAdapter(stubAdapter(async () => { throw new Error('network'); }));
    await screen.findByText('hydrated:true');
    expect(screen.getByText('owner:root')).toBeInTheDocument();
    // Transient (non-404) failure keeps the stored value for a later attempt.
    expect(localStorage.getItem(STORAGE_KEY)).not.toBeNull();
  });

  it('falls back to root and stays hydrated when localStorage.getItem throws', async () => {
    jest.spyOn(Storage.prototype, 'getItem').mockImplementation(() => {
      throw new Error('SecurityError');
    });
    renderWithAdapter(stubAdapter(async () => null));
    expect(screen.getByText('hydrated:true')).toBeInTheDocument();
    expect(screen.getByText('owner:root')).toBeInTheDocument();
  });

  it('becomes hydrated and falls back to root when the stored value is unparseable', async () => {
    localStorage.setItem(STORAGE_KEY, 'not-json{');
    renderWithAdapter(stubAdapter(async () => null));
    await screen.findByText('hydrated:true');
    expect(screen.getByText('owner:root')).toBeInTheDocument();
  });
});
