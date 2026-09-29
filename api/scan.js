let memoryBuses = {};

function busIdFor(busCode) {
  return `bus_${busCode}`;
}

module.exports = async function handler(req, res) {
  res.setHeader('Access-Control-Allow-Origin', '*');
  res.setHeader('Access-Control-Allow-Methods', 'POST, OPTIONS');
  res.setHeader('Access-Control-Allow-Headers', 'Content-Type, Authorization');

  if (req.method === 'OPTIONS') {
    return res.status(200).end();
  }

  if (req.method !== 'POST') {
    return res.status(405).json({ error: 'Method not allowed. Use POST.' });
  }

  try {
    const { bus_code, qr_code_value } = req.body || {};

    if (!bus_code || !qr_code_value) {
      return res.status(400).json({ error: 'bus_code and qr_code_value are required' });
    }

    const busId = busIdFor(bus_code);
    const record = {
      bus_id: busId,
      bus_code: bus_code,
      checkpoint_name: qr_code_value,
      scanned_at: new Date().toISOString(),
    };

    memoryBuses[busId] = record;

    if (process.env.KV_REST_API_URL) {
      try {
        const { kv } = require('@vercel/kv');
        await kv.hset('buses', { [busId]: JSON.stringify(record) });
      } catch (kvErr) {
        console.warn('Vercel KV write error:', kvErr);
      }
    }

    return res.status(200).json({ message: 'Scan recorded successfully' });
  } catch (err) {
    console.error(err);
    return res.status(500).json({ error: 'Server error while recording scan' });
  }
};
