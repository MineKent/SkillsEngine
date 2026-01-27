#!/usr/bin/env node
/**
 * GitBook Cloud Space sync (Markdown -> pages) from local docs markdown files (e.g. `docs/guide/install.md`).
 *
 * Mapping examples:
 *   docs/guide/install.md -> /guide/install
 *   docs/README.md       -> /
 *   docs/<folder>/README.md or docs/<folder>/index.md -> /<folder>
 *
 * Usage:
 *   GITBOOK_TOKEN=... GITBOOK_SPACE_ID=... node scripts/gitbook-sync.mjs --dry-run
 *   GITBOOK_TOKEN=... GITBOOK_SPACE_ID=... node scripts/gitbook-sync.mjs --apply
 *
 * Env:
 *   GITBOOK_TOKEN         (required) Personal Access Token
 *   GITBOOK_SPACE_ID      (required) Space ID
 *   GITBOOK_API_BASE      (optional) default: https://api.gitbook.com/v1
 *   GITBOOK_DOCS_DIR      (optional) default: docs
 *   GITBOOK_REQUEST_DELAY (optional) ms between write requests, default: 150
 */

import fs from 'node:fs/promises';
import path from 'node:path';

const DEFAULT_API_BASE = 'https://api.gitbook.com/v1';

function parseArgs(argv) {
  const args = new Set(argv);
  const apply = args.has('--apply');
  const dryRun = args.has('--dry-run') || !apply;

  if (apply && args.has('--dry-run')) {
    throw new Error('Use either --apply or --dry-run (not both)');
  }

  return {
    apply,
    dryRun,
    verbose: args.has('--verbose'),
    help: args.has('--help') || args.has('-h'),
    localCheck: args.has('--local-check'),
  };
}

function printHelp() {
  console.log(`gitbook-sync (GitBook Cloud)

Sync local Markdown files to a GitBook Space by path.

Usage:
  GITBOOK_TOKEN=... GITBOOK_SPACE_ID=... node scripts/gitbook-sync.mjs --dry-run [--verbose]
  GITBOOK_TOKEN=... GITBOOK_SPACE_ID=... node scripts/gitbook-sync.mjs --apply [--verbose]

Options:
  --dry-run      Do not write anything (default)
  --apply        Create/update pages
  --verbose      Print per-page actions and endpoint errors while probing
  --local-check  Only print detected files/paths/titles (no API calls)
  --help,-h      Show this help

Env vars:
  GITBOOK_TOKEN         Personal Access Token (required)
  GITBOOK_SPACE_ID      Target space ID (required)
  GITBOOK_API_BASE      API base (optional, default: https://api.gitbook.com/v1)
  GITBOOK_DOCS_DIR      Local docs folder (optional, default: docs)
  GITBOOK_REQUEST_DELAY Delay between write requests in ms (optional, default: 150)
`);
}

function requireEnv(name) {
  const v = process.env[name];
  if (!v) throw new Error(`Missing required env var ${name}`);
  return v;
}

function sleep(ms) {
  return new Promise((r) => setTimeout(r, ms));
}

function normalizeSlashes(p) {
  return p.replace(/\\/g, '/');
}

function toGitBookPath(docsDir, filePath) {
  const rel = normalizeSlashes(path.relative(docsDir, filePath));
  if (!rel || rel.startsWith('..')) throw new Error(`File is outside docs dir: ${filePath}`);

  const noExt = rel.replace(/\.md$/i, '');
  const parts = noExt.split('/').filter(Boolean);

  // README.md at root -> /
  // any */README.md or */index.md -> /<dir>
  if (parts.length === 1 && (parts[0].toLowerCase() === 'readme' || parts[0].toLowerCase() === 'index')) {
    return '/';
  }
  const last = parts[parts.length - 1]?.toLowerCase();
  if (last === 'readme' || last === 'index') {
    parts.pop();
  }

  return '/' + parts.join('/');
}

function titleFromMarkdown(fileName, markdown) {
  // First ATX h1
  const m = markdown.match(/^#\s+(.+)$/m);
  if (m?.[1]) return m[1].trim();

  // Fallback: file base
  const base = fileName.replace(/\.md$/i, '');
  return base
    .replace(/[-_]/g, ' ')
    .replace(/\b\w/g, (c) => c.toUpperCase())
    .trim();
}

async function listMarkdownFiles(rootDir) {
  const out = [];
  async function walk(dir) {
    const entries = await fs.readdir(dir, { withFileTypes: true });
    for (const e of entries) {
      // skip hidden folders like .git
      if (e.isDirectory() && e.name.startsWith('.')) continue;
      if (e.isDirectory() && e.name === 'target') continue;

      const full = path.join(dir, e.name);
      if (e.isDirectory()) {
        await walk(full);
      } else if (e.isFile() && e.name.toLowerCase().endsWith('.md')) {
        out.push(full);
      }
    }
  }
  await walk(rootDir);
  out.sort();
  return out;
}

async function gitbookFetch(apiBase, token, method, urlPath, body, { verbose } = {}) {
  const url = apiBase.replace(/\/$/, '') + urlPath;
  const headers = {
    Authorization: `Bearer ${token}`,
    'Content-Type': 'application/json',
    Accept: 'application/json',
  };

  const res = await fetch(url, {
    method,
    headers,
    body: body ? JSON.stringify(body) : undefined,
  });

  const text = await res.text();
  let json;
  try {
    json = text ? JSON.parse(text) : null;
  } catch {
    json = { raw: text };
  }

  if (!res.ok) {
    const msg = json?.message || json?.error || text || `HTTP ${res.status}`;
    const err = new Error(`GitBook API error ${method} ${urlPath}: ${msg}`);
    err.status = res.status;
    err.response = json;
    err.debug = { url, method, body, response: json }; // always include debug
    throw err;
  }

  return json;
}

function extractPagesFromTree(treeJson) {
  // Tries to be tolerant to different API shapes.
  // We want items that look like: { id, path } or { id, slug } etc.
  const pages = [];
  const seen = new Set();

  function visit(node) {
    if (!node || typeof node !== 'object') return;

    const id = node.id || node.pageId || node.entityId;
    const p = node.path || node.href || node.slug;

    if (id && p && !seen.has(String(id))) {
      pages.push({ id: String(id), path: String(p).startsWith('/') ? String(p) : '/' + String(p) });
      seen.add(String(id));
    }

    const children = node.pages || node.children || node.items || node.nodes;
    if (Array.isArray(children)) {
      for (const c of children) visit(c);
    }

    // Sometimes wrapped
    if (node.page) visit(node.page);
  }

  visit(treeJson);
  if (Array.isArray(treeJson)) treeJson.forEach(visit);
  if (treeJson?.items && Array.isArray(treeJson.items)) treeJson.items.forEach(visit);

  return pages;
}

async function fetchExistingPages({ apiBase, token, spaceId, verbose }) {
  const candidates = [
    `/spaces/${encodeURIComponent(spaceId)}/content`,
    `/spaces/${encodeURIComponent(spaceId)}/content/pages`,
    `/spaces/${encodeURIComponent(spaceId)}/pages`,
  ];

  let lastErr;
  for (const p of candidates) {
    try {
      const json = await gitbookFetch(apiBase, token, 'GET', p, null, { verbose });
      const pages = extractPagesFromTree(json);
      if (pages.length > 0) return pages;
      // Even if empty, it might be valid.
      return pages;
    } catch (e) {
      lastErr = e;
      if (verbose) {
        console.error(`[gitbook-sync] List pages failed for ${p}: ${e.message}`);
      }
    }
  }
  throw lastErr || new Error('Unable to list pages (no endpoint worked)');
}

async function createPage({ apiBase, token, spaceId, page }) {
  // We try a couple of commonly-used shapes for GitBook.
  const payloads = [
    { title: page.title, path: page.path, markdown: page.markdown },
    { title: page.title, path: page.path, content: page.markdown, format: 'markdown' },
    { title: page.title, path: page.path, document: { type: 'markdown', content: page.markdown } },
  ];

  const endpoints = [
    `/spaces/${encodeURIComponent(spaceId)}/content/page`,
    `/spaces/${encodeURIComponent(spaceId)}/pages`,
  ];

  let lastErr;
  for (const endpoint of endpoints) {
    for (const body of payloads) {
      try {
        return await gitbookFetch(apiBase, token, 'POST', endpoint, body);
      } catch (e) {
        lastErr = e;
        // Continue trying variants
      }
    }
  }
  throw lastErr || new Error('Unable to create page');
}

async function updatePage({ apiBase, token, spaceId, pageId, page }) {
  const payloads = [
    { title: page.title, path: page.path, markdown: page.markdown },
    { title: page.title, path: page.path, content: page.markdown, format: 'markdown' },
    { title: page.title, path: page.path, document: { type: 'markdown', content: page.markdown } },
  ];

  const endpoints = [
    `/spaces/${encodeURIComponent(spaceId)}/content/page/${encodeURIComponent(pageId)}`,
    `/spaces/${encodeURIComponent(spaceId)}/pages/${encodeURIComponent(pageId)}`,
  ];

  let lastErr;
  for (const endpoint of endpoints) {
    for (const body of payloads) {
      try {
        return await gitbookFetch(apiBase, token, 'PATCH', endpoint, body);
      } catch (e) {
        lastErr = e;
      }
    }
  }
  throw lastErr || new Error('Unable to update page');
}

async function main() {
  const { apply, dryRun, verbose, help, localCheck } = parseArgs(process.argv.slice(2));

  if (help) {
    printHelp();
    return;
  }

  const apiBase = process.env.GITBOOK_API_BASE || DEFAULT_API_BASE;
  const docsDir = process.env.GITBOOK_DOCS_DIR || 'docs';

  if (localCheck) {
    const docsAbs = path.resolve(docsDir);
    const files = await listMarkdownFiles(docsAbs);
    const computed = [];
    for (const f of files) {
      const markdown = await fs.readFile(f, 'utf8');
      computed.push({
        file: path.relative(process.cwd(), f),
        path: toGitBookPath(docsAbs, f),
        title: titleFromMarkdown(path.basename(f), markdown),
      });
    }

    console.log(JSON.stringify(computed, null, 2));
    return;
  }

  const token = requireEnv('GITBOOK_TOKEN');
  const spaceId = requireEnv('GITBOOK_SPACE_ID');
  const requestDelay = Number.parseInt(process.env.GITBOOK_REQUEST_DELAY || '150', 10);

  const docsAbs = path.resolve(docsDir);
  const files = await listMarkdownFiles(docsAbs);

  const desired = [];
  for (const f of files) {
    const markdown = await fs.readFile(f, 'utf8');
    const pagePath = toGitBookPath(docsAbs, f);
    const title = titleFromMarkdown(path.basename(f), markdown);
    desired.push({ file: f, path: pagePath, title, markdown });
  }

  console.log(`[gitbook-sync] Found ${desired.length} markdown files under ${docsDir}`);
  if (dryRun) console.log('[gitbook-sync] DRY RUN (no writes). Use --apply to write changes.');

  // Listing existing pages is safe even in dry-run.
  const existing = await fetchExistingPages({ apiBase, token, spaceId, verbose });
  const existingByPath = new Map(existing.map((p) => [p.path, p]));

  const toCreate = [];
  const toUpdate = [];

  for (const d of desired) {
    const ex = existingByPath.get(d.path);
    if (!ex) toCreate.push(d);
    else toUpdate.push({ ...d, id: ex.id });
  }

  console.log(`[gitbook-sync] Existing pages discovered: ${existing.length}`);
  console.log(`[gitbook-sync] Would create: ${toCreate.length}`);
  console.log(`[gitbook-sync] Would update (by path match): ${toUpdate.length}`);

  if (verbose) {
    for (const d of toCreate) console.log(`  + CREATE ${d.path} <- ${path.relative(process.cwd(), d.file)}`);
    for (const d of toUpdate) console.log(`  ~ UPDATE ${d.path} (id=${d.id}) <- ${path.relative(process.cwd(), d.file)}`);
  }

  if (!apply) return;

  for (const d of toCreate) {
    console.log(`[gitbook-sync] Creating ${d.path}`);
    await createPage({ apiBase, token, spaceId, page: d });
    await sleep(requestDelay);
  }

  for (const d of toUpdate) {
    console.log(`[gitbook-sync] Updating ${d.path} (id=${d.id})`);
    await updatePage({ apiBase, token, spaceId, pageId: d.id, page: d });
    await sleep(requestDelay);
  }

  console.log('[gitbook-sync] Done.');
}

main().catch((e) => {
  console.error('ERROR:', e?.message || e);
  if (e?.debug) {
    console.error('\n=== DEBUG INFO ===');
    console.error(JSON.stringify(e.debug, null, 2));
  }
  if (e?.response) {
    console.error('\n=== API RESPONSE ===');
    console.error(JSON.stringify(e.response, null, 2));
  }
  process.exitCode = 1;
});
