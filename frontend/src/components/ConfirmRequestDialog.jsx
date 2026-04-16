import React from 'react';
import RequestConfirmationDialog from './RequestConfirmationDialog';

export default function ConfirmRequestDialog({ open, matchedCount, buckets, onCancel, onConfirm }) {
  return (
    <RequestConfirmationDialog
      open={open}
      title="Confirm Request"
      totalMatched={matchedCount}
      below10Km={buckets.below10Km}
      below50Km={buckets.below50Km}
      above50Km={buckets.above50Km}
      prompt="Send to nearest 20?"
      onCancel={onCancel}
      onConfirm={onConfirm}
    />
  );
}
