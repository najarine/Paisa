/**
 * Paisa - Personal Financial Operating System
 * Support: najarine@gmail.com
 */

const admin = require("firebase-admin");

if (!admin.apps.length) {
  admin.initializeApp();
}
