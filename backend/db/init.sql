-- docker-entrypoint-initdb.dがこのファイルを流し込む際のクライアント文字コードが
-- latin1になり、UTF-8の日本語リテラルが二重エンコードされて文字化けするため明示する。
SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS cooking_records (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    dish_name VARCHAR(255) NOT NULL,
    recipe_url VARCHAR(1024) NOT NULL,
    cooked_on DATE NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_cooked_on (cooked_on)
);

-- デモ用サンプルデータ(数件の完成記録を初期投入し、履歴画面が空にならないようにする)
INSERT INTO cooking_records (dish_name, recipe_url, cooked_on) VALUES
    ('肉じゃが', 'https://cookpad.com/jp/search/%E8%82%89%E3%81%98%E3%82%83%E3%81%8C', '2026-09-08'),
    ('オムライス', 'https://cookpad.com/jp/search/%E3%82%AA%E3%83%A0%E3%83%A9%E3%82%A4%E3%82%B9', '2026-09-10'),
    ('麻婆豆腐', 'https://cookpad.com/jp/search/%E9%BA%BB%E5%A9%86%E8%B1%86%E8%85%90', '2026-09-12'),
    ('鮭の塩焼き定食', 'https://cookpad.com/jp/search/%E9%AE%AD%E3%81%AE%E5%A1%A9%E7%84%BC%E3%81%8D%E5%AE%9A%E9%A3%9F', '2026-09-14'),
    ('野菜炒め', 'https://cookpad.com/jp/search/%E9%87%8E%E8%8F%9C%E7%82%92%E3%82%81', '2026-09-15'),
    ('卵焼きと味噌汁', 'https://cookpad.com/jp/search/%E5%8D%B5%E7%84%BC%E3%81%8D%E3%81%A8%E5%91%B3%E5%99%8C%E6%B1%81', '2026-09-16');
