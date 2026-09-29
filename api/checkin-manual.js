// Helper: Calculate Haversine distance in meters between two lat/lng pairs
function calculateHaversineDistanceMeters(lat1, lon1, lat2, lon2) {
  const R = 6371000; // Earth radius in meters
  const dLat = (lat2 - lat1) * Math.PI / 180;
  const dLon = (lon2 - lon1) * Math.PI / 180;
  const a =
    Math.sin(dLat / 2) * Math.sin(dLat / 2) +
    Math.cos(lat1 * Math.PI / 180) * Math.cos(lat2 * Math.PI / 180) *
    Math.sin(dLon / 2) * Math.sin(dLon / 2);
  const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
  return R * c;
}

const STOP_COORDINATES = {
  "Main Gate Entrance": { lat: 12.9716, lng: 77.5946 },
  "CITY-STOP-101: City Market Circle": { lat: 12.9650, lng: 77.5770 },
  "Science Block Auditorium": { lat: 12.9780, lng: 77.5990 },
  "CITY-STOP-102: Central Railway Junction": { lat: 12.9770, lng: 77.5710 },
  "Sports Complex Terminal": { lat: 12.9850, lng: 77.6050 }
};

const busLastCheckinTimes = {};

module.exports = async function handler(req, res) {
  res.setHeader('Access-Control-Allow-Origin', '*');
  res.setHeader('Access-Control-Allow-Methods', 'POST, OPTIONS');
  res.setHeader('Access-Control-Allow-Headers', 'Content-Type, Authorization');

  if (req.method === 'OPTIONS') return res.status(200).end();
  if (req.method !== 'POST') return res.status(405).json({ error: 'Method not allowed' });

  try {
    const { bus_code, checkpoint_name, action, lat, lng } = req.body || {};

    if (!bus_code || !checkpoint_name) {
      return res.status(400).json({ error: 'bus_code and checkpoint_name are required' });
    }

    const now = Date.now();
    const lastCheckin = busLastCheckinTimes[bus_code];

    // Timestamp & Sequence Validation: prevent rapid duplicate check-ins (< 5 seconds)
    if (lastCheckin && (now - lastCheckin.timestamp < 5000)) {
      return res.status(400).json({ error: 'Check-in submitted too quickly. Please wait a moment.' });
    }

    // Geofence Radius Validation (if lat & lng provided by mobile device)
    let geofenceVerified = true;
    let distanceMeters = 0;

    const stopCoords = STOP_COORDINATES[checkpoint_name];
    if (stopCoords && lat && lng) {
      distanceMeters = calculateHaversineDistanceMeters(lat, lng, stopCoords.lat, stopCoords.lng);
      // Radius limit: 2000 meters (2km) for software check-in verification
      if (distanceMeters > 2000) {
        geofenceVerified = false;
      }
    }

    busLastCheckinTimes[bus_code] = {
      checkpoint_name,
      action: action || 'ARRIVED',
      timestamp: now,
      lat,
      lng,
      geofenceVerified
    };

    return res.status(200).json({
      message: `Manual Check-in (${action || 'ARRIVED'}) recorded successfully`,
      bus_code,
      checkpoint_name,
      action: action || 'ARRIVED',
      geofence_verified: geofenceVerified,
      distance_meters: Math.round(distanceMeters),
      scanned_at: new Date().toISOString()
    });
  } catch (err) {
    console.error('Manual check-in error:', err);
    return res.status(500).json({ error: 'Server error processing manual check-in' });
  }
};
