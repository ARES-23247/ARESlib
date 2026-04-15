// @ts-check
import { defineConfig } from 'astro/config';
import starlight from '@astrojs/starlight';
import react from '@astrojs/react';
import markdoc from '@astrojs/markdoc';
import cloudflare from '@astrojs/cloudflare';

// Mermaid diagram integration
import { remarkMermaid } from 'remark-mermaid-plugin';

// https://astro.build/config
export default defineConfig({
	site: 'https://areslib.pages.dev',
	output: 'server',
	adapter: cloudflare({
		imageService: 'cloudflare',
		routes: {
			extend: {
				include: ['/keystatic', '/keystatic/*', '/api/keystatic', '/api/keystatic/*']
			}
		}
	}),
	vite: {
		ssr: {
			external: ['node:path', 'node:fs', 'node:url', 'node:util', 'path', 'fs', 'url', 'util', 'postcss', 'util-deprecate'],
		},
		build: {
			chunkSizeWarningLimit: 2000,
		}
	},
	integrations: [
		starlight({
			title: 'ARESLib Documentation',
			customCss: [
				'./src/styles/custom.css',
			],
			logo: {
				src: './src/assets/ares-logo.png',
			},
			favicon: '/img/ares-logo.png',
			social: [{ icon: 'github', label: 'GitHub', href: 'https://github.com/ARES-23247/ARESLib' }],
			sidebar: [
				{
					label: 'Getting Started',
					items: [
						{ label: 'Robot Config Generator', link: '/guides/robot-setup' },
						{ label: 'Architecture Diagrams', link: '/guides/architecture-diagrams' },
						{ label: 'Media Gallery', link: '/guides/media-gallery' },
					],
				},
				{
					label: 'Migration Guides',
					items: [
						{ label: 'From Basic OpMode', link: '/guides/migration-basic-opmode' },
						{ label: 'From FTCLib', link: '/guides/migration-ftclib' },
						{ label: 'From NextFTC', link: '/guides/migration-nextftc' },
						{ label: 'From Road Runner', link: '/guides/migration-roadrunner' },
					],
				},
				{
					label: 'Support',
					items: [
						{ label: 'Troubleshooting Hub', link: '/guides/troubleshooting' },
						{ label: 'Performance Benchmarks', link: '/guides/performance-benchmarks' },
					],
				},
				{
					label: 'Community',
					items: [
						{ label: 'Team Showcase', link: '/guides/community-showcase' },
						{ label: 'Video Tutorials', link: '/guides/video-tutorials' },
						{ label: 'Recipe Library', link: '/guides/recipe-library' },
						{ label: 'FAQ', link: '/guides/faq' },
					],
				},
				{
					label: 'Reference',
					items: [
						{ label: 'API Overview', link: '/reference/api-overview' },
					],
				},
				{
					label: 'The ARESLib Standard',
					link: '/standards/',
				},
				{
					label: 'Foundation Track',
					items: [
						{ label: 'Hardware Abstraction', link: '/tutorials/hardware-abstraction' },
						{ label: 'State Machine Logic', link: '/tutorials/state-machines' },
						{ label: 'Automated Telemetry', link: '/tutorials/telemetry-logging' },
						{ label: 'Zero-Allocation Standard', link: '/tutorials/zero-allocation' },
					],
				},
				{
					label: 'Precision Track',
					items: [
						{ label: 'Swerve Kinematics', link: '/tutorials/swerve-kinematics' },
						{ label: 'Vision Fusion', link: '/tutorials/vision-fusion' },
						{ label: 'Elite Shooting (SOTM)', link: '/tutorials/sotm' },
						{ label: 'SysId & Tuning', link: '/tutorials/sysid-tuning' },
						{ label: 'Live Feedforward Tuning', link: '/tutorials/live-feedforward-tuning' },
						{ label: 'Smart Assist Align', link: '/tutorials/smart-assist-align' },
					],
				},
				{
					label: 'Reliability Track',
					items: [
						{ label: 'Fault Resilience', link: '/tutorials/fault-resilience' },
						{ label: 'Power Management', link: '/tutorials/power-management' },
						{ label: 'Autonomous Flow', link: '/tutorials/autonomous-flow' },
						{ label: 'Championship Testing', link: '/tutorials/championship-testing' },
						{ label: 'Pre-Match Health Checks', link: '/tutorials/health-checks' },
					],
				},
				{
					label: 'HMI & Control',
					items: [
						{ label: 'Advanced Controller Integration', link: '/tutorials/controller-integration' },
						{ label: 'Physics Simulation', link: '/tutorials/physics-sim' },
					],
				},
				{
					label: 'API Reference',
					link: 'https://ARES-23247.github.io/ARESLib/javadoc/index.html',
					attrs: { target: '_blank' }
				},
			],
			components: {
				Footer: './src/components/Footer.astro',
				SiteTitle: './src/components/SiteTitle.astro',
				PageTitle: './src/components/PageTitle.astro',
			},
		}),
		react(),
		markdoc()
	],
	markdown: {
		remarkPlugins: [remarkMermaid],
	},
	// Enhanced search configuration for better documentation search
	search: {
		limit: 250, // Increased search results
		include: '/**/*.{md,mdx}',
		exclude: ['/api/**', '/internal/**'],
	},
	// Vite configuration for better performance
	vite: {
		ssr: {
			external: ['node:path', 'node:fs', 'node:url', 'node:util', 'path', 'fs', 'url', 'util', 'postcss', 'util-deprecate'],
		},
		build: {
			chunkSizeWarningLimit: 2000,
		}
	},
});