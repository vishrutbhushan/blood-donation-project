import React from 'react';
import RequestConfirmationDialog from './RequestConfirmationDialog';

export default function ReRequestConfirmDialog({ open, preview, onCancel, onConfirm }) {
  return (
    <RequestConfirmationDialog
      open={open}
      title="Confirm Re-request"
      totalMatched={preview.totalMatched}
      below10Km={preview.below10Km}
      below50Km={preview.below50Km}
      above50Km={preview.above50Km}
      prompt="Proceed with a new re-request dispatch?"
      onCancel={onCancel}
      onConfirm={onConfirm}
    />
  );
}
