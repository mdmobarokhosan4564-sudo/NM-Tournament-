const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const PLAYER_UID = "player_alice";
const PLAYER2_UID = "player_bob";
const ADMIN_UID = "admin_safi";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function validAppConfig(now) {
  return {
    platformName: "FF Booyah Arena",
    tagline: "Daily Custom Rooms & Esports",
    noticeText: "Tonight 9 PM Bermuda Squad Final!",
    noticeEnabled: true,
    heroBannerTitle: "SEASON 4 GRAND FINALS",
    heroBannerSubtitle: "Win 2,000 Coins + MVP Bonus",
    heroBannerBadge: "LIVE",
    accentTheme: "BLAZE_ORANGE",
    currencyLabel: "BDT",
    paymentInfo: "Send Money: 01700000000",
    supportContact: "Telegram: @ffbooyah",
    globalRules: "No hacks. Mobile only.",
    maintenanceMode: false,
    maintenanceMessage: "Back shortly",
    createdAt: now,
    updatedAt: now,
  };
}

function validCategory(now) {
  return {
    name: "BR Full Map",
    subtitle: "48 Players • Solo / Duo / Squad",
    badge: "48P",
    bannerKey: "br",
    sortOrder: 1,
    active: true,
    createdAt: now,
    updatedAt: now,
  };
}

function validTournament(now, overrides = {}) {
  return {
    title: "Bermuda Night Rush #1",
    categoryId: "cat_br",
    categoryName: "BR Full Map",
    teamType: "SQUAD",
    mapName: "Bermuda",
    perspective: "Mobile Only",
    entryFee: 50,
    prizePool: 1500,
    perKillReward: 20,
    totalSlots: 48,
    filledSlots: 0,
    scheduleText: "Tonight 9:00 PM",
    startTimeMillis: Date.now() + 3600000,
    status: "UPCOMING",
    rules: "No teaming or emulator.",
    roomPublished: false,
    winnerSummary: "",
    featured: true,
    bannerKey: "br",
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

test("Unauthenticated users are blocked from all collections", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("app_config").doc("main").get());
  await assertFails(unauthDb.collection("tournaments").get());
  await assertFails(unauthDb.collection("categories").where("active", "==", true).get());
  await assertFails(unauthDb.collection("users").doc(PLAYER_UID).get());
});

test("Admin passcode claim enforces SHA-256 verification and rejects wrong passcodes", async () => {
  const playerDb = testEnv.authenticatedContext(PLAYER_UID).firestore();
  const adminDb = testEnv.authenticatedContext(ADMIN_UID).firestore();
  const now = new Date(Date.now() - 1000);

  await assertFails(
    playerDb.collection("admins").doc(PLAYER_UID).set({
      userId: PLAYER_UID,
      passcode: "wrongpass123",
      grantedAt: now,
    })
  );

  await assertSucceeds(
    adminDb.collection("admins").doc(ADMIN_UID).set({
      userId: ADMIN_UID,
      passcode: "safi1234@#$",
      grantedAt: now,
    })
  );

  // Admin cannot claim someone else's UID
  await assertFails(
    adminDb.collection("admins").doc(PLAYER_UID).set({
      userId: PLAYER_UID,
      passcode: "safi1234@#$",
      grantedAt: now,
    })
  );
});

test("Admin can customize app_config, categories, tournaments, and room_secret; players cannot", async () => {
  const adminDb = testEnv.authenticatedContext(ADMIN_UID).firestore();
  const playerDb = testEnv.authenticatedContext(PLAYER_UID).firestore();
  const now = new Date(Date.now() - 2000);

  // Player cannot write app_config or tournaments before admin unlock
  await assertFails(playerDb.collection("app_config").doc("main").set(validAppConfig(now)));
  await assertFails(playerDb.collection("tournaments").doc("t_1").set(validTournament(now)));

  // Unlock admin
  await assertSucceeds(
    adminDb.collection("admins").doc(ADMIN_UID).set({
      userId: ADMIN_UID,
      passcode: "safi1234@#$",
      grantedAt: now,
    })
  );

  // Admin creates app_config, category, tournament, and room secret
  await assertSucceeds(adminDb.collection("app_config").doc("main").set(validAppConfig(now)));
  await assertSucceeds(adminDb.collection("categories").doc("cat_br").set(validCategory(now)));
  await assertSucceeds(adminDb.collection("tournaments").doc("t_1").set(validTournament(now)));
  await assertSucceeds(
    adminDb.collection("tournaments").doc("t_1").collection("room_secret").doc("credentials").set({
      roomId: "99887766",
      roomPassword: "BOOYAH",
      updatedAt: now,
    })
  );

  // Player can read config, active categories, and tournaments
  await assertSucceeds(playerDb.collection("app_config").doc("main").get());
  await assertSucceeds(playerDb.collection("categories").where("active", "==", true).get());
  await assertSucceeds(playerDb.collection("tournaments").get());

  // Player CANNOT read room_secret before joining or before roomPublished == true
  await assertFails(
    playerDb.collection("tournaments").doc("t_1").collection("room_secret").doc("credentials").get()
  );

  // Admin cannot set negative startTimeMillis
  await assertFails(
    adminDb.collection("tournaments").doc("t_bad_time").set(validTournament(now, { startTimeMillis: -5 }))
  );
});

test("Atomic tournament join updates filledSlots, creates registration + roster, and unlocks room secret only when published", async () => {
  const adminDb = testEnv.authenticatedContext(ADMIN_UID).firestore();
  const playerDb = testEnv.authenticatedContext(PLAYER_UID).firestore();
  const otherDb = testEnv.authenticatedContext(PLAYER2_UID).firestore();
  const now = new Date(Date.now() - 2000);

  await assertSucceeds(
    adminDb.collection("admins").doc(ADMIN_UID).set({
      userId: ADMIN_UID,
      passcode: "safi1234@#$",
      grantedAt: now,
    })
  );
  await assertSucceeds(adminDb.collection("tournaments").doc("t_1").set(validTournament(now)));
  await assertSucceeds(
    adminDb.collection("tournaments").doc("t_1").collection("room_secret").doc("credentials").set({
      roomId: "44556677",
      roomPassword: "FFPRO",
      updatedAt: now,
    })
  );

  // Player cannot tamper filledSlots alone without creating registration
  await assertFails(
    playerDb.collection("tournaments").doc("t_1").update({
      filledSlots: 1,
      updatedAt: now,
    })
  );

  // Atomic batch join by player
  const batch = playerDb.batch();
  batch.set(playerDb.collection("registrations").doc(`t_1_${PLAYER_UID}`), {
    userId: PLAYER_UID,
    tournamentId: "t_1",
    tournamentTitle: "Bermuda Night Rush #1",
    scheduleText: "Tonight 9:00 PM",
    teamType: "SQUAD",
    mapName: "Bermuda",
    entryFee: 50,
    ffName: "ALICE_FF",
    ffUid: "123456789",
    squadName: "Team Alpha",
    paymentRef: "TXN998811",
    slotNumber: 1,
    status: "PENDING",
    kills: 0,
    rankPosition: 0,
    prizeWon: 0,
    prizePaid: false,
    createdAt: now,
    updatedAt: now,
  });
  batch.set(playerDb.collection("tournaments").doc("t_1").collection("roster").doc(PLAYER_UID), {
    userId: PLAYER_UID,
    ffName: "ALICE_FF",
    squadName: "Team Alpha",
    slotNumber: 1,
    kills: 0,
    rankPosition: 0,
    prizeWon: 0,
    status: "PENDING",
    createdAt: now,
    updatedAt: now,
  });
  batch.update(playerDb.collection("tournaments").doc("t_1"), {
    filledSlots: 1,
    updatedAt: now,
  });
  await assertSucceeds(batch.commit());

  // Player can list own registrations; other player cannot read Alice's private registration
  await assertSucceeds(playerDb.collection("registrations").where("userId", "==", PLAYER_UID).get());
  await assertFails(otherDb.collection("registrations").doc(`t_1_${PLAYER_UID}`).get());
  await assertFails(otherDb.collection("registrations").where("tournamentId", "==", "t_1").get());

  // Admin can list all registrations for the tournament
  await assertSucceeds(adminDb.collection("registrations").where("tournamentId", "==", "t_1").get());

  // Room secret still hidden while roomPublished == false
  await assertFails(
    playerDb.collection("tournaments").doc("t_1").collection("room_secret").doc("credentials").get()
  );

  // Admin publishes room
  await assertSucceeds(
    adminDb.collection("tournaments").doc("t_1").update({
      roomPublished: true,
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Joined player can now read room_secret; non-joined player still CANNOT
  await assertSucceeds(
    playerDb.collection("tournaments").doc("t_1").collection("room_secret").doc("credentials").get()
  );
  await assertFails(
    otherDb.collection("tournaments").doc("t_1").collection("room_secret").doc("credentials").get()
  );

  // If Admin marks a registration as REJECTED, that player loses room_secret access too
  await assertSucceeds(
    adminDb.collection("registrations").doc(`t_1_${PLAYER_UID}`).update({
      status: "REJECTED",
      updatedAt: new Date(Date.now() - 250),
    })
  );
  await assertFails(
    playerDb.collection("tournaments").doc("t_1").collection("room_secret").doc("credentials").get()
  );
  await assertSucceeds(
    adminDb.collection("registrations").doc(`t_1_${PLAYER_UID}`).update({
      status: "CONFIRMED",
      updatedAt: new Date(Date.now() - 150),
    })
  );
  await assertSucceeds(
    playerDb.collection("tournaments").doc("t_1").collection("room_secret").doc("credentials").get()
  );

  // Second player can join with just their in-game username (empty ffUid) and increment filledSlots
  const batch2 = otherDb.batch();
  batch2.set(otherDb.collection("registrations").doc(`t_1_${PLAYER2_UID}`), {
    userId: PLAYER2_UID,
    tournamentId: "t_1",
    tournamentTitle: "Bermuda Night Rush #1",
    scheduleText: "Tonight 9:00 PM",
    teamType: "SQUAD",
    mapName: "Bermuda",
    entryFee: 50,
    ffName: "BOB_SNIPER",
    ffUid: "",
    squadName: "",
    paymentRef: "",
    slotNumber: 2,
    status: "CONFIRMED",
    kills: 0,
    rankPosition: 0,
    prizeWon: 0,
    prizePaid: false,
    createdAt: now,
    updatedAt: now,
  });
  batch2.set(otherDb.collection("tournaments").doc("t_1").collection("roster").doc(PLAYER2_UID), {
    userId: PLAYER2_UID,
    ffName: "BOB_SNIPER",
    squadName: "",
    slotNumber: 2,
    kills: 0,
    rankPosition: 0,
    prizeWon: 0,
    status: "CONFIRMED",
    createdAt: now,
    updatedAt: now,
  });
  batch2.update(otherDb.collection("tournaments").doc("t_1"), {
    filledSlots: 2,
    updatedAt: now,
  });
  await assertSucceeds(batch2.commit());
});

test("User profile protects PII and prevents stat tampering", async () => {
  const playerDb = testEnv.authenticatedContext(PLAYER_UID).firestore();
  const otherDb = testEnv.authenticatedContext(PLAYER2_UID).firestore();
  const now = new Date(Date.now() - 1000);

  await assertSucceeds(
    playerDb.collection("users").doc(PLAYER_UID).set({
      userId: PLAYER_UID,
      displayName: "Alice",
      ffName: "ALICE_FF",
      ffUid: "123456789",
      email: "alice@example.com",
      matchesJoined: 0,
      totalKills: 0,
      totalWins: 0,
      totalWinnings: 0,
      createdAt: now,
      updatedAt: now,
    })
  );

  // Other user cannot read Alice's PII profile
  await assertFails(otherDb.collection("users").doc(PLAYER_UID).get());

  // Alice can update her current Free Fire in-game rank
  await assertSucceeds(
    playerDb.collection("users").doc(PLAYER_UID).update({
      inGameRank: "Heroic",
      updatedAt: new Date(Date.now() - 450),
    })
  );

  // Oversized inGameRank (> 32 chars) is rejected
  await assertFails(
    playerDb.collection("users").doc(PLAYER_UID).update({
      inGameRank: "Grandmaster_Rank_Tier_Overflow_12345",
      updatedAt: new Date(Date.now() - 425),
    })
  );

  // Alice can register/refresh her FCM push token
  await assertSucceeds(
    playerDb.collection("users").doc(PLAYER_UID).update({
      fcmToken: "fcm_device_token_sample_abc123",
      updatedAt: new Date(Date.now() - 400),
    })
  );

  // Other user cannot overwrite Alice's FCM token or inGameRank
  await assertFails(
    otherDb.collection("users").doc(PLAYER_UID).update({
      inGameRank: "Bronze",
      updatedAt: new Date(Date.now() - 350),
    })
  );
  await assertFails(
    otherDb.collection("users").doc(PLAYER_UID).update({
      fcmToken: "hijacked_token",
      updatedAt: new Date(Date.now() - 300),
    })
  );

  // Oversized FCM token (> 512 chars) is rejected
  await assertFails(
    playerDb.collection("users").doc(PLAYER_UID).update({
      fcmToken: "x".repeat(600),
      updatedAt: new Date(Date.now() - 200),
    })
  );

  // Alice cannot inflate her own totalWinnings or totalWins
  await assertFails(
    playerDb.collection("users").doc(PLAYER_UID).update({
      totalWinnings: 99999,
      updatedAt: new Date(Date.now() - 500),
    })
  );
});

test("Leaderboard is admin-written and player-readable; bootstrapped admin claim works", async () => {
  const playerDb = testEnv.authenticatedContext(PLAYER_UID).firestore();
  const bootstrapAdminDb = testEnv
    .authenticatedContext("boot_admin", {
      email: "mdmobarokhosan4564@gmail.com",
      email_verified: true,
    })
    .firestore();
  const now = new Date(Date.now() - 1000);

  // Player cannot write leaderboard directly
  await assertFails(
    playerDb.collection("leaderboard").doc(PLAYER_UID).set({
      userId: PLAYER_UID,
      ffName: "ALICE_FF",
      totalKills: 99,
      totalWins: 10,
      totalWinnings: 50000,
      matchesPlayed: 1,
      createdAt: now,
      updatedAt: now,
    })
  );

  // Bootstrapped admin can write leaderboard and app_config without needing /admins doc
  await assertSucceeds(
    bootstrapAdminDb.collection("leaderboard").doc(PLAYER_UID).set({
      userId: PLAYER_UID,
      ffName: "ALICE_FF",
      totalKills: 7,
      totalWins: 1,
      totalWinnings: 500,
      matchesPlayed: 1,
      createdAt: now,
      updatedAt: now,
    })
  );

  // Signed-in player can read leaderboard
  await assertSucceeds(playerDb.collection("leaderboard").get());
});

test("Wallet Add Money & Withdraw requests enforce balance checks, privacy, and admin-only approval", async () => {
  const adminDb = testEnv.authenticatedContext(ADMIN_UID).firestore();
  const playerDb = testEnv.authenticatedContext(PLAYER_UID).firestore();
  const otherDb = testEnv.authenticatedContext(PLAYER2_UID).firestore();
  const now = new Date(Date.now() - 1000);

  await assertSucceeds(
    adminDb.collection("admins").doc(ADMIN_UID).set({
      userId: ADMIN_UID,
      passcode: "safi1234@#$",
      grantedAt: now,
    })
  );

  await assertSucceeds(
    playerDb.collection("users").doc(PLAYER_UID).set({
      userId: PLAYER_UID,
      displayName: "Alice",
      ffName: "ALICE_FF",
      ffUid: "123456789",
      email: "alice@example.com",
      matchesJoined: 0,
      totalKills: 0,
      totalWins: 0,
      totalWinnings: 0,
      walletBalance: 0,
      createdAt: now,
      updatedAt: now,
    })
  );

  // Player CANNOT inflate their own walletBalance directly
  await assertFails(
    playerDb.collection("users").doc(PLAYER_UID).update({
      walletBalance: 5000,
      updatedAt: now,
    })
  );

  // Player CANNOT submit a Withdraw request exceeding walletBalance (0)
  await assertFails(
    playerDb.collection("wallet_requests").doc("w_over").set({
      userId: PLAYER_UID,
      userName: "Alice",
      ffName: "ALICE_FF",
      type: "WITHDRAW",
      method: "BKASH",
      amount: 200,
      accountNumber: "01711112222",
      txnId: "",
      status: "PENDING",
      adminNote: "",
      createdAt: now,
      updatedAt: now,
    })
  );

  // Player submits Add Money (DEPOSIT) request with TxnID
  await assertSucceeds(
    playerDb.collection("wallet_requests").doc("dep_1").set({
      userId: PLAYER_UID,
      userName: "Alice",
      ffName: "ALICE_FF",
      type: "DEPOSIT",
      method: "NAGAD",
      amount: 500,
      accountNumber: "01711112222",
      txnId: "TXN778899",
      status: "PENDING",
      adminNote: "",
      createdAt: now,
      updatedAt: now,
    })
  );

  // Other player cannot read Alice's wallet request, nor can Alice self-approve it
  await assertFails(otherDb.collection("wallet_requests").doc("dep_1").get());
  await assertFails(
    playerDb.collection("wallet_requests").doc("dep_1").update({
      status: "APPROVED",
      adminNote: "Self",
      updatedAt: now,
    })
  );

  // Admin approves deposit and credits walletBalance
  const batch = adminDb.batch();
  batch.update(adminDb.collection("wallet_requests").doc("dep_1"), {
    status: "APPROVED",
    adminNote: "Verified Nagad",
    updatedAt: now,
  });
  batch.update(adminDb.collection("users").doc(PLAYER_UID), {
    walletBalance: 500,
    updatedAt: now,
  });
  await assertSucceeds(batch.commit());

  // Now player CAN submit a Withdraw request within their 500 balance
  await assertSucceeds(
    playerDb.collection("wallet_requests").doc("wd_1").set({
      userId: PLAYER_UID,
      userName: "Alice",
      ffName: "ALICE_FF",
      type: "WITHDRAW",
      method: "BKASH",
      amount: 300,
      accountNumber: "01812345678",
      txnId: "",
      status: "PENDING",
      adminNote: "",
      createdAt: now,
      updatedAt: now,
    })
  );

  // Admin lists all requests and marks withdrawal paid
  await assertSucceeds(adminDb.collection("wallet_requests").get());
  const wdBatch = adminDb.batch();
  wdBatch.update(adminDb.collection("wallet_requests").doc("wd_1"), {
    status: "APPROVED",
    adminNote: "Sent to 01812345678",
    updatedAt: now,
  });
  wdBatch.update(adminDb.collection("users").doc(PLAYER_UID), {
    walletBalance: 200,
    updatedAt: now,
  });
  await assertSucceeds(wdBatch.commit());
});

test("Legacy user profile missing Booyah Arena fields repairs cleanly during join and wallet deposit approval", async () => {
  const adminDb = testEnv.authenticatedContext("boot_admin", {
    email: "mdmobarokhosan4564@gmail.com",
    email_verified: true,
  }).firestore();
  const legacyPlayerDb = testEnv.authenticatedContext(PLAYER_UID).firestore();
  const now = new Date(Date.now() - 2000);

  // Simulate legacy Blaze Zone user document missing userId, ffName, totalWinnings, walletBalance, createdAt
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    const db = ctx.firestore();
    await db.collection("users").doc(PLAYER_UID).set({
      displayName: "Safi",
      coins: 900,
    });
    await db.collection("tournaments").doc("match_legacy").set(validTournament(now));
    await db.collection("wallet_requests").doc("dep_legacy").set({
      userId: PLAYER_UID,
      userName: "Safi",
      ffName: "",
      type: "DEPOSIT",
      method: "BKASH",
      amount: 500,
      accountNumber: "01772902980",
      txnId: "TXN123456",
      status: "PENDING",
      adminNote: "",
      createdAt: now,
      updatedAt: now,
    });
  });

  // Owner CANNOT use legacy repair to inflate walletBalance or totalWinnings
  await assertFails(
    legacyPlayerDb.collection("users").doc(PLAYER_UID).update({
      userId: PLAYER_UID,
      ffName: "",
      ffUid: "",
      email: "",
      matchesJoined: 0,
      totalKills: 0,
      totalWins: 0,
      totalWinnings: 0,
      walletBalance: 9999,
      createdAt: now,
      updatedAt: now,
    })
  );

  // Atomic join + legacy user repair succeeds
  const joinBatch = legacyPlayerDb.batch();
  joinBatch.set(legacyPlayerDb.collection("registrations").doc(`match_legacy_${PLAYER_UID}`), {
    userId: PLAYER_UID,
    tournamentId: "match_legacy",
    tournamentTitle: "Bermuda Night Rush #1",
    scheduleText: "Tonight 9:00 PM",
    teamType: "SQUAD",
    mapName: "Bermuda",
    entryFee: 50,
    ffName: "SAFI_PRO",
    ffUid: "12345678",
    squadName: "",
    paymentRef: "",
    slotNumber: 1,
    status: "CONFIRMED",
    kills: 0,
    rankPosition: 0,
    prizeWon: 0,
    prizePaid: false,
    createdAt: now,
    updatedAt: now,
  });
  joinBatch.set(legacyPlayerDb.collection("tournaments").doc("match_legacy").collection("roster").doc(PLAYER_UID), {
    userId: PLAYER_UID,
    ffName: "SAFI_PRO",
    squadName: "",
    slotNumber: 1,
    kills: 0,
    rankPosition: 0,
    prizeWon: 0,
    status: "CONFIRMED",
    createdAt: now,
    updatedAt: now,
  });
  joinBatch.update(legacyPlayerDb.collection("tournaments").doc("match_legacy"), {
    filledSlots: 1,
    updatedAt: now,
  });
  joinBatch.update(legacyPlayerDb.collection("users").doc(PLAYER_UID), {
    userId: PLAYER_UID,
    ffName: "SAFI_PRO",
    ffUid: "12345678",
    email: "",
    matchesJoined: 1,
    totalKills: 0,
    totalWins: 0,
    totalWinnings: 0,
    walletBalance: 0,
    createdAt: now,
    updatedAt: now,
  });
  await assertSucceeds(joinBatch.commit());

  // Admin approves deposit and updates walletBalance cleanly
  const approveBatch = adminDb.batch();
  approveBatch.update(adminDb.collection("wallet_requests").doc("dep_legacy"), {
    status: "APPROVED",
    adminNote: "Approved",
    updatedAt: new Date(Date.now() - 500),
  });
  approveBatch.update(adminDb.collection("users").doc(PLAYER_UID), {
    walletBalance: 500,
    updatedAt: new Date(Date.now() - 500),
  });
  await assertSucceeds(approveBatch.commit());
});



