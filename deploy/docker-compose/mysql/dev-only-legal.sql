-- Development fixtures only. NOT a production legal document or a Flyway migration.
INSERT INTO legal_document(document_type,version,title,body,content_hash,status,effective_at)
VALUES
('TERMS','dev-2026-10-21','开发测试用户协议',
 '仅用于 FaFa 本地接口测试，不可作为正式用户协议发布。',
 SHA2('仅用于 FaFa 本地接口测试，不可作为正式用户协议发布。',256),'PUBLISHED','2026-01-01 00:00:00'),
('PRIVACY','dev-2026-10-21','开发测试隐私说明',
 '仅用于 FaFa 本地接口测试，不可作为正式隐私政策发布。',
 SHA2('仅用于 FaFa 本地接口测试，不可作为正式隐私政策发布。',256),'PUBLISHED','2026-01-01 00:00:00');
