export default async function apiRequest(url, options = {}) {
  const response = await fetch(url, {
    headers: { 'Content-Type': 'application/json' },
    ...options,
  });

  if (!response.ok) {
    let message = 'Request failed';
    try {
      const text = await response.text();
      if (text) {
        message = text;
      }
    } catch {
      message = 'Request failed';
    }
    throw new Error(message);
  }

  return response.json();
}
