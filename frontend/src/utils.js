export const isUuid = value => /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i.test(value);
export function moneyValue(value) {
  const normalized = String(value).trim().replace(',', '.');
  if (!/^\d+(\.\d{1,2})?$/.test(normalized)) throw new Error('Введите сумму с точностью до двух знаков после запятой.');
  const number = Number(normalized);
  if (!Number.isFinite(number) || number <= 0 || number > 9999999999999.99) throw new Error('Введите положительную сумму до 9 999 999 999 999,99.');
  return number;
}
export const money = value => new Intl.NumberFormat('ru-RU', { style: 'currency', currency: 'RUB' }).format(value);
export const shortId = value => value ? `${value.slice(0, 8)}…${value.slice(-4)}` : '';
