/* ===================================================
   Hemo-Connect - ADMIN DASHBOARD JAVASCRIPT
   Authentication + Interactive analytics and map functionality
=================================================== */

// ===================================================
//    AUTHENTICATION SYSTEM 
// ===================================================

// Valid admin credentials (in real app, this would be server-side)
var validCredentials = {
  'admin123': { password: 'hemo2026', role: 'National Administrator', region: 'All Regions', name: 'Dr. Sarah Johnson' },
  'manager456': { password: 'blood2026', role: 'Regional Manager', region: 'North Region', name: 'Rajesh Kumar' },
  'manager789': { password: 'blood2026', role: 'Regional Manager', region: 'South Region', name: 'Priya Sharma' },
  'admin456': { password: 'admin2026', role: 'Regional Administrator', region: 'West Region', name: 'Ahmed Khan' }
};

// Current session info
var currentSession = {
  isAuthenticated: false,
  adminId: null,
  adminData: null,
  loginTime: null
};

// Authentication functions 
function handleLogin(event) {
  event.preventDefault();
  
  var adminId = document.getElementById('adminId').value.trim();
  var password = document.getElementById('password').value;
  var region = document.getElementById('region').value;
  
  // Show loading state
  var loginBtn = document.getElementById('loginBtn');
  var btnText = loginBtn.querySelector('.btn-text');
  var btnLoader = loginBtn.querySelector('.btn-loader');
  
  btnText.style.display = 'none';
  btnLoader.style.display = 'inline';
  loginBtn.disabled = true;
  
  // Simulate authentication delay
  setTimeout(function() {
    if (validateCredentials(adminId, password, region)) {
      // Success - setup session and show dashboard
      setupUserSession(adminId, region);
      showDashboard();
    } else {
      // Failed authentication
      showLoginError();
      btnText.style.display = 'inline';
      btnLoader.style.display = 'none';
      loginBtn.disabled = false;
    }
  }, 1500);
  
  return false;
}

function validateCredentials(adminId, password, region) {
  if (!validCredentials[adminId]) {
    return false;
  }
  
  var user = validCredentials[adminId];
  if (user.password !== password) {
    return false;
  }
  
  // Check if region matches (or if admin has access to all regions)
  if (user.region !== 'All Regions' && user.region !== region) {
    return false;
  }
  
  return true;
}

function setupUserSession(adminId, selectedRegion) {
  var userData = validCredentials[adminId];
  
  currentSession.isAuthenticated = true;
  currentSession.adminId = adminId;
  currentSession.adminData = userData;
  currentSession.loginTime = new Date();
  
  // Update UI elements with user info
  updateHeaderInfo(userData, selectedRegion);
  
  // Store session (in real app, would use secure tokens)
  localStorage.setItem('hemoConnectSession', JSON.stringify({
    adminId: adminId,
    loginTime: currentSession.loginTime.toISOString(),
    region: selectedRegion
  }));
}

function updateHeaderInfo(userData, region) {
  // Update admin info in header
  document.getElementById('adminName').textContent = userData.name;
  document.getElementById('adminRole').textContent = userData.role;
  document.getElementById('adminRegion').textContent = region;
  document.getElementById('loginTime').textContent = 'Logged in: ' + new Date().toLocaleTimeString('en-US', {
    hour: '2-digit',
    minute: '2-digit'
  });
}

function showDashboard() {
  document.getElementById('authScreen').style.display = 'none';
  document.getElementById('dashboardScreen').style.display = 'block';
  
  // Initialize dashboard components
  setTimeout(function() {
    initializeDashboard(); 
  }, 100);
}

function showLoginError() {
  // Create or update error message
  var existingError = document.querySelector('.login-error');
  if (existingError) {
    existingError.remove();
  }
  
  var errorDiv = document.createElement('div');
  errorDiv.className = 'login-error';
  errorDiv.innerHTML = `
    <div class="error-content">
      ⚠️ <strong>Authentication Failed</strong><br>
      Please check your credentials and try again.
    </div>
  `;
  
  var form = document.getElementById('loginForm');
  form.insertBefore(errorDiv, form.firstChild);
  
  // Add error styling
  var style = document.createElement('style');
  style.textContent = `
    .login-error {
      background: #fee;
      border: 1px solid #fcc;
      border-radius: 6px;
      padding: 12px;
      margin-bottom: 20px;
      animation: shake 0.5s ease-in-out;
    }
    .error-content {
      font-size: 13px;
      color: #c33;
      text-align: center;
    }
    @keyframes shake {
      0%, 100% { transform: translateX(0); }
      25% { transform: translateX(-5px); }
      75% { transform: translateX(5px); }
    }
  `;
  document.head.appendChild(style);
  
  // Remove error after 5 seconds
  setTimeout(function() {
    if (errorDiv && errorDiv.parentNode) {
      errorDiv.remove();
    }
  }, 5000);
}

// Logout functions
function initiateLogout() {
  document.getElementById('logoutPanel').style.display = 'flex';
}

function cancelLogout() {
  document.getElementById('logoutPanel').style.display = 'none';
}

function confirmLogout() {
  // Clear session
  currentSession = {
    isAuthenticated: false,
    adminId: null,
    adminData: null,
    loginTime: null
  };
  
  localStorage.removeItem('hemoConnectSession');
  
  // Reset form
  document.getElementById('loginForm').reset();
  
  // Show auth screen
  document.getElementById('logoutPanel').style.display = 'none';
  document.getElementById('dashboardScreen').style.display = 'none';
  document.getElementById('authScreen').style.display = 'flex';
}

// Helper functions for demo
function showForgotPassword() {
  alert('Forgot Password feature would redirect to password recovery system.\n\nFor demo purposes, use the provided credentials.');
}

function showHelp() {
  alert('Help System:\n\n1. Use demo credentials provided\n2. Select appropriate region\n3. Contact IT support for real issues\n\nDemo Admin: admin123 / hemo2026');
}

function showProfile() {
  alert('Profile Settings:\n\nThis would open admin profile management.\n\nCurrent User: ' + currentSession.adminData?.name + '\nRole: ' + currentSession.adminData?.role);
}

// Check for existing session on page load
function checkExistingSession() {
  var session = localStorage.getItem('hemoConnectSession');
  if (session) {
    try {
      var sessionData = JSON.parse(session);
      var loginTime = new Date(sessionData.loginTime);
      var now = new Date();
      
      // Check if session is still valid (less than 24 hours old)
      if (now - loginTime < 24 * 60 * 60 * 1000) {
        // Auto-login if session is valid
        if (validCredentials[sessionData.adminId]) {
          setupUserSession(sessionData.adminId, sessionData.region);
          showDashboard();
          return true;
        }
      }
    } catch (e) {
      console.warn('Invalid session data');
    }
  }
  return false;
}

// Update live indicator
function updateLiveStatus() {
  var lastUpdateEl = document.getElementById('lastUpdate');
  if (lastUpdateEl && currentSession.isAuthenticated) {
    var now = new Date();
    lastUpdateEl.textContent = 'Updated: ' + now.toLocaleTimeString('en-US', {
      hour: '2-digit',
      minute: '2-digit'
    });
  }
}

// Initialize dashboard after authentication
function initializeDashboard() {
  if (!currentSession.isAuthenticated) return;
  
  generateHeatmap();
  selectRegion('all');
  initializeMap();
  initializeDonorMap();
  
  // Start live updates
  setInterval(updateLiveStatus, 60000); // Update every minute
  updateLiveStatus();
}

// ===================================================
//    ORIGINAL DASHBOARD FUNCTIONALITY
// ===================================================

// Regional data for different regions of India
var regionalData = {
  north: {
    name: 'North Region',
    camps: 35,
    donors: 847,
    units: 1230,
    states: ['Delhi', 'Punjab', 'Haryana', 'UP', 'Himachal Pradesh']
  },
  west: {
    name: 'West Region', 
    camps: 54,
    donors: 1120,
    units: 1650,
    states: ['Maharashtra', 'Gujarat', 'Rajasthan', 'Goa']
  },
  south: {
    name: 'South Region',
    camps: 48,
    donors: 690,
    units: 980,
    states: ['Karnataka', 'Tamil Nadu', 'Andhra Pradesh', 'Kerala']
  },
  east: {
    name: 'East Region',
    camps: 19,
    donors: 190,
    units: 370,
    states: ['West Bengal', 'Odisha', 'Jharkhand', 'Bihar']
  }
};

// Map variables
var indiaMap;
var donorMap;
var bloodBankMarkers = [];
var donorMarkers = [];
var requestMarkers = [];
var heatmapLayer;
var donorHeatmapLayer;
var markerClusters;
var donorClusters;
var showHeatmap = false;
var showDonorHeatmap = false;
var showClusters = false;
var showRecentRequests = true;

// Blood bank locations with real coordinates
var bloodBankData = [
  // Major Blood Banks
  { name: "AIIMS Blood Bank", city: "Delhi", lat: 28.5672, lng: 77.2100, type: "major", camps: 15, donors: 420, category: "Government" },
  { name: "Tata Memorial Hospital", city: "Mumbai", lat: 19.0176, lng: 72.8562, type: "major", camps: 28, donors: 890, category: "Private" },
  { name: "Apollo Blood Bank", city: "Bangalore", lat: 12.9716, lng: 77.5946, type: "major", camps: 24, donors: 720, category: "Private" },
  { name: "Stanley Medical College", city: "Chennai", lat: 13.0827, lng: 80.2707, type: "major", camps: 19, donors: 580, category: "Government" },
  { name: "Medical College Blood Bank", city: "Kolkata", lat: 22.5726, lng: 88.3639, type: "major", camps: 16, donors: 480, category: "Government" },
  { name: "Civil Hospital Blood Bank", city: "Ahmedabad", lat: 23.0225, lng: 72.5714, type: "major", camps: 22, donors: 670, category: "Government" },
  { name: "NIMS Blood Bank", city: "Hyderabad", lat: 17.3850, lng: 78.4867, type: "major", camps: 13, donors: 390, category: "Private" },
  
  // Regional Centers
  { name: "PGI Blood Bank", city: "Chandigarh", lat: 30.7333, lng: 76.7794, type: "regular", camps: 8, donors: 240, category: "Government" },
  { name: "KGMU Blood Bank", city: "Lucknow", lat: 26.9124, lng: 80.9420, type: "regular", camps: 12, donors: 380, category: "Government" },
  { name: "Regional Blood Center", city: "Kochi", lat: 9.9312, lng: 76.2673, type: "regular", camps: 11, donors: 320, category: "Charitable" },
  { name: "SMS Hospital Blood Bank", city: "Jaipur", lat: 26.9124, lng: 75.7873, type: "regular", camps: 16, donors: 480, category: "Government" },
  { name: "Capital Hospital", city: "Bhubaneswar", lat: 20.2961, lng: 85.8245, type: "regular", camps: 9, donors: 270, category: "Government" },
  { name: "GMC Blood Bank", city: "Pune", lat: 18.5204, lng: 73.8567, type: "regular", camps: 14, donors: 420, category: "Government" },
  { name: "AIIMS Rishikesh", city: "Rishikesh", lat: 30.0869, lng: 78.2676, type: "regular", camps: 7, donors: 180, category: "Government" },
  { name: "JIPMER Blood Bank", city: "Puducherry", lat: 11.9416, lng: 79.8083, type: "regular", camps: 6, donors: 150, category: "Government" }
];

// Recent blood request data
var recentRequests = [
  { id: 'REQ-145', bloodGroup: 'B+', lat: 12.9716, lng: 77.5946, city: 'Bangalore', status: 'active', time: '2 mins ago', urgency: 'high' },
  { id: 'REQ-144', bloodGroup: 'O-', lat: 19.0760, lng: 72.8777, city: 'Mumbai', status: 'fulfilled', time: '15 mins ago', urgency: 'critical' },
  { id: 'REQ-143', bloodGroup: 'AB+', lat: 28.7041, lng: 77.1025, city: 'Delhi', status: 'pending', time: '1 hour ago', urgency: 'medium' },
  { id: 'REQ-142', bloodGroup: 'A+', lat: 13.0827, lng: 80.2707, city: 'Chennai', status: 'active', time: '2 hours ago', urgency: 'high' },
  { id: 'REQ-141', bloodGroup: 'O+', lat: 22.5726, lng: 88.3639, city: 'Kolkata', status: 'fulfilled', time: '3 hours ago', urgency: 'medium' },
  { id: 'REQ-140', bloodGroup: 'B-', lat: 17.3850, lng: 78.4867, city: 'Hyderabad', status: 'pending', time: '4 hours ago', urgency: 'low' }
];

// Donor availability data by location
var donorAvailability = [
  { lat: 28.7041, lng: 77.1025, city: 'Delhi', available: 420, busy: 45, bloodGroups: { 'O+': 85, 'A+': 78, 'B+': 65, 'AB+': 32, 'O-': 25, 'A-': 22, 'B-': 8, 'AB-': 5 } },
  { lat: 19.0760, lng: 72.8777, city: 'Mumbai', available: 890, busy: 78, bloodGroups: { 'O+': 178, 'A+': 156, 'B+': 142, 'AB+': 89, 'O-': 67, 'A-': 54, 'B-': 32, 'AB-': 12 } },
  { lat: 12.9716, lng: 77.5946, city: 'Bangalore', available: 720, busy: 62, bloodGroups: { 'O+': 145, 'A+': 132, 'B+': 118, 'AB+': 67, 'O-': 45, 'A-': 38, 'B-': 22, 'AB-': 8 } },
  { lat: 13.0827, lng: 80.2707, city: 'Chennai', available: 580, busy: 48, bloodGroups: { 'O+': 112, 'A+': 98, 'B+': 87, 'AB+': 52, 'O-': 34, 'A-': 28, 'B-': 18, 'AB-': 6 } },
  { lat: 22.5726, lng: 88.3639, city: 'Kolkata', available: 480, busy: 42, bloodGroups: { 'O+': 95, 'A+': 82, 'B+': 74, 'AB+': 45, 'O-': 28, 'A-': 24, 'B-': 15, 'AB-': 4 } },
  { lat: 23.0225, lng: 72.5714, city: 'Ahmedabad', available: 670, busy: 56, bloodGroups: { 'O+': 134, 'A+': 118, 'B+': 98, 'AB+': 62, 'O-': 42, 'A-': 35, 'B-': 25, 'AB-': 8 } }
];

// State center coordinates for focusing
var stateCoordinates = {
  delhi: [28.6139, 77.2090],
  maharashtra: [19.7515, 75.7139], 
  karnataka: [15.3173, 75.7139],
  tamilnadu: [11.1271, 78.6569],
  westbengal: [22.9868, 87.8550],
  gujarat: [23.0225, 72.5714],
  rajasthan: [27.0238, 74.2179],
  punjab: [31.1471, 75.3412],
  up: [26.8467, 80.9462]
};

// Add missing functions for state selection
function focusOnState(state) {
  if (!state || !stateCoordinates[state]) {
    resetMapView();
    return;
  }
  
  var coords = stateCoordinates[state];
  indiaMap.setView(coords, 7);
  
  // Update regional stats for selected state
  // This would filter data and show relevant stats
  console.log('Focused on state:', state);
}

function toggleClusters() {
  var btn = event.target;
  if (showClusters) {
    indiaMap.removeLayer(markerClusters);
    bloodBankMarkers.forEach(function(marker) {
      indiaMap.addLayer(marker);
    });
    showClusters = false;
    btn.classList.remove('active');
    btn.textContent = 'Show Clusters';
  } else {
    bloodBankMarkers.forEach(function(marker) {
      indiaMap.removeLayer(marker);
    });
    indiaMap.addLayer(markerClusters);
    showClusters = true;
    btn.classList.add('active');
    btn.textContent = 'Hide Clusters';
  }
}

// Initialize dashboard when page loads
document.addEventListener('DOMContentLoaded', function() {
  // Check for existing session first
  if (!checkExistingSession()) {
    // No valid session - show login screen
    document.getElementById('authScreen').style.display = 'flex';
    document.getElementById('dashboardScreen').style.display = 'none';
  }
  // If session exists, dashboard is already shown by checkExistingSession()
});

// Initialize real map with Leaflet
function initializeMap() {
  // Initialize map centered on India
  indiaMap = L.map('indiaMap').setView([20.5937, 78.9629], 5);
  
  // Add OpenStreetMap tiles (slightly desaturated to highlight overlays)
  L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
    attribution: '© OpenStreetMap contributors'
  }).addTo(indiaMap);
  
  // Initialize marker clusters
  markerClusters = L.markerClusterGroup({
    maxClusterRadius: 50,
    disableClusteringAtZoom: 8
  });
  
  // Add blood bank markers
  addBloodBankMarkers();
  
  // Set bounds to India
  var indiaBounds = [[6.4627, 68.1097], [35.5128, 97.3953]];
  indiaMap.fitBounds(indiaBounds);
  indiaMap.setMaxBounds(indiaBounds);
  
  // Show heatmap by default to demonstrate colors
  setTimeout(function() {
    createHeatmapLayer();
    showHeatmap = true;
    var heatmapBtn = document.querySelector('.map-btn[onclick="toggleHeatmap()"]');
    if (heatmapBtn) {
      heatmapBtn.classList.add('active');
      heatmapBtn.textContent = 'Hide Heatmap';
    }
    showHeatmapLegend();
    document.getElementById('heatmapInfo').style.display = 'block';
  }, 500);
}

// Initialize donor availability map
function initializeDonorMap() {
  // Check if donorMap element exists
  if (!document.getElementById('donorMap')) {
    console.warn('donorMap element not found');
    return;
  }
  
  // Initialize donor map centered on India
  donorMap = L.map('donorMap').setView([20.5937, 78.9629], 5);
  
  // Add OpenStreetMap tiles
  L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
    attribution: '© OpenStreetMap contributors'
  }).addTo(donorMap);
  
  // Initialize donor clusters
  donorClusters = L.markerClusterGroup({
    maxClusterRadius: 40,
    disableClusteringAtZoom: 7
  });
  
  // Add donor availability markers
  addDonorMarkers();
  
  // Add recent request markers
  addRecentRequestMarkers();
  
  // Set bounds to India
  var indiaBounds = [[6.4627, 68.1097], [35.5128, 97.3953]];
  donorMap.fitBounds(indiaBounds);
  donorMap.setMaxBounds(indiaBounds);
  
  donorMap.addLayer(donorClusters);
}

function addDonorMarkers() {
  donorAvailability.forEach(function(location) {
    // Create donor availability marker
    var donorHtml = `<div class="donor-marker available"></div>`;
    var donorIcon = L.divIcon({
      html: donorHtml,
      className: 'custom-donor-marker',
      iconSize: [20, 20],
      iconAnchor: [10, 10]
    });
    
    var marker = L.marker([location.lat, location.lng], { icon: donorIcon });
    
    // Create detailed popup
    var popupContent = `
      <div class="donor-popup">
        <div class="popup-title">${location.city} - Donor Availability</div>
        <div class="donor-stats">
          <div class="availability-summary">
            <span class="available-count">${location.available}</span> Available |
            <span class="busy-count">${location.busy}</span> Busy
          </div>
          <div class="blood-group-breakdown">
            <div class="bg-row"><span class="bg-label">O+:</span> <span>${location.bloodGroups['O+']}</span></div>
            <div class="bg-row"><span class="bg-label">A+:</span> <span>${location.bloodGroups['A+']}</span></div>
            <div class="bg-row"><span class="bg-label">B+:</span> <span>${location.bloodGroups['B+']}</span></div>
            <div class="bg-row"><span class="bg-label">AB+:</span> <span>${location.bloodGroups['AB+']}</span></div>
          </div>
        </div>
      </div>
    `;
    
    marker.bindPopup(popupContent);
    donorMarkers.push(marker);
    donorClusters.addLayer(marker);
  });
}

function addRecentRequestMarkers() {
  recentRequests.forEach(function(request) {
    var urgencyClass = request.urgency;
    var statusClass = request.status;
    
    var requestHtml = `<div class="request-marker ${urgencyClass} ${statusClass}">${request.bloodGroup}</div>`;
    var requestIcon = L.divIcon({
      html: requestHtml,
      className: 'custom-request-marker',
      iconSize: [24, 24],
      iconAnchor: [12, 12]
    });
    
    var marker = L.marker([request.lat, request.lng], { icon: requestIcon });
    
    var popupContent = `
      <div class="request-popup">
        <div class="popup-title">${request.id} - ${request.city}</div>
        <div class="request-details">
          <div><strong>Blood Group:</strong> ${request.bloodGroup}</div>
          <div><strong>Status:</strong> <span class="status-${request.status}">${request.status.toUpperCase()}</span></div>
          <div><strong>Urgency:</strong> <span class="urgency-${request.urgency}">${request.urgency.toUpperCase()}</span></div>
          <div><strong>Time:</strong> ${request.time}</div>
        </div>
      </div>
    `;
    
    marker.bindPopup(popupContent);
    requestMarkers.push(marker);
    
    if (showRecentRequests) {
      donorMap.addLayer(marker);
    }
  });
}

// Donor map control functions
function resetDonorMapView() {
  var indiaBounds = [[6.4627, 68.1097], [35.5128, 97.3953]];
  donorMap.fitBounds(indiaBounds);
}

function toggleDonorHeatmap() {
  var btn = event.target;
  if (showDonorHeatmap) {
    if (donorHeatmapLayer) {
      donorMap.removeLayer(donorHeatmapLayer);
    }
    showDonorHeatmap = false;
    btn.classList.remove('active');
    btn.textContent = 'Show Donor Heatmap';
  } else {
    createDonorHeatmapLayer();
    showDonorHeatmap = true;
    btn.classList.add('active');
    btn.textContent = 'Hide Donor Heatmap';
  }
}

function toggleRecentRequests() {
  var btn = event.target;
  if (showRecentRequests) {
    requestMarkers.forEach(function(marker) {
      donorMap.removeLayer(marker);
    });
    showRecentRequests = false;
    btn.classList.remove('active');
    btn.textContent = 'Show Recent Requests';
  } else {
    requestMarkers.forEach(function(marker) {
      donorMap.addLayer(marker);
    });
    showRecentRequests = true;
    btn.classList.add('active');
    btn.textContent = 'Hide Recent Requests';
  }
}

function filterByBloodGroup(bloodGroup) {
  // Filter logic would go here
  console.log('Filtering by blood group:', bloodGroup);
}

function createDonorHeatmapLayer() {
  var donorPoints = donorAvailability.map(function(location) {
    return [location.lat, location.lng, location.available / 200];
  });
  
  donorPoints.forEach(function(point) {
    var intensity = point[2];
    var color = getDonorHeatmapColor(intensity);
    
    var circle = L.circle([point[0], point[1]], {
      color: color.border,
      fillColor: color.fill,
      fillOpacity: 0.4 + (intensity * 0.3),
      opacity: 1.0,
      weight: 2,
      radius: intensity * 35000 + 25000
    });
    
    if (!donorHeatmapLayer) {
      donorHeatmapLayer = L.layerGroup();
    }
    donorHeatmapLayer.addLayer(circle);
  });
  
  if (donorHeatmapLayer) {
    donorMap.addLayer(donorHeatmapLayer);
  }
}

function getDonorHeatmapColor(intensity) {
  var colors = {
    veryHigh: { fill: '#00ff00', border: '#00cc00' }, // Green for high availability
    high: { fill: '#66ff66', border: '#00ff00' },
    medium: { fill: '#ffff00', border: '#cccc00' }, // Yellow for medium
    low: { fill: '#ff9900', border: '#cc6600' }, // Orange for low
    veryLow: { fill: '#ff3300', border: '#cc0000' } // Red for very low
  };
  
  if (intensity >= 3.0) return colors.veryHigh;
  if (intensity >= 2.0) return colors.high;
  if (intensity >= 1.0) return colors.medium;
  if (intensity >= 0.5) return colors.low;
  return colors.veryLow;
}

// Add blood bank markers to the main map
function addBloodBankMarkers() {
  bloodBankData.forEach(function(bank) {
    // Create custom marker
    var markerHtml = `<div class="blood-bank-marker ${bank.type}"></div>`;
    var customIcon = L.divIcon({
      html: markerHtml,
      className: 'custom-marker',
      iconSize: bank.type === 'major' ? [16, 16] : [12, 12],
      iconAnchor: bank.type === 'major' ? [8, 8] : [6, 6]
    });
    
    var marker = L.marker([bank.lat, bank.lng], { icon: customIcon });
    
    // Create popup content
    var popupContent = `
      <div class="blood-donation-popup">
        <div class="popup-title">${bank.name}</div>
        <div class="popup-stats">
          <strong>${bank.city}</strong><br>
          ${bank.camps} Active Camps<br>
          ${bank.donors} Registered Donors<br>
          Category: ${bank.category}
        </div>
      </div>
    `;
    
    marker.bindPopup(popupContent);
    marker.on('click', function() {
      updateRegionalStatsForBank(bank);
    });
    
    bloodBankMarkers.push(marker);
    markerClusters.addLayer(marker);
  });
  
  indiaMap.addLayer(markerClusters);
}

// Map control functions
function resetMapView() {
  var indiaBounds = [[6.4627, 68.1097], [35.5128, 97.3953]];
  indiaMap.fitBounds(indiaBounds);
  clearSelection();
}

function toggleHeatmap() {
  var btn = event.target;
  if (showHeatmap) {
    if (heatmapLayer) {
      indiaMap.removeLayer(heatmapLayer);
    }
    showHeatmap = false;
    btn.classList.remove('active');
    btn.textContent = 'Show Heatmap';
    hideHeatmapLegend();
  } else {
    createHeatmapLayer();
    showHeatmap = true;
    btn.classList.add('active');
    btn.textContent = 'Hide Heatmap';
  }
}

function showHeatmapLegend() {
  // Remove existing legend
  hideHeatmapLegend();
  
  var legendControl = L.control({ position: 'bottomright' });
  
  legendControl.onAdd = function(map) {
    var div = L.DomUtil.create('div', 'heatmap-legend');
    div.innerHTML = `
      <div class="legend-title">Blood Donation Activity</div>
      <div class="legend-item"><span class="legend-color very-high"></span> Very High (90%+)</div>
      <div class="legend-item"><span class="legend-color high"></span> High (80-90%)</div>
      <div class="legend-item"><span class="legend-color medium-high"></span> Medium-High (60-80%)</div>
      <div class="legend-item"><span class="legend-color medium"></span> Medium (40-60%)</div>
      <div class="legend-item"><span class="legend-color medium-low"></span> Medium-Low (30-40%)</div>
      <div class="legend-item"><span class="legend-color low"></span> Low (20-30%)</div>
      <div class="legend-item"><span class="legend-color very-low"></span> Very Low (0-20%)</div>
    `;
    return div;
  };
  
  legendControl.addTo(indiaMap);
  window.currentLegend = legendControl;
}

function hideHeatmapLegend() {
  if (window.currentLegend) {
    indiaMap.removeControl(window.currentLegend);
    window.currentLegend = null;
  }
}

function toggleClusters() {
  var btn = event.target;
  if (showClusters) {
    indiaMap.removeLayer(markerClusters);
    bloodBankMarkers.forEach(function(marker) {
      indiaMap.addLayer(marker);
    });
    showClusters = false;
    btn.classList.remove('active');
    btn.textContent = 'Enable Clusters';
  } else {
    bloodBankMarkers.forEach(function(marker) {
      indiaMap.removeLayer(marker);
    });
    indiaMap.addLayer(markerClusters);
    showClusters = true;
    btn.classList.add('active');
    btn.textContent = 'Disable Clusters';
  }
}

function createHeatmapLayer() {
  // Use only major city points for cleaner visualization
  var majorCityPoints = [
    [28.7041, 77.1025, 1.2], // Delhi area - very high activity
    [19.0760, 72.8777, 1.4], // Mumbai area - very high activity
    [12.9716, 77.5946, 1.1], // Bangalore area - very high activity
    [13.0827, 80.2707, 0.9], // Chennai area - high activity
    [22.5726, 88.3639, 0.7], // Kolkata area - medium-high activity
    [23.0225, 72.5714, 0.8], // Ahmedabad - high activity
    [17.3850, 78.4867, 0.6], // Hyderabad - medium-high activity
    [26.9124, 75.7873, 0.4], // Jaipur - medium activity
  ];
  
  // Filter blood bank data - only show significant activity (>0.3 intensity)
  var significantBloodBanks = bloodBankData
    .map(function(bank) {
      return [bank.lat, bank.lng, bank.donors / 100];
    })
    .filter(function(point) {
      return point[2] >= 0.3; // Only show medium+ activity
    });
  
  var allPoints = significantBloodBanks.concat(majorCityPoints);
  
  // Create smaller, cleaner circles
  allPoints.forEach(function(point) {
    var intensity = point[2];
    var color = getHeatmapColor(intensity);
    
    var circle = L.circle([point[0], point[1]], {
      color: color.border,
      fillColor: color.fill,
      fillOpacity: Math.max(0.2, intensity * 0.4), // Reduced opacity
      opacity: 0.8,
      weight: 1, // Thinner borders
      radius: Math.max(15000, intensity * 25000) // Smaller, more proportional circles
    });
    
    // Add popup with intensity info
    circle.bindPopup(
      `<div class="heatmap-popup">
        <strong>Blood Donation Activity</strong><br>
        Intensity: ${(intensity * 100).toFixed(0)}%<br>
        Activity Level: ${getActivityLevel(intensity)}<br>
        <span style="color: ${color.fill};">● ${color.fill}</span>
      </div>`
    );
    
    if (!heatmapLayer) {
      heatmapLayer = L.layerGroup();
    }
    heatmapLayer.addLayer(circle);
  });
  
  if (heatmapLayer) {
    indiaMap.addLayer(heatmapLayer);
  }
}

// Get color based on heatmap intensity (red = high, blue = low)
function getHeatmapColor(intensity) {
  var colors = {
    // Very High (1.0+) - Bright Red
    veryHigh: { fill: '#ff0000', border: '#cc0000' },
    // High (0.8-1.0) - Orange-Red 
    high: { fill: '#ff4500', border: '#ff0000' },
    // Medium-High (0.6-0.8) - Orange
    mediumHigh: { fill: '#ff8c00', border: '#ff4500' },
    // Medium (0.4-0.6) - Yellow-Orange
    medium: { fill: '#ffd700', border: '#ff8c00' },
    // Medium-Low (0.3-0.4) - Yellow-Green
    mediumLow: { fill: '#9acd32', border: '#ffd700' },
    // Low (0.2-0.3) - Green
    low: { fill: '#32cd32', border: '#9acd32' },
    // Very Low (0.0-0.2) - Blue
    veryLow: { fill: '#1e90ff', border: '#32cd32' }
  };
  
  if (intensity >= 1.0) return colors.veryHigh;
  if (intensity >= 0.8) return colors.high;
  if (intensity >= 0.6) return colors.mediumHigh;
  if (intensity >= 0.4) return colors.medium;
  if (intensity >= 0.3) return colors.mediumLow;
  if (intensity >= 0.2) return colors.low;
  return colors.veryLow;
}

function getActivityLevel(intensity) {
  if (intensity >= 1.0) return 'Very High';
  if (intensity >= 0.8) return 'High';
  if (intensity >= 0.6) return 'Medium-High';
  if (intensity >= 0.4) return 'Medium';
  if (intensity >= 0.3) return 'Medium-Low';
  if (intensity >= 0.2) return 'Low';
  return 'Very Low';
}

function focusOnState(stateId) {
  if (!stateId || !stateCoordinates[stateId]) {
    resetMapView();
    return;
  }
  
  var coords = stateCoordinates[stateId];
  indiaMap.setView(coords, 7);
  
  // Update regional stats
  updateRegionalStatsForState(stateId);
  
  // Highlight relevant markers
  highlightStateMarkers(stateId);
}

function highlightStateMarkers(stateId) {
  // Clear previous highlights
  clearSelection();
  
  // Get state name for filtering
  var stateNames = {
    delhi: 'Delhi',
    maharashtra: 'Mumbai',
    karnataka: 'Bangalore', 
    tamilnadu: 'Chennai',
    westbengal: 'Kolkata',
    gujarat: 'Ahmedabad',
    rajasthan: 'Jaipur',
    punjab: 'Chandigarh',
    up: 'Lucknow'
  };
  
  var targetCity = stateNames[stateId];
  if (targetCity) {
    bloodBankMarkers.forEach(function(marker) {
      var bank = bloodBankData.find(b => 
        Math.abs(b.lat - marker.getLatLng().lat) < 0.1 && 
        Math.abs(b.lng - marker.getLatLng().lng) < 0.1
      );
      
      if (bank && bank.city === targetCity) {
        marker.openPopup();
      }
    });
  }
}

function clearSelection() {
  indiaMap.closePopup();
  selectRegion('all');
}

function updateRegionalStatsForBank(bank) {
  var regionDetails = document.getElementById('regionDetails');
  var regionCard = `
    <div class="region-stat-card">
      <div class="region-name">${bank.name} - ${bank.city}</div>
      <div class="region-metrics">
        <div class="region-metric">
          <span class="metric-label">Blood Camps</span>
          <span class="metric-value">${bank.camps}</span>
        </div>
        <div class="region-metric">
          <span class="metric-label">Active Donors</span>
          <span class="metric-value">${bank.donors.toLocaleString()}</span>
        </div>
        <div class="region-metric">
          <span class="metric-label">Category</span>
          <span class="metric-value">${bank.category}</span>
        </div>
        <div class="region-metric">
          <span class="metric-label">Type</span>
          <span class="metric-value">${bank.type === 'major' ? 'Major Center' : 'Regional Center'}</span>
        </div>
      </div>
    </div>
  `;
  
  regionDetails.innerHTML = regionCard;
}

// Update regional stats for specific state
function updateRegionalStatsForState(stateId) {
  var stateData = getStateData(stateId);
  if (!stateData) return;
  
  var regionDetails = document.getElementById('regionDetails');
  var regionCard = `
    <div class="region-stat-card">
      <div class="region-name">${stateData.name}</div>
      <div class="region-metrics">
        <div class="region-metric">
          <span class="metric-label">Blood Camps</span>
          <span class="metric-value">${stateData.camps}</span>
        </div>
        <div class="region-metric">
          <span class="metric-label">Active Donors</span>
          <span class="metric-value">${stateData.donors.toLocaleString()}</span>
        </div>
        <div class="region-metric">
          <span class="metric-label">Units Available</span>
          <span class="metric-value">${stateData.units}</span>
        </div>
        <div class="region-metric">
          <span class="metric-label">Last Collection</span>
          <span class="metric-value">${stateData.lastCollection}</span>
        </div>
      </div>
    </div>
  `;
  
  regionDetails.innerHTML = regionCard;
}

// Get state-specific data
function getStateData(stateId) {
  var stateDataMap = {
    delhi: { name: 'Delhi', camps: 15, donors: 420, units: 280, lastCollection: '2 hours ago' },
    punjab: { name: 'Punjab', camps: 12, donors: 380, units: 240, lastCollection: '4 hours ago' },
    maharashtra: { name: 'Maharashtra', camps: 28, donors: 890, units: 650, lastCollection: '1 hour ago' },
    karnataka: { name: 'Karnataka', camps: 24, donors: 720, units: 580, lastCollection: '3 hours ago' },
    tamilnadu: { name: 'Tamil Nadu', camps: 19, donors: 580, units: 420, lastCollection: '2 hours ago' },
    westbengal: { name: 'West Bengal', camps: 16, donors: 480, units: 350, lastCollection: '5 hours ago' },
    gujarat: { name: 'Gujarat', camps: 22, donors: 670, units: 490, lastCollection: '1 hour ago' },
    rajasthan: { name: 'Rajasthan', camps: 16, donors: 480, units: 320, lastCollection: '6 hours ago' },
    up: { name: 'Uttar Pradesh', camps: 18, donors: 650, units: 480, lastCollection: '3 hours ago' }
  };
  
  return stateDataMap[stateId] || null;
}

// Region selection functionality
function selectRegion(regionKey) {
  // Remove previous selections
  document.querySelectorAll('.region').forEach(function(region) {
    region.classList.remove('selected');
  });
  
  // Update region details
  var regionDetails = document.getElementById('regionDetails');
  var regionCard = '';
  
  if (regionKey === 'all') {
    regionCard = `
      <div class="region-stat-card">
        <div class="region-name">All Regions Overview</div>
        <div class="region-metrics">
          <div class="region-metric">
            <span class="metric-label">Total Camps</span>
            <span class="metric-value">156</span>
          </div>
          <div class="region-metric">
            <span class="metric-label">Active Donors</span>
            <span class="metric-value">2,847</span>
          </div>
          <div class="region-metric">
            <span class="metric-label">Units Collected</span>
            <span class="metric-value">4,230</span>
          </div>
          <div class="region-metric">
            <span class="metric-label">Avg Response Time</span>
            <span class="metric-value">16 mins</span>
          </div>
        </div>
      </div>
    `;
  } else {
    // Select the clicked region
    var regionElement = document.querySelector('[data-region="' + regionKey + '"]');
    if (regionElement) {
      regionElement.classList.add('selected');
    }
    
    var data = regionalData[regionKey];
    if (data) {
      regionCard = `
        <div class="region-stat-card">
          <div class="region-name">${data.name}</div>
          <div class="region-metrics">
            <div class="region-metric">
              <span class="metric-label">Total Camps</span>
              <span class="metric-value">${data.camps}</span>
            </div>
            <div class="region-metric">
              <span class="metric-label">Active Donors</span>
              <span class="metric-value">${data.donors.toLocaleString()}</span>
            </div>
            <div class="region-metric">
              <span class="metric-label">Units Collected</span>
              <span class="metric-value">${data.units.toLocaleString()}</span>
            </div>
            <div class="region-metric">
              <span class="metric-label">Coverage States</span>
              <span class="metric-value">${data.states.length}</span>
            </div>
          </div>
          <div style="margin-top: 12px; font-size: 12px; color: #888;">
            States: ${data.states.join(', ')}
          </div>
        </div>
      `;
    }
  }
  
  regionDetails.innerHTML = regionCard;
}

// Generate heatmap for request activity
function generateHeatmap() {
  var heatmapCells = document.getElementById('heatmapCells');
  var html = '';
  
  // Generate 7 rows (days) x 6 columns (time slots)
  for (var day = 0; day < 7; day++) {
    html += '<div class="heatmap-row">';
    for (var hour = 0; hour < 6; hour++) {
      var level = getHeatmapLevel(day, hour);
      var requests = getRequestCount(day, hour);
      var tooltip = `${getDayName(day)} ${getHourName(hour)}: ${requests} requests`;
      html += `<div class="heatmap-cell level-${level}" title="${tooltip}" data-day="${day}" data-hour="${hour}"></div>`;
    }
    html += '</div>';
  }
  
  heatmapCells.innerHTML = html;
  
  // Add click handlers to heatmap cells
  document.querySelectorAll('.heatmap-cell').forEach(function(cell) {
    cell.addEventListener('click', function() {
      var day = parseInt(this.dataset.day);
      var hour = parseInt(this.dataset.hour);
      showHeatmapDetails(day, hour);
    });
  });
}

function getDayName(day) {
  var days = ['Monday', 'Tuesday', 'Wednesday', 'Thursday', 'Friday', 'Saturday', 'Sunday'];
  return days[day];
}

function getHourName(hour) {
  var hours = ['6AM-9AM', '9AM-12PM', '12PM-3PM', '3PM-6PM', '6PM-9PM', '9PM+'];
  return hours[hour];
}

// Calculate heatmap intensity level based on day and hour
function getHeatmapLevel(day, hour) {
  // Simulate realistic patterns:
  // - Higher activity during work hours (9AM-6PM = hours 1-4)
  // - Lower activity on weekends (days 5-6)
  // - Peak activity on weekday afternoons
  
  var isWeekend = day >= 5;
  var isWorkHours = hour >= 1 && hour <= 4;
  var isPeakHours = hour >= 2 && hour <= 3;
  
  var baseLevel = 1;
  
  if (isWeekend) {
    baseLevel = Math.max(0, baseLevel - 1);
  }
  
  if (isWorkHours) {
    baseLevel += 1;
  }
  
  if (isPeakHours && !isWeekend) {
    baseLevel += 1;
  }
  
  // Add some randomness
  var randomFactor = Math.random();
  if (randomFactor > 0.7) baseLevel += 1;
  if (randomFactor < 0.3) baseLevel = Math.max(0, baseLevel - 1);
  
  return Math.min(4, Math.max(0, baseLevel));
}

// Get request count for tooltip
function getRequestCount(day, hour) {
  var level = getHeatmapLevel(day, hour);
  var baseCounts = [2, 8, 15, 23, 35];
  var variance = Math.floor(Math.random() * 6) - 3;
  return Math.max(0, baseCounts[level] + variance);
}

// Show details when heatmap cell is clicked
function showHeatmapDetails(day, hour) {
  var dayNames = ['Monday', 'Tuesday', 'Wednesday', 'Thursday', 'Friday', 'Saturday', 'Sunday'];
  var hourNames = ['6AM-9AM', '9AM-12PM', '12PM-3PM', '3PM-6PM', '6PM-9PM', '9PM+'];
  var requests = getRequestCount(day, hour);
  var level = getHeatmapLevel(day, hour);
  
  var activityLevel = ['Very Low', 'Low', 'Medium', 'High', 'Very High'][level];
  var color = ['#f0f8ff', '#87ceeb', '#4682b4', '#ff8c00', '#ff4500'][level];
  
  alert(`${dayNames[day]} ${hourNames[hour]}\n${requests} requests\nActivity Level: ${activityLevel}`);
}

// Simulate real-time updates
function updateMetrics() {
  // Update active requests count
  var activeRequestsElement = document.querySelector('.metric-card:nth-child(2) .metric-value');
  if (activeRequestsElement) {
    var currentCount = parseInt(activeRequestsElement.textContent);
    var change = Math.floor(Math.random() * 5) - 2; // -2 to +2
    var newCount = Math.max(0, currentCount + change);
    activeRequestsElement.textContent = newCount;
  }
  
  // Update fulfillment rate
  var fulfillmentElement = document.querySelector('.metric-card:nth-child(4) .metric-value');
  if (fulfillmentElement) {
    var rates = ['92%', '93%', '94%', '95%', '96%'];
    fulfillmentElement.textContent = rates[Math.floor(Math.random() * rates.length)];
  }
}

// Simulate new request arrival
function addNewRequest() {
  var tbody = document.getElementById('recentRequestsBody');
  if (!tbody) return;
  
  var newRequestId = 'REQ-' + (146 + Math.floor(Math.random() * 50));
  var bloodGroups = ['A+', 'A-', 'B+', 'B-', 'AB+', 'AB-', 'O+', 'O-'];
  var locations = [
    'Mumbai - 400001',
    'Delhi - 110001', 
    'Bangalore - 560001',
    'Chennai - 600001',
    'Kolkata - 700001',
    'Pune - 411001'
  ];
  
  var bloodGroup = bloodGroups[Math.floor(Math.random() * bloodGroups.length)];
  var location = locations[Math.floor(Math.random() * locations.length)];
  
  var newRow = `
    <tr style="background-color: #f0f0f0;">
      <td><strong>${newRequestId}</strong></td>
      <td>${bloodGroup}</td>
      <td>${location}</td>
      <td><span class="status-badge active">Active</span></td>
      <td>0 responses</td>
      <td>Just now</td>
    </tr>
  `;
  
  tbody.insertAdjacentHTML('afterbegin', newRow);
  
  // Remove last row to keep table size manageable
  var rows = tbody.querySelectorAll('tr');
  if (rows.length > 6) {
    rows[rows.length - 1].remove();
  }
  
  // Highlight new row briefly
  setTimeout(function() {
    var firstRow = tbody.querySelector('tr');
    if (firstRow) {
      firstRow.style.backgroundColor = '';
    }
  }, 2000);
}

// Auto-refresh functions
setInterval(updateMetrics, 30000); // Update metrics every 30 seconds
setInterval(addNewRequest, 45000); // Add new request every 45 seconds

// Additional utility functions for demo purposes
function exportData() {
  alert('Export functionality would download CSV/Excel files with current analytics data.');
}

function refreshDashboard() {
  generateHeatmap();
  updateMetrics();
  alert('Dashboard data refreshed successfully!');
}

// Add the missing donor map functionality

// Keyboard shortcuts
document.addEventListener('keydown', function(e) {
  if (e.ctrlKey || e.metaKey) {
    switch(e.key) {
      case 'r':
        e.preventDefault();
        refreshDashboard();
        break;
      case 'e':
        e.preventDefault();
        exportData();
        break;
    }
  }
});  var btnLoader = loginBtn.querySelector('.btn-loader');
  
  btnText.style.display = 'none';
  btnLoader.style.display = 'inline';
  loginBtn.disabled = true;
  
  // Simulate authentication delay
  setTimeout(function() {
    if (validateCredentials(adminId, password, region)) {
      // Success - setup session and show dashboard
      setupUserSession(adminId, region);
      showDashboard();
    } else {
      // Failed authentication
      showLoginError();
      btnText.style.display = 'inline';
      btnLoader.style.display = 'none';
      loginBtn.disabled = false;
    }
  }, 1500);
  
  return false;
}

function validateCredentials(adminId, password, region) {
  if (!validCredentials[adminId]) {
    return false;
  }
  
  var user = validCredentials[adminId];
  if (user.password !== password) {
    return false;
  }
  
  // Check if region matches (or if admin has access to all regions)
  if (user.region !== 'All Regions' && user.region !== region) {
    return false;
  }
  
  return true;
}

function setupUserSession(adminId, selectedRegion) {
  var userData = validCredentials[adminId];
  
  currentSession.isAuthenticated = true;
  currentSession.adminId = adminId;
  currentSession.adminData = userData;
  currentSession.loginTime = new Date();
  
  // Update UI elements with user info
  updateHeaderInfo(userData, selectedRegion);
  
  // Store session (in real app, would use secure tokens)
  localStorage.setItem('hemoConnectSession', JSON.stringify({
    adminId: adminId,
    loginTime: currentSession.loginTime.toISOString(),
    region: selectedRegion
  }));
}

function updateHeaderInfo(userData, region) {
  // Update admin info in header
  document.getElementById('adminName').textContent = userData.name;
  document.getElementById('adminRole').textContent = userData.role;
  document.getElementById('adminRegion').textContent = region;
  document.getElementById('loginTime').textContent = 'Logged in: ' + new Date().toLocaleTimeString('en-US', {
    hour: '2-digit',
    minute: '2-digit'
  });
}

function showDashboard() {
  document.getElementById('authScreen').style.display = 'none';
  document.getElementById('dashboardScreen').style.display = 'block';
  
  // Initialize dashboard components
  setTimeout(function() {
    initializeDashboard(); 
  }, 100);
}

function showLoginError() {
  // Create or update error message
  var existingError = document.querySelector('.login-error');
  if (existingError) {
    existingError.remove();
  }
  
  var errorDiv = document.createElement('div');
  errorDiv.className = 'login-error';
  errorDiv.innerHTML = `
    <div class="error-content">
      ⚠️ <strong>Authentication Failed</strong><br>
      Please check your credentials and try again.
    </div>
  `;
  
  var form = document.getElementById('loginForm');
  form.insertBefore(errorDiv, form.firstChild);
  
  // Add error styling
  var style = document.createElement('style');
  style.textContent = `
    .login-error {
      background: #fee;
      border: 1px solid #fcc;
      border-radius: 6px;
      padding: 12px;
      margin-bottom: 20px;
      animation: shake 0.5s ease-in-out;
    }
    .error-content {
      font-size: 13px;
      color: #c33;
      text-align: center;
    }
    @keyframes shake {
      0%, 100% { transform: translateX(0); }
      25% { transform: translateX(-5px); }
      75% { transform: translateX(5px); }
    }
  `;
  document.head.appendChild(style);
  
  // Remove error after 5 seconds
  setTimeout(function() {
    if (errorDiv && errorDiv.parentNode) {
      errorDiv.remove();
    }
  }, 5000);
}

// Logout functions
function initiateLogout() {
  document.getElementById('logoutPanel').style.display = 'flex';
}

function cancelLogout() {
  document.getElementById('logoutPanel').style.display = 'none';
}

function confirmLogout() {
  // Clear session
  currentSession = {
    isAuthenticated: false,
    adminId: null,
    adminData: null,
    loginTime: null
  };
  
  localStorage.removeItem('hemoConnectSession');
  
  // Reset form
  document.getElementById('loginForm').reset();
  
  // Show auth screen
  document.getElementById('logoutPanel').style.display = 'none';
  document.getElementById('dashboardScreen').style.display = 'none';
  document.getElementById('authScreen').style.display = 'flex';
}

// Helper functions for demo
function showForgotPassword() {
  alert('Forgot Password feature would redirect to password recovery system.\n\nFor demo purposes, use the provided credentials.');
}

function showHelp() {
  alert('Help System:\n\n1. Use demo credentials provided\n2. Select appropriate region\n3. Contact IT support for real issues\n\nDemo Admin: admin123 / hemo2026');
}

function showProfile() {
  alert('Profile Settings:\n\nThis would open admin profile management.\n\nCurrent User: ' + currentSession.adminData?.name + '\nRole: ' + currentSession.adminData?.role);
}

// Check for existing session on page load
function checkExistingSession() {
  var session = localStorage.getItem('hemoConnectSession');
  if (session) {
    try {
      var sessionData = JSON.parse(session);
      var loginTime = new Date(sessionData.loginTime);
      var now = new Date();
      
      // Check if session is still valid (less than 24 hours old)
      if (now - loginTime < 24 * 60 * 60 * 1000) {
        // Auto-login if session is valid
        if (validCredentials[sessionData.adminId]) {
          setupUserSession(sessionData.adminId, sessionData.region);
          showDashboard();
          return true;
        }
      }
    } catch (e) {
      console.warn('Invalid session data');
    }
  }
  return false;
}

// Update live indicator
function updateLiveStatus() {
  var lastUpdateEl = document.getElementById('lastUpdate');
  if (lastUpdateEl && currentSession.isAuthenticated) {
    var now = new Date();
    lastUpdateEl.textContent = 'Updated: ' + now.toLocaleTimeString('en-US', {
      hour: '2-digit',
      minute: '2-digit'
    });
  }
}

// Initialize dashboard after authentication
function initializeDashboard() {
  if (!currentSession.isAuthenticated) return;
  
  generateHeatmap();
  selectRegion('all');
  initializeMap();
  initializeDonorMap();
  
  // Start live updates
  setInterval(updateLiveStatus, 60000); // Update every minute
  updateLiveStatus();
}

// ===================================================
//    ORIGINAL DASHBOARD FUNCTIONALITY
// ===================================================

// Regional data for different regions of India
var regionalData = {
  north: {
    name: 'North Region',
    camps: 35,
    donors: 847,
    units: 1230,
    states: ['Delhi', 'Punjab', 'Haryana', 'UP', 'Himachal Pradesh']
  },
  west: {
    name: 'West Region', 
    camps: 54,
    donors: 1120,
    units: 1650,
    states: ['Maharashtra', 'Gujarat', 'Rajasthan', 'Goa']
  },
  south: {
    name: 'South Region',
    camps: 48,
    donors: 690,
    units: 980,
    states: ['Karnataka', 'Tamil Nadu', 'Andhra Pradesh', 'Kerala']
  },
  east: {
    name: 'East Region',
    camps: 19,
    donors: 190,
    units: 370,
    states: ['West Bengal', 'Odisha', 'Jharkhand', 'Bihar']
  }
};

// Map variables
var indiaMap;
var donorMap;
var bloodBankMarkers = [];
var donorMarkers = [];
var requestMarkers = [];
var heatmapLayer;
var donorHeatmapLayer;
var markerClusters;
var donorClusters;
var showHeatmap = false;
var showDonorHeatmap = false;
var showClusters = false;
var showRecentRequests = true;

// Blood bank locations with real coordinates
var bloodBankData = [
  // Major Blood Banks
  { name: "AIIMS Blood Bank", city: "Delhi", lat: 28.5672, lng: 77.2100, type: "major", camps: 15, donors: 420, category: "Government" },
  { name: "Tata Memorial Hospital", city: "Mumbai", lat: 19.0176, lng: 72.8562, type: "major", camps: 28, donors: 890, category: "Private" },
  { name: "Apollo Blood Bank", city: "Bangalore", lat: 12.9716, lng: 77.5946, type: "major", camps: 24, donors: 720, category: "Private" },
  { name: "Stanley Medical College", city: "Chennai", lat: 13.0827, lng: 80.2707, type: "major", camps: 19, donors: 580, category: "Government" },
  { name: "Medical College Blood Bank", city: "Kolkata", lat: 22.5726, lng: 88.3639, type: "major", camps: 16, donors: 480, category: "Government" },
  { name: "Civil Hospital Blood Bank", city: "Ahmedabad", lat: 23.0225, lng: 72.5714, type: "major", camps: 22, donors: 670, category: "Government" },
  { name: "NIMS Blood Bank", city: "Hyderabad", lat: 17.3850, lng: 78.4867, type: "major", camps: 13, donors: 390, category: "Private" },
  
  // Regional Centers
  { name: "PGI Blood Bank", city: "Chandigarh", lat: 30.7333, lng: 76.7794, type: "regular", camps: 8, donors: 240, category: "Government" },
  { name: "KGMU Blood Bank", city: "Lucknow", lat: 26.9124, lng: 80.9420, type: "regular", camps: 12, donors: 380, category: "Government" },
  { name: "Regional Blood Center", city: "Kochi", lat: 9.9312, lng: 76.2673, type: "regular", camps: 11, donors: 320, category: "Charitable" },
  { name: "SMS Hospital Blood Bank", city: "Jaipur", lat: 26.9124, lng: 75.7873, type: "regular", camps: 16, donors: 480, category: "Government" },
  { name: "Capital Hospital", city: "Bhubaneswar", lat: 20.2961, lng: 85.8245, type: "regular", camps: 9, donors: 270, category: "Government" },
  { name: "GMC Blood Bank", city: "Pune", lat: 18.5204, lng: 73.8567, type: "regular", camps: 14, donors: 420, category: "Government" },
  { name: "AIIMS Rishikesh", city: "Rishikesh", lat: 30.0869, lng: 78.2676, type: "regular", camps: 7, donors: 180, category: "Government" },
  { name: "JIPMER Blood Bank", city: "Puducherry", lat: 11.9416, lng: 79.8083, type: "regular", camps: 6, donors: 150, category: "Government" }
];

// Recent blood request data
var recentRequests = [
  { id: 'REQ-145', bloodGroup: 'B+', lat: 12.9716, lng: 77.5946, city: 'Bangalore', status: 'active', time: '2 mins ago', urgency: 'high' },
  { id: 'REQ-144', bloodGroup: 'O-', lat: 19.0760, lng: 72.8777, city: 'Mumbai', status: 'fulfilled', time: '15 mins ago', urgency: 'critical' },
  { id: 'REQ-143', bloodGroup: 'AB+', lat: 28.7041, lng: 77.1025, city: 'Delhi', status: 'pending', time: '1 hour ago', urgency: 'medium' },
  { id: 'REQ-142', bloodGroup: 'A+', lat: 13.0827, lng: 80.2707, city: 'Chennai', status: 'active', time: '2 hours ago', urgency: 'high' },
  { id: 'REQ-141', bloodGroup: 'O+', lat: 22.5726, lng: 88.3639, city: 'Kolkata', status: 'fulfilled', time: '3 hours ago', urgency: 'medium' },
  { id: 'REQ-140', bloodGroup: 'B-', lat: 17.3850, lng: 78.4867, city: 'Hyderabad', status: 'pending', time: '4 hours ago', urgency: 'low' }
];

// Donor availability data by location
var donorAvailability = [
  { lat: 28.7041, lng: 77.1025, city: 'Delhi', available: 420, busy: 45, bloodGroups: { 'O+': 85, 'A+': 78, 'B+': 65, 'AB+': 32, 'O-': 25, 'A-': 22, 'B-': 8, 'AB-': 5 } },
  { lat: 19.0760, lng: 72.8777, city: 'Mumbai', available: 890, busy: 78, bloodGroups: { 'O+': 178, 'A+': 156, 'B+': 142, 'AB+': 89, 'O-': 67, 'A-': 54, 'B-': 32, 'AB-': 12 } },
  { lat: 12.9716, lng: 77.5946, city: 'Bangalore', available: 720, busy: 62, bloodGroups: { 'O+': 145, 'A+': 132, 'B+': 118, 'AB+': 67, 'O-': 45, 'A-': 38, 'B-': 22, 'AB-': 8 } },
  { lat: 13.0827, lng: 80.2707, city: 'Chennai', available: 580, busy: 48, bloodGroups: { 'O+': 112, 'A+': 98, 'B+': 87, 'AB+': 52, 'O-': 34, 'A-': 28, 'B-': 18, 'AB-': 6 } },
  { lat: 22.5726, lng: 88.3639, city: 'Kolkata', available: 480, busy: 42, bloodGroups: { 'O+': 95, 'A+': 82, 'B+': 74, 'AB+': 45, 'O-': 28, 'A-': 24, 'B-': 15, 'AB-': 4 } },
  { lat: 23.0225, lng: 72.5714, city: 'Ahmedabad', available: 670, busy: 56, bloodGroups: { 'O+': 134, 'A+': 118, 'B+': 98, 'AB+': 62, 'O-': 42, 'A-': 35, 'B-': 25, 'AB-': 8 } }
];

// State center coordinates for focusing
var stateCoordinates = {
  delhi: [28.6139, 77.2090],
  maharashtra: [19.7515, 75.7139], 
  karnataka: [15.3173, 75.7139],
  tamilnadu: [11.1271, 78.6569],
  westbengal: [22.9868, 87.8550],
  gujarat: [23.0225, 72.5714],
  rajasthan: [27.0238, 74.2179],
  punjab: [31.1471, 75.3412],
  up: [26.8467, 80.9462]
};

// Add missing functions for state selection
function focusOnState(state) {
  if (!state || !stateCoordinates[state]) {
    resetMapView();
    return;
  }
  
  var coords = stateCoordinates[state];
  indiaMap.setView(coords, 7);
  
  // Update regional stats for selected state
  // This would filter data and show relevant stats
  console.log('Focused on state:', state);
}

function toggleClusters() {
  var btn = event.target;
  if (showClusters) {
    indiaMap.removeLayer(markerClusters);
    bloodBankMarkers.forEach(function(marker) {
      indiaMap.addLayer(marker);
    });
    showClusters = false;
    btn.classList.remove('active');
    btn.textContent = 'Show Clusters';
  } else {
    bloodBankMarkers.forEach(function(marker) {
      indiaMap.removeLayer(marker);
    });
    indiaMap.addLayer(markerClusters);
    showClusters = true;
    btn.classList.add('active');
    btn.textContent = 'Hide Clusters';
  }
}

// Initialize dashboard when page loads
document.addEventListener('DOMContentLoaded', function() {
  // Check for existing session first
  if (!checkExistingSession()) {
    // No valid session - show login screen
    document.getElementById('authScreen').style.display = 'flex';
    document.getElementById('dashboardScreen').style.display = 'none';
  }
  // If session exists, dashboard is already shown by checkExistingSession()
});

// Initialize real map with Leaflet
function initializeMap() {
  // Initialize map centered on India
  indiaMap = L.map('indiaMap').setView([20.5937, 78.9629], 5);
  
  // Add OpenStreetMap tiles (slightly desaturated to highlight overlays)
  L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
    attribution: '© OpenStreetMap contributors'
  }).addTo(indiaMap);
  
  // Initialize marker clusters
  markerClusters = L.markerClusterGroup({
    maxClusterRadius: 50,
    disableClusteringAtZoom: 8
  });
  
  // Add blood bank markers
  addBloodBankMarkers();
  
  // Set bounds to India
  var indiaBounds = [[6.4627, 68.1097], [35.5128, 97.3953]];
  indiaMap.fitBounds(indiaBounds);
  indiaMap.setMaxBounds(indiaBounds);
  
  // Show heatmap by default to demonstrate colors
  setTimeout(function() {
    createHeatmapLayer();
    showHeatmap = true;
    var heatmapBtn = document.querySelector('.map-btn[onclick="toggleHeatmap()"]');
    if (heatmapBtn) {
      heatmapBtn.classList.add('active');
      heatmapBtn.textContent = 'Hide Heatmap';
    }
    showHeatmapLegend();
    document.getElementById('heatmapInfo').style.display = 'block';
  }, 500);
}

// Initialize donor availability map
function initializeDonorMap() {
  // Check if donorMap element exists
  if (!document.getElementById('donorMap')) {
    console.warn('donorMap element not found');
    return;
  }
  
  // Initialize donor map centered on India
  donorMap = L.map('donorMap').setView([20.5937, 78.9629], 5);
  
  // Add OpenStreetMap tiles
  L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
    attribution: '© OpenStreetMap contributors'
  }).addTo(donorMap);
  
  // Initialize donor clusters
  donorClusters = L.markerClusterGroup({
    maxClusterRadius: 40,
    disableClusteringAtZoom: 7
  });
  
  // Add donor availability markers
  addDonorMarkers();
  
  // Add recent request markers
  addRecentRequestMarkers();
  
  // Set bounds to India
  var indiaBounds = [[6.4627, 68.1097], [35.5128, 97.3953]];
  donorMap.fitBounds(indiaBounds);
  donorMap.setMaxBounds(indiaBounds);
  
  donorMap.addLayer(donorClusters);
}

function addDonorMarkers() {
  donorAvailability.forEach(function(location) {
    // Create donor availability marker
    var donorHtml = `<div class="donor-marker available"></div>`;
    var donorIcon = L.divIcon({
      html: donorHtml,
      className: 'custom-donor-marker',
      iconSize: [20, 20],
      iconAnchor: [10, 10]
    });
    
    var marker = L.marker([location.lat, location.lng], { icon: donorIcon });
    
    // Create detailed popup
    var popupContent = `
      <div class="donor-popup">
        <div class="popup-title">${location.city} - Donor Availability</div>
        <div class="donor-stats">
          <div class="availability-summary">
            <span class="available-count">${location.available}</span> Available |
            <span class="busy-count">${location.busy}</span> Busy
          </div>
          <div class="blood-group-breakdown">
            <div class="bg-row"><span class="bg-label">O+:</span> <span>${location.bloodGroups['O+']}</span></div>
            <div class="bg-row"><span class="bg-label">A+:</span> <span>${location.bloodGroups['A+']}</span></div>
            <div class="bg-row"><span class="bg-label">B+:</span> <span>${location.bloodGroups['B+']}</span></div>
            <div class="bg-row"><span class="bg-label">AB+:</span> <span>${location.bloodGroups['AB+']}</span></div>
          </div>
        </div>
      </div>
    `;
    
    marker.bindPopup(popupContent);
    donorMarkers.push(marker);
    donorClusters.addLayer(marker);
  });
}

function addRecentRequestMarkers() {
  recentRequests.forEach(function(request) {
    var urgencyClass = request.urgency;
    var statusClass = request.status;
    
    var requestHtml = `<div class="request-marker ${urgencyClass} ${statusClass}">${request.bloodGroup}</div>`;
    var requestIcon = L.divIcon({
      html: requestHtml,
      className: 'custom-request-marker',
      iconSize: [24, 24],
      iconAnchor: [12, 12]
    });
    
    var marker = L.marker([request.lat, request.lng], { icon: requestIcon });
    
    var popupContent = `
      <div class="request-popup">
        <div class="popup-title">${request.id} - ${request.city}</div>
        <div class="request-details">
          <div><strong>Blood Group:</strong> ${request.bloodGroup}</div>
          <div><strong>Status:</strong> <span class="status-${request.status}">${request.status.toUpperCase()}</span></div>
          <div><strong>Urgency:</strong> <span class="urgency-${request.urgency}">${request.urgency.toUpperCase()}</span></div>
          <div><strong>Time:</strong> ${request.time}</div>
        </div>
      </div>
    `;
    
    marker.bindPopup(popupContent);
    requestMarkers.push(marker);
    
    if (showRecentRequests) {
      donorMap.addLayer(marker);
    }
  });
}

// Donor map control functions
function resetDonorMapView() {
  var indiaBounds = [[6.4627, 68.1097], [35.5128, 97.3953]];
  donorMap.fitBounds(indiaBounds);
}

function toggleDonorHeatmap() {
  var btn = event.target;
  if (showDonorHeatmap) {
    if (donorHeatmapLayer) {
      donorMap.removeLayer(donorHeatmapLayer);
    }
    showDonorHeatmap = false;
    btn.classList.remove('active');
    btn.textContent = 'Show Donor Heatmap';
  } else {
    createDonorHeatmapLayer();
    showDonorHeatmap = true;
    btn.classList.add('active');
    btn.textContent = 'Hide Donor Heatmap';
  }
}

function toggleRecentRequests() {
  var btn = event.target;
  if (showRecentRequests) {
    requestMarkers.forEach(function(marker) {
      donorMap.removeLayer(marker);
    });
    showRecentRequests = false;
    btn.classList.remove('active');
    btn.textContent = 'Show Recent Requests';
  } else {
    requestMarkers.forEach(function(marker) {
      donorMap.addLayer(marker);
    });
    showRecentRequests = true;
    btn.classList.add('active');
    btn.textContent = 'Hide Recent Requests';
  }
}

function filterByBloodGroup(bloodGroup) {
  // Filter logic would go here
  console.log('Filtering by blood group:', bloodGroup);
}

function createDonorHeatmapLayer() {
  var donorPoints = donorAvailability.map(function(location) {
    return [location.lat, location.lng, location.available / 200];
  });
  
  donorPoints.forEach(function(point) {
    var intensity = point[2];
    var color = getDonorHeatmapColor(intensity);
    
    var circle = L.circle([point[0], point[1]], {
      color: color.border,
      fillColor: color.fill,
      fillOpacity: 0.4 + (intensity * 0.3),
      opacity: 1.0,
      weight: 2,
      radius: intensity * 35000 + 25000
    });
    
    if (!donorHeatmapLayer) {
      donorHeatmapLayer = L.layerGroup();
    }
    donorHeatmapLayer.addLayer(circle);
  });
  
  if (donorHeatmapLayer) {
    donorMap.addLayer(donorHeatmapLayer);
  }
}

function getDonorHeatmapColor(intensity) {
  var colors = {
    veryHigh: { fill: '#00ff00', border: '#00cc00' }, // Green for high availability
    high: { fill: '#66ff66', border: '#00ff00' },
    medium: { fill: '#ffff00', border: '#cccc00' }, // Yellow for medium
    low: { fill: '#ff9900', border: '#cc6600' }, // Orange for low
    veryLow: { fill: '#ff3300', border: '#cc0000' } // Red for very low
  };
  
  if (intensity >= 3.0) return colors.veryHigh;
  if (intensity >= 2.0) return colors.high;
  if (intensity >= 1.0) return colors.medium;
  if (intensity >= 0.5) return colors.low;
  return colors.veryLow;
}

// Add blood bank markers to the main map
function addBloodBankMarkers() {
  bloodBankData.forEach(function(bank) {
    // Create custom marker
    var markerHtml = `<div class="blood-bank-marker ${bank.type}"></div>`;
    var customIcon = L.divIcon({
      html: markerHtml,
      className: 'custom-marker',
      iconSize: bank.type === 'major' ? [16, 16] : [12, 12],
      iconAnchor: bank.type === 'major' ? [8, 8] : [6, 6]
    });
    
    var marker = L.marker([bank.lat, bank.lng], { icon: customIcon });
    
    // Create popup content
    var popupContent = `
      <div class="blood-donation-popup">
        <div class="popup-title">${bank.name}</div>
        <div class="popup-stats">
          <strong>${bank.city}</strong><br>
          ${bank.camps} Active Camps<br>
          ${bank.donors} Registered Donors<br>
          Category: ${bank.category}
        </div>
      </div>
    `;
    
    marker.bindPopup(popupContent);
    marker.on('click', function() {
      updateRegionalStatsForBank(bank);
    });
    
    bloodBankMarkers.push(marker);
    markerClusters.addLayer(marker);
  });
  
  indiaMap.addLayer(markerClusters);
}

// Map control functions
function resetMapView() {
  var indiaBounds = [[6.4627, 68.1097], [35.5128, 97.3953]];
  indiaMap.fitBounds(indiaBounds);
  clearSelection();
}

function toggleHeatmap() {
  var btn = event.target;
  if (showHeatmap) {
    if (heatmapLayer) {
      indiaMap.removeLayer(heatmapLayer);
    }
    showHeatmap = false;
    btn.classList.remove('active');
    btn.textContent = 'Show Heatmap';
    hideHeatmapLegend();
  } else {
    createHeatmapLayer();
    showHeatmap = true;
    btn.classList.add('active');
    btn.textContent = 'Hide Heatmap';
  }
}

function showHeatmapLegend() {
  // Remove existing legend
  hideHeatmapLegend();
  
  var legendControl = L.control({ position: 'bottomright' });
  
  legendControl.onAdd = function(map) {
    var div = L.DomUtil.create('div', 'heatmap-legend');
    div.innerHTML = `
      <div class="legend-title">Blood Donation Activity</div>
      <div class="legend-item"><span class="legend-color very-high"></span> Very High (90%+)</div>
      <div class="legend-item"><span class="legend-color high"></span> High (80-90%)</div>
      <div class="legend-item"><span class="legend-color medium-high"></span> Medium-High (60-80%)</div>
      <div class="legend-item"><span class="legend-color medium"></span> Medium (40-60%)</div>
      <div class="legend-item"><span class="legend-color medium-low"></span> Medium-Low (30-40%)</div>
      <div class="legend-item"><span class="legend-color low"></span> Low (20-30%)</div>
      <div class="legend-item"><span class="legend-color very-low"></span> Very Low (0-20%)</div>
    `;
    return div;
  };
  
  legendControl.addTo(indiaMap);
  window.currentLegend = legendControl;
}

function hideHeatmapLegend() {
  if (window.currentLegend) {
    indiaMap.removeControl(window.currentLegend);
    window.currentLegend = null;
  }
}

function toggleClusters() {
  var btn = event.target;
  if (showClusters) {
    indiaMap.removeLayer(markerClusters);
    bloodBankMarkers.forEach(function(marker) {
      indiaMap.addLayer(marker);
    });
    showClusters = false;
    btn.classList.remove('active');
    btn.textContent = 'Enable Clusters';
  } else {
    bloodBankMarkers.forEach(function(marker) {
      indiaMap.removeLayer(marker);
    });
    indiaMap.addLayer(markerClusters);
    showClusters = true;
    btn.classList.add('active');
    btn.textContent = 'Disable Clusters';
  }
}

function createHeatmapLayer() {
  var heatmapPoints = bloodBankData.map(function(bank) {
    return [bank.lat, bank.lng, bank.donors / 100]; // Intensity based on donor count
  });
  
  // Create heat map points with random additional points for demo
  var additionalPoints = [
    [28.7041, 77.1025, 1.2], // Delhi area - very high activity
    [19.0760, 72.8777, 1.4], // Mumbai area - very high activity
    [12.9716, 77.5946, 1.1], // Bangalore area - very high activity
    [13.0827, 80.2707, 0.9], // Chennai area - high activity
    [22.5726, 88.3639, 0.7], // Kolkata area - medium-high activity
    [23.0225, 72.5714, 0.8], // Ahmedabad - high activity
    [17.3850, 78.4867, 0.6], // Hyderabad - medium-high activity
    [26.9124, 75.7873, 0.4], // Jaipur - medium activity
    [30.7333, 76.7794, 0.3], // Chandigarh - medium-low activity
    [26.8467, 80.9462, 0.5], // Lucknow - medium activity
    [21.1458, 79.0882, 0.2], // Nagpur - low activity
    [15.2993, 74.1240, 0.15], // Goa - very low activity
  ];
  
  var allPoints = heatmapPoints.concat(additionalPoints);
  
  // Create colorful circles based on intensity
  allPoints.forEach(function(point) {
    var intensity = point[2];
    var color = getHeatmapColor(intensity);
    
    var circle = L.circle([point[0], point[1]], {
      color: color.border,
      fillColor: color.fill,
      fillOpacity: 0.5 + (intensity * 0.3), // Dynamic opacity
      opacity: 1.0,
      weight: 3,
      radius: intensity * 45000 + 20000 // Dynamic radius
    });
    
    // Add popup with intensity info
    circle.bindPopup(
      `<div class="heatmap-popup">
        <strong>Blood Donation Activity</strong><br>
        Intensity: ${(intensity * 100).toFixed(0)}%<br>
        Activity Level: ${getActivityLevel(intensity)}<br>
        <span style="color: ${color.fill};">● ${color.fill}</span>
      </div>`
    );
    
    if (!heatmapLayer) {
      heatmapLayer = L.layerGroup();
    }
    heatmapLayer.addLayer(circle);
  });
  
  if (heatmapLayer) {
    indiaMap.addLayer(heatmapLayer);
  }
}

// Get color based on heatmap intensity (red = high, blue = low)
function getHeatmapColor(intensity) {
  var colors = {
    // Very High (1.0+) - Bright Red
    veryHigh: { fill: '#ff0000', border: '#cc0000' },
    // High (0.8-1.0) - Orange-Red 
    high: { fill: '#ff4500', border: '#ff0000' },
    // Medium-High (0.6-0.8) - Orange
    mediumHigh: { fill: '#ff8c00', border: '#ff4500' },
    // Medium (0.4-0.6) - Yellow-Orange
    medium: { fill: '#ffd700', border: '#ff8c00' },
    // Medium-Low (0.3-0.4) - Yellow-Green
    mediumLow: { fill: '#9acd32', border: '#ffd700' },
    // Low (0.2-0.3) - Green
    low: { fill: '#32cd32', border: '#9acd32' },
    // Very Low (0.0-0.2) - Blue
    veryLow: { fill: '#1e90ff', border: '#32cd32' }
  };
  
  if (intensity >= 1.0) return colors.veryHigh;
  if (intensity >= 0.8) return colors.high;
  if (intensity >= 0.6) return colors.mediumHigh;
  if (intensity >= 0.4) return colors.medium;
  if (intensity >= 0.3) return colors.mediumLow;
  if (intensity >= 0.2) return colors.low;
  return colors.veryLow;
}

function getActivityLevel(intensity) {
  if (intensity >= 1.0) return 'Very High';
  if (intensity >= 0.8) return 'High';
  if (intensity >= 0.6) return 'Medium-High';
  if (intensity >= 0.4) return 'Medium';
  if (intensity >= 0.3) return 'Medium-Low';
  if (intensity >= 0.2) return 'Low';
  return 'Very Low';
}

function focusOnState(stateId) {
  if (!stateId || !stateCoordinates[stateId]) {
    resetMapView();
    return;
  }
  
  var coords = stateCoordinates[stateId];
  indiaMap.setView(coords, 7);
  
  // Update regional stats
  updateRegionalStatsForState(stateId);
  
  // Highlight relevant markers
  highlightStateMarkers(stateId);
}

function highlightStateMarkers(stateId) {
  // Clear previous highlights
  clearSelection();
  
  // Get state name for filtering
  var stateNames = {
    delhi: 'Delhi',
    maharashtra: 'Mumbai',
    karnataka: 'Bangalore', 
    tamilnadu: 'Chennai',
    westbengal: 'Kolkata',
    gujarat: 'Ahmedabad',
    rajasthan: 'Jaipur',
    punjab: 'Chandigarh',
    up: 'Lucknow'
  };
  
  var targetCity = stateNames[stateId];
  if (targetCity) {
    bloodBankMarkers.forEach(function(marker) {
      var bank = bloodBankData.find(b => 
        Math.abs(b.lat - marker.getLatLng().lat) < 0.1 && 
        Math.abs(b.lng - marker.getLatLng().lng) < 0.1
      );
      
      if (bank && bank.city === targetCity) {
        marker.openPopup();
      }
    });
  }
}

function clearSelection() {
  indiaMap.closePopup();
  selectRegion('all');
}

function updateRegionalStatsForBank(bank) {
  var regionDetails = document.getElementById('regionDetails');
  var regionCard = `
    <div class="region-stat-card">
      <div class="region-name">${bank.name} - ${bank.city}</div>
      <div class="region-metrics">
        <div class="region-metric">
          <span class="metric-label">Blood Camps</span>
          <span class="metric-value">${bank.camps}</span>
        </div>
        <div class="region-metric">
          <span class="metric-label">Active Donors</span>
          <span class="metric-value">${bank.donors.toLocaleString()}</span>
        </div>
        <div class="region-metric">
          <span class="metric-label">Category</span>
          <span class="metric-value">${bank.category}</span>
        </div>
        <div class="region-metric">
          <span class="metric-label">Type</span>
          <span class="metric-value">${bank.type === 'major' ? 'Major Center' : 'Regional Center'}</span>
        </div>
      </div>
    </div>
  `;
  
  regionDetails.innerHTML = regionCard;
}

// Update regional stats for specific state
function updateRegionalStatsForState(stateId) {
  var stateData = getStateData(stateId);
  if (!stateData) return;
  
  var regionDetails = document.getElementById('regionDetails');
  var regionCard = `
    <div class="region-stat-card">
      <div class="region-name">${stateData.name}</div>
      <div class="region-metrics">
        <div class="region-metric">
          <span class="metric-label">Blood Camps</span>
          <span class="metric-value">${stateData.camps}</span>
        </div>
        <div class="region-metric">
          <span class="metric-label">Active Donors</span>
          <span class="metric-value">${stateData.donors.toLocaleString()}</span>
        </div>
        <div class="region-metric">
          <span class="metric-label">Units Available</span>
          <span class="metric-value">${stateData.units}</span>
        </div>
        <div class="region-metric">
          <span class="metric-label">Last Collection</span>
          <span class="metric-value">${stateData.lastCollection}</span>
        </div>
      </div>
    </div>
  `;
  
  regionDetails.innerHTML = regionCard;
}

// Get state-specific data
function getStateData(stateId) {
  var stateDataMap = {
    delhi: { name: 'Delhi', camps: 15, donors: 420, units: 280, lastCollection: '2 hours ago' },
    punjab: { name: 'Punjab', camps: 12, donors: 380, units: 240, lastCollection: '4 hours ago' },
    maharashtra: { name: 'Maharashtra', camps: 28, donors: 890, units: 650, lastCollection: '1 hour ago' },
    karnataka: { name: 'Karnataka', camps: 24, donors: 720, units: 580, lastCollection: '3 hours ago' },
    tamilnadu: { name: 'Tamil Nadu', camps: 19, donors: 580, units: 420, lastCollection: '2 hours ago' },
    westbengal: { name: 'West Bengal', camps: 16, donors: 480, units: 350, lastCollection: '5 hours ago' },
    gujarat: { name: 'Gujarat', camps: 22, donors: 670, units: 490, lastCollection: '1 hour ago' },
    rajasthan: { name: 'Rajasthan', camps: 16, donors: 480, units: 320, lastCollection: '6 hours ago' },
    up: { name: 'Uttar Pradesh', camps: 18, donors: 650, units: 480, lastCollection: '3 hours ago' }
  };
  
  return stateDataMap[stateId] || null;
}

// Region selection functionality
function selectRegion(regionKey) {
  // Remove previous selections
  document.querySelectorAll('.region').forEach(function(region) {
    region.classList.remove('selected');
  });
  
  // Update region details
  var regionDetails = document.getElementById('regionDetails');
  var regionCard = '';
  
  if (regionKey === 'all') {
    regionCard = `
      <div class="region-stat-card">
        <div class="region-name">All Regions Overview</div>
        <div class="region-metrics">
          <div class="region-metric">
            <span class="metric-label">Total Camps</span>
            <span class="metric-value">156</span>
          </div>
          <div class="region-metric">
            <span class="metric-label">Active Donors</span>
            <span class="metric-value">2,847</span>
          </div>
          <div class="region-metric">
            <span class="metric-label">Units Collected</span>
            <span class="metric-value">4,230</span>
          </div>
          <div class="region-metric">
            <span class="metric-label">Avg Response Time</span>
            <span class="metric-value">16 mins</span>
          </div>
        </div>
      </div>
    `;
  } else {
    // Select the clicked region
    var regionElement = document.querySelector('[data-region="' + regionKey + '"]');
    if (regionElement) {
      regionElement.classList.add('selected');
    }
    
    var data = regionalData[regionKey];
    if (data) {
      regionCard = `
        <div class="region-stat-card">
          <div class="region-name">${data.name}</div>
          <div class="region-metrics">
            <div class="region-metric">
              <span class="metric-label">Total Camps</span>
              <span class="metric-value">${data.camps}</span>
            </div>
            <div class="region-metric">
              <span class="metric-label">Active Donors</span>
              <span class="metric-value">${data.donors.toLocaleString()}</span>
            </div>
            <div class="region-metric">
              <span class="metric-label">Units Collected</span>
              <span class="metric-value">${data.units.toLocaleString()}</span>
            </div>
            <div class="region-metric">
              <span class="metric-label">Coverage States</span>
              <span class="metric-value">${data.states.length}</span>
            </div>
          </div>
          <div style="margin-top: 12px; font-size: 12px; color: #888;">
            States: ${data.states.join(', ')}
          </div>
        </div>
      `;
    }
  }
  
  regionDetails.innerHTML = regionCard;
}

// Generate heatmap for request activity
function generateHeatmap() {
  var heatmapCells = document.getElementById('heatmapCells');
  var html = '';
  
  // Generate 7 rows (days) x 6 columns (time slots)
  for (var day = 0; day < 7; day++) {
    html += '<div class="heatmap-row">';
    for (var hour = 0; hour < 6; hour++) {
      var level = getHeatmapLevel(day, hour);
      var requests = getRequestCount(day, hour);
      var tooltip = `${getDayName(day)} ${getHourName(hour)}: ${requests} requests`;
      html += `<div class="heatmap-cell level-${level}" title="${tooltip}" data-day="${day}" data-hour="${hour}"></div>`;
    }
    html += '</div>';
  }
  
  heatmapCells.innerHTML = html;
  
  // Add click handlers to heatmap cells
  document.querySelectorAll('.heatmap-cell').forEach(function(cell) {
    cell.addEventListener('click', function() {
      var day = parseInt(this.dataset.day);
      var hour = parseInt(this.dataset.hour);
      showHeatmapDetails(day, hour);
    });
  });
}

function getDayName(day) {
  var days = ['Monday', 'Tuesday', 'Wednesday', 'Thursday', 'Friday', 'Saturday', 'Sunday'];
  return days[day];
}

function getHourName(hour) {
  var hours = ['6AM-9AM', '9AM-12PM', '12PM-3PM', '3PM-6PM', '6PM-9PM', '9PM+'];
  return hours[hour];
}

// Calculate heatmap intensity level based on day and hour
function getHeatmapLevel(day, hour) {
  // Simulate realistic patterns:
  // - Higher activity during work hours (9AM-6PM = hours 1-4)
  // - Lower activity on weekends (days 5-6)
  // - Peak activity on weekday afternoons
  
  var isWeekend = day >= 5;
  var isWorkHours = hour >= 1 && hour <= 4;
  var isPeakHours = hour >= 2 && hour <= 3;
  
  var baseLevel = 1;
  
  if (isWeekend) {
    baseLevel = Math.max(0, baseLevel - 1);
  }
  
  if (isWorkHours) {
    baseLevel += 1;
  }
  
  if (isPeakHours && !isWeekend) {
    baseLevel += 1;
  }
  
  // Add some randomness
  var randomFactor = Math.random();
  if (randomFactor > 0.7) baseLevel += 1;
  if (randomFactor < 0.3) baseLevel = Math.max(0, baseLevel - 1);
  
  return Math.min(4, Math.max(0, baseLevel));
}

// Get request count for tooltip
function getRequestCount(day, hour) {
  var level = getHeatmapLevel(day, hour);
  var baseCounts = [2, 8, 15, 23, 35];
  var variance = Math.floor(Math.random() * 6) - 3;
  return Math.max(0, baseCounts[level] + variance);
}

// Show details when heatmap cell is clicked
function showHeatmapDetails(day, hour) {
  var dayNames = ['Monday', 'Tuesday', 'Wednesday', 'Thursday', 'Friday', 'Saturday', 'Sunday'];
  var hourNames = ['6AM-9AM', '9AM-12PM', '12PM-3PM', '3PM-6PM', '6PM-9PM', '9PM+'];
  var requests = getRequestCount(day, hour);
  var level = getHeatmapLevel(day, hour);
  
  var activityLevel = ['Very Low', 'Low', 'Medium', 'High', 'Very High'][level];
  var color = ['#f0f8ff', '#87ceeb', '#4682b4', '#ff8c00', '#ff4500'][level];
  
  alert(`${dayNames[day]} ${hourNames[hour]}\n${requests} requests\nActivity Level: ${activityLevel}`);
}

// Simulate real-time updates
function updateMetrics() {
  // Update active requests count
  var activeRequestsElement = document.querySelector('.metric-card:nth-child(2) .metric-value');
  if (activeRequestsElement) {
    var currentCount = parseInt(activeRequestsElement.textContent);
    var change = Math.floor(Math.random() * 5) - 2; // -2 to +2
    var newCount = Math.max(0, currentCount + change);
    activeRequestsElement.textContent = newCount;
  }
  
  // Update fulfillment rate
  var fulfillmentElement = document.querySelector('.metric-card:nth-child(4) .metric-value');
  if (fulfillmentElement) {
    var rates = ['92%', '93%', '94%', '95%', '96%'];
    fulfillmentElement.textContent = rates[Math.floor(Math.random() * rates.length)];
  }
}

// Simulate new request arrival
function addNewRequest() {
  var tbody = document.getElementById('recentRequestsBody');
  if (!tbody) return;
  
  var newRequestId = 'REQ-' + (146 + Math.floor(Math.random() * 50));
  var bloodGroups = ['A+', 'A-', 'B+', 'B-', 'AB+', 'AB-', 'O+', 'O-'];
  var locations = [
    'Mumbai - 400001',
    'Delhi - 110001', 
    'Bangalore - 560001',
    'Chennai - 600001',
    'Kolkata - 700001',
    'Pune - 411001'
  ];
  
  var bloodGroup = bloodGroups[Math.floor(Math.random() * bloodGroups.length)];
  var location = locations[Math.floor(Math.random() * locations.length)];
  
  var newRow = `
    <tr style="background-color: #f0f0f0;">
      <td><strong>${newRequestId}</strong></td>
      <td>${bloodGroup}</td>
      <td>${location}</td>
      <td><span class="status-badge active">Active</span></td>
      <td>0 responses</td>
      <td>Just now</td>
    </tr>
  `;
  
  tbody.insertAdjacentHTML('afterbegin', newRow);
  
  // Remove last row to keep table size manageable
  var rows = tbody.querySelectorAll('tr');
  if (rows.length > 6) {
    rows[rows.length - 1].remove();
  }
  
  // Highlight new row briefly
  setTimeout(function() {
    var firstRow = tbody.querySelector('tr');
    if (firstRow) {
      firstRow.style.backgroundColor = '';
    }
  }, 2000);
}

// Auto-refresh functions
setInterval(updateMetrics, 30000); // Update metrics every 30 seconds
setInterval(addNewRequest, 45000); // Add new request every 45 seconds

// Additional utility functions for demo purposes
function exportData() {
  alert('Export functionality would download CSV/Excel files with current analytics data.');
}

function refreshDashboard() {
  generateHeatmap();
  updateMetrics();
  alert('Dashboard data refreshed successfully!');
}

// Add the missing donor map functionality

// Keyboard shortcuts
document.addEventListener('keydown', function(e) {
  if (e.ctrlKey || e.metaKey) {
    switch(e.key) {
      case 'r':
        e.preventDefault();
        refreshDashboard();
        break;
      case 'e':
        e.preventDefault();
        exportData();
        break;
    }
  }
});
// Map variables
var indiaMap;
var donorMap;
var bloodBankMarkers = [];
var donorMarkers = [];
var requestMarkers = [];
var heatmapLayer;
var donorHeatmapLayer;
var markerClusters;
var donorClusters;
var showHeatmap = false;
var showDonorHeatmap = false;
var showClusters = false;
var showRecentRequests = true;

// Blood bank locations with real coordinates
var bloodBankData = [
  // Major Blood Banks
  { name: "AIIMS Blood Bank", city: "Delhi", lat: 28.5672, lng: 77.2100, type: "major", camps: 15, donors: 420, category: "Government" },
  { name: "Tata Memorial Hospital", city: "Mumbai", lat: 19.0176, lng: 72.8562, type: "major", camps: 28, donors: 890, category: "Private" },
  { name: "Apollo Blood Bank", city: "Bangalore", lat: 12.9716, lng: 77.5946, type: "major", camps: 24, donors: 720, category: "Private" },
  { name: "Stanley Medical College", city: "Chennai", lat: 13.0827, lng: 80.2707, type: "major", camps: 19, donors: 580, category: "Government" },
  { name: "Medical College Blood Bank", city: "Kolkata", lat: 22.5726, lng: 88.3639, type: "major", camps: 16, donors: 480, category: "Government" },
  { name: "Civil Hospital Blood Bank", city: "Ahmedabad", lat: 23.0225, lng: 72.5714, type: "major", camps: 22, donors: 670, category: "Government" },
  { name: "NIMS Blood Bank", city: "Hyderabad", lat: 17.3850, lng: 78.4867, type: "major", camps: 13, donors: 390, category: "Private" },
  
  // Regional Centers
  { name: "PGI Blood Bank", city: "Chandigarh", lat: 30.7333, lng: 76.7794, type: "regular", camps: 8, donors: 240, category: "Government" },
  { name: "KGMU Blood Bank", city: "Lucknow", lat: 26.9124, lng: 80.9420, type: "regular", camps: 12, donors: 380, category: "Government" },
  { name: "Regional Blood Center", city: "Kochi", lat: 9.9312, lng: 76.2673, type: "regular", camps: 11, donors: 320, category: "Charitable" },
  { name: "SMS Hospital Blood Bank", city: "Jaipur", lat: 26.9124, lng: 75.7873, type: "regular", camps: 16, donors: 480, category: "Government" },
  { name: "Capital Hospital", city: "Bhubaneswar", lat: 20.2961, lng: 85.8245, type: "regular", camps: 9, donors: 270, category: "Government" },
  { name: "GMC Blood Bank", city: "Pune", lat: 18.5204, lng: 73.8567, type: "regular", camps: 14, donors: 420, category: "Government" },
  { name: "AIIMS Rishikesh", city: "Rishikesh", lat: 30.0869, lng: 78.2676, type: "regular", camps: 7, donors: 180, category: "Government" },
  { name: "JIPMER Blood Bank", city: "Puducherry", lat: 11.9416, lng: 79.8083, type: "regular", camps: 6, donors: 150, category: "Government" }
];

// Recent blood request data
var recentRequests = [
  { id: 'REQ-145', bloodGroup: 'B+', lat: 12.9716, lng: 77.5946, city: 'Bangalore', status: 'active', time: '2 mins ago', urgency: 'high' },
  { id: 'REQ-144', bloodGroup: 'O-', lat: 19.0760, lng: 72.8777, city: 'Mumbai', status: 'fulfilled', time: '15 mins ago', urgency: 'critical' },
  { id: 'REQ-143', bloodGroup: 'AB+', lat: 28.7041, lng: 77.1025, city: 'Delhi', status: 'pending', time: '1 hour ago', urgency: 'medium' },
  { id: 'REQ-142', bloodGroup: 'A+', lat: 13.0827, lng: 80.2707, city: 'Chennai', status: 'active', time: '2 hours ago', urgency: 'high' },
  { id: 'REQ-141', bloodGroup: 'O+', lat: 22.5726, lng: 88.3639, city: 'Kolkata', status: 'fulfilled', time: '3 hours ago', urgency: 'medium' },
  { id: 'REQ-140', bloodGroup: 'B-', lat: 17.3850, lng: 78.4867, city: 'Hyderabad', status: 'pending', time: '4 hours ago', urgency: 'low' }
];

// Donor availability data by location
var donorAvailability = [
  { lat: 28.7041, lng: 77.1025, city: 'Delhi', available: 420, busy: 45, bloodGroups: { 'O+': 85, 'A+': 78, 'B+': 65, 'AB+': 32, 'O-': 25, 'A-': 22, 'B-': 8, 'AB-': 5 } },
  { lat: 19.0760, lng: 72.8777, city: 'Mumbai', available: 890, busy: 78, bloodGroups: { 'O+': 178, 'A+': 156, 'B+': 142, 'AB+': 89, 'O-': 67, 'A-': 54, 'B-': 32, 'AB-': 12 } },
  { lat: 12.9716, lng: 77.5946, city: 'Bangalore', available: 720, busy: 62, bloodGroups: { 'O+': 145, 'A+': 132, 'B+': 118, 'AB+': 67, 'O-': 45, 'A-': 38, 'B-': 22, 'AB-': 8 } },
  { lat: 13.0827, lng: 80.2707, city: 'Chennai', available: 580, busy: 48, bloodGroups: { 'O+': 112, 'A+': 98, 'B+': 87, 'AB+': 52, 'O-': 34, 'A-': 28, 'B-': 18, 'AB-': 6 } },
  { lat: 22.5726, lng: 88.3639, city: 'Kolkata', available: 480, busy: 42, bloodGroups: { 'O+': 95, 'A+': 82, 'B+': 74, 'AB+': 45, 'O-': 28, 'A-': 24, 'B-': 15, 'AB-': 4 } },
  { lat: 23.0225, lng: 72.5714, city: 'Ahmedabad', available: 670, busy: 56, bloodGroups: { 'O+': 134, 'A+': 118, 'B+': 98, 'AB+': 62, 'O-': 42, 'A-': 35, 'B-': 25, 'AB-': 8 } }
];

// State center coordinates for focusing
var stateCoordinates = {
  delhi: [28.6139, 77.2090],
  maharashtra: [19.7515, 75.7139], 
  karnataka: [15.3173, 75.7139],
  tamilnadu: [11.1271, 78.6569],
  westbengal: [22.9868, 87.8550],
  gujarat: [23.0225, 72.5714],
  rajasthan: [27.0238, 74.2179],
  punjab: [31.1471, 75.3412],
  up: [26.8467, 80.9462]
};

// Add missing functions for state selection
function focusOnState(state) {
  if (!state || !stateCoordinates[state]) {
    resetMapView();
    return;
  }
  
  var coords = stateCoordinates[state];
  indiaMap.setView(coords, 7);
  
  // Update regional stats for selected state
  // This would filter data and show relevant stats
  console.log('Focused on state:', state);
}

function toggleClusters() {
  var btn = event.target;
  if (showClusters) {
    indiaMap.removeLayer(markerClusters);
    bloodBankMarkers.forEach(function(marker) {
      indiaMap.addLayer(marker);
    });
    showClusters = false;
    btn.classList.remove('active');
    btn.textContent = 'Show Clusters';
  } else {
    bloodBankMarkers.forEach(function(marker) {
      indiaMap.removeLayer(marker);
    });
    indiaMap.addLayer(markerClusters);
    showClusters = true;
    btn.classList.add('active');
    btn.textContent = 'Hide Clusters';
  }
}

// Initialize dashboard when page loads
document.addEventListener('DOMContentLoaded', function() {
  generateHeatmap();
  selectRegion('all');
  initializeMap();
  initializeDonorMap();
});

// Initialize real map with Leaflet
function initializeMap() {
  // Initialize map centered on India
  indiaMap = L.map('indiaMap').setView([20.5937, 78.9629], 5);
  
  // Add OpenStreetMap tiles (slightly desaturated to highlight overlays)
  L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
    attribution: '© OpenStreetMap contributors'
  }).addTo(indiaMap);
  
  // Initialize marker clusters
  markerClusters = L.markerClusterGroup({
    maxClusterRadius: 50,
    disableClusteringAtZoom: 8
  });
  
  // Add blood bank markers
  addBloodBankMarkers();
  
  // Set bounds to India
  var indiaBounds = [[6.4627, 68.1097], [35.5128, 97.3953]];
  indiaMap.fitBounds(indiaBounds);
  indiaMap.setMaxBounds(indiaBounds);
  
  // Show heatmap by default to demonstrate colors
  setTimeout(function() {
    createHeatmapLayer();
    showHeatmap = true;
    var heatmapBtn = document.querySelector('.map-btn[onclick="toggleHeatmap()"]');
    if (heatmapBtn) {
      heatmapBtn.classList.add('active');
      heatmapBtn.textContent = 'Hide Heatmap';
    }
    showHeatmapLegend();
    document.getElementById('heatmapInfo').style.display = 'block';
  }, 500);
}

// Initialize donor availability map
function initializeDonorMap() {
  // Check if donorMap element exists
  if (!document.getElementById('donorMap')) {
    console.warn('donorMap element not found');
    return;
  }
  
  // Initialize donor map centered on India
  donorMap = L.map('donorMap').setView([20.5937, 78.9629], 5);
  
  // Add OpenStreetMap tiles
  L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
    attribution: '© OpenStreetMap contributors'
  }).addTo(donorMap);
  
  // Initialize donor clusters
  donorClusters = L.markerClusterGroup({
    maxClusterRadius: 40,
    disableClusteringAtZoom: 7
  });
  
  // Add donor availability markers
  addDonorMarkers();
  
  // Add recent request markers
  addRecentRequestMarkers();
  
  // Set bounds to India
  var indiaBounds = [[6.4627, 68.1097], [35.5128, 97.3953]];
  donorMap.fitBounds(indiaBounds);
  donorMap.setMaxBounds(indiaBounds);
  
  donorMap.addLayer(donorClusters);
}

function addDonorMarkers() {
  donorAvailability.forEach(function(location) {
    // Create donor availability marker
    var donorHtml = `<div class="donor-marker available"></div>`;
    var donorIcon = L.divIcon({
      html: donorHtml,
      className: 'custom-donor-marker',
      iconSize: [20, 20],
      iconAnchor: [10, 10]
    });
    
    var marker = L.marker([location.lat, location.lng], { icon: donorIcon });
    
    // Create detailed popup
    var popupContent = `
      <div class="donor-popup">
        <div class="popup-title">${location.city} - Donor Availability</div>
        <div class="donor-stats">
          <div class="availability-summary">
            <span class="available-count">${location.available}</span> Available |
            <span class="busy-count">${location.busy}</span> Busy
          </div>
          <div class="blood-group-breakdown">
            <div class="bg-row"><span class="bg-label">O+:</span> <span>${location.bloodGroups['O+']}</span></div>
            <div class="bg-row"><span class="bg-label">A+:</span> <span>${location.bloodGroups['A+']}</span></div>
            <div class="bg-row"><span class="bg-label">B+:</span> <span>${location.bloodGroups['B+']}</span></div>
            <div class="bg-row"><span class="bg-label">AB+:</span> <span>${location.bloodGroups['AB+']}</span></div>
          </div>
        </div>
      </div>
    `;
    
    marker.bindPopup(popupContent);
    donorMarkers.push(marker);
    donorClusters.addLayer(marker);
  });
}

function addRecentRequestMarkers() {
  recentRequests.forEach(function(request) {
    var urgencyClass = request.urgency;
    var statusClass = request.status;
    
    var requestHtml = `<div class="request-marker ${urgencyClass} ${statusClass}">${request.bloodGroup}</div>`;
    var requestIcon = L.divIcon({
      html: requestHtml,
      className: 'custom-request-marker',
      iconSize: [24, 24],
      iconAnchor: [12, 12]
    });
    
    var marker = L.marker([request.lat, request.lng], { icon: requestIcon });
    
    var popupContent = `
      <div class="request-popup">
        <div class="popup-title">${request.id} - ${request.city}</div>
        <div class="request-details">
          <div><strong>Blood Group:</strong> ${request.bloodGroup}</div>
          <div><strong>Status:</strong> <span class="status-${request.status}">${request.status.toUpperCase()}</span></div>
          <div><strong>Urgency:</strong> <span class="urgency-${request.urgency}">${request.urgency.toUpperCase()}</span></div>
          <div><strong>Time:</strong> ${request.time}</div>
        </div>
      </div>
    `;
    
    marker.bindPopup(popupContent);
    requestMarkers.push(marker);
    
    if (showRecentRequests) {
      donorMap.addLayer(marker);
    }
  });
}

// Donor map control functions
function resetDonorMapView() {
  var indiaBounds = [[6.4627, 68.1097], [35.5128, 97.3953]];
  donorMap.fitBounds(indiaBounds);
}

function toggleDonorHeatmap() {
  var btn = event.target;
  if (showDonorHeatmap) {
    if (donorHeatmapLayer) {
      donorMap.removeLayer(donorHeatmapLayer);
    }
    showDonorHeatmap = false;
    btn.classList.remove('active');
    btn.textContent = 'Show Donor Heatmap';
  } else {
    createDonorHeatmapLayer();
    showDonorHeatmap = true;
    btn.classList.add('active');
    btn.textContent = 'Hide Donor Heatmap';
  }
}

function toggleRecentRequests() {
  var btn = event.target;
  if (showRecentRequests) {
    requestMarkers.forEach(function(marker) {
      donorMap.removeLayer(marker);
    });
    showRecentRequests = false;
    btn.classList.remove('active');
    btn.textContent = 'Show Recent Requests';
  } else {
    requestMarkers.forEach(function(marker) {
      donorMap.addLayer(marker);
    });
    showRecentRequests = true;
    btn.classList.add('active');
    btn.textContent = 'Hide Recent Requests';
  }
}

function filterByBloodGroup(bloodGroup) {
  // Filter logic would go here
  console.log('Filtering by blood group:', bloodGroup);
}

function createDonorHeatmapLayer() {
  var donorPoints = donorAvailability.map(function(location) {
    return [location.lat, location.lng, location.available / 200];
  });
  
  donorPoints.forEach(function(point) {
    var intensity = point[2];
    var color = getDonorHeatmapColor(intensity);
    
    var circle = L.circle([point[0], point[1]], {
      color: color.border,
      fillColor: color.fill,
      fillOpacity: 0.4 + (intensity * 0.3),
      opacity: 1.0,
      weight: 2,
      radius: intensity * 35000 + 25000
    });
    
    if (!donorHeatmapLayer) {
      donorHeatmapLayer = L.layerGroup();
    }
    donorHeatmapLayer.addLayer(circle);
  });
  
  if (donorHeatmapLayer) {
    donorMap.addLayer(donorHeatmapLayer);
  }
}

function getDonorHeatmapColor(intensity) {
  var colors = {
    veryHigh: { fill: '#00ff00', border: '#00cc00' }, // Green for high availability
    high: { fill: '#66ff66', border: '#00ff00' },
    medium: { fill: '#ffff00', border: '#cccc00' }, // Yellow for medium
    low: { fill: '#ff9900', border: '#cc6600' }, // Orange for low
    veryLow: { fill: '#ff3300', border: '#cc0000' } // Red for very low
  };
  
  if (intensity >= 3.0) return colors.veryHigh;
  if (intensity >= 2.0) return colors.high;
  if (intensity >= 1.0) return colors.medium;
  if (intensity >= 0.5) return colors.low;
  return colors.veryLow;
}

// Add blood bank markers to the main map
function addBloodBankMarkers() {
  bloodBankData.forEach(function(bank) {
    // Create custom marker
    var markerHtml = `<div class="blood-bank-marker ${bank.type}"></div>`;
    var customIcon = L.divIcon({
      html: markerHtml,
      className: 'custom-marker',
      iconSize: bank.type === 'major' ? [16, 16] : [12, 12],
      iconAnchor: bank.type === 'major' ? [8, 8] : [6, 6]
    });
    
    var marker = L.marker([bank.lat, bank.lng], { icon: customIcon });
    
    // Create popup content
    var popupContent = `
      <div class="blood-donation-popup">
        <div class="popup-title">${bank.name}</div>
        <div class="popup-stats">
          <strong>${bank.city}</strong><br>
          ${bank.camps} Active Camps<br>
          ${bank.donors} Registered Donors<br>
          Category: ${bank.category}
        </div>
      </div>
    `;
    
    marker.bindPopup(popupContent);
    marker.on('click', function() {
      updateRegionalStatsForBank(bank);
    });
    
    bloodBankMarkers.push(marker);
    markerClusters.addLayer(marker);
  });
  
  indiaMap.addLayer(markerClusters);
}

// Map control functions
function resetMapView() {
  var indiaBounds = [[6.4627, 68.1097], [35.5128, 97.3953]];
  indiaMap.fitBounds(indiaBounds);
  clearSelection();
}

function toggleHeatmap() {
  var btn = event.target;
  if (showHeatmap) {
    if (heatmapLayer) {
      indiaMap.removeLayer(heatmapLayer);
    }
    showHeatmap = false;
    btn.classList.remove('active');
    btn.textContent = 'Show Heatmap';
    hideHeatmapLegend();
  } else {
    createHeatmapLayer();
    showHeatmap = true;
    btn.classList.add('active');
    btn.textContent = 'Hide Heatmap';
  }
}

function showHeatmapLegend() {
  // Remove existing legend
  hideHeatmapLegend();
  
  var legendControl = L.control({ position: 'bottomright' });
  
  legendControl.onAdd = function(map) {
    var div = L.DomUtil.create('div', 'heatmap-legend');
    div.innerHTML = `
      <div class="legend-title">Blood Donation Activity</div>
      <div class="legend-item"><span class="legend-color very-high"></span> Very High (90%+)</div>
      <div class="legend-item"><span class="legend-color high"></span> High (80-90%)</div>
      <div class="legend-item"><span class="legend-color medium-high"></span> Medium-High (60-80%)</div>
      <div class="legend-item"><span class="legend-color medium"></span> Medium (40-60%)</div>
      <div class="legend-item"><span class="legend-color medium-low"></span> Medium-Low (30-40%)</div>
      <div class="legend-item"><span class="legend-color low"></span> Low (20-30%)</div>
      <div class="legend-item"><span class="legend-color very-low"></span> Very Low (0-20%)</div>
    `;
    return div;
  };
  
  legendControl.addTo(indiaMap);
  window.currentLegend = legendControl;
}

function hideHeatmapLegend() {
  if (window.currentLegend) {
    indiaMap.removeControl(window.currentLegend);
    window.currentLegend = null;
  }
}

function toggleClusters() {
  var btn = event.target;
  if (showClusters) {
    indiaMap.removeLayer(markerClusters);
    bloodBankMarkers.forEach(function(marker) {
      indiaMap.addLayer(marker);
    });
    showClusters = false;
    btn.classList.remove('active');
    btn.textContent = 'Enable Clusters';
  } else {
    bloodBankMarkers.forEach(function(marker) {
      indiaMap.removeLayer(marker);
    });
    indiaMap.addLayer(markerClusters);
    showClusters = true;
    btn.classList.add('active');
    btn.textContent = 'Disable Clusters';
  }
}

function createHeatmapLayer() {
  var heatmapPoints = bloodBankData.map(function(bank) {
    return [bank.lat, bank.lng, bank.donors / 100]; // Intensity based on donor count
  });
  
  // Create heat map points with random additional points for demo
  var additionalPoints = [
    [28.7041, 77.1025, 1.2], // Delhi area - very high activity
    [19.0760, 72.8777, 1.4], // Mumbai area - very high activity
    [12.9716, 77.5946, 1.1], // Bangalore area - very high activity
    [13.0827, 80.2707, 0.9], // Chennai area - high activity
    [22.5726, 88.3639, 0.7], // Kolkata area - medium-high activity
    [23.0225, 72.5714, 0.8], // Ahmedabad - high activity
    [17.3850, 78.4867, 0.6], // Hyderabad - medium-high activity
    [26.9124, 75.7873, 0.4], // Jaipur - medium activity
    [30.7333, 76.7794, 0.3], // Chandigarh - medium-low activity
    [26.8467, 80.9462, 0.5], // Lucknow - medium activity
    [21.1458, 79.0882, 0.2], // Nagpur - low activity
    [15.2993, 74.1240, 0.15], // Goa - very low activity
  ];
  
  var allPoints = heatmapPoints.concat(additionalPoints);
  
  // Create colorful circles based on intensity
  allPoints.forEach(function(point) {
    var intensity = point[2];
    var color = getHeatmapColor(intensity);
    
    var circle = L.circle([point[0], point[1]], {
      color: color.border,
      fillColor: color.fill,
      fillOpacity: 0.5 + (intensity * 0.3), // Dynamic opacity
      opacity: 1.0,
      weight: 3,
      radius: intensity * 45000 + 20000 // Dynamic radius
    });
    
    // Add popup with intensity info
    circle.bindPopup(
      `<div class="heatmap-popup">
        <strong>Blood Donation Activity</strong><br>
        Intensity: ${(intensity * 100).toFixed(0)}%<br>
        Activity Level: ${getActivityLevel(intensity)}<br>
        <span style="color: ${color.fill};">● ${color.fill}</span>
      </div>`
    );
    
    if (!heatmapLayer) {
      heatmapLayer = L.layerGroup();
    }
    heatmapLayer.addLayer(circle);
  });
  
  if (heatmapLayer) {
    indiaMap.addLayer(heatmapLayer);
  }
}

// Get color based on heatmap intensity (red = high, blue = low)
function getHeatmapColor(intensity) {
  var colors = {
    // Very High (1.0+) - Bright Red
    veryHigh: { fill: '#ff0000', border: '#cc0000' },
    // High (0.8-1.0) - Orange-Red 
    high: { fill: '#ff4500', border: '#ff0000' },
    // Medium-High (0.6-0.8) - Orange
    mediumHigh: { fill: '#ff8c00', border: '#ff4500' },
    // Medium (0.4-0.6) - Yellow-Orange
    medium: { fill: '#ffd700', border: '#ff8c00' },
    // Medium-Low (0.3-0.4) - Yellow-Green
    mediumLow: { fill: '#9acd32', border: '#ffd700' },
    // Low (0.2-0.3) - Green
    low: { fill: '#32cd32', border: '#9acd32' },
    // Very Low (0.0-0.2) - Blue
    veryLow: { fill: '#1e90ff', border: '#32cd32' }
  };
  
  if (intensity >= 1.0) return colors.veryHigh;
  if (intensity >= 0.8) return colors.high;
  if (intensity >= 0.6) return colors.mediumHigh;
  if (intensity >= 0.4) return colors.medium;
  if (intensity >= 0.3) return colors.mediumLow;
  if (intensity >= 0.2) return colors.low;
  return colors.veryLow;
}

function getActivityLevel(intensity) {
  if (intensity >= 1.0) return 'Very High';
  if (intensity >= 0.8) return 'High';
  if (intensity >= 0.6) return 'Medium-High';
  if (intensity >= 0.4) return 'Medium';
  if (intensity >= 0.3) return 'Medium-Low';
  if (intensity >= 0.2) return 'Low';
  return 'Very Low';
}

function focusOnState(stateId) {
  if (!stateId || !stateCoordinates[stateId]) {
    resetMapView();
    return;
  }
  
  var coords = stateCoordinates[stateId];
  indiaMap.setView(coords, 7);
  
  // Update regional stats
  updateRegionalStatsForState(stateId);
  
  // Highlight relevant markers
  highlightStateMarkers(stateId);
}

function highlightStateMarkers(stateId) {
  // Clear previous highlights
  clearSelection();
  
  // Get state name for filtering
  var stateNames = {
    delhi: 'Delhi',
    maharashtra: 'Mumbai',
    karnataka: 'Bangalore', 
    tamilnadu: 'Chennai',
    westbengal: 'Kolkata',
    gujarat: 'Ahmedabad',
    rajasthan: 'Jaipur',
    punjab: 'Chandigarh',
    up: 'Lucknow'
  };
  
  var targetCity = stateNames[stateId];
  if (targetCity) {
    bloodBankMarkers.forEach(function(marker) {
      var bank = bloodBankData.find(b => 
        Math.abs(b.lat - marker.getLatLng().lat) < 0.1 && 
        Math.abs(b.lng - marker.getLatLng().lng) < 0.1
      );
      
      if (bank && bank.city === targetCity) {
        marker.openPopup();
      }
    });
  }
}

function clearSelection() {
  indiaMap.closePopup();
  selectRegion('all');
}

function updateRegionalStatsForBank(bank) {
  var regionDetails = document.getElementById('regionDetails');
  var regionCard = `
    <div class="region-stat-card">
      <div class="region-name">${bank.name} - ${bank.city}</div>
      <div class="region-metrics">
        <div class="region-metric">
          <span class="metric-label">Blood Camps</span>
          <span class="metric-value">${bank.camps}</span>
        </div>
        <div class="region-metric">
          <span class="metric-label">Active Donors</span>
          <span class="metric-value">${bank.donors.toLocaleString()}</span>
        </div>
        <div class="region-metric">
          <span class="metric-label">Category</span>
          <span class="metric-value">${bank.category}</span>
        </div>
        <div class="region-metric">
          <span class="metric-label">Type</span>
          <span class="metric-value">${bank.type === 'major' ? 'Major Center' : 'Regional Center'}</span>
        </div>
      </div>
    </div>
  `;
  
  regionDetails.innerHTML = regionCard;
}

// Update regional stats for specific state
function updateRegionalStatsForState(stateId) {
  var stateData = getStateData(stateId);
  if (!stateData) return;
  
  var regionDetails = document.getElementById('regionDetails');
  var regionCard = `
    <div class="region-stat-card">
      <div class="region-name">${stateData.name}</div>
      <div class="region-metrics">
        <div class="region-metric">
          <span class="metric-label">Blood Camps</span>
          <span class="metric-value">${stateData.camps}</span>
        </div>
        <div class="region-metric">
          <span class="metric-label">Active Donors</span>
          <span class="metric-value">${stateData.donors.toLocaleString()}</span>
        </div>
        <div class="region-metric">
          <span class="metric-label">Units Available</span>
          <span class="metric-value">${stateData.units}</span>
        </div>
        <div class="region-metric">
          <span class="metric-label">Last Collection</span>
          <span class="metric-value">${stateData.lastCollection}</span>
        </div>
      </div>
    </div>
  `;
  
  regionDetails.innerHTML = regionCard;
}

// Get state-specific data
function getStateData(stateId) {
  var stateDataMap = {
    delhi: { name: 'Delhi', camps: 15, donors: 420, units: 280, lastCollection: '2 hours ago' },
    punjab: { name: 'Punjab', camps: 12, donors: 380, units: 240, lastCollection: '4 hours ago' },
    maharashtra: { name: 'Maharashtra', camps: 28, donors: 890, units: 650, lastCollection: '1 hour ago' },
    karnataka: { name: 'Karnataka', camps: 24, donors: 720, units: 580, lastCollection: '3 hours ago' },
    tamilnadu: { name: 'Tamil Nadu', camps: 19, donors: 580, units: 420, lastCollection: '2 hours ago' },
    westbengal: { name: 'West Bengal', camps: 16, donors: 480, units: 350, lastCollection: '5 hours ago' },
    gujarat: { name: 'Gujarat', camps: 22, donors: 670, units: 490, lastCollection: '1 hour ago' },
    rajasthan: { name: 'Rajasthan', camps: 16, donors: 480, units: 320, lastCollection: '6 hours ago' },
    up: { name: 'Uttar Pradesh', camps: 18, donors: 650, units: 480, lastCollection: '3 hours ago' }
  };
  
  return stateDataMap[stateId] || null;
}

// Region selection functionality
function selectRegion(regionKey) {
  // Remove previous selections
  document.querySelectorAll('.region').forEach(function(region) {
    region.classList.remove('selected');
  });
  
  // Update region details
  var regionDetails = document.getElementById('regionDetails');
  var regionCard = '';
  
  if (regionKey === 'all') {
    regionCard = `
      <div class="region-stat-card">
        <div class="region-name">All Regions Overview</div>
        <div class="region-metrics">
          <div class="region-metric">
            <span class="metric-label">Total Camps</span>
            <span class="metric-value">156</span>
          </div>
          <div class="region-metric">
            <span class="metric-label">Active Donors</span>
            <span class="metric-value">2,847</span>
          </div>
          <div class="region-metric">
            <span class="metric-label">Units Collected</span>
            <span class="metric-value">4,230</span>
          </div>
          <div class="region-metric">
            <span class="metric-label">Avg Response Time</span>
            <span class="metric-value">16 mins</span>
          </div>
        </div>
      </div>
    `;
  } else {
    // Select the clicked region
    var regionElement = document.querySelector('[data-region="' + regionKey + '"]');
    if (regionElement) {
      regionElement.classList.add('selected');
    }
    
    var data = regionalData[regionKey];
    if (data) {
      regionCard = `
        <div class="region-stat-card">
          <div class="region-name">${data.name}</div>
          <div class="region-metrics">
            <div class="region-metric">
              <span class="metric-label">Total Camps</span>
              <span class="metric-value">${data.camps}</span>
            </div>
            <div class="region-metric">
              <span class="metric-label">Active Donors</span>
              <span class="metric-value">${data.donors.toLocaleString()}</span>
            </div>
            <div class="region-metric">
              <span class="metric-label">Units Collected</span>
              <span class="metric-value">${data.units.toLocaleString()}</span>
            </div>
            <div class="region-metric">
              <span class="metric-label">Coverage States</span>
              <span class="metric-value">${data.states.length}</span>
            </div>
          </div>
          <div style="margin-top: 12px; font-size: 12px; color: #888;">
            States: ${data.states.join(', ')}
          </div>
        </div>
      `;
    }
  }
  
  regionDetails.innerHTML = regionCard;
}

// Generate heatmap for request activity
function generateHeatmap() {
  var heatmapCells = document.getElementById('heatmapCells');
  var html = '';
  
  // Generate 7 rows (days) x 6 columns (time slots)
  for (var day = 0; day < 7; day++) {
    html += '<div class="heatmap-row">';
    for (var hour = 0; hour < 6; hour++) {
      var level = getHeatmapLevel(day, hour);
      var requests = getRequestCount(day, hour);
      var tooltip = `${getDayName(day)} ${getHourName(hour)}: ${requests} requests`;
      html += `<div class="heatmap-cell level-${level}" title="${tooltip}" data-day="${day}" data-hour="${hour}"></div>`;
    }
    html += '</div>';
  }
  
  heatmapCells.innerHTML = html;
  
  // Add click handlers to heatmap cells
  document.querySelectorAll('.heatmap-cell').forEach(function(cell) {
    cell.addEventListener('click', function() {
      var day = parseInt(this.dataset.day);
      var hour = parseInt(this.dataset.hour);
      showHeatmapDetails(day, hour);
    });
  });
}

function getDayName(day) {
  var days = ['Monday', 'Tuesday', 'Wednesday', 'Thursday', 'Friday', 'Saturday', 'Sunday'];
  return days[day];
}

function getHourName(hour) {
  var hours = ['6AM-9AM', '9AM-12PM', '12PM-3PM', '3PM-6PM', '6PM-9PM', '9PM+'];
  return hours[hour];
}

// Calculate heatmap intensity level based on day and hour
function getHeatmapLevel(day, hour) {
  // Simulate realistic patterns:
  // - Higher activity during work hours (9AM-6PM = hours 1-4)
  // - Lower activity on weekends (days 5-6)
  // - Peak activity on weekday afternoons
  
  var isWeekend = day >= 5;
  var isWorkHours = hour >= 1 && hour <= 4;
  var isPeakHours = hour >= 2 && hour <= 3;
  
  var baseLevel = 1;
  
  if (isWeekend) {
    baseLevel = Math.max(0, baseLevel - 1);
  }
  
  if (isWorkHours) {
    baseLevel += 1;
  }
  
  if (isPeakHours && !isWeekend) {
    baseLevel += 1;
  }
  
  // Add some randomness
  var randomFactor = Math.random();
  if (randomFactor > 0.7) baseLevel += 1;
  if (randomFactor < 0.3) baseLevel = Math.max(0, baseLevel - 1);
  
  return Math.min(4, Math.max(0, baseLevel));
}

// Get request count for tooltip
function getRequestCount(day, hour) {
  var level = getHeatmapLevel(day, hour);
  var baseCounts = [2, 8, 15, 23, 35];
  var variance = Math.floor(Math.random() * 6) - 3;
  return Math.max(0, baseCounts[level] + variance);
}

// Show details when heatmap cell is clicked
function showHeatmapDetails(day, hour) {
  var dayNames = ['Monday', 'Tuesday', 'Wednesday', 'Thursday', 'Friday', 'Saturday', 'Sunday'];
  var hourNames = ['6AM-9AM', '9AM-12PM', '12PM-3PM', '3PM-6PM', '6PM-9PM', '9PM+'];
  var requests = getRequestCount(day, hour);
  var level = getHeatmapLevel(day, hour);
  
  var activityLevel = ['Very Low', 'Low', 'Medium', 'High', 'Very High'][level];
  var color = ['#f0f8ff', '#87ceeb', '#4682b4', '#ff8c00', '#ff4500'][level];
  
  alert(`${dayNames[day]} ${hourNames[hour]}\n${requests} requests\nActivity Level: ${activityLevel}`);
}

// Simulate real-time updates
function updateMetrics() {
  // Update active requests count
  var activeRequestsElement = document.querySelector('.metric-card:nth-child(2) .metric-value');
  if (activeRequestsElement) {
    var currentCount = parseInt(activeRequestsElement.textContent);
    var change = Math.floor(Math.random() * 5) - 2; // -2 to +2
    var newCount = Math.max(0, currentCount + change);
    activeRequestsElement.textContent = newCount;
  }
  
  // Update fulfillment rate
  var fulfillmentElement = document.querySelector('.metric-card:nth-child(4) .metric-value');
  if (fulfillmentElement) {
    var rates = ['92%', '93%', '94%', '95%', '96%'];
    fulfillmentElement.textContent = rates[Math.floor(Math.random() * rates.length)];
  }
}

// Simulate new request arrival
function addNewRequest() {
  var tbody = document.getElementById('recentRequestsBody');
  if (!tbody) return;
  
  var newRequestId = 'REQ-' + (146 + Math.floor(Math.random() * 50));
  var bloodGroups = ['A+', 'A-', 'B+', 'B-', 'AB+', 'AB-', 'O+', 'O-'];
  var locations = [
    'Mumbai - 400001',
    'Delhi - 110001', 
    'Bangalore - 560001',
    'Chennai - 600001',
    'Kolkata - 700001',
    'Pune - 411001'
  ];
  
  var bloodGroup = bloodGroups[Math.floor(Math.random() * bloodGroups.length)];
  var location = locations[Math.floor(Math.random() * locations.length)];
  
  var newRow = `
    <tr style="background-color: #f0f0f0;">
      <td><strong>${newRequestId}</strong></td>
      <td>${bloodGroup}</td>
      <td>${location}</td>
      <td><span class="status-badge active">Active</span></td>
      <td>0 responses</td>
      <td>Just now</td>
    </tr>
  `;
  
  tbody.insertAdjacentHTML('afterbegin', newRow);
  
  // Remove last row to keep table size manageable
  var rows = tbody.querySelectorAll('tr');
  if (rows.length > 6) {
    rows[rows.length - 1].remove();
  }
  
  // Highlight new row briefly
  setTimeout(function() {
    var firstRow = tbody.querySelector('tr');
    if (firstRow) {
      firstRow.style.backgroundColor = '';
    }
  }, 2000);
}

// Auto-refresh functions
setInterval(updateMetrics, 30000); // Update metrics every 30 seconds
setInterval(addNewRequest, 45000); // Add new request every 45 seconds

// Additional utility functions for demo purposes
function exportData() {
  alert('Export functionality would download CSV/Excel files with current analytics data.');
}

function refreshDashboard() {
  generateHeatmap();
  updateMetrics();
  alert('Dashboard data refreshed successfully!');
}

// Add the missing donor map functionality

// Keyboard shortcuts
document.addEventListener('keydown', function(e) {
  if (e.ctrlKey || e.metaKey) {
    switch(e.key) {
      case 'r':
        e.preventDefault();
        refreshDashboard();
        break;
      case 'e':
        e.preventDefault();
        exportData();
        break;
    }
  }
});
