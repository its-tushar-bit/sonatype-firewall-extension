/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { Component } from 'react';
import type { ReactNode, ErrorInfo } from 'react';
import { useLocation } from 'react-router';
import { ErrorPage } from './ErrorPage';
import { reloadPage } from 'GuideRoot/utils/navigation';

interface Props {
  children: ReactNode;
}

interface CoreProps extends Props {
  locationKey: string;
}

interface State {
  hasError: boolean;
  capturedLocationKey?: string;
}

class ErrorBoundaryCore extends Component<CoreProps, State> {
  state: State = { hasError: false };

  static getDerivedStateFromError(): Partial<State> {
    return { hasError: true };
  }

  static getDerivedStateFromProps(props: CoreProps, state: State): Partial<State> | null {
    if (state.hasError && state.capturedLocationKey === undefined) {
      return { capturedLocationKey: props.locationKey };
    }
    if (state.hasError && state.capturedLocationKey !== props.locationKey) {
      return { hasError: false, capturedLocationKey: undefined };
    }
    return null;
  }

  componentDidCatch(error: Error, info: ErrorInfo) {
    // TODO: wire to error reporting service (e.g. Sentry)
    console.error('[ErrorBoundary]', error, info.componentStack);
  }

  render() {
    if (this.state.hasError) {
      return <ErrorPage onRetry={reloadPage} />;
    }
    return this.props.children;
  }
}

export function ErrorBoundary({ children }: Props) {
  const { pathname, key } = useLocation();
  // Combine pathname + key so the boundary resets on any navigation, even same-route
  // navigations where React Router may theoretically reuse the same key.
  return <ErrorBoundaryCore locationKey={`${pathname}::${key}`}>{children}</ErrorBoundaryCore>;
}
