import React from 'react';

export default function StatusBanner({ error, message, loading }) {
  if (!error && !message && !loading) {
    return null;
  }

  return (
    <div className="banner-stack">
      {error ? <div className="error">{error}</div> : null}
      {message ? <div className="ok">{message}</div> : null}
      {loading ? <div className="loading">Loading...</div> : null}
    </div>
  );
}
