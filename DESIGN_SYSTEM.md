# Sistema de Diseño: ProOrder Management

## Descripción General
ProOrder Management es una aplicación de compras y seguimiento de pedidos orientada primero a dispositivos móviles (mobile-first), diseñada para pequeñas y medianas empresas. Se centra en la claridad, la eficiencia y la confianza profesional a través de una estética limpia en azul corporativo.

## Identidad Visual
*   **Color Primario:** `#1e3a8a` (Azul Corporativo Profundo)
*   **Fondo:** `#faf8ff` (Superficie limpia y ligeramente fría)
*   **Tipografía:** Inter (Sans-serif, centrada en la legibilidad)
*   **Forma:** Radio de borde de 4px (Round Four) para una sensación moderna pero profesional.

## Paleta de Colores (Tokens)
*   **Superficie (Surface):** `#faf8ff`
*   **Contenedor de Superficie Bajo (Surface Container Low):** `#f2f3ff`
*   **Superficie Brillante (Surface Bright):** `#faf8ff`
*   **Primario (Primary):** `#1e3a8a`
*   **Contorno (Outline):** `#d2d9f4`

## Componentes Compartidos

### TopAppBar (Barra de Aplicación Superior)
*   **Estilo:** Título pequeño "ProcureTrack", avatar principal (leading avatar) e icono de notificación secundario (trailing notification icon).
*   **Diseño:** Altura de 64px, relleno horizontal y separación con borde inferior.

### BottomNavBar (Barra de Navegación Inferior)
*   **Estilo:** 4 destinos (Panel de Control, Pedidos, Inventario, Clientes) con iconos y etiquetas.
*   **Estado Activo:** Fondo de contenedor primario con texto e icono sobre el contenedor primario (on-primary-container).
*   **Elevación:** Plana con borde superior.

## Inventario de Pantallas

### 1. Iniciar Sesión (Login)
*   **Propósito:** Punto de entrada seguro.
*   **Elementos:** Logotipo, campo de correo electrónico, campo de contraseña (con botón para mostrar/ocultar), casilla "Recordarme" y botón de acción principal "Iniciar sesión".

### 2. Panel de Control (Dashboard)
*   **Propósito:** Resumen de la salud del negocio.
*   **Características Clave:**
    *   Tarjetas de resumen (Pedidos Pendientes, Alertas de Stock, Gasto Total, Proveedores Activos).
    *   Lista de Pedidos Recientes con etiquetas de estado (Pendiente, Aprobado).
    *   Sección "Acción Requerida" para tareas urgentes.
    *   Enlaces rápidos para Plantillas, Proveedores, Reportes y Configuración.

### 3. Inventario (Inventory)
*   **Propósito:** Monitoreo y gestión de existencias.
*   **Características:** Barra de búsqueda, botón de filtros, tarjetas de lista de productos que muestran SKU, Categoría, Precio y Nivel de stock con etiquetas de estado codificadas por colores (En Stock, Stock Bajo, Sin Stock).

### 4. Clientes (Customers)
*   **Propósito:** Gestión de relaciones con clientes.
*   **Características:** Búsqueda, filtrado y ordenación. Tarjetas de clientes que muestran información de contacto, valor de vida del cliente (Lifetime Value) y fecha del último pedido.

### 5. Nuevo Pedido (New Order)
*   **Propósito:** Flujo guiado de creación de pedidos.
*   **Estructura:** Asistente (stepper) de 3 pasos (Cliente -> Productos -> Resumen). Lista de selección de clientes con identificadores y correos de contacto.

### 6. Orden Completada (Success Screen)
*   **Propósito:** Confirmación posterior a la compra y opción de compartir.
*   **Características:** Icono de éxito, ID del pedido, monto total, acción primaria para descargar PDF y acciones secundarias para compartir (Correo electrónico, WhatsApp).

### 7. Plantilla de PDF de Orden de Compra (PDF Template)
*   **Propósito:** Generación de documentos formales.
*   **Diseño:** Estilo de documento en blanco limpio con encabezado "ProcureTrack", metadatos de la Orden de Compra (N.º de OC, Fecha, Estado), bloques de Proveedor/Dirección de envío, tabla detallada de elementos de línea y resumen financiero (Subtotal, Impuestos, Envío, Total General). Incluye Términos y Condiciones en la parte inferior.

## Principios de Diseño
*   **Fidelidad:** Reproducción precisa de datos corporativos.
*   **Claridad:** Alto contraste para legibilidad en dispositivos móviles.
*   **Eficiencia:** Flujos guiados y acciones primarias destacadas.
