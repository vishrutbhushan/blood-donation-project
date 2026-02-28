/* ===================================================
   Hemo-Connect - WIREFRAME PROTOTYPE JS
=================================================== */

var requestCounter = 2;
var searchContext = { name: '', group: 'B+', component: 'Whole Blood', hospital: '', pincode: '' };

/* -- Screen navigation -- */
function showScreen(id) {
  var all = ['searchScreen', 'resultsScreen', 'dashboardScreen'];
  all.forEach(function(s) {
    var el = document.getElementById(s);
    if (el) el.classList.toggle('hidden', s !== id);
  });
}

function showDashView(id) {
  var all = ['dashHomeView', 'dashDonorsView', 'dashHistoryView'];
  all.forEach(function(v) {
    var el = document.getElementById(v);
    if (el) el.classList.toggle('hidden', v !== id);
  });
}

/* -- Search form -- */
function initiateSearch() {
  var name      = document.getElementById('patientName').value.trim();
  var group     = document.getElementById('bloodGroup').value;
  var component = document.getElementById('bloodComponent').value;
  var hospital  = document.getElementById('hospitalName').value.trim();
  var pincode   = document.getElementById('hospitalPincode').value.trim();

  if (!name || !group || !component || !hospital || !pincode) {
    alert('Please fill in all fields.');
    return;
  }
  if (!/^\d{6}$/.test(pincode)) {
    alert('Enter a valid 6-digit pincode.');
    return;
  }

  searchContext = { name: name, group: group, component: component, hospital: hospital, pincode: pincode };
  document.getElementById('pincodeConfirmVal').textContent = pincode;
  document.getElementById('pincodeConfirm').classList.remove('hidden');
  proceedToResults();
}

function proceedToResults() {
  var group     = document.getElementById('bloodGroup').value;
  var component = document.getElementById('bloodComponent').value;
  var pincode   = document.getElementById('hospitalPincode').value.trim();
  var pin       = parseInt(pincode, 10);

  var noResults = (group === 'AB-');
  document.getElementById('bloodBankResults').classList.toggle('hidden', noResults);
  document.getElementById('noResults').classList.toggle('hidden', !noResults);

  if (noResults) {
    document.getElementById('noResultsGroup').textContent = group;
    document.getElementById('noResultsPin').textContent   = pincode;
  } else {
    document.getElementById('resultsPin').textContent = pincode;

    var rows = [
      { name: 'Apollo Blood Bank',    cat: 'Private',    dist: '0.8 km', addr: '12 MG Road',        pin: pincode,          ph: '080-22334455', email: 'apollo@blood.in',      units: 12, updated: '15 mins ago' },
      { name: 'JSS Blood Bank',       cat: 'Private',    dist: '1.2 km', addr: 'Ramanashree Arcade', pin: String(pin + 1),  ph: '080-41125575', email: 'jss@bloodbank.in',     units: 8,  updated: '45 mins ago' },
      { name: 'NIMHANS Blood Centre', cat: 'Govt',       dist: '6.5 km', addr: 'Hosur Road',         pin: String(pin + 3),  ph: '080-46110007', email: 'nimhans@nic.in',       units: 6,  updated: '2 hrs ago' },
      { name: 'Rotary Blood Bank',    cat: 'Charitable', dist: '8.2 km', addr: 'Millers Road',       pin: String(pin + 7),  ph: '080-23340740', email: 'info@rotaryblood.org', units: 15, updated: '35 mins ago'  }
    ];
    var tbody = document.getElementById('bbTableBody');
    tbody.innerHTML = '';
    rows.forEach(function(r) {
      tbody.innerHTML +=
        '<tr>' +
        '<td><strong>' + r.name + '</strong></td>' +
        '<td>' + r.cat + '</td>' +
        '<td>' + r.dist + '</td>' +
        '<td>' + r.addr + ' - ' + r.pin + '</td>' +
        '<td><div>Ph: ' + r.ph + '</div><div>' + r.email + '</div></td>' +
        '<td>' + r.units + '</td>' +
        '<td class="muted">' + r.updated + '</td>' +
        '</tr>';
    });
  }

  showScreen('resultsScreen');
}

function goBackToForm() {
  document.getElementById('pincodeConfirm').classList.add('hidden');
  showScreen('searchScreen');
}

/* -- OTP flow -- */
function openOtpPopup() {
  document.getElementById('otpStep1').classList.remove('hidden');
  document.getElementById('otpStep2').classList.add('hidden');
  document.getElementById('mobileInput').value = '';
  if (document.getElementById('otpInput')) {
    document.getElementById('otpInput').value = '';
  }
  openModal('otpPopup');
}

function sendOTP() {
  var mob = document.getElementById('mobileInput').value.trim();
  if (!/^\d{10}$/.test(mob)) {
    alert('Please enter a valid 10-digit mobile number.');
    return;
  }
  document.getElementById('otpSentTo').textContent = '+91 ' + mob.slice(0,5) + ' ' + mob.slice(5);
  document.getElementById('otpStep1').classList.add('hidden');
  document.getElementById('otpStep2').classList.remove('hidden');
}

function confirmOTP() {
  var otp = document.getElementById('otpInput').value.trim();
  if (otp.length < 4) {
    alert('Please enter the OTP.');
    return;
  }
  closeAllPopups();
  goToDashboard();
}

/* -- Dashboard -- */
function goToDashboard() {
  closeAllPopups();
  var mob = document.getElementById('mobileInput').value.trim();
  if (mob) {
    document.getElementById('dashUserBadge').textContent = '+91 ' + mob.slice(0,5) + ' ' + mob.slice(5);
  }
  var pName = document.getElementById('patientName').value.trim() || 'User';
  document.getElementById('dashGreeting').textContent = 'Hello, ' + pName;
  
  if (!searchContext.pincode) {
    searchContext.pincode = '560001';
  }
  
  updateDashboardData();
  showScreen('dashboardScreen');
  showDashView('dashHomeView');
}

function updateDashboardData() {
  var grp = searchContext.group || 'B+';
  var comp = searchContext.component || 'Whole Blood';
  if (document.getElementById('histBloodGroup')) {
    document.getElementById('histBloodGroup').textContent = grp + ' / ' + comp;
  }
}

function showDonorsView() {
  populateDonorList();
  showDashView('dashDonorsView');
}

function populateDonorList() {
  var donors = [
    { id: 'REQ-001', name: 'Priya Sharma', phone: '+91 91234 56789', dist: '1.8 km' },
    { id: 'REQ-001', name: 'Arjun Mehta', phone: '+91 78901 23456', dist: '3.2 km' },
    { id: 'REQ-001', name: 'Snehal Banerjee', phone: '+91 85639 12745', dist: '5.1 km' }
  ];
  var tbody = document.getElementById('donorTableBody');
  if (tbody) {
    tbody.innerHTML = '';
    donors.forEach(function(d) {
      tbody.innerHTML += '<tr><td>' + d.id + '</td><td>' + d.name + '</td><td>' + d.phone + '</td><td>' + d.dist + '</td></tr>';
    });
  }
}

function showHistoryView() {
  showDashView('dashHistoryView');
}

function showDashHome() {
  showDashView('dashHomeView');
}

/* -- Request Blood popup -- */
function openRequestBloodPopup() {
  var pin  = searchContext.pincode || '';
  var grp  = searchContext.group || 'B+';
  var comp = searchContext.component || 'Whole Blood';
  if (document.getElementById('slide2Pincode'))   document.getElementById('slide2Pincode').textContent   = pin;
  if (document.getElementById('slide2Group'))     document.getElementById('slide2Group').textContent     = grp;
  if (document.getElementById('slide2Component')) document.getElementById('slide2Component').textContent = comp;
  openModal('requestBloodPopup');
}

function confirmRequest() {
  requestCounter++;
  var id = 'REQ-' + String(requestCounter).padStart(3, '0');
  document.getElementById('generatedRequestId').textContent = id;
  
  var grp = searchContext.group || 'B+';
  var comp = searchContext.component || 'Whole Blood';
  if (document.getElementById('histBloodGroup2')) {
    document.getElementById('histBloodGroup2').textContent = grp + ' / ' + comp;
  }
  
  closeAllPopups();
  openModal('confirmationPopup');
}

/* -- Modal utility -- */
function openModal(id) {
  document.getElementById(id).classList.remove('hidden');
}

function closeAllPopups() {
  ['otpPopup', 'requestBloodPopup', 'confirmationPopup'].forEach(function(id) {
    var el = document.getElementById(id);
    if (el) el.classList.add('hidden');
  });
}

/* Close modal on backdrop click */
document.addEventListener('DOMContentLoaded', function() {
  document.querySelectorAll('.modal-overlay').forEach(function(overlay) {
    overlay.addEventListener('click', function(e) {
      if (e.target === overlay && overlay.id === 'otpPopup') {
        closeAllPopups();
      }
    });
  });
});

