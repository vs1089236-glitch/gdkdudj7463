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
const ADMIN_UID = "admin_999";

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

test("Unauthenticated users cannot read or write any collection", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(unauthDb.collection("transactions").get());
  await assertFails(unauthDb.collection("payoutRequests").get());
  await assertFails(
    unauthDb.collection("transactions").doc("tx_1").set({
      userId: ALICE_UID,
      userName: "Alice",
      type: "COMMISSION",
      title: "Unauthorized",
      amountPaise: 10000,
      commissionRateBps: 500,
      status: "PENDING",
      createdAt: new Date(Date.now() - 1000),
      updatedAt: new Date(Date.now() - 1000),
    })
  );
});

test("User profile creation, update, and cross-user isolation", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID, { email: "alice@example.com", email_verified: true }).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID, { email: "bob@example.com", email_verified: true }).firestore();
  const now = new Date(Date.now() - 5000);

  // Valid create
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Alice Sharma",
      email: "alice@example.com",
      referralCode: "HOYOALICE",
      kycVerified: false,
      biometricEnabled: false,
      createdAt: now,
      updatedAt: now,
    })
  );

  // Cannot self-verify KYC on create or update
  await assertFails(
    bobDb.collection("users").doc(BOB_UID).set({
      userId: BOB_UID,
      displayName: "Bob",
      referralCode: "HOYOBOB1",
      kycVerified: true,
      biometricEnabled: false,
      createdAt: now,
      updatedAt: now,
    })
  );
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      kycVerified: true,
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Valid partial update
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      phone: "+919876543210",
      biometricEnabled: true,
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Cross-user read/list/update blocked
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
  await assertFails(bobDb.collection("users").get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).update({ displayName: "Hacked" }));
});

test("Payout methods subcollection isolation and schema bounds", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const now = new Date(Date.now() - 5000);

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("payoutMethods").doc("pm_1").set({
      userId: ALICE_UID,
      methodType: "UPI",
      label: "Primary UPI",
      upiId: "alice@okaxis",
      accountHolderName: "Alice Sharma",
      primaryMethod: true,
      createdAt: now,
      updatedAt: now,
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("payoutMethods").get()
  );

  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("payoutMethods").get()
  );
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("payoutMethods").doc("pm_1").get()
  );
});

test("Transactions enforce owner queries, pending creation, and admin-only verification", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID, { email: "alice@example.com", email_verified: true }).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID, { email: "bob@example.com", email_verified: true }).firestore();
  const adminDb = testEnv.authenticatedContext(ADMIN_UID, { email: "vs1089236@gmail.com", email_verified: true }).firestore();
  const spoofedAdminDb = testEnv.authenticatedContext(BOB_UID, { email: "vs1089236@gmail.com", email_verified: false }).firestore();
  const now = new Date(Date.now() - 5000);

  // Owner creates PENDING transaction (no @DocumentId `id` key)
  await assertSucceeds(
    aliceDb.collection("transactions").doc("tx_1").set({
      userId: ALICE_UID,
      userName: "Alice Sharma",
      type: "COMMISSION",
      title: "SaaS Partner Referral",
      clientOrOrderRef: "ORD-102",
      amountPaise: 45000,
      commissionRateBps: 1000,
      status: "PENDING",
      notes: "Client onboarding complete",
      createdAt: now,
      updatedAt: now,
    })
  );

  // Reject creating pre-verified transaction or extra id key or future timestamp
  await assertFails(
    aliceDb.collection("transactions").doc("tx_2").set({
      userId: ALICE_UID,
      userName: "Alice Sharma",
      type: "COMMISSION",
      title: "Self Verified",
      amountPaise: 45000,
      commissionRateBps: 1000,
      status: "VERIFIED",
      createdAt: now,
      updatedAt: now,
    })
  );
  await assertFails(
    aliceDb.collection("transactions").doc("tx_3").set({
      id: "tx_3",
      userId: ALICE_UID,
      userName: "Alice Sharma",
      type: "COMMISSION",
      title: "Ghost ID",
      amountPaise: 45000,
      commissionRateBps: 1000,
      status: "PENDING",
      createdAt: now,
      updatedAt: now,
    })
  );

  // Filtered query succeeds; unfiltered query fails for normal users
  await assertSucceeds(aliceDb.collection("transactions").where("userId", "==", ALICE_UID).get());
  await assertFails(aliceDb.collection("transactions").get());
  await assertFails(bobDb.collection("transactions").where("userId", "==", ALICE_UID).get());

  // Spoofed unverified admin email fails
  await assertFails(spoofedAdminDb.collection("transactions").get());
  await assertFails(
    spoofedAdminDb.collection("transactions").doc("tx_1").update({
      status: "VERIFIED",
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Verified admin can list all and verify transaction
  await assertSucceeds(adminDb.collection("transactions").get());
  await assertSucceeds(
    adminDb.collection("transactions").doc("tx_1").update({
      status: "VERIFIED",
      verifiedBy: "vs1089236@gmail.com",
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Owner cannot edit VERIFIED transaction (terminal state lock)
  await assertFails(
    aliceDb.collection("transactions").doc("tx_1").update({
      title: "Edited after verification",
      updatedAt: new Date(Date.now() - 500),
    })
  );
});

test("Payout requests enforce minimum amount, owner queries, and admin processing", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const adminDb = testEnv.authenticatedContext(ADMIN_UID, { email: "vs1089236@gmail.com", email_verified: true }).firestore();
  const now = new Date(Date.now() - 5000);

  // Minimum ₹100.00 (10000 paise)
  await assertFails(
    aliceDb.collection("payoutRequests").doc("pay_low").set({
      userId: ALICE_UID,
      userName: "Alice",
      amountPaise: 5000,
      methodType: "UPI",
      destinationDisplay: "alice@okaxis",
      status: "PENDING",
      createdAt: now,
      updatedAt: now,
    })
  );

  await assertSucceeds(
    aliceDb.collection("payoutRequests").doc("pay_1").set({
      userId: ALICE_UID,
      userName: "Alice",
      amountPaise: 25000,
      methodType: "UPI",
      destinationDisplay: "alice@okaxis",
      status: "PENDING",
      createdAt: now,
      updatedAt: now,
    })
  );

  await assertSucceeds(aliceDb.collection("payoutRequests").where("userId", "==", ALICE_UID).get());
  await assertFails(aliceDb.collection("payoutRequests").get());

  await assertSucceeds(
    adminDb.collection("payoutRequests").doc("pay_1").update({
      status: "COMPLETED",
      referenceUtr: "UTR99887766",
      adminNote: "Settled via NEFT/UPI",
      updatedAt: new Date(Date.now() - 1000),
    })
  );
});
