ALTER TABLE pos_invoice
ADD COLUMN IF NOT EXISTS voided_at timestamp null,
ADD COLUMN IF NOT EXISTS voided_by varchar(100) null,
ADD COLUMN IF NOT EXISTS void_reason text null,
ADD COLUMN IF NOT EXISTS refund_amount numeric(12, 2) not null default 0;
