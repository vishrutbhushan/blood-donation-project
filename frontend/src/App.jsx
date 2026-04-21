import React, { useEffect, useMemo, useState } from 'react';
import {
  Accordion,
  AccordionDetails,
  AccordionSummary,
  AppBar,
  Box,
  Button,
  Container,
  FormControl,
  InputLabel,
  MenuItem,
  Paper,
  Select,
  Tab,
  Tabs,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  TextField,
  Toolbar,
  Typography,
} from '@mui/material';
import ExpandMoreIcon from '@mui/icons-material/ExpandMore';
import WaterDropOutlinedIcon from '@mui/icons-material/WaterDropOutlined';
import { useDispatch, useSelector } from 'react-redux';
import ConfirmRequestDialog from './components/ConfirmRequestDialog';
import DonorLoginDialog from './components/DonorLoginDialog';
import ReRequestConfirmDialog from './components/ReRequestConfirmDialog';
import apiRequest from './lib/apiRequest';
import { clearStatus, setDonorLoginOpen, setError, setLoading, setScreen, setStatusText } from './store/uiSlice';
import {
  setActiveRequest,
  setBanks,
  setLastRequestId,
  setRequests,
  setResponses,
  setSearchField,
  setSearchId,
  setUserId,
} from './store/searchSlice';
import {
  setAbhaId,
  setConfirmOpen,
  setDistanceBuckets,
  setDonorField,
  setDonorSearched,
  setMatchedCount,
  setOtpSent,
  setOtpValue,
  setOtpVerified,
  setProfile,
} from './store/donorSlice';

function tabIndex(screen) {
  if (screen === 'donors') {
    return 1;
  }
  return 0;
}

export default function App() {
  const dispatch = useDispatch();
  const [referenceData, setReferenceData] = useState({ bloodGroups: [], bloodComponents: [] });

  const { screen, loading, error, statusText, donorLoginOpen } = useSelector((state) => state.ui);
  const {
    form,
    banks,
    userId,
    searchId,
    lastRequestId,
    activeRequest,
    requests,
    responses,
  } = useSelector((state) => state.search);
  const {
    abhaId,
    name,
    otpSent,
    otpValue,
    otpVerified,
    form: donorForm,
    matchedCount,
    distanceBuckets,
    confirmOpen,
    searched,
  } = useSelector((state) => state.donor);
  const [reRequestPreview, setReRequestPreview] = useState({
    open: false,
    requestId: null,
    totalMatched: 0,
    below10Km: 0,
    below50Km: 0,
    above50Km: 0,
  });

  function closeReRequestPreview() {
    setReRequestPreview({ open: false, requestId: null, totalMatched: 0, below10Km: 0, below50Km: 0, above50Km: 0 });
  }

  useEffect(() => {
    let cancelled = false;

    apiRequest('/api/backend/reference-data')
      .then((data) => {
        if (cancelled) {
          return;
        }

        const bloodGroups = Array.isArray(data?.bloodGroups) ? data.bloodGroups : [];
        const bloodComponents = Array.isArray(data?.bloodComponents) ? data.bloodComponents : [];
        setReferenceData({ bloodGroups, bloodComponents });

        if (!form.bloodGroup && bloodGroups.length > 0) {
          dispatch(setSearchField({ key: 'bloodGroup', value: bloodGroups[0] }));
        }
        if (!form.bloodComponent && bloodComponents.length > 0) {
          dispatch(setSearchField({ key: 'bloodComponent', value: bloodComponents[0] }));
        }
        if (!donorForm.bloodGroup && bloodGroups.length > 0) {
          dispatch(setDonorField({ key: 'bloodGroup', value: bloodGroups[0] }));
        }
      })
      .catch(() => {
        if (!cancelled) {
          dispatch(setError('Reference data failed to load'));
        }
      });

    return () => {
      cancelled = true;
    };
  }, [dispatch]);

  const canSearch = useMemo(() => {
    return (
      form.bloodGroup &&
      form.bloodComponent &&
      /^\d{6}$/.test(form.hospitalPincode.trim())
    );
  }, [form]);

  function updateSearchForm(key, value) {
    dispatch(setSearchField({ key, value }));
  }

  function updateDonorSearchForm(key, value) {
    dispatch(setDonorField({ key, value }));
  }

  async function refreshUserState(currentUserId = userId) {
    if (!currentUserId) {
      return;
    }
    const [active, requestRows, responseRows] = await Promise.all([
      apiRequest(`/api/backend/requests/user/${currentUserId}/active`).catch(() => ({ active: false })),
      apiRequest(`/api/backend/requests/user/${currentUserId}`).catch(() => []),
      apiRequest(`/api/backend/requests/user/${currentUserId}/responses`).catch(() => []),
    ]);
    dispatch(setActiveRequest(Boolean(active.active)));
    dispatch(setRequests(requestRows));
    dispatch(setResponses(responseRows));
  }

  function closeDonorLoginDialog() {
    dispatch(setDonorLoginOpen(false));
  }

  function openDonorLoginDialog() {
    dispatch(setDonorLoginOpen(true));
    dispatch(clearStatus());
  }

  async function sendDonorOtp() {
    if (!/^\d{14}$/.test(abhaId.trim())) {
      dispatch(setError('Invalid ABHA'));
      return;
    }

    dispatch(setLoading(true));
    dispatch(clearStatus());

    try {
      const result = await apiRequest('/api/backend/auth/send-otp', {
        method: 'POST',
        body: JSON.stringify({ abhaId: abhaId.trim() }),
      });
      dispatch(setProfile({ name: result.name, phone: result.phone }));
      dispatch(setOtpSent(true));
      dispatch(setStatusText('OTP sent'));
    } catch {
      dispatch(setError('OTP send failed'));
    } finally {
      dispatch(setLoading(false));
    }
  }

  async function verifyDonorOtp() {
    if (!otpSent || !otpValue.trim()) {
      dispatch(setError('Invalid OTP'));
      return;
    }

    dispatch(setLoading(true));
    dispatch(clearStatus());

    try {
      const verified = await apiRequest('/api/backend/auth/verify-otp', {
        method: 'POST',
        body: JSON.stringify({ abhaId: abhaId.trim(), otp: otpValue.trim() }),
      });
      dispatch(setProfile({ name: verified.name, phone: verified.phone }));
      dispatch(setOtpVerified(true));

      const user = await apiRequest('/api/backend/users/get-or-create', {
        method: 'POST',
        body: JSON.stringify({ abhaId: abhaId.trim() }),
      });
      dispatch(setUserId(user.userId));
      await refreshUserState(user.userId);

      dispatch(setStatusText('Verified'));
      dispatch(setDonorLoginOpen(false));
      dispatch(setScreen('donors'));
    } catch {
      dispatch(setOtpVerified(false));
      dispatch(setError('OTP verification failed'));
    } finally {
      dispatch(setLoading(false));
    }
  }

  async function fetchAndTransformBanks(group, component, pincode) {
    const query = new URLSearchParams({
      pincode: pincode || '',
      bloodGroup: group || '',
      component: component || '',
    }).toString();
    const rows = await apiRequest(`/api/backend/blood-banks/search?${query}`);
    const list = Array.isArray(rows) ? rows : [];

    return list
      .map((row) => ({
        name: row.bankName || '-',
        category: row.category || '-',
        pincode: row.pincode || '-',
        contact: row.phone || '-',
        address: row.address || '-',
        source: row.sourceId || '-',
        distanceKm: Number(row.distanceKm || 0),
        bloodGroup: group || '-',
        component: component || '-',
      }))
      .sort((left, right) => left.distanceKm - right.distanceKm);
  }

  async function fetchDonors(group, pincode) {
    const query = new URLSearchParams({
      bloodGroup: group || '',
      pincode: pincode || '',
      offset: '0',
      limit: '200',
    }).toString();

    const result = await apiRequest(`/api/backend/donors/search?${query}`);
    return {
      totalMatched: Number(result?.totalMatched || 0),
      below10Km: Number(result?.below10KmCount || 0),
      below50Km: Number(result?.below50KmCount || 0),
      above50Km: Number(result?.above50KmCount || 0),
    };
  }

  async function searchBloodBanks() {
    if (!canSearch) {
      dispatch(setError('Invalid input'));
      return;
    }

    dispatch(setLoading(true));
    dispatch(clearStatus());

    try {
      dispatch(setSearchId(null));
      const bankRows = await fetchAndTransformBanks(form.bloodGroup, form.bloodComponent, form.hospitalPincode.trim());
      dispatch(setBanks(bankRows));
      dispatch(setScreen('blood-banks'));
      dispatch(setStatusText('Search complete'));
    } catch {
      dispatch(setError('Search failed'));
    } finally {
      dispatch(setLoading(false));
    }
  }

  async function searchDonors() {
    if (!otpVerified) {
      dispatch(setError('Login required'));
      return;
    }
    if (activeRequest) {
      dispatch(setError('Active request exists'));
      return;
    }
    if (!donorForm.hospitalName.trim() || !/^\d{6}$/.test(donorForm.pincode.trim())) {
      dispatch(setError('Hospital name and valid pincode are required'));
      return;
    }

    dispatch(setLoading(true));
    dispatch(clearStatus());

    try {
      const donorResult = await fetchDonors(donorForm.bloodGroup, donorForm.pincode.trim());
      dispatch(setMatchedCount(donorResult.totalMatched));
      dispatch(setDistanceBuckets({
        below10Km: donorResult.below10Km,
        below50Km: donorResult.below50Km,
        above50Km: donorResult.above50Km,
      }));
      dispatch(setDonorSearched(true));
      dispatch(setConfirmOpen(true));
      dispatch(setStatusText('Matches loaded'));
    } catch {
      dispatch(setError('Search failed'));
    } finally {
      dispatch(setLoading(false));
    }
  }

  async function confirmAndCreateRequest() {
    if (!otpVerified || !userId) {
      dispatch(setError('Login required'));
      return;
    }

    dispatch(setLoading(true));
    dispatch(clearStatus());
    try {
      let resolvedSearchId = searchId;
      if (!resolvedSearchId) {
        const createdSearch = await apiRequest(`/api/backend/searches/${userId}`, {
          method: 'POST',
          body: JSON.stringify({
            hospitalName: donorForm.hospitalName.trim(),
            hospitalPincode: donorForm.pincode.trim(),
            bloodGroup: donorForm.bloodGroup,
            bloodComponent: form.bloodComponent,
          }),
        });
        resolvedSearchId = createdSearch.searchId;
        dispatch(setSearchId(resolvedSearchId));
      }

      const request = await apiRequest(`/api/backend/requests/${resolvedSearchId}`, {
        method: 'POST',
        body: JSON.stringify({
          bloodGroup: donorForm.bloodGroup,
          component: form.bloodComponent,
          unitsRequested: Number(form.unitsRequested) || 1,
          matchedCount,
        }),
      });
      dispatch(setLastRequestId(request.requestId));
      dispatch(setConfirmOpen(false));
      dispatch(setStatusText('Request created'));
      await refreshUserState();
    } catch {
      dispatch(setError('Request failed'));
    } finally {
      dispatch(setLoading(false));
    }
  }

  async function onReRequest(requestId) {
    dispatch(setLoading(true));
    dispatch(clearStatus());
    try {
      const preview = await apiRequest(`/api/backend/requests/${requestId}/re-request-preview`);
      setReRequestPreview({
        open: true,
        requestId,
        totalMatched: Number(preview?.totalMatched || 0),
        below10Km: Number(preview?.below10KmCount || 0),
        below50Km: Number(preview?.below50KmCount || 0),
        above50Km: Number(preview?.above50KmCount || 0),
      });
      dispatch(setStatusText('Re-request preview loaded'));
    } catch {
      dispatch(setError('Re-request failed'));
    } finally {
      dispatch(setLoading(false));
    }
  }

  async function confirmReRequest() {
    if (!reRequestPreview.requestId) {
      return;
    }
    dispatch(setLoading(true));
    dispatch(clearStatus());
    try {
      await apiRequest(`/api/backend/requests/${reRequestPreview.requestId}/re-request`, { method: 'POST' });
      closeReRequestPreview();
      dispatch(setStatusText('Re-request created'));
      await refreshUserState();
    } catch {
      dispatch(setError('Re-request failed'));
    } finally {
      dispatch(setLoading(false));
    }
  }

  async function onDispatchNext(requestId) {
    dispatch(setLoading(true));
    dispatch(clearStatus());
    try {
      await apiRequest(`/api/backend/requests/${requestId}/dispatch-next`, { method: 'POST' });
      dispatch(setStatusText('Next 20 notified'));
      await refreshUserState();
    } catch {
      dispatch(setError('Dispatch failed'));
    } finally {
      dispatch(setLoading(false));
    }
  }

  function handleScreenTabChange(_event, newValue) {
    if (newValue === 0) {
      dispatch(setScreen('blood-banks'));
      return;
    }

    if (!otpVerified) {
      openDonorLoginDialog();
      return;
    }

    dispatch(setScreen('donors'));
  }

  return (
    <Box className="app-bg">
      <AppBar position="static" elevation={0} className="app-header">
        <Toolbar className="header-toolbar">
          <Box className="brand-row">
            <WaterDropOutlinedIcon className="drop-icon" />
            <Typography variant="h6" className="brand-title">Hemo Connect</Typography>
          </Box>
          <Tabs value={tabIndex(screen)} onChange={handleScreenTabChange} className="main-tabs" textColor="inherit" indicatorColor="secondary">
            <Tab label="Search for Blood Banks" />
            <Tab label="Search for Donors" />
          </Tabs>
        </Toolbar>
      </AppBar>

      <Container maxWidth="lg" className="content-wrap">
        {error ? <Paper className="error-strip">{error}</Paper> : null}

        {screen === 'blood-banks' && (
          <Paper className="panel" variant="outlined" elevation={0}>
            <Box className="form-grid">
              <FormControl>
                <InputLabel>Blood Group</InputLabel>
                <Select value={form.bloodGroup} label="Blood Group" onChange={(e) => updateSearchForm('bloodGroup', e.target.value)}>
                  <MenuItem value="" disabled>Select blood group</MenuItem>
                  {referenceData.bloodGroups.map((group) => <MenuItem key={group} value={group}>{group}</MenuItem>)}
                </Select>
              </FormControl>
              <FormControl>
                <InputLabel>Component</InputLabel>
                <Select value={form.bloodComponent} label="Component" onChange={(e) => updateSearchForm('bloodComponent', e.target.value)}>
                  <MenuItem value="" disabled>Select component</MenuItem>
                  {referenceData.bloodComponents.map((component) => <MenuItem key={component} value={component}>{component}</MenuItem>)}
                </Select>
              </FormControl>
              <TextField label="Pincode" value={form.hospitalPincode} onChange={(e) => updateSearchForm('hospitalPincode', e.target.value)} />
            </Box>

            <Box className="action-row">
              <Button className="primary-btn" variant="contained" onClick={searchBloodBanks} disabled={loading}>Search</Button>
            </Box>

            <Box className="results-wrap">
              {banks.length === 0 ? (
                <Paper variant="outlined" className="empty-box">No records found</Paper>
              ) : (
                <TableContainer component={Paper} variant="outlined">
                  <Table size="small">
                    <TableHead>
                      <TableRow>
                        <TableCell>#</TableCell>
                        <TableCell>Name</TableCell>
                        <TableCell>Category</TableCell>
                        <TableCell>Pincode</TableCell>
                        <TableCell>Distance (km)</TableCell>
                        <TableCell>Address</TableCell>
                        <TableCell>Contact</TableCell>
                        <TableCell>Source</TableCell>
                      </TableRow>
                    </TableHead>
                    <TableBody>
                      {banks.map((bank, idx) => (
                        <TableRow key={`${bank.name}-${idx}`}>
                          <TableCell>{idx + 1}</TableCell>
                          <TableCell>{bank.name}</TableCell>
                          <TableCell>{bank.category}</TableCell>
                          <TableCell>{bank.pincode}</TableCell>
                          <TableCell>{Number.isFinite(bank.distanceKm) ? bank.distanceKm.toFixed(2) : '0.00'}</TableCell>
                          <TableCell>{bank.address}</TableCell>
                          <TableCell>{bank.contact}</TableCell>
                          <TableCell>{bank.source}</TableCell>
                        </TableRow>
                      ))}
                    </TableBody>
                  </Table>
                </TableContainer>
              )}
            </Box>
          </Paper>
        )}

        {screen === 'donors' && (
          <Box className="stack-wrap">
            <Paper className="panel" variant="outlined" elevation={0}>
              <Box className="form-grid">
                <TextField label="Hospital Name" value={donorForm.hospitalName} onChange={(e) => updateDonorSearchForm('hospitalName', e.target.value)} />
                <FormControl>
                  <InputLabel>Blood Group</InputLabel>
                  <Select value={donorForm.bloodGroup} label="Blood Group" onChange={(e) => updateDonorSearchForm('bloodGroup', e.target.value)}>
                    <MenuItem value="" disabled>Select blood group</MenuItem>
                    {referenceData.bloodGroups.map((group) => <MenuItem key={group} value={group}>{group}</MenuItem>)}
                  </Select>
                </FormControl>
                <TextField label="Pincode" value={donorForm.pincode} onChange={(e) => updateDonorSearchForm('pincode', e.target.value)} />
              </Box>
              <Box className="action-row">
                <Button className="primary-btn" variant="contained" onClick={searchDonors} disabled={loading || !otpVerified || activeRequest}>Search Donors</Button>
              </Box>

              {searched && (
                <Box className="results-wrap">
                  <TableContainer component={Paper} variant="outlined">
                    <Table size="small">
                      <TableHead>
                        <TableRow>
                          <TableCell>Total Matched</TableCell>
                          <TableCell>{'< 10 km'}</TableCell>
                          <TableCell>{'10 to < 50 km'}</TableCell>
                          <TableCell>{'>= 50 km'}</TableCell>
                        </TableRow>
                      </TableHead>
                      <TableBody>
                        <TableRow>
                          <TableCell>{matchedCount}</TableCell>
                          <TableCell>{distanceBuckets.below10Km}</TableCell>
                          <TableCell>{distanceBuckets.below50Km}</TableCell>
                          <TableCell>{distanceBuckets.above50Km}</TableCell>
                        </TableRow>
                      </TableBody>
                    </Table>
                  </TableContainer>
                </Box>
              )}
            </Paper>

            <Accordion className="accordion" elevation={0}>
              <AccordionSummary expandIcon={<ExpandMoreIcon />}>Requests</AccordionSummary>
              <AccordionDetails>
                {requests.length === 0 ? (
                  <Paper variant="outlined" className="empty-box">No records found</Paper>
                ) : (
                  <TableContainer component={Paper} variant="outlined">
                    <Table size="small">
                      <TableHead>
                        <TableRow>
                          <TableCell>Request ID</TableCell>
                          <TableCell>Blood Group</TableCell>
                          <TableCell>Component</TableCell>
                          <TableCell>Status</TableCell>
                          <TableCell>Expiry</TableCell>
                          <TableCell>Contacted</TableCell>
                          <TableCell>Action</TableCell>
                        </TableRow>
                      </TableHead>
                      <TableBody>
                        {requests.map((row) => (
                          <TableRow key={row.requestId}>
                            <TableCell>{row.requestId}</TableCell>
                            <TableCell>{row.bloodGroup}</TableCell>
                            <TableCell>{row.component}</TableCell>
                            <TableCell>{row.status}</TableCell>
                            <TableCell>{String(row.expiresAt || '-')}</TableCell>
                            <TableCell>{row.numberOfDonorsContacted}</TableCell>
                            <TableCell>
                              <Box className="inline-actions">
                                <Button size="small" onClick={() => onReRequest(row.requestId)} disabled={!row.canReRequest}>Re-request</Button>
                                <Button size="small" onClick={() => onDispatchNext(row.requestId)}>Next 20</Button>
                              </Box>
                            </TableCell>
                          </TableRow>
                        ))}
                      </TableBody>
                    </Table>
                  </TableContainer>
                )}
              </AccordionDetails>
            </Accordion>

            <Accordion className="accordion" elevation={0}>
              <AccordionSummary expandIcon={<ExpandMoreIcon />}>Donor Responses</AccordionSummary>
              <AccordionDetails>
                {responses.length === 0 ? (
                  <Paper variant="outlined" className="empty-box">No records found</Paper>
                ) : (
                  <TableContainer component={Paper} variant="outlined">
                    <Table size="small">
                      <TableHead>
                        <TableRow>
                          <TableCell>Request ID</TableCell>
                          <TableCell>Name</TableCell>
                          <TableCell>ABHA</TableCell>
                          <TableCell>Phone</TableCell>
                          <TableCell>Status</TableCell>
                        </TableRow>
                      </TableHead>
                      <TableBody>
                        {responses.map((row) => (
                          <TableRow key={row.responseId}>
                            <TableCell>{row.requestId}</TableCell>
                            <TableCell>{row.donorName}</TableCell>
                            <TableCell>{row.abhaId || '-'}</TableCell>
                            <TableCell>{row.phoneNumber}</TableCell>
                            <TableCell>{row.responseStatus}</TableCell>
                          </TableRow>
                        ))}
                      </TableBody>
                    </Table>
                  </TableContainer>
                )}
              </AccordionDetails>
            </Accordion>
          </Box>
        )}
      </Container>


      <DonorLoginDialog
        open={donorLoginOpen}
        loading={loading}
        abhaId={abhaId}
        otpValue={otpValue}
        canVerify={otpSent}
        onAbhaChange={(value) => {
          dispatch(setAbhaId(value));
          dispatch(setOtpSent(false));
          dispatch(setOtpVerified(false));
          dispatch(setOtpValue(''));
        }}
        onOtpChange={(value) => dispatch(setOtpValue(value))}
        onSendOtp={sendDonorOtp}
        onVerify={verifyDonorOtp}
        onClose={closeDonorLoginDialog}
      />

      <ConfirmRequestDialog
        open={confirmOpen}
        matchedCount={matchedCount}
        buckets={distanceBuckets}
        onCancel={() => dispatch(setConfirmOpen(false))}
        onConfirm={confirmAndCreateRequest}
      />

      <ReRequestConfirmDialog
        open={reRequestPreview.open}
        preview={reRequestPreview}
        onCancel={closeReRequestPreview}
        onConfirm={confirmReRequest}
      />
    </Box>
  );
}
