import {themes as prismThemes} from 'prism-react-renderer';
import type {Config} from '@docusaurus/types';
import type * as Preset from '@docusaurus/preset-classic';

// This runs in Node.js - Don't use client-side code here (browser APIs, JSX...)

const config: Config = {
  title: 'ARESLib',
  tagline: 'Elite FTC software framework with deterministic logging, zero-allocation math, and high-fidelity physics simulation.',
  favicon: 'img/favicon.ico',

  url: 'https://MARSProgramming.github.io',
  baseUrl: '/ARESLib/',

  organizationName: 'MARSProgramming',
  projectName: 'ARESLib',

  onBrokenLinks: 'warn',

  i18n: {
    defaultLocale: 'en',
    locales: ['en'],
  },

  markdown: {
    mermaid: true,
  },
  themes: ['@docusaurus/theme-mermaid'],

  presets: [
    [
      'classic',
      {
        docs: {
          sidebarPath: './sidebars.ts',
          editUrl: 'https://github.com/MARSProgramming/ARESLib/tree/master/website/',
        },
        blog: {
          showReadingTime: true,
          editUrl: 'https://github.com/MARSProgramming/ARESLib/tree/master/website/',
        },
        theme: {
          customCss: './src/css/custom.css',
        },
      } satisfies Preset.Options,
    ],
  ],

  themeConfig: {
    colorMode: {
      defaultMode: 'dark',
      disableSwitch: true,
      respectPrefersColorScheme: false,
    },
    navbar: {
      title: 'ARESLib',
      logo: {
        alt: 'ARES Logo',
        src: 'img/logo.svg',
      },
      items: [
        {
          type: 'docSidebar',
          sidebarId: 'tutorialSidebar',
          position: 'left',
          label: 'Tutorials',
        },
        {
          href: 'https://github.com/MARSProgramming/ARESLib',
          label: 'GitHub',
          position: 'right',
        },
      ],
    },
    footer: {
      style: 'dark',
      links: [
        {
          title: 'Docs',
          items: [
            {
              label: 'Tutorials',
              to: '/docs/intro',
            },
          ],
        },
        {
          title: 'Community',
          items: [
            {
              label: 'ARESLib Project',
              href: 'https://github.com/MARSProgramming/ARESLib',
            },
          ],
        },
      ],
      copyright: `Copyright © ${new Date().getFullYear()} Mountaineer Area RoboticS — FTC Team 23247. Built with Docusaurus.`,
    },
    prism: {
      theme: prismThemes.github,
      darkTheme: prismThemes.dracula,
      additionalLanguages: ['java', 'groovy'],
    },
  } satisfies Preset.ThemeConfig,
};

export default config;
