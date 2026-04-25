import requests
import random
import time
import os
from datetime import datetime
from collections import deque

API_URL  = os.getenv("API_URL", "http://localhost:8080")
ENDPOINT = f"{API_URL}/api/sales"
INTERVAL = int(os.getenv("INTERVAL_SECONDS", "8"))

# Tracks last 20 customer+item combos to avoid immediate repeats
recent_combos = deque(maxlen=20)

CATALOGUE = [
    {"itemName": "22K Gold Bangle",            "itemCategory": "BRACELET", "itemWeight": 22.5, "basePrice": 1250.00},
    {"itemName": "Diamond Solitaire Ring",      "itemCategory": "RING",     "itemWeight":  3.2, "basePrice": 4500.00},
    {"itemName": "Silver Anklet Pair",          "itemCategory": "BRACELET", "itemWeight": 18.0, "basePrice":  180.00},
    {"itemName": "Platinum Wedding Band",       "itemCategory": "RING",     "itemWeight":  5.5, "basePrice": 1700.00},
    {"itemName": "Gold Jhumka Earrings",        "itemCategory": "EARRING",  "itemWeight": 12.0, "basePrice":  620.00},
    {"itemName": "Diamond Tennis Bracelet",     "itemCategory": "BRACELET", "itemWeight":  8.8, "basePrice": 3200.00},
    {"itemName": "18K Gold Chain Necklace",     "itemCategory": "NECKLACE", "itemWeight": 15.3, "basePrice":  980.00},
    {"itemName": "Silver Pendant Set",          "itemCategory": "NECKLACE", "itemWeight": 10.0, "basePrice":  220.00},
    {"itemName": "Diamond Stud Earrings",       "itemCategory": "EARRING",  "itemWeight":  2.4, "basePrice": 1800.00},
    {"itemName": "22K Gold Mangalsutra",        "itemCategory": "NECKLACE", "itemWeight": 20.0, "basePrice": 1400.00},
    {"itemName": "Sapphire Gold Ring",          "itemCategory": "RING",     "itemWeight":  4.5, "basePrice":  760.00},
    {"itemName": "Gold Kada Bracelet",          "itemCategory": "BRACELET", "itemWeight": 30.0, "basePrice": 1650.00},
    {"itemName": "Pearl Necklace Set",          "itemCategory": "NECKLACE", "itemWeight": 25.0, "basePrice":  450.00},
    {"itemName": "Platinum Diamond Ring",       "itemCategory": "RING",     "itemWeight":  6.0, "basePrice": 5500.00},
    {"itemName": "Gold Nose Pin",               "itemCategory": "EARRING",  "itemWeight":  1.2, "basePrice":   95.00},
    {"itemName": "18K Gold Ear Drops",          "itemCategory": "EARRING",  "itemWeight":  5.5, "basePrice":  480.00},
    {"itemName": "Diamond Halo Ring",           "itemCategory": "RING",     "itemWeight":  4.0, "basePrice": 3800.00},
    {"itemName": "Gold Temple Necklace",        "itemCategory": "NECKLACE", "itemWeight": 35.0, "basePrice": 2100.00},
    {"itemName": "Rose Gold Chain Bracelet",    "itemCategory": "BRACELET", "itemWeight":  9.0, "basePrice":  540.00},
    {"itemName": "Silver Cufflinks",            "itemCategory": "BRACELET", "itemWeight":  8.0, "basePrice":  140.00},
    {"itemName": "Emerald Gold Ring",           "itemCategory": "RING",     "itemWeight":  5.0, "basePrice": 2800.00},
    {"itemName": "Gold Choker Necklace",        "itemCategory": "NECKLACE", "itemWeight": 28.0, "basePrice": 1900.00},
    {"itemName": "Ruby Stud Earrings",          "itemCategory": "EARRING",  "itemWeight":  3.0, "basePrice": 1200.00},
    {"itemName": "Silver Charm Bracelet",       "itemCategory": "BRACELET", "itemWeight": 12.0, "basePrice":  320.00},
    {"itemName": "White Gold Band Ring",        "itemCategory": "RING",     "itemWeight":  4.2, "basePrice":  890.00},
    {"itemName": "Gold Layered Necklace",       "itemCategory": "NECKLACE", "itemWeight": 18.0, "basePrice":  750.00},
    {"itemName": "Hoop Earrings Gold",          "itemCategory": "EARRING",  "itemWeight":  6.0, "basePrice":  380.00},
    {"itemName": "Diamond Eternity Ring",       "itemCategory": "RING",     "itemWeight":  3.8, "basePrice": 6200.00},
    {"itemName": "Gold Bead Bracelet",          "itemCategory": "BRACELET", "itemWeight": 14.0, "basePrice":  420.00},
    {"itemName": "Antique Silver Necklace",     "itemCategory": "NECKLACE", "itemWeight": 22.0, "basePrice":  610.00},
]

CUSTOMERS = [
    "Priya Sharma",    "Ravi Kumar",      "Anita Nair",      "David Thomas",
    "Meena Patel",     "Sarah Johnson",   "Kiran Reddy",     "Lisa Chan",
    "Rajesh Iyer",     "Emily Wilson",    "Arun Pillai",     "Nadia Hassan",
    "Tom Bradley",     "Kavitha Rao",     "James Park",      "Divya Krishnan",
    "Michael Brown",   "Sunita Verma",    "Lena Fischer",    "Carlos Reyes",
    "Ayesha Khan",     "Yuki Tanaka",     "Omar Sheikh",     "Fatima Al-Rashid",
    "George Mitchell", "Preethi Menon",   "Daniel Okonkwo",  "Sofia Rossi",
    "Haruto Yamamoto", "Isabella Costa",  "Samuel Osei",     "Zara Ahmed",
    "Patrick O'Brien", "Mei Lin",         "Arjun Kapoor",    "Elena Petrov",
    "Hassan Al-Farsi", "Chloe Dupont",    "Vikram Singh",    "Amara Diallo",
]

PAYMENT_WEIGHTS = [0.55, 0.45]

counter = 100


def next_invoice():
    global counter
    counter += 1
    return f"INV-G{counter:04d}"


def pick_unique_combo():
    attempts = 0
    while attempts < 50:
        customer = random.choice(CUSTOMERS)
        product  = random.choice(CATALOGUE)
        combo    = f"{customer}:{product['itemName']}"
        if combo not in recent_combos:
            recent_combos.append(combo)
            return customer, product
        attempts += 1
    return random.choice(CUSTOMERS), random.choice(CATALOGUE)


def vary_price(base_price):
    return round(base_price * random.uniform(0.95, 1.05), 2)


def vary_weight(base_weight):
    return round(base_weight * random.uniform(0.97, 1.03), 3)


def build_sale():
    customer, product = pick_unique_combo()
    return {
        "invoiceNumber": next_invoice(),
        "customerName":  customer,
        "itemName":      product["itemName"],
        "itemCategory":  product["itemCategory"],
        "itemWeight":    vary_weight(product["itemWeight"]),
        "price":         vary_price(product["basePrice"]),
        "paymentMethod": random.choices(["CASH", "CARD"], weights=PAYMENT_WEIGHTS)[0],
        "soldAt":        datetime.utcnow().strftime("%Y-%m-%dT%H:%M:%SZ"),
    }


def wait_for_api(retries=20, delay=5):
    print(f"Waiting for API at {ENDPOINT}...")
    for i in range(1, retries + 1):
        try:
            r = requests.get(f"{API_URL}/api/sales", timeout=3)
            if r.status_code < 500:
                print(f"API ready (attempt {i})\n")
                return True
        except requests.exceptions.RequestException:
            pass
        print(f"  Attempt {i}/{retries} — retrying in {delay}s...")
        time.sleep(delay)
    return False


def main():
    if not wait_for_api():
        print("API unavailable. Exiting.")
        return

    print(f"Posting one sale every {INTERVAL}s  (Ctrl+C to stop)\n")

    while True:
        sale = build_sale()
        try:
            r = requests.post(ENDPOINT, json=sale, timeout=5)
            if r.status_code == 201:
                d = r.json()
                print(f"[OK]  {d['invoiceNumber']:<14} | {d['customerName']:<22} | "
                      f"{d['itemName']:<32} | {d['itemWeight']:>7}g | "
                      f"£{d['price']:>8.2f} | {d['paymentMethod']}")
            else:
                print(f"[ERR] HTTP {r.status_code} — {r.text[:100]}")
        except requests.exceptions.RequestException as e:
            print(f"[ERR] {e}")
        time.sleep(INTERVAL)


if __name__ == "__main__":
    main()