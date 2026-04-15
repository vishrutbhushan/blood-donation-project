import React from 'react';

export default function DonorSearchSection({
  phone,
  otpValue,
  otpSent,
  otpVerified,
  loading,
  donorForm,
  bloodGroups = [],
  onPhoneChange,
  onSendOtp,
  onOtpChange,
  onVerifyOtp,
  onDonorChange,
  onSearchDonors,
}) {
  const isPhoneValid = /^\d{10}$/.test(phone.trim());

  return (
    <section className="card-section">
      <div className="section-header">
        <h2>Search Donors</h2>
        <p className="muted">OTP is required here only.</p>
      </div>

      <div className="grid two">
        <label>
          Phone (10 digits)
          <input value={phone} onChange={(event) => onPhoneChange(event.target.value)} />
        </label>
        <label>
          Blood Group
          <select value={donorForm.bloodGroup} onChange={(event) => onDonorChange('bloodGroup', event.target.value)}>
            <option value="" disabled>Select blood group</option>
            {bloodGroups.map((group) => (
              <option key={group} value={group}>
                {group}
              </option>
            ))}
          </select>
        </label>
        <label>
          Pincode
          <input value={donorForm.pincode} onChange={(event) => onDonorChange('pincode', event.target.value)} />
        </label>
      </div>

      <div className="otp-row donor-otp-row">
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

      <div className="actions">
        <button type="button" onClick={onSearchDonors} disabled={loading || !otpVerified}>
          Search Donors
        </button>
      </div>
    </section>
  );
}
