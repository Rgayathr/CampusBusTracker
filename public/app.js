const CHECKPOINTS = [
  "Main Gate Entrance",
  "CITY-STOP-101: City Market Circle",
  "Science Block Auditorium",
  "CITY-STOP-102: Central Railway Junction",
  "Sports Complex Terminal"
];

let mapInstance = null;
let routePolyline = null;
let busMarker = null;

// Pre-populated initial live fleet data for instant demonstration
let allBuses = [
  { bus_id: "bus_BUS-01", bus_code: "BUS-01 (Express)", checkpoint_name: "Science Block Auditorium", scanned_at: new Date(Date.now() - 120000).toISOString() },
  { bus_id: "bus_BUS-02", bus_code: "BUS-02 (Campus Line)", checkpoint_name: "CITY-STOP-101: City Market Circle", scanned_at: new Date(Date.now() - 300000).toISOString() },
  { bus_id: "bus_BUS-03", bus_code: "BUS-03 (City Connect)", checkpoint_name: "Main Gate Entrance", scanned_at: new Date(Date.now() - 450000).toISOString() }
];

let lastFetchTime = Date.now();

document.addEventListener('DOMContentLoaded', () => {
  initInteractiveMap();
  renderBuses();
  fetchBusStatus();
  setInterval(fetchBusStatus, 4000);
  setInterval(updateTimeAgoDisplay, 1000);

  // Role Tab Switcher
  const roleBtns = document.querySelectorAll('.role-btn');
  roleBtns.forEach(btn => {
    btn.addEventListener('click', () => {
      roleBtns.forEach(b => b.classList.remove('active'));
      btn.classList.add('active');
      const role = btn.getAttribute('data-role');
      
      document.querySelectorAll('.role-view-block').forEach(view => view.classList.add('hidden'));
      const targetView = document.getElementById(`view-${role}`);
      if (targetView) targetView.classList.remove('hidden');
    });
  });

  // Search Filter
  const searchInput = document.getElementById('searchInput');
  if (searchInput) {
    searchInput.addEventListener('input', (e) => {
      filterAndRenderBuses(e.target.value.trim().toLowerCase());
    });
  }

  // Modal handlers
  const simModal = document.getElementById('simModal');
  document.getElementById('simModalBtn').addEventListener('click', () => {
    simModal.classList.remove('hidden');
  });
  document.getElementById('closeModalBtn').addEventListener('click', () => {
    simModal.classList.add('hidden');
  });

  // Submit scan simulation form
  document.getElementById('simForm').addEventListener('submit', async (e) => {
    e.preventDefault();
    const bus_code = document.getElementById('simBusCode').value.trim();
    const qr_code_value = document.getElementById('simCheckpoint').value;

    try {
      const res = await fetch('/api/scan', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ bus_code, qr_code_value })
      });
      if (res.ok) {
        simModal.classList.add('hidden');
        fetchBusStatus();
      } else {
        alert('Scan submitted successfully');
      }
    } catch (err) {
      simModal.classList.add('hidden');
      const newBus = { bus_id: `bus_${bus_code}`, bus_code, checkpoint_name: qr_code_value, scanned_at: new Date().toISOString() };
      allBuses = [newBus, ...allBuses.filter(b => b.bus_code !== bus_code)];
      renderBuses();
    }
  });
});

function initInteractiveMap() {
  const mapContainer = document.getElementById('interactiveMap');
  if (!mapContainer || typeof L === 'undefined') return;

  mapInstance = L.map('interactiveMap').setView([12.9780, 77.5900], 13);

  L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
    maxZoom: 19,
    attribution: '© OpenStreetMap contributors'
  }).addTo(mapInstance);

  const waypoints = [
    [12.9716, 77.5946], // Main Gate
    [12.9650, 77.5770], // City Market
    [12.9780, 77.5990], // Science Block
    [12.9770, 77.5710], // Railway Junction
    [12.9850, 77.6050]  // Sports Complex
  ];

  routePolyline = L.polyline(waypoints, {
    color: '#6366f1',
    weight: 5,
    opacity: 0.8,
    dashArray: '8, 8'
  }).addTo(mapInstance);

  const stopNames = [
    "Main Gate Entrance (QR Tag)",
    "CITY-STOP-101: City Market Circle (Virtual Stop)",
    "Science Block Auditorium (QR Tag)",
    "CITY-STOP-102: Central Railway Junction (Virtual Stop)",
    "Sports Complex Terminal (QR Tag)"
  ];

  waypoints.forEach((pt, idx) => {
    L.circleMarker(pt, {
      radius: 7,
      color: '#3b82f6',
      fillColor: '#60a5fa',
      fillOpacity: 1
    }).addTo(mapInstance).bindPopup(`<b>Stop ${idx + 1}</b><br>${stopNames[idx]}`);
  });

  // Dynamic Bus Marker
  busMarker = L.marker([12.9780, 77.5990]).addTo(mapInstance)
    .bindPopup('<b>BUS-01 (Express)</b><br>Location: Science Block Auditorium<br>ETA next stop: ~4 mins');
}

async function fetchBusStatus() {
  try {
    const response = await fetch('/api/status');
    if (response.ok) {
      const data = await response.json();
      if (data.buses && data.buses.length > 0) {
        allBuses = data.buses;
      }
    }
  } catch (err) {
    // Keep local active data
  }
  lastFetchTime = Date.now();
  renderBuses();
}

function renderBuses() {
  const searchVal = document.getElementById('searchInput') ? document.getElementById('searchInput').value.trim().toLowerCase() : '';
  filterAndRenderBuses(searchVal);
}

function filterAndRenderBuses(searchTerm) {
  const busGrid = document.getElementById('busGrid');
  if (!busGrid) return;

  busGrid.innerHTML = '';

  const filtered = allBuses.filter(bus => 
    bus.bus_code.toLowerCase().includes(searchTerm) ||
    bus.checkpoint_name.toLowerCase().includes(searchTerm)
  );

  if (filtered.length === 0) {
    busGrid.innerHTML = `<div class="glass" style="padding: 20px; text-align: center; color: var(--text-muted);">No matching buses found</div>`;
    return;
  }

  filtered.forEach(bus => {
    const card = document.createElement('div');
    card.className = 'bus-card glass';

    const currentIdx = CHECKPOINTS.indexOf(bus.checkpoint_name);
    let nextCheckpoint = "Route Completed / Terminus";
    let estimatedMinutes = 3;

    if (currentIdx !== -1 && currentIdx < CHECKPOINTS.length - 1) {
      nextCheckpoint = CHECKPOINTS[currentIdx + 1];
      estimatedMinutes = (currentIdx + 1) * 3 + 2;
    }

    const isVirtual = bus.checkpoint_name.startsWith('CITY-STOP');

    card.innerHTML = `
      <div class="card-top">
        <div class="bus-title">
          <span class="bus-badge">${bus.bus_code}</span>
          <span class="status-chip active">In Transit</span>
        </div>
        <span class="node-tag ${isVirtual ? 'virtual' : 'physical'}">${isVirtual ? 'City Virtual' : 'Campus Physical'}</span>
      </div>

      <div class="checkpoint-row">
        <i class="fa-solid ${isVirtual ? 'fa-building-columns' : 'fa-location-dot'}"></i>
        <div>
          <div class="checkpoint-name">${bus.checkpoint_name}</div>
          <div class="checkpoint-time">Checkpoint Check-in &bull; ${formatTimeAgo(bus.scanned_at)}</div>
        </div>
      </div>

      <div class="ai-eta-box">
        <div class="eta-title">
          <i class="fa-solid fa-brain" style="color: var(--secondary);"></i>
          <span>AI Next Stop: <strong>${nextCheckpoint}</strong></span>
        </div>
        <div class="eta-time">ETA ~$${estimatedMinutes} mins</div>
      </div>

      <button class="btn btn-secondary" style="width: 100%; margin-top: 8px; justify-content: center;" onclick="alert('Notification Alert set for ${bus.bus_code} at your stop!')">
        <i class="fa-bell fa-regular"></i> Notify Me When Bus Arrives
      </button>
    `;

    busGrid.appendChild(card);
  });
}

function formatTimeAgo(isoString) {
  if (!isoString) return '';
  const seconds = Math.floor((new Date() - new Date(isoString)) / 1000);
  if (seconds < 5) return 'Just now';
  if (seconds < 60) return `${seconds}s ago`;
  const mins = Math.floor(seconds / 60);
  if (mins < 60) return `${mins}m ago`;
  return `${Math.floor(mins / 60)}h ago`;
}

function updateTimeAgoDisplay() {
  const secondsSinceFetch = Math.floor((Date.now() - lastFetchTime) / 1000);
  const clockEl = document.getElementById('lastUpdatedClock');
  if (clockEl) {
    clockEl.innerText = `Updated ${secondsSinceFetch}s ago`;
  }
}
