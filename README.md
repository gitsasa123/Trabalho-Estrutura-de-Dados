n# 🌳 Estruturas de Dados em Árvore: BST, AVL e Rubro-Negra

> Projeto da disciplina de **Estrutura de Dados** que implementa e compara árvores binárias de busca e árvores balanceadas usando conjuntos de dados reais.

![Linguagem](https://img.shields.io/badge/linguagem-Python-blue)
![Licença](https://img.shields.io/badge/licença-MIT-green)
![Status](https://img.shields.io/badge/status-em%20desenvolvimento-yellow)

---

## 📑 Sumário

- [Sobre o Projeto](#-sobre-o-projeto)
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

Descreva aqui, em 2 ou 3 parágrafos, o contexto do trabalho.

**Exemplo:** Este projeto implementa três estruturas de dados em árvore (Árvore Binária de Busca, AVL e Rubro-Negra) e as avalia em cenários práticos de inserção, busca e remoção, utilizando dados reais. O objetivo é observar como o balanceamento influencia o desempenho em diferentes tipos de entrada (ordenada, aleatória e parcialmente ordenada).

---

## 🎯 Objetivos

- Implementar uma **Árvore Binária de Busca (BST)**.
- Implementar **duas árvores balanceadas** (AVL e Rubro-Negra).
- Aplicar as estruturas em **conjuntos de dados reais**.
- Comparar **tempo de execução, altura e número de rotações**.
- Documentar as características de cada estrutura em cenários práticos.

---

## 🌲 Estruturas Implementadas

| Estrutura | Arquivo | Balanceamento | Complexidade (pior caso) |
|-----------|---------|---------------|--------------------------|
| Árvore Binária de Busca | `bst.py` | Não possui | O(n) |
| Árvore AVL | `avl.py` | Rigoroso (fator de balanceamento) | O(log n) |
| Árvore Rubro-Negra | `red_black.py` | Flexível (regras de cor) | O(log n) |

### Operações suportadas

- `inserir(chave)`
- `buscar(chave)`
- `remover(chave)`
- `altura()`
- `percurso_em_ordem()`, `percurso_pre_ordem()`, `percurso_pos_ordem()`
- `contar_rotacoes()` *(somente AVL e Rubro-Negra)*

---

## 📊 Conjuntos de Dados

| Dataset | Fonte | Nº de registros | Chave utilizada | Descrição |
|---------|-------|-----------------|-----------------|-----------|
| Exemplo A | [Nome da fonte](https://exemplo.com) | 10.000 | ID | Descrição breve do conjunto |
| Exemplo B | [Nome da fonte](https://exemplo.com) | 100.000 | CPF / Código | Descrição breve do conjunto |
| Exemplo C | [Nome da fonte](https://exemplo.com) | 500.000 | Data | Descrição breve do conjunto |

> **Sugestões de fontes:** Kaggle, Dados Abertos do Governo Federal, IBGE, UCI Machine Learning Repository.

Os arquivos originais ficam em `data/raw/` e as versões tratadas em `data/processed/`.

---

## 🗂 Estrutura do Repositório

```
.
├── README.md
├── LICENSE
├── requirements.txt
├── data/
│   ├── raw/                 # Dados originais
│   └── processed/           # Dados tratados
├── src/
│   ├── bst.py               # Árvore Binária de Busca
│   ├── avl.py               # Árvore AVL
│   ├── red_black.py         # Árvore Rubro-Negra
│   ├── node.py              # Classe(s) de nó
│   ├── data_loader.py       # Leitura e tratamento dos datasets
│   ├── benchmark.py         # Medição de desempenho
│   └── main.py              # Ponto de entrada
├── tests/
│   ├── test_bst.py
│   ├── test_avl.py
│   └── test_red_black.py
├── results/
│   ├── tabelas/             # Resultados em CSV
│   └── graficos/            # Gráficos gerados
└── docs/
    └── relatorio.pdf        # Relatório final (opcional)
```

---

## ⚙️ Requisitos

- Python 3.10 ou superior
- Bibliotecas listadas em `requirements.txt`:
  - `pandas`
  - `matplotlib`
  - `pytest`

---

## 🚀 Como Executar

```bash
# 1. Clonar o repositório
git clone https://github.com/usuario/nome-do-repositorio.git
cd nome-do-repositorio

# 2. (Opcional) Criar ambiente virtual
python -m venv venv
source venv/bin/activate      # Linux/Mac
venv\Scripts\activate         # Windows

# 3. Instalar dependências
pip install -r requirements.txt

# 4. Executar o programa
python src/main.py
```

### Parâmetros opcionais

```bash
python src/main.py --dataset data/processed/exemplo.csv --arvore avl
```

| Parâmetro | Descrição | Valores |
|-----------|-----------|---------|
| `--dataset` | Caminho do arquivo de dados | caminho `.csv` |
| `--arvore` | Estrutura a ser usada | `bst`, `avl`, `rb`, `todas` |

---

## 💡 Exemplo de Uso

```python
from src.avl import AVL

arvore = AVL()
for valor in [10, 20, 30, 40, 50, 25]:
    arvore.inserir(valor)

print(arvore.buscar(25))              # True
print(arvore.altura())                # 3
print(arvore.percurso_em_ordem())     # [10, 20, 25, 30, 40, 50]
```

---

## 🔬 Metodologia dos Experimentos

Descreva como os testes foram conduzidos.

**Cenários avaliados:**

1. **Inserção ordenada:** pior caso para a BST.
2. **Inserção aleatória:** caso médio.
3. **Busca de elementos existentes e inexistentes.**
4. **Remoção de elementos.**

**Métricas coletadas:**

- Tempo de execução (ms)
- Altura final da árvore
- Quantidade de rotações
- Uso de memória *(opcional)*

**Ambiente de teste:** *(exemplo)* Intel Core i5, 8 GB RAM, Python 3.12, Windows 11.

---

## 📈 Resultados

### Tabela comparativa *(valores fictícios)*

| Estrutura | Inserção (ms) | Busca (ms) | Remoção (ms) | Altura | Rotações |
|-----------|---------------|------------|--------------|--------|----------|
| BST | 000 | 000 | 000 | 000 | - |
| AVL | 000 | 000 | 000 | 000 | 000 |
| Rubro-Negra | 000 | 000 | 000 | 000 | 000 |

### Gráficos

![Comparação de tempo](results/graficos/tempo_insercao.png)
![Comparação de altura](results/graficos/altura.png)

---

## 🧠 Análise e Discussão

Use esta seção para interpretar os resultados. Sugestões de tópicos:

- Em quais cenários a BST apresentou degradação de desempenho?
- Qual árvore teve melhor desempenho em buscas? E em inserções?
- Quanto o número de rotações impactou o tempo total?
- Qual estrutura é mais adequada para cada tipo de aplicação real?
- Limitações do estudo e possíveis melhorias.

---

## ✅ Testes

```bash
pytest tests/
```

---

## 👥 Equipe

| Nome | GitHub | Responsabilidade |
|------|--------|------------------|
| Henrique Balassa | [@henriquebalassa](https://github.com/henriquebalassa) | BST e leitura de dados |
| Laura Veiga      | [@usuario2](https://github.com/usuario2) | AVL e benchmarks |
| Saulo Resende    | [@gitsasa123](https://github.com/gitsasa123) | Rubro-Negra e relatório |

**Disciplina:** Estrutura de Dados
**Professor(a):** Nome do Professor
**Instituição:** Nome da Instituição
**Semestre:** 2026/2

---

## 📚 Referências

- CORMEN, T. H. et al. **Algoritmos: Teoria e Prática**. 3. ed. Elsevier, 2012.
- SEDGEWICK, R.; WAYNE, K. **Algorithms**. 4. ed. Addison-Wesley, 2011.
- Visualizador de árvores: <https://www.cs.usfca.edu/~galles/visualization/>
- Documentação do Python: <https://docs.python.org/3/>

---

## 📄 Licença

Este projeto está licenciado sob a licença **MIT**. Consulte o arquivo [LICENSE](LICENSE) para mais detalhes.
