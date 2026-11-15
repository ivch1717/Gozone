import React, { useEffect, useRef } from 'react';
import { X } from 'lucide-react';

export default function Modal({ title, children, close, busy }) {
  const ref = useRef(null);
  useEffect(() => {
    const dialog = ref.current;
    const previous = document.activeElement;
    dialog.showModal();
    return () => { dialog.close(); previous?.focus(); };
  }, []);
  return <dialog ref={ref} onCancel={event => { event.preventDefault(); if (!busy) close(); }} onClick={event => { if (event.target === ref.current && !busy) close(); }} aria-labelledby="dialog-title">
    <div className="modal-inner"><div className="modal-heading"><h2 id="dialog-title">{title}</h2><button className="icon-button" aria-label="Закрыть" disabled={busy} onClick={close}><X size={20}/></button></div>{children}</div>
  </dialog>;
}
