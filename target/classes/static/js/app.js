/* DocVault — app.js */
(function () {
  'use strict';

  // CSRF token from meta tags (set by Thymeleaf)
  const CSRF_TOKEN  = document.querySelector('meta[name="_csrf"]')?.content;
  const CSRF_HEADER = document.querySelector('meta[name="_csrf_header"]')?.content;

  let currentPage  = 0;
  let currentQuery = '';
  let currentType  = 'all';
  let currentCategory = 'all';
  let pendingFiles = null; // held while user fills in meta modal

  // ─── Boot ──────────────────────────────────────────
  document.addEventListener('DOMContentLoaded', () => {
    loadStats();
    loadDocuments();
    initDropZone();
  });

  // ─── Stats ─────────────────────────────────────────
  async function loadStats() {
    try {
      const data = await apiFetch('/api/documents/stats');
      document.getElementById('stat-docs').textContent  = data.totalDocuments;
      document.getElementById('stat-size').textContent  = data.totalSizeFormatted;
      document.getElementById('stat-types').textContent = data.distinctFileTypes;
    } catch (e) { /* silently skip */ }
  }

  // ─── Document list ──────────────────────────────────
  async function loadDocuments() {
    const params = new URLSearchParams({
      query: currentQuery,
      type:  currentType,
      category: currentCategory,
      page:  currentPage,
      size:  20,
    });
    try {
      const data = await apiFetch('/api/documents?' + params);
      renderDocuments(data);
    } catch (e) {
      showToast('Failed to load documents');
    }
  }

  function renderDocuments(page) {
    const list  = document.getElementById('doc-list');
    const empty = document.getElementById('empty-state');
    const pager = document.getElementById('pagination');

    if (!page.documents.length) {
      list.innerHTML = '';
      empty.style.display = 'block';
      pager.style.display = 'none';
      return;
    }

    empty.style.display = 'none';
    list.innerHTML = page.documents.map(docCard).join('');

    // Pagination
    if (page.totalPages > 1) {
      pager.style.display = 'flex';
      document.getElementById('page-info').textContent =
        `Page ${page.currentPage + 1} of ${page.totalPages}`;
      document.getElementById('prev-btn').disabled = !page.hasPrevious;
      document.getElementById('next-btn').disabled = !page.hasNext;
    } else {
      pager.style.display = 'none';
    }
  }

  function docCard(doc) {
    const icon  = categoryIcon(doc.category);
    const badge = `<span class="doc-badge badge-${doc.category}">${doc.category}</span>`;
    const folder = `<span class="folder-badge">${categoryLabel(doc.documentCategory)}</span>`;
    const date  = new Date(doc.createdAt).toLocaleDateString(undefined,
      { month: 'short', day: 'numeric', year: 'numeric' });
    const desc  = doc.description ? `· ${doc.description}` : '';

    return `
      <div class="doc-card" id="doc-${doc.id}">
        <div class="doc-icon ${doc.category}">${icon}</div>
        <div class="doc-info">
          <div class="doc-name" title="${esc(doc.originalFilename)}">${esc(doc.originalFilename)}</div>
          <div class="doc-meta">
            ${badge}
            ${folder}
            ${doc.fileSizeFormatted} · ${date} ${esc(desc)}
          </div>
        </div>
        <div class="doc-actions">
          <button class="btn-icon" onclick="downloadDoc(${doc.id})"
                  title="Download" aria-label="Download ${esc(doc.originalFilename)}">⬇️</button>
          <button class="btn-icon danger" onclick="deleteDoc(${doc.id}, '${esc(doc.originalFilename)}')"
                  title="Delete" aria-label="Delete ${esc(doc.originalFilename)}">🗑️</button>
        </div>
      </div>`;
  }

  function categoryIcon(cat) {
    return { pdf: '📄', image: '🖼️', document: '📝', spreadsheet: '📊', other: '📁' }[cat] || '📁';
  }

  function categoryLabel(category) {
    return {
      general: 'General',
      study: 'Study',
      personal: 'Personal details',
      finance: 'Finance',
      work: 'Work',
      medical: 'Medical',
      legal: 'Legal',
    }[category] || 'General';
  }

  // ─── Upload ─────────────────────────────────────────
  function initDropZone() {
    const zone  = document.getElementById('drop-zone');
    const input = document.getElementById('file-input');
    if (!zone || !input) return;

    input.addEventListener('change', e => showMeta(e.target.files));
    zone.addEventListener('dragover',  e => { e.preventDefault(); zone.classList.add('drag'); });
    zone.addEventListener('dragleave', ()  => zone.classList.remove('drag'));
    zone.addEventListener('drop',      e  => { e.preventDefault(); zone.classList.remove('drag'); showMeta(e.dataTransfer.files); });
  }

  function showMeta(files) {
    if (!files || !files.length) return;
    pendingFiles = files;
    document.getElementById('meta-category').value = 'general';
    document.getElementById('meta-description').value = '';
    document.getElementById('meta-tags').value        = '';
    document.getElementById('meta-modal').style.display = 'flex';
    document.getElementById('meta-description').focus();
  }

  window.cancelUpload = function () {
    pendingFiles = null;
    document.getElementById('meta-modal').style.display = 'none';
    document.getElementById('file-input').value = '';
  };

  window.confirmUpload = async function () {
    const description = document.getElementById('meta-description').value.trim();
    const tags        = document.getElementById('meta-tags').value.trim();
    const category    = document.getElementById('meta-category').value;
    document.getElementById('meta-modal').style.display = 'none';

    const files = Array.from(pendingFiles);
    pendingFiles = null;
    document.getElementById('file-input').value = '';

    const progress = document.getElementById('upload-progress');
    const fill     = document.getElementById('progress-fill');
    const label    = document.getElementById('progress-label');
    progress.style.display = 'block';

    let uploaded = 0;
    for (let i = 0; i < files.length; i++) {
      label.textContent = `Uploading ${i + 1} of ${files.length}: ${files[i].name}`;
      fill.style.width  = Math.round(((i + 0.5) / files.length) * 100) + '%';
      try {
        const fd = new FormData();
        fd.append('file', files[i]);
        fd.append('documentCategory', category);
        if (description) fd.append('description', description);
        if (tags)        fd.append('tags', tags);
        await apiFetch('/api/documents', { method: 'POST', body: fd });
        uploaded++;
      } catch (e) {
        showToast(`Failed to upload ${files[i].name}: ${e.message}`);
      }
    }

    fill.style.width = '100%';
    setTimeout(() => { progress.style.display = 'none'; fill.style.width = '0'; }, 600);

    if (uploaded > 0) {
      showToast(uploaded === 1 ? `"${files[0].name}" uploaded` : `${uploaded} files uploaded`);
      loadStats();
      loadDocuments();
    }
  };

  // ─── Download ────────────────────────────────────────
  window.downloadDoc = function (id) {
    try {
      const a = Object.assign(document.createElement('a'), {
        href: `/api/documents/${id}/download`,
      });
      document.body.appendChild(a);
      a.click();
      a.remove();
      showToast('Starting download');
    } catch (e) {
      showToast('Download failed: ' + e.message);
    }
  };

  // ─── Delete ──────────────────────────────────────────
  window.deleteDoc = async function (id, filename) {
    if (!confirm(`Delete "${filename}"? This cannot be undone.`)) return;
    try {
      await apiFetch(`/api/documents/${id}`, { method: 'DELETE' });
      document.getElementById(`doc-${id}`)?.remove();
      showToast(`"${filename}" deleted`);
      loadStats();
      // Refresh if list is now empty
      if (!document.querySelectorAll('.doc-card').length) loadDocuments();
    } catch (e) {
      showToast('Delete failed: ' + e.message);
    }
  };

  // ─── Search / filter ──────────────────────────────────
  let searchTimer;
  window.onSearch = function (val) {
    clearTimeout(searchTimer);
    searchTimer = setTimeout(() => { currentQuery = val; currentPage = 0; loadDocuments(); }, 350);
  };

  window.setFilter = function (type, btn) {
    currentType = type;
    currentPage = 0;
    document.querySelectorAll('.filter-tab').forEach(b => b.classList.remove('active'));
    btn.classList.add('active');
    loadDocuments();
  };

  window.setCategoryFilter = function (category) {
    currentCategory = category;
    currentPage = 0;
    loadDocuments();
  };

  window.changePage = function (delta) {
    currentPage += delta;
    if (currentPage < 0) currentPage = 0;
    loadDocuments();
    window.scrollTo({ top: 0, behavior: 'smooth' });
  };

  // ─── API helper ──────────────────────────────────────
  async function apiFetch(url, options = {}) {
    const headers = { ...options.headers };
    if (CSRF_HEADER && CSRF_TOKEN) headers[CSRF_HEADER] = CSRF_TOKEN;
    // Don't set Content-Type for FormData (browser sets it with boundary)
    if (!(options.body instanceof FormData)) headers['Content-Type'] = 'application/json';

    const res = await fetch(url, { ...options, headers });
    if (!res.ok) {
      const err = await res.json().catch(() => ({ message: res.statusText }));
      throw new Error(err.message || res.statusText);
    }
    if (res.status === 204) return null;
    return res.json();
  }

  // ─── Toast ────────────────────────────────────────────
  function showToast(msg) {
    const el = document.getElementById('toast');
    if (!el) return;
    el.textContent = msg;
    el.classList.add('show');
    clearTimeout(el._t);
    el._t = setTimeout(() => el.classList.remove('show'), 2400);
  }

  // ─── XSS escape ──────────────────────────────────────
  function esc(str) {
    return String(str || '').replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;').replace(/"/g,'&quot;');
  }

})();
