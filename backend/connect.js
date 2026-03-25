require("dotenv").config();
const knex = require("knex");

const dbUrl = process.env.DATABASE_URL;
if (!dbUrl) {
  console.error("DATABASE_URL not set. See .env.example");
  process.exit(1);
}

const db = knex({
  client: "pg",
  connection: dbUrl,
  pool: { min: 0, max: 7 }
});

(async () => {
  try {
    await db.raw("select 1+1 as result");
    console.log("Database connection successful");
  } catch (err) {
    console.error("Database connection failed:", err.message || err);
    process.exit(1);
  } finally {
    await db.destroy();
  }
})();
