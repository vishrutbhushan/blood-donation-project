import React from 'react';
import { Box, Button, Dialog, DialogActions, DialogContent, DialogTitle, TextField } from '@mui/material';

export default function DonorLoginDialog({
  open,
  loading,
  abhaId,
  otpValue,
  otpSent,
  canVerify,
  onAbhaChange,
  onOtpChange,
  onSendOtp,
  onVerify,
  onClose,
}) {
  const isAbhaValid = /^\d{14}$/.test(abhaId.trim());

  return (
    <Dialog open={open} onClose={onClose} fullWidth maxWidth="xs">
      <DialogTitle>Donor Login</DialogTitle>
      <DialogContent>
        <Box className="login-grid">
          <TextField
            label="ABHA ID (14 digits)"
            value={abhaId}
            onChange={(e) => onAbhaChange(e.target.value)}
            fullWidth
            margin="dense"
          />
          <TextField
            label="OTP"
            value={otpValue}
            onChange={(e) => onOtpChange(e.target.value)}
            fullWidth
            margin="dense"
          />
          <Box className="action-row">
            <Button variant="outlined" onClick={onSendOtp} disabled={loading || !isAbhaValid || otpSent}>Send OTP</Button>
            <Button variant="contained" className="primary-btn" onClick={onVerify} disabled={loading || !canVerify}>Verify</Button>
          </Box>
        </Box>
      </DialogContent>
      <DialogActions>
        <Button onClick={onClose}>Cancel</Button>
      </DialogActions>
    </Dialog>
  );
}
