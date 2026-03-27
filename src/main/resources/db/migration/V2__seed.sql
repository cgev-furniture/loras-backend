INSERT INTO categories (name) VALUES
  ('Arts'),
  ('Kitchen'),
  ('Home Furniture'),
  ('Outdoor Advertising'),
  ('Laser Cutting'),
  ('3D Printing'),
  ('Custom Orders');

-- bcrypt hash of 'loras2024' (strength 12)
INSERT INTO admin_users (username, password_hash, must_change_password) VALUES
  ('admin', '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewYpfQN7cSIBiVTe', true);
