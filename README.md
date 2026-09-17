# Showcial Media — Sistema de Ordens de Serviço (OS)

Sistema completo para gestão de Ordens de Serviço, controle financeiro, recebimentos via Pix, geração de relatórios, envio de mensagens personalizadas no WhatsApp e emissão de OS em PDF para impressão.

---

## 🏢 Dados da Empresa Configurados

- **Razão / Nome Fantasia:** Showcial Media
- **CNPJ:** `34.741.143/0001-76`
- **Chave Pix (CNPJ):** `34.741.143/0001-76`
- **Favorecido / Titular do Pix:** Jorge Leandro de Jesus Braga

---

## 🚀 Como Hospedar no GitHub Pages (Passo a Passo)

Este projeto já está 100% pronto para ser hospedado no **GitHub Pages** sem precisar de compilação ou servidores externos.

### Método 1: Direto pelo GitHub Pages (Mais Rápido — 2 Cliques)

1. Envie ou sincronize este repositório no seu GitHub.
2. No seu repositório no GitHub, clique na aba **Settings** (Configurações).
3. No menu lateral esquerdo, clique em **Pages**.
4. Em **Build and deployment**:
   - **Source:** Selecione `Deploy from a branch`.
   - **Branch:** Selecione `main` (ou `master`) e escolha a pasta `/ (root)` ou `/docs`.
   - Clique em **Save**.
5. Aguarde cerca de 1 a 2 minutos e o GitHub informará o link do seu site no ar (exemplo: `https://seu-usuario.github.io/seu-repositorio/`)!

### Método 2: Via GitHub Actions (Automático)

O arquivo `.github/workflows/deploy.yml` já está configurado. Basta ir em **Settings** > **Pages** > em **Source** selecionar `GitHub Actions`. Toda vez que você fizer push, o site é atualizado automaticamente!

---

## 🛠️ Recursos Incluídos no Site

- **Painel Dashboard:** Indicadores em tempo real de faturamento total, valores já recebidos, saldo devedor e contadores de OS abertas/em andamento.
- **Gestão Completa de OS (CRUD):** Criação e edição com numeração sequencial (#0001, #0002...), dados do cliente, cálculo automático de quantidade, valor unitário e desconto.
- **Fotos de Referência com Zoom e Links:** Anexação de fotos de referência com galeria, visualização em tela cheia (zoom) e links diretos para visualização do cliente.
- **Emissão e Impressão de OS em PDF:** Layout A4 estilizado para impressão e download de PDF com logotipo, dados completos da Showcial Media, chave Pix de Jorge Leandro de Jesus Braga e campos de assinatura.
- **Compartilhamento Rápido no WhatsApp:** Envio com mensagem pronta, formatada com quebra de linha, valores, dados do Pix e links das fotos anexadas.
- **Controle de Pagamentos:** Registro de entradas e parcelas parciais com cálculo automático de quitação.
- **Base de Clientes:** Histórico de serviços e total acumulado por cliente.
- **Backup Local e Nuvem:** Armazenamento offline automático via LocalStorage, exportação/importação de arquivos JSON de backup e suporte a sincronização com o Supabase.
- **Versão Mobile & Desktop:** Layout 100% responsivo para uso no computador ou celular.
