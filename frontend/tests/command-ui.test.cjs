const {readFileSync} = require('node:fs');
const vm = require('node:vm');
const assert = require('node:assert/strict');
const path = require('node:path');

async function dashboardCase(status, confirmedState) {
    const classes = new Set();
    const indicatorClasses = new Set(['off']);
    const classList = set => ({ contains: v => set.has(v), add: v => set.add(v), remove: v => set.delete(v),
        toggle: (v, on) => on ? set.add(v) : set.delete(v) });
    const indicator = { classList: classList(indicatorClasses), setAttribute() {} };
    const control = { dataset: {}, innerHTML: '<div class="knob"></div>', style: {},
        classList: classList(classes), closest: () => ({querySelector: () => indicator}) };
    const alerts = [];
    let polls = 0;
    const context = vm.createContext({ console, URLSearchParams,
        document: { addEventListener() {}, querySelector: () => control, getElementById: () => null },
        setTimeout: fn => { fn(); return 1; }, alert: msg => alerts.push(msg),
        fetch: async url => {
            if (url.endsWith('/state')) return {status: 202, json: async () => ({historyId: 10, status: 'LOADING'})};
            if (url.includes('/activehistories/')) {
                assert.equal(classes.has('active'), false, '202 and LOADING cannot turn the switch on');
                polls++;
                return {ok: true, json: async () => polls === 1 ? {status: 'LOADING', state: 'OFF'} : {status, state: confirmedState}};
            }
            return {ok: true, json: async () => ({})};
        }
    });
    vm.runInContext(readFileSync(path.join(__dirname, '../js/dashboard.js'), 'utf8'), context);
    await context.toggleDevice(1);
    assert.equal(polls, 2);
    assert.equal(classes.has('active'), confirmedState === 'ON');
    assert.equal(alerts.length, status === 'FAILURE' ? 1 : 0);
    assert.equal(control.dataset.loading, 'false');
}

async function historyCase(status, expectedClass, interval) {
    const rows = [];
    const tbody = {innerHTML: '', appendChild: row => rows.push(row)};
    let delay;
    const context = vm.createContext({console, URLSearchParams,
        document: {hidden: false, addEventListener() {}, getElementById: id => id === 'historyTableBody' ? tbody : null,
            createElement: () => ({innerHTML: ''})}, window: {addEventListener() {}},
        clearTimeout() {}, setTimeout: (fn, ms) => {delay = ms; return 1;},
        fetch: async () => ({ok: true, json: async () => ({data: [{action: 'ON', status, deviceName: '<LED>'}],
            pagination: {total: 1, totalPages: 1, page: 1}})})
    });
    vm.runInContext(readFileSync(path.join(__dirname, '../js/active-history.js'), 'utf8'), context);
    await context.loadHistory();
    assert.equal(rows.length, 1);
    assert.ok(rows[0].innerHTML.includes(expectedClass));
    assert.ok(rows[0].innerHTML.includes('&lt;LED&gt;'));
    assert.equal(delay, interval);
}

(async () => {
    await dashboardCase('SUCCESS', 'ON');
    await dashboardCase('FAILURE', 'OFF');
    await historyCase('LOADING', 'badge-loading', 1000);
    await historyCase('SUCCESS', 'badge-success', 5000);
    await historyCase('FAILURE', 'badge-failure', 5000);
    await historyCase('ON', 'badge-legacy', 5000);
    console.log('PASS: 6 UI cases (acceptance, confirmation, failure, polling, legacy records)');
})().catch(e => { console.error(e); process.exitCode = 1; });
