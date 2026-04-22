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
  const bucketRows = [
    { label: '<10 km', count: below10Km },
    { label: '10 to <50 km', count: below50Km },
    { label: '>=50 km', count: above50Km },
  ].filter((row) => row.count > 0);

  return (
    <Dialog open={open} onClose={onCancel} fullWidth maxWidth="xs">
      <DialogTitle>{title}</DialogTitle>
      <DialogContent>
        <Typography>Total matched: {totalMatched}</Typography>
        {bucketRows.map((row) => (
          <Typography key={row.label}>{row.label}: {row.count}</Typography>
        ))}
        <Typography sx={{ mt: 1 }}>{prompt}</Typography>
      </DialogContent>
      <DialogActions>
        <Button onClick={onCancel}>Cancel</Button>
        <Button onClick={onConfirm} className="primary-btn" variant="contained">Confirm</Button>
      </DialogActions>
    </Dialog>
  );
}
