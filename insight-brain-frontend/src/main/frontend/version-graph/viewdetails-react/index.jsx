/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { createRoot } from 'react-dom/client';

import App from './components/App';

const uiView = document.getElementById('ui-view');

createRoot(uiView).render(<App />);
