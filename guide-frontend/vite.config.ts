/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

export default defineConfig({
  plugins: [react()],
  base: '/guide/',
  build: {
    outDir: 'target/generated-resources/guide/guide-assets',
    emptyOutDir: true,
  },
  server: {
    proxy: {
      '/rest': 'http://localhost:8072',
      '/api': 'http://localhost:8072',
      '/components': 'http://localhost:8090',
      '/vulnerabilities': 'http://localhost:8090',
    },
  },
});
