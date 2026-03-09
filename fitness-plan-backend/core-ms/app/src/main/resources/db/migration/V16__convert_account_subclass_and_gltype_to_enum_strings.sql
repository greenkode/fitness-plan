ALTER TABLE account ALTER COLUMN subclass TYPE varchar(10);

UPDATE account SET subclass = CASE subclass
    WHEN 'C' THEN 'COMPOSITE'
    WHEN 'F' THEN 'FINAL'
    ELSE 'BASE'
END WHERE subclass IN (' ', 'C', 'F');

ALTER TABLE account ALTER COLUMN subclass SET DEFAULT 'BASE';

ALTER TABLE account ALTER COLUMN gl_type TYPE varchar(10) USING CASE gl_type
    WHEN 1 THEN 'DEBIT'
    WHEN 2 THEN 'CREDIT'
    ELSE 'UNDEFINED'
END;

ALTER TABLE account ALTER COLUMN gl_type SET DEFAULT 'UNDEFINED';
