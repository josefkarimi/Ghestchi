const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_uid_123";
const BOB_UID = "bob_uid_456";

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

function pastDate(msAgo = 2000) {
  return new Date(Date.now() - msAgo);
}

// --- 1. Unauthenticated access must fail everywhere ---
test("Unauthenticated user: denied read/write across all collections", async () => {
  const unauth = testEnv.unauthenticatedContext().firestore();
  const now = pastDate();

  await assertFails(unauth.collection("users").doc(ALICE_UID).get());
  await assertFails(unauth.collection("users").doc(ALICE_UID).collection("loans").get());
  await assertFails(unauth.collection("users").doc(ALICE_UID).collection("accounts").get());
  await assertFails(unauth.collection("users").doc(ALICE_UID).collection("categories").get());
  await assertFails(unauth.collection("users").doc(ALICE_UID).collection("transactions").get());
  await assertFails(unauth.collection("users").doc(ALICE_UID).collection("people").get());

  await assertFails(
    unauth.collection("users").doc(ALICE_UID).collection("loans").doc("loan-1").set({
      title: "وام",
      source: "تجارت",
      amount: 1000000,
      totalCount: 12,
      startDay: 1,
      startMonth: 9,
      startYear: 1405,
      card: "",
      paidIndices: [0],
      createdAt: now,
      updatedAt: now,
    })
  );
});

// --- 2. User workspace metadata CRUD + cross-user isolation ---
test("User workspace doc: owner create/update succeeds, cross-user & ghost fields fail", async () => {
  const alice = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bob = testEnv.authenticatedContext(BOB_UID).firestore();
  const created = pastDate(5000);

  await assertSucceeds(
    alice.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      initialDataSeeded: true,
      categoriesInitialized: true,
      createdAt: created,
      updatedAt: created,
    })
  );

  await assertSucceeds(alice.collection("users").doc(ALICE_UID).get());
  await assertSucceeds(
    alice.collection("users").doc(ALICE_UID).update({
      initialDataSeeded: true,
      updatedAt: pastDate(1000),
    })
  );

  // Cross-user read & write denied
  await assertFails(bob.collection("users").doc(ALICE_UID).get());
  await assertFails(
    bob.collection("users").doc(ALICE_UID).update({
      initialDataSeeded: false,
      updatedAt: pastDate(500),
    })
  );

  // Ghost field & future timestamp rejected
  await assertFails(
    alice.collection("users").doc(ALICE_UID).update({
      isAdmin: true,
      updatedAt: pastDate(500),
    })
  );
  await assertFails(
    alice.collection("users").doc(ALICE_UID).update({
      updatedAt: new Date(Date.now() + 60000),
    })
  );
});

// --- 3. Loans CRUD, partial paidIndices update, @DocumentId id rejection ---
test("Loans: valid create/update/list/delete succeed; invalid payloads & cross-user fail", async () => {
  const alice = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bob = testEnv.authenticatedContext(BOB_UID).firestore();
  const created = pastDate(5000);
  const loanRef = alice.collection("users").doc(ALICE_UID).collection("loans").doc("loan-1795366866503");

  await assertSucceeds(
    loanRef.set({
      title: "شارژ ساختمون",
      source: "فاطمه",
      amount: 1000000,
      totalCount: 12,
      startDay: 1,
      startMonth: 9,
      startYear: 1405,
      card: "",
      paidIndices: [0],
      createdAt: created,
      updatedAt: created,
    })
  );

  await assertSucceeds(alice.collection("users").doc(ALICE_UID).collection("loans").get());

  // Partial update: pay installment
  await assertSucceeds(
    loanRef.update({
      paidIndices: [0, 1],
      updatedAt: pastDate(1000),
    })
  );

  // Full edit update
  await assertSucceeds(
    loanRef.update({
      title: "شارژ ساختمان",
      source: "مدیریت",
      amount: 1200000,
      totalCount: 12,
      startDay: 1,
      startMonth: 9,
      startYear: 1405,
      card: "6037-9975-0000-0000",
      paidIndices: [0, 1],
      updatedAt: pastDate(500),
    })
  );

  // Immutable createdAt violation fails
  await assertFails(
    loanRef.update({
      createdAt: pastDate(100),
      updatedAt: pastDate(100),
    })
  );

  // Storing @DocumentId 'id' inside payload must fail
  await assertFails(
    alice.collection("users").doc(ALICE_UID).collection("loans").doc("loan-bad-id").set({
      id: "loan-bad-id",
      title: "Bad",
      source: "Bank",
      amount: 100,
      totalCount: 1,
      startDay: 1,
      startMonth: 1,
      startYear: 1405,
      card: "",
      paidIndices: [],
      createdAt: created,
      updatedAt: created,
    })
  );

  // Cross-user list/get/delete fail
  await assertFails(bob.collection("users").doc(ALICE_UID).collection("loans").get());
  await assertFails(bob.collection("users").doc(ALICE_UID).collection("loans").doc("loan-1795366866503").get());
  await assertFails(bob.collection("users").doc(ALICE_UID).collection("loans").doc("loan-1795366866503").delete());

  await assertSucceeds(loanRef.delete());
});

// --- 4. Accounts, Categories & People CRUD ---
test("Accounts, Categories & People: valid CRUD succeeds and cross-user fails", async () => {
  const alice = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bob = testEnv.authenticatedContext(BOB_UID).firestore();
  const created = pastDate(5000);

  const accRef = alice.collection("users").doc(ALICE_UID).collection("accounts").doc("acc-main");
  await assertSucceeds(
    accRef.set({
      name: "تجارت جاری",
      initialBalance: 20000000,
      cardNumber: "",
      color: "#0284c7",
      createdAt: created,
      updatedAt: created,
    })
  );
  await assertSucceeds(accRef.update({ name: "تجارت", updatedAt: pastDate(1000) }));

  const catRef = alice.collection("users").doc(ALICE_UID).collection("categories").doc("cat-lend");
  await assertSucceeds(
    catRef.set({
      name: "قرض دادن",
      type: "expense",
      emoji: "🤝",
      color: "#e11d48",
      subcategories: ["قرض دادن به دیگران", "تسویه بدهی من"],
      debtCategory: true,
      createdAt: created,
      updatedAt: created,
    })
  );
  await assertSucceeds(
    catRef.update({
      subcategories: ["قرض دادن به دیگران", "تسویه بدهی"],
      updatedAt: pastDate(1000),
    })
  );

  const personRef = alice.collection("users").doc(ALICE_UID).collection("people").doc("person-1");
  await assertSucceeds(
    personRef.set({
      name: "بابا",
      note: "",
      createdAt: created,
      updatedAt: created,
    })
  );

  await assertSucceeds(alice.collection("users").doc(ALICE_UID).collection("accounts").get());
  await assertSucceeds(alice.collection("users").doc(ALICE_UID).collection("categories").get());
  await assertSucceeds(alice.collection("users").doc(ALICE_UID).collection("people").get());

  // Cross-user denied
  await assertFails(bob.collection("users").doc(ALICE_UID).collection("accounts").get());
  await assertFails(bob.collection("users").doc(ALICE_UID).collection("categories").get());
  await assertFails(bob.collection("users").doc(ALICE_UID).collection("people").get());
});

// --- 5. Transactions: Expense, Income, Transfer & Validation Guards ---
test("Transactions: expense, transfer, subcategory update succeed; invalid transfer fails", async () => {
  const alice = testEnv.authenticatedContext(ALICE_UID).firestore();
  const created = pastDate(5000);
  const txs = alice.collection("users").doc(ALICE_UID).collection("transactions");

  // Standard expense (omitting null optional keys like Kotlin .filterValues { it != null })
  await assertSucceeds(
    txs.doc("tx-1").set({
      type: "expense",
      amount: 1272800,
      accountId: "acc-cash",
      categoryId: "cat-loan",
      subcategory: "قسط بانک",
      year: 1405,
      month: 9,
      day: 1,
      note: "پرداخت قسط ۵۷ وام ازدواج یوسف",
      linkedLoanId: "loan-1795206047087",
      linkedInstallmentIndex: 56,
      createdAt: created,
      updatedAt: created,
    })
  );

  // Transfer between two distinct accounts
  await assertSucceeds(
    txs.doc("tx-transfer-1").set({
      type: "transfer",
      amount: 5000000,
      fromAccountId: "acc-main",
      toAccountId: "acc-cash",
      subcategory: "",
      year: 1405,
      month: 9,
      day: 2,
      note: "کارت به کارت",
      createdAt: created,
      updatedAt: created,
    })
  );

  // Subcategory cascade update
  await assertSucceeds(
    txs.doc("tx-1").update({
      subcategory: "قسط بانکی",
      updatedAt: pastDate(1000),
    })
  );

  // Same-account transfer must fail
  await assertFails(
    txs.doc("tx-transfer-bad").set({
      type: "transfer",
      amount: 500000,
      fromAccountId: "acc-main",
      toAccountId: "acc-main",
      subcategory: "",
      year: 1405,
      month: 9,
      day: 2,
      note: "",
      createdAt: created,
      updatedAt: created,
    })
  );

  // Expense missing categoryId must fail
  await assertFails(
    txs.doc("tx-exp-bad").set({
      type: "expense",
      amount: 50000,
      accountId: "acc-cash",
      subcategory: "",
      year: 1405,
      month: 9,
      day: 2,
      note: "",
      createdAt: created,
      updatedAt: created,
    })
  );
});
