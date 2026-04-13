import React from 'react';

export default function ResultsTable({ banks, form, onCreateRequest, onBack, loading, userId, searchId, lastRequestId }) {
  return (
    <section className="card-section">
      <div className="section-header">
        <h2>Search Results</h2>
        <p className="muted">
          Showing {banks.length} matches for {form.bloodGroup} / {form.bloodComponent} near {form.hospitalPincode || '-'}.
        </p>
      </div>

      {banks.length === 0 ? (
        <div className="empty-state">No matching blood banks found in WHO or Redcross source APIs.</div>
      ) : (
        <div className="table-wrap">
          <table>
            <thead>
              <tr>
                <th>Name</th>
                <th>Category</th>
                <th>Pincode</th>
                <th>Address</th>
                <th>Contact</th>
                <th>Source</th>
              </tr>
            </thead>
            <tbody>
              {banks.map((bank, index) => (
                <tr key={`${bank.name}-${index}`}>
                  <td>{bank.name}</td>
                  <td>{bank.category}</td>
                  <td>{bank.pincode}</td>
                  <td>{bank.address}</td>
                  <td>{bank.contact}</td>
                  <td>{bank.source}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      <div className="actions">
        <button type="button" onClick={onCreateRequest} disabled={loading || !searchId}>
          Create Blood Request
        </button>
        <button type="button" onClick={onBack} disabled={loading}>
          Back to Search
        </button>
      </div>

      <div className="summary-row">
        <span>User ID: {userId || '-'}</span>
        <span>Search ID: {searchId || '-'}</span>
        <span>Last Request ID: {lastRequestId || '-'}</span>
      </div>
    </section>
  );
}
