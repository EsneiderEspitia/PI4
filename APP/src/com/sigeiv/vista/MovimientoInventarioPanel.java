package com.sigeiv.vista;

import com.sigeiv.controlador.MovimientoInventarioController;
import com.sigeiv.controlador.MovimientoInventarioController.ResultadoOperacion;
import com.sigeiv.controlador.ProductoController;
import com.sigeiv.controlador.UsuarioController;
import com.sigeiv.modelo.MovimientoInventario;
import com.sigeiv.modelo.Producto;
import com.sigeiv.modelo.Usuario;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

/**
 * Panel de Radicacion de Movimientos de Inventario y Documentos Soporte (Fase 4 - RF-05, RF-07, RF-08, RF-09).
 * Permite radicar Ordenes de Entrega de Lote (Produccion), Actas de Devolucion con Dictamen,
 * Actas de Ajuste Fisico por Conteo y Actas de Baja por Merma Tecnica,
 * canalizandolas a la Bandeja de Autorizaciones en estado PENDIENTE.
 */
public class MovimientoInventarioPanel extends JPanel {

    private final UsuarioController usuarioCtrl;
    private final MovimientoInventarioController movCtrl = new MovimientoInventarioController();
    private final ProductoController prodCtrl = new ProductoController();

    private JTabbedPane tabbedPane;

    // Componentes Pestaña 1: Lote de Produccion
    private JComboBox<Producto> cmbProdLote;
    private JLabel lblSkuLote, lblStockLote, lblCostoLote;
    private JTextField txtNroOp, txtNroLote, txtCantLote, txtObsLote;

    // Componentes Pestaña 2: Devolucion de Cliente
    private JComboBox<Producto> cmbProdDev;
    private JLabel lblSkuDev, lblStockDev;
    private JTextField txtFacturaDev, txtCantDev, txtObsDev;
    private JComboBox<String> cmbDictamenDev;

    // Componentes Pestaña 3: Ajuste Fisico por Conteo
    private JComboBox<Producto> cmbProdAju;
    private JLabel lblSkuAju, lblStockSistemaAju, lblDiscrepanciaAju;
    private JTextField txtConteoFisicoAju, txtObsAju;
    private JComboBox<String> cmbCausalAju;

    // Componentes Pestaña 4: Baja por Merma Tecnica
    private JComboBox<Producto> cmbProdBaj;
    private JLabel lblSkuBaj, lblStockBaj;
    private JTextField txtCantBaj, txtObsBaj;
    private JComboBox<String> cmbMotivoBaj;

    // Componentes Pestaña 5: Historial
    private JTable tablaHistorial;
    private DefaultTableModel modeloHistorial;
    private JLabel lblTotalHistorial;

    public MovimientoInventarioPanel(UsuarioController usuarioCtrl) {
        this.usuarioCtrl = usuarioCtrl;
        setBackground(MainFrame.COLOR_FONDO);
        setLayout(new BorderLayout(12, 12));
        setBorder(new EmptyBorder(16, 16, 16, 16));

        inicializarInterfaz();
        cargarProductosEnCombos();
        cargarHistorial();
    }

    private void inicializarInterfaz() {
        // 1. ENCABEZADO
        JPanel panelHeader = new JPanel(new BorderLayout());
        panelHeader.setOpaque(false);

        JPanel panelTitulos = new JPanel();
        panelTitulos.setLayout(new BoxLayout(panelTitulos, BoxLayout.Y_AXIS));
        panelTitulos.setOpaque(false);

        JLabel lblTitulo = new JLabel("Radicación de Movimientos y Documentos Soporte");
        lblTitulo.setFont(new Font("Segoe UI", Font.BOLD, 22));
        lblTitulo.setForeground(MainFrame.COLOR_TEXTO);

        JLabel lblSubtitulo = new JLabel("Emisión formal de Lotes de Producción, Devoluciones, Ajustes Físicos y Mermas (Fase 4 - RF-05..RF-09)");
        lblSubtitulo.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblSubtitulo.setForeground(MainFrame.COLOR_TEXTO_SEC);

        panelTitulos.add(lblTitulo);
        panelTitulos.add(Box.createVerticalStrut(4));
        panelTitulos.add(lblSubtitulo);
        panelHeader.add(panelTitulos, BorderLayout.WEST);

        Usuario u = usuarioCtrl.getUsuarioLogueado();
        JLabel lblUsuarioInfo = new JLabel("Operador: " + (u != null ? u.getNombreUsuario() : "Desconocido"));
        lblUsuarioInfo.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblUsuarioInfo.setForeground(MainFrame.COLOR_PRIMARIO);
        panelHeader.add(lblUsuarioInfo, BorderLayout.EAST);

        add(panelHeader, BorderLayout.NORTH);

        // 2. CONTENEDOR DE PESTAÑAS
        tabbedPane = new JTabbedPane();
        tabbedPane.setFont(new Font("Segoe UI", Font.BOLD, 13));
        tabbedPane.setBackground(MainFrame.COLOR_PANEL);
        tabbedPane.setForeground(MainFrame.COLOR_TEXTO);

        tabbedPane.addTab("🏭 Lote de Producción (RF-05)", crearPestanaLoteProduccion());
        tabbedPane.addTab("🔄 Devolución Cliente (RF-07)", crearPestanaDevolucion());
        tabbedPane.addTab("⚖️ Ajuste Físico (RF-08)", crearPestanaAjusteFisico());
        tabbedPane.addTab("⚠️ Baja por Merma (RF-09)", crearPestanaBajaMerma());
        tabbedPane.addTab("📋 Historial de Radicados", crearPestanaHistorial());

        add(tabbedPane, BorderLayout.CENTER);
    }

    // =========================================================================
    // PESTAÑA 1: ORDEN DE ENTREGA DE LOTE DE PRODUCCION (RF-05)
    // =========================================================================
    private JPanel crearPestanaLoteProduccion() {
        JPanel panel = new JPanel(new BorderLayout(12, 12));
        panel.setBackground(MainFrame.COLOR_PANEL);
        panel.setBorder(new EmptyBorder(16, 20, 16, 20));

        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(8, 8, 8, 8);
        g.fill = GridBagConstraints.HORIZONTAL;

        cmbProdLote = new JComboBox<>();
        cmbProdLote.setPreferredSize(new Dimension(380, 32));
        lblSkuLote = crearLabelInfo("SKU: -");
        lblStockLote = crearLabelInfo("Stock Bodega: -");
        lblCostoLote = crearLabelInfo("Costo Estándar: -");

        cmbProdLote.addActionListener(e -> {
            Producto p = (Producto) cmbProdLote.getSelectedItem();
            if (p != null) {
                lblSkuLote.setText("SKU: " + (p.getCodigoSku() != null ? p.getCodigoSku() : "N/A"));
                lblStockLote.setText("Stock Bodega: " + p.getStockActual() + " un.");
                lblCostoLote.setText(String.format("Costo Est.: $%,.2f", p.getCostoEstandar()));
            }
        });

        txtNroOp = crearTextField(movCtrl.generarSiguienteNumeroSoporte("ENT_PROD"));
        txtNroLote = crearTextField("LOTE-TIG-" + (System.currentTimeMillis() % 1000));
        txtCantLote = crearTextField("10");
        txtObsLote = crearTextField("Lote inspeccionado en pulido y soldadura TIG conforme.");

        // Fila 0: Producto
        g.gridx = 0; g.gridy = 0; form.add(crearLabelCampo("Asador / Producto Fabricado:"), g);
        g.gridx = 1; form.add(cmbProdLote, g);

        // Fila 1: Datos tecnicos del asador
        JPanel panelDatos = new JPanel(new FlowLayout(FlowLayout.LEFT, 16, 0));
        panelDatos.setOpaque(false);
        panelDatos.add(lblSkuLote);
        panelDatos.add(lblStockLote);
        panelDatos.add(lblCostoLote);
        g.gridx = 1; g.gridy = 1; form.add(panelDatos, g);

        // Fila 2: N.° Orden de Produccion (OP)
        g.gridx = 0; g.gridy = 2; form.add(crearLabelCampo("N.° Orden de Producción (OP):"), g);
        g.gridx = 1; form.add(txtNroOp, g);

        // Fila 3: N.° de Lote
        g.gridx = 0; g.gridy = 3; form.add(crearLabelCampo("Código / Lote de Fabricación:"), g);
        g.gridx = 1; form.add(txtNroLote, g);

        // Fila 4: Cantidad ensamblada
        g.gridx = 0; g.gridy = 4; form.add(crearLabelCampo("Cantidad Ensamblada (Unidades):"), g);
        g.gridx = 1; form.add(txtCantLote, g);

        // Fila 5: Observaciones tecnicas
        g.gridx = 0; g.gridy = 5; form.add(crearLabelCampo("Observaciones de Control de Calidad:"), g);
        g.gridx = 1; form.add(txtObsLote, g);

        // Boton radicar
        JButton btnRadicar = ProductoPanel.crearBoton("Radicar Entrega de Lote (Enviar a Bandeja)", MainFrame.COLOR_PRIMARIO);
        btnRadicar.setPreferredSize(new Dimension(380, 40));
        btnRadicar.addActionListener(e -> radicarLoteProduccion());

        JPanel panelBoton = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        panelBoton.setOpaque(false);
        panelBoton.add(btnRadicar);

        panel.add(form, BorderLayout.CENTER);
        panel.add(panelBoton, BorderLayout.SOUTH);
        return panel;
    }

    private void radicarLoteProduccion() {
        Producto p = (Producto) cmbProdLote.getSelectedItem();
        if (p == null) {
            JOptionPane.showMessageDialog(this, "Seleccione un producto.", "Validacion", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int cant;
        try {
            cant = Integer.parseInt(txtCantLote.getText().trim());
            if (cant <= 0) throw new NumberFormatException();
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Ingrese una cantidad entera valida mayor a 0.", "Error de Formato", JOptionPane.ERROR_MESSAGE);
            return;
        }

        ResultadoOperacion res = movCtrl.radicarEntregaLoteProduccion(
            txtNroOp.getText().trim(),
            txtNroLote.getText().trim(),
            p.getIdProducto(),
            cant,
            p.getCostoEstandar(),
            usuarioCtrl.getUsuarioLogueado(),
            txtObsLote.getText().trim()
        );

        mostrarResultado(res);
        if (res.isExito()) {
            txtNroOp.setText(movCtrl.generarSiguienteNumeroSoporte("ENT_PROD"));
            txtNroLote.setText("LOTE-TIG-" + (System.currentTimeMillis() % 1000));
            cargarHistorial();
        }
    }

    // =========================================================================
    // PESTAÑA 2: ACTA DE DEVOLUCION DE CLIENTE (RF-07)
    // =========================================================================
    private JPanel crearPestanaDevolucion() {
        JPanel panel = new JPanel(new BorderLayout(12, 12));
        panel.setBackground(MainFrame.COLOR_PANEL);
        panel.setBorder(new EmptyBorder(16, 20, 16, 20));

        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(8, 8, 8, 8);
        g.fill = GridBagConstraints.HORIZONTAL;

        cmbProdDev = new JComboBox<>();
        cmbProdDev.setPreferredSize(new Dimension(380, 32));
        lblSkuDev = crearLabelInfo("SKU: -");
        lblStockDev = crearLabelInfo("Stock Actual: -");

        cmbProdDev.addActionListener(e -> {
            Producto p = (Producto) cmbProdDev.getSelectedItem();
            if (p != null) {
                lblSkuDev.setText("SKU: " + (p.getCodigoSku() != null ? p.getCodigoSku() : "N/A"));
                lblStockDev.setText("Stock Bodega: " + p.getStockActual() + " un.");
            }
        });

        txtFacturaDev = crearTextField("FAC-2026-00" + (System.currentTimeMillis() % 100));
        txtCantDev = crearTextField("1");
        txtObsDev = crearTextField("Cliente reporta desajuste en remache de manija termica.");

        String[] dictamenes = {
            "Apto para Reingreso Inmediato a Stock (Defecto cosmético menor/embalaje)",
            "Requiere Reproceso en Taller Metalmecánico (Alineación/Soldadura TIG)",
            "Daño No Imputable al Fabricante / Devolución Rechazada"
        };
        cmbDictamenDev = new JComboBox<>(dictamenes);
        cmbDictamenDev.setPreferredSize(new Dimension(380, 32));

        g.gridx = 0; g.gridy = 0; form.add(crearLabelCampo("Producto / Asador Devuelto:"), g);
        g.gridx = 1; form.add(cmbProdDev, g);

        JPanel panelDatos = new JPanel(new FlowLayout(FlowLayout.LEFT, 16, 0));
        panelDatos.setOpaque(false);
        panelDatos.add(lblSkuDev);
        panelDatos.add(lblStockDev);
        g.gridx = 1; g.gridy = 1; form.add(panelDatos, g);

        g.gridx = 0; g.gridy = 2; form.add(crearLabelCampo("N.° Factura o Remisión Origen:"), g);
        g.gridx = 1; form.add(txtFacturaDev, g);

        g.gridx = 0; g.gridy = 3; form.add(crearLabelCampo("Cantidad Devuelta (Unidades):"), g);
        g.gridx = 1; form.add(txtCantDev, g);

        g.gridx = 0; g.gridy = 4; form.add(crearLabelCampo("Dictamen Técnico Preliminar:"), g);
        g.gridx = 1; form.add(cmbDictamenDev, g);

        g.gridx = 0; g.gridy = 5; form.add(crearLabelCampo("Motivo / Diagnóstico de Calidad:"), g);
        g.gridx = 1; form.add(txtObsDev, g);

        JButton btnRadicar = ProductoPanel.crearBoton("Radicar Devolución de Cliente (Enviar a Bandeja)", MainFrame.COLOR_PRIMARIO);
        btnRadicar.setPreferredSize(new Dimension(380, 40));
        btnRadicar.addActionListener(e -> radicarDevolucion());

        JPanel panelBoton = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        panelBoton.setOpaque(false);
        panelBoton.add(btnRadicar);

        panel.add(form, BorderLayout.CENTER);
        panel.add(panelBoton, BorderLayout.SOUTH);
        return panel;
    }

    private void radicarDevolucion() {
        Producto p = (Producto) cmbProdDev.getSelectedItem();
        if (p == null) {
            JOptionPane.showMessageDialog(this, "Seleccione un producto devuelto.", "Validacion", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int cant;
        try {
            cant = Integer.parseInt(txtCantDev.getText().trim());
            if (cant <= 0) throw new NumberFormatException();
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Ingrese una cantidad valida mayor a 0.", "Error de Formato", JOptionPane.ERROR_MESSAGE);
            return;
        }

        ResultadoOperacion res = movCtrl.radicarDevolucionCliente(
            txtFacturaDev.getText().trim(),
            (String) cmbDictamenDev.getSelectedItem(),
            p.getIdProducto(),
            cant,
            p.getCostoEstandar(),
            usuarioCtrl.getUsuarioLogueado(),
            txtObsDev.getText().trim()
        );

        mostrarResultado(res);
        if (res.isExito()) {
            txtFacturaDev.setText("FAC-2026-00" + (System.currentTimeMillis() % 100));
            cargarHistorial();
        }
    }

    // =========================================================================
    // PESTAÑA 3: ACTA DE AJUSTE POR CONTEO FISICO (RF-08)
    // =========================================================================
    private JPanel crearPestanaAjusteFisico() {
        JPanel panel = new JPanel(new BorderLayout(12, 12));
        panel.setBackground(MainFrame.COLOR_PANEL);
        panel.setBorder(new EmptyBorder(16, 20, 16, 20));

        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(8, 8, 8, 8);
        g.fill = GridBagConstraints.HORIZONTAL;

        cmbProdAju = new JComboBox<>();
        cmbProdAju.setPreferredSize(new Dimension(380, 32));
        lblSkuAju = crearLabelInfo("SKU: -");
        lblStockSistemaAju = crearLabelInfo("Stock Teórico Sistema: 0 un.");
        lblDiscrepanciaAju = new JLabel("Discrepancia: 0 un. (Conciliado)");
        lblDiscrepanciaAju.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblDiscrepanciaAju.setForeground(MainFrame.COLOR_EXITO);

        txtConteoFisicoAju = crearTextField("0");
        txtObsAju = crearTextField("Conteo ciclico mensual en seccion A de bodega.");

        String[] causales = {
            "Diferencia detectada en conteo cíclico mensual de bodega",
            "Error de digitación o registro en despacho comercial anterior",
            "Rotura / daño accidental en estiba no registrado oportunamente",
            "Muestra comercial exhibida sin remisión formal",
            "Otro motivo técnico (detallar en justificación)"
        };
        cmbCausalAju = new JComboBox<>(causales);
        cmbCausalAju.setPreferredSize(new Dimension(380, 32));

        Runnable recalcularDiscrepancia = () -> {
            Producto p = (Producto) cmbProdAju.getSelectedItem();
            if (p != null) {
                lblSkuAju.setText("SKU: " + (p.getCodigoSku() != null ? p.getCodigoSku() : "N/A"));
                lblStockSistemaAju.setText("Stock Teórico Sistema: " + p.getStockActual() + " un.");

                try {
                    int conteo = Integer.parseInt(txtConteoFisicoAju.getText().trim());
                    int diff = conteo - p.getStockActual();
                    if (diff > 0) {
                        lblDiscrepanciaAju.setText("Discrepancia: SOBRANTE (+ " + diff + " un.)");
                        lblDiscrepanciaAju.setForeground(MainFrame.COLOR_EXITO);
                    } else if (diff < 0) {
                        lblDiscrepanciaAju.setText("Discrepancia: FALTANTE (" + diff + " un.)");
                        lblDiscrepanciaAju.setForeground(MainFrame.COLOR_ERROR);
                    } else {
                        lblDiscrepanciaAju.setText("Discrepancia: 0 un. (Exacto)");
                        lblDiscrepanciaAju.setForeground(MainFrame.COLOR_TEXTO_SEC);
                    }
                } catch (NumberFormatException ex) {
                    lblDiscrepanciaAju.setText("Conteo inválido");
                    lblDiscrepanciaAju.setForeground(MainFrame.COLOR_ALERTA);
                }
            }
        };

        cmbProdAju.addActionListener(e -> {
            Producto p = (Producto) cmbProdAju.getSelectedItem();
            if (p != null) {
                txtConteoFisicoAju.setText(String.valueOf(p.getStockActual()));
            }
            recalcularDiscrepancia.run();
        });

        txtConteoFisicoAju.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { recalcularDiscrepancia.run(); }
            public void removeUpdate(DocumentEvent e) { recalcularDiscrepancia.run(); }
            public void changedUpdate(DocumentEvent e) { recalcularDiscrepancia.run(); }
        });

        g.gridx = 0; g.gridy = 0; form.add(crearLabelCampo("Producto / Asador a Conciliar:"), g);
        g.gridx = 1; form.add(cmbProdAju, g);

        JPanel panelDatos = new JPanel(new FlowLayout(FlowLayout.LEFT, 16, 0));
        panelDatos.setOpaque(false);
        panelDatos.add(lblSkuAju);
        panelDatos.add(lblStockSistemaAju);
        panelDatos.add(lblDiscrepanciaAju);
        g.gridx = 1; g.gridy = 1; form.add(panelDatos, g);

        g.gridx = 0; g.gridy = 2; form.add(crearLabelCampo("Conteo Físico Real en Bodega:"), g);
        g.gridx = 1; form.add(txtConteoFisicoAju, g);

        g.gridx = 0; g.gridy = 3; form.add(crearLabelCampo("Causal Técnica Justificada:"), g);
        g.gridx = 1; form.add(cmbCausalAju, g);

        g.gridx = 0; g.gridy = 4; form.add(crearLabelCampo("Justificación / Detalle Técnico:"), g);
        g.gridx = 1; form.add(txtObsAju, g);

        JButton btnRadicar = ProductoPanel.crearBoton("Radicar Acta de Ajuste Físico (Autorización Gerencial)", MainFrame.COLOR_PRIMARIO);
        btnRadicar.setPreferredSize(new Dimension(380, 40));
        btnRadicar.addActionListener(e -> radicarAjusteFisico());

        JPanel panelBoton = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        panelBoton.setOpaque(false);
        panelBoton.add(btnRadicar);

        panel.add(form, BorderLayout.CENTER);
        panel.add(panelBoton, BorderLayout.SOUTH);
        return panel;
    }

    private void radicarAjusteFisico() {
        Producto p = (Producto) cmbProdAju.getSelectedItem();
        if (p == null) {
            JOptionPane.showMessageDialog(this, "Seleccione un producto para ajustar.", "Validacion", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int conteo;
        try {
            conteo = Integer.parseInt(txtConteoFisicoAju.getText().trim());
            if (conteo < 0) throw new NumberFormatException();
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "El conteo fisico real debe ser un entero no negativo.", "Error de Formato", JOptionPane.ERROR_MESSAGE);
            return;
        }

        ResultadoOperacion res = movCtrl.radicarAjusteFisico(
            p.getIdProducto(),
            conteo,
            p.getStockActual(),
            p.getCostoEstandar(),
            (String) cmbCausalAju.getSelectedItem(),
            usuarioCtrl.getUsuarioLogueado(),
            txtObsAju.getText().trim()
        );

        mostrarResultado(res);
        if (res.isExito()) {
            cargarHistorial();
        }
    }

    // =========================================================================
    // PESTAÑA 4: ACTA DE BAJA POR MERMA TECNICA (RF-09)
    // =========================================================================
    private JPanel crearPestanaBajaMerma() {
        JPanel panel = new JPanel(new BorderLayout(12, 12));
        panel.setBackground(MainFrame.COLOR_PANEL);
        panel.setBorder(new EmptyBorder(16, 20, 16, 20));

        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(8, 8, 8, 8);
        g.fill = GridBagConstraints.HORIZONTAL;

        cmbProdBaj = new JComboBox<>();
        cmbProdBaj.setPreferredSize(new Dimension(380, 32));
        lblSkuBaj = crearLabelInfo("SKU: -");
        lblStockBaj = crearLabelInfo("Stock Bodega: 0 un.");

        cmbProdBaj.addActionListener(e -> {
            Producto p = (Producto) cmbProdBaj.getSelectedItem();
            if (p != null) {
                lblSkuBaj.setText("SKU: " + (p.getCodigoSku() != null ? p.getCodigoSku() : "N/A"));
                lblStockBaj.setText("Stock Bodega: " + p.getStockActual() + " un.");
            }
        });

        txtCantBaj = crearTextField("1");
        txtObsBaj = crearTextField("Fisura en soldadura TIG detectada durante prueba de presion de aire.");

        String[] motivosMerma = {
            "Fallo estructural irreparable en cordón de soldadura TIG",
            "Abolladura severa o deformación durante flete / transporte",
            "Fisura o daño metalúrgico en lámina de acero AISI",
            "Pieza destruida en ensayo de control de calidad destructivo",
            "Otra avería irreparable justificada"
        };
        cmbMotivoBaj = new JComboBox<>(motivosMerma);
        cmbMotivoBaj.setPreferredSize(new Dimension(380, 32));

        g.gridx = 0; g.gridy = 0; form.add(crearLabelCampo("Producto / Asador a Dar de Baja:"), g);
        g.gridx = 1; form.add(cmbProdBaj, g);

        JPanel panelDatos = new JPanel(new FlowLayout(FlowLayout.LEFT, 16, 0));
        panelDatos.setOpaque(false);
        panelDatos.add(lblSkuBaj);
        panelDatos.add(lblStockBaj);
        g.gridx = 1; g.gridy = 1; form.add(panelDatos, g);

        g.gridx = 0; g.gridy = 2; form.add(crearLabelCampo("Cantidad a Dar de Baja (Unidades):"), g);
        g.gridx = 1; form.add(txtCantBaj, g);

        g.gridx = 0; g.gridy = 3; form.add(crearLabelCampo("Causal Técnica de Merma:"), g);
        g.gridx = 1; form.add(cmbMotivoBaj, g);

        g.gridx = 0; g.gridy = 4; form.add(crearLabelCampo("Justificación / Dictamen Irreparable:"), g);
        g.gridx = 1; form.add(txtObsBaj, g);

        JButton btnRadicar = ProductoPanel.crearBoton("Radicar Acta de Baja (Autorización Gerencial)", MainFrame.COLOR_ERROR);
        btnRadicar.setPreferredSize(new Dimension(380, 40));
        btnRadicar.addActionListener(e -> radicarBajaMerma());

        JPanel panelBoton = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        panelBoton.setOpaque(false);
        panelBoton.add(btnRadicar);

        panel.add(form, BorderLayout.CENTER);
        panel.add(panelBoton, BorderLayout.SOUTH);
        return panel;
    }

    private void radicarBajaMerma() {
        Producto p = (Producto) cmbProdBaj.getSelectedItem();
        if (p == null) {
            JOptionPane.showMessageDialog(this, "Seleccione un producto.", "Validacion", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int cant;
        try {
            cant = Integer.parseInt(txtCantBaj.getText().trim());
            if (cant <= 0) throw new NumberFormatException();
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "La cantidad a dar de baja debe ser mayor a 0.", "Error de Formato", JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (cant > p.getStockActual()) {
            JOptionPane.showMessageDialog(this,
                "No puedes dar de baja " + cant + " unidades.\nEl stock actual en bodega es de solo " + p.getStockActual() + " un.",
                "Stock Insuficiente", JOptionPane.WARNING_MESSAGE);
            return;
        }

        ResultadoOperacion res = movCtrl.radicarBajaMermaTecnica(
            p.getIdProducto(),
            cant,
            p.getCostoEstandar(),
            (String) cmbMotivoBaj.getSelectedItem(),
            usuarioCtrl.getUsuarioLogueado(),
            txtObsBaj.getText().trim()
        );

        mostrarResultado(res);
        if (res.isExito()) {
            cargarHistorial();
        }
    }

    // =========================================================================
    // PESTAÑA 5: HISTORIAL DE DOCUMENTOS RADICADOS
    // =========================================================================
    private JPanel crearPestanaHistorial() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setBackground(MainFrame.COLOR_PANEL);
        panel.setBorder(new EmptyBorder(12, 12, 12, 12));

        // Barra superior de herramientas del historial
        JPanel barra = new JPanel(new BorderLayout());
        barra.setOpaque(false);

        lblTotalHistorial = new JLabel("Cargando historial...");
        lblTotalHistorial.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblTotalHistorial.setForeground(MainFrame.COLOR_TEXTO_SEC);

        JButton btnRefrescar = ProductoPanel.crearBoton("Refrescar Historial", MainFrame.COLOR_CAMPO);
        btnRefrescar.setForeground(MainFrame.COLOR_TEXTO);
        btnRefrescar.setBorder(new LineBorder(MainFrame.COLOR_BORDE, 1));
        btnRefrescar.addActionListener(e -> cargarHistorial());

        barra.add(lblTotalHistorial, BorderLayout.WEST);
        barra.add(btnRefrescar, BorderLayout.EAST);
        panel.add(barra, BorderLayout.NORTH);

        // Tabla de historial
        String[] columnas = {"ID", "N.° Documento", "Tipo de Soporte", "Naturaleza", "Fecha / Hora", "Radicado Por", "Estado", "Observaciones"};
        modeloHistorial = new DefaultTableModel(columnas, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };

        tablaHistorial = new JTable(modeloHistorial);
        tablaHistorial.setRowHeight(26);
        tablaHistorial.getTableHeader().setReorderingAllowed(false);

        // Renderer de estado
        tablaHistorial.getColumnModel().getColumn(6).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object val, boolean sel, boolean foc, int row, int col) {
                JLabel c = (JLabel) super.getTableCellRendererComponent(table, val, sel, foc, row, col);
                String estado = (val != null) ? val.toString().toUpperCase() : "";
                c.setHorizontalAlignment(SwingConstants.CENTER);
                c.setFont(new Font("Segoe UI", Font.BOLD, 11));
                if (estado.contains("PENDIENTE")) {
                    c.setForeground(MainFrame.COLOR_ALERTA);
                } else if (estado.contains("AUTORIZADO") || estado.contains("APROBADO")) {
                    c.setForeground(MainFrame.COLOR_EXITO);
                } else if (estado.contains("RECHAZADO")) {
                    c.setForeground(MainFrame.COLOR_ERROR);
                } else {
                    c.setForeground(MainFrame.COLOR_TEXTO);
                }
                return c;
            }
        });

        panel.add(new JScrollPane(tablaHistorial), BorderLayout.CENTER);
        return panel;
    }

    private void cargarHistorial() {
        modeloHistorial.setRowCount(0);
        List<MovimientoInventario> lista = movCtrl.listarTodosMovimientos();
        for (MovimientoInventario m : lista) {
            modeloHistorial.addRow(new Object[]{
                m.getIdMovimiento(),
                m.getNumeroDocumentoSoporte(),
                m.getNombreTipoMovimiento(),
                m.getNaturalezaAfectacion(),
                m.getFechaHora(),
                m.getNombreUsuarioCreador(),
                m.getEstadoAutorizacion(),
                m.getObservaciones()
            });
        }
        lblTotalHistorial.setText("Total documentos registrados: " + lista.size());
    }

    private void cargarProductosEnCombos() {
        List<Producto> productos = prodCtrl.listarTodos();
        cmbProdLote.removeAllItems();
        cmbProdDev.removeAllItems();
        cmbProdAju.removeAllItems();
        cmbProdBaj.removeAllItems();

        for (Producto p : productos) {
            cmbProdLote.addItem(p);
            cmbProdDev.addItem(p);
            cmbProdAju.addItem(p);
            cmbProdBaj.addItem(p);
        }
    }

    private void mostrarResultado(ResultadoOperacion res) {
        if (res.isExito()) {
            JOptionPane.showMessageDialog(this, res.getMensaje(), "Operación Exitosa", JOptionPane.INFORMATION_MESSAGE);
        } else {
            JOptionPane.showMessageDialog(this, res.getMensaje(), "Error de Radicación", JOptionPane.ERROR_MESSAGE);
        }
    }

    private JLabel crearLabelCampo(String texto) {
        JLabel l = new JLabel(texto);
        l.setFont(new Font("Segoe UI", Font.BOLD, 12));
        l.setForeground(MainFrame.COLOR_TEXTO);
        return l;
    }

    private JLabel crearLabelInfo(String texto) {
        JLabel l = new JLabel(texto);
        l.setFont(new Font("Segoe UI", Font.ITALIC, 11));
        l.setForeground(MainFrame.COLOR_TEXTO_SEC);
        return l;
    }

    private JTextField crearTextField(String valorDefecto) {
        JTextField tf = new JTextField(valorDefecto);
        tf.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        tf.setBackground(MainFrame.COLOR_CAMPO);
        tf.setForeground(MainFrame.COLOR_TEXTO);
        tf.setCaretColor(MainFrame.COLOR_TEXTO);
        tf.setBorder(new LineBorder(MainFrame.COLOR_BORDE, 1));
        tf.setPreferredSize(new Dimension(380, 30));
        return tf;
    }
}
