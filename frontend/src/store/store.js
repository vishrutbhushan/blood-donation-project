import { configureStore } from '@reduxjs/toolkit';
import uiReducer from './uiSlice';
import { initialUiState } from './uiSlice';
import searchReducer, { initialSearchState } from './searchSlice';
import donorReducer, { initialDonorState } from './donorSlice';

const STORAGE_KEY = 'hemo-connect.session.v1';

function clone(value) {
  return JSON.parse(JSON.stringify(value));
}

function loadPersistedState() {
  if (typeof window === 'undefined') {
    return undefined;
  }

  try {
    const raw = window.localStorage.getItem(STORAGE_KEY);
    if (!raw) {
      return undefined;
    }

    const parsed = JSON.parse(raw);
    return {
      ui: { ...clone(initialUiState), ...(parsed.ui || {}) },
      search: {
        ...clone(initialSearchState),
        ...(parsed.search || {}),
        form: { ...clone(initialSearchState.form), ...((parsed.search && parsed.search.form) || {}) },
      },
      donor: {
        ...clone(initialDonorState),
        ...(parsed.donor || {}),
        form: { ...clone(initialDonorState.form), ...((parsed.donor && parsed.donor.form) || {}) },
        distanceBuckets: {
          ...clone(initialDonorState.distanceBuckets),
          ...((parsed.donor && parsed.donor.distanceBuckets) || {}),
        },
      },
    };
  } catch {
    return undefined;
  }
}

function persistState(state) {
  if (typeof window === 'undefined') {
    return;
  }

  const snapshot = {
    ui: {
      screen: state.ui.screen,
      donorLoginOpen: false,
      loading: false,
      error: '',
      statusText: '',
    },
    search: state.search,
    donor: state.donor,
  };

  window.localStorage.setItem(STORAGE_KEY, JSON.stringify(snapshot));
}

export const store = configureStore({
  preloadedState: loadPersistedState(),
  reducer: {
    ui: uiReducer,
    search: searchReducer,
    donor: donorReducer,
  },
});

if (typeof window !== 'undefined') {
  store.subscribe(() => {
    persistState(store.getState());
  });
}
