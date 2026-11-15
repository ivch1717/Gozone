import test from 'node:test';
import assert from 'node:assert/strict';
import { isUuid, moneyValue } from './utils.js';

test('Money accepts decimal comma and two decimal places', () => {
  assert.equal(moneyValue(' 125,45 '), 125.45);
  assert.equal(moneyValue('1000'), 1000);
});
test('Money rejects invalid, non-positive, overly precise or oversized values', () => {
  for (const value of ['', '0', '-1', 'NaN', 'Infinity', '1e3', '1.001', '10000000000000', '10 рублей']) {
    assert.throws(() => moneyValue(value), Error, value);
  }
});
test('User identifier requires a full UUID', () => {
  assert.equal(isUuid('550e8400-e29b-41d4-a716-446655440000'), true);
  assert.equal(isUuid('not-a-user'), false);
  assert.equal(isUuid(''), false);
});
