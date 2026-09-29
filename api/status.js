const { kv } = require('@vercel/kv');

const INITIAL_DEMO_BUSES = [
  { bus_id: "bus_BUS-01", bus_code: "BUS-01 (Express)", checkpoint_name: "Science Block Auditorium", scanned_at: new Date(Date.now() - 120000).toISOString() },
  { bus_id: "bus_BUS-02", bus_code: "BUS-02 (Campus Line)", checkpoint_name: "CITY-STOP-101: City Market Circle", scanned_at: new Date(Date.now() - 300000).toISOString() },
  { bus_id: "bus_BUS-03", bus_code: "BUS-03 (City Connect)", checkpoint_name: "Main Gate Entrance", scanned_at: new Date(Date.now() - 450000).toISOString() }
];

let memoryBuses = {};
INITIAL_DEMO_BUSES.forEach(b => { memoryBuses[b.bus_id] = b; });

module.exports = async function handler(req, res) {
  res.setHeader('Access-Control-Allow-Origin', '*');
  res.setHeader('Access-Control-Allow-Methods', 'GET, OPTIONS');
  res.setHeader('Access-Control-Allow-Headers', 'Content-Type');

  if (req.method === 'OPTIONS') {
    return res.status(200).end();
  }

  if (req.method !== 'GET') {
    return res.status(405).json({ error: 'Method not allowed. Use GET.' });
  }

  try {
    let buses = Object.values(memoryBuses);
    if (process.env.KV_REST_API_URL) {
      try {
        const { kv } = require('@vercel/kv');
        const all = await kv.hgetall('buses');
        if (all) {
          const fetched = Object.values(all).map((v) => (typeof v === 'string' ? JSON.parse(v) : v));
          if (fetched.length > 0) buses = fetched;
        }
      } catch (kvErr) {
        console.warn('Vercel KV read error:', kvErr);
      }
    }

    return res.status(200).json({ buses });
  } catch (err) {
    console.error(err);
    return res.status(200).json({ buses: INITIAL_DEMO_BUSES });
  }
};
