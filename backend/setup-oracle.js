// One-off setup: create the Oracle Autonomous DB tables (posts, pages,
// settings) from schema-oracle.sql. Run once on the VM after the ADMIN
// password is in the backend .env:
//   node setup-oracle.js
// Safe to re-run: CREATE TABLE is skipped when the table already exists.
require('dotenv').config();
const fs = require('fs');
const path = require('path');
const oracledb = require('oracledb');

async function main() {
  if (!process.env.ORACLE_DB_CONNECT_STRING || !process.env.ORACLE_DB_PASSWORD) {
    console.error('ORACLE_DB_CONNECT_STRING / ORACLE_DB_PASSWORD are not set in .env');
    process.exit(1);
  }
  const sql = fs.readFileSync(path.join(__dirname, 'schema-oracle.sql'), 'utf8');
  // Strip full-line comments first, so the first statement (which follows
  // the header comment) is not mistaken for a comment and skipped.
  const noComments = sql.split('\n').filter((line) => !line.trim().startsWith('--')).join('\n');
  const statements = noComments.split(/;\s*(?:\n|$)/).map((s) => s.trim()).filter((s) => s.length > 0);
  const conn = await oracledb.getConnection({
    user: process.env.ORACLE_DB_USER || 'ADMIN',
    password: process.env.ORACLE_DB_PASSWORD,
    connectString: process.env.ORACLE_DB_CONNECT_STRING,
  });
  for (const stmt of statements) {
    try {
      await conn.execute(stmt);
      console.log('OK:', stmt.split('\n')[0].slice(0, 60));
    } catch (e) {
      if (/ORA-00955/.test(e.message)) {
        console.log('SKIP (already exists):', stmt.split('\n')[0].slice(0, 60));
      } else {
        throw e;
      }
    }
  }
  await conn.commit();
  const tables = await conn.execute(
    "SELECT table_name FROM user_tables WHERE table_name IN ('POSTS','PAGES','SETTINGS') ORDER BY table_name",
    [], { outFormat: oracledb.OUT_FORMAT_OBJECT },
  );
  console.log('Oracle tables present:', tables.rows.map((r) => r.TABLE_NAME).join(', '));
  await conn.close();
  process.exit(0);
}

main().catch((e) => { console.error('setup-oracle failed:', e.message); process.exit(1); });
