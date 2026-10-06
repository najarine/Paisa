const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

test("Unauthenticated user: cannot read user financial records", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("wallets").get());
});

test("Unauthenticated user: cannot write user financial records", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).set({ uid: ALICE_UID }));
});

test("Authenticated owner: can create and read their own wallet", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const walletDoc = aliceDb.collection("users").doc(ALICE_UID).collection("wallets").doc("wallet_1");
  await assertSucceeds(walletDoc.set({
    id: "wallet_1",
    name: "Cash Wallet",
    type: "CASH",
    balance: 5000.0
  }));
  await assertSucceeds(walletDoc.get());
});

test("Cross-user access: Bob cannot read Alice's personal wallet", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("users").doc(ALICE_UID).collection("wallets").doc("alice_wallet").set({
      id: "alice_wallet",
      name: "Alice Secret Fund",
      balance: 100000.0
    });
  });

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("wallets").doc("alice_wallet").get());
});

test("Cross-user access: Bob cannot write to Alice's transactions", async () => {
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const txDoc = bobDb.collection("users").doc(ALICE_UID).collection("transactions").doc("tx_attack");
  await assertFails(txDoc.set({
    amount: 1000.0,
    type: "EXPENSE"
  }));
});

test("Default deny: unmapped collection cannot be accessed", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(aliceDb.collection("unmapped_admin_secrets").doc("secret_doc").get());
});
