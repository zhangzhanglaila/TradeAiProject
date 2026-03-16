export function formatAmount(amount, digits = 2) {
  const n = Number(amount)
  if (!Number.isFinite(n)) {
    return (0).toLocaleString('zh-CN', {
      minimumFractionDigits: digits,
      maximumFractionDigits: digits
    })
  }

  return n.toLocaleString('zh-CN', {
    minimumFractionDigits: digits,
    maximumFractionDigits: digits
  })
}
