CREATE TABLE IF NOT EXISTS sales (
                                     id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    invoice_number VARCHAR(20)   NOT NULL UNIQUE,
    customer_name  VARCHAR(100)  NOT NULL,
    item_name      VARCHAR(100)  NOT NULL,
    item_category  VARCHAR(50)   NOT NULL,
    item_weight    NUMERIC(8,3)  NOT NULL,
    price          NUMERIC(12,2) NOT NULL,
    payment_method VARCHAR(10)   NOT NULL CHECK (payment_method IN ('CASH','CARD')),
    sold_at        TIMESTAMPTZ   NOT NULL DEFAULT NOW()
    );

CREATE TABLE IF NOT EXISTS daily_reports (
                                             id                 BIGSERIAL PRIMARY KEY,
                                             report_date        DATE          NOT NULL UNIQUE,
                                             total_transactions INT           NOT NULL DEFAULT 0,
                                             total_revenue      NUMERIC(14,2) NOT NULL DEFAULT 0,
    cash_revenue       NUMERIC(14,2) NOT NULL DEFAULT 0,
    card_revenue       NUMERIC(14,2) NOT NULL DEFAULT 0,
    generated_at       TIMESTAMPTZ   NOT NULL DEFAULT NOW()
    );

CREATE INDEX idx_sales_sold_at ON sales(sold_at);

INSERT INTO sales (invoice_number, customer_name, item_name, item_category, item_weight, price, payment_method, sold_at) VALUES
                                                                                                                             ('INV-001', 'Priya Sharma',   '22K Gold Bangle',        'BRACELET', 22.5,  1250.00, 'CASH', NOW() - INTERVAL '8 days'),
                                                                                                                             ('INV-002', 'Ravi Kumar',     'Diamond Solitaire Ring',  'RING',      3.2,  4500.00, 'CARD', NOW() - INTERVAL '8 days'),
                                                                                                                             ('INV-003', 'Anita Nair',     'Silver Anklet Pair',      'BRACELET', 18.0,   180.00, 'CASH', NOW() - INTERVAL '7 days'),
                                                                                                                             ('INV-004', 'David Thomas',   'Platinum Wedding Band',   'RING',      5.5,  1700.00, 'CARD', NOW() - INTERVAL '7 days'),
                                                                                                                             ('INV-005', 'Meena Patel',    'Gold Jhumka Earrings',    'EARRING',  12.0,   620.00, 'CASH', NOW() - INTERVAL '6 days'),
                                                                                                                             ('INV-006', 'Sarah Johnson',  'Diamond Tennis Bracelet', 'BRACELET',  8.8,  3200.00, 'CARD', NOW() - INTERVAL '5 days'),
                                                                                                                             ('INV-007', 'Kiran Reddy',    '18K Gold Chain Necklace', 'NECKLACE', 15.3,   980.00, 'CASH', NOW() - INTERVAL '4 days'),
                                                                                                                             ('INV-008', 'Lisa Chan',      'Diamond Stud Earrings',   'EARRING',   2.4,  1800.00, 'CARD', NOW() - INTERVAL '3 days'),
                                                                                                                             ('INV-009', 'Rajesh Iyer',    '22K Gold Mangalsutra',    'NECKLACE', 20.0,  1400.00, 'CASH', NOW() - INTERVAL '2 days'),
                                                                                                                             ('INV-010', 'Tom Bradley',    'Platinum Diamond Ring',   'RING',      6.0,  5500.00, 'CARD', NOW() - INTERVAL '1 day'),
                                                                                                                             ('INV-011', 'Divya Krishnan', '18K Gold Ear Drops',      'EARRING',   5.5,   480.00, 'CASH', NOW()),
                                                                                                                             ('INV-012', 'Michael Brown',  'Diamond Halo Ring',       'RING',      4.0,  3800.00, 'CARD', NOW());