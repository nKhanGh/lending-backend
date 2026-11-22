INSERT INTO credit_accounts (id, user_id, total_limit, outstanding_amount, reserved_amount, currency, version)
VALUES ('00000000-0000-0000-0000-000000000101',
        '00000000-0000-0000-0000-000000000001',
        500.00, 0.00, 0.00, 'SGD', 0);

INSERT INTO products (id, name, description, price, currency, active) VALUES
('00000000-0000-0000-0000-000000000201', 'Wireless Headphones', 'Demo product for the SGD 120 PayLater flow', 120.00, 'SGD', TRUE),
('00000000-0000-0000-0000-000000000202', 'Mechanical Keyboard', 'Compact hot-swappable keyboard', 100.01, 'SGD', TRUE),
('00000000-0000-0000-0000-000000000203', 'Smart Watch', 'Fitness and notification watch', 180.00, 'SGD', TRUE),
('00000000-0000-0000-0000-000000000204', 'Portable Speaker', 'Water-resistant Bluetooth speaker', 75.50, 'SGD', TRUE),
('00000000-0000-0000-0000-000000000205', 'USB-C Hub', 'Seven-port travel hub', 49.90, 'SGD', TRUE),
('00000000-0000-0000-0000-000000000206', 'E-reader', 'Paper-like reading display', 220.00, 'SGD', TRUE),
('00000000-0000-0000-0000-000000000207', 'Webcam', 'Full HD conferencing camera', 89.00, 'SGD', TRUE),
('00000000-0000-0000-0000-000000000208', 'Laptop Stand', 'Adjustable aluminium stand', 39.90, 'SGD', TRUE);
