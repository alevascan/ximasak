package com.ximasak.backend.service;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.ximasak.backend.entity.Cliente;
import com.ximasak.backend.entity.DetalleVenta;
import com.ximasak.backend.entity.Venta;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;

@Service
public class ComprobanteService {

    private static final Font TITULO = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16);
    private static final Font NEGRITA = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10);
    private static final Font NORMAL = FontFactory.getFont(FontFactory.HELVETICA, 10);
    private static final Font NOTA = FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 8);
    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    /** tipo: "BOLETA" o "FACTURA". Lanza RuntimeException si la factura no es válida. */
    public byte[] generarPdf(Venta venta, String tipo) {
        boolean esFactura = "FACTURA".equalsIgnoreCase(tipo);
        Cliente cliente = venta.getCliente();

        if (esFactura && (cliente == null || cliente.getRuc() == null || cliente.getRuc().isBlank())) {
            throw new RuntimeException("El cliente debe tener un RUC registrado para emitir factura");
        }

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document doc = new Document(PageSize.A4, 40, 40, 40, 40);
        try {
            PdfWriter.getInstance(doc, out);
            doc.open();

            String serie = esFactura ? "F001" : "B001";
            String numero = String.format("%s-%06d", serie, venta.getIdVenta());

            Paragraph titulo = new Paragraph("XIMASAK EIRL", TITULO);
            titulo.setAlignment(Element.ALIGN_CENTER);
            doc.add(titulo);

            Paragraph sub = new Paragraph((esFactura ? "FACTURA" : "BOLETA") + " DE VENTA  " + numero, NEGRITA);
            sub.setAlignment(Element.ALIGN_CENTER);
            sub.setSpacingAfter(12);
            doc.add(sub);

            doc.add(new Paragraph("Fecha: " + (venta.getFecha() != null ? venta.getFecha().format(FECHA) : "-"), NORMAL));
            doc.add(new Paragraph("Cliente: " + (cliente != null ? cliente.getNombre() : "-"), NORMAL));
            if (esFactura) {
                doc.add(new Paragraph("RUC: " + cliente.getRuc(), NORMAL));
            } else if (cliente != null && cliente.getDni() != null) {
                doc.add(new Paragraph("DNI: " + cliente.getDni(), NORMAL));
            }
            if (venta.getMedioPago() != null) {
                doc.add(new Paragraph("Medio de pago: " + venta.getMedioPago().getNombre(), NORMAL));
            }
            if (venta.getUsuario() != null) {
                doc.add(new Paragraph("Atendido por: " + venta.getUsuario().getNombre(), NORMAL));
            }

            PdfPTable tabla = new PdfPTable(new float[]{5f, 1.5f, 2f, 2f});
            tabla.setWidthPercentage(100);
            tabla.setSpacingBefore(14);
            tabla.setSpacingAfter(10);
            for (String h : new String[]{"Producto", "Cant.", "P. Unit.", "Subtotal"}) {
                PdfPCell c = new PdfPCell(new Phrase(h, NEGRITA));
                c.setHorizontalAlignment(Element.ALIGN_CENTER);
                tabla.addCell(c);
            }
            for (DetalleVenta d : venta.getDetalles()) {
                tabla.addCell(new PdfPCell(new Phrase(d.getProducto().getNombre(), NORMAL)));
                tabla.addCell(derecha(String.valueOf(d.getCantidad())));
                tabla.addCell(derecha(money(d.getPrecioUnitario())));
                tabla.addCell(derecha(money(d.getSubtotal())));
            }
            doc.add(tabla);

            doc.add(totalLinea("Subtotal (sin IGV):", venta.getSubtotal(), NORMAL));
            doc.add(totalLinea("IGV (18%):", venta.getIgv(), NORMAL));
            doc.add(totalLinea("TOTAL:", venta.getTotal(), NEGRITA));

            Paragraph nota = new Paragraph(
                    "Documento interno generado por el sistema SIRVS. No constituye un comprobante "
                  + "electrónico validado por SUNAT.", NOTA);
            nota.setSpacingBefore(24);
            doc.add(nota);

            doc.close();
        } catch (DocumentException e) {
            throw new RuntimeException("No se pudo generar el PDF: " + e.getMessage(), e);
        }
        return out.toByteArray();
    }

    private PdfPCell derecha(String texto) {
        PdfPCell c = new PdfPCell(new Phrase(texto, NORMAL));
        c.setHorizontalAlignment(Element.ALIGN_RIGHT);
        return c;
    }

    private Paragraph totalLinea(String etiqueta, BigDecimal monto, Font f) {
        Paragraph p = new Paragraph(etiqueta + "  S/ " + money(monto), f);
        p.setAlignment(Element.ALIGN_RIGHT);
        return p;
    }

    private String money(BigDecimal v) {
        return v == null ? "0.00" : v.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString();
    }
}
