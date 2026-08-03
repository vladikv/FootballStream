Chart.defaults.color = '#8D93A6';
Chart.defaults.font.family = 'Inter';
Chart.defaults.borderColor = 'rgba(255,255,255,0.08)';

const EMERALD = '#34D399';
const CYAN = '#22D3EE';
const GOLD = '#FBBF24';
const ROSE = '#FB7185';

let formChart = null;
let compareChart = null;
let currentDate = new Date();

function startSyncedAtClock() {
    const el = document.getElementById('synced-at');
    const start = Date.now();
    setInterval(() => {
        el.textContent = `loaded ${Math.floor((Date.now() - start) / 1000)}s ago`;
    }, 1000);
}

function formatDate(date) {
    return date.toISOString().split('T')[0];
}

async function loadMatchesForDate() {
    const listEl = document.getElementById('match-list');
    document.getElementById('date-picker').value = formatDate(currentDate);
    listEl.innerHTML = Array.from({ length: 4 }, () => '<li class="skeleton"></li>').join('');

    const res = await fetch(`/api/leagues/${SELECTED_LEAGUE_ID}/matches?date=${formatDate(currentDate)}`);
    const matches = await res.json();

    if (matches.length === 0) {
        listEl.innerHTML = '<li class="empty-note">No matches on this day.</li>';
        return;
    }

    listEl.innerHTML = matches.map((m, i) => `
        <li class="match-row" style="animation-delay:${i * 0.04}s">
            <span class="match-row__team">
                <img src="${m.homeCrest || ''}" width="16" height="16" alt="" loading="lazy" onerror="this.style.display='none'">
                ${m.home}
            </span>
            <span class="match-row__score mono">${m.homeScore ?? '–'} : ${m.awayScore ?? '–'}</span>
            <span class="match-row__team match-row__team--away">
                ${m.away}
                <img src="${m.awayCrest || ''}" width="16" height="16" alt="" loading="lazy" onerror="this.style.display='none'">
            </span>
        </li>
    `).join('');
}

function initDateNav() {
    document.getElementById('date-prev').addEventListener('click', () => {
        currentDate.setDate(currentDate.getDate() - 1);
        loadMatchesForDate();
    });
    document.getElementById('date-next').addEventListener('click', () => {
        currentDate.setDate(currentDate.getDate() + 1);
        loadMatchesForDate();
    });
    document.getElementById('date-picker').addEventListener('change', (e) => {
        currentDate = new Date(e.target.value);
        loadMatchesForDate();
    });
    loadMatchesForDate();
}

async function loadTeams(leagueId) {
    const res = await fetch(`/api/leagues/${leagueId}/teams`);
    const teams = await res.json();

    const formSelect = document.getElementById('form-team-select');
    const compareA = document.getElementById('compare-team-a');
    const compareB = document.getElementById('compare-team-b');
    [formSelect, compareA, compareB].forEach(s => s.innerHTML = '');

    teams.forEach(team => {
        [formSelect, compareA, compareB].forEach(select => {
            const o = document.createElement('option');
            o.value = team.id;
            o.textContent = team.name;
            select.appendChild(o);
        });
    });

    if (teams.length > 1) compareB.selectedIndex = 1;
    return teams;
}

async function renderFormChart(teamId) {
    const res = await fetch(`/api/teams/${teamId}/form?lastN=5`);
    const data = await res.json();

    const labels = data.results.map(r => r.opponent).reverse();
    const goalDiff = data.results.map(r => (r.scoreFor ?? 0) - (r.scoreAgainst ?? 0)).reverse();
    const colors = data.results.map(r => r.outcome === 'W' ? EMERALD : r.outcome === 'L' ? ROSE : GOLD).reverse();

    if (formChart) formChart.destroy();
    formChart = new Chart(document.getElementById('form-chart'), {
        type: 'bar',
        data: { labels, datasets: [{ data: goalDiff, backgroundColor: colors, borderRadius: 6 }] },
        options: {
            maintainAspectRatio: false,
            plugins: { legend: { display: false } },
            scales: { x: { grid: { display: false } }, y: { beginAtZero: true } }
        }
    });
}

async function renderCompareChart(teamAId, teamBId) {
    const res = await fetch(`/api/teams/compare?teamA=${teamAId}&teamB=${teamBId}`);
    const data = await res.json();
    const a = data.teamA, b = data.teamB;

    if (compareChart) compareChart.destroy();
    compareChart = new Chart(document.getElementById('compare-chart'), {
        type: 'radar',
        data: {
            labels: ['Points', 'Won', 'Drawn', 'Goals For', 'Goals Against'],
            datasets: [
                { label: a.name, data: [a.points, a.won, a.draw, a.goalsFor, a.goalsAgainst], borderColor: EMERALD, backgroundColor: 'rgba(52,211,153,0.15)' },
                { label: b.name, data: [b.points, b.won, b.draw, b.goalsFor, b.goalsAgainst], borderColor: CYAN, backgroundColor: 'rgba(34,211,238,0.15)' }
            ]
        },
        options: {
            maintainAspectRatio: false,
            scales: { r: { ticks: { display: false }, pointLabels: { color: '#8D93A6' } } }
        }
    });
}

async function init() {
    startSyncedAtClock();
    if (!SELECTED_LEAGUE_ID) return;

    initDateNav();

    const teams = await loadTeams(SELECTED_LEAGUE_ID);
    if (teams.length === 0) return;

    const formSelect = document.getElementById('form-team-select');
    const compareA = document.getElementById('compare-team-a');
    const compareB = document.getElementById('compare-team-b');

    await renderFormChart(formSelect.value);
    await renderCompareChart(compareA.value, compareB.value);

    formSelect.addEventListener('change', () => renderFormChart(formSelect.value));
    compareA.addEventListener('change', () => renderCompareChart(compareA.value, compareB.value));
    compareB.addEventListener('change', () => renderCompareChart(compareA.value, compareB.value));
}

document.addEventListener('DOMContentLoaded', init);