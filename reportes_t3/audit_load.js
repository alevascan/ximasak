const http = require('http');
const fs = require('fs');
const path = require('path');
const autocannon = require('autocannon');

const REPO_ROOT = path.resolve(__dirname, '..');
const DIST = path.join(REPO_ROOT, 'XIMASAK angular', 'ximasak-app', 'dist', 'ximasak-app');
const OUT = path.join(REPO_ROOT, 'reportes_t3');
const MIME = {
  '.html': 'text/html; charset=utf-8',
  '.js': 'application/javascript; charset=utf-8',
  '.css': 'text/css; charset=utf-8',
  '.png': 'image/png',
  '.jpg': 'image/jpeg',
  '.jpeg': 'image/jpeg',
  '.ico': 'image/x-icon'
};

const files = fs.readdirSync(DIST);
const mainJs = '/' + (files.find(f => f.startsWith('main.') && f.endsWith('.js')) || 'index.html');
const stylesCss = '/' + (files.find(f => f.startsWith('styles.') && f.endsWith('.css')) || 'index.html');

const PRODUCTOS = [
  { idProducto: 1, nombre: 'Memoria RAM 16GB DDR4', marca: { idMarca: 1, nombre: 'Kingston' }, categoria: 'memorias', precioUnitario: 118.0, stock: 12, stockMinimo: 3, estado: true },
  { idProducto: 2, nombre: 'Router Wi-Fi Dual Band AC1200', marca: { idMarca: 2, nombre: 'TP-Link' }, categoria: 'redes', precioUnitario: 180.0, stock: 10, stockMinimo: 3, estado: true },
  { idProducto: 3, nombre: 'Case Gamer con Panel RGB', marca: { idMarca: 3, nombre: 'Halion' }, categoria: 'cases', precioUnitario: 250.0, stock: 2, stockMinimo: 3, estado: true },
  { idProducto: 4, nombre: 'Mouse Óptico Inalámbrico', marca: { idMarca: 4, nombre: 'Logitech' }, categoria: 'perifericos', precioUnitario: 45.0, stock: 15, stockMinimo: 3, estado: true },
  { idProducto: 5, nombre: 'Teclado Mecánico RGB Switch Blue', marca: { idMarca: 5, nombre: 'Antryx' }, categoria: 'perifericos', precioUnitario: 190.0, stock: 8, stockMinimo: 3, estado: true },
  { idProducto: 6, nombre: 'Disco Duro SSD 500GB NVMe', marca: { idMarca: 6, nombre: 'Western Digital' }, categoria: 'memorias', precioUnitario: 165.2, stock: 20, stockMinimo: 3, estado: true }
];

const spaSrv = http.createServer((req, res) => {
  const reqPath = (req.url || '/').split('?')[0];
  let filePath = path.join(DIST, reqPath === '/' ? 'index.html' : reqPath);
  if (!fs.existsSync(filePath) || fs.statSync(filePath).isDirectory()) {
    filePath = path.join(DIST, 'index.html');
  }
  const ext = path.extname(filePath).toLowerCase();
  fs.readFile(filePath, (err, data) => {
    if (err) {
      res.writeHead(500);
      res.end('Error');
      return;
    }
    res.writeHead(200, { 'Content-Type': MIME[ext] || 'application/octet-stream' });
    res.end(data);
  });
});

const apiSrv = http.createServer((req, res) => {
  const payload = JSON.stringify(PRODUCTOS.filter(p => p.estado));
  res.writeHead(200, { 'Content-Type': 'application/json; charset=utf-8' });
  res.end(payload);
});

function runBench(opts) {
  return new Promise((resolve, reject) => {
    autocannon(opts, (err, res) => (err ? reject(err) : resolve(res)));
  });
}

async function main() {
  await new Promise(r => spaSrv.listen(4201, '127.0.0.1', r));
  await new Promise(r => apiSrv.listen(8081, '127.0.0.1', r));

  try {
    const spaRes = await runBench({
      title: 'Frontend Angular completo (/catalogo + CSS + main.js)',
      url: 'http://localhost:4201',
      connections: 25,
      duration: 10,
      requests: [
        { method: 'GET', path: '/catalogo' },
        { method: 'GET', path: stylesCss },
        { method: 'GET', path: mainJs }
      ]
    });

    const apiRes = await runBench({
      title: 'API Catalogo (GET /api/productos)',
      url: 'http://localhost:8081/api/productos',
      connections: 30,
      duration: 10
    });

    const summary = {
      timestamp: new Date().toISOString(),
      tool: 'autocannon 9.0.0',
      scenarios: [spaRes, apiRes].map(r => ({
        title: r.title,
        url: r.url,
        connections: r.connections,
        duration: r.duration,
        requestsTotal: r.requests.total,
        requestsAvgPerSec: r.requests.average,
        latencyAvgMs: r.latency.average,
        latencyP50Ms: r.latency.p50,
        latencyP90Ms: r.latency.p90,
        latencyP99Ms: r.latency.p99,
        latencyMaxMs: r.latency.max,
        throughputAvgBytesPerSec: r.throughput.average,
        status2xx: r['2xx'],
        non2xx: r.non2xx,
        errors: r.errors,
        timeouts: r.timeouts
      }))
    };

    fs.writeFileSync(path.join(OUT, 'load_test_results.json'), JSON.stringify(summary, null, 2));
    console.log('Tool:', summary.tool);
    for (const s of summary.scenarios) {
      console.log(`  * ${s.title} (${s.connections} conc, ${s.duration}s)`);
      console.log(`    2xx=${s.status2xx} | ${s.requestsAvgPerSec} req/s | avg=${s.latencyAvgMs}ms | p99=${s.latencyP99Ms}ms | err=${s.errors}`);
    }
  } finally {
    spaSrv.close();
    apiSrv.close();
  }
}

main().catch(e => {
  console.error(e);
  spaSrv.close();
  apiSrv.close();
  process.exit(1);
});
