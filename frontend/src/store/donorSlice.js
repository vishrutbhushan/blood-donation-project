import { createSlice } from '@reduxjs/toolkit';

const donorSlice = createSlice({
  name: 'donor',
  initialState: {
    abhaId: '',
    phone: '',
    name: '',
    otpSent: false,
    otpValue: '',
    otpVerified: false,
    form: { bloodGroup: 'B+', pincode: '' },
    donors: [],
    matchedCount: 0,
    confirmOpen: false,
    searched: false,
  },
  reducers: {
    setAbhaId(state, action) {
      state.abhaId = action.payload;
    },
    setProfile(state, action) {
      state.name = action.payload.name || '';
      state.phone = action.payload.phone || '';
    },
    setOtpSent(state, action) {
      state.otpSent = action.payload;
    },
    setOtpValue(state, action) {
      state.otpValue = action.payload;
    },
    setOtpVerified(state, action) {
      state.otpVerified = action.payload;
    },
    setDonorField(state, action) {
      const { key, value } = action.payload;
      state.form[key] = value;
    },
    setDonors(state, action) {
      state.donors = action.payload;
      state.matchedCount = action.payload.length;
    },
    setMatchedCount(state, action) {
      state.matchedCount = action.payload;
    },
    setDonorSearched(state, action) {
      state.searched = action.payload;
    },
    setConfirmOpen(state, action) {
      state.confirmOpen = action.payload;
    },
    resetDonorOtp(state) {
      state.abhaId = '';
      state.phone = '';
      state.name = '';
      state.otpSent = false;
      state.otpValue = '';
      state.otpVerified = false;
      state.donors = [];
      state.matchedCount = 0;
      state.confirmOpen = false;
      state.searched = false;
    },
  },
});

export const {
  setAbhaId,
  setProfile,
  setOtpSent,
  setOtpValue,
  setOtpVerified,
  setDonorField,
  setDonors,
  setMatchedCount,
  setDonorSearched,
  setConfirmOpen,
  resetDonorOtp,
} = donorSlice.actions;

export default donorSlice.reducer;
