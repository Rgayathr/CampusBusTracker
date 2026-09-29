let memoryBuses = {};

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
          buses = Object.values(all).map((v) => (typeof v === 'string' ? JSON.parse(v) : v));
        }
      } catch (kvErr) {
        console.warn('Vercel KV read error:', kvErr);
      }
    }

    return res.status(200).json({ buses });
  } catch (err) {
    console.error(err);
    return res.status(500).json({ error: 'Server error while fetching status' });
  }
};
