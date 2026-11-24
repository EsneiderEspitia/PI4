# PROYECTO INTEGRADOR — ENTREGA FASE 4
## SISTEMA INTEGRADO DE GESTIÓN DE INVENTARIOS Y VENTAS PARA VOLCANO BARRILES (SIGEIV-VOLCANO)
### Subsistema de Registro y Radicación de Movimientos de Inventario y Documentos Soporte (RF-05 al RF-09)

---

### PORTADA INSTITUCIONAL Y METADATOS DE ENTREGA

* **Institución Universitaria:** Institución Universitaria Antonio José Camacho (UNIAJC)  
* **Facultad:** Facultad de Ingenierías  
* **Programa Académico:** Tecnología en Sistemas de Información (PTSI)  
* **Asignaturas Integradoras (Semestre 4):**  
  * *Ingeniería de Software I* (Grupo 411)  
  * *Programación II* (Grupo 411)  
  * *Bases de Datos II* (Grupo 411)  
* **Periodo Académico:** 2026-2  
* **Equipo de Desarrollo:**  
  * Angel Esteban Cuenú  
  * Carlos Esneider Espitia Paz  
  * Kevin García Montoya  
  * Angela Perdomo Vélez  
* **Docente Asesor:** Javier Humberto Cortés  
* **Rama Oficial de Desarrollo:** `angel`  
* **Ubicación:** Santiago de Cali, Valle del Cauca, Colombia  
* **Fecha:** Noviembre de 2026  

---

## 1. INTRODUCCIÓN Y ALCANCE DE LA FASE 4

### 1.1 Contexto y Antecedentes
En el marco del Proyecto Integrador de Cuarto Semestre de la UNIAJC, el equipo de desarrollo ha completado de forma secuencial y acumulativa las fases previas del sistema **SIGEIV-Volcano**:
1. **Fase 1 (Arquitectura y Modelo Base):** Implementación de la arquitectura estricta MVC en Java Swing, criptografía SHA-256 en autenticación, transaccionalidad ACID en ventas/compras y formulación metodológica institucional.
2. **Fase 2 (Seguridad, Roles y Control de Acceso):** Incorporación de los 5 roles jerárquicos oficiales (Administrador, Jefe de Producción, Almacén, Ventas, Auditor), atributos `email` y `puedeAutorizar`, bloqueo preventivo tras 3 intentos fallidos y control dinámico del menú lateral en `MainFrame`.
3. **Fase 3 (Bandeja de Autorizaciones Pendientes):** Construcción de la persistencia de movimientos de inventario (`movimiento_inventario`), detalles (`detalle_movimiento`), libro mayor inmutable (`kardex`) y bitácora de auditoría (`auditoria`), junto con la pantalla `AutorizacionesPanel.java` para que los roles directivos aprueben o rechacen solicitudes.

### 1.2 Objetivo Central de la Fase 4
La Fase 3 dotó al sistema de la capacidad de *autorizar o rechazar* documentos, pero existía una brecha operativa evidente en la interfaz de usuario: **no existía el módulo visual ni los métodos transaccionales para que el Encargado de Almacén u Operaciones radicara y emitiera formalmente dichos documentos soporte**.

La **Fase 4** resuelve de raíz esta problemática implementando el **Subsistema de Registro y Radicación de Movimientos de Inventario y Documentos Soporte (RF-05, RF-06, RF-07, RF-08, RF-09)**:
* **RF-05 (Orden de Entrega de Lote de Producción):** Entrada formal de asadores terminados desde el taller metalmecánico a la bodega con N.° de Orden de Producción (OP) y N.° de Lote.
* **RF-06 (Remisión de Despacho de Venta Comercial):** Salida formal de asadores vinculada a factura y remisión comercial.
* **RF-07 (Acta de Devolución de Cliente con Dictamen Técnico):** Entrada condicionada por garantía con dictamen de calidad (*Apto reingreso*, *Reproceso taller*, *Daño no imputable*).
* **RF-08 (Acta de Ajuste Físico por Conteo):** Ajuste de inventario con cálculo automático de discrepancias en tiempo real (faltante/sobrante) y causal técnica justificada.
* **RF-09 (Acta de Baja por Merma Técnica / Avería):** Salida definitiva por daño irreparable o fallo metalúrgico en láminas o soldadura TIG.

Todos los documentos radicados se registran de forma atómica bajo estado `PENDIENTE`, garantizando el principio de segregación de funciones (el operador no altera el stock directamente; debe ser avalado en la Bandeja de Autorizaciones).

---

## 2. ESPECIFICACIÓN FORMAL DE REQUERIMIENTOS Y CASOS DE USO (ESTÁNDAR UNIAJC)

Siguiendo las directrices oficiales de la cátedra de Ingeniería de Software UNIAJC, cada Requerimiento Funcional (RF) se especifica en la tabla oficial de 6 secciones con fila combinada `ESPECIFICACIÓN`, e inmediatamente después se detalla su correspondiente **Caso de Uso (CU)** con banners combinados y flujos de eventos.

---

### 2.1 Requerimiento Funcional RF-05: Orden de Entrega de Lote de Producción

| Campo | Especificación |
| :--- | :--- |
| **Nombre del Requerimiento** | RF-05. Radicación de Orden de Entrega de Lote de Producción |
| **Funcionalidad** | El sistema debe permitir registrar el ingreso formal de asadores terminados desde el taller de manufactura hacia la bodega mediante una orden de entrega de lote vinculada a su Orden de Producción (OP). |
| <div align="center">**ESPECIFICACIÓN**</div> | |
| **Resumen** | • El Encargado de Almacén selecciona la pestaña de Entrega de Lote y elige el modelo de asador fabricado.<br>• El sistema consulta la base de datos y presenta en pantalla el SKU, stock actual en bodega y costo estándar de fabricación.<br>• El usuario ingresa el N.° de Orden de Producción (OP), el código de Lote, la cantidad de asadores ensamblados y las notas de inspección de calidad.<br>• Si los datos son válidos, el sistema radica el documento en estado `PENDIENTE`, genera el consecutivo oficial (ej. `OP-2026-003`), registra la auditoría y lo canaliza a la Bandeja de Autorizaciones.<br>• Si los campos están vacíos o la cantidad es menor o igual a cero, emite un mensaje de error impidiendo la radicación. |
| **Entrada** | Asador seleccionado, N.° de Orden de Producción (OP), código de lote, cantidad de unidades ensambladas, costo unitario, observaciones técnicas. |
| **Salida** | • Diálogo de confirmación: *"¡ORDEN DE ENTREGA DE LOTE RADICADA EXITOSAMENTE! N.° Documento Soporte: OP-2026-XXX... Estado: PENDIENTE"*, formulario limpio e historial actualizado.<br>• Mensaje de error: *"El numero de Orden de Produccion (OP) es obligatorio."* o *"La cantidad ensamblada del lote debe ser mayor a cero."* |

#### Caso de Uso Detallado CU05: Radicar Orden de Entrega de Lote

| CASO DE USO: CU05 — RADICAR ORDEN DE ENTREGA DE LOTE DE PRODUCCIÓN | |
| :--- | :--- |
| **Nombre del caso** | Radicar orden de entrega de lote de producción |
| **Actores** | Encargado de Almacén y Bodega (Actor Primario), Administrador (Actor Secundario) |
| **Descripción** | Permite registrar formalmente el ingreso de un lote de asadores ensamblados provenientes del taller metalmecánico, dejándolo listo para dictamen en la Bandeja de Autorizaciones. |
| **Precondiciones** | El usuario debe haber iniciado sesión con rol de Almacén o Administrador y el asador debe existir en el catálogo activo de productos. |
| **Postcondiciones** | Se crea un registro en `movimiento_inventario` con estado `PENDIENTE`, sus renglones en `detalle_movimiento` y un sello en `auditoria`. El stock físico en bodega **no se incrementa** hasta que el Jefe de Producción lo apruebe. |
| **Reglas de negocio** | • La cantidad debe ser un entero estrictamente mayor a 0.<br>• El N.° de OP y el Lote no pueden ser nulos ni vacíos.<br>• El documento generado requiere autorización obligatoria del Jefe de Producción o Administrador. |
| **Referencias** | RF-05 - Radicación de Orden de Entrega de Lote de Producción |
| <div align="center">**ESTRUCTURA DE FLUJOS DE EVENTOS**</div> | |
| **Flujo principal<br>(Escenario exitoso)** | 1. El actor ingresa al panel de Movimientos y selecciona la pestaña "Lote de Producción".<br>2. El sistema carga el catálogo de asadores y genera automáticamente el siguiente consecutivo oficial (`OP-2026-XXX`).<br>3. El actor selecciona el asador, visualiza el SKU y costo unitario estándar.<br>4. El actor digita el N.° de OP, N.° de Lote, cantidad terminada y observaciones técnicas.<br>5. El actor presiona el botón "Radicar Entrega de Lote".<br>6. El sistema ejecuta una transacción ACID: inserta la cabecera del movimiento en estado `PENDIENTE`, inserta el detalle del producto, registra el evento en auditoría y confirma la transacción.<br>7. El sistema notifica al usuario el éxito de la radicación, limpia el formulario y actualiza la tabla de historial. |
| **Flujos alternativos** | • **4a. El actor no modifica el consecutivo sugerido:**<br>  4a.1 El sistema toma el correlativo automático generado por la base de datos.<br>  4a.2 Continúa en el paso 5. |
| **Flujos de excepción** | • **4b. Datos obligatorios incompletos o cantidad inválida:**<br>  4b.1 El sistema detecta campos vacíos o cantidad $\le 0$.<br>  4b.2 El sistema emite: *"Ingrese una cantidad entera valida mayor a 0."* y detiene la radicación.<br>• **6a. Error transaccional en base de datos:**<br>  6a.1 Se produce un fallo en la conexión JDBC.<br>  6a.2 El sistema ejecuta `rollback()`, no altera tablas y emite: *"Error al radicar orden de entrega de lote: [detalle]"*. |

---

### 2.2 Requerimiento Funcional RF-06: Remisión de Despacho Comercial de Ventas

| Campo | Especificación |
| :--- | :--- |
| **Nombre del Requerimiento** | RF-06. Emisión de Remisión de Despacho Comercial de Venta |
| **Funcionalidad** | El sistema debe generar automáticamente una remisión formal de despacho de inventario (tipo `SAL_VENT`) asociada a cada venta concretada, garantizando trazabilidad total con el Kardex. |
| <div align="center">**ESPECIFICACIÓN**</div> | |
| **Resumen** | • El Asesor Comercial o Almacén procesa una venta desde el módulo de Ventas.<br>• El sistema descuenta el stock de forma atómica en la transacción de venta.<br>• De manera complementaria e integrada, el sistema radica automáticamente un movimiento de inventario de tipo `SAL_VENT` con código `REM-VTA-[ID_VENTA]`.<br>• Si la venta se procesa con éxito, el sistema notifica tanto el registro de la factura como la radicación de la remisión de despacho de inventario.<br>• Si la venta falla por falta de existencias, no se emite ninguna remisión de despacho. |
| **Entrada** | Venta confirmada, cliente receptor, listado de productos y cantidades vendidas. |
| **Salida** | • Diálogo de confirmación: *"Venta #X procesada exitosamente. Remisión de despacho N.° REM-VTA-X radicada para inventario."*<br>• Mensaje de error: *"Error al registrar la venta. Verifique existencias de stock."* |

#### Caso de Uso Detallado CU06: Emitir Remisión de Despacho Comercial

| CASO DE USO: CU06 — EMITIR REMISIÓN DE DESPACHO COMERCIAL | |
| :--- | :--- |
| **Nombre del caso** | Emitir remisión de despacho comercial de venta |
| **Actores** | Asesor Comercial / Ventas (Actor Primario), Encargado de Almacén (Actor Secundario) |
| **Descripción** | Permite enlazar automáticamente la transacción comercial de venta con el subsistema de movimientos de inventario, generando la remisión de salida física correspondiente. |
| **Precondiciones** | Debe existir un cliente seleccionado, asadores agregados al carrito de compra y stock suficiente en bodega. |
| **Postcondiciones** | Se registra la venta, sus detalles, se descuenta el stock del asador y se crea la remisión `REM-VTA-XXX` en `movimiento_inventario`. |
| **Reglas de negocio** | • El stock disponible debe ser mayor o igual a la cantidad vendida.<br>• El documento generado se asocia al usuario logueado en la sesión de venta. |
| **Referencias** | RF-06 - Emisión de Remisión de Despacho Comercial de Venta |
| <div align="center">**ESTRUCTURA DE FLUJOS DE EVENTOS**</div> | |
| **Flujo principal<br>(Escenario exitoso)** | 1. El actor selecciona cliente y productos en el panel de Ventas y pulsa "Realizar Venta".<br>2. El sistema valida suficiencia de stock para todos los productos.<br>3. El sistema ejecuta la transacción de venta registrando factura y detalles.<br>4. El sistema invoca al `MovimientoInventarioController` para radicar la remisión `REM-VTA-[ID]`.<br>5. El sistema notifica la venta y la remisión al actor, limpiando el carrito. |
| **Flujos alternativos** | • **3a. Venta con múltiples renglones:**<br>  3a.1 El sistema genera un detalle de movimiento por cada ítem del carrito.<br>  3a.2 Continúa en el paso 4. |
| **Flujos de excepción** | • **2a. Stock insuficiente en uno o más productos:**<br>  2a.1 El sistema detecta que la cantidad supera las existencias.<br>  2a.2 Emite: *"Stock insuficiente para el producto [SKU]"* y aborta la transacción. |

---

### 2.3 Requerimiento Funcional RF-07: Acta de Devolución de Cliente con Dictamen

| Campo | Especificación |
| :--- | :--- |
| **Nombre del Requerimiento** | RF-07. Radicación de Acta de Devolución de Cliente con Dictamen Técnico |
| **Funcionalidad** | El sistema debe permitir registrar la radicación de asadores devueltos por clientes bajo trámite de garantía, exigiendo el número de factura/remisión origen y un dictamen técnico de calidad preliminar. |
| <div align="center">**ESPECIFICACIÓN**</div> | |
| **Resumen** | • El usuario ingresa a la pestaña de Devolución de Cliente y selecciona el asador retornado.<br>• El usuario ingresa el número de factura origen, cantidad devuelta y selecciona el Dictamen Técnico de calidad.<br>• El usuario detalla la causal técnica o inconformidad manifestada por el cliente.<br>• El sistema valida que la cantidad sea mayor a cero y que la factura no esté vacía.<br>• El sistema radica el movimiento bajo tipo `DEV_CLIE` en estado `PENDIENTE` con prefijo oficial `DEV-2026-XXX`, enviándolo a la Bandeja de Autorizaciones del Jefe de Producción. |
| **Entrada** | Asador devuelto, N.° de factura/remisión origen, cantidad de unidades, dictamen técnico preliminar (combo), observaciones de garantía. |
| **Salida** | • Diálogo de confirmación: *"¡ACTA DE DEVOLUCION RADICADA EXITOSAMENTE! N.° Documento Soporte: DEV-2026-XXX... Estado: PENDIENTE"*, formulario limpio e historial actualizado.<br>• Mensaje de error: *"El numero de factura o remision origen es obligatorio."* o *"El dictamen tecnico de calidad es obligatorio."* |

#### Caso de Uso Detallado CU07: Radicar Acta de Devolución de Cliente

| CASO DE USO: CU07 — RADICAR ACTA DE DEVOLUCIÓN DE CLIENTE | |
| :--- | :--- |
| **Nombre del caso** | Radicar acta de devolución de cliente |
| **Actores** | Encargado de Almacén (Actor Primario), Asesor Comercial (Actor Secundario), Jefe de Producción (Autorizador) |
| **Descripción** | Permite asentar formalmente la recepción de un asador devuelto por garantía, registrando el dictamen preliminar para su evaluación técnica. |
| **Precondiciones** | El usuario debe estar autenticado en el sistema y el producto debe corresponder a una referencia registrada. |
| **Postcondiciones** | Se crea el documento soporte `DEV-2026-XXX` en estado `PENDIENTE`. El inventario en bodega no se incrementa hasta que el Jefe de Producción autorice el reingreso. |
| **Reglas de negocio** | • El dictamen técnico debe seleccionarse obligatoriamente entre: *Apto para reingreso*, *Requiere reproceso en taller* o *Daño no imputable / Rechazada*.<br>• La cantidad debe ser superior a 0. |
| **Referencias** | RF-07 - Radicación de Acta de Devolución de Cliente con Dictamen Técnico |
| <div align="center">**ESTRUCTURA DE FLUJOS DE EVENTOS**</div> | |
| **Flujo principal<br>(Escenario exitoso)** | 1. El actor accede a la pestaña "Devolución Cliente (RF-07)".<br>2. El sistema presenta el formulario y genera el consecutivo oficial `DEV-2026-XXX`.<br>3. El actor selecciona el asador y digita el número de factura origen.<br>4. El actor selecciona el dictamen preliminar del combo desplegable.<br>5. El actor ingresa el detalle del diagnóstico y presiona "Radicar Devolución de Cliente".<br>6. El sistema guarda la cabecera y el detalle en estado `PENDIENTE` y asienta el log de auditoría.<br>7. El sistema confirma la radicación exitosa y refresca el historial. |
| **Flujos alternativos** | • **4a. Devolución dictaminada como daño no imputable:**<br>  4a.1 El actor selecciona dictamen de garantía rechazada.<br>  4a.2 El documento se radica con dicha observación para dictamen final gerencial. |
| **Flujos de excepción** | • **3a. Factura origen vacía:**<br>  3a.1 El sistema detecta campo de factura en blanco.<br>  3a.2 Emite: *"El numero de factura o remision origen es obligatorio."* y bloquea el envío. |

---

### 2.4 Requerimiento Funcional RF-08: Acta de Ajuste por Inventario Físico

| Campo | Especificación |
| :--- | :--- |
| **Nombre del Requerimiento** | RF-08. Radicación de Acta de Ajuste por Inventario Físico |
| **Funcionalidad** | El sistema debe permitir registrar las discrepancias detectadas entre el conteo físico real en bodega y las existencias teóricas del sistema, calculando en tiempo real la diferencia (faltante/sobrante) y exigiendo una causal técnica justificada. |
| <div align="center">**ESPECIFICACIÓN**</div> | |
| **Resumen** | • El Encargado de Almacén selecciona el asador sometido a conteo físico cíclico.<br>• El sistema consulta la base de datos y presenta el Stock Teórico del Sistema.<br>• El usuario ingresa el Conteo Físico Real verificado en bodega.<br>• El sistema calcula y muestra dinámicamente la discrepancia: si el conteo es mayor, indica `SOBRANTE (+X un.)` en verde; si es menor, indica `FALTANTE (-X un.)` en rojo; si es igual, indica `Exacto (0 un.)`.<br>• El usuario selecciona la causal técnica justificada y escribe las observaciones.<br>• Al radicar, el sistema genera el soporte `AJU-2026-XXX` en estado `PENDIENTE`, canalizándolo para **autorización indelegable del Administrador / Gerente General**. |
| **Entrada** | Asador a conciliar, conteo físico real verificado, stock teórico actual, causal técnica (combo), observaciones justificativas. |
| **Salida** | • Diálogo de confirmación: *"¡ACTA DE AJUSTE FISICO RADICADA EXITOSAMENTE! N.° Documento Soporte: AJU-2026-XXX... Estado: PENDIENTE (Requiere autorizacion indelegable de Gerencia General)"*.<br>• Mensaje de error: *"El conteo fisico real no puede ser un valor negativo."* o *"Debe seleccionar una causal tecnica justificada para el ajuste."* |

#### Caso de Uso Detallado CU08: Radicar Acta de Ajuste por Inventario Físico

| CASO DE USO: CU08 — RADICAR ACTA DE AJUSTE POR INVENTARIO FÍSICO | |
| :--- | :--- |
| **Nombre del caso** | Radicar acta de ajuste por inventario físico |
| **Actores** | Encargado de Almacén (Actor Primario), Administrador / Gerente General (Autorizador Exclusivo) |
| **Descripción** | Permite conciliar discrepancias de existencias en bodega, documentando la causa raíz del descuadre para aprobación gerencial. |
| **Precondiciones** | El usuario debe estar autenticado y el producto debe encontrarse registrado en el inventario. |
| **Postcondiciones** | Se radica el documento `AJU-2026-XXX` en estado `PENDIENTE`. El stock del asador no se altera hasta que el Gerente General lo apruebe formalmente en su bandeja. |
| **Reglas de negocio** | • El conteo físico no puede ser negativo ($\ge 0$).<br>• El documento requiere de forma indelegable la aprobación del Administrador / Gerente General (Rol 1).<br>• El detalle del movimiento almacena el conteo físico real como saldo final a conciliar. |
| **Referencias** | RF-08 - Radicación de Acta de Ajuste por Inventario Físico |
| <div align="center">**ESTRUCTURA DE FLUJOS DE EVENTOS**</div> | |
| **Flujo principal<br>(Escenario exitoso)** | 1. El actor ingresa a la pestaña "Ajuste Físico (RF-08)".<br>2. El sistema carga el producto seleccionado y muestra el stock teórico actual.<br>3. El actor digita el conteo físico real verificado en bodega.<br>4. El sistema calcula en caliente la discrepancia y actualiza la etiqueta de estado y color visual.<br>5. El actor selecciona la causal justificada (ej. "Error de digitación en despacho anterior") y redacta las notas.<br>6. El actor pulsa "Radicar Acta de Ajuste Físico".<br>7. El sistema valida la información, genera `AJU-2026-XXX` en estado `PENDIENTE`, registra en auditoría y confirma al usuario. |
| **Flujos alternativos** | • **4a. El conteo físico coincide exactamente con el stock teórico:**<br>  4a.1 El sistema reporta discrepancia 0 (Exacto).<br>  4a.2 El actor puede radicar el acta como constancia de verificación cíclica satisfactoria. |
| **Flujos de excepción** | • **3a. Conteo con formato alfanumérico o negativo:**<br>  3a.1 El sistema detecta valor no numérico o $< 0$.<br>  3a.2 Emite: *"El conteo fisico real debe ser un entero no negativo."* y aborta la operación. |

---

### 2.5 Requerimiento Funcional RF-09: Acta de Baja por Merma Técnica / Avería

| Campo | Especificación |
| :--- | :--- |
| **Nombre del Requerimiento** | RF-09. Radicación de Acta de Baja por Merma Técnica / Avería |
| **Funcionalidad** | El sistema debe permitir registrar solicitudes de baja definitiva de asadores que hayan sufrido daños irreparables o mermas técnicas en manufactura o transporte, validando existencias y exigiendo justificación. |
| <div align="center">**ESPECIFICACIÓN**</div> | |
| **Resumen** | • El Encargado de Almacén o Jefe de Producción selecciona el asador averiado.<br>• El sistema presenta el stock disponible en bodega.<br>• El usuario ingresa la cantidad a dar de baja, selecciona la causal técnica de merma (falla estructural en soldadura TIG, abolladura grave en flete, fisura en lámina AISI, etc.) y escribe la justificación.<br>• El sistema valida preventivamente que la cantidad a dar de baja no supere las existencias físicas en bodega.<br>• Si la validación es exitosa, se genera el documento `BAJ-2026-XXX` en estado `PENDIENTE` para autorización gerencial.<br>• Si la cantidad supera el stock, se bloquea la radicación emitiendo advertencia de stock insuficiente. |
| **Entrada** | Asador averiado, cantidad de unidades a dar de baja, motivo de merma técnica (combo), dictamen técnico justificativo. |
| **Salida** | • Diálogo de confirmación: *"¡ACTA DE BAJA POR MERMA RADICADA EXITOSAMENTE! N.° Documento Soporte: BAJ-2026-XXX... Estado: PENDIENTE (Requiere autorizacion de Gerencia General)"*.<br>• Mensaje de advertencia: *"No puedes dar de baja X unidades. El stock actual en bodega es de solo Y un."* |

#### Caso de Uso Detallado CU09: Radicar Acta de Baja por Merma Técnica

| CASO DE USO: CU09 — RADICAR ACTA DE BAJA POR MERMA TÉCNICA | |
| :--- | :--- |
| **Nombre del caso** | Radicar acta de baja por merma técnica |
| **Actores** | Encargado de Almacén (Actor Primario), Jefe de Producción (Actor Secundario), Administrador (Autorizador Exclusivo) |
| **Descripción** | Permite solicitar la baja definitiva de asadores con averías irreparables para su retiro del inventario oficial. |
| **Precondiciones** | El usuario debe estar autenticado y el asador debe tener existencias disponibles en bodega ($> 0$). |
| **Postcondiciones** | Se radica el documento `BAJ-2026-XXX` en estado `PENDIENTE`. El stock del asador no se reduce hasta que el Gerente General lo apruebe formalmente. |
| **Reglas de negocio** | • La cantidad a dar de baja debe ser $\ge 1$ y no puede superar el stock actual del producto.<br>• La autorización de la baja es potestad exclusiva del Administrador / Gerente General (Rol 1). |
| **Referencias** | RF-09 - Radicación de Acta de Baja por Merma Técnica / Avería |
| <div align="center">**ESTRUCTURA DE FLUJOS DE EVENTOS**</div> | |
| **Flujo principal<br>(Escenario exitoso)** | 1. El actor accede a la pestaña "Baja por Merma (RF-09)".<br>2. El sistema muestra los datos del asador y el stock disponible.<br>3. El actor ingresa la cantidad de asadores dañados.<br>4. El actor selecciona la causal de merma en el combo desplegable.<br>5. El actor escribe el dictamen técnico justificativo y presiona "Radicar Acta de Baja".<br>6. El sistema valida que cantidad $\le$ stock y genera `BAJ-2026-XXX` en estado `PENDIENTE`.<br>7. El sistema confirma la radicación exitosa al usuario y actualiza la grilla de historial. |
| **Flujos alternativos** | • **4a. Merma por ensayo de calidad destructivo:**<br>  4a.1 El actor selecciona dicha causal técnica reglamentaria.<br>  4a.2 Continúa en el paso 5. |
| **Flujos de excepción** | • **3a. Cantidad ingresada excede el stock físico:**<br>  3a.1 El sistema detecta cantidad solicitada $>$ stock actual.<br>  3a.2 Emite aviso: *"No puedes dar de baja X unidades. El stock actual en bodega es de solo Y un."* y aborta la radicación. |

---

### 2.6 Requerimientos No Funcionales de Fase 4

#### RNF-04: Transaccionalidad ACID en Radicación de Movimientos
| Campo | Especificación |
| :--- | :--- |
| **Nombre del Requerimiento** | RNF-04. Transaccionalidad ACID en Radicación de Movimientos |
| **Funcionalidad** | El sistema debe garantizar atomicidad, consistencia, aislamiento y durabilidad (ACID) en cada operación de radicación de documentos soporte mediante transacciones JDBC controladas. |
| <div align="center">**ESPECIFICACIÓN**</div> | |
| **Resumen** | • El operador solicita la radicación de cualquier documento soporte desde la interfaz gráfica.<br>• El DAO desactiva el autocommit (`conn.setAutoCommit(false)`) e inserta de forma encadenada la cabecera en `movimiento_inventario`, los renglones en `detalle_movimiento` y el log en `auditoria`.<br>• Si todas las sentencias se completan sin errores, ejecuta `conn.commit()` garantizando persistencia inmediata.<br>• Si ocurre una falla de red, bloqueo o excepción SQL, el sistema ejecuta de inmediato `conn.rollback()`, restaurando el estado previo de la base de datos sin dejar registros huérfanos. |
| **Entrada** | Objetos `MovimientoInventario` y lista de `DetalleMovimiento` instanciados por el controlador. |
| **Salida** | • Confirmación de persistencia íntegra.<br>• Mensaje de contingencia ante fallos: *"Error durante la radicación transaccional: [detalle de la excepción]"*. |

#### RNF-05: Trazabilidad Integral e Inmutabilidad en Auditoría
| Campo | Especificación |
| :--- | :--- |
| **Nombre del Requerimiento** | RNF-05. Trazabilidad Integral e Inmutabilidad en Auditoría |
| **Funcionalidad** | El sistema debe registrar un asiento cronológico e inalterable en la tabla `auditoria` cada vez que se radique, autorice o rechace un documento soporte de inventario. |
| <div align="center">**ESPECIFICACIÓN**</div> | |
| **Resumen** | • Cada vez que un usuario interactúa con los módulos de movimientos o autorizaciones, el sistema captura automáticamente su ID, fecha/hora exacta del servidor, acción ejecutada y detalle técnico.<br>• El sistema sella la transacción en la tabla `auditoria`.<br>• La tabla no dispone de operaciones de modificación (`UPDATE`) ni borrado (`DELETE`) accesibles desde la interfaz, constituyendo una bitácora forense de solo inserción (*Append-Only*). |
| **Entrada** | ID de usuario en sesión activa, código de acción, identificador del documento soporte y observaciones. |
| **Salida** | • Registro inmutable persistido en SQLite.<br>• Registro visible para roles directivos y auditores en consultas analíticas. |

---

## 3. ARQUITECTURA DE SOFTWARE Y PATRÓN MVC EN FASE 4

Para dar estricto cumplimiento al criterio **SCE2.14** (Arquitectura MVC) y **SCE2.20** (Gestión de Excepciones), el desarrollo se dividió rigurosamente en tres capas desacopladas:

```
┌────────────────────────────────────────────────────────────────────────┐
│                        CAPA DE PRESENTACIÓN (VISTA)                     │
│  - MovimientoInventarioPanel.java (JTabbedPane: Lote, Dev, Aju, Baj)  │
│  - MainFrame.java (Integración en Sidebar con Matriz de Acceso)        │
│  - VentaPanel.java (Enlace automático de despachos RF-06)             │
└────────────────────────────────────┬───────────────────────────────────┘
                                     │ Invoca métodos de negocio
                                     ▼
┌────────────────────────────────────────────────────────────────────────┐
│                      CAPA DE NEGOCIO (CONTROLADOR)                     │
│  - MovimientoInventarioController.java                                 │
│    * radicarEntregaLoteProduccion(...)                                 │
│    * radicarDevolucionCliente(...)                                     │
│    * radicarAjusteFisico(...)                                          │
│    * radicarBajaMermaTecnica(...)                                      │
│    * radicarDespachoVenta(...)                                         │
│    * generarSiguienteNumeroSoporte(...)                                │
│    * ResultadoOperacion (Encapsulador de éxito / mensaje de error)     │
└────────────────────────────────────┬───────────────────────────────────┘
                                     │ Ejecuta persistencia ACID
                                     ▼
┌────────────────────────────────────────────────────────────────────────┐
│                    CAPA DE ACCESO A DATOS (MODELO / DAO)               │
│  - MovimientoInventarioDAO.java                                        │
│    * radicarMovimiento(mov, detalles) -> Transacción JDBC              │
│    * generarSiguienteNumeroSoporte(codigoTipo)                         │
│    * listarTiposMovimiento(), listarTodosMovimientos()                │
│  - Entidades POO:                                                      │
│    * MovimientoInventario.java                                         │
│    * DetalleMovimiento.java                                            │
│    * TipoMovimiento.java                                               │
│    * Kardex.java                                                       │
│    * Auditoria.java                                                    │
│  - Motor Relacional: SQLite 3 (`sigeiv_volcano.db`)                    │
└────────────────────────────────────────────────────────────────────────┘
```

---

## 4. MATRIZ DE CONTROL DE ACCESO EN FASE 4

En concordancia con el modelo de seguridad RBAC de la Fase 2, los permisos de radicación y autorización en la Fase 4 se distribuyen de la siguiente manera:

| Tipo de Documento Soporte | Código | Radica (Crea en PENDIENTE) | Autorizador Requerido (Fase 3) | Efecto en Kardex tras Autorización |
| :--- | :---: | :---: | :---: | :---: |
| **Entrega de Lote de Producción** | `ENT_PROD` | Almacén (3) / Admin (1) | Jefe de Producción (2) o Admin (1) | Entrada (+): Incrementa Stock |
| **Remisión Despacho de Venta** | `SAL_VENT` | Asesor Comercial (4) / Admin (1) | Asesor Comercial (4) o Admin (1) | Salida (-): Descuenta Stock |
| **Devolución de Cliente** | `DEV_CLIE` | Almacén (3) / Admin (1) | Jefe de Producción (2) o Admin (1) | Entrada (+): Incrementa Stock |
| **Ajuste por Inventario Físico** | `AJU_FISI` | Almacén (3) / Admin (1) | **Administrador / Gerente (1)** *(Indelegable)* | Conciliación: Saldo = Conteo Físico |
| **Baja por Merma Técnica** | `BAJ_MERM` | Almacén (3) / Admin (1) | **Administrador / Gerente (1)** *(Indelegable)* | Salida (-): Descuenta Stock |

> [!IMPORTANT]
> **Segregación de Funciones:** El rol *Auditor / Consultor Externo* (Rol 5) cuenta con acceso exclusivamente de consulta y lectura; el botón de radicación de movimientos y la bandeja de autorizaciones están protegidos para impedir cualquier inserción o mutación de datos por parte de este perfil.

---

## 5. VALIDACIÓN TÉCNICA Y EVIDENCIAS DE PRUEBAS AUTOMATIZADAS

Para garantizar que el software opere al **100% libre de fallos** (cumpliendo con **SCE2.8**, **SCE2.12** y **SCE2.20**), se desarrollaron y ejecutaron tres suites de pruebas automatizadas:

### 5.1 Resultados de `TestVerificacionFase4.java` (Suite de Radicación)
```text
==================================================================
   SUITE DE PRUEBAS AUTOMATIZADAS - FASE 4 (UNIAJC)
   Subsistema de Radicación de Documentos Soporte de Inventario
==================================================================

--- 1. Generacion de Consecutivos de Documentos Soporte ---
  [PASS] Prefijo de Entrega de Lote (OP-2026-)
  [PASS] Prefijo de Devolucion Cliente (DEV-2026-)
  [PASS] Prefijo de Ajuste Fisico (AJU-2026-)
  [PASS] Prefijo de Baja por Merma (BAJ-2026-)
  [PASS] Prefijo de Despacho de Venta (REM-2026-)

--- 2. Catalogo Parametrizado de Tipos de Movimiento ---
  [PASS] Catalogo no nulo y contiene los 5 tipos oficiales

--- 3. Validaciones Preventivas de Entrada ---
  [PASS] Rechazo de OP y Lote vacios
  [PASS] Rechazo de cantidad negativa
  [PASS] Rechazo sin sesion de usuario activa

--- 4. Radicacion de Entrega de Lote (RF-05) ---
  [PASS] Radicacion exitosa de orden de lote en estado PENDIENTE

--- 5. Radicacion de Devolucion de Cliente (RF-07) ---
  [PASS] Radicacion exitosa de devolucion con dictamen

--- 6. Radicacion de Ajuste Fisico (RF-08) ---
  [PASS] Radicacion exitosa de ajuste fisico

--- 7. Radicacion de Baja por Merma Tecnica (RF-09) ---
  [PASS] Radicacion exitosa de acta de baja tecnica

--- 8. Verificacion de Flujo hacia la Bandeja de Autorizaciones ---
  [PASS] Documentos radicados aparecen en listarPendientes()
  [PASS] Historial completo contiene documentos registrados
==================================================================
  RESUMEN FINAL FASE 4: 15/15 pruebas superadas (100,0% de exito)
==================================================================
```

### 5.2 Resultados de `TestVerificacionS4.java` (Integración Radicación $\rightarrow$ Autorización)
```text
==================================================================
  INICIANDO SUITE DE PRUEBAS DE INTEGRACION - FASE 3 (OE3)
  Bandeja de Autorizaciones, Kardex Transaccional y Roles Jerarquicos
==================================================================
[PASS] Prueba 1: listarPendientes() retorno 10 documentos pendientes.
[PASS] Prueba 2: obtenerDetalles() retorno 1 renglones. Primer SKU: ASAD-CLA-200L
[PASS] Prueba 3: Segregacion de funciones exitosa (Asesor Comercial bloqueado en Lote).
[PASS] Prueba 4: Segregacion de funciones exitosa (Jefe Produccion bloqueado en Ajuste Fisico).
[PASS] Prueba 5: Autorizacion transaccional ACID completada (Stock previo: 43 -> nuevo: 48, delta: +5).
[PASS] Prueba 6: Rechazo con dictamen tecnico registrado con exito en Auditoria.
==================================================================
  RESUMEN DE PRUEBAS: Exitos: 6 | Fallos: 0 (100,0% de exito)
==================================================================
```

### 5.3 Resultados de `AppTestSuite.java` (Regresión General del Sistema)
```text
=================================================
   SIGEIV-Volcano: Ejecucion de Suite de Pruebas
=================================================
--- 1. Pruebas de Criptografia (HashUtil) ---
  [OK] Generacion SHA-256 coincide con el vector de prueba
  [OK] Hash de texto vacio genera 64 caracteres hex
--- 2. Pruebas de Conexion a Base de Datos ---
  [OK] Obtencion de instancia Singleton ConexionDB
  [OK] Conexion JDBC activa y no cerrada
--- 3. Pruebas de Controlador de Usuarios ---
  [OK] Autenticacion exitosa con credenciales validas de admin
  [OK] Verificacion de rol esAdmin()
  [OK] Rechazo de credenciales incorrectas
  [OK] Rechazo de credenciales vacias
--- 4. Pruebas de Controlador de Productos ---
  [OK] Listado de productos no es nulo ni vacio
  [OK] Busqueda de producto ID #1 existente
  [OK] Filtro por nombre 'Barril' retorna coincidencias
  [OK] Rechazo preventivo de producto con datos negativos o vacios
--- 5. Pruebas de Controlador de Inventario ---
  [OK] Conteo total de productos mayor a 0
  [OK] Valorizacion de inventario es positiva
  [OK] Consulta de lista de stock bajo no nula
--- 6. Pruebas de Venta Transaccional y Validacion de Stock ---
  [OK] Venta rechazada preventivamente por stock insuficiente
=================================================
 Resultado Final: 16/16 pruebas superadas (100,0%)
=================================================
```

---

## 6. REFERENCIAS BIBLIOGRÁFICAS (NORMAS APA 7.ª EDICIÓN)

* Ballou, R. H. (2004). *Logística: Administración de la cadena de suministro* (5.ª ed.). Pearson Educación.
* Chase, R. B., Jacobs, F. R., & Aquilano, N. J. (2018). *Administración de operaciones: Producción y cadena de suministros* (15.ª ed.). McGraw-Hill Interamericana.
* Date, C. J. (2004). *Introducción a los sistemas de bases de datos* (8.ª ed.). Pearson Educación.
* Deitel, P., & Deitel, H. (2017). *Java: Cómo programar* (10.ª ed.). Pearson Educación.
* Elmasri, R., & Navathe, S. B. (2016). *Fundamentals of database systems* (7.ª ed.). Pearson.
* Fowler, M. (2003). *Patterns of enterprise application architecture*. Addison-Wesley Professional.
* IEEE Computer Society. (1998). *IEEE Std 830-1998: IEEE Recommended Practice for Software Requirements Specifications*. IEEE. https://doi.org/10.1109/IEEESTD.1998.88286
* Kendall, K. E., & Kendall, J. E. (2011). *Análisis y diseño de sistemas* (8.ª ed.). Pearson Educación.
* Pressman, R. S. (2010). *Ingeniería del software: Un enfoque práctico* (7.ª ed.). McGraw-Hill Interamericana.
* Silberschatz, A., Korth, H. F., & Sudarshan, S. (2014). *Fundamentos de bases de datos* (6.ª ed.). McGraw-Hill Interamericana.
* Sommerville, I. (2016). *Ingeniería del software* (10.ª ed.). Pearson Educación.

---
*Documento maestro generado para la entrega formal de la Fase 4 del Proyecto Integrador — UNIAJC, Semestre 2026-2.*
