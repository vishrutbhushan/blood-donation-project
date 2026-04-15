import React from 'react';
import { Button, Dialog, DialogActions, DialogContent, DialogTitle, Typography } from '@mui/material';

export default function ReRequestConfirmDialog({ open, preview, onCancel, onConfirm }) {
  return (
    <Dialog open={open} onClose={onCancel} fullWidth maxWidth="xs">
      <DialogTitle>Confirm Re-request</DialogTitle>
      <DialogContent>
        <Typography>Total matched: {preview.totalMatched}</Typography>
        <Typography>{'<10 km'}: {preview.below10Km}</Typography>
        <Typography>{'10 to <50 km'}: {preview.below50Km}</Typography>
        <Typography>{'>=50 km'}: {preview.above50Km}</Typography>
        <Typography sx={{ mt: 1 }}>Proceed with a new re-request dispatch?</Typography>
      </DialogContent>
      <DialogActions>
        <Button onClick={onCancel}>Cancel</Button>
        <Button onClick={onConfirm} className="primary-btn" variant="contained">Confirm</Button>
      </DialogActions>
    </Dialog>
  );
}
