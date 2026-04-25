CREATE TABLE clients (
    id         UUID      PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id    UUID      NOT NULL UNIQUE,
    status     VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'INACTIVE', 'ARCHIVED')),
    joined_at  TIMESTAMP NOT NULL DEFAULT NOW(),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE plans (
    id          UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    client_id   UUID        NOT NULL REFERENCES clients(id),
    title       VARCHAR(255) NOT NULL,
    description TEXT,
    status      VARCHAR(20)  NOT NULL DEFAULT 'DRAFT' CHECK (status IN ('DRAFT', 'ACTIVE', 'COMPLETED', 'ARCHIVED')),
    created_at  TIMESTAMP   NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP   NOT NULL DEFAULT NOW()
);

CREATE TABLE phases (
    id          UUID      PRIMARY KEY DEFAULT gen_random_uuid(),
    plan_id     UUID      NOT NULL REFERENCES plans(id),
    title       VARCHAR(255) NOT NULL,
    order_index INTEGER   NOT NULL DEFAULT 0,
    start_date  DATE,
    end_date    DATE,
    created_at  TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT phases_order_unique UNIQUE (plan_id, order_index)
);

CREATE TABLE tasks (
    id              UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    phase_id        UUID        NOT NULL REFERENCES phases(id),
    title           VARCHAR(255) NOT NULL,
    description     TEXT,
    recurring       BOOLEAN     NOT NULL DEFAULT FALSE,
    recurrence_type VARCHAR(10) NOT NULL DEFAULT 'NONE' CHECK (recurrence_type IN ('DAILY', 'WEEKLY', 'NONE')),
    due_date        DATE,
    created_at      TIMESTAMP   NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP   NOT NULL DEFAULT NOW()
);

CREATE TABLE task_completions (
    id           UUID      PRIMARY KEY DEFAULT gen_random_uuid(),
    task_id      UUID      NOT NULL REFERENCES tasks(id),
    client_id    UUID      NOT NULL REFERENCES clients(id),
    completed_at TIMESTAMP NOT NULL DEFAULT NOW(),
    note         TEXT,
    created_at   TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT task_completions_unique_per_day UNIQUE (task_id, client_id, (completed_at::DATE))
);

CREATE TABLE metrics (
    id         UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    name       VARCHAR(100) NOT NULL,
    unit       VARCHAR(50),
    created_by UUID        NOT NULL,
    created_at TIMESTAMP   NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP   NOT NULL DEFAULT NOW(),
    CONSTRAINT metrics_name_unique UNIQUE (name, created_by)
);

CREATE TABLE progress_logs (
    id         UUID           PRIMARY KEY DEFAULT gen_random_uuid(),
    client_id  UUID           NOT NULL REFERENCES clients(id),
    metric_id  UUID           NOT NULL REFERENCES metrics(id),
    date       DATE           NOT NULL,
    value      NUMERIC(10, 2) NOT NULL,
    note       TEXT,
    created_at TIMESTAMP      NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP      NOT NULL DEFAULT NOW(),
    CONSTRAINT progress_logs_unique_per_day UNIQUE (client_id, metric_id, date)
);

CREATE TABLE coach_notes (
    id          UUID      PRIMARY KEY DEFAULT gen_random_uuid(),
    client_id   UUID      NOT NULL REFERENCES clients(id),
    authored_by UUID      NOT NULL,
    content     TEXT      NOT NULL,
    is_private  BOOLEAN   NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Indexes
CREATE INDEX idx_plans_client_id       ON plans(client_id);
CREATE INDEX idx_phases_plan_id        ON phases(plan_id);
CREATE INDEX idx_tasks_phase_id        ON tasks(phase_id);
CREATE INDEX idx_task_completions_task_id   ON task_completions(task_id);
CREATE INDEX idx_task_completions_client_id ON task_completions(client_id);
CREATE INDEX idx_progress_logs_client_id    ON progress_logs(client_id);
CREATE INDEX idx_progress_logs_date         ON progress_logs(date);
CREATE INDEX idx_coach_notes_client_id      ON coach_notes(client_id);