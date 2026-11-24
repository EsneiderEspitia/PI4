package com.sigeiv.test;

import com.sigeiv.controlador.MovimientoInventarioController;
import com.sigeiv.controlador.MovimientoInventarioController.ResultadoOperacion;
import com.sigeiv.controlador.UsuarioController;
import com.sigeiv.dao.ConexionDB;
import com.sigeiv.modelo.MovimientoInventario;
import com.sigeiv.modelo.TipoMovimiento;
import com.sigeiv.modelo.Usuario;

import java.util.List;

/**
 * Suite de Pruebas Automatizadas para la Fase 4 (RF-05, RF-06, RF-07, RF-08, RF-09).
 * Valida la radicacion transaccional de documentos soporte de inventario,
 * la generacion de consecutivos oficiales, validaciones de integridad y flujo hacia la bandeja de pendientes.
 */
public class TestVerificacionFase4 {

    private static int pruebasTotales = 0;
    private static int pruebasExitosas = 0;

    public static void main(String[] args) {
        System.out.println("==================================================================");
        System.out.println("   SUITE DE PRUEBAS AUTOMATIZADAS - FASE 4 (UNIAJC)");
        System.out.println("   Subsistema de Radicación de Documentos Soporte de Inventario");
        System.out.println("==================================================================");

        try {
            // Verificar conexion Singleton
            ConexionDB conexion = ConexionDB.getInstancia();
            if (conexion.getConexion() == null || conexion.getConexion().isClosed()) {
                System.err.println("[ERROR CRITICO] No hay conexion activa a la base de datos.");
                return;
            }

            MovimientoInventarioController movCtrl = new MovimientoInventarioController();
            UsuarioController usuarioCtrl = new UsuarioController();

            // Usuario de prueba: Almacen / Bodega (id_rol = 3) o Administrador (id_rol = 1)
            Usuario usuarioOperador = usuarioCtrl.autenticar("admin", "admin123");
            if (usuarioOperador == null) {
                // Fallback: crear usuario en memoria para la prueba
                usuarioOperador = new Usuario(1, "Operador Almacen", "almacen.test", "hash", "almacen@volcano.co", 3, true, true);
            }

            // 1. Generacion de Consecutivos Oficiales
            testGeneracionConsecutivos(movCtrl);

            // 2. Consulta de Catalogo de Tipos de Movimiento
            testCatalogoTiposMovimiento(movCtrl);

            // 3. Validacion Preventiva contra Datos Invalidos
            testValidacionesPreventivas(movCtrl, usuarioOperador);

            // 4. Radicacion de Entrega de Lote de Produccion (RF-05)
            testRadicacionLoteProduccion(movCtrl, usuarioOperador);

            // 5. Radicacion de Devolucion de Cliente con Dictamen (RF-07)
            testRadicacionDevolucionCliente(movCtrl, usuarioOperador);

            // 6. Radicacion de Ajuste Fisico por Conteo (RF-08)
            testRadicacionAjusteFisico(movCtrl, usuarioOperador);

            // 7. Radicacion de Baja por Merma Tecnica (RF-09)
            testRadicacionBajaMerma(movCtrl, usuarioOperador);

            // 8. Verificacion en Bandeja de Pendientes e Historial
            testVisibilidadEnBandeja(movCtrl);

        } catch (Exception e) {
            System.err.println("[EXCEPCION NO CONTROLADA EN TEST] " + e.getMessage());
            e.printStackTrace();
        }

        System.out.println("==================================================================");
        double porcentaje = (pruebasTotales > 0) ? ((double) pruebasExitosas / pruebasTotales) * 100.0 : 0.0;
        System.out.printf("  RESUMEN FINAL FASE 4: %d/%d pruebas superadas (%.1f%% de exito)\n",
                pruebasExitosas, pruebasTotales, porcentaje);
        System.out.println("==================================================================");
    }

    private static void testGeneracionConsecutivos(MovimientoInventarioController movCtrl) {
        System.out.println("\n--- 1. Generacion de Consecutivos de Documentos Soporte ---");
        evaluar("Prefijo de Entrega de Lote (OP-2026-)", movCtrl.generarSiguienteNumeroSoporte("ENT_PROD").startsWith("OP-2026-"));
        evaluar("Prefijo de Devolucion Cliente (DEV-2026-)", movCtrl.generarSiguienteNumeroSoporte("DEV_CLIE").startsWith("DEV-2026-"));
        evaluar("Prefijo de Ajuste Fisico (AJU-2026-)", movCtrl.generarSiguienteNumeroSoporte("AJU_FISI").startsWith("AJU-2026-"));
        evaluar("Prefijo de Baja por Merma (BAJ-2026-)", movCtrl.generarSiguienteNumeroSoporte("BAJ_MERM").startsWith("BAJ-2026-"));
        evaluar("Prefijo de Despacho de Venta (REM-2026-)", movCtrl.generarSiguienteNumeroSoporte("SAL_VENT").startsWith("REM-2026-"));
    }

    private static void testCatalogoTiposMovimiento(MovimientoInventarioController movCtrl) {
        System.out.println("\n--- 2. Catalogo Parametrizado de Tipos de Movimiento ---");
        List<TipoMovimiento> tipos = movCtrl.listarTiposMovimiento();
        evaluar("Catalogo no nulo y contiene los 5 tipos oficiales", tipos != null && tipos.size() >= 5);
    }

    private static void testValidacionesPreventivas(MovimientoInventarioController movCtrl, Usuario op) {
        System.out.println("\n--- 3. Validaciones Preventivas de Entrada ---");
        ResultadoOperacion r1 = movCtrl.radicarEntregaLoteProduccion("", "", 1, 10, 100000, op, "test");
        evaluar("Rechazo de OP y Lote vacios", !r1.isExito());

        ResultadoOperacion r2 = movCtrl.radicarEntregaLoteProduccion("OP-01", "L-01", 1, -5, 100000, op, "test");
        evaluar("Rechazo de cantidad negativa", !r2.isExito());

        ResultadoOperacion r3 = movCtrl.radicarEntregaLoteProduccion("OP-01", "L-01", 1, 10, 100000, null, "test");
        evaluar("Rechazo sin sesion de usuario activa", !r3.isExito());
    }

    private static void testRadicacionLoteProduccion(MovimientoInventarioController movCtrl, Usuario op) {
        System.out.println("\n--- 4. Radicacion de Entrega de Lote (RF-05) ---");
        ResultadoOperacion res = movCtrl.radicarEntregaLoteProduccion(
            "OP-2026-TIG-TEST",
            "LOTE-TEST-01",
            1, // Barril 200L
            5,
            180000.0,
            op,
            "Prueba automatizada de radicacion de lote"
        );
        evaluar("Radicacion exitosa de orden de lote en estado PENDIENTE", res.isExito());
    }

    private static void testRadicacionDevolucionCliente(MovimientoInventarioController movCtrl, Usuario op) {
        System.out.println("\n--- 5. Radicacion de Devolucion de Cliente (RF-07) ---");
        ResultadoOperacion res = movCtrl.radicarDevolucionCliente(
            "FAC-TEST-999",
            "Apto para Reingreso Inmediato a Stock",
            1,
            1,
            180000.0,
            op,
            "Prueba automatizada de devolucion"
        );
        evaluar("Radicacion exitosa de devolucion con dictamen", res.isExito());
    }

    private static void testRadicacionAjusteFisico(MovimientoInventarioController movCtrl, Usuario op) {
        System.out.println("\n--- 6. Radicacion de Ajuste Fisico (RF-08) ---");
        ResultadoOperacion res = movCtrl.radicarAjusteFisico(
            1,
            28, // Conteo fisico
            25, // Teorico en sistema
            180000.0,
            "Diferencia detectada en conteo cíclico mensual",
            op,
            "Prueba automatizada de ajuste"
        );
        evaluar("Radicacion exitosa de ajuste fisico", res.isExito());
    }

    private static void testRadicacionBajaMerma(MovimientoInventarioController movCtrl, Usuario op) {
        System.out.println("\n--- 7. Radicacion de Baja por Merma Tecnica (RF-09) ---");
        ResultadoOperacion res = movCtrl.radicarBajaMermaTecnica(
            1,
            1,
            180000.0,
            "Fallo estructural irreparable en soldadura TIG",
            op,
            "Prueba automatizada de merma"
        );
        evaluar("Radicacion exitosa de acta de baja tecnica", res.isExito());
    }

    private static void testVisibilidadEnBandeja(MovimientoInventarioController movCtrl) {
        System.out.println("\n--- 8. Verificacion de Flujo hacia la Bandeja de Autorizaciones ---");
        List<MovimientoInventario> pendientes = movCtrl.listarPendientes();
        evaluar("Documentos radicados aparecen en listarPendientes()", pendientes != null && !pendientes.isEmpty());

        List<MovimientoInventario> todos = movCtrl.listarTodosMovimientos();
        evaluar("Historial completo contiene documentos registrados", todos != null && todos.size() >= pendientes.size());
    }

    private static void evaluar(String nombrePrueba, boolean condicion) {
        pruebasTotales++;
        if (condicion) {
            pruebasExitosas++;
            System.out.println("  [PASS] " + nombrePrueba);
        } else {
            System.err.println("  [FAIL] " + nombrePrueba);
        }
    }
}
