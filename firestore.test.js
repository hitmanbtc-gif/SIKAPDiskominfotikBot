const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_asn_123";
const ALICE_EMAIL = "alice.asn@gorontaloprov.go.id";
const BOB_UID = "bob_pengawas_456";
const BOB_EMAIL = "bob.pengawas@gorontaloprov.go.id";
const CHARLIE_UID = "charlie_intruder_789";
const CHARLIE_EMAIL = "charlie@example.com";
const ADMIN_UID = "kadis_admin_001";
const ADMIN_EMAIL = "hitmanbtc@gmail.com";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function validProfilePayload(uid, email, overrides = {}) {
  const now = new Date(Date.now() - 2000);
  return {
    userId: uid,
    fullName: "Siti Rahma, S.Kom",
    nip: "199205142022032001",
    jabatanLevel: "PELAKSANA",
    jabatanTitle: "Pranata Komputer Ahli Pertama",
    unitBidang: "Bidang TIK & E-Government",
    email,
    pengawasEmail: BOB_EMAIL,
    adminEmail: ADMIN_EMAIL,
    kadisEmail: ADMIN_EMAIL,
    myPhone: "6281234567890",
    pengawasPhone: "@kasubbag_tik_gorontalo",
    adminPhone: "-100198273645",
    kadisPhone: "554433221",
    createdAt: now,
    updatedAt: now,
    ...overrides,
  };
}

function validAdminConfigPayload(overrides = {}) {
  const now = new Date(Date.now() - 1000);
  return {
    botUsername: "@LapkinDiskominfotikBot",
    defaultGroupChatId: "-1001928374650",
    pengawasChatId: "112233445",
    adminChatId: "223344556",
    kadisChatId: "334455667",
    kadisFullName: "Drs. Rifli M. Katili, M.Si",
    kadisNip: "196808171994031009",
    broadcastAnnouncement: "Batas pengisian laporan harian ASN pukul 17.00 WITA.",
    requireGpsLocation: false,
    requirePhotoEvidence: false,
    autoSendPdfToTelegram: true,
    autoSendGpsPin: true,
    updatedByEmail: ADMIN_EMAIL,
    updatedAt: now,
    ...overrides,
  };
}

function validReportPayload(uid, email, overrides = {}) {
  const now = new Date(Date.now() - 2000);
  return {
    userId: uid,
    authorName: "Siti Rahma, S.Kom",
    authorNip: "199205142022032001",
    authorJabatan: "Pranata Komputer Ahli Pertama",
    authorTier: "PELAKSANA",
    authorEmail: email,
    activityDate: "2026-11-23",
    startTime: "08:00",
    endTime: "11:30",
    summary: "Pemeliharaan Jaringan Intra Pemerintah Provinsi Gorontalo",
    description: "Melakukan pengecekan router core dan sinkronisasi VPN antar OPD.",
    classification: "RUTIN",
    unitBidang: "Bidang TIK & E-Government",
    subKegiatan: "Pengelolaan Infrastruktur Jaringan Intra Pemerintah & Pusat Data",
    locationName: "Ruang Server Diskominfotik Provinsi Gorontalo",
    gpsLatitude: 0.543544,
    gpsLongitude: 123.056769,
    gpsAccuracyMeters: 12.5,
    gpsAddress: "Jl. Prof. Dr. HB Jassin, Kota Gorontalo",
    gpsCaptured: true,
    photoBase64: "data_base64_stub_jpeg",
    photoCaption: "Pengecekan rak server pusat data",
    status: "SUBMITTED_PENGAWAS",
    currentTier: "PENGAWAS_FUNGSIONAL",
    reviewerEmails: [BOB_EMAIL, ADMIN_EMAIL],
    pengawasEmail: BOB_EMAIL,
    adminEmail: ADMIN_EMAIL,
    kadisEmail: ADMIN_EMAIL,
    pengawasReviewerName: "",
    pengawasNote: "",
    pengawasReviewedAtMillis: 0,
    adminReviewerName: "",
    adminNote: "",
    adminReviewedAtMillis: 0,
    kadisReviewerName: "",
    kadisNote: "",
    kadisReviewedAtMillis: 0,
    lastRevisionNote: "",
    createdAt: now,
    updatedAt: now,
    ...overrides,
  };
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
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

test("Unauthenticated user cannot read or write profiles, admin_config, or activity_reports", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).set(validProfilePayload(ALICE_UID, ALICE_EMAIL)));
  await assertFails(unauthDb.collection("admin_config").doc("global").get());
  await assertFails(unauthDb.collection("admin_config").doc("global").set(validAdminConfigPayload()));
  await assertFails(unauthDb.collection("activity_reports").where("userId", "==", ALICE_UID).get());
  await assertFails(unauthDb.collection("activity_reports").doc("rep_1").set(validReportPayload(ALICE_UID, ALICE_EMAIL)));
});

test("User profile isolation: owner can create/update; cross-user read/write denied", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID, { email: ALICE_EMAIL, email_verified: true }).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID, { email: BOB_EMAIL, email_verified: true }).firestore();

  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).set(validProfilePayload(ALICE_UID, ALICE_EMAIL)));
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      jabatanTitle: "Pranata Komputer Ahli Muda",
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Cross-user PII read/write denied
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).update({ fullName: "Hijacked" }));

  // Ghost field & future timestamp rejected
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      isSuperAdmin: true,
      updatedAt: new Date(Date.now() - 500),
    })
  );
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      updatedAt: new Date(Date.now() + 60000),
    })
  );
});

test("Telegram Bot Admin config: admin can write global config, regular pelaksana can read but cannot overwrite", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID, { email: ALICE_EMAIL, email_verified: true }).firestore();
  const realAdminDb = testEnv.authenticatedContext(ADMIN_UID, { email: ADMIN_EMAIL, email_verified: true }).firestore();

  // Regular Pelaksana cannot write global admin config
  await assertFails(
    aliceDb.collection("admin_config").doc("global").set(validAdminConfigPayload())
  );

  // Bootstrapped Admin can create and update global admin config
  await assertSucceeds(
    realAdminDb.collection("admin_config").doc("global").set(validAdminConfigPayload())
  );

  // Regular signed-in ASN can read global admin config
  await assertSucceeds(
    aliceDb.collection("admin_config").doc("global").get()
  );

  // Admin config cannot be deleted
  await assertFails(
    realAdminDb.collection("admin_config").doc("global").delete()
  );
});

test("ActivityReport create & query alignment: owner filtered query succeeds, unfiltered query fails", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID, { email: ALICE_EMAIL, email_verified: true }).firestore();
  const charlieDb = testEnv.authenticatedContext(CHARLIE_UID, { email: CHARLIE_EMAIL, email_verified: true }).firestore();

  await assertSucceeds(
    aliceDb.collection("activity_reports").doc("rep_001").set(validReportPayload(ALICE_UID, ALICE_EMAIL))
  );

  // Cannot create with @DocumentId `id` field or pre-approved status
  await assertFails(
    aliceDb.collection("activity_reports").doc("rep_002").set(
      validReportPayload(ALICE_UID, ALICE_EMAIL, { id: "rep_002" })
    )
  );
  await assertFails(
    aliceDb.collection("activity_reports").doc("rep_003").set(
      validReportPayload(ALICE_UID, ALICE_EMAIL, { status: "APPROVED_FINAL" })
    )
  );

  // Scoped owner query succeeds
  await assertSucceeds(
    aliceDb.collection("activity_reports").where("userId", "==", ALICE_UID).get()
  );

  // Unfiltered query by non-admin fails
  await assertFails(aliceDb.collection("activity_reports").get());
  await assertFails(charlieDb.collection("activity_reports").get());
  await assertFails(charlieDb.collection("activity_reports").doc("rep_001").get());
});

test("Hierarchical review workflow: assigned reviewer can escalate/approve; cannot alter author activity data; terminal state locked", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID, { email: ALICE_EMAIL, email_verified: true }).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID, { email: BOB_EMAIL, email_verified: true }).firestore();
  const charlieDb = testEnv.authenticatedContext(CHARLIE_UID, { email: CHARLIE_EMAIL, email_verified: true }).firestore();
  const spoofAdminDb = testEnv.authenticatedContext("spoof_uid", { email: ADMIN_EMAIL, email_verified: false }).firestore();
  const realAdminDb = testEnv.authenticatedContext(ADMIN_UID, { email: ADMIN_EMAIL, email_verified: true }).firestore();

  await assertSucceeds(
    aliceDb.collection("activity_reports").doc("rep_flow").set(validReportPayload(ALICE_UID, ALICE_EMAIL))
  );

  // Assigned reviewer query succeeds
  await assertSucceeds(
    bobDb.collection("activity_reports").where("reviewerEmails", "array-contains", BOB_EMAIL).get()
  );

  // Unverified email spoofing admin fails
  await assertFails(spoofAdminDb.collection("activity_reports").get());

  // Unassigned user cannot review
  await assertFails(
    charlieDb.collection("activity_reports").doc("rep_flow").update({
      status: "SUBMITTED_ADMINISTRATOR",
      currentTier: "ADMINISTRATOR",
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Assigned Pengawas cannot tamper with author's summary or GPS
  await assertFails(
    bobDb.collection("activity_reports").doc("rep_flow").update({
      summary: "Manipulated summary",
      status: "SUBMITTED_ADMINISTRATOR",
      currentTier: "ADMINISTRATOR",
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Assigned Pengawas approves & forwards to Administrator
  await assertSucceeds(
    bobDb.collection("activity_reports").doc("rep_flow").update({
      status: "SUBMITTED_ADMINISTRATOR",
      currentTier: "ADMINISTRATOR",
      pengawasReviewerName: "Bob Pengawas, ST",
      pengawasNote: "Diverifikasi sesuai jadwal pemeliharaan.",
      pengawasReviewedAtMillis: Date.now() - 500,
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Author cannot edit once submitted (unless returned for REVISION)
  await assertFails(
    aliceDb.collection("activity_reports").doc("rep_flow").update({
      summary: "Changed after submission",
      updatedAt: new Date(Date.now() - 400),
    })
  );

  // Author cannot delete submitted report
  await assertFails(aliceDb.collection("activity_reports").doc("rep_flow").delete());

  // Kepala Dinas (admin) finalizes report
  await assertSucceeds(
    realAdminDb.collection("activity_reports").doc("rep_flow").update({
      status: "APPROVED_FINAL",
      currentTier: "COMPLETED",
      kadisReviewerName: "Kepala Dinas Kominfo",
      kadisNote: "Disahkan sebagai laporan kinerja resmi.",
      kadisReviewedAtMillis: Date.now() - 300,
      updatedAt: new Date(Date.now() - 300),
    })
  );

  // Terminal state APPROVED_FINAL is locked against non-admin reviewer edits
  await assertFails(
    bobDb.collection("activity_reports").doc("rep_flow").update({
      status: "REVISION",
      currentTier: "PELAKSANA",
      updatedAt: new Date(Date.now() - 200),
    })
  );
});
