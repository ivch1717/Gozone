export class ApiError extends Error {
  constructor(status, message) { super(message); this.status = status; }
}
async function request(path, options = {}) {
  let response;
  try {
    response = await fetch(path, { ...options, headers: { 'Content-Type': 'application/json', ...options.headers }, signal: AbortSignal.timeout(15000) });
  } catch { throw new Error('Нет связи с сервером. Проверьте, что контейнеры запущены, и повторите попытку.'); }
  const body = await response.text();
  if (!response.ok) {
    let message = body;
    try { const json = JSON.parse(body); message = json.detail || json.message || json.error || body; } catch { /* API also returns plain text. */ }
    throw new ApiError(response.status, message && !message.includes('<html') ? message : `Сервер вернул ошибку ${response.status}.`);
  }
  return body ? JSON.parse(body) : null;
}
const post = (path, data) => request(path, { method: 'POST', body: JSON.stringify(data) });
export const api = {
  balance: user => request(`/payments/balance?userId=${encodeURIComponent(user)}`),
  account: user => post('/payments/account', { userId: user }),
  topUp: (user, amount) => post(`/payments/accounts/${user}/top-ups`, { amount }),
  orders: (user, page, size, sort) => request(`/orders/users/${user}?${new URLSearchParams({ page, size, sort })}`),
  order: id => request(`/orders/${id}`),
  createOrder: data => post('/orders', data),
};
