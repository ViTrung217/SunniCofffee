-- Remove obsolete approval wording from existing default catalogue descriptions.
-- Preserve custom text, prices, inventory and uploaded images.
UPDATE products
SET description = REPLACE(description,
    '; tên, giá, tồn và thuộc tính chưa được cửa hàng xác nhận.', '.'),
    version = version + 1
WHERE sku LIKE 'DEMO-%'
  AND description LIKE '%; tên, giá, tồn và thuộc tính chưa được cửa hàng xác nhận.%';

UPDATE categories
SET description = 'Danh mục SuniCoffee.'
WHERE code IN ('COFFEE', 'MACHINE', 'BREWING', 'OUTDOOR')
  AND description = 'Danh mục dữ liệu phục vụ đồ án, chưa được cửa hàng xác nhận.';
