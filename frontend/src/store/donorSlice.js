import { createSlice } from '@reduxjs/toolkit';

export const initialDonorState = {
  abhaId: '',
  phone: '',
  name: '',
  otpSent: false,
  otpValue: '',
  otpVerified: false,
  form: { bloodGroup: '', pincode: '', hospitalName: '' },
  matchedCount: 0,
  distanceBuckets: {
    below10Km: 0,
    below50Km: 0,
    above50Km: 0,
  },
  confirmOpen: false,
  searched: false,
};

const donorSlice = createSlice({
  name: 'donor',
  initialState: initialDonorState,
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
    setMatchedCount(state, action) {
      state.matchedCount = action.payload;
    },
    setDistanceBuckets(state, action) {
      state.distanceBuckets = action.payload;
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
      state.matchedCount = 0;
      state.distanceBuckets = { below10Km: 0, below50Km: 0, above50Km: 0 };
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
  setMatchedCount,
  setDistanceBuckets,
  setDonorSearched,
  setConfirmOpen,
  resetDonorOtp,
} = donorSlice.actions;

export default donorSlice.reducer;
