const http = require('http');
const fs = require('fs');
const path = require('path');
const { Builder } = require('selenium-webdriver');
const edge = require('selenium-webdriver/edge');

const REPO_ROOT = path.resolve(__dirname, '..');
const DIST_DIR = path.join(REPO_ROOT, 'XIMASAK angular', 'ximasak-app', 'dist', 'ximasak-app');
const OUT_DIR = path.join(REPO_ROOT, 'reportes_t3');
const AXE_JS = fs.readFileSync(
  require.resolve('axe-core/axe.min.js'),
  'utf8'
);

const MIME = {
  '.html': 'text/html; charset=utf-8',
  '.js': 'application/javascript; charset=utf-8',
  '.css': 'text/css; charset=utf-8',
  '.json': 'application/json; charset=utf-8',
  '.png': 'image/png',
  '.jpg': 'image/jpeg',
  '.jpeg': 'image/jpeg',
  '.ico': 'image/x-icon',
};

const server = http.createServer((req, res) => {
  const urlPath = (req.url || '/').split('?')[0];
  let filePath = path.join(DIST_DIR, urlPath === '/' ? 'index.html' : urlPath);
  if (!fs.existsSync(filePath) || fs.statSync(filePath).isDirectory()) {
    filePath = path.join(DIST_DIR, 'index.html');
  }
  const ext = path.extname(filePath).toLowerCase();
  res.writeHead(200, { 'Content-Type': MIME[ext] || 'application/octet-stream' });
  fs.createReadStream(filePath).pipe(res);
});

async function main() {
  await new Promise((r) => server.listen(4202, '127.0.0.1', r));

  const options = new edge.Options();
  options.addArguments('--headless=new', '--window-size=1440,900', '--disable-gpu', '--no-sandbox', '--log-level=3', '--silent');
  const service = new edge.ServiceBuilder(
    path.join(REPO_ROOT, 'ximasak_full', 'backend', 'drivers', 'msedgedriver.exe')
  ).addArguments('--silent');

  const driver = await new Builder().forBrowser('MicrosoftEdge').setEdgeOptions(options).setEdgeService(service).build();

  try {
    const routes = ['/', '/catalogo', '/reserva', '/clientes', '/dashboard'];
    const summary = {};
    for (const route of routes) {
      if (route === '/dashboard') {
        await driver.get('http://localhost:4202/');
        await driver.executeScript(`
          localStorage.setItem('token', 'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.admin.sirvs');
          localStorage.setItem('rol', 'ADMIN');
          localStorage.setItem('idUsuario', '1');
          localStorage.setItem('nombreUsuario', 'Matias Admin');
        `);
      }
      await driver.get('http://localhost:4202' + route);
      await driver.sleep(900);
      await driver.executeScript(AXE_JS);
      const res = await driver.executeAsyncScript(`
        const cb = arguments[arguments.length - 1];
        axe.run(document, { runOnly: ['wcag2a', 'wcag2aa', 'wcag21a', 'wcag21aa', 'best-practice'] })
          .then(r => cb({ violations: r.violations, passes: r.passes.length, incomplete: r.incomplete.length }))
          .catch(e => cb({ error: e.message }));
      `);
      summary[route] = {
        passes: res.passes,
        incomplete: res.incomplete,
        violations: (res.violations || []).map(v => ({
          id: v.id,
          impact: v.impact,
          help: v.help,
          nodes: v.nodes.length,
          targets: v.nodes.slice(0, 5).map(n => n.target.join(' '))
        }))
      };
      console.log(`Route ${route.padEnd(10)} : passes=${res.passes}, violations=${summary[route].violations.length}`);
    }
    fs.writeFileSync(path.join(OUT_DIR, 'axe_after.json'), JSON.stringify(summary, null, 2));
    console.log('Saved reportes_t3/axe_after.json');
  } finally {
    await driver.quit();
    server.close();
  }
}

main().catch((e) => {
  console.error(e);
  server.close();
  process.exit(1);
});
