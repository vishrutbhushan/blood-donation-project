import { createSlice } from '@reduxjs/toolkit';

const initialState = {
  screen: 'blood-banks',
  loading: false,
  error: '',
  statusText: '',
  donorLoginOpen: false,
};

const uiSlice = createSlice({
  name: 'ui',
  initialState,
  reducers: {
    setScreen(state, action) {
      state.screen = action.payload;
    },
    setLoading(state, action) {
      state.loading = action.payload;
    },
    setError(state, action) {
      state.error = action.payload;
    },
    setStatusText(state, action) {
      state.statusText = action.payload;
    },
    clearStatus(state) {
      state.error = '';
      state.statusText = '';
    },
    setDonorLoginOpen(state, action) {
      state.donorLoginOpen = action.payload;
    },
  },
});

export const { setScreen, setLoading, setError, setStatusText, clearStatus, setDonorLoginOpen } = uiSlice.actions;
export default uiSlice.reducer;
