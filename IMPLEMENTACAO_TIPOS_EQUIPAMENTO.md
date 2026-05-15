# Implementação de Tipos de Equipamento - Resumo das Mudanças

## 📋 Objetivo
Adicionar suporte para diferentes tipos de equipamentos (Laptop, Desktop, Impressora, Balança) com campos condicionais específicos:
- **Impressora**: Campo "Caminho CUPS"
- **Balança**: Campo "Caminho Monitor"

---

## ✅ Mudanças Realizadas

### 1. Backend - Entity (AgenteEntity.java)
**Arquivo**: `src/main/java/com/example/projetoAP/repository/AgenteEntity.java`

**Campos adicionados:**
```java
private String tipoEquipamento;      // LAPTOP / DESKTOP / IMPRESSORA / BALANCA
private String caminhoCups;          // Para impressoras (caminho CUPS)
private String caminhoMonitor;       // Para balança (caminho do monitor)
```

**Getters e Setters adicionados:**
- `getTipoEquipamento()` / `setTipoEquipamento()`
- `getCaminhoCups()` / `setCaminhoCups()`
- `getCaminhoMonitor()` / `setCaminhoMonitor()`

---

### 2. Backend - Controller (AgenteController.java)
**Arquivo**: `src/main/java/com/example/projetoAP/controler/AgenteController.java`

**Função `aplicarCamposEquipamento()` atualizada:**
- Adicionado processamento de `tipoEquipamento`
- Adicionado processamento de `caminhoCups`
- Adicionado processamento de `caminhoMonitor`

---

### 3. Frontend - HTML (equipamentos.html)
**Arquivo**: `src/main/resources/templates/equipamentos.html`

**Campos adicionados no modal de cadastro:**
```html
<!-- Select de Tipo de Equipamento -->
<select id="eq-tipoEquipamento" onchange="atualizarCamposCondicionais()">
    <option value="">Selecione um tipo</option>
    <option value="LAPTOP">Laptop</option>
    <option value="DESKTOP">Desktop</option>
    <option value="IMPRESSORA">Impressora</option>
    <option value="BALANCA">Balança</option>
</select>

<!-- Campo condicional para Impressoras -->
<input id="eq-caminhoCups" placeholder="Ex: /dev/usb/lp0 ou socket://ip:9100">

<!-- Campo condicional para Balanças -->
<input id="eq-caminhoMonitor" placeholder="Ex: /dev/ttyUSB0 ou com0">
```

---

### 4. Frontend - JavaScript (equipamentos.js)
**Arquivo**: `src/main/resources/static/css/equipamentos.js`

**Novas funções:**
```javascript
// Mostra/oculta campos baseado no tipo de equipamento selecionado
function atualizarCamposCondicionais() {
    var tipoEquip = document.getElementById('eq-tipoEquipamento').value;
    // Se IMPRESSORA: mostra campo Caminho CUPS
    // Se BALANCA: mostra campo Caminho Monitor
    // Senão: oculta ambos
}
```

**Funções atualizadas:**
- `abrirModalNovo()`: Limpa os novos campos
- `editarEquip()`: Carrega os valores dos novos campos
- `salvarEquip()`: Envia os novos campos ao backend
- `renderDetInfo()`: Mostra os campos condicionais nos detalhes

---

### 5. Banco de Dados - Migração SQL
**Arquivo**: `src/main/resources/db/migration/V008__add_tipo_equipamento_fields.sql`

```sql
ALTER TABLE agente ADD COLUMN tipo_equipamento VARCHAR(50);
ALTER TABLE agente ADD COLUMN caminho_cups VARCHAR(255);
ALTER TABLE agente ADD COLUMN caminho_monitor VARCHAR(255);
CREATE INDEX idx_agente_tipo_equipamento ON agente(tipo_equipamento);
```

---

## 🎯 Como Funciona

### Fluxo no Cadastro:
1. Usuário vai em "Cadastro de Equipamento"
2. Seleciona o **Tipo de Equipamento** (Laptop, Desktop, Impressora, Balança)
3. Se selecionou **Impressora**:
   - Campo "Caminho CUPS" aparece
   - Exemplo: `/dev/usb/lp0` ou `socket://192.168.1.100:9100`
4. Se selecionou **Balança**:
   - Campo "Caminho Monitor" aparece
   - Exemplo: `/dev/ttyUSB0` ou `com0` (para Windows)
5. Ao salvar, os dados são enviados ao backend

### Fluxo nos Detalhes:
1. Ao abrir "Detalhes do Equipamento"
2. Se o tipo é **Impressora**:
   - Mostra: "Caminho CUPS: [valor]"
3. Se o tipo é **Balança**:
   - Mostra: "Caminho Monitor: [valor]"
4. Outros tipos não mostram esses campos

---

## 📊 Estrutura do Banco de Dados

| Campo | Tipo | Descrição |
|-------|------|-----------|
| tipo_equipamento | VARCHAR(50) | LAPTOP, DESKTOP, IMPRESSORA, BALANCA |
| caminho_cups | VARCHAR(255) | Caminho CUPS para impressoras |
| caminho_monitor | VARCHAR(255) | Caminho equivalente para balanças |

---

## 🧪 Testes Recomendados

1. **Cadastro de Impressora**:
   - Tipo de Equipamento: Impressora
   - Preencher Caminho CUPS
   - Salvar e verificar nos detalhes

2. **Cadastro de Balança**:
   - Tipo de Equipamento: Balança
   - Preencher Caminho Monitor
   - Salvar e verificar nos detalhes

3. **Cadastro de Laptop/Desktop**:
   - Tipo de Equipamento: Laptop ou Desktop
   - Verificar se os campos condicionais NÃO aparecem

4. **Edição**:
   - Abrir um equipamento salvo
   - Verificar se os campos condicionais aparecem corretamente

---

## 📝 Notas Téceis

- Os campos `caminhoCups` e `caminhoMonitor` só aparecem quando relevante (validação no frontend com `atualizarCamposCondicionais()`)
- Os detalhes só mostram esses campos se existirem e se o tipo de equipamento for apropriado
- Nenhuma validação obrigatória foi adicionada; os campos são opcionais
- A migração SQL usa Flyway (V008) e será executada automaticamente na próxima inicialização

---

## ✨ Arquivos Modificados

1. ✅ `AgenteEntity.java` - Adicionou 3 campos + getters/setters
2. ✅ `AgenteController.java` - Processar novos campos
3. ✅ `equipamentos.html` - Modal com select e campos condicionais
4. ✅ `equipamentos.js` - Funções para validação condicional
5. ✅ `V008__add_tipo_equipamento_fields.sql` - Migração do banco

---

**Data**: 15 de maio de 2026  
**Status**: ✅ Implementado e compilado
