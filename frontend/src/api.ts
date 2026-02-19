const API_BASE = "http://localhost:8080/api";

export type CreatePixRequest = {
  payerId: string;
  receiverKey: string;
  amount: number;
  description?: string;
  idempotencyKey: string;
};

export type PixResponse = {
  id: string;
  payerId: string;
  receiverKey: string;
  amount: number;
  description?: string;
  status: string;
  fraudStatus: string;
  endToEndId?: string;
};

export async function createPix(req: CreatePixRequest): Promise<PixResponse> {
  const r = await fetch(`${API_BASE}/pix`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(req),
  });
  if (!r.ok) throw new Error(await r.text());
  return r.json();
}

export async function getPix(id: string): Promise<PixResponse> {
  const r = await fetch(`${API_BASE}/pix/${id}`);
  if (!r.ok) throw new Error(await r.text());
  return r.json();
}
