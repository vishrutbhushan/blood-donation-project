import React from 'react';
import { Button, Dialog, DialogActions, DialogContent, DialogTitle, Typography } from '@mui/material';

export default function RequestConfirmationDialog({
  open,
  title,
  totalMatched,
  below10Km,
  below50Km,
  above50Km,
  prompt,
  onCancel,
  onConfirm,
}) {
  return (
    <Dialog open={open} onClose={onCancel} fullWidth maxWidth="xs">
      <DialogTitle>{title}</DialogTitle>
      <DialogContent>
        <Typography>Total matched: {totalMatched}</Typography>
        <Typography>{'<10 km'}: {below10Km}</Typography>
        <Typography>{'10 to <50 km'}: {below50Km}</Typography>
        <Typography>{'>=50 km'}: {above50Km}</Typography>
        <Typography sx={{ mt: 1 }}>{prompt}</Typography>
      </DialogContent>
      <DialogActions>
        <Button onClick={onCancel}>Cancel</Button>
        <Button onClick={onConfirm} className="primary-btn" variant="contained">Confirm</Button>
      </DialogActions>
    </Dialog>
  );
}
