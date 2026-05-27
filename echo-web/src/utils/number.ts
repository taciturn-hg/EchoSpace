/**
 * 大于等于 10000 时转为 "x.xw" 格式（保留一位小数，去掉末尾 0）
 * <p>例如：12345 → "1.2w"，10000 → "1w"，9999 → "9999"</p>
 */
export function formatCount(n: number): string {
  if (n >= 10000) {
    const v = n / 10000
    return (v % 1 === 0 ? v.toFixed(0) : v.toFixed(1)) + 'w'
  }
  return String(n)
}
