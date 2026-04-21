import { createSlice } from '@reduxjs/toolkit';

const initialForm = {
  bloodGroup: '',
  bloodComponent: '',
  hospitalPincode: '',
  unitsRequested: 1,
};

const searchSlice = createSlice({
  name: 'search',
  initialState: {
    form: initialForm,
    banks: [],
    userId: null,
    searchId: null,
    lastRequestId: null,
    activeRequest: false,
    requests: [],
    responses: [],
  },
  reducers: {
    setSearchField(state, action) {
      const { key, value } = action.payload;
      state.form[key] = value;
    },
    setBanks(state, action) {
      state.banks = action.payload;
    },
    setUserId(state, action) {
      state.userId = action.payload;
    },
    setSearchId(state, action) {
      state.searchId = action.payload;
    },
    setLastRequestId(state, action) {
      state.lastRequestId = action.payload;
    },
    setActiveRequest(state, action) {
      state.activeRequest = action.payload;
    },
    setRequests(state, action) {
      state.requests = action.payload;
    },
    setResponses(state, action) {
      state.responses = action.payload;
    },
  },
});

export const {
  setSearchField,
  setBanks,
  setUserId,
  setSearchId,
  setLastRequestId,
  setActiveRequest,
  setRequests,
  setResponses,
} = searchSlice.actions;
export default searchSlice.reducer;
