package com.example.projetoAP.controler;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.projetoAP.EndpointDTO;
import com.example.projetoAP.Service.EndpointInterfaceDTO;
import com.example.projetoAP.controler.SetoresService.SetorService;
import com.example.projetoAP.controler.service.AuthService;
import com.example.projetoAP.controler.service.CalcularPerdas;
import com.example.projetoAP.controler.service.EndpointService;
import com.example.projetoAP.controler.service.GroupService;
import com.example.projetoAP.domain.model.UpdateDTO;
import com.example.projetoAP.domain.vo.Filter;

import com.example.projetoAP.controler.service.AuditService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/endpoint")
@CrossOrigin(origins = "*")
public class ApControler {

    @Autowired
    CalcularPerdas calcularPerdas;
    @Autowired
    private SimpMessagingTemplate messagingTemplate;
    @Autowired
    GroupService groupService;
    @Autowired
    AuthService authService;
    @Autowired
    AuditService auditService;
    EndpointService endpointService;
    SetorService setorService;
    ApControler(EndpointService endpointService, SetorService setor){
        this.endpointService = endpointService;
        this.setorService = setor;
    }


    @GetMapping("/monitoramento")
    public String getVier(){
        return "index";
    }

    @PostMapping("/monitoramento")
    public String getVierPost(String id,Model model){
        model.addAttribute("v",id);
        return "index";
    }
    @GetMapping("/pagina")
    public String getVierPage(){
        return "pagina";
    }
    @PostMapping("/pagina")
    public String getVierListPost( String mac, Model model){
           model.addAttribute("nome",mac);
        return "pagina";
    }
    @GetMapping("/lista")
    public String getVierListPage(Model model, @RequestParam(name = "mac", required = false) String mac, @RequestParam(name = "ReceiveGrupoId",required = false) String ReceiveGrupoId, HttpSession sessionAttribute){
        // Normaliza: string vazia equivale a nulo (evita salvar filtro vazio na sessao)
        if (mac != null && mac.trim().isEmpty()) mac = null;
        if (ReceiveGrupoId != null && ReceiveGrupoId.trim().isEmpty()) ReceiveGrupoId = null;

        if (ReceiveGrupoId != null) {
            sessionAttribute.setAttribute("search", ReceiveGrupoId);
        }
        List<EndpointDTO> l = new ArrayList<>();
        var re = sessionAttribute.getAttribute("search");
        if (mac != null) {
            sessionAttribute.removeAttribute("search");
            final String termo = mac.trim();
            var lista = endpointService.getLista().stream()
                    .filter(e -> e.mac().contains(termo)
                            || e.ip().contains(termo)
                            || e.localizacao().contains(termo)
                    ).toList();
            l.addAll(lista);
        } else if (re != null && !re.toString().isEmpty()) {
            var lista = endpointService.getLista().stream()
                    .filter(e -> e.grupo().equals(re.toString())).toList();
            l.addAll(lista);
        } else {
            // Sem filtro: garante que a sessao fique limpa e mostra tudo
            sessionAttribute.removeAttribute("search");
            model.addAttribute("off", endpointService.getLista().stream().filter(e -> e.status().equals("DOWN")).toList());
            l.addAll(endpointService.getLista());
        }
        var lista = groupService.findAll();
        model.addAttribute("groups", lista);
        model.addAttribute("lista", l);
        return "lista";
    }
    @GetMapping("/antenas")
    public String getVierPagePost(Model model, String mac){
        model.addAttribute("nome",mac);
        return "antenas";
    }
    @GetMapping("/registro")
    public String setRegistro(Model model,Long id){
        if(id!=null){
           var hpst = endpointService.findOneById(id);
            model.addAttribute("form",hpst);
        }
        var lista = groupService.findAll();
        var listaSetor = setorService.getLista();
          model.addAttribute("groups",lista);
        model.addAttribute("setor",listaSetor);
        return "registro";
    }
    @PostMapping("/registro")
    @Transactional
    public String setRegistros(RedirectAttributes redirectAttributes,Model model, EndpointInterfaceDTO e) throws IOException, InterruptedException {
        System.out.println("chegou aqui");
       try {
          endpointService.setRegistro(e);
          redirectAttributes.addFlashAttribute("mensagem", "Registrado");
           return "redirect:/endpoint/registro";

       }catch (RuntimeException ex){
           redirectAttributes.addFlashAttribute("form",e);
           redirectAttributes.addFlashAttribute("erro", "Ocorreu um erro: " + ex.getMessage());
           return "redirect:/endpoint/registro";
       }

    }

    @GetMapping("erro")
    public String paginaNaoEncontrada(){
        return "404";
    }

    @PostMapping("/mult")
    public String mult(@RequestParam(value = "file",required = false) MultipartFile file,Model red, String ReceiveGrupoId,String nome) throws IOException, InterruptedException {
        String End = "/home/grupomateus/Área de trabalho/monitoramento/arquivos/";
        String caminho = End+file.getOriginalFilename();
        file.transferTo(new File(caminho));
        endpointService.salvaMultPartAsync(caminho,ReceiveGrupoId,nome);
        red.addAttribute("mensagem","Processando dados...");
        return "redirect:/registro";
    };
    @GetMapping("/web")
    public String websocket(){
        return "listaweb";
    }
    @GetMapping("/usuarios")
    public String usuarios(){
        return "usuarios";
    }
    @GetMapping("/historico")
    public String historico(){
        return "historico";
    }
    @GetMapping("/monitoramento/char")
    public String monitor(){
        return "charlista";
    }
    @GetMapping("/char")
    public String Char(@RequestParam(required = false, defaultValue = "0") String limit,
                       @RequestParam(required = false, defaultValue = "") String hostname,
                       @RequestParam(required = false, defaultValue = "0") String hostid,
                       @RequestParam(required = false, defaultValue = "1") String itemid, Model model) throws ExecutionException, InterruptedException {
        long limitValue = parseLongOrDefault(limit, 0L);
        long hostIdValue = parseLongOrDefault(hostid, 0L);
        long itemIdValue = parseLongOrDefault(itemid, 1L);

        if (hostIdValue <= 0) {
            model.addAttribute("data", Map.of("result", List.of()));
            return "char";
        }

        var lista = calcularPerdas.listaMetricas(new Filter(limitValue, itemIdValue, hostIdValue));
        model.addAttribute("data", lista.get());
        return "char";
    }

    private long parseLongOrDefault(String value, long defaultValue) {
        try {
            return Long.parseLong(value);
        } catch (Exception e) {
            return defaultValue;
        }
    }
    @PutMapping("/registro")
    public String atualizaDados(UpdateDTO e,Model red){
        endpointService.atualizaEndpoint(e);
        return "redirect:/endpoint/lista";
    }
    @DeleteMapping("/delete")
    @Transactional
    @ResponseBody
    public org.springframework.http.ResponseEntity<?> deletar(@RequestParam Long id,
                                                               @RequestHeader(value = "Authorization", required = false) String token,
                                                               HttpServletRequest request) {
        if (!authService.temPermissao(token, "acessoDelete")) {
            return org.springframework.http.ResponseEntity.status(403).body(Map.of("message", "Sem permissao para deletar. Faca login novamente."));
        }
        String username = authService.getUsername(token);
        endpointService.delete(id);
        auditService.registrar(username, "DELETAR_ENDPOINT", "/endpoint/delete",
                "Deletou endpoint ID: " + id, getClientIp(request), request.getHeader("User-Agent"));
        return org.springframework.http.ResponseEntity.ok(Map.of("message", "Endpoint excluido com sucesso"));
    }

    @GetMapping("/export")
    @ResponseBody
    public ResponseEntity<ByteArrayResource> exportarExcel(
            @RequestParam(name = "mac", required = false) String mac,
            @RequestParam(name = "ReceiveGrupoId", required = false) String ReceiveGrupoId) throws IOException {

        if (mac != null && mac.trim().isEmpty()) mac = null;
        if (ReceiveGrupoId != null && ReceiveGrupoId.trim().isEmpty()) ReceiveGrupoId = null;

        List<EndpointDTO> lista = endpointService.getLista();
        if (mac != null) {
            final String termo = mac.trim();
            lista = lista.stream().filter(e -> e.mac().contains(termo)
                    || e.ip().contains(termo)
                    || e.localizacao().contains(termo)).toList();
        } else if (ReceiveGrupoId != null) {
            final String grupo = ReceiveGrupoId;
            lista = lista.stream().filter(e -> e.grupo() != null && e.grupo().equals(grupo)).toList();
        }

        try (Workbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = wb.createSheet("Endpoints");

            CellStyle header = wb.createCellStyle();
            Font fontHdr = wb.createFont();
            fontHdr.setBold(true);
            fontHdr.setColor(IndexedColors.WHITE.getIndex());
            header.setFont(fontHdr);
            header.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
            header.setFillPattern(org.apache.poi.ss.usermodel.FillPatternType.SOLID_FOREGROUND);

            String[] cols = {"Localizacao", "MAC", "Hostname", "Status", "IPv4", "Modelo", "SN", "Grupo", "Filial", "Complexo", "Data", "Ultima Vez Online"};
            Row h = sheet.createRow(0);
            for (int i = 0; i < cols.length; i++) {
                Cell c = h.createCell(i);
                c.setCellValue(cols[i]);
                c.setCellStyle(header);
            }

            int r = 1;
            for (EndpointDTO e : lista) {
                Row row = sheet.createRow(r++);
                row.createCell(0).setCellValue(safe(e.localizacao()));
                row.createCell(1).setCellValue(safe(e.mac()));
                row.createCell(2).setCellValue(safe(e.nome()));
                row.createCell(3).setCellValue(safe(e.status()));
                row.createCell(4).setCellValue(safe(e.ip()));
                row.createCell(5).setCellValue(safe(e.modelo()));
                row.createCell(6).setCellValue(safe(e.sn()));
                row.createCell(7).setCellValue(safe(e.grupo()));
                row.createCell(8).setCellValue(e.filial() != null ? String.valueOf(e.filial()) : "");
                row.createCell(9).setCellValue(safe(e.nomeComplexo()));
                row.createCell(10).setCellValue(safe(e.data()));
                row.createCell(11).setCellValue(safe(e.dataUltimaVezOnline()));
            }

            for (int i = 0; i < cols.length; i++) sheet.autoSizeColumn(i);

            wb.write(out);
            byte[] bytes = out.toByteArray();

            HttpHeaders headers = new HttpHeaders();
            headers.setContentDisposition(org.springframework.http.ContentDisposition.attachment()
                    .filename("endpoints-" + System.currentTimeMillis() + ".xlsx").build());
            return ResponseEntity.ok()
                    .headers(headers)
                    .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                    .contentLength(bytes.length)
                    .body(new ByteArrayResource(bytes));
        }
    }

    @GetMapping("/modelo")
    @ResponseBody
    public ResponseEntity<ByteArrayResource> baixarModelo() throws IOException {
        try (Workbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            CellStyle header = wb.createCellStyle();
            Font fontHdr = wb.createFont();
            fontHdr.setBold(true);
            fontHdr.setColor(IndexedColors.WHITE.getIndex());
            header.setFont(fontHdr);
            header.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
            header.setFillPattern(org.apache.poi.ss.usermodel.FillPatternType.SOLID_FOREGROUND);

            CellStyle bold = wb.createCellStyle();
            Font fb = wb.createFont(); fb.setBold(true);
            bold.setFont(fb);

            // Sheet 0: dados para importacao (ordem esperada por EndpointService.processarLinha)
            Sheet sheet = wb.createSheet("Endpoints");
            String[] cols = {"Localizacao", "MAC", "Hostname", "Descricao", "IPv4", "Modelo", "SN"};
            Row h = sheet.createRow(0);
            for (int i = 0; i < cols.length; i++) {
                Cell c = h.createCell(i);
                c.setCellValue(cols[i]);
                c.setCellStyle(header);
            }

            String[][] exemplos = {
                {"F116-SC-ADM01", "24:1F:BD:D9:8F:E6", "SLZ-CD116-ADM01", "", "10.220.113.4", "Aruba IAP-305", "SN000123"},
                {"F111-LG-AP02",  "B0:27:CF:CF:29:90", "SLZ-CD356-SPZ-AP13", "", "10.220.113.204", "Aruba IAP-305", "SN000124"},
                {"F90-FT-03",     "B8:50:01:72:94:98", "SLZ-CD116-FAT03",     "", "10.220.113.186", "Aruba IAP-225", "SN000125"}
            };
            for (int r = 0; r < exemplos.length; r++) {
                Row row = sheet.createRow(r + 1);
                for (int c = 0; c < exemplos[r].length; c++) {
                    row.createCell(c).setCellValue(exemplos[r][c]);
                }
            }
            for (int i = 0; i < cols.length; i++) sheet.autoSizeColumn(i);

            // Sheet 1: instrucoes
            Sheet ajuda = wb.createSheet("Instrucoes");
            int k = 0;
            Row t = ajuda.createRow(k++);
            Cell tc = t.createCell(0); tc.setCellValue("INSTRUCOES DE IMPORTACAO"); tc.setCellStyle(header);
            k++;
            ajuda.createRow(k++).createCell(0).setCellValue("1. Antes de enviar, selecione o COMPLEXO e a CATEGORIA no formulario de upload.");
            ajuda.createRow(k++).createCell(0).setCellValue("2. Apague os exemplos antes de preencher com seus dados reais.");
            ajuda.createRow(k++).createCell(0).setCellValue("3. Mantenha a ORDEM e NOMES das colunas da aba 'Endpoints'.");
            ajuda.createRow(k++).createCell(0).setCellValue("4. A coluna Descricao fica reservada para notas; pode ficar em branco.");
            k++;
            Row t2 = ajuda.createRow(k++);
            Cell tc2 = t2.createCell(0); tc2.setCellValue("COLUNAS"); tc2.setCellStyle(header);
            String[][] guia = {
                {"A - Localizacao", "Profile / sala / posicao. Ex: F116-SC-ADM01"},
                {"B - MAC",          "Endereco MAC. Ex: 24:1F:BD:D9:8F:E6"},
                {"C - Hostname",     "Nome do dispositivo. Ex: SLZ-CD116-ADM01"},
                {"D - Descricao",    "Opcional (nao e obrigatorio preencher)"},
                {"E - IPv4",         "IP v4. Ex: 10.220.113.4"},
                {"F - Modelo",       "Modelo do equipamento. Ex: Aruba IAP-305"},
                {"G - SN",           "Numero de serie"}
            };
            for (String[] g : guia) {
                Row r = ajuda.createRow(k++);
                Cell a = r.createCell(0); a.setCellValue(g[0]); a.setCellStyle(bold);
                r.createCell(1).setCellValue(g[1]);
            }
            ajuda.autoSizeColumn(0);
            ajuda.autoSizeColumn(1);

            wb.write(out);
            byte[] bytes = out.toByteArray();

            HttpHeaders headers = new HttpHeaders();
            headers.setContentDisposition(org.springframework.http.ContentDisposition.attachment()
                    .filename("modelo-endpoints.xlsx").build());
            return ResponseEntity.ok()
                    .headers(headers)
                    .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                    .contentLength(bytes.length)
                    .body(new ByteArrayResource(bytes));
        }
    }

    private String safe(String s) { return s == null ? "" : s; }

    private String getClientIp(HttpServletRequest request) {
        String xForwarded = request.getHeader("X-Forwarded-For");
        if (xForwarded != null && !xForwarded.isEmpty()) {
            return xForwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
///sudo docker run --name mysql -e MYSQL_DATABASE=informatica -e MYSQL_ROOT_PASSWORD=Tgfhvbnm1@ -p 3306:3306 -d mysql:latest