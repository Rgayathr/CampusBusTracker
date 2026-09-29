const DEFAULT_ROUTES = [
  {
    route_id: "ROUTE-CAMPUS-A",
    route_name: "Campus Main Shuttle Route",
    stops: [
      { stop_id: "STOP-01", stop_name: "Main Gate Entrance", lat: 12.9716, lng: 77.5946, is_physical_tag: true },
      { stop_id: "STOP-02", stop_name: "CITY-STOP-101: City Market Circle", lat: 12.9650, lng: 77.5770, is_physical_tag: false },
      { stop_id: "STOP-03", stop_name: "Science Block Auditorium", lat: 12.9780, lng: 77.5990, is_physical_tag: true },
      { stop_id: "STOP-04", stop_name: "CITY-STOP-102: Central Railway Junction", lat: 12.9770, lng: 77.5710, is_physical_tag: false },
      { stop_id: "STOP-05", stop_name: "Sports Complex Terminal", lat: 12.9850, lng: 77.6050, is_physical_tag: true }
    ]
  }
];

let activeRoutes = DEFAULT_ROUTES;

module.exports = async function handler(req, res) {
  res.setHeader('Access-Control-Allow-Origin', '*');
  res.setHeader('Access-Control-Allow-Methods', 'GET, POST, OPTIONS');
  res.setHeader('Access-Control-Allow-Headers', 'Content-Type, Authorization');

  if (req.method === 'OPTIONS') return res.status(200).end();

  if (req.method === 'GET') {
    return res.status(200).json({ routes: activeRoutes });
  }

  if (req.method === 'POST') {
    const { route_id, route_name, stops } = req.body || {};
    if (!route_id || !stops) {
      return res.status(400).json({ error: 'route_id and stops array are required' });
    }
    const updated = { route_id, route_name: route_name || 'Custom Route', stops };
    activeRoutes = [updated, ...activeRoutes.filter(r => r.route_id !== route_id)];
    return res.status(200).json({ message: 'Route coordinates saved successfully', route: updated });
  }

  return res.status(405).json({ error: 'Method not allowed' });
};
