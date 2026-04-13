/* ===== ARESLib Shared Navigation & Footer Injection ===== */

document.addEventListener('DOMContentLoaded', () => {
    injectHeader();
    injectFooter();
    injectStars();
});

const SEARCH_INDEX = [
    { title: 'Home', desc: 'Framework overview and installation', url: 'index.html', keywords: 'home landing install' },
    { title: 'Standards', desc: 'Elite coding requirements and zero-allocation rules', url: 'standards.html', keywords: 'rules standards elite allocation' },
    { title: 'Tutorials Index', desc: 'All available training guides', url: 'tutorials/index.html', keywords: 'tutorials training guides' },
    { title: 'Zero-Allocation', desc: 'Eliminate GC jitter in the hot path', url: 'tutorials/zero-allocation.html', keywords: 'performance gc memory' },
    { title: 'Physics Simulation', desc: 'High-fidelity modeling with dyn4j', url: 'tutorials/physics-sim.html', keywords: 'sim physics dyn4j' },
    { title: 'State Machines', desc: 'Managing complex scoring sequences', url: 'tutorials/state-machines.html', keywords: 'state logic sequence' },
    { title: 'Telemetry & Logging', desc: 'Visualizing state with AdvantageScope', url: 'tutorials/telemetry-logging.html', keywords: 'logs telemetry scope' },
    { title: 'SOTM Solver', desc: 'Elite Shooting while moving', url: 'tutorials/sotm.html', keywords: 'sotm elite shooting kinematics aim' },
    { title: 'Vision Fusion', desc: 'MegaTag 2.0 and pose estimation', url: 'tutorials/vision-fusion.html', keywords: 'vision apriltag fusion megatag localization' },
    { title: 'Swerve Kinematics', desc: 'Vector summation and discretization', url: 'tutorials/swerve-kinematics.html', keywords: 'swerve kinematics vector math discretization' },
    { title: 'Championship Testing', desc: 'Headless JUnit 5 and IO mocking', url: 'tutorials/championship-testing.html', keywords: 'testing junit unit headless coverage' },
    { title: 'SysId Tuning', desc: 'Characterization and feedforward', url: 'tutorials/sysid-tuning.html', keywords: 'sysid feedforward tuning ks kv ka' },
    { title: 'Autonomous Flow', desc: 'PathPlanner and dynamic avoidance', url: 'tutorials/autonomous-flow.html', keywords: 'auto autonomous pathplanner path adstar avoidance' },
    { title: 'Fault Resilience', desc: 'Hardware health and driver alerts', url: 'tutorials/fault-resilience.html', keywords: 'faults health alert diagnostics rumble' },
    { title: 'Hardware Abstraction (HAL)', desc: 'Decoupling logic from electronics', url: 'tutorials/hardware-abstraction.html', keywords: 'hal io abstraction motor real sim' },
    { title: 'Power Management', desc: 'Battery sag and load shedding', url: 'tutorials/power-management.html', keywords: 'power battery voltage current shedding' },
    { title: 'AI Agents & Skills', desc: 'AI index definitions for intelligent agents', url: 'tutorials/index.html#ai-skills', keywords: 'ai agents skills marketplace plugin' }
];

function injectHeader() {
    const headerPlaceholder = document.getElementById('header-placeholder');
    if (!headerPlaceholder) return;

    const isSubdir = window.location.pathname.includes('/tutorials/');
    const basePath = isSubdir ? '../' : '';
    const editUrl = getEditUrl();
    const activePage = window.location.pathname.split('/').pop() || 'index.html';

    headerPlaceholder.innerHTML = `
        <header class="nav-header">
            <nav>
                <a href="${basePath}index.html" class="logo">ARESLib<span>2</span></a>
                <div class="nav-links">
                    <a href="${basePath}index.html" class="${activePage === 'index.html' && !isSubdir ? 'active' : ''}">Home</a>
                    <a href="${basePath}standards.html" class="${activePage === 'standards.html' ? 'active' : ''}">Standards</a>
                    <a href="${basePath}tutorials/index.html" class="${isSubdir && activePage !== 'ai-skills.html' ? 'active' : ''}">TUTORIALS</a>
                    <a href="${basePath}tutorials/index.html#ai-skills" class="${activePage === 'ai-skills.html' ? 'active' : ''}">AI Agents & Skills</a>
                </div>

                <div class="nav-search">
                    <span class="search-icon">🔍</span>
                    <input type="text" id="search-input" class="search-input" placeholder="Search ARESLib...">
                    <ul id="search-results"></ul>
                </div>

                <div class="nav-actions" style="display: flex; gap: 1rem; align-items: center;">
                    <a href="${basePath}javadoc/index.html" style="font-size: 0.8rem; color: var(--primary-color); border: 1px solid rgba(205,127,50,0.4); padding: 4px 10px; border-radius: 4px; font-weight: 600;">API</a>
                    <a href="${editUrl}" target="_blank" class="nav-edit">Edit Page</a>
                </div>
            </nav>
        </header>
    `;

    setupSearch();
}

function setupSearch() {
    const searchInput = document.getElementById('search-input');
    const searchResults = document.getElementById('search-results');
    
    if (!searchInput || !searchResults) return;

    searchInput.addEventListener('input', (e) => {
        const query = e.target.value.toLowerCase();
        if (!query) {
            searchResults.style.display = 'none';
            return;
        }

        const filtered = SEARCH_INDEX.filter(item => 
            item.title.toLowerCase().includes(query) || 
            item.desc.toLowerCase().includes(query) ||
            item.keywords.toLowerCase().includes(query)
        );

        if (filtered.length > 0) {
            const isInsideTutorial = window.location.pathname.includes('/tutorials/');
            const relBase = isInsideTutorial ? '../' : '';
            
            searchResults.innerHTML = filtered.map(item => `
                <li class="search-result-item" onclick="window.location.href='${relBase}${item.url}'">
                    <span class="title">${item.title}</span>
                    <span class="desc">${item.desc}</span>
                </li>
            `).join('');
            searchResults.style.display = 'block';
        } else {
            searchResults.style.display = 'none';
        }
    });

    document.addEventListener('click', (e) => {
        if (!searchInput.contains(e.target) && !searchResults.contains(e.target)) {
            searchResults.style.display = 'none';
        }
    });
}

function injectFooter() {
    const footerPlaceholder = document.getElementById('footer-placeholder');
    if (!footerPlaceholder) return;

    footerPlaceholder.innerHTML = `
        <footer>
            <p>&copy; 2026 ARESLib Framework - Championship Grade FTC Software.</p>
            <div style="margin-top: 1rem; opacity: 0.6; font-size: 0.8rem;">
                <a href="https://github.com/ARES-23247/ARESLib" style="color: inherit; margin: 0 10px;">GitHub</a>
                <a href="${getEditUrl()}" style="color: inherit; margin: 0 10px;">Wiki Edit</a>
            </div>
        </footer>
    `;
}

function getEditUrl() {
    const repoRoot = "https://github.com/ARES-23247/ARESLib/edit/master/docs/";
    let path = window.location.pathname.split('/').pop();
    
    const segments = window.location.pathname.split('/');
    const isTutorial = segments.includes('tutorials');
    
    if (!path || path === "" || path === "docs" || path.includes('index.html') && !isTutorial) {
        path = "index.html";
    }

    const finalPath = isTutorial ? `tutorials/${path}` : path;
    return repoRoot + finalPath;
}

function injectStars() {
    let starsContainer = document.getElementById('stars-container');
    if (!starsContainer) {
        starsContainer = document.createElement('div');
        starsContainer.id = 'stars-container';
        document.body.prepend(starsContainer);
    }

    const starCount = 150;
    for (let i = 0; i < starCount; i++) {
        const star = document.createElement('div');
        star.className = 'star';
        const x = Math.random() * 100;
        const y = Math.random() * 100;
        const duration = 2 + Math.random() * 3;
        const delay = Math.random() * 5;
        const size = 1 + Math.random() * 2;
        star.style.left = `${x}%`;
        star.style.top = `${y}%`;
        star.style.width = `${size}px`;
        star.style.height = `${size}px`;
        star.style.setProperty('--duration', `${duration}s`);
        star.style.animationDelay = `${delay}s`;
        starsContainer.appendChild(star);
    }
}
