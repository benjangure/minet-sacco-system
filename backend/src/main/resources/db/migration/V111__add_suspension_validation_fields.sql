-- Add validation fields to member_suspensions table
ALTER TABLE member_suspensions 
ADD COLUMN validated_by BIGINT,
ADD COLUMN validation_notes TEXT,
ADD COLUMN validated_at TIMESTAMP NULL DEFAULT NULL;

-- Add foreign key constraint for validated_by
ALTER TABLE member_suspensions 
ADD CONSTRAINT fk_suspension_validated_by 
FOREIGN KEY (validated_by) REFERENCES users(id);
