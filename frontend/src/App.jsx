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
import { clearStatus, setDonorLoginOpen, setLoading, setScreen, setStatusText } from './store/uiSlice';
import {
  setBanks,
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
  resetDonorOtp,
} from './store/donorSlice';
import { resetSearchState } from './store/searchSlice';
import { resetUiState } from './store/uiSlice';

function tabIndex(screen) {
  if (screen === 'donors') {
    return 1;
  }
  return 0;
}

export default function App() {
  const dispatch = useDispatch();
  const [referenceData, setReferenceData] = useState({ bloodGroups: [], bloodComponents: [] });
  const showBrowserError = (message) => {
    if (typeof window !== 'undefined') {
      window.alert(message);
    }
  };
  const formatDateTime = (value) => {
    if (!value) {
      return { date: '-', time: '-' };
    }

    const dateValue = new Date(value);
    if (Number.isNaN(dateValue.getTime())) {
      return { date: '-', time: '-' };
    }

    return {
      date: new Intl.DateTimeFormat('en-GB', { day: '2-digit', month: 'short', year: 'numeric' }).format(dateValue),
      time: new Intl.DateTimeFormat('en-GB', { hour: '2-digit', minute: '2-digit', second: '2-digit', hour12: true }).format(dateValue),
    };
  };

  const { screen, loading, statusText, donorLoginOpen } = useSelector((state) => state.ui);
  const {
    form,
    banks,
    userId,
    searchId,
    requests,
    responses,
  } = useSelector((state) => state.search);
  const {
    abhaId,
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
          showBrowserError('Reference data failed to load');
        }
      });

    return () => {
      cancelled = true;
    };
  }, [dispatch]);

  useEffect(() => {
    if (!userId || !otpVerified) {
      return undefined;
    }

    let cancelled = false;

    const syncUserState = async () => {
      if (cancelled) {
        return;
      }
      await refreshUserState(userId);
    };

    syncUserState();

    const handleVisibilityChange = () => {
      if (document.visibilityState === 'visible') {
        syncUserState();
      }
    };

    document.addEventListener('visibilitychange', handleVisibilityChange);

    return () => {
      cancelled = true;
      document.removeEventListener('visibilitychange', handleVisibilityChange);
    };
  }, [userId, otpVerified]);

  useEffect(() => {
    if (!userId || !otpVerified) {
      return undefined;
    }

    const timer = window.setInterval(() => {
      refreshUserState(userId).catch(() => {});
    }, 5000);

    return () => {
      window.clearInterval(timer);
    };
  }, [userId, otpVerified]);

  const canSearch = useMemo(() => {
    return (
      form.bloodGroup &&
      form.bloodComponent &&
      form.hospitalPincode.trim().length > 0
    );
  }, [form]);

  const canSearchDonors = useMemo(() => {
    return (
      donorForm.hospitalName.trim().length > 0 &&
      donorForm.bloodGroup &&
      donorForm.pincode.trim().length > 0
    );
  }, [donorForm]);

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
    const [requestRows, responseRows] = await Promise.all([
      apiRequest(`/api/backend/requests/user/${currentUserId}`).catch(() => []),
      apiRequest(`/api/backend/requests/user/${currentUserId}/responses`).catch(() => []),
    ]);
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
      showBrowserError('Invalid ABHA');
      return;
    }

    dispatch(setLoading(true));
    dispatch(clearStatus());

    try {
      await apiRequest('/api/backend/auth/send-otp', {
        method: 'POST',
        body: JSON.stringify({ abhaId: abhaId.trim() }),
      });
      dispatch(setOtpSent(true));
      dispatch(setStatusText('OTP sent'));
    } catch {
      showBrowserError('OTP send failed');
    } finally {
      dispatch(setLoading(false));
    }
  }

  async function verifyDonorOtp() {
    if (!otpSent || !otpValue.trim()) {
      showBrowserError('Invalid OTP');
      return;
    }

    dispatch(setLoading(true));
    dispatch(clearStatus());

    try {
      await apiRequest('/api/backend/auth/verify-otp', {
        method: 'POST',
        body: JSON.stringify({ abhaId: abhaId.trim(), otp: otpValue.trim() }),
      });
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
      showBrowserError('OTP verification failed');
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
        compatibleStock: Array.isArray(row.compatibleStock) ? row.compatibleStock : [],
      }))
      .sort((left, right) => left.distanceKm - right.distanceKm);
  }

  async function fetchDonors(group, pincode) {
    const query = new URLSearchParams({
      bloodGroup: group || '',
      pincode: pincode || '',
      offset: '0',
      limit: '20',
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
      showBrowserError('Invalid input');
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
      showBrowserError('Search failed');
    } finally {
      dispatch(setLoading(false));
    }
  }

  async function searchDonors() {
    if (!otpVerified) {
      showBrowserError('Login required');
      return;
    }
    const currentRequestState = await apiRequest(`/api/backend/requests/user/${userId}/active`).catch(() => ({ active: false, createdToday: false }));
    if (currentRequestState?.active || currentRequestState?.createdToday) {
      window.alert('Only 1 unique request is allowed in a day.');
      return;
    }
    if (!canSearchDonors) {
      showBrowserError('Hospital name, blood group, and pincode are required');
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
      showBrowserError('Search failed');
    } finally {
      dispatch(setLoading(false));
    }
  }

  async function confirmAndCreateRequest() {
    if (!otpVerified || !userId) {
      showBrowserError('Login required');
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

      await apiRequest(`/api/backend/requests/${resolvedSearchId}`, {
        method: 'POST',
        body: JSON.stringify({
          bloodGroup: donorForm.bloodGroup,
          component: form.bloodComponent,
          unitsRequested: Number(form.unitsRequested) || 1,
          matchedCount,
        }),
      });
      dispatch(setConfirmOpen(false));
      dispatch(setStatusText('Request created'));
      await refreshUserState();
    } catch {
      showBrowserError('Request failed');
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
      showBrowserError('Re-request failed');
    } finally {
      dispatch(setLoading(false));
    }
  }

  async function handleReRequestClick(requestId, canReRequest) {
    if (!canReRequest) {
      const row = requests.find((request) => request.requestId === requestId);
      window.alert(row?.reRequestBlockedReason || 'Please wait for 1 hour before re-requesting this request.');
      return;
    }

    await onReRequest(requestId);
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
      showBrowserError('Re-request failed');
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

  function handleLogout() {
    dispatch(resetUiState());
    dispatch(resetSearchState());
    dispatch(resetDonorOtp());
    if (typeof window !== 'undefined') {
      window.localStorage.removeItem('hemo-connect.session.v1');
    }
  }

  return (
    <Box className="app-bg">
      <AppBar position="static" elevation={0} className="app-header">
        <Toolbar className="header-toolbar">
          <Box className="brand-row">
            <WaterDropOutlinedIcon className="drop-icon" />
            <Typography variant="h6" className="brand-title">Hemo Connect</Typography>
          </Box>
          <Box className="header-actions">
            <Tabs value={tabIndex(screen)} onChange={handleScreenTabChange} className="main-tabs" textColor="inherit" indicatorColor="secondary">
              <Tab label="Search for Blood Banks" />
              <Tab label="Search for Donors" />
            </Tabs>
            {otpVerified && userId ? (
              <Button variant="outlined" className="logout-btn" onClick={handleLogout}>
                Logout
              </Button>
            ) : null}
          </Box>
        </Toolbar>
      </AppBar>

      <Container maxWidth="lg" className="content-wrap">
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
              <TextField
                label="Pincode"
                value={form.hospitalPincode}
                onChange={(e) => updateSearchForm('hospitalPincode', e.target.value.replace(/\D/g, '').slice(0, 5))}
                inputProps={{ inputMode: 'numeric', pattern: '[0-9]*', maxLength: 5 }}
              />
            </Box>

            <Box className="action-row">
              <Button className="primary-btn" variant="contained" onClick={searchBloodBanks} disabled={loading || !canSearch}>Search</Button>
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
                        <TableCell>Compatible stock</TableCell>
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
                          <TableCell>
                            {bank.compatibleStock.length > 0 ? (
                              <Box sx={{ display: 'flex', flexDirection: 'column', gap: 0.25 }}>
                                {bank.compatibleStock.map((stock, stockIdx) => (
                                  <Typography key={`${bank.name}-${stock.bloodGroup}-${stock.component}-${stockIdx}`} variant="caption" component="span">
                                    {stock.bloodGroup} {stock.component} = {stock.unitsAvailable} units
                                  </Typography>
                                ))}
                              </Box>
                            ) : (
                              '-'
                            )}
                          </TableCell>
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
                <TextField
                  label="Pincode"
                  value={donorForm.pincode}
                  onChange={(e) => updateDonorSearchForm('pincode', e.target.value.replace(/\D/g, '').slice(0, 5))}
                  inputProps={{ inputMode: 'numeric', pattern: '[0-9]*', maxLength: 5 }}
                />
              </Box>
              <Box className="action-row">
                <Button className="primary-btn" variant="contained" onClick={searchDonors} disabled={loading || !otpVerified || !canSearchDonors}>Search Donors</Button>
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
                          <TableCell>Status</TableCell>
                          <TableCell>Created Date</TableCell>
                          <TableCell>Created Time</TableCell>
                          <TableCell>Contacted</TableCell>
                          <TableCell>Action</TableCell>
                        </TableRow>
                      </TableHead>
                      <TableBody>
                        {requests.map((row) => (
                          <TableRow key={row.requestId}>
                            <TableCell>{row.requestId}</TableCell>
                            <TableCell>{row.bloodGroup}</TableCell>
                            <TableCell>{row.status}</TableCell>
                            <TableCell>{formatDateTime(row.createdAt).date}</TableCell>
                            <TableCell>{formatDateTime(row.createdAt).time}</TableCell>
                            <TableCell>{row.numberOfDonorsContacted}</TableCell>
                            <TableCell>
                              <Button
                                variant="outlined"
                                size="small"
                                onClick={() => handleReRequestClick(row.requestId, row.canReRequest)}
                              >
                                Re-request
                              </Button>
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
                          <TableCell>Phone</TableCell>
                          <TableCell>Responded At</TableCell>
                        </TableRow>
                      </TableHead>
                      <TableBody>
                        {responses.map((row) => (
                          <TableRow key={row.responseId}>
                            <TableCell>{row.requestId}</TableCell>
                            <TableCell>{row.donorName}</TableCell>
                            <TableCell>{row.phoneNumber}</TableCell>
                            <TableCell>{`${formatDateTime(row.respondedAt).date} ${formatDateTime(row.respondedAt).time}`}</TableCell>
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
        otpSent={otpSent}
        canVerify={otpSent && /^\d{6}$/.test(otpValue.trim())}
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
