(() => {
  const $ = (selector, root = document) => root.querySelector(selector);
  const $$ = (selector, root = document) => [...root.querySelectorAll(selector)];
  const csrf = () => $('input[name="_csrf"]')?.value || '';
  const motionReduced = window.matchMedia('(prefers-reduced-motion: reduce)').matches;
  const chartColors = { cyan: '#69d7e3', rose: '#e58ad2', violet: '#a493ff', blue: '#8da9c7', awake: '#737d8b' };

  async function requestJson(url, options = {}) {
    const headers = new Headers(options.headers || {});
    headers.set('Accept', 'application/json');
    headers.set('X-Requested-With', 'XMLHttpRequest');
    if (options.body) headers.set('Content-Type', 'application/json');
    if (options.method && options.method !== 'GET') headers.set('X-CSRF-TOKEN', csrf());
    const response = await fetch(url, { credentials: 'same-origin', ...options, headers });
    const text = await response.text();
    const isJson = (response.headers.get('content-type') || '').includes('application/json');
    let body;
    try { body = isJson && text ? JSON.parse(text) : {}; } catch { body = {}; }
    if (response.redirected || response.status === 401 || response.status === 403) {
      throw new Error('Your sign-in may have expired. Refresh the page and sign in again.');
    }
    if (!isJson) throw new Error(`SleepSense returned an unexpected response (${response.status}).`);
    if (!response.ok) throw new Error(body.message || `Request failed (${response.status}).`);
    return body;
  }

  const chartBase = {
    responsive: true,
    maintainAspectRatio: false,
    animation: { duration: motionReduced ? 0 : 340, easing: 'easeOutQuart' },
    interaction: { intersect: false, mode: 'index' },
    plugins: {
      legend: { display: false },
      tooltip: { backgroundColor: '#101720', borderColor: '#344253', borderWidth: 1, titleColor: '#edf3fa', bodyColor: '#b9c6d4', padding: 9, displayColors: false }
    },
    scales: {
      x: { grid: { display: false }, ticks: { color: '#8694a5', maxTicksLimit: 7, maxRotation: 0, font: { size: 9 } } },
      y: { grid: { color: 'rgba(159, 177, 194, .1)' }, ticks: { color: '#8694a5', maxTicksLimit: 5, font: { size: 9 } } }
    }
  };

  function makeLineChart(id, label, color, suffix, options = {}) {
    return new Chart(document.getElementById(id), {
      type: 'line',
      data: { labels: [], datasets: [{ label, data: [], borderColor: color, backgroundColor: `${color}20`, fill: true, tension: .32, borderWidth: 2, pointRadius: 2, pointHoverRadius: 5, pointBackgroundColor: color, spanGaps: false }] },
      options: { ...chartBase, scales: { ...chartBase.scales, y: { ...chartBase.scales.y, ...(options.y || {}) } },
        plugins: { ...chartBase.plugins, tooltip: { ...chartBase.plugins.tooltip, callbacks: { label: context => context.raw == null ? 'No reading' : `${context.raw}${suffix}` } } } }
    });
  }

  const trends = {
    duration: makeLineChart('durationTrendChart', 'Duration', chartColors.cyan, ' h', { y: { beginAtZero: true, suggestedMax: 10 } }),
    score: makeLineChart('smartScoreTrendChart', 'Smart Sleep Score', chartColors.rose, ' / 100', { y: { min: 0, max: 100 } }),
    deep: makeLineChart('deepTrendChart', 'Deep sleep', chartColors.cyan, ' min', { y: { beginAtZero: true } }),
    rem: makeLineChart('remTrendChart', 'REM sleep', chartColors.violet, ' min', { y: { beginAtZero: true } }),
    awake: makeLineChart('awakeTrendChart', 'Awake time', chartColors.rose, ' min', { y: { beginAtZero: true } }),
    heartRate: makeLineChart('heartRateTrendChart', 'Average heart rate', chartColors.cyan, ' BPM'),
    spo2: makeLineChart('spo2TrendChart', 'SpO2', chartColors.violet, '%', { y: { min: 85, max: 100 } }),
    consistency: makeLineChart('consistencyTrendChart', 'Consistency', chartColors.rose, '%', { y: { min: 0, max: 100 } })
  };

  const stageChart = new Chart($('#stageDistributionChart'), {
    type: 'doughnut',
    data: { labels: ['Awake', 'Light', 'Deep', 'REM'], datasets: [{ data: [null, null, null, null], backgroundColor: [chartColors.awake, chartColors.blue, chartColors.cyan, chartColors.violet], borderColor: '#090c12', borderWidth: 3, hoverOffset: 4 }] },
    options: { ...chartBase, cutout: '63%', plugins: { ...chartBase.plugins,
      legend: { display: true, position: 'bottom', labels: { color: '#aeb9c5', boxWidth: 7, boxHeight: 7, usePointStyle: true, pointStyle: 'circle', padding: 12, font: { size: 9 } } },
      tooltip: { ...chartBase.plugins.tooltip, callbacks: { label: item => item.raw == null ? `${item.label}: no readings` : `${item.label}: ${item.raw.toFixed(1)}%` } }
    } }
  });

  function emptyNote(chart, empty) {
    const host = chart.canvas.closest('.chart-canvas');
    let note = $('.chart-empty-note', host);
    if (!note) { note = document.createElement('p'); note.className = 'chart-empty-note'; note.textContent = 'No saved readings for this period.'; host.append(note); }
    note.hidden = !empty;
  }

  function putChart(chart, labels, values) {
    chart.data.labels = labels;
    chart.data.datasets[0].data = values;
    chart.update();
    emptyNote(chart, values.length === 0 || values.every(value => value == null || !Number.isFinite(Number(value))));
  }

  function formatDuration(value) {
    if (typeof value !== 'number' || !Number.isFinite(value)) return 'Not available';
    const minutes = Math.round(value);
    return `${Math.floor(minutes / 60)}h ${minutes % 60}m`;
  }
  function metricText(value, suffix = '') {
    return typeof value === 'number' && Number.isFinite(value) ? `${Number(value.toFixed(1))}${suffix}` : 'Not available';
  }

  function renderAnalyticsSummary(data) {
    const metrics = data.metrics || {};
    const summary = $('#analyticsSummary');
    summary.replaceChildren();
    const values = [
      ['Average sleep duration', formatDuration(metrics.avgSleepDurationMinutes)],
      ['Average Sleep Score', metrics.avgSleepScore == null ? 'Not available' : `${Math.round(metrics.avgSleepScore)} / 100`],
      ['Best session', metrics.bestSession ? `${metrics.bestSession.score} · ${metrics.bestSession.date}` : 'Not available'],
      ['Lowest score', metrics.lowestScore ? `${metrics.lowestScore.score} · ${metrics.lowestScore.date}` : 'Not available'],
      ['Average deep sleep', `${formatDuration(metrics.avgDeepMinutes)} · ${metricText(metrics.avgDeepPercentage, '%')}`],
      ['Average REM sleep', `${formatDuration(metrics.avgRemMinutes)} · ${metricText(metrics.avgRemPercentage, '%')}`],
      ['Average awake time', metricText(metrics.avgAwakeMinutes, ' min')],
      ['Schedule consistency', metricText(metrics.consistencyPercentage, '%')],
      ['Average heart rate', metricText(metrics.avgHeartRate, ' BPM')],
      ['Average SpO₂', metricText(metrics.avgSpo2, '%')]
    ];
    const list = document.createElement('dl'); list.className = 'analytics-stats';
    values.forEach(([label, value]) => {
      const item = document.createElement('div'); item.className = 'analytics-stat';
      const term = document.createElement('dt'); term.textContent = label;
      const description = document.createElement('dd'); description.textContent = value;
      item.append(term, description); list.append(item);
    });
    summary.append(list);
    const count = document.createElement('p'); count.className = 'analytics-count';
    count.textContent = `${data.sessionCount || 0} saved session${data.sessionCount === 1 ? '' : 's'} in the selected period`;
    summary.append(count);
  }

  function renderComparisons(data) {
    const host = $('#analyticsComparison'); host.replaceChildren();
    const comparisons = data.comparisons || {};
    const labels = {
      avgSleepDurationMinutes: 'Sleep duration', avgSleepScore: 'Sleep Score', avgDeepPercentage: 'Deep sleep',
      avgRemPercentage: 'REM sleep', avgAwakeMinutes: 'Awake time', avgHeartRate: 'Heart rate',
      avgSpo2: 'SpO₂', consistencyPercentage: 'Consistency'
    };
    const list = document.createElement('ul'); list.className = 'comparison-list';
    Object.entries(labels).forEach(([key, label]) => {
      const item = comparisons[key] || {};
      const row = document.createElement('li'); row.className = `comparison-${item.direction || 'unavailable'}`;
      const name = document.createElement('span'); name.textContent = label;
      const value = document.createElement('strong');
      if (item.direction === 'unavailable') value.textContent = 'Prior period unavailable';
      else if (item.direction === 'steady') value.textContent = 'No material change';
      else if (typeof item.difference === 'number') {
        const suffix = key.includes('Percentage') ? ' pp' : key.includes('Minutes') ? ' min' : key === 'avgSleepScore' ? ' pts' : '';
        value.textContent = `${item.difference > 0 ? '+' : ''}${item.difference}${suffix} · ${item.direction}`;
      } else value.textContent = 'Change recorded';
      row.append(name, value); list.append(row);
    });
    const title = document.createElement('h3'); title.textContent = 'Compared with the previous period';
    host.append(title, list);
  }

  function updateAnalyticsCharts(data) {
    const points = data.sessions || [];
    const labels = points.map(point => point.date);
    putChart(trends.duration, labels, points.map(point => point.totalMinutes == null ? null : Math.round(point.totalMinutes / 6) / 10));
    putChart(trends.score, labels, points.map(point => point.score ?? null));
    putChart(trends.deep, labels, points.map(point => point.deepMinutes ?? null));
    putChart(trends.rem, labels, points.map(point => point.remMinutes ?? null));
    putChart(trends.awake, labels, points.map(point => point.awakeMinutes ?? null));
    putChart(trends.heartRate, labels, points.map(point => point.heartRate ?? null));
    putChart(trends.spo2, labels, points.map(point => point.spo2 ?? null));
    const stages = data.stageDistribution || {};
    const stageValues = [stages.awake ?? null, stages.light ?? null, stages.deep ?? null, stages.rem ?? null];
    stageChart.data.datasets[0].data = stageValues;
    stageChart.update();
    emptyNote(stageChart, stageValues.every(value => value == null));
    const consistency = data.consistencyBySession || [];
    putChart(trends.consistency, consistency.map(point => point.date), consistency.map(point => point.value));
  }

  async function loadAnalytics(days = Number($('#analyticsPeriod').value || 7)) {
    const summary = $('#analyticsSummary');
    summary.setAttribute('aria-busy', 'true');
    try {
      const data = await requestJson(`/api/analytics?days=${days}`);
      renderAnalyticsSummary(data); renderComparisons(data); updateAnalyticsCharts(data);
    } catch (error) {
      summary.innerHTML = '';
      const message = document.createElement('p'); message.className = 'feature-error'; message.textContent = error.message || 'Sleep analytics could not be loaded.';
      summary.append(message);
    } finally { summary.removeAttribute('aria-busy'); }
  }

  function relativePower(value) {
    if (value == null || value === '') return null;
    const parsed = Number(value);
    return Number.isFinite(parsed) && parsed >= 0 && parsed <= 1 ? parsed : null;
  }
  function eegRowsFromDom() {
    return $$('#sessionHistoryData > span').map(point => ({
      id: point.dataset.id, date: point.dataset.date, mode: point.dataset.mode,
      delta: point.dataset.hasDelta === 'true' ? relativePower(point.dataset.delta) : null,
      theta: point.dataset.hasTheta === 'true' ? relativePower(point.dataset.theta) : null,
      alpha: point.dataset.hasAlpha === 'true' ? relativePower(point.dataset.alpha) : null,
      beta: point.dataset.hasBeta === 'true' ? relativePower(point.dataset.beta) : null
    }));
  }
  function eegRowsFromPayload(history) {
    return (history || []).map(point => {
      const available = point.availableMetrics || [];
      return { id: String(point.id), date: point.date, mode: point.dataMode,
        delta: available.includes('deltaPower') ? relativePower(point.deltaPower) : null,
        theta: available.includes('thetaPower') ? relativePower(point.thetaPower) : null,
        alpha: available.includes('alphaPower') ? relativePower(point.alphaPower) : null,
        beta: available.includes('betaPower') ? relativePower(point.betaPower) : null };
    });
  }
  const eegSessionChart = new Chart($('#eegSessionChart'), {
    type: 'bar', data: { labels: ['Delta', 'Theta', 'Alpha', 'Beta'], datasets: [{ data: [null, null, null, null], backgroundColor: [chartColors.cyan, chartColors.blue, chartColors.violet, chartColors.rose], borderRadius: 5, maxBarThickness: 45 }] },
    options: { ...chartBase, plugins: { ...chartBase.plugins, tooltip: { ...chartBase.plugins.tooltip, callbacks: { label: item => {
      const selected = eegRows?.find(row => row.id === $('#eegSessionSelect').value);
      return item.raw == null ? 'No saved value' : `Relative power ${item.raw.toFixed(2)} · ${selected?.mode || 'simulated'}`;
    } } } },
      scales: { ...chartBase.scales, x: { ...chartBase.scales.x, grid: { display: false } }, y: { ...chartBase.scales.y, min: 0, max: 1, ticks: { ...chartBase.scales.y.ticks, stepSize: .25 } } } }
  });
  const eegDistributionChart = new Chart($('#eegDistributionChart'), {
    type: 'doughnut', data: { labels: ['Delta', 'Theta', 'Alpha', 'Beta'], datasets: [{ data: [null, null, null, null], backgroundColor: [chartColors.cyan, chartColors.blue, chartColors.violet, chartColors.rose], borderColor: '#090c12', borderWidth: 3 }] },
    options: { ...chartBase, cutout: '62%', plugins: { ...chartBase.plugins,
      legend: { display: true, position: 'bottom', labels: { color: '#aeb9c5', boxWidth: 7, boxHeight: 7, usePointStyle: true, pointStyle: 'circle', padding: 12, font: { size: 9 } } },
      tooltip: { ...chartBase.plugins.tooltip, callbacks: { label: item => item.raw == null ? `${item.label}: no value` : `${item.label}: ${item.raw.toFixed(1)}%` } }
    } }
  });
  const eegBandColors = [chartColors.cyan, chartColors.blue, chartColors.violet, chartColors.rose];
  let eegHistoryRows = [];
  const eegHistoryChart = new Chart($('#eegHistoryChart'), {
    type: 'line',
    data: { labels: [], datasets: ['Delta', 'Theta', 'Alpha', 'Beta'].map((label, index) => ({
      label, data: [], borderColor: eegBandColors[index], backgroundColor: `${eegBandColors[index]}20`,
      pointBackgroundColor: eegBandColors[index], pointRadius: 2, pointHoverRadius: 5,
      borderWidth: 1.8, tension: .3, spanGaps: false
    })) },
    options: { ...chartBase, plugins: { ...chartBase.plugins,
      legend: { display: true, position: 'bottom', labels: { color: '#aeb9c5', boxWidth: 7, boxHeight: 7, usePointStyle: true, pointStyle: 'circle', padding: 12, font: { size: 9 } } },
      tooltip: { ...chartBase.plugins.tooltip, callbacks: {
        label: context => {
          const row = eegHistoryRows[context.dataIndex];
          return context.raw == null ? `${context.dataset.label}: no saved value`
            : `${context.dataset.label}: ${Number(context.raw).toFixed(2)} · ${row?.mode || 'simulated'}`;
        }
      } }
    }, scales: { ...chartBase.scales, y: { ...chartBase.scales.y, min: 0, max: 1,
      ticks: { ...chartBase.scales.y.ticks, stepSize: .25 } } } }
  });
  let eegRows = eegRowsFromDom();
  function updateEegHistory() {
    eegHistoryRows = [...eegRows].reverse();
    eegHistoryChart.data.labels = eegHistoryRows.map(row => row.date);
    eegHistoryChart.data.datasets.forEach((dataset, index) => {
      const key = ['delta', 'theta', 'alpha', 'beta'][index];
      dataset.data = eegHistoryRows.map(row => row[key]);
    });
    eegHistoryChart.update();
    emptyNote(eegHistoryChart, eegHistoryRows.length === 0 || eegHistoryRows.every(row =>
      row.delta == null && row.theta == null && row.alpha == null && row.beta == null));
    const modes = [...new Set(eegHistoryRows.map(row => row.mode || 'simulated'))];
    const status = $('#eegHistoryMode');
    if (status) status.textContent = modes.length === 0 ? 'No saved EEG band values are available.'
      : modes.every(mode => mode === 'simulated') ? 'Simulated values only · historical summaries, not raw EEG.'
      : modes.length === 1 ? `${modes[0]} session summaries · not raw EEG.`
        : 'Mixed measured and simulated session summaries · see each chart point for its source.';
  }
  function updateEegSelection() {
    const select = $('#eegSessionSelect');
    const rows = [...eegRows];
    const previous = select.value;
    select.replaceChildren();
    rows.forEach(row => { const option = document.createElement('option'); option.value = row.id; option.textContent = `${row.date} · ${row.mode || 'simulated'}`; select.append(option); });
    if (!rows.length) { const option = document.createElement('option'); option.value = ''; option.textContent = 'No saved sessions'; select.append(option); }
    if (rows.some(row => row.id === previous)) select.value = previous;
    const selected = rows.find(row => row.id === select.value) || rows[0];
    const sessionMode = $('#eegSessionMode');
    if (sessionMode) sessionMode.textContent = selected
      ? `${selected.mode === 'measured' ? 'Measured' : 'Simulated'} session snapshot · relative band values`
      : 'No saved EEG session values';
    const values = selected ? [selected.delta, selected.theta, selected.alpha, selected.beta] : [null, null, null, null];
    eegSessionChart.data.datasets[0].data = values; eegSessionChart.update();
    emptyNote(eegSessionChart, values.every(value => value == null));
    const total = values.filter(Number.isFinite).reduce((sum, value) => sum + value, 0);
    const normalized = total > 0 ? values.map(value => Number.isFinite(value) ? Math.round(value / total * 1000) / 10 : null) : [null, null, null, null];
    eegDistributionChart.data.datasets[0].data = normalized; eegDistributionChart.update();
    emptyNote(eegDistributionChart, normalized.every(value => value == null));
    updateEegHistory();
  }
  $('#eegSessionSelect').addEventListener('change', updateEegSelection);
  updateEegSelection();

  const analyticsPeriod = $('#analyticsPeriod');
  analyticsPeriod.addEventListener('change', () => loadAnalytics(Number(analyticsPeriod.value)));
  loadAnalytics();

  const todayLocal = () => {
    const date = new Date();
    return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`;
  };
  const shiftDate = (value, days) => {
    const date = new Date(`${value}T12:00:00`); date.setDate(date.getDate() + days);
    return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`;
  };
  const journalForm = $('#journalForm');
  const journalDate = $('#journalDate');
  const journalOriginalDate = $('#journalOriginalDate');
  const journalStatus = $('#journalStatus');
  const journalList = $('#journalList');
  const journalFilter = $('#journalFilter');
  const journalThrough = $('#journalThrough');
  journalDate.max = todayLocal(); journalDate.value = todayLocal();
  journalFilter.max = todayLocal(); journalFilter.value = shiftDate(todayLocal(), -29);
  journalThrough.max = todayLocal(); journalThrough.value = todayLocal();

  function resetJournalForm() {
    journalForm.reset(); journalDate.disabled = false; journalDate.value = todayLocal();
    journalOriginalDate.value = ''; $('#stressLevel').value = 5; $('#caffeineCups').value = 0;
    $('#screenTimeMinutes').value = 0; $('#journalSave').textContent = 'Save entry'; $('#journalCancel').hidden = true;
  }
  function editJournal(entry) {
    journalDate.value = entry.date; journalDate.disabled = true; journalOriginalDate.value = entry.date;
    $('#moodBefore').value = entry.moodBefore || ''; $('#moodAfter').value = entry.moodAfter || '';
    $('#stressLevel').value = entry.stressLevel; $('#caffeineCups').value = entry.caffeineCups;
    $('#exercised').checked = entry.exercised; $('#screenTimeMinutes').value = entry.screenTimeMinutes;
    $('#eveningMealTime').value = entry.eveningMealTime || ''; $('#sleepEnvironment').value = entry.sleepEnvironment || '';
    $('#journalNotes').value = entry.notes || ''; $('#journalSave').textContent = 'Update entry'; $('#journalCancel').hidden = false;
    journalForm.scrollIntoView({ behavior: motionReduced ? 'auto' : 'smooth', block: 'center' });
    $('#moodBefore').focus({ preventScroll: true });
  }
  function renderJournal(entries) {
    journalList.replaceChildren();
    if (!entries.length) { const empty = document.createElement('p'); empty.className = 'empty-state'; empty.textContent = 'No journal entries in this date range. Save an entry to start connecting your routine with your sleep history.'; journalList.append(empty); return; }
    entries.forEach(entry => {
      const article = document.createElement('article'); article.className = 'journal-entry';
      const heading = document.createElement('div'); heading.className = 'journal-entry-head';
      const date = document.createElement('strong'); date.textContent = new Intl.DateTimeFormat(undefined, { dateStyle: 'medium', timeZone: 'UTC' }).format(new Date(`${entry.date}T12:00:00Z`));
      const actions = document.createElement('div'); actions.className = 'journal-entry-actions';
      const edit = document.createElement('button'); edit.type = 'button'; edit.className = 'text-button'; edit.textContent = 'Edit'; edit.addEventListener('click', () => editJournal(entry));
      const remove = document.createElement('button'); remove.type = 'button'; remove.className = 'text-button'; remove.textContent = 'Delete'; remove.addEventListener('click', async () => {
        if (!window.confirm(`Delete the journal entry for ${entry.date}?`)) return;
        try { await requestJson(`/api/journal/${entry.date}`, { method: 'DELETE' }); await loadJournal(); }
        catch (error) { journalStatus.textContent = error.message; journalStatus.classList.add('is-error'); }
      });
      actions.append(edit, remove); heading.append(date, actions);
      const details = document.createElement('p');
      const moods = [entry.moodBefore && `before bed: ${entry.moodBefore}`, entry.moodAfter && `after waking: ${entry.moodAfter}`].filter(Boolean).join(' · ');
      details.textContent = `${moods ? moods + ' · ' : ''}stress ${entry.stressLevel}/10 · caffeine ${entry.caffeineCups} cups · screen ${entry.screenTimeMinutes} min${entry.exercised ? ' · exercise' : ''}`;
      article.append(heading, details);
      if (entry.sleepEnvironment) { const environment = document.createElement('p'); environment.textContent = `Environment · ${entry.sleepEnvironment}`; article.append(environment); }
      if (entry.notes) { const notes = document.createElement('p'); notes.className = 'journal-note'; notes.textContent = entry.notes; article.append(notes); }
      journalList.append(article);
    });
  }
  async function loadJournal() {
    journalList.setAttribute('aria-busy', 'true');
    try {
      const query = new URLSearchParams({ from: journalFilter.value, through: journalThrough.value });
      const data = await requestJson(`/api/journal?${query}`); renderJournal(data.entries || []);
      await loadCorrelations();
    } catch (error) { journalList.innerHTML = ''; const message = document.createElement('p'); message.className = 'feature-error'; message.textContent = error.message; journalList.append(message); }
    finally { journalList.removeAttribute('aria-busy'); }
  }
  async function loadCorrelations() {
    const panel = $('#journalCorrelations');
    try {
      const data = await requestJson('/api/journal/correlations?days=30');
      panel.replaceChildren(); const title = document.createElement('strong'); title.textContent = 'Habit patterns'; panel.append(title);
      const note = document.createElement('p'); note.textContent = data.ready
        ? `${data.sampleSize} paired dates · ${data.simulated ? 'includes simulated sleep readings. ' : ''}${data.disclaimer}`
        : `${data.sampleSize} paired dates so far. Add at least five dates with both a journal entry and saved sleep session. ${data.disclaimer}`;
      panel.append(note);
      if (data.ready) {
        const list = document.createElement('ul');
        data.correlations.forEach(item => {
          const row = document.createElement('li'); const coefficient = item.coefficient == null ? '' : ` · r = ${item.coefficient}`;
          row.textContent = `${item.habit} ↔ ${item.sleepMetric}${coefficient}: ${item.note}`; list.append(row);
        });
        panel.append(list);
      }
    } catch { /* Journal editing remains available if correlations cannot be loaded. */ }
  }
  journalForm.addEventListener('submit', async event => {
    event.preventDefault();
    const button = $('#journalSave'); button.disabled = true; journalStatus.textContent = 'Saving your entry…'; journalStatus.classList.remove('is-error');
    const entry = { date: journalDate.value, moodBefore: $('#moodBefore').value, moodAfter: $('#moodAfter').value,
      stressLevel: Number($('#stressLevel').value), caffeineCups: Number($('#caffeineCups').value),
      exercised: $('#exercised').checked, screenTimeMinutes: Number($('#screenTimeMinutes').value),
      eveningMealTime: $('#eveningMealTime').value, sleepEnvironment: $('#sleepEnvironment').value,
      notes: $('#journalNotes').value };
    try { await requestJson('/api/journal', { method: 'PUT', body: JSON.stringify(entry) });
      journalStatus.textContent = 'Journal entry saved.'; resetJournalForm(); await loadJournal();
    } catch (error) { journalStatus.textContent = error.message; journalStatus.classList.add('is-error'); }
    finally { button.disabled = false; }
  });
  $('#journalCancel').addEventListener('click', resetJournalForm);
  journalFilter.addEventListener('change', loadJournal); journalThrough.addEventListener('change', loadJournal);
  loadJournal();

  const goalForm = $('#goalForm');
  const goalStatus = $('#goalStatus');
  let currentGoal = null;
  function renderGoalProgress(data) {
    const host = $('#goalProgress'); host.replaceChildren();
    const heading = document.createElement('h3'); heading.textContent = 'This week';
    const summary = document.createElement('p'); summary.className = 'goal-summary';
    summary.textContent = `${data.weeklyConsistencyCount}/${data.weeklyConsistencyTarget} schedule nights · ${data.streakDays}-night consistency streak`;
    const bars = document.createElement('div'); bars.className = 'goal-week'; bars.setAttribute('aria-label', 'Progress for the last seven days');
    (data.days || []).forEach(day => {
      const item = document.createElement('div'); item.className = day.complete ? 'goal-day is-complete' : 'goal-day';
      const marker = document.createElement('span'); marker.setAttribute('aria-hidden', 'true');
      const label = document.createElement('small'); label.textContent = day.label;
      item.append(marker, label); bars.append(item);
    });
    const historyTitle = document.createElement('h3'); historyTitle.className = 'goal-history-title'; historyTitle.textContent = 'Last four weeks · current schedule';
    const history = document.createElement('div'); history.className = 'goal-history'; history.setAttribute('role', 'img');
    history.setAttribute('aria-label', 'Measured sleep sessions meeting your current schedule during the last twenty eight days');
    (data.history || []).forEach(day => {
      const marker = document.createElement('span'); marker.className = day.complete ? 'is-complete' : '';
      marker.title = `${day.date}${day.complete ? ' · goal met' : ' · no measured session meeting goal'}`;
      marker.setAttribute('aria-hidden', 'true'); history.append(marker);
    });
    const note = document.createElement('p'); note.className = 'goal-data-note';
    note.textContent = `${data.measuredSessionCount} measured sessions count toward goals. ${data.simulatedSessionCount} simulated sessions are shown for review but do not earn achievements.`;
    const badgeTitle = document.createElement('h3'); badgeTitle.textContent = 'Achievements';
    const earned = new Map((data.achievements || []).map(item => [item.code, item]));
    const badgeList = document.createElement('ul'); badgeList.className = 'achievement-list';
    (data.eligibleAchievementDefinitions || []).forEach(definition => {
      const award = earned.get(definition.code);
      const row = document.createElement('li'); row.className = award ? 'is-earned' : 'is-locked';
      const name = document.createElement('strong'); name.textContent = definition.title;
      const description = document.createElement('span'); description.textContent = award ? `Earned ${award.earnedOn}` : definition.description;
      row.append(name, description); badgeList.append(row);
    });
    host.append(heading, summary, bars, historyTitle, history, note, badgeTitle, badgeList);
  }
  async function loadGoals() {
    try {
      const data = await requestJson('/api/goals'); currentGoal = data.goal;
      $('#targetSleepMinutes').value = currentGoal.targetSleepMinutes;
      $('#targetBedtime').value = currentGoal.targetBedtime;
      $('#targetWakeTime').value = currentGoal.targetWakeTime;
      $('#weeklyConsistencyNights').value = currentGoal.weeklyConsistencyNights;
      $('#bedtimeReminderEnabled').checked = currentGoal.bedtimeReminderEnabled;
      $('#reminderTime').value = currentGoal.reminderTime;
      renderGoalProgress(data);
    } catch (error) { $('#goalProgress').textContent = error.message; }
  }
  goalForm.addEventListener('submit', async event => {
    event.preventDefault(); const button = $('button[type="submit"]', goalForm); button.disabled = true;
    goalStatus.textContent = 'Saving your sleep goals…'; goalStatus.classList.remove('is-error');
    const goal = { targetSleepMinutes: Number($('#targetSleepMinutes').value), targetBedtime: $('#targetBedtime').value,
      targetWakeTime: $('#targetWakeTime').value, weeklyConsistencyNights: Number($('#weeklyConsistencyNights').value),
      bedtimeReminderEnabled: $('#bedtimeReminderEnabled').checked, reminderTime: $('#reminderTime').value };
    try { await requestJson('/api/goals', { method: 'PUT', body: JSON.stringify(goal) }); goalStatus.textContent = 'Sleep goals saved.'; await loadGoals(); }
    catch (error) { goalStatus.textContent = error.message; goalStatus.classList.add('is-error'); }
    finally { button.disabled = false; }
  });
  $('#enableNotifications').addEventListener('click', async event => {
    if (!('Notification' in window)) { goalStatus.textContent = 'This browser does not support notifications.'; return; }
    const button = event.currentTarget; button.disabled = true;
    try { const permission = await Notification.requestPermission(); goalStatus.textContent = permission === 'granted' ? 'Browser reminders are allowed.' : 'Notification permission was not granted.'; }
    catch { goalStatus.textContent = 'This browser could not request notification permission.'; goalStatus.classList.add('is-error'); }
    finally { button.disabled = false; }
  });
  function checkBedtimeReminder() {
    if (!currentGoal?.bedtimeReminderEnabled || !('Notification' in window) || Notification.permission !== 'granted') return;
    const now = new Date(); const today = todayLocal();
    const currentTime = `${String(now.getHours()).padStart(2, '0')}:${String(now.getMinutes()).padStart(2, '0')}`;
    if (currentTime !== currentGoal.reminderTime) return;
    const key = `sleepsense-bedtime-reminder-${today}`;
    try { if (localStorage.getItem(key)) return; localStorage.setItem(key, 'sent'); } catch { return; }
    new Notification('SleepSense bedtime reminder', { body: 'Your saved sleep goal is coming up. Start winding down when it feels right.' });
  }
  window.setInterval(checkBedtimeReminder, 30_000);
  loadGoals();

  window.refreshAdvancedFeatures = history => {
    eegRows = eegRowsFromPayload(history); updateEegSelection();
    loadAnalytics(Number(analyticsPeriod.value || 7));
  };
})();
