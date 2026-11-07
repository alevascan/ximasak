# XIMASAK EIRL — Versión Angular

Migración del proyecto original (HTML + CSS + JS puro) a **Angular 17** con componentes standalone, tal como pidió Thiago. Se conservó toda la funcionalidad: navegación entre vistas, catálogo con filtros/orden/búsqueda, modo administrador (login, agregar/editar/eliminar productos), reservas con calendario, y el panel admin de reservas. Todo se guarda en `localStorage`, igual que en la versión original.

## Cómo ejecutarlo

Necesitas [Node.js](https://nodejs.org/) instalado (versión 18 o superior).

```bash
# 1. Instalar dependencias (descarga Angular, solo la primera vez)
npm install

# 2. Levantar el servidor de desarrollo
npm start
```

Luego abre `http://localhost:4200` en el navegador.

> Si prefieres usar el Angular CLI global: `npm install -g @angular/cli` y luego `ng serve -o`.

## Estructura del proyecto

```
src/
├── app/
│   ├── app.component.ts/html     -> layout raíz (header + router-outlet + footer)
│   ├── app.routes.ts             -> rutas: '', 'catalogo', 'reserva'
│   ├── app.config.ts             -> configuración del router
│   ├── components/
│   │   ├── header/               -> navegación, alto contraste, login admin
│   │   └── footer/
│   ├── pages/
│   │   ├── inicio/               -> antigua sección #vista-inicio
│   │   ├── catalogo/             -> antigua sección #vista-catalogo (filtros + admin)
│   │   └── reserva/              -> antigua sección #vista-reserva (calendario + admin reservas)
│   └── services/
│       ├── auth.service.ts       -> estado de sesión admin (signal)
│       ├── catalogo.service.ts   -> productos + persistencia en localStorage
│       └── reserva.service.ts    -> reservas + persistencia en localStorage
└── assets/fotos/                 -> todas tus imágenes originales
```

## Qué cambió respecto al proyecto original (y por qué)

- **Navegación**: los `data-target` + `display:none/block` se reemplazaron por `routerLink` y rutas reales de Angular (`/`, `/catalogo`, `/reserva`). Ya no se ocultan secciones con CSS: cada vista es su propia página.
- **Estado global (`esAdminActivo`, productos, reservas)**: antes vivía en variables sueltas dentro de `script.js`. Ahora vive en **servicios** (`AuthService`, `CatalogoService`, `ReservaService`) usando `signal()`, que es el mecanismo reactivo moderno de Angular. Cualquier componente que use el servicio se actualiza automáticamente, sin tocar el DOM a mano.
- **`*ngFor` / `*ngIf`**: reemplazan los `innerHTML += ...` y los bucles `forEach` que armaban HTML como texto.
- **`[(ngModel)]`**: reemplaza los `document.getElementById(...).value` de los formularios.
- **"Reservar Producto" desde el catálogo**: en vez de llenar el formulario manipulando el DOM de otra vista, se navega a `/reserva?producto=NombreDelProducto` y el componente de reserva lee ese parámetro para precargar la descripción.
- **Subida de imagen al agregar producto**: en el HTML original el `<input type="file">` solo guardaba el *nombre* del archivo como ruta (`fotos/nombre.jpg`), pero el archivo nunca se copiaba a ningún lado — es decir, no funcionaba realmente ni en la versión original. En esta versión, los productos nuevos usan una imagen genérica y se deja indicado en el código (`catalogo.component.ts`) dónde conectar una subida real si más adelante agregan un backend.

## Nota para el informe (calidad de software)

Como esto es para un curso de Calidad de Software, dos cosas que probablemente les convenga documentar/mejorar antes de entregar:

1. **Autenticación de administrador**: sigue siendo un usuario/clave fijo en el código (`XIMASAK` / `0000`), igual que en la versión original. Sirve para la demo, pero no es seguro para producción — vale la pena mencionarlo como limitación conocida en el capítulo de pruebas/calidad del informe.
2. **Persistencia solo en el navegador**: como no hay backend, los datos (`localStorage`) son distintos en cada navegador/computadora. Si el jurado prueba el sistema en otra máquina, no verá los mismos productos/reservas que ustedes cargaron.
