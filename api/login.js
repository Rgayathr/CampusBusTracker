const bcrypt = require('bcryptjs');
const jwt = require('jsonwebtoken');

const JWT_SECRET = process.env.JWT_SECRET || 'campus_bus_tracker_secret_key_2026';
const loginAttempts = {};

const EMAIL_REGEX = /^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$/;

module.exports = async function handler(req, res) {
  res.setHeader('Access-Control-Allow-Origin', '*');
  res.setHeader('Access-Control-Allow-Methods', 'POST, OPTIONS');
  res.setHeader('Access-Control-Allow-Headers', 'Content-Type, Authorization, X-CSRF-Token');

  if (req.method === 'OPTIONS') return res.status(200).end();
  if (req.method !== 'POST') return res.status(405).json({ error: 'Method not allowed. Use POST.' });

  try {
    const ip = req.headers['x-forwarded-for'] || req.socket.remoteAddress || 'ip_unknown';
    const now = Date.now();

    // 1. IP-Based Rate Limiting (Max 5 attempts per 60 seconds)
    if (!loginAttempts[ip]) {
      loginAttempts[ip] = { count: 1, firstAttempt: now };
    } else {
      if (now - loginAttempts[ip].firstAttempt > 60000) {
        loginAttempts[ip] = { count: 1, firstAttempt: now };
      } else {
        loginAttempts[ip].count += 1;
        if (loginAttempts[ip].count > 5) {
          return res.status(429).json({ error: 'Too many failed login attempts. Rate limit exceeded. Please try again in 60 seconds.' });
        }
      }
    }

    const { email, password } = req.body || {};
    if (!email || !password) {
      return res.status(400).json({ error: 'Email address and password are required.' });
    }

    const normalizedEmail = email.trim().toLowerCase();

    if (!EMAIL_REGEX.test(normalizedEmail)) {
      return res.status(400).json({ error: 'Invalid email format.' });
    }

    // Role determination based on institutional email keywords
    let role = 'student';
    if (normalizedEmail.includes('driver')) role = 'driver';
    else if (normalizedEmail.includes('admin') || normalizedEmail.includes('management')) role = 'management';
    else if (normalizedEmail.includes('parent')) role = 'parent';

    const userName = normalizedEmail.split('@')[0]
      .replace('.', ' ')
      .split(' ')
      .map(str => str.charAt(0).toUpperCase() + str.slice(1))
      .join(' ');

    const token = jwt.sign(
      { userId: `usr_${normalizedEmail}`, email: normalizedEmail, role, isVerified: true, name: userName },
      JWT_SECRET,
      { expiresIn: '30d' }
    );

    return res.status(200).json({
      token,
      user_id: `usr_${normalizedEmail}`,
      role,
      name: userName,
      email: normalizedEmail,
      is_verified: true,
      message: 'Authentication successful'
    });
  } catch (err) {
    console.error('Login error:', err);
    return res.status(500).json({ error: 'Internal server error during authentication.' });
  }
};