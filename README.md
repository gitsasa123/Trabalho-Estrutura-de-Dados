🌳 Estruturas de Dados em Árvore: BST, AVL e Rubro-Negra

> Projeto da disciplina de **Estrutura de Dados** (Engenharia de Computação – IFC São Bento do Sul) que implementa, em Java, uma árvore binária de busca e duas árvores balanceadas e as compara usando um conjunto de dados real de músicas, o **Free Music Archive (FMA)**.

![Linguagem](https://img.shields.io/badge/linguagem-Java-blue)
![Status](https://img.shields.io/badge/status-implementa%C3%A7%C3%A3o%20completa-yellow)

---

## 📑 Sumário

- [Sobre o Projeto](#-sobre-o-projeto)
- [Andamento](#-andamento)
- [Objetivos](#-objetivos)
- [Estruturas Implementadas](#-estruturas-implementadas)
- [Conjuntos de Dados](#-conjuntos-de-dados)
- [Estrutura do Repositório](#-estrutura-do-repositório)
- [Requisitos](#-requisitos)
- [Como Executar](#-como-executar)
- [Exemplo de Uso](#-exemplo-de-uso)
- [Metodologia dos Experimentos](#-metodologia-dos-experimentos)
- [Resultados](#-resultados)
- [Análise e Discussão](#-análise-e-discussão)
- [Testes](#-testes)
- [Equipe](#-equipe)
- [Referências](#-referências)
- [Licença](#-licença)

---

## 📖 Sobre o Projeto

Este projeto implementa três árvores de busca em Java — **BST** (árvore binária de busca sem balanceamento), **AVL** e **Rubro-Negra** — com as operações de inserção, busca, remoção, altura, percursos e contagem de comparações. As três seguem a mesma interface (`Arvore`), o que permite rodar exatamente os mesmos experimentos em cada uma.

Usamos metadados reais de faixas do Free Music Archive (chave: `track_id`) para comparar **tempo de execução, número de comparações e altura** de cada estrutura em cenários práticos, como inserção em ordem crescente (pior caso da BST) e em ordem aleatória (caso médio), e assim entender as vantagens e desvantagens de cada uma.

---

## 📌 Andamento

- [x] Escolha do dataset (FMA)
- [x] Interface comum (`Arvore`) e convenções do grupo
- [x] BST (`inserir`, `buscar`, `remover`, `altura`, `percorrer`, contador de comparações)
- [x] AVL (com rebalanceamento na remoção)
- [x] Rubro-Negra (com rebalanceamento na remoção)
- [x] Loader do dataset e script de preparo dos dados
- [x] Testes de cada árvore
- [x] Benchmark e gerador de gráficos
- [x] Demonstração interativa (`Main`)
- [ ] Rodar o benchmark com o FMA real e preencher a seção de [Resultados](#-resultados)
- [ ] Slides e apresentação

---

## 🎯 Objetivos

- Implementar uma **Árvore Binária de Busca (BST)**.
- Implementar **duas árvores balanceadas** (AVL e Rubro-Negra), com remoção que mantém o balanceamento.
- Aplicar as estruturas a um **conjunto de dados real** (mínimo de 10.000 registros).
- Comparar **tempo de execução, número de comparações e altura** das árvores.
- Documentar as características de cada estrutura em cenários práticos.

---

## 🌲 Estruturas Implementadas

| Estrutura | Arquivo | Balanceamento | Complexidade (pior caso) |
|-----------|---------|---------------|--------------------------|
| Árvore Binária de Busca | `BST.java` | Não possui | O(n) |
| Árvore AVL | `AVL.java` | Rigoroso (fator de balanceamento) | O(log n) |
| Árvore Rubro-Negra | `RubroNegra.java` | Flexível (regras de cor) | O(log n) |

### Operações suportadas (interface `Arvore<K, V>`)

- `inserir(chave, dados)`
- `buscar(chave)` → dados (ou `null` se a chave não existe)
- `remover(chave)` → `true` se a chave existia
- `altura()`
- `percorrer(Ordem.PRE | Ordem.EM | Ordem.POS)` → lista de chaves
- `contadorComparacoes()` e `zerarContador()`
- `tamanho()` e `validar()` (verifica as propriedades da estrutura; usado nos testes)

### Convenções (iguais nas três árvores)

- **Chave repetida** em `inserir`: os dados antigos são substituídos e o tamanho não muda.
- **Altura:** número de nós do caminho mais longo da raiz até uma folha (árvore vazia = 0; um único nó = 1).
- **Comparações:** cada comparação entre chaves (`compareTo`) feita em `inserir`, `buscar` e `remover` incrementa o contador.
- **BST iterativa:** todos os métodos da BST usam laços (e não recursão), pois com chaves em ordem crescente a altura chega a ~100 mil e a recursão estouraria a pilha.

---

## 📊 Conjuntos de Dados

| Dataset | Fonte | Nº de registros | Chave utilizada | Descrição |
|---------|-------|-----------------|-----------------|-----------|
| FMA – metadados de faixas (`tracks.csv`) | [Free Music Archive Dataset](https://github.com/mdeff/fma) · [UCI](https://archive.ics.uci.edu/dataset/386/fma+a+dataset+for+music+analysis) | 106.574 faixas no total; _(a preencher: quantidade em `data/faixas.csv`, impressa pelo script de preparo)_ | `track_id` (inteiro único) | Título, artista, gênero principal, número de ouvintes e duração de faixas reais publicadas no Free Music Archive |

- **Licença dos dados:** CC BY 4.0.
- O arquivo original (`fma_metadata.zip`, 342 MiB) **não é versionado**. O repositório inclui o script de download e preparo (veja [Como Executar](#-como-executar)).
- Faixas sem gênero principal ficam com o gênero "Desconhecido"; faixas sem título, artista, ouvintes ou duração são descartadas.
- O `track_id` já vem em ordem crescente: inserir sem embaralhar é o pior caso da BST (a árvore vira uma lista); embaralhando, temos o caso médio.

---

## 🗂 Estrutura do Repositório

```
.
├── README.md
├── LICENSE
├── .gitignore
├── data/
│   ├── fma_metadata/        # Dados originais do FMA (não versionados)
│   └── faixas.csv           # Dados tratados, gerados pelo script de preparo
├── scripts/
│   ├── preparar_dados.py    # Converte o tracks.csv do FMA em data/faixas.csv
│   └── gerar_graficos.py    # Gera os gráficos e a tabela-resumo a partir dos CSVs do benchmark
├── src/
│   └── arvores/
│       ├── Arvore.java            # Interface comum às 3 árvores
│       ├── Ordem.java             # PRE, EM, POS
│       ├── BST.java               # Árvore Binária de Busca
│       ├── AVL.java               # Árvore AVL
│       ├── RubroNegra.java        # Árvore Rubro-Negra
│       ├── Faixa.java             # Dados guardados em cada nó
│       ├── CarregadorFaixas.java  # Leitura de data/faixas.csv
│       ├── TesteArvore.java       # Testes das árvores
│       ├── Benchmark.java         # Medição de desempenho
│       └── Main.java              # Demonstração interativa
├── docs/
│   └── GUIA_DE_ESTUDO.md    # Mapa do código e perguntas prováveis para a apresentação
├── out/                     # Classes compiladas (não versionado)
└── results/
    ├── tabelas/             # CSVs do benchmark e tabela-resumo
    └── graficos/            # Gráficos gerados
```

---

## ⚙️ Requisitos

- **Java 17 ou superior** (testado com Java 21)
- **Python 3** com `pandas` e `matplotlib` (`pip install pandas matplotlib`) — usado apenas para preparar os dados e gerar os gráficos

---

## 🚀 Como Executar

Os comandos abaixo funcionam no terminal do VS Code (PowerShell ou Git Bash), no Linux e no macOS. Rode todos na pasta do projeto.

### 1. Baixar e preparar os dados

Baixe o arquivo https://os.unil.cloud.switch.ch/fma/fma_metadata.zip (342 MiB) e extraia dentro de `data/`, de modo que exista `data/fma_metadata/tracks.csv`.

```bash
# Linux / macOS / Git Bash
curl -L -o data/fma_metadata.zip https://os.unil.cloud.switch.ch/fma/fma_metadata.zip
unzip data/fma_metadata.zip -d data/

# Windows (PowerShell): use curl.exe (curl, sozinho, é outro comando no PowerShell)
curl.exe -L -o data/fma_metadata.zip https://os.unil.cloud.switch.ch/fma/fma_metadata.zip
tar -xf data/fma_metadata.zip -C data
```

Depois, gere o CSV tratado (a quantidade de faixas aparece no final):

```bash
pip install pandas matplotlib
python scripts/preparar_dados.py data/fma_metadata/tracks.csv data/faixas.csv
```

Se o script avisar de coluna faltando, ele lista as colunas existentes: ajuste o dicionário `COLUNAS` no topo de `scripts/preparar_dados.py`.

### 2. Compilar e testar

```bash
javac -encoding UTF-8 -d out src/arvores/*.java
java -cp out arvores.TesteArvore
```

> Dicas para Windows: se o PowerShell não expandir o `*.java`, use `javac -encoding UTF-8 -d out (Get-ChildItem src/arvores/*.java)`. Se os acentos aparecerem trocados no terminal, rode o Java com `java -Dfile.encoding=UTF-8 -Dstdout.encoding=UTF-8 -cp out ...`.

### 3. Demonstração interativa

```bash
java -cp out arvores.Main data/faixas.csv 20000 embaralhado
java -cp out arvores.Main data/faixas.csv 20000 ordenado      # pior caso da BST
```

### 4. Executar o benchmark

```bash
java -cp out arvores.Benchmark
```

Parâmetros opcionais: `java -cp out arvores.Benchmark [csv] [N] [repeticoes] [passo] [pastaSaida] [nAltura]`

| Parâmetro | Descrição | Padrão |
|-----------|-----------|--------|
| `csv` | Dados tratados | `data/faixas.csv` |
| `N` | Nº de faixas no experimento de desempenho (0 = todas) | `50000` |
| `repeticoes` | Repetições de cada medição (usamos a média) | `5` |
| `passo` | A cada quantas inserções medir a altura | `1000` |
| `pastaSaida` | Onde gravar os CSVs | `results/tabelas` |
| `nAltura` | Nº de faixas no experimento de altura (0 = todas) | `0` |

O benchmark grava em `results/tabelas/`: `desempenho.csv`, `altura_incremental.csv` e `ambiente.txt` (versão do Java, sistema e processadores, para a seção "Ambiente de teste"). Na BST com chaves ordenadas as operações são lentas (O(n²) no total): espere alguns minutos. Para um teste rápido: `java -cp out arvores.Benchmark data/faixas.csv 10000 2`.

### 5. Gerar os gráficos

```bash
python scripts/gerar_graficos.py
```

Grava em `results/graficos/`: `desempenho_tempo.png`, `desempenho_comparacoes.png` e `altura.png` (altura × elementos inseridos, medida a cada 1.000 inserções até o tamanho total do dataset), além de `results/tabelas/resumo_para_readme.md` com as tabelas prontas para colar na seção [Resultados](#-resultados).

---

## 💡 Exemplo de Uso

```java
import arvores.*;

Arvore<Integer, String> arvore = new AVL<>();
int[] chaves = {10, 20, 30, 40, 50, 25};
for (int k : chaves) {
    arvore.inserir(k, "dados-" + k);
}

System.out.println(arvore.buscar(25));              // dados-25
System.out.println(arvore.altura());                // 3
System.out.println(arvore.percorrer(Ordem.EM));     // [10, 20, 25, 30, 40, 50]
System.out.println(arvore.contadorComparacoes());   // total de comparações feitas
```

---

## 🔬 Metodologia dos Experimentos

**Cenários avaliados** (os mesmos dados, na mesma ordem, para as três árvores):

1. **Inserção em ordem crescente de `track_id`:** pior caso para a BST.
2. **Inserção em ordem aleatória** (embaralhamento com semente fixa, para ser reproduzível): caso médio.

**Operações medidas em cada cenário:** inserção de todas as faixas; busca de todas as chaves existentes; busca do mesmo número de chaves inexistentes; remoção de metade das chaves, em ordem aleatória. Ao final, `validar()` confirma que cada árvore continua correta.

**Métricas coletadas:**

- Tempo de execução de cada operação (ms): média e desvio padrão de 5 repetições, depois de um aquecimento da JVM
- Número de comparações entre chaves
- Altura após cada etapa e altura a cada 1.000 inserções (balanceamento)
- Uso de memória: não medido (opcional no enunciado)

**Ambiente de teste:** _(a preencher com `results/tabelas/ambiente.txt`: processador, memória RAM, sistema operacional e versão do Java)_

---

## 📈 Resultados

> ⚠️ _Ainda não executamos o benchmark com o FMA real. Cole aqui o conteúdo de `results/tabelas/resumo_para_readme.md` e os gráficos depois de rodar os passos 4 e 5._

### Tabela comparativa

| Estrutura | Inserção (ms) | Busca (ms) | Remoção (ms) | Comparações | Altura final |
|-----------|---------------|------------|--------------|-------------|--------------|
| BST | — | — | — | — | — |
| AVL | — | — | — | — | — |
| Rubro-Negra | — | — | — | — | — |

### Gráficos

```
![Tempo](results/graficos/desempenho_tempo.png)
![Comparações](results/graficos/desempenho_comparacoes.png)
![Altura](results/graficos/altura.png)
```

---

## 🧠 Análise e Discussão

_(a preencher depois dos experimentos)_ Tópicos que vamos discutir:

- Em quais cenários a BST apresentou degradação de desempenho?
- Qual árvore teve melhor desempenho em buscas? E em inserções e remoções?
- A AVL (mais rigorosa) e a Rubro-Negra (mais flexível) diferem em altura e em comparações?
- Qual estrutura é mais adequada para cada tipo de aplicação real?
- Limitações do estudo e possíveis melhorias.

---

## ✅ Testes

```bash
javac -encoding UTF-8 -d out src/arvores/*.java
java -cp out arvores.TesteArvore
```

Os testes comparam cada árvore com o `TreeMap` do Java em milhares de operações aleatórias (inserir, remover e buscar) e chamam `validar()` depois de cada operação. Também conferem os três percursos, o contador de comparações, chaves repetidas, inserção em ordem crescente e decrescente e o limite de altura logarítmica das árvores balanceadas.

---

## 👥 Equipe

| Nome | GitHub | Responsabilidade |
|------|--------|------------------|
| Henrique Balassa | [@henriquebalassa](https://github.com/henriquebalassa) | BST e leitura de dados |
| Laura Veiga      | [@Lauraveigaa](https://github.com/Lauraveigaa) | AVL e benchmarks |
| Saulo Resende    | [@gitsasa123](https://github.com/gitsasa123) | Rubro-Negra e relatório |

**Disciplina:** Estrutura de Dados
**Professor:** Lucas Dominguez Cordeiro
**Curso:** Engenharia de Computação
**Instituição:** Instituto Federal Catarinense – Campus São Bento do Sul
**Semestre:** 2026/2

---

## 📚 Referências

- DEFFERRARD, M.; BENZI, K.; VANDERGHEYNST, P.; BRESSON, X. **FMA: A Dataset for Music Analysis**. In: 18th International Society for Music Information Retrieval Conference (ISMIR), 2017. Disponível em: <https://arxiv.org/abs/1612.01840>. Dados: <https://github.com/mdeff/fma>.
- CORMEN, T. H. et al. **Algoritmos: Teoria e Prática**. 3. ed. Rio de Janeiro: Elsevier, 2012. (Rubro-Negra: cap. 13)
- GOODRICH, M. T.; TAMASSIA, R. **Estruturas de Dados & Algoritmos em Java**. Bookman, 2013.
- SEDGEWICK, R.; WAYNE, K. **Algorithms**. 4. ed. Addison-Wesley, 2011.
- Visualizador de árvores: <https://www.cs.usfca.edu/~galles/visualization/>
- Documentação da API Java: <https://docs.oracle.com/en/java/javase/21/docs/api/>

---

## 📄 Licença

O código deste projeto está licenciado sob a licença **MIT**. Consulte o arquivo [LICENSE](LICENSE) para mais detalhes. Os dados do FMA seguem a licença **CC BY 4.0**.
