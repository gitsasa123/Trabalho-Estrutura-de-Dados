🌳 Estruturas de Dados em Árvore: BST, AVL e Rubro-Negra

> Projeto da disciplina de **Estrutura de Dados** (Engenharia de Computação – IFC São Bento do Sul) que implementa, em Java, uma árvore binária de busca e duas árvores balanceadas e as compara usando um conjunto de dados real de músicas, o **Free Music Archive (FMA)**.

![Linguagem](https://img.shields.io/badge/linguagem-Java-blue)
![Status](https://img.shields.io/badge/status-conclu%C3%ADdo-brightgreen)

---

## 📑 Sumário

- [Sobre o Projeto](#-sobre-o-projeto)
- [Objetivos](#-objetivos)
- [Estruturas Implementadas](#-estruturas-implementadas)
- [Conjuntos de Dados](#-conjuntos-de-dados)
- [Requisitos](#-requisitos)
- [Como Executar](#-como-executar)
- [Metodologia dos Experimentos](#-metodologia-dos-experimentos)
- [Resultados](#-resultados)
- [Análise e Discussão](#-análise-e-discussão)
- [Testes](#-testes)
- [Referências](#-referências)

---

## 📖 Sobre o Projeto

Este projeto implementa três árvores de busca em Java — **BST** (árvore binária de busca sem balanceamento), **AVL** e **Rubro-Negra** — com as operações de inserção, busca, remoção, altura, percursos e contagem de comparações. As três seguem a mesma interface (`Arvore`), o que permite rodar exatamente os mesmos experimentos em cada uma.

Usamos metadados reais de faixas do Free Music Archive (chave: `track_id`) para comparar **tempo de execução, número de comparações e altura** de cada estrutura em cenários práticos, como inserção em ordem crescente (pior caso da BST) e em ordem aleatória (caso médio), e assim entender as vantagens e desvantagens de cada uma.

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

### Operações suportadas

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
| FMA – metadados de faixas (`tracks.csv`) | [Free Music Archive Dataset](https://github.com/mdeff/fma) · [UCI](https://archive.ics.uci.edu/dataset/386/fma+a+dataset+for+music+analysis) | 106.574 faixas no total; **106.573** em `data/faixas.csv` depois da limpeza (`track_id` de 2 a 155.320) | `track_id` (inteiro único) | Título, artista, gênero principal, número de ouvintes e duração de faixas reais publicadas no Free Music Archive |

---


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
(Caso as imagens não consigam ser visualizadas dentro da IDE, as imagens vão estar salvas dentro das pastas do projeto em seu computador)

---

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

**Ambiente de teste** (gerado pelo próprio benchmark em `results/tabelas/ambiente.txt`):

| Item | Valor |
|------|-------|
| Java | 26.0.2 (Java HotSpot 64-Bit Server VM) |
| Sistema | Windows 11 (amd64) |
| Processadores lógicos | 16 |
| Memória máxima da JVM | 3.944 MB |
| Dataset | `data/faixas.csv` (106.573 faixas) |
| N do experimento de desempenho | 50.000 faixas |
| Repetições | 5 |
| Passo da altura | 1.000 inserções |
| Semente do embaralhamento | 2026 |

---

## 📈 Resultados

Resultados do benchmark rodado no ambiente descrito acima. Os tempos são o **total** de cada conjunto de operações (média de 5 repetições, em ms) e variam de uma execução para outra; as **comparações** e as **alturas** não dependem da máquina. Os números completos estão em `results/tabelas/desempenho.csv` e `results/tabelas/altura_incremental.csv`.

### Altura final (todas as 106.573 faixas inseridas)

| Cenário | BST | AVL | Rubro-Negra |
|---------|-----|-----|-------------|
| Ordem crescente de `track_id` | **106.573** | 17 | 31 |
| Ordem aleatória | 40 | 20 | 20 |

### Desempenho — inserção em ordem crescente de `track_id` (pior caso da BST), N = 50.000

| Estrutura | Inserção (ms) | Busca existente (ms) | Busca inexistente (ms) | Remoção (ms) | Comparações (inserção) | Altura após inserir | Altura após remover |
|-----------|---------------|----------------------|------------------------|--------------|------------------------|---------------------|---------------------|
| BST | 4.047,7 | 4.215,8 | 7.115,1 | 1.726,9 | 1.249.975.000 | 50.000 | 25.000 |
| AVL | 10,2 | 20,0 | 7,0 | 13,1 | 734.465 | 16 | 16 |
| Rubro-Negra | 9,9 | 19,5 | 8,7 | 11,4 | 1.286.225 | 29 | 17 |

### Desempenho — inserção em ordem aleatória (caso médio), N = 50.000

| Estrutura | Inserção (ms) | Busca existente (ms) | Busca inexistente (ms) | Remoção (ms) | Comparações (inserção) | Altura após inserir | Altura após remover |
|-----------|---------------|----------------------|------------------------|--------------|------------------------|---------------------|---------------------|
| BST | 25,6 | 22,4 | 7,0 | 11,7 | 962.667 | 38 | 34 |
| AVL | 28,0 | 18,6 | 7,2 | 14,3 | 720.039 | 18 | 17 |
| Rubro-Negra | 22,7 | 18,9 | 7,0 | 11,9 | 725.785 | 19 | 18 |

### Gráficos

![Tempo](results/graficos/desempenho_tempo.png)
![Comparações](results/graficos/desempenho_comparacoes.png)
![Altura](results/graficos/altura.png)

---

## 🧠 Análise e Discussão

- **Degradação da BST:** com chaves em ordem crescente a BST virou uma lista: altura de 106.573 com todas as faixas e 1.249.975.000 comparações para inserir 50.000 chaves, exatamente n(n−1)/2 (comportamento O(n) por operação, O(n²) no total). Inserir levou cerca de 4 s, contra cerca de 10 ms na AVL e na Rubro-Negra (centenas de vezes mais). A busca por chave inexistente foi a mais cara (cerca de 7 s). O benchmark completo levou quase 2 minutos, quase todo gasto na BST ordenada.
- **Caso médio:** com chaves embaralhadas a BST se comportou bem (altura 40; 38 com 50.000 faixas) e ficou na mesma ordem de grandeza das balanceadas. Chegou a inserir mais rápido que a AVL, que paga o custo das rotações.
- **Buscas, inserções e remoções:** nos tempos, AVL e Rubro-Negra ficaram praticamente empatadas; diferenças de poucos milissegundos estão dentro do ruído de medição. Nas comparações, a AVL fez menos que a Rubro-Negra na inserção ordenada (734.465 contra 1.286.225), porque a árvore ficou mais baixa (16 contra 29).
- **AVL × Rubro-Negra:** a AVL é mais rígida e manteve altura menor (17 e 20 com todas as faixas; limite teórico ≈ 24). A Rubro-Negra é mais flexível: 31 (ordenado, perto do limite teórico ≈ 33) e 20 (aleatório). As duas garantem O(log n) nos dois cenários, e a remoção também manteve o equilíbrio (alturas continuaram baixas depois de remover metade das chaves).
- **Aplicações:** se os dados podem chegar ordenados, ou parcialmente ordenados, uma árvore balanceada é a escolha segura. A Rubro-Negra é a usada em bibliotecas padrão (`TreeMap` do Java); a AVL tende a favorecer cargas com muitas buscas. A BST simples só se justifica quando a ordem de entrada é realmente aleatória.
- **Limitações:** um único dataset e uma única máquina; tempos dependem da JVM (por isso aquecimento e repetições); memória não medida; o experimento de desempenho usa 50.000 das 106.573 faixas (a altura usa todas); chaves inteiras "encaixotadas" (`Integer`). Como melhorias: medir memória, testar chaves de texto e outros datasets e comparar com uma árvore B.

---

## ✅ Testes

```bash
javac -encoding UTF-8 -d out src/arvores/*.java
java -cp out arvores.TesteArvore
```

Os testes (804.567 verificações aprovadas) comparam cada árvore com o `TreeMap` do Java em milhares de operações aleatórias (inserir, remover e buscar) e chamam `validar()` depois de cada operação. Também conferem os três percursos, o contador de comparações, chaves repetidas, inserção em ordem crescente e decrescente e o limite de altura logarítmica das árvores balanceadas.

---

---

## 📚 Referências

- DEFFERRARD, M.; BENZI, K.; VANDERGHEYNST, P.; BRESSON, X. **FMA: A Dataset for Music Analysis**. In: 18th International Society for Music Information Retrieval Conference (ISMIR), 2017. Disponível em: <https://arxiv.org/abs/1612.01840>. Dados: <https://github.com/mdeff/fma>.
- CORMEN, T. H. et al. **Algoritmos: Teoria e Prática**. 3. ed. Rio de Janeiro: Elsevier, 2012. (Rubro-Negra: cap. 13)
- GOODRICH, M. T.; TAMASSIA, R. **Estruturas de Dados & Algoritmos em Java**. Bookman, 2013.
- SEDGEWICK, R.; WAYNE, K. **Algorithms**. 4. ed. Addison-Wesley, 2011.
- Visualizador de árvores: <https://www.cs.usfca.edu/~galles/visualization/>
- Documentação da API Java: <https://docs.oracle.com/en/java/javase/21/docs/api/>

---
