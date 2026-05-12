const DANGEROUS_TAGS = /<\s*(script|iframe|object|embed|applet|link|meta|base)\b[^>]*>[\s\S]*?<\s*\/\s*\1\s*>/gi;
const SELF_CLOSING_DANGEROUS = /<\s*(script|iframe|object|embed|applet|link|meta|base)\b[^>]*\/?>/gi;
const EVENT_HANDLERS = /\s+on\w+\s*=\s*(?:"[^"]*"|'[^']*'|[^\s>]+)/gi;
const JAVASCRIPT_URL = /\s+href\s*=\s*["']\s*javascript\s*:/gi;
const JAVASCRIPT_SRC = /\s+src\s*=\s*["']\s*javascript\s*:/gi;

export function sanitizeHtml(html: string): string {
  if (!html) return '';
  return html
    .replace(DANGEROUS_TAGS, '')
    .replace(SELF_CLOSING_DANGEROUS, '')
    .replace(EVENT_HANDLERS, '')
    .replace(JAVASCRIPT_URL, ' href="javascript:void(0)"')
    .replace(JAVASCRIPT_SRC, ' src=""');
}
