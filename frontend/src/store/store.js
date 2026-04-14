import { configureStore } from '@reduxjs/toolkit';
import uiReducer from './uiSlice';
import searchReducer from './searchSlice';
import donorReducer from './donorSlice';

export const store = configureStore({
  reducer: {
    ui: uiReducer,
    search: searchReducer,
    donor: donorReducer,
  },
});
