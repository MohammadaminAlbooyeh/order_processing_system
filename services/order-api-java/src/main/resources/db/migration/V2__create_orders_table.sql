CREATE TABLE orders (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'pending',
    destination VARCHAR(500),
    weight DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    items JSONB NOT NULL DEFAULT '[]',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX ix_orders_id ON orders (id);
CREATE INDEX ix_orders_user_id ON orders (user_id);
