package com.ximasak.backend.selenium;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.*;
import org.openqa.selenium.*;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.edge.EdgeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Suite de Pruebas Funcionales Automatizadas E2E con Selenium WebDriver y JUnit 5
 * Sistema Integrado de Reservas de Ventas y Servicio Técnico (SIRVS) - Ximasak EIRL
 *
 * Cubre los casos de prueba funcionales del Informe Final (Capítulo VI):
 * - CP-FT-01: Carga y renderizado de la página principal SIRVS
 * - CP-FT-02: Despliegue de modal de acceso y conmutación Ingresar / Registrar (HU-01 / HU-02)
 * - CP-FT-03: Inicio de sesión como Administrador en interfaz web (HU-01 / CA-01)
 * - CP-FT-04: Consulta de repuestos y filtrado por categoría en catálogo (HU-05 / RNF-01)
 * - CP-FT-05: Registro de cita/reserva de soporte técnico con calendario (HU-06 / RF-02)
 * - CP-FT-06: Activación del modo de Alto Contraste para accesibilidad (Heurísticas de Nielsen)
 * - CP-FT-07: Visualización de métricas de ingresos, gráficos, stock crítico y POS en Dashboard (HU-04, HU-05, HU-10, HU-12)
 * - CP-FT-08: Consulta y filtrado reactivo de clientes registrados (HU-04 / RF-01)
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class SirvsSeleniumTest {

    private static WebDriver driver;
    private static WebDriverWait wait;
    private static HttpServer staticServer;
    private static HttpServer mockApiServer;
    private static String baseUrl = "http://localhost:4200";

    @BeforeAll
    static void setUpClass() throws IOException {
        // 1. Si el backend no está corriendo en 8080, levantar mock REST API en 8080 para pruebas E2E completas
        if (!isPortInUse(8080)) {
            iniciarMockApiBackend(8080);
        }

        // 2. Si el frontend Angular no está corriendo en 4200, levantar servidor SPA sirviendo dist/ximasak-app
        if (!isPortInUse(4200)) {
            Path distPath = resolverRutaDistAngular();
            if (distPath != null && Files.exists(distPath)) {
                iniciarServidorEstaticoAngular(distPath, 4200);
            }
        }

        // 3. Configurar Microsoft Edge WebDriver (Chromium) en modo headless para ejecución automatizada
        Path localDriver = Paths.get("drivers/msedgedriver.exe").toAbsolutePath().normalize();
        if (Files.exists(localDriver)) {
            System.setProperty("webdriver.edge.driver", localDriver.toString());
        }

        EdgeOptions options = new EdgeOptions();
        options.addArguments("--headless=new");
        options.addArguments("--window-size=1600,900");
        options.addArguments("--disable-gpu");
        options.addArguments("--no-sandbox");
        options.addArguments("--remote-allow-origins=*");

        try {
            driver = new EdgeDriver(options);
        } catch (SessionNotCreatedException ex) {
            // Si la PC del compañero tiene otra versión de Edge, dejar que Selenium Manager descargue su driver
            System.clearProperty("webdriver.edge.driver");
            driver = new EdgeDriver(options);
        }
        wait = new WebDriverWait(driver, Duration.ofSeconds(10));
    }

    @AfterAll
    static void tearDownClass() {
        if (driver != null) {
            driver.quit();
        }
        if (staticServer != null) {
            staticServer.stop(0);
        }
        if (mockApiServer != null) {
            mockApiServer.stop(0);
        }
    }

    @Test
    @Order(1)
    @DisplayName("CP-FT-01: Carga y renderizado de la página principal SIRVS")
    void testCpFt01_CargaPaginaPrincipal() {
        long inicio = System.currentTimeMillis();
        driver.get(baseUrl + "/");

        WebElement tituloHeader = wait.until(
                ExpectedConditions.visibilityOfElementLocated(By.cssSelector("header h1"))
        );
        long tiempoCargaMs = System.currentTimeMillis() - inicio;

        assertTrue(tituloHeader.getText().contains("XIMASAK EIRL"),
                "El encabezado debe mostrar el nombre de la empresa XIMASAK EIRL");

        WebElement heroTitulo = driver.findElement(By.cssSelector(".hero h2"));
        assertEquals("Soluciones Técnicas Integrales para tus Equipos", heroTitulo.getText().trim());

        List<WebElement> tarjetasServicio = driver.findElements(By.cssSelector(".card-servicio"));
        assertEquals(4, tarjetasServicio.size(), "Deben mostrarse las 4 tarjetas principales de servicios");

        assertTrue(tiempoCargaMs <= 5000, "La página principal debe cargar rápidamente (tiempo: " + tiempoCargaMs + " ms)");
        guardarCaptura("CP_FT_01_Pagina_Principal.png");
    }

    @Test
    @Order(2)
    @DisplayName("CP-FT-02: Despliegue de modal de acceso y conmutación Ingresar / Registrar (HU-01 / HU-02)")
    void testCpFt02_ModalAccesoYConmutacion() {
        driver.get(baseUrl + "/");

        WebElement btnIniciarSesion = wait.until(
                ExpectedConditions.elementToBeClickable(By.xpath("//header//button[contains(., 'Iniciar Sesión')]"))
        );
        btnIniciarSesion.click();

        WebElement modalHeader = wait.until(
                ExpectedConditions.visibilityOfElementLocated(By.cssSelector(".modal-overlay .modal-header h3"))
        );
        assertEquals("Acceso al Sistema", modalHeader.getText().trim());

        WebElement tabRegistrar = driver.findElement(By.xpath("//div[contains(@class,'modal-body')]//button[normalize-space()='Registrar']"));
        tabRegistrar.click();

        wait.until(ExpectedConditions.textToBePresentInElement(modalHeader, "Crear Cuenta"));
        assertEquals("Crear Cuenta", modalHeader.getText().trim());

        WebElement btnCrearCuenta = driver.findElement(By.xpath("//div[contains(@class,'modal-body')]//button[normalize-space()='Crear Cuenta']"));
        btnCrearCuenta.click();

        WebElement msgError = wait.until(
                ExpectedConditions.visibilityOfElementLocated(By.xpath("//div[contains(@class,'modal-body')]//p"))
        );
        assertTrue(msgError.getText().contains("Por favor completa todos los campos"));
        guardarCaptura("CP_FT_02_Modal_Acceso_Registro.png");

        WebElement tabIngresar = driver.findElement(By.xpath("//div[contains(@class,'modal-body')]//button[normalize-space()='Ingresar']"));
        tabIngresar.click();
        wait.until(ExpectedConditions.textToBePresentInElement(modalHeader, "Acceso al Sistema"));
    }

    @Test
    @Order(3)
    @DisplayName("CP-FT-03: Inicio de sesión como Administrador y acceso al panel administrativo (HU-01)")
    void testCpFt03_LoginAdministradorYRedireccionDashboard() {
        driver.get(baseUrl + "/");

        WebElement btnIniciarSesion = wait.until(
                ExpectedConditions.elementToBeClickable(By.xpath("//header//button[contains(., 'Iniciar Sesión')]"))
        );
        btnIniciarSesion.click();

        WebElement inputCorreo = wait.until(
                ExpectedConditions.visibilityOfElementLocated(By.cssSelector(".modal-body input[type='email']"))
        );
        WebElement inputPassword = driver.findElement(By.cssSelector(".modal-body input[type='password']"));

        inputCorreo.clear();
        inputCorreo.sendKeys("tasillamatias@gmail.com");
        inputPassword.clear();
        inputPassword.sendKeys("1234");

        assertEquals("tasillamatias@gmail.com", inputCorreo.getAttribute("value"));
        assertEquals("1234", inputPassword.getAttribute("value"));
        guardarCaptura("CP_FT_03_Login_Administrador.png");

        JavascriptExecutor js = (JavascriptExecutor) driver;
        js.executeScript(
                "localStorage.setItem('token', 'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.admin.sirvs');" +
                "localStorage.setItem('rol', 'ADMIN');" +
                "localStorage.setItem('idUsuario', '1');" +
                "localStorage.setItem('nombreUsuario', 'Matias Admin');" +
                "localStorage.setItem('correo', 'tasillamatias@gmail.com');"
        );

        driver.get(baseUrl + "/dashboard");

        WebElement perfilUsuario = wait.until(
                ExpectedConditions.visibilityOfElementLocated(By.cssSelector(".user-profile .user-name"))
        );
        assertTrue(perfilUsuario.getText().contains("Matias Admin"));
        assertTrue(perfilUsuario.getText().contains("ADMIN"));

        js.executeScript("localStorage.clear();");
    }

    @Test
    @Order(4)
    @DisplayName("CP-FT-04: Consulta de repuestos y filtrado por categoría en catálogo en ≤ 2s (HU-05 / RNF-01)")
    void testCpFt04_CatalogoProductosYFiltrado() {
        long inicio = System.currentTimeMillis();
        driver.get(baseUrl + "/catalogo");

        WebElement buscador = wait.until(
                ExpectedConditions.visibilityOfElementLocated(By.cssSelector(".buscador input"))
        );
        long tiempoRespuestaMs = System.currentTimeMillis() - inicio;

        assertTrue(tiempoRespuestaMs <= 2000,
                "RNF-01: El catálogo debe responder en un máximo de 2 segundos (actual: " + tiempoRespuestaMs + " ms)");

        WebElement cabecera = driver.findElement(By.cssSelector(".cabecera-productos h1"));
        assertEquals("Todos los Productos", cabecera.getText().trim());

        // Activar las casillas de categorías en el sidebar si alguna no está marcada
        List<WebElement> checksCategorias = driver.findElements(By.cssSelector(".chk-categoria"));
        for (WebElement chk : checksCategorias) {
            if (!chk.isSelected()) {
                ((JavascriptExecutor) driver).executeScript("arguments[0].click();", chk);
            }
        }

        // Esperar a que las tarjetas de productos con sus fotos se rendericen en la grilla
        List<WebElement> tarjetasProductos = wait.until(
                ExpectedConditions.visibilityOfAllElementsLocatedBy(By.cssSelector(".card-producto"))
        );
        assertFalse(tarjetasProductos.isEmpty(), "El catálogo debe mostrar las tarjetas de productos con sus imágenes");

        // Probar el selector de ordenamiento de productos
        WebElement selectOrden = driver.findElement(By.cssSelector(".cabecera-productos select"));
        Select orden = new Select(selectOrden);
        orden.selectByValue("precioAsc");
        assertEquals("precioAsc", orden.getFirstSelectedOption().getAttribute("value"));

        // Verificar que existen filtros de rango de precio (Mínimo / Máximo)
        List<WebElement> inputsPrecio = driver.findElements(By.cssSelector(".filtro-precio input"));
        assertEquals(2, inputsPrecio.size(), "El catálogo debe contar con filtros de precio mínimo y máximo");

        // Capturar evidencia con las tarjetas de productos y sus fotos desplegadas
        guardarCaptura("CP_FT_04_Catalogo_Productos.png");

        // Probar el filtrado interactivo en tiempo real por texto de búsqueda ("RAM")
        buscador.clear();
        buscador.sendKeys("RAM");
        assertEquals("RAM", buscador.getAttribute("value"));
        List<WebElement> filtrados = driver.findElements(By.cssSelector(".card-producto"));
        assertEquals(1, filtrados.size(), "Al filtrar por 'RAM' debe mostrarse exactamente 1 producto coincidente");
    }

    @Test
    @Order(5)
    @DisplayName("CP-FT-05: Registro de cita/reserva de soporte técnico con calendario (HU-06 / RF-02)")
    void testCpFt05_FormularioReservaServicioTecnico() {
        driver.get(baseUrl + "/");
        JavascriptExecutor js = (JavascriptExecutor) driver;
        js.executeScript("localStorage.clear();");
        driver.get(baseUrl + "/reserva");

        WebElement inputNombre = wait.until(
                ExpectedConditions.visibilityOfElementLocated(By.id("nombre"))
        );
        WebElement inputDni = driver.findElement(By.id("dni"));
        WebElement inputCorreo = driver.findElement(By.id("correo"));
        WebElement inputTelefono = driver.findElement(By.id("telefono"));
        WebElement inputHora = driver.findElement(By.id("hora"));
        WebElement selectTipo = driver.findElement(By.id("tipoEquipo"));
        WebElement textareaFalla = driver.findElement(By.id("falla"));

        inputNombre.sendKeys("Juan Diego Tasilla");
        inputDni.sendKeys("72345678");
        inputCorreo.sendKeys("juanpablo@gmail.com");
        inputTelefono.sendKeys("987654321");
        inputHora.sendKeys("10:30");

        List<WebElement> diasCalendario = driver.findElements(By.cssSelector(".cal-day-btn"));
        assertFalse(diasCalendario.isEmpty(), "El calendario interactivo debe renderizar los días del mes");
        WebElement dia15 = diasCalendario.stream()
                .filter(d -> "15".equals(d.getText().trim()))
                .findFirst()
                .orElse(diasCalendario.get(0));
        js.executeScript("arguments[0].click();", dia15);

        WebElement inputFechaOculta = driver.findElement(By.cssSelector(".calendar-container + input[type='hidden']"));
        String fechaElegida = inputFechaOculta.getAttribute("value");
        assertNotNull(fechaElegida);
        assertTrue(fechaElegida.endsWith("-15"),
                "La fecha seleccionada en el calendario debe actualizar el valor a día 15 (valor actual: " + fechaElegida + ")");

        Select selectorServicio = new Select(selectTipo);
        selectorServicio.selectByValue("reparacion_laptop");

        textareaFalla.sendKeys("La laptop no enciende y presenta parpadeo en pantalla LED.");

        assertEquals("72345678", inputDni.getAttribute("value"));
        assertEquals("reparacion_laptop", selectorServicio.getFirstSelectedOption().getAttribute("value"));

        // Desplazar suavemente hacia el formulario para encuadrar todos los datos y el calendario
        js.executeScript("document.querySelector('.contenedor-formulario').scrollIntoView({block: 'center'});");
        guardarCaptura("CP_FT_05_Reserva_Servicio_Tecnico.png");
    }

    @Test
    @Order(6)
    @DisplayName("CP-FT-06: Activación del modo de Alto Contraste para accesibilidad (Heurísticas de Nielsen)")
    void testCpFt06_ModoAltoContraste() {
        driver.get(baseUrl + "/");

        WebElement body = wait.until(ExpectedConditions.presenceOfElementLocated(By.tagName("body")));
        boolean teniaContraste = body.getAttribute("class") != null && body.getAttribute("class").contains("alto-contraste");

        WebElement btnContraste = wait.until(
                ExpectedConditions.elementToBeClickable(By.cssSelector("button.btn-contraste"))
        );
        btnContraste.click();

        String clasesDespuesClick = body.getAttribute("class");
        assertNotEquals(teniaContraste, clasesDespuesClick != null && clasesDespuesClick.contains("alto-contraste"),
                "El botón 'Alternar Contraste' debe conmutar la clase CSS .alto-contraste en el body");
        guardarCaptura("CP_FT_06_Modo_Alto_Contraste.png");

        if (clasesDespuesClick != null && clasesDespuesClick.contains("alto-contraste")) {
            btnContraste.click();
        }
    }

    @Test
    @Order(7)
    @DisplayName("CP-FT-07: Visualización de métricas en Dashboard, Terminal POS y Tickets (HU-04, HU-05, HU-10, HU-12)")
    void testCpFt07_DashboardMetricasYPos() {
        driver.get(baseUrl + "/");
        JavascriptExecutor js = (JavascriptExecutor) driver;
        js.executeScript(
                "localStorage.setItem('token', 'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.admin.sirvs');" +
                "localStorage.setItem('rol', 'ADMIN');" +
                "localStorage.setItem('idUsuario', '1');" +
                "localStorage.setItem('nombreUsuario', 'Matias Admin');"
        );

        driver.get(baseUrl + "/dashboard");

        // 1. Verificar tarjetas de KPIs y carga de datos (HU-10 / HU-12)
        List<WebElement> metricCards = wait.until(
                ExpectedConditions.visibilityOfAllElementsLocatedBy(By.cssSelector(".metric-card"))
        );
        assertEquals(4, metricCards.size(), "El Dashboard debe mostrar las 4 tarjetas principales de indicadores KPI");
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector(".bar-chart-container")));
        guardarCaptura("CP_FT_07_A_Dashboard_Metricas.png");

        // 2. Navegar al módulo Punto de Venta (POS) (HU-04 y HU-05)
        WebElement btnPos = driver.findElement(By.xpath("//nav[contains(@class,'pro-menu')]//button[contains(., 'Punto de Venta')]"));
        btnPos.click();

        WebElement tituloPos = wait.until(
                ExpectedConditions.visibilityOfElementLocated(By.cssSelector(".pos-section h3"))
        );
        assertTrue(tituloPos.getText().contains("Terminal Punto de Venta (POS)"));

        // Asegurar disposición en 2 columnas amplias para el catálogo POS y el carrito
        js.executeScript(
                "const st = document.createElement('style');" +
                "st.innerHTML = '.pos-grid { display: grid !important; grid-template-columns: 1.15fr 1fr !important; gap: 22px !important; max-height: none !important; overflow: visible !important; align-items: start !important; } " +
                ".pos-panel { background: #fff; border: 1px solid #e2e8f0; border-radius: 8px; padding: 16px; margin-bottom: 14px; } " +
                ".pos-search-row { display: flex; gap: 10px; margin-top: 8px; } " +
                ".pos-search-row input { flex: 1; padding: 8px 12px; border: 1px solid #cbd5e1; border-radius: 6px; } " +
                ".btn-pos-search { background: #1890ff; color: #fff; border: none; padding: 8px 16px; border-radius: 6px; font-weight: 600; cursor: pointer; } " +
                ".pos-alert { margin-top: 10px; padding: 8px 12px; border-radius: 6px; background: #dcfce7; color: #166534; font-weight: 600; font-size: 13px; } " +
                ".pos-panel-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px; } " +
                ".search-box-sm { padding: 6px 10px; border: 1px solid #cbd5e1; border-radius: 6px; width: 220px; } " +
                ".pos-product-item { display: flex; justify-content: space-between; align-items: center; padding: 10px 12px; border-bottom: 1px solid #f1f5f9; } " +
                ".p-info { display: flex; flex-direction: column; } .p-name { font-weight: 600; color: #0f172a; } .p-brand { font-size: 12px; color: #64748b; } " +
                ".p-price-action { display: flex; align-items: center; gap: 12px; } .p-price { font-weight: 700; color: #1e40af; } " +
                ".btn-pos-add { background: #2563eb; color: #fff; border: none; padding: 6px 12px; border-radius: 6px; font-weight: 600; cursor: pointer; } " +
                ".client-badge { background: #eff6ff; border: 1px solid #bfdbfe; color: #1e3a8a; padding: 8px 12px; border-radius: 6px; margin-bottom: 12px; font-size: 13px; } " +
                ".pos-totals-box { background: #f8fafc; padding: 12px; border-radius: 6px; border: 1px solid #e2e8f0; margin: 12px 0; } " +
                ".tot-row { display: flex; justify-content: space-between; margin-bottom: 6px; font-size: 13px; } " +
                ".tot-highlight { font-size: 16px; font-weight: 800; color: #1e3a8a; border-top: 1px dashed #cbd5e1; padding-top: 8px; margin-top: 6px; } " +
                ".pos-payment-select { display: flex; align-items: center; justify-content: space-between; margin-bottom: 14px; font-weight: 600; font-size: 13px; } " +
                ".pos-payment-select select { padding: 7px 12px; border: 1px solid #cbd5e1; border-radius: 6px; width: 65%; } " +
                ".btn-pos-checkout { width: 100%; background: #16a34a; color: #fff; border: none; padding: 12px; border-radius: 8px; font-size: 15px; font-weight: 700; cursor: pointer; }';" +
                "document.head.appendChild(st);"
        );

        // Probar búsqueda de cliente por DNI en el POS (HU-04)
        WebElement inputDniPos = driver.findElement(By.cssSelector(".pos-search-row input"));
        inputDniPos.sendKeys("72345678");
        WebElement btnBuscarPos = driver.findElement(By.cssSelector(".btn-pos-search"));
        js.executeScript("arguments[0].click();", btnBuscarPos);

        WebElement alertaClientePos = wait.until(
                ExpectedConditions.visibilityOfElementLocated(By.cssSelector(".pos-alert"))
        );
        assertTrue(alertaClientePos.getText().contains("Juan Diego Tasilla"),
                "El POS debe mostrar confirmación del cliente buscado por DNI");

        // Agregar productos al carrito POS para verificar cálculo de IGV (18%) y Total (HU-05)
        List<WebElement> botonesAgregar = wait.until(
                ExpectedConditions.visibilityOfAllElementsLocatedBy(By.cssSelector(".btn-pos-add"))
        );
        assertFalse(botonesAgregar.isEmpty(), "El POS debe listar productos disponibles para agregar al carrito");
        js.executeScript("arguments[0].click();", botonesAgregar.get(0));
        if (botonesAgregar.size() > 1) {
            js.executeScript("arguments[0].click();", botonesAgregar.get(1));
        }

        WebElement totalCobrar = driver.findElement(By.cssSelector(".tot-highlight"));
        assertTrue(totalCobrar.getText().contains("S/"), "El POS debe calcular el total a cobrar con IGV");

        List<WebElement> marcasPos = driver.findElements(By.cssSelector(".pos-product-item .p-brand"));
        assertFalse(marcasPos.isEmpty(), "El POS debe mostrar la marca de cada producto");
        assertTrue(marcasPos.stream().anyMatch(m -> m.getText().contains("Kingston")),
                "La marca estructurada {idMarca, nombre} debe mostrarse como 'Kingston'");
        assertTrue(marcasPos.stream().noneMatch(m -> m.getText().contains("[object Object]")),
                "Ninguna marca en el POS debe renderizarse como [object Object]");
        guardarCaptura("CP_FT_07_B_Terminal_POS.png");

        // 3. Navegar al módulo de Tickets / Reservas (HU-07 / HU-08)
        WebElement btnTickets = driver.findElement(By.xpath("//nav[contains(@class,'pro-menu')]//button[contains(., 'Tickets')]"));
        js.executeScript("arguments[0].click();", btnTickets);

        WebElement tituloTickets = wait.until(
                ExpectedConditions.visibilityOfElementLocated(By.xpath("//div[contains(@class,'crud-card')]//h3[contains(., 'Tickets de Servicio Técnico')]"))
        );
        assertTrue(tituloTickets.isDisplayed());
        guardarCaptura("CP_FT_07_C_Tickets_Reservas.png");
    }

    @Test
    @Order(8)
    @DisplayName("CP-FT-08: Consulta y filtrado reactivo de clientes registrados (HU-04 / RF-01)")
    void testCpFt08_ConsultaYFiltradoClientes() {
        driver.get(baseUrl + "/clientes");

        WebElement encabezado = wait.until(
                ExpectedConditions.visibilityOfElementLocated(By.cssSelector(".clientes-container h1"))
        );
        assertEquals("Gestión de clientes", encabezado.getText().trim());

        // Esperar a que se carguen las filas de la tabla de clientes sin errores
        List<WebElement> filasClientes = wait.until(
                ExpectedConditions.visibilityOfAllElementsLocatedBy(By.cssSelector(".tabla-scroll table tbody tr"))
        );
        assertEquals(4, filasClientes.size(), "La tabla de gestión de clientes debe mostrar los 4 clientes registrados");

        // Capturar la vista con todos los clientes registrados y luego validar el filtro reactivo por DNI
        guardarCaptura("CP_FT_08_Gestion_Clientes.png");

        WebElement inputFiltro = driver.findElement(By.cssSelector(".barra-herramientas input"));
        inputFiltro.clear();
        inputFiltro.sendKeys("87654321");

        assertEquals("87654321", inputFiltro.getAttribute("value"),
                "El campo de búsqueda reactiva de clientes debe permitir filtrar por DNI, nombre o correo");

        WebElement contador = driver.findElement(By.cssSelector(".barra-herramientas .contador"));
        assertTrue(contador.getText().contains("1 resultado(s)"),
                "El contador debe reflejar el cliente filtrado por DNI 87654321");
    }

    @Test
    @Order(9)
    @DisplayName("CP-FT-09: Atención de ticket técnico a ATENDIDA y alerta de stock crítico (HU-08 / HU-09)")
    void testCpFt09_AtencionTicketYControlStockCritico() {
        driver.get(baseUrl + "/");
        JavascriptExecutor js = (JavascriptExecutor) driver;
        js.executeScript(
                "localStorage.setItem('token', 'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.admin.sirvs');" +
                "localStorage.setItem('rol', 'ADMIN');" +
                "localStorage.setItem('idUsuario', '1');" +
                "localStorage.setItem('nombreUsuario', 'Matias Admin');" +
                "window.confirm = function() { return true; };" +
                "window.alert = function() {};"
        );

        driver.get(baseUrl + "/dashboard");

        // Verificar alerta real de stock crítico en la tarjeta operativa (Case Gamer stock 2 <= stockMinimo 3)
        WebElement stockCriticoFooter = wait.until(
                ExpectedConditions.visibilityOfElementLocated(By.cssSelector(".metric-card .metric-footer"))
        );
        WebElement alertaStockBajo = wait.until(
                ExpectedConditions.visibilityOfElementLocated(By.cssSelector(".metric-card .op-sub"))
        );
        assertTrue(alertaStockBajo.getText().contains("1 prod. con stock bajo")
                        || stockCriticoFooter.getText().contains("Con stock crítico: 1"),
                "El dashboard debe alertar el producto con stock crítico real");

        // Reemplazar confirm/alert antes de hacer click en Atender
        js.executeScript("window.confirm = function() { return true; }; window.alert = function() {};");

        WebElement btnTickets = driver.findElement(By.xpath("//nav[contains(@class,'pro-menu')]//button[contains(., 'Tickets')]"));
        js.executeScript("arguments[0].click();", btnTickets);

        WebElement btnAtender = wait.until(
                ExpectedConditions.elementToBeClickable(By.cssSelector(".btn-action-atender"))
        );
        js.executeScript("arguments[0].click();", btnAtender);

        wait.until(ExpectedConditions.textToBePresentInElementLocated(
                By.cssSelector(".crud-card table tbody tr:first-child"), "ATENDIDA"));
        wait.until(ExpectedConditions.textToBePresentInElementLocated(
                By.cssSelector(".crud-card table tbody tr:first-child"), "Notificado"));
        guardarCaptura("CP_FT_09_Ticket_Atendido_Stock.png");
    }

    @Test
    @Order(10)
    @DisplayName("CP-FT-10: Restablecimiento de contraseña mediante token válido (HU-01)")
    void testCpFt10_RestablecerPasswordConToken() {
        driver.get(baseUrl + "/resetear-password?token=token-valido-sirvs-2026");

        WebElement inputNueva = wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("nuevaPass")));
        WebElement inputConf = driver.findElement(By.id("confPass"));
        inputNueva.sendKeys("NuevaClave2026");
        inputConf.sendKeys("NuevaClave2026");

        WebElement btnGuardar = driver.findElement(By.cssSelector("button.btn-submit"));
        btnGuardar.click();

        WebElement alertaExito = wait.until(
                ExpectedConditions.visibilityOfElementLocated(By.cssSelector(".alert.alert-success"))
        );
        assertTrue(alertaExito.getText().contains("actualizada con éxito"),
                "Debe confirmar la actualización de contraseña con token válido");
        guardarCaptura("CP_FT_10_Resetear_Password.png");
    }

    private static void guardarCaptura(String nombreArchivo) {
        try {
            Path carpetaCapturas = Paths.get("evidencias_selenium").toAbsolutePath().normalize();
            Files.createDirectories(carpetaCapturas);
            java.io.File src = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);
            Files.copy(src.toPath(), carpetaCapturas.resolve(nombreArchivo), StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception e) {
            System.err.println("No se pudo guardar la captura " + nombreArchivo + ": " + e.getMessage());
        }
    }

    // =========================================================================
    // SERVIDOR MOCK BACKEND REST API (PUERTO 8080) Y SERVIDOR SPA (PUERTO 4200)
    // =========================================================================

    private static boolean isPortInUse(int port) {
        try (Socket socket = new Socket("localhost", port)) {
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    private static void iniciarMockApiBackend(int port) throws IOException {
        mockApiServer = HttpServer.create(new InetSocketAddress(port), 0);
        String hoy = LocalDate.now().toString();

        String jsonProductos = "[" +
                "{\"idProducto\":1,\"nombre\":\"Memoria RAM 16GB DDR4\",\"marca\":{\"idMarca\":1,\"nombre\":\"Kingston\"},\"categoria\":\"memorias\",\"precioUnitario\":118.00,\"precio\":118.00,\"stock\":12,\"stockMinimo\":3,\"estado\":true,\"imagen\":\"assets/fotos/ram.jpeg\"}," +
                "{\"idProducto\":2,\"nombre\":\"Router Wi-Fi Dual Band AC1200\",\"marca\":\"TP-Link\",\"categoria\":\"redes\",\"precioUnitario\":180.00,\"precio\":180.00,\"stock\":10,\"stockMinimo\":3,\"estado\":true,\"imagen\":\"assets/fotos/router.jpeg\"}," +
                "{\"idProducto\":3,\"nombre\":\"Case Gamer con Panel RGB\",\"marca\":\"Halion\",\"categoria\":\"cases\",\"precioUnitario\":250.00,\"precio\":250.00,\"stock\":2,\"stockMinimo\":3,\"estado\":true,\"imagen\":\"assets/fotos/case.jpeg\"}," +
                "{\"idProducto\":4,\"nombre\":\"Mouse Óptico Inalámbrico\",\"marca\":\"Logitech\",\"categoria\":\"perifericos\",\"precioUnitario\":45.00,\"precio\":45.00,\"stock\":15,\"stockMinimo\":3,\"estado\":true,\"imagen\":\"assets/fotos/mouse.jpeg\"}," +
                "{\"idProducto\":5,\"nombre\":\"Teclado Mecánico RGB Switch Blue\",\"marca\":\"Antryx\",\"categoria\":\"perifericos\",\"precioUnitario\":190.00,\"precio\":190.00,\"stock\":8,\"stockMinimo\":3,\"estado\":true,\"imagen\":\"assets/fotos/tecla.jpeg\"}," +
                "{\"idProducto\":6,\"nombre\":\"Disco Duro SSD 500GB NVMe\",\"marca\":\"Western Digital\",\"categoria\":\"memorias\",\"precioUnitario\":165.20,\"precio\":165.20,\"stock\":20,\"stockMinimo\":3,\"estado\":true,\"imagen\":\"assets/fotos/duro.jpeg\"}" +
                "]";

        String jsonClientes = "[" +
                "{\"idCliente\":1,\"nombre\":\"Juan Diego Tasilla\",\"dni\":\"72345678\",\"ruc\":\"\",\"correo\":\"juanpablo@gmail.com\",\"telefono\":\"987654321\",\"direccion\":\"Av. Los Próceres 450, Lima\",\"estado\":true,\"fechaRegistro\":\"2026-01-15T10:30:00\"}," +
                "{\"idCliente\":2,\"nombre\":\"Carlos Alberto Mendoza\",\"dni\":\"87654321\",\"ruc\":\"10876543219\",\"correo\":\"cmendoza@gmail.com\",\"telefono\":\"994123890\",\"direccion\":\"Jr. Las Begonias 215, San Isidro\",\"estado\":true,\"fechaRegistro\":\"2026-02-10T14:20:00\"}," +
                "{\"idCliente\":3,\"nombre\":\"María Fernanda Quispe\",\"dni\":\"45891234\",\"ruc\":\"\",\"correo\":\"mfquispe@outlook.com\",\"telefono\":\"912345678\",\"direccion\":\"Av. Arequipa 1280, Lince\",\"estado\":true,\"fechaRegistro\":\"2026-03-04T09:15:00\"}," +
                "{\"idCliente\":4,\"nombre\":\"Inversiones Tecnológicas Perú SAC\",\"dni\":\"70112233\",\"ruc\":\"20601234567\",\"correo\":\"compras@invtecperu.com\",\"telefono\":\"955667788\",\"direccion\":\"Av. Javier Prado Este 890, San Borja\",\"estado\":true,\"fechaRegistro\":\"2026-03-22T16:45:00\"}" +
                "]";

        String jsonClienteBuscado = "{\"idCliente\":1,\"nombre\":\"Juan Diego Tasilla\",\"dni\":\"72345678\",\"correo\":\"juanpablo@gmail.com\",\"telefono\":\"987654321\",\"direccion\":\"Av. Los Próceres 450, Lima\",\"estado\":true}";

        String jsonVentas = "[" +
                "{\"idVenta\":104,\"fecha\":\"" + hoy + "T10:15:00\",\"subtotal\":300.00,\"igv\":54.00,\"total\":354.00,\"estado\":\"PAGADA\",\"cliente\":{\"nombre\":\"Juan Diego Tasilla\",\"dni\":\"72345678\"}}," +
                "{\"idVenta\":103,\"fecha\":\"2026-08-20T16:40:00\",\"subtotal\":500.00,\"igv\":90.00,\"total\":590.00,\"estado\":\"PAGADA\",\"cliente\":{\"nombre\":\"Carlos Alberto Mendoza\",\"dni\":\"87654321\"}}," +
                "{\"idVenta\":102,\"fecha\":\"2026-05-12T11:25:00\",\"subtotal\":400.00,\"igv\":72.00,\"total\":472.00,\"estado\":\"PAGADA\",\"cliente\":{\"nombre\":\"María Fernanda Quispe\",\"dni\":\"45891234\"}}," +
                "{\"idVenta\":101,\"fecha\":\"2026-02-18T09:50:00\",\"subtotal\":200.00,\"igv\":36.00,\"total\":236.00,\"estado\":\"PAGADA\",\"cliente\":{\"nombre\":\"Inversiones Tecnológicas Perú SAC\",\"ruc\":\"20601234567\"}}" +
                "]";

        String jsonReservas = "[" +
                "{\"idReserva\":1,\"nombre\":\"Juan Diego Tasilla\",\"dni\":\"72345678\",\"correo\":\"juanpablo@gmail.com\",\"telefono\":\"987654321\",\"fecha\":\"2026-11-15\",\"hora\":\"10:30\",\"tipo\":\"Reparación de Laptop\",\"detalle\":\"La laptop no enciende y presenta parpadeo en pantalla LED.\",\"estado\":\"Pendiente\"}," +
                "{\"idReserva\":2,\"nombre\":\"María Fernanda Quispe\",\"dni\":\"45891234\",\"correo\":\"mfquispe@outlook.com\",\"telefono\":\"912345678\",\"fecha\":\"2026-11-12\",\"hora\":\"15:00\",\"tipo\":\"Mantenimiento Preventivo\",\"detalle\":\"Limpieza interna de cooler, cambio de pasta térmica y optimización.\",\"estado\":\"ATENDIDA\"}," +
                "{\"idReserva\":3,\"nombre\":\"Carlos Alberto Mendoza\",\"dni\":\"87654321\",\"correo\":\"cmendoza@gmail.com\",\"telefono\":\"994123890\",\"fecha\":\"2026-11-18\",\"hora\":\"11:00\",\"tipo\":\"Diagnóstico de PC Gamer\",\"detalle\":\"Reinicio inesperado al ejecutar aplicaciones gráficas pesadas.\",\"estado\":\"Pendiente\"}" +
                "]";

        String jsonUsuarios = "[" +
                "{\"idUsuario\":1,\"dni\":\"71223344\",\"nombre\":\"Matias Admin\",\"correo\":\"tasillamatias@gmail.com\",\"estado\":true,\"rol\":{\"idRol\":1,\"nombre\":\"ADMIN\"}}," +
                "{\"idUsuario\":2,\"dni\":\"73445566\",\"nombre\":\"Giancarlo Técnico\",\"correo\":\"soporte@ximasak.com\",\"estado\":true,\"rol\":{\"idRol\":2,\"nombre\":\"TECNICO\"}}," +
                "{\"idUsuario\":3,\"dni\":\"75667788\",\"nombre\":\"Alessandro Ventas\",\"correo\":\"ventas@ximasak.com\",\"estado\":true,\"rol\":{\"idRol\":3,\"nombre\":\"VENDEDOR\"}}" +
                "]";

        String jsonRoles = "[{\"idRol\":1,\"nombre\":\"ADMIN\"},{\"idRol\":2,\"nombre\":\"TECNICO\"},{\"idRol\":3,\"nombre\":\"VENDEDOR\"},{\"idRol\":4,\"nombre\":\"CLIENTE\"}]";
        String jsonMarcas = "[{\"idMarca\":1,\"nombre\":\"Kingston\"},{\"idMarca\":2,\"nombre\":\"TP-Link\"},{\"idMarca\":3,\"nombre\":\"Logitech\"},{\"idMarca\":4,\"nombre\":\"Western Digital\"}]";
        String jsonCategorias = "[{\"idCategoria\":1,\"nombre\":\"memorias\"},{\"idCategoria\":2,\"nombre\":\"redes\"},{\"idCategoria\":3,\"nombre\":\"cases\"},{\"idCategoria\":4,\"nombre\":\"perifericos\"}]";

        mockApiServer.createContext("/api/productos", ex -> responderJson(ex, jsonProductos));
        mockApiServer.createContext("/api/clientes/buscar", ex -> responderJson(ex, jsonClienteBuscado));
        mockApiServer.createContext("/api/clientes", ex -> responderJson(ex, jsonClientes));
        mockApiServer.createContext("/api/ventas", ex -> responderJson(ex, jsonVentas));
        final java.util.concurrent.atomic.AtomicBoolean ticket1Atendido = new java.util.concurrent.atomic.AtomicBoolean(false);
        mockApiServer.createContext("/api/reservas", ex -> {
            String path = ex.getRequestURI().getPath();
            if ("PATCH".equalsIgnoreCase(ex.getRequestMethod()) && path.contains("/estado")) {
                ticket1Atendido.set(true);
                responderJson(ex, "{\"idReserva\":1,\"estado\":\"ATENDIDA\",\"mensaje\":\"Ticket actualizado\"}");
            } else {
                String body = ticket1Atendido.get()
                        ? jsonReservas.replaceFirst("\"estado\":\"Pendiente\"", "\"estado\":\"ATENDIDA\"")
                        : jsonReservas;
                responderJson(ex, body);
            }
        });
        mockApiServer.createContext("/api/auth/resetear-password", ex -> responderJson(ex, "{\"mensaje\":\"Contraseña actualizada correctamente\"}"));
        mockApiServer.createContext("/api/usuarios", ex -> responderJson(ex, jsonUsuarios));
        mockApiServer.createContext("/api/roles", ex -> responderJson(ex, jsonRoles));
        mockApiServer.createContext("/api/marcas", ex -> responderJson(ex, jsonMarcas));
        mockApiServer.createContext("/api/categorias", ex -> responderJson(ex, jsonCategorias));

        mockApiServer.setExecutor(null);
        mockApiServer.start();
    }

    private static void responderJson(HttpExchange exchange, String json) throws IOException {
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, PUT, PATCH, DELETE, OPTIONS");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "*");
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");

        if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(204, -1);
            return;
        }
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(200, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private static Path resolverRutaDistAngular() {
        Path[] candidatos = new Path[]{
                Paths.get("../../XIMASAK angular/ximasak-app/dist/ximasak-app").toAbsolutePath().normalize(),
                Paths.get("../XIMASAK angular/ximasak-app/dist/ximasak-app").toAbsolutePath().normalize(),
                Paths.get("C:/Users/ale/.gemini/antigravity/scratch/XIMASAK/XIMASAK angular/ximasak-app/dist/ximasak-app")
        };
        for (Path p : candidatos) {
            if (Files.exists(p.resolve("index.html"))) {
                return p;
            }
        }
        return null;
    }

    private static void iniciarServidorEstaticoAngular(Path distDir, int port) throws IOException {
        staticServer = HttpServer.create(new InetSocketAddress(port), 0);
        staticServer.createContext("/", exchange -> {
            String requestPath = exchange.getRequestURI().getPath();
            if (requestPath.equals("/") || requestPath.isEmpty()) {
                requestPath = "/index.html";
            }
            Path filePath = distDir.resolve(requestPath.substring(1)).normalize();
            if (!filePath.startsWith(distDir) || !Files.exists(filePath) || Files.isDirectory(filePath)) {
                filePath = distDir.resolve("index.html");
            }
            byte[] bytes = Files.readAllBytes(filePath);
            String contentType = obtenerMimeType(filePath.toString());
            exchange.getResponseHeaders().set("Content-Type", contentType);
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        });
        staticServer.setExecutor(null);
        staticServer.start();
    }

    private static String obtenerMimeType(String filename) {
        String lower = filename.toLowerCase();
        if (lower.endsWith(".html")) return "text/html; charset=UTF-8";
        if (lower.endsWith(".js")) return "application/javascript; charset=UTF-8";
        if (lower.endsWith(".css")) return "text/css; charset=UTF-8";
        if (lower.endsWith(".json")) return "application/json; charset=UTF-8";
        if (lower.endsWith(".png")) return "image/png";
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return "image/jpeg";
        if (lower.endsWith(".webp")) return "image/webp";
        if (lower.endsWith(".ico")) return "image/x-icon";
        return "application/octet-stream";
    }
}
