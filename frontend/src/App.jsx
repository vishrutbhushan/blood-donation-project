import React from 'react';
import { useMemo, useState } from 'react';
import DonorResultsTable from './components/DonorResultsTable';
import DonorSearchSection from './components/DonorSearchSection';
import ResultsTable from './components/ResultsTable';
import SearchForm from './components/SearchForm';
import StatusBanner from './components/StatusBanner';

const initialForm = {
  patientName: '',
  phone: '',
  bloodGroup: 'B+',
  bloodComponent: 'Whole Blood',
  hospitalName: '',
  hospitalPincode: '',
  unitsRequested: 1,
};

async function apiRequest(url, options = {}) {
  const response = await fetch(url, {
    headers: { 'Content-Type': 'application/json' },
    ...options,
  });

  if (!response.ok) {
    const message = await response.text();
    throw new Error(message || `Request failed: ${response.status}`);
  }

  return response.json();
}

function normalizeWhoBank(bank) {
  return {
    name: bank.name || '-',
    category: bank.category || '-',
    pincode: bank.pincode || '-',
    contact: bank.phone || '-',
    address: [bank.street, bank.city, bank.state].filter(Boolean).join(', ') || '-',
    inventory: Array.isArray(bank.blood_inventory) ? bank.blood_inventory : [],
    source: 'WHO',
  };
}

function normalizeRedcrossCentre(centre) {
  return {
    name: centre.name || '-',
    category: centre.category || '-',
    pincode: centre.postal_code || '-',
    contact: centre.contact_number || '-',
    address: centre.full_address || '-',
    inventory: Array.isArray(centre.blood_inventory) ? centre.blood_inventory : [],
    source: 'Redcross',
  };
}

function normalizeWhoDonor(donor) {
  return {
    name: donor.name || '-',
    bloodGroup: donor.blood_group || '-',
    pincode: donor.pincode || '-',
    location: [donor.city, donor.state].filter(Boolean).join(', ') || '-',
    phone: donor.phone || '-',
    source: 'WHO',
  };
}

function normalizeRedcrossDonor(person) {
  return {
    name: person.full_name || '-',
    bloodGroup: person.blood_type || '-',
    pincode: person.pincode || '-',
    location: person.address || '-',
    phone: person.contact_number || '-',
    source: 'Redcross',
  };
}

function matchesInventory(item, group, component) {
  const itemGroup = item.blood_group || item.blood_type;
  const itemComponent = item.component_type || item.component;
  const units = item.units_available ?? item.quantity ?? 0;
  return itemGroup === group && itemComponent === component && units > 0;
}

export default function App() {
  const [screen, setScreen] = useState('blood-banks');
  const [form, setForm] = useState(initialForm);
  const [banks, setBanks] = useState([]);
  const [donors, setDonors] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [message, setMessage] = useState('');
  const [donorPhone, setDonorPhone] = useState('');
  const [donorOtpSent, setDonorOtpSent] = useState(false);
  const [donorOtpValue, setDonorOtpValue] = useState('');
  const [donorOtpVerified, setDonorOtpVerified] = useState(false);
  const [donorForm, setDonorForm] = useState({ bloodGroup: 'B+', pincode: '' });
  const [donorSearched, setDonorSearched] = useState(false);

  const [userId, setUserId] = useState(null);
  const [searchId, setSearchId] = useState(null);
  const [lastRequestId, setLastRequestId] = useState(null);

  const canSearch = useMemo(() => {
    return (
      form.patientName.trim() &&
      /^\d{10}$/.test(form.phone.trim()) &&
      form.bloodGroup &&
      form.bloodComponent &&
      form.hospitalName.trim() &&
      /^\d{6}$/.test(form.hospitalPincode.trim())
    );
  }, [form]);

  function updateForm(key, value) {
    setForm((prev) => ({ ...prev, [key]: value }));
  }

  function updateDonorForm(key, value) {
    setDonorForm((prev) => ({ ...prev, [key]: value }));
  }

  function resetDonorOtp() {
    setDonorOtpSent(false);
    setDonorOtpValue('');
    setDonorOtpVerified(false);
    setDonors([]);
    setDonorSearched(false);
  }

  async function sendDonorOtp() {
    if (!/^\d{10}$/.test(donorPhone.trim())) {
      setError('Enter a valid 10-digit phone before sending OTP.');
      return;
    }

    setLoading(true);
    setError('');
    setMessage('');

    try {
      await apiRequest('/api/backend/auth/send-otp', {
        method: 'POST',
        body: JSON.stringify({ phone: donorPhone.trim() }),
      });
      setDonorOtpSent(true);
      setMessage('OTP sent. Use 1234 for now.');
    } catch (e) {
      setError(`Could not send OTP: ${e.message}`);
    } finally {
      setLoading(false);
    }
  }

  async function verifyDonorOtp() {
    if (!donorOtpSent) {
      setError('Send OTP first.');
      return;
    }

    if (!donorOtpValue.trim()) {
      setError('Enter OTP to verify.');
      return;
    }

    setLoading(true);
    setError('');
    setMessage('');

    try {
      await apiRequest('/api/backend/auth/verify-otp', {
        method: 'POST',
        body: JSON.stringify({ phone: donorPhone.trim(), otp: donorOtpValue.trim() }),
      });
      setDonorOtpVerified(true);
      setMessage('OTP verified.');
    } catch (e) {
      setDonorOtpVerified(false);
      setError(`OTP verification failed: ${e.message}`);
    } finally {
      setLoading(false);
    }
  }

  async function fetchBanks(group, component, pincode) {
    const [whoBanks, redcrossCentres] = await Promise.all([
      apiRequest('/api/who/blood-banks').catch(() => []),
      apiRequest('/api/redcross/centres').catch(() => []),
    ]);

    const merged = [
      ...whoBanks.map(normalizeWhoBank),
      ...redcrossCentres.map(normalizeRedcrossCentre),
    ];

    return merged.filter((row) => {
      const samePincode = !pincode || String(row.pincode) === String(pincode);
      const hasMatch = row.inventory.some((inv) => matchesInventory(inv, group, component));
      return samePincode && hasMatch;
    });
  }

  async function fetchDonors(group, pincode) {
    const [whoDonors, redcrossPeople] = await Promise.all([
      apiRequest('/api/who/donors').catch(() => []),
      apiRequest('/api/redcross/people').catch(() => []),
    ]);

    const merged = [...whoDonors.map(normalizeWhoDonor), ...redcrossPeople.map(normalizeRedcrossDonor)];

    return merged.filter((row) => {
      const sameGroup = !group || row.bloodGroup === group;
      const samePincode = !pincode || String(row.pincode) === String(pincode);
      return sameGroup && samePincode;
    });
  }

  async function runSearch() {
    if (!canSearch) {
      setError('Fill all fields. Phone must be 10 digits and pincode must be 6 digits.');
      return;
    }

    setLoading(true);
    setError('');
    setMessage('');

    try {
      const user = await apiRequest('/api/backend/users/get-or-create', {
        method: 'POST',
        body: JSON.stringify({ name: form.patientName.trim(), phone: form.phone.trim() }),
      });
      setUserId(user.userId);

      const createdSearch = await apiRequest(`/api/backend/searches/${user.userId}`, {
        method: 'POST',
        body: JSON.stringify({
          hospitalName: form.hospitalName.trim(),
          hospitalPincode: form.hospitalPincode.trim(),
          bloodGroup: form.bloodGroup,
          bloodComponent: form.bloodComponent,
        }),
      });

      setSearchId(createdSearch.searchId);

      const bankRows = await fetchBanks(form.bloodGroup, form.bloodComponent, form.hospitalPincode.trim());
      setBanks(bankRows);
      setScreen('results');
    } catch (e) {
      setError(`Search failed: ${e.message}`);
    } finally {
      setLoading(false);
    }
  }

  async function runDonorSearch() {
    if (!donorOtpVerified) {
      setError('Verify OTP first. Use 1234.');
      return;
    }

    if (!donorForm.bloodGroup || !donorForm.pincode.trim()) {
      setError('Fill donor blood group and pincode.');
      return;
    }

    setLoading(true);
    setError('');
    setMessage('');

    try {
      const donorRows = await fetchDonors(donorForm.bloodGroup, donorForm.pincode.trim());
      setDonors(donorRows);
      setDonorSearched(true);
      setScreen('donors');
    } catch (e) {
      setError(`Donor search failed: ${e.message}`);
    } finally {
      setLoading(false);
    }
  }

  async function createRequest() {
    if (!searchId) {
      setError('Search ID not available. Please search again.');
      return;
    }

    setLoading(true);
    setError('');
    setMessage('');

    try {
      const request = await apiRequest(`/api/backend/requests/${searchId}`, {
        method: 'POST',
        body: JSON.stringify({
          bloodGroup: form.bloodGroup,
          component: form.bloodComponent,
          unitsRequested: Number(form.unitsRequested) || 1,
        }),
      });
      setLastRequestId(request.requestId);
      setMessage(`Request created successfully. Request ID: ${request.requestId}`);
    } catch (e) {
      setError(`Could not create request: ${e.message}`);
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="app-shell">
      <header className="page-header">
        <div>
          <p className="eyebrow">Hemo-Connect</p>
          <h1>Blood Request Portal</h1>
          <p className="muted">Simple prototype-style interface for blood banks and donors.</p>
        </div>

        <div className="tabs" role="tablist" aria-label="Screens">
          <button type="button" onClick={() => setScreen('blood-banks')} disabled={loading} aria-pressed={screen !== 'donors'}>
            Blood Banks
          </button>
          <button type="button" onClick={() => setScreen('donors')} disabled={loading} aria-pressed={screen === 'donors'}>
            Donors
          </button>
        </div>
      </header>

      <StatusBanner error={error} message={message} loading={loading} />

      <main className="content-grid">
        {screen === 'blood-banks' ? (
          <SearchForm form={form} onChange={updateForm} onSearch={runSearch} loading={loading} />
        ) : screen === 'donors' ? (
          <>
            <DonorSearchSection
              phone={donorPhone}
              otpValue={donorOtpValue}
              otpSent={donorOtpSent}
              otpVerified={donorOtpVerified}
              loading={loading}
              donorForm={donorForm}
              onPhoneChange={(value) => {
                setDonorPhone(value);
                resetDonorOtp();
              }}
              onSendOtp={sendDonorOtp}
              onOtpChange={setDonorOtpValue}
              onVerifyOtp={verifyDonorOtp}
              onDonorChange={updateDonorForm}
              onSearchDonors={runDonorSearch}
            />
          </>
        ) : (
          <ResultsTable
            banks={banks}
            form={form}
            onCreateRequest={createRequest}
            onBack={() => setScreen('blood-banks')}
            loading={loading}
            userId={userId}
            searchId={searchId}
            lastRequestId={lastRequestId}
          />
        )}

        {screen === 'donors' ? (
          <DonorResultsTable
            donors={donors}
            donorForm={donorForm}
            onBack={() => setScreen('donors')}
            loading={loading}
            searched={donorSearched}
          />
        ) : null}
      </main>
    </div>
  );
}
