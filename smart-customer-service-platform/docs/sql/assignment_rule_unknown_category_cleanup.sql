-- 停用早期测试阶段创建的 UNKNOWN 分类派单规则。
-- 当前受控分类仅允许 ORDER、PAYMENT、REFUND、LOGISTICS、ACCOUNT、
-- TECHNICAL、COMPLAINT、GENERAL；继续启用 UNKNOWN 会导致审核通过的工单无法匹配规则。
UPDATE assignment_rule
SET enabled = 0
WHERE category = 'UNKNOWN'
  AND enabled = 1;
