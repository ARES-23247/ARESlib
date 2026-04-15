import React, { useState } from 'react';
import { X, ZoomIn, Monitor, Cpu, Eye, Activity, Settings } from 'lucide-react';

interface Screenshot {
  id: string;
  title: string;
  description: string;
  category: 'telemetry' | 'simulation' | 'vision' | 'performance' | 'configuration';
  icon: any;
  placeholder: string;
  fullPath: string;
}

const SCREENSHOTS: Screenshot[] = [
  {
    id: 'advantagescope-layout',
    title: 'AdvantageScope Layout',
    description: 'Custom AdvantageScope layout showing real-time telemetry from swerve drive, vision systems, and mechanism control.',
    category: 'telemetry',
    icon: Monitor,
    placeholder: 'https://via.placeholder.com/600x400/090A0F/CD7F32?text=AdvantageScope+Dashboard',
    fullPath: '/img/screenshots/advantagescope-layout.png'
  },
  {
    id: 'physics-simulation',
    title: 'Physics Simulation',
    description: 'Desktop physics simulation using dyn4j engine. Test autonomous paths and mechanisms without hardware.',
    category: 'simulation',
    icon: Cpu,
    placeholder: 'https://via.placeholder.com/600x400/090A0F/CD7F32?text=Physics+Simulation',
    fullPath: '/img/screenshots/physics-simulation.png'
  },
  {
    id: 'vision-fusion',
    title: 'Multi-Camera Vision Fusion',
    description: 'AprilTag detection with Kalman filtering and MegaTag 2.0 localization. Shows confidence scoring and ghost rejection.',
    category: 'vision',
    icon: Eye,
    placeholder: 'https://via.placeholder.com/600x400/090A0F/CD7F32?text=Vision+Fusion+Dashboard',
    fullPath: '/img/screenshots/vision-fusion.png'
  },
  {
    id: 'performance-metrics',
    title: 'Zero-Allocation Performance',
    description: 'Real-time performance monitoring showing 250Hz loop with zero heap allocations. GC pauses eliminated.',
    category: 'performance',
    icon: Activity,
    placeholder: 'https://via.placeholder.com/600x400/090A0F/CD7F32?text=Performance+Metrics',
    fullPath: '/img/screenshots/performance-metrics.png'
  },
  {
    id: 'ftc-dashboard',
    title: 'FTC Dashboard Integration',
    description: 'Web-based FTC Dashboard showing configurable variables, camera streams, and field overlay drawing.',
    category: 'telemetry',
    icon: Monitor,
    placeholder: 'https://via.placeholder.com/600x400/090A0F/CD7F32?text=FTC+Dashboard',
    fullPath: '/img/screenshots/ftc-dashboard.png'
  },
  {
    id: 'health-monitoring',
    title: 'Fault Management System',
    description: 'Real-time hardware health monitoring with LED feedback, haptic alerts, and automatic fallback modes.',
    category: 'configuration',
    icon: Settings,
    placeholder: 'https://via.placeholder.com/600x400/090A0F/CD7F32?text=Health+Monitoring',
    fullPath: '/img/screenshots/health-monitoring.png'
  }
];

const CATEGORY_COLORS = {
  telemetry: 'from-blue-500 to-cyan-500',
  simulation: 'from-purple-500 to-pink-500',
  vision: 'from-green-500 to-emerald-500',
  performance: 'from-orange-500 to-red-500',
  configuration: 'from-yellow-500 to-orange-500',
};

interface ScreenshotGalleryProps {
  category?: 'all' | Screenshot['category'];
}

export default function ScreenshotGallery({ category = 'all' }: ScreenshotGalleryProps) {
  const [selectedScreenshot, setSelectedScreenshot] = useState<Screenshot | null>(null);
  const [filter, setFilter] = useState(category);

  const filteredScreenshots = filter === 'all'
    ? SCREENSHOTS
    : SCREENSHOTS.filter(s => s.category === filter);

  return (
    <div className="screenshot-gallery">
      <div className="gallery-header">
        <h3>Screenshots & Visuals</h3>
        <div className="gallery-filters">
          <button
            className={`filter-btn ${filter === 'all' ? 'active' : ''}`}
            onClick={() => setFilter('all')}
          >
            All
          </button>
          <button
            className={`filter-btn ${filter === 'telemetry' ? 'active' : ''}`}
            onClick={() => setFilter('telemetry')}
          >
            Telemetry
          </button>
          <button
            className={`filter-btn ${filter === 'simulation' ? 'active' : ''}`}
            onClick={() => setFilter('simulation')}
          >
            Simulation
          </button>
          <button
            className={`filter-btn ${filter === 'vision' ? 'active' : ''}`}
            onClick={() => setFilter('vision')}
          >
            Vision
          </button>
          <button
            className={`filter-btn ${filter === 'performance' ? 'active' : ''}`}
            onClick={() => setFilter('performance')}
          >
            Performance
          </button>
        </div>
      </div>

      <div className="screenshot-grid">
        {filteredScreenshots.map((screenshot) => {
          const IconComponent = screenshot.icon;
          return (
            <div
              key={screenshot.id}
              className="screenshot-card"
              onClick={() => setSelectedScreenshot(screenshot)}
            >
              <div className="screenshot-image">
                <img
                  src={screenshot.placeholder}
                  alt={screenshot.title}
                  loading="lazy"
                />
                <div className="screenshot-overlay">
                  <ZoomIn size={24} />
                </div>
                <div className={`category-badge bg-gradient-to-r ${CATEGORY_COLORS[screenshot.category]}`}>
                  <IconComponent size={14} />
                  {screenshot.category}
                </div>
              </div>
              <div className="screenshot-info">
                <h4>{screenshot.title}</h4>
                <p>{screenshot.description}</p>
              </div>
            </div>
          );
        })}
      </div>

      {selectedScreenshot && (
        <div
          className="modal-overlay"
          onClick={() => setSelectedScreenshot(null)}
        >
          <div className="modal-content" onClick={(e) => e.stopPropagation()}>
            <button
              className="modal-close"
              onClick={() => setSelectedScreenshot(null)}
            >
              <X size={24} />
            </button>
            <img
              src={selectedScreenshot.placeholder}
              alt={selectedScreenshot.title}
            />
            <div className="modal-info">
              <h3>{selectedScreenshot.title}</h3>
              <p>{selectedScreenshot.description}</p>
              <div className="modal-meta">
                <span className="category-tag">
                  <selectedScreenshot.icon size={16} />
                  {selectedScreenshot.category}
                </span>
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}