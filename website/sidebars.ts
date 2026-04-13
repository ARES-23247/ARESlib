import type {SidebarsConfig} from '@docusaurus/plugin-content-docs';

// This runs in Node.js - Don't use client-side code here (browser APIs, JSX...)

/**
 * Creating a sidebar enables you to:
 - create an ordered group of docs
 - render a sidebar for each doc of that group
 - provide next/previous navigation

 The sidebars can be generated from the filesystem, or explicitly defined here.

 Create as many sidebars as you want.
 */
const sidebars: SidebarsConfig = {
  tutorialSidebar: [
    'intro',
    {
      type: 'category',
      label: 'Foundation',
      items: [
        'tutorials/foundation/hardware-abstraction',
        'tutorials/foundation/swerve-kinematics',
        'tutorials/foundation/control-basics',
      ],
    },
    {
      type: 'category',
      label: 'Performance',
      items: [
        'tutorials/performance/zero-allocation',
      ],
    },
    {
      type: 'category',
      label: 'Simulation',
      items: [
        'tutorials/simulation/physics-sim',
      ],
    },
    {
      type: 'category',
      label: 'Architecture',
      items: [
        'tutorials/architecture/state-machines',
      ],
    },
    {
      type: 'category',
      label: 'Diagnostics',
      items: [
        'tutorials/diagnostics/telemetry-logging',
      ],
    },
    {
      type: 'category',
      label: 'Elite Features',
      items: [
        'tutorials/elite/vision-fusion',
        'tutorials/elite/sotm',
      ],
    },
    {
      type: 'category',
      label: 'Intelligence',
      items: [
        'tutorials/intelligence/sysid-tuning',
        'tutorials/intelligence/autonomous-flow',
      ],
    },
    {
      type: 'category',
      label: 'Reliability',
      items: [
        'tutorials/reliability/fault-resilience',
        'tutorials/reliability/power-management',
      ],
    },
    {
      type: 'category',
      label: 'Validation',
      items: [
        'tutorials/validation/championship-testing',
      ],
    },
  ],
};

export default sidebars;
