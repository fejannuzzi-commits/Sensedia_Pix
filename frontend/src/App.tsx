import { useState } from "react";
import { createPix, getPix, PixResponse } from "./api";

export default function App() {
  const [payerId, setPayerId] = useState("123");
  const [receiverKey, setReceiverKey] = useState("email@pix.com");
  const [amount, setAmount] = useState(10);
  const [description, setDescription] = useState("Pedido #1001");
  const [idempotencyKey, setIdempotencyKey] = useState(() => crypto.randomUUID());
  const [pix, setPix] = useState<PixResponse | null>(null);
  const [error, setError] = useState<string>("");

  async function onCreate() {
    setError("");
    try {
      const res = await createPix({ payerId, receiverKey, amount, description, idempotencyKey });
      setPix(res);
    } catch (e: any) {
      setError(e.message ?? String(e));
    }
  }

  async function onRefresh() {
    if (!pix) return;
    setError("");
    try {
      const res = await getPix(pix.id);
      setPix(res);
    } catch (e: any) {
      setError(e.message ?? String(e));
    }
  }

  return (
    <div style={{ fontFamily: "system-ui", padding: 24, maxWidth: 820, margin: "0 auto" }}>
      <h2>PIX Demo</h2>

      <div style={{ display: "grid", gap: 10 }}>
        <label>
          PayerId
          <input value={payerId} onChange={(e) => setPayerId(e.target.value)} style={{ width: "100%" }} />
        </label>

        <label>
          Receiver Key
          <input value={receiverKey} onChange={(e) => setReceiverKey(e.target.value)} style={{ width: "100%" }} />
        </label>

        <label>
          Amount
          <input type="number" value={amount} onChange={(e) => setAmount(Number(e.target.value))} style={{ width: "100%" }} />
        </label>

        <label>
          Description
          <input value={description} onChange={(e) => setDescription(e.target.value)} style={{ width: "100%" }} />
        </label>

        <label>
          Idempotency Key
          <input value={idempotencyKey} onChange={(e) => setIdempotencyKey(e.target.value)} style={{ width: "100%" }} />
        </label>

        <div style={{ display: "flex", gap: 12, flexWrap: "wrap" }}>
          <button onClick={onCreate}>Criar PIX</button>
          <button onClick={() => setIdempotencyKey(crypto.randomUUID())}>Gerar nova idempotencyKey</button>
          <button onClick={onRefresh} disabled={!pix}>Atualizar status</button>
        </div>

        {error && <pre style={{ color: "crimson", whiteSpace: "pre-wrap" }}>{error}</pre>}

        {pix && (
          <div style={{ border: "1px solid #ddd", borderRadius: 8, padding: 12 }}>
            <div><b>ID:</b> {pix.id}</div>
            <div><b>Status:</b> {pix.status}</div>
            <div><b>Fraud:</b> {pix.fraudStatus}</div>
            <div><b>EndToEndId:</b> {pix.endToEndId ?? "-"}</div>
          </div>
        )}
      </div>
    </div>
  );
}
