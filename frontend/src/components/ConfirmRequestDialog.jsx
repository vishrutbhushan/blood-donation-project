import React from 'react';
import { Button, Dialog, DialogActions, DialogContent, DialogTitle, Typography } from '@mui/material';

export default function ConfirmRequestDialog({ open, matchedCount, buckets, onCancel, onConfirm }) {
  return (
    <Dialog open={open} onClose={onCancel} fullWidth maxWidth="xs">
      <DialogTitle>Confirm Request</DialogTitle>
      <DialogContent>
        <Typography>Total matched: {matchedCount}</Typography>
        <Typography>{'<10 km'}: {buckets.below10Km}</Typography>
        <Typography>{'10 to <50 km'}: {buckets.below50Km}</Typography>
        <Typography>{'>=50 km'}: {buckets.above50Km}</Typography>
        <Typography sx={{ mt: 1 }}>Send to nearest 20?</Typography>
      </DialogContent>
      <DialogActions>
        <Button onClick={onCancel}>Cancel</Button>
        <Button onClick={onConfirm} className="primary-btn" variant="contained">Confirm</Button>
      </DialogActions>
    </Dialog>
  );
}
