import {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} from "@firebase/rules-unit-testing";
import * as fs from "fs";
import * as path from "path";
import {
  doc,
  getDoc,
  setDoc,
  updateDoc,
  deleteDoc,
  runTransaction,
  serverTimestamp,
} from "firebase/firestore";

describe("Shree Shyam Store Firestore Security Rules", () => {
  let testEnv: any;

  before(async () => {
    const rulesPath = path.resolve("../firestore.rules");
    const rules = fs.readFileSync(rulesPath, "utf8");
    testEnv = await initializeTestEnvironment({
      projectId: "demo-shreeshyamstore",
      firestore: {
        rules,
        host: "127.0.0.1",
        port: 8080,
      },
    });
  });

  after(async () => {
    await testEnv.cleanup();
  });

  beforeEach(async () => {
    await testEnv.clearFirestore();
  });

  describe("Signed-out Access", () => {
    it("should deny read/write to users, shops, and members when unauthenticated", async () => {
      const db = testEnv.unauthenticatedContext().firestore();
      
      await assertFails(getDoc(doc(db, "users", "alice")));
      await assertFails(setDoc(doc(db, "users", "alice"), { uid: "alice" }));
      
      await assertFails(getDoc(doc(db, "shops", "shop1")));
      await assertFails(setDoc(doc(db, "shops", "shop1"), { shopId: "shop1" }));
      
      await assertFails(getDoc(doc(db, "shops", "shop1", "members", "alice")));
      await assertFails(setDoc(doc(db, "shops", "shop1", "members", "alice"), { uid: "alice" }));
    });
  });

  describe("User Profile Security", () => {
    it("should allow a user to read their own profile", async () => {
      const db = testEnv.authenticatedContext("alice", { email: "alice@gmail.com" }).firestore();
      await assertSucceeds(getDoc(doc(db, "users", "alice")));
    });

    it("should deny a user from reading another user's profile", async () => {
      const db = testEnv.authenticatedContext("alice", { email: "alice@gmail.com" }).firestore();
      await assertFails(getDoc(doc(db, "users", "bob")));
    });

    it("should deny profile creation if the email does not match the Google authenticated email", async () => {
      const db = testEnv.authenticatedContext("alice", { email: "alice@gmail.com" }).firestore();
      
      await assertFails(
        setDoc(doc(db, "users", "alice"), {
          uid: "alice",
          email: "bob@gmail.com", // Forged email
          displayName: "Alice",
          activeShopId: null,
          createdAt: serverTimestamp(),
        })
      );
    });

    it("should deny profile creation if any required fields are missing (partial writes)", async () => {
      const db = testEnv.authenticatedContext("alice", { email: "alice@gmail.com" }).firestore();
      
      await assertFails(
        setDoc(doc(db, "users", "alice"), {
          uid: "alice",
          email: "alice@gmail.com",
          // missing displayName, activeShopId, createdAt
        })
      );
    });

    it("should deny profile creation if extra arbitrary fields are provided (hasOnly violation)", async () => {
      const db = testEnv.authenticatedContext("alice", { email: "alice@gmail.com" }).firestore();
      
      await assertFails(
        setDoc(doc(db, "users", "alice"), {
          uid: "alice",
          email: "alice@gmail.com",
          displayName: "Alice",
          activeShopId: null,
          createdAt: serverTimestamp(),
          role: "admin", // Arbitrary extra field
        })
      );
    });

    it("should deny profile creation if the display name exceeds 100 characters", async () => {
      const db = testEnv.authenticatedContext("alice", { email: "alice@gmail.com" }).firestore();
      const longName = "a".repeat(101);
      
      await assertFails(
        setDoc(doc(db, "users", "alice"), {
          uid: "alice",
          email: "alice@gmail.com",
          displayName: longName,
          activeShopId: null,
          createdAt: serverTimestamp(),
        })
      );
    });

    it("should deny profile creation if createdAt is not request.time (invalid timestamp)", async () => {
      const db = testEnv.authenticatedContext("alice", { email: "alice@gmail.com" }).firestore();
      
      await assertFails(
        setDoc(doc(db, "users", "alice"), {
          uid: "alice",
          email: "alice@gmail.com",
          displayName: "Alice",
          activeShopId: null,
          createdAt: new Date(2020, 1, 1), // Invalid timestamp
        })
      );
    });
  });

  describe("Atomic Shop and Profile Onboarding", () => {
    it("should succeed when creating user, shop, and membership profiles atomically in a transaction", async () => {
      const db = testEnv.authenticatedContext("alice", { email: "alice@gmail.com" }).firestore();
      
      await assertSucceeds(
        runTransaction(db, async (transaction) => {
          const userRef = doc(db, "users", "alice");
          const shopRef = doc(db, "shops", "shop_alice");
          const memberRef = doc(db, "shops", "shop_alice", "members", "alice");

          transaction.set(userRef, {
            uid: "alice",
            email: "alice@gmail.com",
            displayName: "Alice",
            activeShopId: "shop_alice",
            createdAt: serverTimestamp(),
          });

          transaction.set(shopRef, {
            shopId: "shop_alice",
            name: "Alice Kiryana Store",
            ownerPhone: "9876543210",
            ownerUid: "alice",
            createdAt: serverTimestamp(),
          });

          transaction.set(memberRef, {
            uid: "alice",
            shopId: "shop_alice",
            role: "owner",
            status: "active",
            createdAt: serverTimestamp(),
          });
        })
      );
    });

    it("should fail onboarding if any of the three documents are missing (partial writes)", async () => {
      const db = testEnv.authenticatedContext("alice", { email: "alice@gmail.com" }).firestore();
      
      // Attempting to create user and shop without creating the membership document
      await assertFails(
        runTransaction(db, async (transaction) => {
          const userRef = doc(db, "users", "alice");
          const shopRef = doc(db, "shops", "shop_alice");

          transaction.set(userRef, {
            uid: "alice",
            email: "alice@gmail.com",
            displayName: "Alice",
            activeShopId: "shop_alice",
            createdAt: serverTimestamp(),
          });

          transaction.set(shopRef, {
            shopId: "shop_alice",
            name: "Alice Kiryana Store",
            ownerPhone: "9876543210",
            ownerUid: "alice",
            createdAt: serverTimestamp(),
          });
        })
      );
    });

    it("should fail if the shop ownerUid does not match the creating authenticated user", async () => {
      const db = testEnv.authenticatedContext("alice", { email: "alice@gmail.com" }).firestore();
      
      await assertFails(
        runTransaction(db, async (transaction) => {
          const userRef = doc(db, "users", "alice");
          const shopRef = doc(db, "shops", "shop_alice");
          const memberRef = doc(db, "shops", "shop_alice", "members", "alice");

          transaction.set(userRef, {
            uid: "alice",
            email: "alice@gmail.com",
            displayName: "Alice",
            activeShopId: "shop_alice",
            createdAt: serverTimestamp(),
          });

          transaction.set(shopRef, {
            shopId: "shop_alice",
            name: "Alice Kiryana Store",
            ownerPhone: "9876543210",
            ownerUid: "bob", // Forged ownerUid
            createdAt: serverTimestamp(),
          });

          transaction.set(memberRef, {
            uid: "alice",
            shopId: "shop_alice",
            role: "owner",
            status: "active",
            createdAt: serverTimestamp(),
          });
        })
      );
    });

    it("should fail if the owner phone length is invalid (less than 10 or greater than 15 digits)", async () => {
      const db = testEnv.authenticatedContext("alice", { email: "alice@gmail.com" }).firestore();
      
      await assertFails(
        runTransaction(db, async (transaction) => {
          const userRef = doc(db, "users", "alice");
          const shopRef = doc(db, "shops", "shop_alice");
          const memberRef = doc(db, "shops", "shop_alice", "members", "alice");

          transaction.set(userRef, {
            uid: "alice",
            email: "alice@gmail.com",
            displayName: "Alice",
            activeShopId: "shop_alice",
            createdAt: serverTimestamp(),
          });

          transaction.set(shopRef, {
            shopId: "shop_alice",
            name: "Alice Kiryana Store",
            ownerPhone: "12345", // Too short
            ownerUid: "alice",
            createdAt: serverTimestamp(),
          });

          transaction.set(memberRef, {
            uid: "alice",
            shopId: "shop_alice",
            role: "owner",
            status: "active",
            createdAt: serverTimestamp(),
          });
        })
      );
    });

    it("should fail if the owner phone contains non-digit characters", async () => {
      const db = testEnv.authenticatedContext("alice", { email: "alice@gmail.com" }).firestore();
      
      await assertFails(
        runTransaction(db, async (transaction) => {
          const userRef = doc(db, "users", "alice");
          const shopRef = doc(db, "shops", "shop_alice");
          const memberRef = doc(db, "shops", "shop_alice", "members", "alice");

          transaction.set(userRef, {
            uid: "alice",
            email: "alice@gmail.com",
            displayName: "Alice",
            activeShopId: "shop_alice",
            createdAt: serverTimestamp(),
          });

          transaction.set(shopRef, {
            shopId: "shop_alice",
            name: "Alice Kiryana Store",
            ownerPhone: "98765abcde",
            ownerUid: "alice",
            createdAt: serverTimestamp(),
          });

          transaction.set(memberRef, {
            uid: "alice",
            shopId: "shop_alice",
            role: "owner",
            status: "active",
            createdAt: serverTimestamp(),
          });
        })
      );
    });

    it("should fail if the membership role is not owner or status is not active on create", async () => {
      const db = testEnv.authenticatedContext("alice", { email: "alice@gmail.com" }).firestore();
      
      await assertFails(
        runTransaction(db, async (transaction) => {
          const userRef = doc(db, "users", "alice");
          const shopRef = doc(db, "shops", "shop_alice");
          const memberRef = doc(db, "shops", "shop_alice", "members", "alice");

          transaction.set(userRef, {
            uid: "alice",
            email: "alice@gmail.com",
            displayName: "Alice",
            activeShopId: "shop_alice",
            createdAt: serverTimestamp(),
          });

          transaction.set(shopRef, {
            shopId: "shop_alice",
            name: "Alice Kiryana Store",
            ownerPhone: "9876543210",
            ownerUid: "alice",
            createdAt: serverTimestamp(),
          });

          transaction.set(memberRef, {
            uid: "alice",
            shopId: "shop_alice",
            role: "employee", // Forged role
            status: "active",
            createdAt: serverTimestamp(),
          });
        })
      );
    });
  });

  describe("Update and Delete Constraints", () => {
    beforeEach(async () => {
      // Seed initial valid user, shop, and membership
      await testEnv.withSecurityRulesDisabled(async (context) => {
        const db = context.firestore();
        await setDoc(doc(db, "users", "alice"), {
          uid: "alice",
          email: "alice@gmail.com",
          displayName: "Alice",
          activeShopId: "shop_alice",
          createdAt: new Date(),
        });
        await setDoc(doc(db, "shops", "shop_alice"), {
          shopId: "shop_alice",
          name: "Alice Kiryana Store",
          ownerPhone: "9876543210",
          ownerUid: "alice",
          createdAt: new Date(),
        });
        await setDoc(doc(db, "shops", "shop_alice", "members", "alice"), {
          uid: "alice",
          shopId: "shop_alice",
          role: "owner",
          status: "active",
          createdAt: new Date(),
        });
      });
    });

    it("should allow owner to update shop name and ownerPhone", async () => {
      const db = testEnv.authenticatedContext("alice", { email: "alice@gmail.com" }).firestore();
      
      await assertSucceeds(
        updateDoc(doc(db, "shops", "shop_alice"), {
          name: "Alice New Store Name",
          ownerPhone: "9999988888",
        })
      );
    });

    it("should deny owner from updating shop ownerUid or shopId", async () => {
      const db = testEnv.authenticatedContext("alice", { email: "alice@gmail.com" }).firestore();
      
      await assertFails(
        updateDoc(doc(db, "shops", "shop_alice"), {
          ownerUid: "bob",
        })
      );
    });

    it("should allow user to update their activeShopId", async () => {
      const db = testEnv.authenticatedContext("alice", { email: "alice@gmail.com" }).firestore();
      
      // Let's seed another shop first
      await testEnv.withSecurityRulesDisabled(async (context) => {
        const adminDb = context.firestore();
        await setDoc(doc(adminDb, "shops", "shop_alice_2"), {
          shopId: "shop_alice_2",
          name: "Alice Second Store",
          ownerPhone: "9876543210",
          ownerUid: "alice",
          createdAt: new Date(),
        });
        await setDoc(doc(adminDb, "shops", "shop_alice_2", "members", "alice"), {
          uid: "alice",
          shopId: "shop_alice_2",
          role: "owner",
          status: "active",
          createdAt: new Date(),
        });
      });

      await assertSucceeds(
        updateDoc(doc(db, "users", "alice"), {
          activeShopId: "shop_alice_2",
        })
      );
    });

    it("should deny user from updating their email, uid, or createdAt", async () => {
      const db = testEnv.authenticatedContext("alice", { email: "alice@gmail.com" }).firestore();
      
      await assertFails(
        updateDoc(doc(db, "users", "alice"), {
          email: "malicious@gmail.com",
        })
      );
    });

    it("should deny shop profile deletion", async () => {
      const db = testEnv.authenticatedContext("alice", { email: "alice@gmail.com" }).firestore();
      await assertFails(deleteDoc(doc(db, "shops", "shop_alice")));
    });

    it("should deny member update and deletion", async () => {
      const db = testEnv.authenticatedContext("alice", { email: "alice@gmail.com" }).firestore();
      const memberRef = doc(db, "shops", "shop_alice", "members", "alice");
      
      await assertFails(updateDoc(memberRef, { role: "admin" }));
      await assertFails(deleteDoc(memberRef));
    });
  });

  describe("Cross-Shop Access Restrictions", () => {
    beforeEach(async () => {
      // Seed two shops belonging to different owners
      await testEnv.withSecurityRulesDisabled(async (context) => {
        const db = context.firestore();
        
        // Alice's setup
        await setDoc(doc(db, "users", "alice"), {
          uid: "alice",
          email: "alice@gmail.com",
          displayName: "Alice",
          activeShopId: "shop_alice",
          createdAt: new Date(),
        });
        await setDoc(doc(db, "shops", "shop_alice"), {
          shopId: "shop_alice",
          name: "Alice Store",
          ownerPhone: "9876543210",
          ownerUid: "alice",
          createdAt: new Date(),
        });
        await setDoc(doc(db, "shops", "shop_alice", "members", "alice"), {
          uid: "alice",
          shopId: "shop_alice",
          role: "owner",
          status: "active",
          createdAt: new Date(),
        });

        // Bob's setup
        await setDoc(doc(db, "users", "bob"), {
          uid: "bob",
          email: "bob@gmail.com",
          displayName: "Bob",
          activeShopId: "shop_bob",
          createdAt: new Date(),
        });
        await setDoc(doc(db, "shops", "shop_bob"), {
          shopId: "shop_bob",
          name: "Bob Store",
          ownerPhone: "9876543211",
          ownerUid: "bob",
          createdAt: new Date(),
        });
        await setDoc(doc(db, "shops", "shop_bob", "members", "bob"), {
          uid: "bob",
          shopId: "shop_bob",
          role: "owner",
          status: "active",
          createdAt: new Date(),
        });
      });
    });

    it("should deny Alice from reading or writing to Bob's shop profile", async () => {
      const db = testEnv.authenticatedContext("alice", { email: "alice@gmail.com" }).firestore();
      
      await assertFails(getDoc(doc(db, "shops", "shop_bob")));
      await assertFails(
        updateDoc(doc(db, "shops", "shop_bob"), {
          name: "Bob Hack Store",
        })
      );
    });

    it("should deny Alice from reading, creating, or writing to Bob's membership collection", async () => {
      const db = testEnv.authenticatedContext("alice", { email: "alice@gmail.com" }).firestore();
      const bobMemberRef = doc(db, "shops", "shop_bob", "members", "bob");
      const aliceUnderBobMemberRef = doc(db, "shops", "shop_bob", "members", "alice");

      await assertFails(getDoc(bobMemberRef));
      
      await assertFails(
        setDoc(aliceUnderBobMemberRef, {
          uid: "alice",
          shopId: "shop_bob",
          role: "owner",
          status: "active",
          createdAt: serverTimestamp(),
        })
      );
    });

    it("should deny Alice from setting their user activeShopId to Bob's shop ID", async () => {
      const db = testEnv.authenticatedContext("alice", { email: "alice@gmail.com" }).firestore();
      
      await assertFails(
        updateDoc(doc(db, "users", "alice"), {
          activeShopId: "shop_bob", // bob's shop - alice is not owner/member there in Bob's shop
        })
      );
    });
  });
  describe("Shop Data Sync Collections", () => {
    beforeEach(async () => {
      await testEnv.withSecurityRulesDisabled(async (context) => {
        const db = context.firestore();
        await setDoc(doc(db, "users", "alice"), {
          uid: "alice",
          email: "alice@gmail.com",
          displayName: "Alice",
          activeShopId: "shop_alice",
          createdAt: new Date(),
        });
        await setDoc(doc(db, "shops", "shop_alice"), {
          shopId: "shop_alice",
          name: "Alice Store",
          ownerPhone: "9876543210",
          ownerUid: "alice",
          createdAt: new Date(),
        });
        await setDoc(doc(db, "shops", "shop_alice", "members", "alice"), {
          uid: "alice",
          shopId: "shop_alice",
          role: "owner",
          status: "active",
          createdAt: new Date(),
        });
      });
    });

    it("should allow the owner to sync category, product, customer, credit ledger, sale, sale item, and stock documents", async () => {
      const db = testEnv.authenticatedContext("alice", { email: "alice@gmail.com" }).firestore();

      await assertSucceeds(setDoc(doc(db, "shops", "shop_alice", "categories", "cat-1"), {
        localUuid: "cat-1",
        shopId: "shop_alice",
        syncStatus: "SYNCED",
        createdByUid: "alice",
        updatedByUid: "alice",
        name: "Rice",
        isActive: true,
        createdAt: 1,
        updatedAt: 2,
      }));

      await assertSucceeds(setDoc(doc(db, "shops", "shop_alice", "products", "prod-1"), {
        localUuid: "prod-1",
        shopId: "shop_alice",
        syncStatus: "SYNCED",
        createdByUid: "alice",
        updatedByUid: "alice",
        name: "Loose Rice",
        categoryId: "cat-1",
        mrp: 80,
        sellingPrice: 75,
        purchasePrice: null,
        unitType: "WEIGHT",
        displayUnit: "KILOGRAM",
        baseUnit: "GRAM",
        allowsDecimalQuantity: true,
        quantityScale: 3,
        pricePerUnitPaise: 7500,
        priceUnitBaseQty: 1000,
        purchasePricePerUnitPaise: null,
        purchasePriceUnitBaseQty: null,
        currentStock: 10,
        stockQuantityBase: 10000,
        trackStock: true,
        lowStockAlertQty: 2,
        lowStockAlertBase: 2000,
        isActive: true,
        createdAt: 1,
        updatedAt: 2,
      }));

      await assertSucceeds(setDoc(doc(db, "shops", "shop_alice", "customers", "cust-1"), {
        localUuid: "cust-1",
        shopId: "shop_alice",
        syncStatus: "SYNCED",
        createdByUid: "alice",
        updatedByUid: "alice",
        name: "Ramesh",
        phone: "9876543210",
        isActive: true,
        createdAt: 1,
        updatedAt: 2,
      }));

      await assertSucceeds(setDoc(doc(db, "shops", "shop_alice", "udhaarTransactions", "tx-1"), {
        localUuid: "tx-1",
        shopId: "shop_alice",
        syncStatus: "SYNCED",
        createdByUid: "alice",
        updatedByUid: "alice",
        customerId: "cust-1",
        saleId: null,
        type: "CREDIT",
        amount: 120,
        amountPaise: 12000,
        note: "Manual credit",
        createdAt: 3,
      }));

      await assertSucceeds(setDoc(doc(db, "shops", "shop_alice", "sales", "sale-1"), {
        localUuid: "sale-1",
        shopId: "shop_alice",
        syncStatus: "SYNCED",
        createdByUid: "alice",
        updatedByUid: "alice",
        idempotencyKey: "sale-op-1",
        billNumber: "BILL-001",
        totalAmount: 120,
        totalAmountPaise: 12000,
        paymentMode: "UDHAAR",
        saleStatus: "COMPLETED",
        customerId: "cust-1",
        note: null,
        createdAt: 4,
      }));

      await assertSucceeds(setDoc(doc(db, "shops", "shop_alice", "saleItems", "item-1"), {
        localUuid: "item-1",
        shopId: "shop_alice",
        syncStatus: "SYNCED",
        createdByUid: "alice",
        updatedByUid: "alice",
        saleId: "sale-1",
        productId: "prod-1",
        productNameSnapshot: "Loose Rice",
        quantity: 1,
        unitTypeSnapshot: "WEIGHT",
        displayUnitSnapshot: "KILOGRAM",
        baseUnitSnapshot: "GRAM",
        enteredQuantityText: "1.6",
        quantityBase: 1600,
        unitPrice: 120,
        originalPricePerUnitPaise: 7500,
        originalPriceUnitBaseQty: 1000,
        effectivePricePerUnitPaise: 7500,
        effectivePriceUnitBaseQty: 1000,
        rateOverridden: false,
        lineTotal: 120,
        lineTotalPaise: 12000,
        purchasePricePerUnitPaiseSnapshot: null,
        purchasePriceUnitBaseQtySnapshot: null,
      }));

      await assertSucceeds(setDoc(doc(db, "shops", "shop_alice", "stockAdjustments", "adj-1"), {
        localUuid: "adj-1",
        shopId: "shop_alice",
        syncStatus: "SYNCED",
        createdByUid: "alice",
        updatedByUid: "alice",
        productId: "prod-1",
        oldStock: 10,
        oldQuantityBase: 10000,
        newStock: 8,
        newQuantityBase: 8400,
        difference: -2,
        differenceBase: -1600,
        displayUnitSnapshot: "KILOGRAM",
        reason: "Sale BILL-001",
        createdAt: 5,
      }));
    });

    it("should deny another signed-in user from reading or writing Alice shop data", async () => {
      const aliceDb = testEnv.authenticatedContext("alice", { email: "alice@gmail.com" }).firestore();
      await setDoc(doc(aliceDb, "shops", "shop_alice", "categories", "cat-1"), {
        localUuid: "cat-1",
        shopId: "shop_alice",
        syncStatus: "SYNCED",
        createdByUid: "alice",
        updatedByUid: "alice",
        name: "Rice",
        isActive: true,
        createdAt: 1,
        updatedAt: 2,
      });

      const bobDb = testEnv.authenticatedContext("bob", { email: "bob@gmail.com" }).firestore();
      await assertFails(getDoc(doc(bobDb, "shops", "shop_alice", "categories", "cat-1")));
      await assertFails(setDoc(doc(bobDb, "shops", "shop_alice", "categories", "cat-2"), {
        localUuid: "cat-2",
        shopId: "shop_alice",
        syncStatus: "SYNCED",
        createdByUid: "bob",
        updatedByUid: "bob",
        name: "Hack",
        isActive: true,
        createdAt: 1,
        updatedAt: 2,
      }));
    });
  });
});
