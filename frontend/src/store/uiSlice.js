import { createSlice } from '@reduxjs/toolkit';

export const initialUiState = {
  screen: 'blood-banks',
  loading: false,
  error: '',
  statusText: '',
  donorLoginOpen: false,
};

const uiSlice = createSlice({
  name: 'ui',
  initialState: initialUiState,
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
    resetUiState() {
      return initialUiState;
    },
  },
});

export const { setScreen, setLoading, setError, setStatusText, clearStatus, setDonorLoginOpen, resetUiState } = uiSlice.actions;
export default uiSlice.reducer;
