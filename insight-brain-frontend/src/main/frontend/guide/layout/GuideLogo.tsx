/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import { useNavigate } from 'react-router';
import logoDark from './images/guide-logo-dark.svg';
import logoLight from './images/guide-logo-light.svg';
import { useTheme } from './ThemeProvider';

export function GuideLogo() {
  const navigate = useNavigate();
  const { resolvedTheme } = useTheme();

  const handleClick = (e: React.MouseEvent) => {
    e.preventDefault();
    navigate('/');
  };

  return (
    <a href="#/" onClick={handleClick} style={{ display: 'flex', alignItems: 'center', textDecoration: 'none', color: 'inherit', cursor: 'pointer' }}>
      <img
        src={resolvedTheme === 'dark' ? logoDark : logoLight}
        alt="Sonatype Guide"
        style={{ maxHeight: '100%', maxWidth: '250px', height: '100%', width: 'auto' }}
      />
    </a>
  );
}
