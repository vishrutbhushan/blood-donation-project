import React from 'react';

export default function OtpPanel({
  phone,
  otpValue,
  otpSent,
  otpVerified,
  loading,
  onSendOtp,
  onOtpChange,
  onVerifyOtp,
}) {
  const isPhoneValid = /^\d{10}$/.test(phone.trim());

  return (
    <section className="card-section">
      <div className="section-header">
        <h2>Phone Verification</h2>
        <p className="muted">Use the hardcoded OTP 1234 for now.</p>
      </div>

      <div className="otp-row">
        <button type="button" onClick={onSendOtp} disabled={loading || !isPhoneValid}>
          Send OTP
        </button>
        <input
          type="text"
          inputMode="numeric"
          placeholder="Enter OTP"
          value={otpValue}
          onChange={(event) => onOtpChange(event.target.value)}
        />
        <button type="button" onClick={onVerifyOtp} disabled={loading || !otpSent}>
          Verify OTP
        </button>
      </div>

      <div className="status-line">
        <span>Status</span>
        <strong>{otpVerified ? 'Verified' : otpSent ? 'Pending verification' : 'Not sent'}</strong>
      </div>
    </section>
  );
}
