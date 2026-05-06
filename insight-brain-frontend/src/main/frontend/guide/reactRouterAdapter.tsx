/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import { forwardRef, useMemo } from 'react';
import {
  Link as RouterLink,
  useNavigate,
  useLocation,
  useSearchParams as useRouterSearchParams,
} from 'react-router';
import type { NavigationAdapter, LinkProps, FormProps } from '@guide/ui-core';

function LinkAdapter({ href, prefetch, children, ...props }: LinkProps) {
  return (
    <RouterLink to={href} {...props}>
      {children}
    </RouterLink>
  );
}

const FormAdapter = forwardRef<HTMLFormElement, FormProps>(
  ({ action, children, ...props }, ref) => {
    const navigate = useNavigate();
    const handleSubmit = (e: React.FormEvent<HTMLFormElement>) => {
      e.preventDefault();
      const formData = new FormData(e.currentTarget);
      const params = new URLSearchParams();
      formData.forEach((value, key) => {
        if (typeof value === 'string' && value !== '') {
          params.append(key, value);
        }
      });
      const queryString = params.toString();
      const separator = action.includes('?') ? '&' : '?';
      navigate(queryString ? `${action}${separator}${queryString}` : action);
    };
    return (
      <form ref={ref} action={action} onSubmit={handleSubmit} {...props}>
        {children}
      </form>
    );
  }
);
FormAdapter.displayName = 'FormAdapter';

export function useReactRouterAdapter(): NavigationAdapter {
  const nav = useNavigate();
  return useMemo(
    () => ({
      navigate: (url: string, options?: { scroll?: boolean; replace?: boolean }) => {
        nav(url, {
          replace: options?.replace,
          preventScrollReset: options?.scroll === false,
        });
      },
      usePathname: () => useLocation().pathname,
      useSearchParams: () => useRouterSearchParams()[0],
      Link: LinkAdapter,
      Form: FormAdapter,
    }),
    [nav]
  );
}
