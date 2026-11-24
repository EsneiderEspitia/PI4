package com.sigeiv.controlador;

import com.sigeiv.dao.MovimientoInventarioDAO;
import com.sigeiv.modelo.DetalleMovimiento;
import com.sigeiv.modelo.MovimientoInventario;
import com.sigeiv.modelo.TipoMovimiento;
import com.sigeiv.modelo.Usuario;

import java.util.ArrayList;
import java.util.List;

/**
 * Controlador de Logica de Negocio para el Subsistema de Movimientos de Inventario,
 * Documentos Soporte y Bandeja de Autorizaciones (Fase 2 / Fase 3 - OE3, RNF-05).
 */
public class MovimientoInventarioController {

    private final MovimientoInventarioDAO movimientoDAO = new MovimientoInventarioDAO();

    /**
     * Clase auxiliar de resultado de operacion para desacoplar logica y vista.
     */
    public static class ResultadoOperacion {
        private final boolean exito;
        private final String mensaje;

        private ResultadoOperacion(boolean exito, String mensaje) {
            this.exito = exito;
            this.mensaje = mensaje;
        }

        public static ResultadoOperacion ok(String mensaje) {
            return new ResultadoOperacion(true, mensaje);
        }

        public static ResultadoOperacion error(String mensaje) {
            return new ResultadoOperacion(false, mensaje);
        }

        public boolean isExito() {
            return exito;
        }

        public String getMensaje() {
            return mensaje;
        }
    }

    /**
     * Obtiene el listado de todos los movimientos de inventario en estado PENDIENTE.
     */
    public List<MovimientoInventario> listarPendientes() {
        return movimientoDAO.listarPendientes();
    }

    /**
     * Obtiene el detalle de renglones de producto de un movimiento.
     */
    public List<DetalleMovimiento> obtenerDetalles(int idMovimiento) {
        return movimientoDAO.listarDetalles(idMovimiento);
    }

    /**
     * Valida permisos jerarquicos y autoriza formalmente un documento soporte.
     * 
     * Reglas de Negocio Oficiales (Tabla 2 y Seccion 8.3 del README):
     *  - Lote de Produccion (ENT_PROD) / Devolucion (DEV_CLIE) -> Requiere Jefe de Produccion (2) o Admin (1).
     *  - Ajuste Fisico (AJU_FISI) / Baja Tecnica (BAJ_MERM)     -> Requiere Administrador / Gerente General (1).
     *  - Despacho de Venta (SAL_VENT)                           -> Requiere Asesor Comercial (4) o Admin (1).
     */
    public ResultadoOperacion autorizarMovimiento(int idMovimiento, Usuario usuarioLogueado) {
        if (usuarioLogueado == null) {
            return ResultadoOperacion.error("No hay una sesion de usuario activa.");
        }

        MovimientoInventario mov = movimientoDAO.buscarPorId(idMovimiento);
        if (mov == null) {
            return ResultadoOperacion.error("El movimiento seleccionado no existe en la base de datos.");
        }

        if (!mov.isPendiente()) {
            return ResultadoOperacion.error("El documento ya se encuentra en estado: " + mov.getEstadoAutorizacion());
        }

        // Verificacion de rol jerarquico autorizado segun tipo de movimiento
        String codTipo = mov.getCodigoTipoMovimiento();
        int rolUsuario = usuarioLogueado.getIdRol();
        boolean esAdmin = (rolUsuario == 1);
        boolean puedeAutorizar = usuarioLogueado.isPuedeAutorizar() || esAdmin;

        if (!puedeAutorizar) {
            return ResultadoOperacion.error("Tu cuenta de usuario no tiene facultades de autorizacion (puede_autorizar = false).");
        }

        String rolRequeridoTexto;
        boolean rolValido;

        if ("ENT_PROD".equalsIgnoreCase(codTipo) || "DEV_CLIE".equalsIgnoreCase(codTipo)) {
            // Lote de Produccion o Devolucion con Dictamen -> Jefe de Produccion (2) o Admin (1)
            rolValido = esAdmin || (rolUsuario == 2);
            rolRequeridoTexto = "Jefe de Produccion y Calidad o Administrador";
        } else if ("AJU_FISI".equalsIgnoreCase(codTipo) || "BAJ_MERM".equalsIgnoreCase(codTipo)) {
            // Ajuste Fisico o Baja Tecnica -> Autorizacion indelegable de Gerencia General (1)
            rolValido = esAdmin;
            rolRequeridoTexto = "Administrador / Gerente General (Autorizacion Indelegable)";
        } else if ("SAL_VENT".equalsIgnoreCase(codTipo)) {
            // Despacho de Venta -> Asesor Comercial (4) o Admin (1)
            rolValido = esAdmin || (rolUsuario == 4);
            rolRequeridoTexto = "Asesor Comercial / Ventas o Administrador";
        } else {
            // Por defecto, solo Administrador
            rolValido = esAdmin;
            rolRequeridoTexto = "Administrador / Gerente General";
        }

        if (!rolValido) {
            return ResultadoOperacion.error(
                "ACCESO DENEGADO POR CONTROL INTERNO:\n\n" +
                "El documento soporte " + mov.getNumeroDocumentoSoporte() + " (" + mov.getNombreTipoMovimiento() + ")\n" +
                "requiere autorizacion exclusiva de: " + rolRequeridoTexto + ".\n\n" +
                "Tu rol actual es: " + obtenerNombreRol(rolUsuario) + "."
            );
        }

        try {
            boolean exito = movimientoDAO.autorizarMovimiento(idMovimiento, usuarioLogueado.getIdUsuario());
            if (exito) {
                return ResultadoOperacion.ok(
                    "¡DOCUMENTO AUTORIZADO EXITOSAMENTE!\n\n" +
                    "Documento Soporte: " + mov.getNumeroDocumentoSoporte() + "\n" +
                    "Tipo: " + mov.getNombreTipoMovimiento() + "\n" +
                    "Autorizado por: " + usuarioLogueado.getNombreUsuario() + " [" + obtenerNombreRol(rolUsuario) + "]\n\n" +
                    "El stock fisico en bodega ha sido actualizado y el asiento oficial en Kardex ha quedado registrado."
                );
            } else {
                return ResultadoOperacion.error("No fue posible completar la autorizacion del documento.");
            }
        } catch (Exception e) {
            return ResultadoOperacion.error("Error durante la autorizacion transaccional:\n" + e.getMessage());
        }
    }

    /**
     * Valida permisos jerarquicos y rechaza un documento soporte con causal obligatoria.
     */
    public ResultadoOperacion rechazarMovimiento(int idMovimiento, Usuario usuarioLogueado, String motivoRechazo) {
        if (usuarioLogueado == null) {
            return ResultadoOperacion.error("No hay una sesion de usuario activa.");
        }

        if (motivoRechazo == null || motivoRechazo.trim().isEmpty()) {
            return ResultadoOperacion.error("Debe proporcionar una justificacion tecnica obligatoria para el rechazo.");
        }

        MovimientoInventario mov = movimientoDAO.buscarPorId(idMovimiento);
        if (mov == null) {
            return ResultadoOperacion.error("El movimiento seleccionado no existe.");
        }

        if (!mov.isPendiente()) {
            return ResultadoOperacion.error("El documento ya no esta en estado PENDIENTE.");
        }

        // Verificacion de roles
        int rolUsuario = usuarioLogueado.getIdRol();
        boolean esAdmin = (rolUsuario == 1);
        boolean puedeAutorizar = usuarioLogueado.isPuedeAutorizar() || esAdmin;

        if (!puedeAutorizar) {
            return ResultadoOperacion.error("Tu cuenta no cuenta con permisos para rechazar o emitir dictamenes de documentos soporte.");
        }

        try {
            boolean exito = movimientoDAO.rechazarMovimiento(idMovimiento, usuarioLogueado.getIdUsuario(), motivoRechazo.trim());
            if (exito) {
                return ResultadoOperacion.ok(
                    "El documento soporte " + mov.getNumeroDocumentoSoporte() + " ha sido RECHAZADO.\n\n" +
                    "Causal tecnica registrada en auditoria: \"" + motivoRechazo.trim() + "\"\n" +
                    "No se alteraron las existencias del inventario."
                );
            } else {
                return ResultadoOperacion.error("No se pudo completar el rechazo del documento.");
            }
        } catch (Exception e) {
            return ResultadoOperacion.error("Error al procesar el rechazo:\n" + e.getMessage());
        }
    }

    private String obtenerNombreRol(int idRol) {
        switch (idRol) {
            case 1: return "Administrador / Gerente";
            case 2: return "Jefe de Produccion";
            case 3: return "Encargado de Almacen";
            case 4: return "Asesor Comercial";
            case 5: return "Auditor / Consultor";
            default: return "Rol " + idRol;
        }
    }

    // =========================================================================
    // FASE 4: METODOS DE NEGOCIO PARA RADICACION DE DOCUMENTOS SOPORTE
    // =========================================================================

    /**
     * Radica una Orden de Entrega de Lote de Produccion (RF-05).
     * Ingreso formal de asadores terminados desde el taller de manufactura a bodega.
     */
    public ResultadoOperacion radicarEntregaLoteProduccion(String nroOP, String nroLote,
                                                           int idProducto, int cantidad, double costoUnitario,
                                                           Usuario usuarioCreador, String observaciones) {
        if (usuarioCreador == null) {
            return ResultadoOperacion.error("No hay una sesion de usuario activa.");
        }
        if (idProducto <= 0) {
            return ResultadoOperacion.error("Debe seleccionar un producto o asador valido.");
        }
        if (cantidad <= 0) {
            return ResultadoOperacion.error("La cantidad ensamblada del lote debe ser mayor a cero.");
        }
        if (nroOP == null || nroOP.trim().isEmpty()) {
            return ResultadoOperacion.error("El numero de Orden de Produccion (OP) es obligatorio.");
        }
        if (nroLote == null || nroLote.trim().isEmpty()) {
            return ResultadoOperacion.error("El codigo o numero de Lote es obligatorio.");
        }

        String nroDoc = generarSiguienteNumeroSoporte("ENT_PROD");
        String obsFinal = String.format("ORDEN PROD: %s | LOTE: %s | NOTA: %s",
                nroOP.trim(), nroLote.trim(), (observaciones != null ? observaciones.trim() : ""));

        MovimientoInventario mov = new MovimientoInventario();
        mov.setNumeroDocumentoSoporte(nroDoc);
        mov.setIdTipoMovimiento(1); // ENT_PROD
        mov.setIdUsuarioCreador(usuarioCreador.getIdUsuario());
        mov.setObservaciones(obsFinal);

        DetalleMovimiento det = new DetalleMovimiento();
        det.setIdProducto(idProducto);
        det.setCantidad(cantidad);
        det.setCostoUnitario(costoUnitario > 0 ? costoUnitario : 0.0);

        List<DetalleMovimiento> detalles = new ArrayList<>();
        detalles.add(det);

        try {
            boolean exito = movimientoDAO.radicarMovimiento(mov, detalles);
            if (exito) {
                return ResultadoOperacion.ok(
                    "¡ORDEN DE ENTREGA DE LOTE RADICADA EXITOSAMENTE!\n\n" +
                    "N.° Documento Soporte: " + nroDoc + "\n" +
                    "Orden de Produccion: " + nroOP.trim() + " | Lote: " + nroLote.trim() + "\n" +
                    "Cantidad: " + cantidad + " unidades\n" +
                    "Estado: PENDIENTE (Enviado a Bandeja de Autorizaciones del Jefe de Produccion)"
                );
            }
            return ResultadoOperacion.error("No fue posible radicar la entrega de lote.");
        } catch (Exception e) {
            return ResultadoOperacion.error("Error al radicar orden de entrega de lote:\n" + e.getMessage());
        }
    }

    /**
     * Radica un Acta de Devolucion de Cliente con Dictamen Tecnico (RF-07).
     */
    public ResultadoOperacion radicarDevolucionCliente(String nroFactura, String dictamenTecnico,
                                                       int idProducto, int cantidad, double costoUnitario,
                                                       Usuario usuarioCreador, String observaciones) {
        if (usuarioCreador == null) {
            return ResultadoOperacion.error("No hay una sesion de usuario activa.");
        }
        if (idProducto <= 0) {
            return ResultadoOperacion.error("Debe seleccionar un producto o asador valido.");
        }
        if (cantidad <= 0) {
            return ResultadoOperacion.error("La cantidad devuelta debe ser mayor a cero.");
        }
        if (nroFactura == null || nroFactura.trim().isEmpty()) {
            return ResultadoOperacion.error("El numero de factura o remision origen es obligatorio.");
        }
        if (dictamenTecnico == null || dictamenTecnico.trim().isEmpty()) {
            return ResultadoOperacion.error("El dictamen tecnico de calidad es obligatorio.");
        }

        String nroDoc = generarSiguienteNumeroSoporte("DEV_CLIE");
        String obsFinal = String.format("FACTURA: %s | DICTAMEN: %s | MOTIVO: %s",
                nroFactura.trim(), dictamenTecnico.trim(), (observaciones != null ? observaciones.trim() : ""));

        MovimientoInventario mov = new MovimientoInventario();
        mov.setNumeroDocumentoSoporte(nroDoc);
        mov.setIdTipoMovimiento(3); // DEV_CLIE
        mov.setIdUsuarioCreador(usuarioCreador.getIdUsuario());
        mov.setObservaciones(obsFinal);

        DetalleMovimiento det = new DetalleMovimiento();
        det.setIdProducto(idProducto);
        det.setCantidad(cantidad);
        det.setCostoUnitario(costoUnitario > 0 ? costoUnitario : 0.0);

        List<DetalleMovimiento> detalles = new ArrayList<>();
        detalles.add(det);

        try {
            boolean exito = movimientoDAO.radicarMovimiento(mov, detalles);
            if (exito) {
                return ResultadoOperacion.ok(
                    "¡ACTA DE DEVOLUCION RADICADA EXITOSAMENTE!\n\n" +
                    "N.° Documento Soporte: " + nroDoc + "\n" +
                    "Factura de Origen: " + nroFactura.trim() + "\n" +
                    "Dictamen Tecnico: " + dictamenTecnico.trim() + "\n" +
                    "Cantidad: " + cantidad + " unidades\n" +
                    "Estado: PENDIENTE (Enviado a Bandeja de Autorizaciones)"
                );
            }
            return ResultadoOperacion.error("No fue posible radicar la devolucion.");
        } catch (Exception e) {
            return ResultadoOperacion.error("Error al radicar devolucion de cliente:\n" + e.getMessage());
        }
    }

    /**
     * Radica un Acta de Ajuste por Inventario Fisico (RF-08).
     * Exige conteo fisico real y causal justificada.
     */
    public ResultadoOperacion radicarAjusteFisico(int idProducto, int conteoFisico, int stockActualSistema,
                                                  double costoUnitario, String causalTecnica,
                                                  Usuario usuarioCreador, String observaciones) {
        if (usuarioCreador == null) {
            return ResultadoOperacion.error("No hay una sesion de usuario activa.");
        }
        if (idProducto <= 0) {
            return ResultadoOperacion.error("Debe seleccionar un producto o asador valido.");
        }
        if (conteoFisico < 0) {
            return ResultadoOperacion.error("El conteo fisico real no puede ser un valor negativo.");
        }
        if (causalTecnica == null || causalTecnica.trim().isEmpty()) {
            return ResultadoOperacion.error("Debe seleccionar una causal tecnica justificada para el ajuste.");
        }

        int diferencia = conteoFisico - stockActualSistema;
        String tipoDif = diferencia >= 0 ? "SOBRANTE (+ " + diferencia + ")" : "FALTANTE (" + diferencia + ")";
        String nroDoc = generarSiguienteNumeroSoporte("AJU_FISI");

        String obsFinal = String.format("CONTEO FISICO: %d | SISTEMA: %d | DISCREPANCIA: %s | CAUSAL: %s | NOTA: %s",
                conteoFisico, stockActualSistema, tipoDif, causalTecnica.trim(),
                (observaciones != null ? observaciones.trim() : ""));

        MovimientoInventario mov = new MovimientoInventario();
        mov.setNumeroDocumentoSoporte(nroDoc);
        mov.setIdTipoMovimiento(4); // AJU_FISI
        mov.setIdUsuarioCreador(usuarioCreador.getIdUsuario());
        mov.setObservaciones(obsFinal);

        // En ajuste fisico, la cantidad representa el saldo conciliado final
        DetalleMovimiento det = new DetalleMovimiento();
        det.setIdProducto(idProducto);
        det.setCantidad(conteoFisico);
        det.setCostoUnitario(costoUnitario > 0 ? costoUnitario : 0.0);

        List<DetalleMovimiento> detalles = new ArrayList<>();
        detalles.add(det);

        try {
            boolean exito = movimientoDAO.radicarMovimiento(mov, detalles);
            if (exito) {
                return ResultadoOperacion.ok(
                    "¡ACTA DE AJUSTE FISICO RADICADA EXITOSAMENTE!\n\n" +
                    "N.° Documento Soporte: " + nroDoc + "\n" +
                    "Conteo Fisico: " + conteoFisico + " | Sistema: " + stockActualSistema + " (" + tipoDif + ")\n" +
                    "Causal: " + causalTecnica.trim() + "\n" +
                    "Estado: PENDIENTE (Requiere autorizacion indelegable de Gerencia General)"
                );
            }
            return ResultadoOperacion.error("No fue posible radicar el ajuste fisico.");
        } catch (Exception e) {
            return ResultadoOperacion.error("Error al radicar ajuste fisico:\n" + e.getMessage());
        }
    }

    /**
     * Radica un Acta de Baja por Merma Tecnica o Averia Irreparable (RF-09).
     */
    public ResultadoOperacion radicarBajaMermaTecnica(int idProducto, int cantidad, double costoUnitario,
                                                      String motivoMerma, Usuario usuarioCreador, String observaciones) {
        if (usuarioCreador == null) {
            return ResultadoOperacion.error("No hay una sesion de usuario activa.");
        }
        if (idProducto <= 0) {
            return ResultadoOperacion.error("Debe seleccionar un producto o asador valido.");
        }
        if (cantidad <= 0) {
            return ResultadoOperacion.error("La cantidad a dar de baja debe ser mayor a cero.");
        }
        if (motivoMerma == null || motivoMerma.trim().isEmpty()) {
            return ResultadoOperacion.error("Debe seleccionar el motivo o causal tecnica de la merma.");
        }

        String nroDoc = generarSiguienteNumeroSoporte("BAJ_MERM");
        String obsFinal = String.format("MOTIVO BAJA: %s | NOTA: %s",
                motivoMerma.trim(), (observaciones != null ? observaciones.trim() : ""));

        MovimientoInventario mov = new MovimientoInventario();
        mov.setNumeroDocumentoSoporte(nroDoc);
        mov.setIdTipoMovimiento(5); // BAJ_MERM
        mov.setIdUsuarioCreador(usuarioCreador.getIdUsuario());
        mov.setObservaciones(obsFinal);

        DetalleMovimiento det = new DetalleMovimiento();
        det.setIdProducto(idProducto);
        det.setCantidad(cantidad);
        det.setCostoUnitario(costoUnitario > 0 ? costoUnitario : 0.0);

        List<DetalleMovimiento> detalles = new ArrayList<>();
        detalles.add(det);

        try {
            boolean exito = movimientoDAO.radicarMovimiento(mov, detalles);
            if (exito) {
                return ResultadoOperacion.ok(
                    "¡ACTA DE BAJA POR MERMA RADICADA EXITOSAMENTE!\n\n" +
                    "N.° Documento Soporte: " + nroDoc + "\n" +
                    "Cantidad a dar de baja: " + cantidad + " unidades\n" +
                    "Causal: " + motivoMerma.trim() + "\n" +
                    "Estado: PENDIENTE (Requiere autorizacion de Gerencia General)"
                );
            }
            return ResultadoOperacion.error("No fue posible radicar el acta de baja.");
        } catch (Exception e) {
            return ResultadoOperacion.error("Error al radicar baja tecnica:\n" + e.getMessage());
        }
    }

    /**
     * Radica una Remision de Despacho de Venta (RF-06).
     */
    public ResultadoOperacion radicarDespachoVenta(String nroRemision, int idProducto, int cantidad,
                                                  double costoUnitario, Usuario usuarioCreador, String observaciones) {
        if (usuarioCreador == null) {
            return ResultadoOperacion.error("No hay una sesion de usuario activa.");
        }
        if (idProducto <= 0 || cantidad <= 0) {
            return ResultadoOperacion.error("Producto y cantidad validos son obligatorios.");
        }

        String nroDoc = (nroRemision != null && !nroRemision.trim().isEmpty())
                ? nroRemision.trim() : generarSiguienteNumeroSoporte("SAL_VENT");

        MovimientoInventario mov = new MovimientoInventario();
        mov.setNumeroDocumentoSoporte(nroDoc);
        mov.setIdTipoMovimiento(2); // SAL_VENT
        mov.setIdUsuarioCreador(usuarioCreador.getIdUsuario());
        mov.setObservaciones(observaciones != null ? observaciones.trim() : "Despacho comercial de venta");

        DetalleMovimiento det = new DetalleMovimiento();
        det.setIdProducto(idProducto);
        det.setCantidad(cantidad);
        det.setCostoUnitario(costoUnitario);

        List<DetalleMovimiento> detalles = new ArrayList<>();
        detalles.add(det);

        try {
            boolean exito = movimientoDAO.radicarMovimiento(mov, detalles);
            if (exito) {
                return ResultadoOperacion.ok("Remision de despacho " + nroDoc + " registrada correctamente.");
            }
            return ResultadoOperacion.error("No se pudo radicar la remision de despacho.");
        } catch (Exception e) {
            return ResultadoOperacion.error("Error al radicar remision de despacho:\n" + e.getMessage());
        }
    }

    public List<TipoMovimiento> listarTiposMovimiento() {
        return movimientoDAO.listarTiposMovimiento();
    }

    public String generarSiguienteNumeroSoporte(String codigoTipo) {
        return movimientoDAO.generarSiguienteNumeroSoporte(codigoTipo);
    }

    public List<MovimientoInventario> listarTodosMovimientos() {
        return movimientoDAO.listarTodosMovimientos();
    }
}
