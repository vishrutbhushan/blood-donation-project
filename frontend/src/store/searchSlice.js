import { createSlice } from '@reduxjs/toolkit';

export const initialSearchForm = {
  bloodGroup: '',
  bloodComponent: '',
  hospitalPincode: '',
  unitsRequested: 1,
};

export const initialSearchState = {
  form: initialSearchForm,
  banks: [],
  userId: null,
  searchId: null,
  requests: [],
  responses: [],
};

const searchSlice = createSlice({
  name: 'search',
  initialState: initialSearchState,
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
    setRequests(state, action) {
      state.requests = action.payload;
    },
    setResponses(state, action) {
      state.responses = action.payload;
    },
    resetSearchState() {
      return initialSearchState;
    },
  },
});

export const {
  setSearchField,
  setBanks,
  setUserId,
  setSearchId,
  setRequests,
  setResponses,
  resetSearchState,
} = searchSlice.actions;
export default searchSlice.reducer;
