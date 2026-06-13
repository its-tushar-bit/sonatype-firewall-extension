/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { Button } from '@radix-ui/themes';
import type { ComponentPropsWithoutRef, ReactNode } from 'react';

type RadixButtonProps = ComponentPropsWithoutRef<typeof Button>;

type ButtonLinkProps = Omit<ComponentPropsWithoutRef<'a'>, 'children'> & {
  readonly children: ReactNode;
  /** Open in a new tab (adds target=_blank + rel=noopener noreferrer). */
  readonly newTab?: boolean;
  readonly size?: RadixButtonProps['size'];
  readonly variant?: RadixButtonProps['variant'];
  readonly color?: RadixButtonProps['color'];
};

/**
 * An anchor styled as a Radix Button — the "button link" pattern that recurred
 * across the application/waiver pages (Classic escape-hatch links, quick
 * actions, etc.). Extracted to one component per CLM-39709 review #18 so the
 * `<Button asChild><a ...></a></Button>` boilerplate and its
 * target/rel/href wiring live in a single place.
 */
export function ButtonLink({
  href,
  children,
  newTab = false,
  size = '2',
  variant = 'soft',
  color,
  target,
  rel,
  ...anchorProps
}: ButtonLinkProps): JSX.Element {
  const openInNewTab = newTab || target === '_blank';
  return (
    <Button asChild size={size} variant={variant} color={color}>
      <a
        href={href}
        {...anchorProps}
        {...(openInNewTab
          ? { target: '_blank', rel: rel ?? 'noopener noreferrer' }
          : { target, rel })}
      >
        {children}
      </a>
    </Button>
  );
}
