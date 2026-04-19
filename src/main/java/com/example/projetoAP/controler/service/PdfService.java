package com.example.projetoAP.controler.service;

import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Service
public class PdfService {

    private static final Font TITULO = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16, new Color(34, 85, 170));
    private static final Font SUBTITULO = FontFactory.getFont(FontFactory.HELVETICA, 10, Color.DARK_GRAY);
    private static final Font CABECALHO = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, Color.WHITE);
    private static final Font CELULA = FontFactory.getFont(FontFactory.HELVETICA, 8, Color.BLACK);
    private static final Color HEADER_BG = new Color(34, 85, 170);

    public byte[] gerarTabela(String titulo, String subtitulo, String[] colunas,
                              List<List<String>> linhas) {
        try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
            Document doc = new Document(PageSize.A4.rotate(), 30, 30, 40, 30);
            PdfWriter.getInstance(doc, os);
            doc.open();

            Paragraph t = new Paragraph(titulo, TITULO);
            t.setAlignment(Element.ALIGN_CENTER);
            doc.add(t);

            String rodape = (subtitulo != null ? subtitulo + " - " : "")
                    + "Gerado em " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
            Paragraph s = new Paragraph(rodape, SUBTITULO);
            s.setAlignment(Element.ALIGN_CENTER);
            s.setSpacingAfter(12f);
            doc.add(s);

            PdfPTable tabela = new PdfPTable(colunas.length);
            tabela.setWidthPercentage(100);
            for (String c : colunas) {
                PdfPCell cell = new PdfPCell(new Phrase(c, CABECALHO));
                cell.setBackgroundColor(HEADER_BG);
                cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                cell.setPadding(5f);
                tabela.addCell(cell);
            }
            boolean zebra = false;
            for (List<String> linha : linhas) {
                Color bg = zebra ? new Color(245, 247, 250) : Color.WHITE;
                zebra = !zebra;
                for (String v : linha) {
                    PdfPCell cell = new PdfPCell(new Phrase(v == null ? "" : v, CELULA));
                    cell.setBackgroundColor(bg);
                    cell.setPadding(4f);
                    tabela.addCell(cell);
                }
            }
            doc.add(tabela);
            doc.close();
            return os.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Falha ao gerar PDF: " + e.getMessage(), e);
        }
    }

    public byte[] gerarRelatorioEquipamentos(List<Map<String, Object>> equipamentos) {
        String[] cols = { "Patrimonio", "Hostname", "Setor", "Filial", "Tipo",
                "Marca/Modelo", "Status", "Uso", "Manutencoes" };
        List<List<String>> linhas = equipamentos.stream().map(e -> List.of(
                str(e.get("patrimonio")),
                str(e.get("hostname")),
                str(e.get("setor")),
                str(e.get("filial")),
                str(e.get("tipo")),
                str(e.get("marca")) + " " + str(e.get("modelo")),
                str(e.get("status")),
                str(e.get("statusUso")),
                str(e.get("numManutencoes"))
        )).toList();
        return gerarTabela("Relatorio de Equipamentos", "Total: " + equipamentos.size(), cols, linhas);
    }

    public byte[] gerarRelatorioManutencoes(List<Map<String, Object>> manutencoes) {
        String[] cols = { "ID", "Patrimonio", "Hostname", "Setor", "Tipo",
                "Status", "Tecnico", "Entrada", "Entrega", "Duracao (h)" };
        List<List<String>> linhas = manutencoes.stream().map(m -> List.of(
                str(m.get("id")),
                str(m.get("patrimonio")),
                str(m.get("hostname")),
                str(m.get("setor")),
                str(m.get("tipoManutencao")),
                str(m.get("status")),
                str(m.get("tecnicoNome")),
                str(m.get("dataEntrada")),
                str(m.get("dataEntrega")),
                str(m.get("duracaoHoras"))
        )).toList();
        return gerarTabela("Relatorio de Manutencoes", "Total: " + manutencoes.size(), cols, linhas);
    }

    public byte[] gerarRelatorioRanking(List<Map<String, Object>> ranking, String periodo) {
        String[] cols = { "Posicao", "Tecnico", "Username", "Cargo", "Total manutencoes" };
        int[] pos = { 1 };
        List<List<String>> linhas = ranking.stream().map(r -> List.of(
                String.valueOf(pos[0]++),
                str(r.get("nome")),
                str(r.get("username")),
                str(r.get("cargo")),
                str(r.get("total"))
        )).toList();
        return gerarTabela("Ranking de Manutencoes", "Periodo: " + periodo, cols, linhas);
    }

    public byte[] gerarRelatorioMetricas(List<Map<String, Object>> usuarios) {
        String[] cols = { "Nome", "Username", "Cargo", "Perfil",
                "Total", "Concluidos", "Tempo medio (h)" };
        List<List<String>> linhas = usuarios.stream().map(u -> List.of(
                str(u.get("nome")),
                str(u.get("username")),
                str(u.get("cargo")),
                str(u.get("perfil")),
                str(u.get("total")),
                str(u.get("concluidos")),
                str(u.get("tempoMedioHoras"))
        )).toList();
        return gerarTabela("Metricas por Usuario", "Total usuarios: " + usuarios.size(), cols, linhas);
    }

    private String str(Object o) { return o == null ? "" : String.valueOf(o); }
}
