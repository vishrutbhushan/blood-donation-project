import React from 'react';

export default function SearchForm({ form, bloodGroups = [], bloodComponents = [], onChange, onSearch, loading }) {
  return (
    <section className="card-section">
      <div className="section-header">
        <h2>Find Blood Banks</h2>
        <p className="muted">Simple search based on patient, hospital, blood group, and pincode.</p>
      </div>

      <div className="grid two">
        <label>
          Patient Name
          <input value={form.patientName} onChange={(event) => onChange('patientName', event.target.value)} />
        </label>
        <label>
          Phone (10 digits)
          <input value={form.phone} onChange={(event) => onChange('phone', event.target.value)} />
        </label>
        <label>
          Blood Group
          <select value={form.bloodGroup} onChange={(event) => onChange('bloodGroup', event.target.value)}>
            <option value="" disabled>Select blood group</option>
            {bloodGroups.map((group) => (
              <option key={group} value={group}>
                {group}
              </option>
            ))}
          </select>
        </label>
        <label>
          Blood Component
          <select value={form.bloodComponent} onChange={(event) => onChange('bloodComponent', event.target.value)}>
            <option value="" disabled>Select component</option>
            {bloodComponents.map((component) => (
              <option key={component} value={component}>
                {component}
              </option>
            ))}
          </select>
        </label>
        <label>
          Hospital Name
          <input value={form.hospitalName} onChange={(event) => onChange('hospitalName', event.target.value)} />
        </label>
        <label>
          Hospital Pincode
          <input value={form.hospitalPincode} onChange={(event) => onChange('hospitalPincode', event.target.value)} />
        </label>
        <label>
          Units Needed
          <input
            type="number"
            min="1"
            value={form.unitsRequested}
            onChange={(event) => onChange('unitsRequested', event.target.value)}
          />
        </label>
      </div>

      <div className="actions">
        <button type="button" onClick={onSearch} disabled={loading}>
          Search Blood Banks
        </button>
      </div>
    </section>
  );
}
