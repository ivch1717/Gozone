import React, { useCallback, useEffect, useRef, useState } from 'react';
import { ArrowDownLeft, ArrowUpRight, ArrowRight, Check, ChevronLeft, ChevronRight, CircleHelp, CreditCard, LayoutDashboard, LoaderCircle, Package, Plus, RefreshCw, ShoppingBag, UserRound, Wallet, X } from 'lucide-react';
import { api } from './api.js';
import { isUuid, money, moneyValue, shortId } from './utils.js';
import Modal from './Modal.jsx';

const statusNames = { NEW: 'В обработке', PAID: 'Оплачен', CANCELED: 'Отменён' };
function Status({ value }) { return <span className={`status ${value?.toLowerCase()}`}><i/>{statusNames[value] || value}</span>; }
function initialUser() {
  try { const stored = localStorage.getItem('gozone.user'); if (isUuid(stored || '')) return stored; } catch { /* Storage can be disabled. */ }
  return crypto.randomUUID();
}

export default function App() {
  const [user, setUser] = useState(initialUser);
  const [view, setView] = useState('overview');
  const [balance, setBalance] = useState(null);
  const [accountExists, setAccountExists] = useState(null);
  const [data, setData] = useState(null);
  const [page, setPage] = useState(0);
  const [size, setSize] = useState(5);
  const [sort, setSort] = useState('id,asc');
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [toast, setToast] = useState('');
  const [modal, setModal] = useState(null);
  const [busy, setBusy] = useState(false);
  const [formError, setFormError] = useState('');
  const [amount, setAmount] = useState('1000');
  const [userInput, setUserInput] = useState(user);
  const [selectedOrder, setSelectedOrder] = useState(null);
  const [tracked, setTracked] = useState(null);
  const generation = useRef(0);
  const currentUser = useRef(user);
  currentUser.current = user;
  const refresh = useCallback(async (silent = false) => {
    const ticket = ++generation.current;
    if (!silent) setLoading(true);
    const results = await Promise.allSettled([api.balance(user), api.orders(user, page, size, sort)]);
    if (ticket !== generation.current) return;
    const errors = [];
    if (results[0].status === 'fulfilled') { setBalance(results[0].value); setAccountExists(true); }
    else if (results[0].reason.status === 404) { setAccountExists(false); setBalance(null); }
    else errors.push(results[0].reason.message);
    if (results[1].status === 'fulfilled') {
      const next = results[1].value;
      setData(next);
      if (page > 0 && page >= next.page.totalPages) setPage(Math.max(0, next.page.totalPages - 1));
    } else errors.push(results[1].reason.message);
    setError([...new Set(errors)].join(' '));
    setLoading(false);
  }, [user, page, size, sort]);
  useEffect(() => { refresh(); return () => { generation.current++; }; }, [refresh]);
  useEffect(() => { try { localStorage.setItem('gozone.user', user); } catch { /* Optional persistence. */ } }, [user]);
  useEffect(() => { if (!toast) return; const timer = setTimeout(() => setToast(''), 5000); return () => clearTimeout(timer); }, [toast]);
  const hasPending = data?.content.some(order => order.status === 'NEW');
  useEffect(() => {
    if (!hasPending && !tracked) return;
    let cancelled = false;
    let timer;
    const poll = async () => {
      await refresh(true);
      if (tracked) {
        try {
          const order = await api.order(tracked);
          if (!cancelled && order.status !== 'NEW') {
            setTracked(null);
            setSelectedOrder(previous => previous?.id === order.id ? order : previous);
            setToast(order.status === 'PAID' ? 'Заказ оплачен. Баланс обновлён.' : 'Заказ отменён. Списание не выполнено.');
            await refresh(true);
          }
        } catch { /* Main refresh displays connectivity errors; retry later. */ }
      }
      if (!cancelled) timer = setTimeout(poll, 3000);
    };
    timer = setTimeout(poll, 3000);
    return () => { cancelled = true; clearTimeout(timer); };
  }, [hasPending, tracked, refresh]);
  useEffect(() => {
    if (!selectedOrder) return;
    const updated = data?.content.find(order => order.id === selectedOrder.id);
    if (updated && updated.status !== selectedOrder.status) setSelectedOrder(updated);
  }, [data, selectedOrder]);

  function open(name) { setFormError(''); setModal(name); if (name === 'user') setUserInput(user); }
  async function mutate(action, success) {
    const actingUser = user;
    setBusy(true); setFormError('');
    try {
      const result = await action();
      if (currentUser.current !== actingUser) return;
      setModal(null); setToast(success); await refresh(true); return result;
    } catch (e) { setFormError(e.message); } finally { setBusy(false); }
  }
  function switchUser(event) {
    event.preventDefault();
    if (!isUuid(userInput.trim())) { setFormError('Введите UUID пользователя в формате xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx.'); return; }
    if (userInput.trim().toLowerCase() === user) { setModal(null); refresh(); return; }
    generation.current++;
    setUser(userInput.trim().toLowerCase()); setData(null); setBalance(null); setAccountExists(null); setPage(0); setTracked(null); setSelectedOrder(null); setError(''); setModal(null); setToast('Пользователь выбран');
  }
  async function submitOrder(event) {
    event.preventDefault();
    const values = new FormData(event.currentTarget);
    let price;
    try { price = moneyValue(values.get('amount')); } catch (e) { setFormError(e.message); return; }
    const name = values.get('name').trim();
    if (!name) { setFormError('Введите название заказа.'); return; }
    const order = await mutate(() => api.createOrder({ userId: user, name, amount: price, description: values.get('description').trim() }), 'Заказ создан. Ожидаем результат оплаты.');
    if (order) { setTracked(order.status === 'NEW' ? order.id : null); setSelectedOrder(order); setModal('detail'); }
  }
  const total = data?.page.totalElements ?? 0;
  const titles = { overview: 'Обзор', orders: 'Мои заказы', account: 'Мой счёт' };

  return <div className="app-shell">
    <aside className="sidebar">
      <a href="#" className="brand" onClick={event => { event.preventDefault(); setView('overview'); }}><span className="brand-mark">g<span>↗</span></span>gozone<span className="brand-dot">.</span></a>
      <div className="workspace-label">ЛИЧНЫЙ КАБИНЕТ</div>
      <nav aria-label="Основная навигация">{[['overview', LayoutDashboard, 'Обзор'], ['orders', ShoppingBag, 'Мои заказы'], ['account', Wallet, 'Мой счёт']].map(([key, Icon, text]) => <button key={key} onClick={() => setView(key)} className={view === key ? 'nav-item active' : 'nav-item'} aria-current={view === key ? 'page' : undefined}><Icon size={20}/>{text}{key === 'orders' && <span className="nav-count">{total}</span>}</button>)}</nav>
      <div className="sidebar-note"><div className="note-icon"><CircleHelp size={20}/></div><strong>Всё под контролем</strong><p>Пополняйте счёт и следите за оплатой заказов в одном месте.</p><button onClick={() => open('help')}>Как это работает <ArrowUpRight size={15}/></button></div>
      <button className="sidebar-user" onClick={() => open('user')}><span className="avatar"><UserRound size={19}/></span><span><strong>Мой профиль</strong><small>{shortId(user)}</small></span><ArrowRight size={17}/></button>
    </aside>
    <div className="main-shell"><header className="topbar"><span>Личный кабинет <span className="breadcrumb">/</span> <strong>{titles[view]}</strong></span><button className="profile-button" onClick={() => open('user')}><span className="profile-dot"/> Пользователь <span className="profile-id">{user.slice(0, 8)}</span><UserRound size={17}/></button></header>
      <main><div className="page-heading"><div><div className="eyebrow">ВАШЕ ПРОСТРАНСТВО</div><h1>{titles[view]}</h1><p>{view === 'orders' ? 'Все заказы и их актуальные статусы.' : view === 'account' ? 'Управляйте балансом для будущих покупок.' : 'Ваш баланс, заказы и ничего лишнего.'}</p></div><div className="heading-actions"><button className="icon-button refresh" onClick={() => refresh()} disabled={loading} aria-label="Обновить данные"><RefreshCw size={19} className={loading ? 'spin' : ''}/></button><button className="button primary" onClick={() => open('order')}><Plus size={18}/>Новый заказ</button></div></div>
      {error && <div className="error-banner" role="alert"><span>{error}</span><button onClick={() => refresh()}>Повторить</button></div>}
      {view !== 'orders' && <section className="summary-grid" aria-label="Сводка"><div className="balance-card"><div className="balance-top"><span><Wallet size={19}/>Доступный баланс</span><span className="currency-label">RUB</span></div><div className="balance-value">{balance !== null ? money(balance) : accountExists === false ? 'Счёт не открыт' : '—'}</div><p>{accountExists === false ? 'Откройте счёт, чтобы оплачивать заказы' : 'Для новых покупок и больших планов'}</p><button className="balance-action" disabled={busy || accountExists === null} onClick={() => open(accountExists ? 'topup' : 'account')}><Plus size={17}/>{accountExists === false ? 'Открыть счёт' : 'Пополнить счёт'}</button><div className="card-decoration" aria-hidden="true"/></div><div className="stat-card"><span className="stat-icon"><Package size={23}/></span><span className="muted">Всего заказов</span><strong>{data ? total : '—'}</strong><button onClick={() => setView('orders')}>Перейти к заказам <ArrowUpRight size={17}/></button></div><div className="tip-card"><span className="tip-kicker"><span/> ПРОСТО И УДОБНО</span><h2>От заказа<br/>до оплаты — один шаг.</h2><p>Создайте заказ, а оплату со счёта мы выполним автоматически.</p><button onClick={() => open('order')}>Создать заказ <ArrowRight size={18}/></button></div></section>}
      {view === 'account' ? <section className="panel account-panel"><div><span className="stat-icon"><CreditCard size={24}/></span><h2>Счёт пользователя</h2><p>Счёт привязан к UUID. Используйте тот же идентификатор, чтобы вернуться к своему балансу и заказам.</p><code className="full-id">{user}</code><button className="button secondary" onClick={() => open('user')}>Сменить пользователя</button></div><div className="account-hint"><ArrowDownLeft size={26}/><h3>Сначала пополнение, затем покупка</h3><p>При оформлении заказа оплата проходит в фоне. Итоговый статус появится автоматически. Если счёта или средств недостаточно, заказ будет отменён.</p></div></section> : <section className="panel orders-panel"><div className="section-heading"><div><h2>{view === 'overview' ? 'Ваши заказы' : 'Все заказы'} <span className="count-badge">{total}</span></h2><p>Статусы оплаты обновляются автоматически</p></div><label className="sort-control"><span>Сортировка</span><select value={sort} onChange={event => { setSort(event.target.value); setPage(0); }}><option value="id,asc">По номеру</option><option value="name,asc">По названию</option><option value="amount,desc">Сначала дороже</option><option value="amount,asc">Сначала дешевле</option><option value="status,asc">По статусу</option></select></label></div>
      <div className="table-scroll"><table><thead><tr><th>ЗАКАЗ</th><th>НОМЕР</th><th>СУММА</th><th>СТАТУС</th><th><span className="sr-only">Подробнее</span></th></tr></thead><tbody>{data?.content.map(order => <tr key={order.id}><td><div className="order-name"><span className="order-icon"><ShoppingBag size={18}/></span><span><strong>{order.name}</strong><small>{order.description || 'Без описания'}</small></span></div></td><td><span className="order-id" title={order.id}>{shortId(order.id)}</span></td><td className="order-amount">{money(order.amount)}</td><td><Status value={order.status}/></td><td><button className="icon-button" aria-label={`Открыть заказ ${order.name}`} onClick={() => { setSelectedOrder(order); open('detail'); }}><ArrowUpRight size={18}/></button></td></tr>)}</tbody></table></div>
      {(!data || data.content.length === 0) && <div className="empty-state"><span className="empty-icon">{loading ? <LoaderCircle size={27} className="spin"/> : <ShoppingBag size={29}/>}</span><h3>{loading ? 'Загружаем заказы' : error ? 'Не удалось загрузить заказы' : 'Здесь будут ваши заказы'}</h3><p>{loading ? 'Это займёт несколько секунд' : error ? 'Проверьте соединение и обновите страницу.' : 'Начните с первого — остальное мы возьмём на себя.'}</p>{!loading && !error && <button className="button secondary" onClick={() => open('order')}><Plus size={17}/>Создать заказ</button>}</div>}
      <div className="pagination"><span>{total ? `${page * size + 1}–${Math.min((page + 1) * size, total)} из ${total}` : '0 заказов'}</span><div><label>На странице <select aria-label="Заказов на странице" value={size} onChange={event => { setSize(Number(event.target.value)); setPage(0); }}><option>5</option><option>10</option><option>20</option></select></label><button className="icon-button" disabled={page === 0 || loading} onClick={() => setPage(page - 1)} aria-label="Предыдущая страница"><ChevronLeft size={18}/></button><span className="page-number">{page + 1}</span><button className="icon-button" disabled={!data || page + 1 >= data.page.totalPages || loading} onClick={() => setPage(page + 1)} aria-label="Следующая страница"><ChevronRight size={18}/></button></div></div></section>}
      <footer className="footer"><span>gozone<span className="brand-dot">.</span> <span className="footer-caption">Покупки начинаются здесь</span></span><span><span className="live-dot"/> {hasPending || tracked ? 'Проверяем оплату…' : 'Баланс и заказы в одном месте'}</span></footer>
      </main></div>
      {toast && <div className="toast" role="status"><Check size={20}/><span>{toast}</span><button className="icon-button" onClick={() => setToast('')} aria-label="Скрыть уведомление"><X size={16}/></button></div>}
      {modal && <Modal title={{ user: 'Выбрать пользователя', account: 'Открыть счёт', topup: 'Пополнить счёт', order: 'Новый заказ', detail: 'Детали заказа', help: 'Как работает Gozone' }[modal]} close={() => setModal(null)} busy={busy}>
      {modal === 'user' && <form onSubmit={switchUser}><p className="form-description">Введите UUID существующего пользователя или создайте новый. Это переключение профиля для демо, без авторизации.</p><label className="field">UUID пользователя<input required value={userInput} onChange={event => setUserInput(event.target.value)} autoComplete="off" spellCheck="false"/></label><button type="button" className="text-button" onClick={() => setUserInput(crypto.randomUUID())}><RefreshCw size={15}/>Сгенерировать новый UUID</button>{formError && <p className="form-error" role="alert">{formError}</p>}<button className="button primary wide">Выбрать пользователя</button></form>}
      {modal === 'account' && <><p className="form-description">Создадим счёт с нулевым балансом для пользователя <code>{shortId(user)}</code>. Затем его можно будет пополнить.</p>{formError && <p className="form-error" role="alert">{formError}</p>}<button className="button primary wide" disabled={busy} onClick={() => mutate(() => api.account(user), 'Счёт открыт. Можно пополнять баланс.')} >{busy ? 'Открываем…' : 'Открыть счёт'}</button></>}
      {modal === 'topup' && <form onSubmit={event => { event.preventDefault(); let value; try { value = moneyValue(amount); } catch (e) { setFormError(e.message); return; } mutate(() => api.topUp(user, value), 'Счёт успешно пополнен'); }}><p className="form-description">Сумма будет добавлена к текущему балансу. В демо реальные деньги не используются.</p><label className="field">Сумма, ₽<input inputMode="decimal" required value={amount} onChange={event => setAmount(event.target.value)} disabled={busy}/></label><div className="presets">{[500, 1000, 5000].map(value => <button type="button" key={value} className={amount === String(value) ? 'selected' : ''} disabled={busy} onClick={() => setAmount(String(value))}>{money(value)}</button>)}</div>{formError && <p className="form-error" role="alert">{formError}</p>}<button className="button primary wide" disabled={busy}>{busy ? 'Пополняем…' : 'Пополнить счёт'}</button></form>}
      {modal === 'order' && <form onSubmit={submitOrder}><p className="form-description">После создания стоимость автоматически спишется с вашего счёта.</p>{accountExists === false && <p className="form-warning">У вас пока нет счёта. Без счёта заказ будет отменён — сначала откройте и пополните его.</p>}<label className="field">Название<input name="name" required maxLength={200} placeholder="Например, беспроводные наушники" disabled={busy}/></label><label className="field">Стоимость, ₽<input name="amount" required inputMode="decimal" placeholder="0,00" disabled={busy}/></label><label className="field">Описание <span className="optional">необязательно</span><textarea name="description" maxLength={255} rows={3} placeholder="Детали вашего заказа" disabled={busy}/></label>{formError && <p className="form-error" role="alert">{formError}</p>}<button className="button primary wide" disabled={busy}>{busy ? 'Создаём…' : 'Создать и оплатить'}<ArrowRight size={17}/></button></form>}
      {modal === 'detail' && selectedOrder && <div className="order-detail"><Status value={selectedOrder.status}/><h3>{selectedOrder.name}</h3><div className="detail-price">{money(selectedOrder.amount)}</div><p>{selectedOrder.description || 'Описание не указано'}</p><dl><dt>Номер заказа</dt><dd>{selectedOrder.id}</dd><dt>Пользователь</dt><dd>{selectedOrder.userId}</dd></dl>{selectedOrder.status === 'NEW' && <p className="form-warning">Ожидаем результат оплаты. Статус обновится автоматически.</p>}</div>}
      {modal === 'help' && <div className="help-content">{[['01', 'Выберите пользователя', 'UUID связывает ваш счёт и заказы. Сохраните его, чтобы вернуться к тому же профилю.'], ['02', 'Откройте и пополните счёт', 'Укажите сумму пополнения. Она сразу отобразится в доступном балансе.'], ['03', 'Создайте заказ', 'Оплата пройдёт в фоне. Если средств достаточно, статус изменится на «Оплачен».']].map(([number, title, text]) => <div key={number}><span>{number}</span><section><h3>{title}</h3><p>{text}</p></section></div>)}</div>}
      </Modal>}
    </div>;
}
