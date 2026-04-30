/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { createRef } from 'react';
import { render, screen, waitFor } from './test-utils';
import userEvent from '@testing-library/user-event';
import { Routes, Route, useLocation } from 'react-router';
import {
  useNavigate,
  useAdapterPathname,
  useAdapterSearchParams,
  useLink,
  useForm,
  type NavigationAdapter,
} from '@guide/ui-core';
import { useReactRouterAdapter } from 'GuideRoot/reactRouterAdapter';

function LocationDisplay() {
  const location = useLocation();
  return <span data-testid="location">{location.pathname}{location.search}</span>;
}

describe('useReactRouterAdapter', () => {
  it('returns an object conforming to NavigationAdapter', () => {
    let adapter: NavigationAdapter | null = null;

    function Capture() {
      adapter = useReactRouterAdapter();
      return null;
    }

    render(<Capture />);

    expect(adapter).not.toBeNull();
    expect(typeof adapter!.navigate).toBe('function');
    expect(typeof adapter!.usePathname).toBe('function');
    expect(typeof adapter!.useSearchParams).toBe('function');
    expect(adapter!.Link).toBeDefined();
    expect(adapter!.Form).toBeDefined();
  });

  describe('navigate', () => {
    it('delegates to React Router useNavigate', async () => {
      function TestComponent() {
        const navigate = useNavigate();
        return <button onClick={() => navigate('/target')}>Go</button>;
      }

      render(
        <Routes>
          <Route path="/" element={<TestComponent />} />
          <Route path="/target" element={<LocationDisplay />} />
        </Routes>,
      );

      await userEvent.setup().click(screen.getByRole('button', { name: 'Go' }));

      await waitFor(() => {
        expect(screen.getByTestId('location')).toHaveTextContent('/target');
      });
    });

    it('forwards query strings', async () => {
      function TestComponent() {
        const navigate = useNavigate();
        return <button onClick={() => navigate('/search?q=test&filter=active')}>Go</button>;
      }

      render(
        <Routes>
          <Route path="/" element={<TestComponent />} />
          <Route path="/search" element={<LocationDisplay />} />
        </Routes>,
      );

      await userEvent.setup().click(screen.getByRole('button', { name: 'Go' }));

      await waitFor(() => {
        expect(screen.getByTestId('location')).toHaveTextContent('/search?q=test&filter=active');
      });
    });
  });

  describe('useAdapterPathname', () => {
    it('returns useLocation().pathname', () => {
      function TestComponent() {
        const pathname = useAdapterPathname();
        return <span data-testid="pathname">{pathname}</span>;
      }

      render(
        <Routes>
          <Route path="/components/:ecosystem/:pkg/:version" element={<TestComponent />} />
        </Routes>,
        { routerOptions: { initialEntries: ['/components/npm/lodash/4.17.21'] } },
      );

      expect(screen.getByTestId('pathname')).toHaveTextContent('/components/npm/lodash/4.17.21');
    });

    it('returns / at root', () => {
      function TestComponent() {
        const pathname = useAdapterPathname();
        return <span data-testid="pathname">{pathname}</span>;
      }

      render(
        <Routes>
          <Route path="/" element={<TestComponent />} />
        </Routes>,
      );

      expect(screen.getByTestId('pathname')).toHaveTextContent('/');
    });
  });

  describe('useAdapterSearchParams', () => {
    it('returns React Router useSearchParams()[0]', () => {
      function TestComponent() {
        const params = useAdapterSearchParams();
        return (
          <div>
            <span data-testid="q">{params.get('q')}</span>
            <span data-testid="filter">{params.get('filter')}</span>
          </div>
        );
      }

      render(
        <Routes>
          <Route path="/search" element={<TestComponent />} />
        </Routes>,
        { routerOptions: { initialEntries: ['/search?q=lodash&filter=vulnerabilities'] } },
      );

      expect(screen.getByTestId('q')).toHaveTextContent('lodash');
      expect(screen.getByTestId('filter')).toHaveTextContent('vulnerabilities');
    });

    it('returns empty params when no query string', () => {
      function TestComponent() {
        const params = useAdapterSearchParams();
        return <span data-testid="q">{params.get('q') ?? 'none'}</span>;
      }

      render(
        <Routes>
          <Route path="/" element={<TestComponent />} />
        </Routes>,
      );

      expect(screen.getByTestId('q')).toHaveTextContent('none');
    });
  });

  describe('useLink (LinkAdapter)', () => {
    it('maps href prop to React Router Link to prop', () => {
      function TestComponent() {
        const Link = useLink();
        return <Link href="/vulnerabilities/CVE-2023-1234">View CVE</Link>;
      }

      render(
        <Routes>
          <Route path="/" element={<TestComponent />} />
        </Routes>,
      );

      expect(screen.getByRole('link', { name: 'View CVE' })).toHaveAttribute(
        'href',
        '/vulnerabilities/CVE-2023-1234',
      );
    });

    it('navigates via React Router on click', async () => {
      function TestComponent() {
        const Link = useLink();
        return <Link href="/target">Go</Link>;
      }

      render(
        <Routes>
          <Route path="/" element={<TestComponent />} />
          <Route path="/target" element={<LocationDisplay />} />
        </Routes>,
      );

      await userEvent.setup().click(screen.getByRole('link', { name: 'Go' }));

      await waitFor(() => {
        expect(screen.getByTestId('location')).toHaveTextContent('/target');
      });
    });

    it('forwards HTML attributes', () => {
      function TestComponent() {
        const Link = useLink();
        return (
          <Link href="/search" className="nav-link" id="search-link" aria-label="Search">
            Search
          </Link>
        );
      }

      render(
        <Routes>
          <Route path="/" element={<TestComponent />} />
        </Routes>,
      );

      const link = screen.getByRole('link', { name: 'Search' });
      expect(link).toHaveClass('nav-link');
      expect(link).toHaveAttribute('id', 'search-link');
    });
  });

  describe('useForm (FormAdapter)', () => {
    it('renders as a native form element', () => {
      function TestComponent() {
        const Form = useForm();
        return (
          <Form action="/search" data-testid="form">
            <button type="submit">Submit</button>
          </Form>
        );
      }

      render(
        <Routes>
          <Route path="/" element={<TestComponent />} />
        </Routes>,
      );

      expect(screen.getByTestId('form').tagName).toBe('FORM');
    });

    it('prevents default submit and navigates with serialized FormData', async () => {
      function TestComponent() {
        const Form = useForm();
        return (
          <Form action="/search">
            <input name="q" defaultValue="lodash" />
            <input name="ecosystem" defaultValue="npm" />
            <button type="submit">Search</button>
          </Form>
        );
      }

      render(
        <Routes>
          <Route path="/" element={<TestComponent />} />
          <Route path="/search" element={<LocationDisplay />} />
        </Routes>,
      );

      await userEvent.setup().click(screen.getByRole('button', { name: 'Search' }));

      await waitFor(() => {
        const loc = screen.getByTestId('location').textContent;
        expect(loc).toContain('/search?');
        expect(loc).toContain('q=lodash');
        expect(loc).toContain('ecosystem=npm');
      });
    });

    it('omits empty string values from query params', async () => {
      function TestComponent() {
        const Form = useForm();
        return (
          <Form action="/filter">
            <input name="q" defaultValue="react" />
            <input name="empty" defaultValue="" />
            <button type="submit">Filter</button>
          </Form>
        );
      }

      render(
        <Routes>
          <Route path="/" element={<TestComponent />} />
          <Route path="/filter" element={<LocationDisplay />} />
        </Routes>,
      );

      await userEvent.setup().click(screen.getByRole('button', { name: 'Filter' }));

      await waitFor(() => {
        const loc = screen.getByTestId('location').textContent;
        expect(loc).toContain('q=react');
        expect(loc).not.toContain('empty=');
      });
    });

    it('navigates to action without ? when all values are empty', async () => {
      function TestComponent() {
        const Form = useForm();
        return (
          <Form action="/clear">
            <input name="q" defaultValue="" />
            <button type="submit">Clear</button>
          </Form>
        );
      }

      render(
        <Routes>
          <Route path="/" element={<TestComponent />} />
          <Route path="/clear" element={<LocationDisplay />} />
        </Routes>,
      );

      await userEvent.setup().click(screen.getByRole('button', { name: 'Clear' }));

      await waitFor(() => {
        expect(screen.getByTestId('location').textContent).toBe('/clear');
      });
    });

    it('appends form params with & when action already contains query params', async () => {
      function TestComponent() {
        const Form = useForm();
        return (
          <Form action="/search?tab=results">
            <input name="q" defaultValue="lodash" />
            <button type="submit">Search</button>
          </Form>
        );
      }

      render(
        <Routes>
          <Route path="/" element={<TestComponent />} />
          <Route path="/search" element={<LocationDisplay />} />
        </Routes>,
      );

      await userEvent.setup().click(screen.getByRole('button', { name: 'Search' }));

      await waitFor(() => {
        const loc = screen.getByTestId('location').textContent;
        expect(loc).toContain('/search?tab=results&q=lodash');
      });
    });

    it('supports ref forwarding', () => {
      const ref = createRef<HTMLFormElement>();

      function TestComponent() {
        const Form = useForm();
        return (
          <Form ref={ref} action="/submit" data-testid="ref-form">
            <input name="field" />
          </Form>
        );
      }

      render(
        <Routes>
          <Route path="/" element={<TestComponent />} />
        </Routes>,
      );

      expect(ref.current).toBeInstanceOf(HTMLFormElement);
      expect(ref.current).toBe(screen.getByTestId('ref-form'));
    });
  });
});
