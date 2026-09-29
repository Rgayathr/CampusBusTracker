const ROUTE_POLYLINES = [
  {
    route_id: "ROUTE-CAMPUS-A",
    route_name: "Campus & City Express Line",
    bus_code: "BUS-01 (Express)",
    polyline_points: [
      { stop_id: "STOP-01", name: "Main Gate Entrance", lat: 12.9716, lng: 77.5946, is_physical_tag: true, status: "ACTIVE" },
      { stop_id: "STOP-02", name: "CITY-STOP-101: City Market Circle", lat: 12.9650, lng: 77.5770, is_physical_tag: false, status: "ACTIVE" },
      { stop_id: "STOP-03", name: "Science Block Auditorium", lat: 12.9780, lng: 77.5990, is_physical_tag: true, status: "ACTIVE" },
      { stop_id: "STOP-04", name: "CITY-STOP-102: Central Railway Junction", lat: 12.9770, lng: 77.5710, is_physical_tag: false, status: "ACTIVE" },
      { stop_id: "STOP-05", name: "Sports Complex Terminal", lat: 12.9850, lng: 77.6050, is_physical_tag: true, status: "ACTIVE" }
    ],
    current_estimated_position: {
      lat: 12.9780,
      lng: 77.5990,
      last_checkpoint: "Science Block Auditorium",
      next_checkpoint: "CITY-STOP-102: Central Railway Junction",
      estimated_eta_minutes: 4,
      traffic_status: "NORMAL"
    }
  }
];

module.exports = async function handler(req, res) {
  res.setHeader('Access-Control-Allow-Origin', '*');
  res.setHeader('Access-Control-Allow-Methods', 'GET, OPTIONS');
  res.setHeader('Access-Control-Allow-Headers', 'Content-Type, Authorization');

  if (req.method === 'OPTIONS') return res.status(200).end();
  if (req.method !== 'GET') return res.status(405).json({ error: 'Method not allowed. Use GET.' });

  try {
    return res.status(200).json({
      routes: ROUTE_POLYLINES,
      timestamp: new Date().toISOString()
    });
  } catch (err) {
    console.error('Map data error:', err);
    return res.status(500).json({ error: 'Server error retrieving map data' });
  }
};
