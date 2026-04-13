import React from 'react';

export default function DonorResultsTable({ donors, donorForm, onBack, loading, searched }) {
  return (
    <section className="card-section">
      <div className="section-header">
        <h2>Donor Results</h2>
        <p className="muted">
          Showing {donors.length} matches for {donorForm.bloodGroup} near {donorForm.pincode || '-'}.
        </p>
      </div>

      {!searched ? (
        <div className="empty-state">Search for donors to see results.</div>
      ) : donors.length === 0 ? (
        <div className="empty-state">No matching donors found in WHO or Redcross source APIs.</div>
      ) : (
        <div className="table-wrap">
          <table>
            <thead>
              <tr>
                <th>Name</th>
                <th>Blood Group</th>
                <th>Pincode</th>
                <th>Location</th>
                <th>Phone</th>
                <th>Source</th>
              </tr>
            </thead>
            <tbody>
              {donors.map((donor, index) => (
                <tr key={`${donor.name}-${index}`}>
                  <td>{donor.name}</td>
                  <td>{donor.bloodGroup}</td>
                  <td>{donor.pincode}</td>
                  <td>{donor.location}</td>
                  <td>{donor.phone}</td>
                  <td>{donor.source}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      <div className="actions">
        <button type="button" onClick={onBack} disabled={loading}>
          Back to Search
        </button>
      </div>
    </section>
  );
}
