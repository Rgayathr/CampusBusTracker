const bcrypt = require('bcryptjs');
const jwt = require('jsonwebtoken');

const JWT_SECRET = process.env.JWT_SECRET || 'campus_bus_tracker_secret_key_2026';
const usersStore = {};

// Regex for legitimate email syntax
const EMAIL_REGEX = /^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$/;

// Strong Password Regex: Min 8 chars, 1 uppercase, 1 lowercase, 1 digit, 1 special symbol
const PASSWORD_REGEX = /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[!@#$%^&*()_+\-=\[\]{};':"\\|,.<>\/?]).{8,}$/;

// Institutional domains that bypass manual admin approval
const INSTITUTIONAL_DOMAINS = ['college.edu', 'campus.ac.in', 'university.edu'];

module.exports = async function handler(req, res) {
  res.setHeader('Access-Control-Allow-Origin', '*');
  res.setHeader('Access-Control-Allow-Methods', 'POST, OPTIONS');
  res.setHeader('Access-Control-Allow-Headers', 'Content-Type, Authorization, X-CSRF-Token');

  if (req.method === 'OPTIONS') return res.status(200).end();
  if (req.method !== 'POST') return res.status(405).json({ error: 'Method not allowed. Use POST.' });

  try {
    const { name, email, password, role } = req.body || {};

    if (!email || !password || !name) {
      return res.status(400).json({ error: 'Full name, email, and password are required.' });
    }

    const normalizedEmail = email.trim().toLowerCase();

    // 1. Strict Email Format Validation
    if (!EMAIL_REGEX.test(normalizedEmail)) {
      return res.status(400).json({ error: 'Invalid email format. Please provide a legitimate email address.' });
    }

    // 2. Strong Password Policy Enforcement
    if (!PASSWORD_REGEX.test(password)) {
      return res.status(400).json({
        error: 'Password must be at least 8 characters long and include an uppercase letter, lowercase letter, number, and special character (!@#$%^&*).'
      });
    }

    // 3. User Uniqueness Check
    if (usersStore[normalizedEmail]) {
      return res.status(400).json({ error: 'An account with this email address already exists.' });
    }

    // 4. Role Validation & Institutional Verification
    const validRoles = ['student', 'parent', 'driver', 'management'];
    const userRole = validRoles.includes(role?.toLowerCase()) ? role.toLowerCase() : 'student';

    const domain = normalizedEmail.split('@')[1];
    const isInstitutional = INSTITUTIONAL_DOMAINS.some(d => domain?.endsWith(d));
    const isVerified = isInstitutional || userRole === 'management';

    // 5. Password Hashing (Bcrypt 10 salt rounds)
    const hashedPassword = await bcrypt.hash(password, 10);
    const userId = `usr_${Date.now()}`;

    const user = {
      user_id: userId,
      name: name.trim(),
      email: normalizedEmail,
      password_hash: hashedPassword,
      role: userRole,
      is_verified: isVerified,
      is_active: true,
      created_at: new Date().toISOString(),
      updated_at: new Date().toISOString()
    };

    usersStore[normalizedEmail] = user;

    // 6. Signed JWT Generation
    const token = jwt.sign(
      { userId: user.user_id, email: user.email, role: user.role, isVerified: user.is_verified, name: user.name },
      JWT_SECRET,
      { expiresIn: '30d' }
    );

    return res.status(200).json({
      token,
      user_id: user.user_id,
      role: user.role,
      name: user.name,
      email: user.email,
      is_verified: user.is_verified,
      message: isVerified
        ? 'Account registered successfully.'
        : 'Account created! Pending institutional domain verification or admin approval.'
    });
  } catch (err) {
    console.error('Signup error:', err);
    return res.status(500).json({ error: 'Internal server error during account registration.' });
  }
};