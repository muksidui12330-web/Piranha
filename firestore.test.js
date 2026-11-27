const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ADMIN_UID = "admin_uid_1";
const ADMIN_EMAIL = "muksidui12330@gmail.com";
const SECOND_ADMIN_UID = "admin_uid_2";
const SECOND_ADMIN_EMAIL = "coowner@gmail.com";
const PACKER_UID = "packer_uid_1";
const PACKER_EMAIL = "packer1@gmail.com";
const ALICE_UID = "alice_uid_1";
const ALICE_EMAIL = "alice@gmail.com";
const BOB_UID = "bob_uid_1";
const BOB_EMAIL = "bob@gmail.com";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function pastDate(msOffset = 2000) {
  return new Date(Date.now() - msOffset);
}

function adminContext(uid = ADMIN_UID, email = ADMIN_EMAIL, verified = true) {
  return testEnv.authenticatedContext(uid, { email, email_verified: verified });
}

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
  if (testEnv) await testEnv.cleanup();
});

beforeEach(async () => {
  if (testEnv) await testEnv.clearFirestore();
});

test("Unauthenticated users are denied across all collections", async () => {
  const unauth = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauth.collection("config").doc("shop").get());
  await assertFails(unauth.collection("items").get());
  await assertFails(unauth.collection("users").doc(ALICE_UID).get());
  await assertFails(unauth.collection("orders").where("customerId", "==", ALICE_UID).get());
  await assertFails(unauth.collection("staff").doc(PACKER_EMAIL).get());
});

test("Email spoofing attack (email_verified: false) cannot access Admin operations", async () => {
  const spoofAdmin = adminContext("spoof_uid", ADMIN_EMAIL, false).firestore();
  await assertFails(
    spoofAdmin.collection("config").doc("shop").set({
      shopName: "Gramin Kirana",
      bannerText: "Delivery in 30 minutes",
      locationText: "Nagaon, Assam",
      openingHours: "7:00 AM - 9:00 PM",
      deliveryCharge: 20,
      minimumOrderAmount: 200,
      deliveryMinutes: 30,
      deliveryTimeText: "Delivery in 30 minutes",
      upiId: "shop@upi",
      updatedAt: pastDate(),
    })
  );
});

test("Admin manages shop config, staff roles (second Admin + Packer), and catalog items", async () => {
  const adminDb = adminContext().firestore();

  await assertSucceeds(
    adminDb.collection("config").doc("shop").set({
      shopName: "Gramin Kirana",
      bannerText: "Delivery in 30 minutes",
      bannerTextBn: "৩০ মিনিটে ডেলিভারি",
      locationText: "Nagaon Village Market, Assam",
      latitude: 26.3464,
      longitude: 92.684,
      openingHours: "7:00 AM - 9:00 PM",
      deliveryCharge: 20,
      minimumOrderAmount: 200,
      deliveryMinutes: 30,
      deliveryTimeText: "Delivery in 30 minutes",
      deliveryTimeTextBn: "৩০ মিনিটে ডেলিভারি",
      upiId: "graminkirana@upi",
      updatedAt: pastDate(),
    })
  );

  // Add second Admin & Packer
  await assertSucceeds(
    adminDb.collection("staff").doc(SECOND_ADMIN_EMAIL).set({
      email: SECOND_ADMIN_EMAIL,
      role: "ADMIN",
      name: "Co-Owner",
      createdAt: pastDate(),
      updatedAt: pastDate(),
    })
  );
  await assertSucceeds(
    adminDb.collection("staff").doc(PACKER_EMAIL).set({
      email: PACKER_EMAIL,
      role: "PACKER",
      name: "Raju",
      createdAt: pastDate(),
      updatedAt: pastDate(),
    })
  );

  // Second admin can create catalog item
  const secondAdminDb = adminContext(SECOND_ADMIN_UID, SECOND_ADMIN_EMAIL, true).firestore();
  await assertSucceeds(
    secondAdminDb.collection("items").doc("item_rice_1").set({
      nameEn: "Assam Joha Rice",
      nameBn: "আসাম জোহা চাল",
      category: "GRAINS",
      price: 85,
      unit: "1 kg",
      available: true,
      imageKey: "img_rice_grains",
      similarItemIds: [],
      createdAt: pastDate(),
      updatedAt: pastDate(),
    })
  );

  // Customer cannot create or edit catalog items
  const aliceDb = adminContext(ALICE_UID, ALICE_EMAIL, true).firestore();
  await assertSucceeds(aliceDb.collection("items").get());
  await assertFails(
    aliceDb.collection("items").doc("item_rice_1").update({ price: 1, updatedAt: pastDate(500) })
  );
});

test("Customer profile PII isolation, rating tamper prevention, and self-cancel penalty", async () => {
  const aliceDb = adminContext(ALICE_UID, ALICE_EMAIL, true).firestore();
  const bobDb = adminContext(BOB_UID, BOB_EMAIL, true).firestore();

  // Cannot create profile with tampered cash/rating
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).set({
      uid: ALICE_UID,
      email: ALICE_EMAIL,
      name: "Alice",
      mobile: "9876543210",
      address: "Ward 2, Assam",
      rating: 5,
      successfulDeliveredCount: 100,
      cashEnabled: true,
      cashAdminOverride: false,
      profileCompleted: true,
      createdAt: pastDate(),
      updatedAt: pastDate(),
    })
  );

  // Valid customer profile create
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      uid: ALICE_UID,
      email: ALICE_EMAIL,
      name: "Alice Devi",
      mobile: "9876543210",
      address: "Ward 2, Village Road, Assam",
      latitude: 26.35,
      longitude: 92.69,
      rating: 5,
      successfulDeliveredCount: 0,
      cashEnabled: true,
      cashAdminOverride: false,
      profileCompleted: true,
      createdAt: pastDate(),
      updatedAt: pastDate(),
    })
  );

  // Cross-user PII read/list denied
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
  await assertFails(bobDb.collection("users").get());

  // Customer cannot self-raise rating or toggle cashAdminOverride
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      cashAdminOverride: true,
      updatedAt: pastDate(500),
    })
  );

  // Customer self-cancellation penalty: 5 -> 4
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      rating: 4,
      cashEnabled: true,
      updatedAt: pastDate(500),
    })
  );
});

test("Order lifecycle: UPI/Cash rules, 1-star cash block, Packer Ready, Admin Confirm & Deliver", async () => {
  const adminDb = adminContext().firestore();
  const packerDb = adminContext(PACKER_UID, PACKER_EMAIL, true).firestore();
  const aliceDb = adminContext(ALICE_UID, ALICE_EMAIL, true).firestore();
  const bobDb = adminContext(BOB_UID, BOB_EMAIL, true).firestore();

  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    const db = ctx.firestore();
    await db.collection("staff").doc(PACKER_EMAIL).set({
      email: PACKER_EMAIL,
      role: "PACKER",
      name: "Raju",
      createdAt: pastDate(5000),
      updatedAt: pastDate(5000),
    });
    await db.collection("users").doc(ALICE_UID).set({
      uid: ALICE_UID,
      email: ALICE_EMAIL,
      name: "Alice Devi",
      mobile: "9876543210",
      address: "Ward 2, Assam",
      rating: 5,
      successfulDeliveredCount: 0,
      cashEnabled: true,
      cashAdminOverride: false,
      profileCompleted: true,
      createdAt: pastDate(5000),
      updatedAt: pastDate(5000),
    });
    await db.collection("users").doc(BOB_UID).set({
      uid: BOB_UID,
      email: BOB_EMAIL,
      name: "Bob Das",
      mobile: "9876500000",
      address: "Ward 4, Assam",
      rating: 1,
      successfulDeliveredCount: 0,
      cashEnabled: false,
      cashAdminOverride: false,
      profileCompleted: true,
      createdAt: pastDate(5000),
      updatedAt: pastDate(5000),
    });
  });

  // 1-star customer (Bob) CANNOT place Cash order, CAN place UPI order
  const baseOrder = {
    customerName: "Bob Das",
    customerMobile: "9876500000",
    customerAddress: "Ward 4, Assam",
    items: [{ itemId: "i1", nameEn: "Rice", nameBn: "চাল", unit: "5 kg", price: 300, quantity: 1 }],
    itemsTotal: 300,
    deliveryCharge: 20,
    grandTotal: 320,
    cashReceived: false,
    adminAcknowledged: false,
    createdAt: pastDate(),
    updatedAt: pastDate(),
  };

  await assertFails(
    bobDb.collection("orders").doc("order_bob_cash").set({
      ...baseOrder,
      customerId: BOB_UID,
      paymentMethod: "CASH",
      status: "PLACED",
    })
  );

  await assertSucceeds(
    bobDb.collection("orders").doc("order_bob_upi").set({
      ...baseOrder,
      customerId: BOB_UID,
      paymentMethod: "UPI",
      upiTransactionId: "UPI123456789",
      status: "WAITING_PAYMENT_CONFIRMATION",
    })
  );

  // Alice (5 stars) CAN place Cash order
  await assertSucceeds(
    aliceDb.collection("orders").doc("order_alice_cash").set({
      ...baseOrder,
      customerId: ALICE_UID,
      customerName: "Alice Devi",
      paymentMethod: "CASH",
      status: "PLACED",
    })
  );

  // Alice query with whereEqualTo succeeds; unfiltered query fails; reading Bob's order fails
  await assertSucceeds(aliceDb.collection("orders").where("customerId", "==", ALICE_UID).get());
  await assertFails(aliceDb.collection("orders").get());
  await assertFails(aliceDb.collection("orders").doc("order_bob_upi").get());

  // Packer CANNOT confirm Bob's UPI payment
  await assertFails(
    packerDb.collection("orders").doc("order_bob_upi").update({
      status: "PACKING",
      updatedAt: pastDate(500),
    })
  );

  // Admin confirms Bob's UPI payment -> PACKING + confirmedAt
  await assertSucceeds(
    adminDb.collection("orders").doc("order_bob_upi").update({
      status: "PACKING",
      adminAcknowledged: true,
      confirmedAt: pastDate(500),
      updatedAt: pastDate(500),
    })
  );

  // Packer marks PACKING -> READY
  await assertSucceeds(
    packerDb.collection("orders").doc("order_bob_upi").update({
      status: "READY",
      updatedAt: pastDate(300),
    })
  );

  // Admin marks DELIVERED
  await assertSucceeds(
    adminDb.collection("orders").doc("order_bob_upi").update({
      status: "DELIVERED",
      updatedAt: pastDate(100),
    })
  );
});
