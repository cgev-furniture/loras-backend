CREATE TABLE categories (
  id         BIGSERIAL PRIMARY KEY,
  name       VARCHAR(100) NOT NULL UNIQUE,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE projects (
  id          BIGSERIAL PRIMARY KEY,
  title       VARCHAR(255) NOT NULL,
  slug        VARCHAR(255) NOT NULL UNIQUE,
  description TEXT,
  published   BOOLEAN NOT NULL DEFAULT FALSE,
  featured    BOOLEAN NOT NULL DEFAULT FALSE,
  video_url   VARCHAR(500),
  created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  deleted_at  TIMESTAMPTZ
);

CREATE INDEX idx_projects_slug      ON projects(slug);
CREATE INDEX idx_projects_published ON projects(published);
CREATE INDEX idx_projects_featured  ON projects(featured);

CREATE TABLE project_images (
  id          BIGSERIAL PRIMARY KEY,
  project_id  BIGINT NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
  object_path VARCHAR(500) NOT NULL,
  sort_order  INT NOT NULL DEFAULT 0,
  created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_project_images_project_id ON project_images(project_id);

CREATE TABLE project_categories (
  project_id  BIGINT NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
  category_id BIGINT NOT NULL REFERENCES categories(id),
  PRIMARY KEY (project_id, category_id)
);

CREATE TABLE admin_users (
  id                   BIGSERIAL PRIMARY KEY,
  username             VARCHAR(100) NOT NULL UNIQUE,
  password_hash        VARCHAR(255) NOT NULL,
  must_change_password BOOLEAN NOT NULL DEFAULT TRUE,
  created_at           TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE contact_inquiries (
  id              BIGSERIAL PRIMARY KEY,
  name            VARCHAR(255) NOT NULL,
  email           VARCHAR(255) NOT NULL,
  phone           VARCHAR(50),
  message         TEXT NOT NULL,
  email_delivered BOOLEAN NOT NULL DEFAULT FALSE,
  read            BOOLEAN NOT NULL DEFAULT FALSE,
  submitted_at    TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
