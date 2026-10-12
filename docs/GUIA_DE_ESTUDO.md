# Guia de estudo — para explicar o código na apresentação

O enunciado diz que **cada integrante precisa saber explicar qualquer trecho do código**. Este guia
é um atalho para isso: o que cada arquivo faz, a ideia de cada árvore e as perguntas que o professor
provavelmente vai fazer. Todos os arquivos de código têm comentários em português explicando o porquê.

## 1. O projeto em 30 segundos

Implementamos três árvores de busca em Java (BST, AVL e Rubro-Negra) com a mesma interface
(`Arvore`). Carregamos faixas reais do Free Music Archive (chave = `track_id`) e comparamos
**tempo, número de comparações e altura** em dois cenários: inserindo as chaves **em ordem crescente**
(pior caso da BST) e **em ordem aleatória** (caso médio). A conclusão esperada: a BST é rápida no caso
médio, mas degrada para O(n) quando as chaves chegam ordenadas; AVL e Rubro-Negra mantêm altura O(log n)
sempre.

## 2. Mapa do código (leia nesta ordem)

| Ordem | Arquivo | O que faz |
|---|---|---|
| 1 | `Arvore.java` | A "interface": a lista de operações que toda árvore tem (inserir, buscar, remover, altura, percorrer, contador). |
| 2 | `BST.java` | Árvore binária de busca simples. Tudo iterativo (com `while`), porque em ordem crescente a altura chega a ~100 mil e a recursão estouraria a pilha. |
| 3 | `AVL.java` | BST + altura guardada em cada nó + rotações quando o fator de balanceamento sai de {-1, 0, +1}. |
| 4 | `RubroNegra.java` | BST + cores + 5 regras. Conserto por recoloração e rotações (inserção: 3 casos; remoção: 4 casos). Baseada no Cormen, cap. 13. |
| 5 | `CarregadorFaixas.java` e `Faixa.java` | Lê o CSV tratado e entrega pares (chave, dados). |
| 6 | `TesteArvore.java` | Testes: compara cada árvore com o `TreeMap` do Java e chama `validar()` depois de cada operação. |
| 7 | `Benchmark.java` | Mede tempo, comparações e altura e grava CSVs em `results/tabelas`. |
| 8 | `scripts/gerar_graficos.py` | Lê os CSVs e desenha os gráficos. |
| 9 | `scripts/preparar_dados.py` | Converte o `tracks.csv` do FMA (cabeçalho de várias linhas) em `data/faixas.csv`. |
| 10 | `Main.java` | Demo interativa para a apresentação. |

## 3. As ideias que você precisa dominar

**BST.** Regra: tudo à esquerda de um nó é menor, tudo à direita é maior. Buscar/inserir = descer
comparando, no máximo "altura" comparações. Se as chaves chegam ordenadas, cada uma vai para a direita
da anterior e a árvore vira uma lista: altura = n, custo O(n) por operação.

**AVL.** Em todo nó, |altura(esq) − altura(dir)| ≤ 1. Quando uma inserção ou remoção quebra isso,
rotacionamos: *simples* (esquerda-esquerda ou direita-direita) ou *dupla* (esquerda-direita ou
direita-esquerda). A rotação não muda a ordem das chaves, só a forma. Altura ≤ ~1,44·log₂(n). Código:
`balancear()` e `rotacaoDireita/rotacaoEsquerda`. A remoção também rebalanceia, subindo pelo caminho
(a recursão faz isso "na volta").

**Rubro-Negra.** Cada nó é vermelho ou preto. (1) raiz preta; (2) NIL preto; (3) vermelho não tem filho
vermelho; (4) todos os caminhos até as folhas têm o mesmo número de nós pretos. Isso garante que o
caminho mais longo é no máximo 2× o mais curto: altura ≤ 2·log₂(n+1). Na inserção, o nó entra
vermelho e olhamos o **tio**: tio vermelho → recolore e sobe; tio preto → 1 ou 2 rotações. Na remoção,
se sai um nó preto sobra um "preto extra" que o `consertarRemocao` resolve olhando o **irmão**.

**AVL × Rubro-Negra.** A AVL é mais rígida: altura menor, buscas um pouco mais rápidas, mas faz mais
rotações. A Rubro-Negra tolera mais desbalanceamento: altura um pouco maior, mas inserções e remoções
são mais baratas (poucas rotações). Por isso muitas bibliotecas usam Rubro-Negra (o `TreeMap` do Java é
uma).

## 4. Perguntas prováveis e respostas curtas

- **Por que a BST ficou tão lenta no cenário ordenado?** Cada chave nova é maior que todas, então vai
  sempre para a direita: a árvore vira uma lista e cada inserção/busca percorre n nós (O(n²) no total).
- **Por que a chave é o `track_id`?** É um inteiro único, comparável e já vem em ordem crescente, o que
  nos dá o pior caso da BST de graça; embaralhando, temos o caso médio.
- **Como contamos as comparações?** Toda comparação de chaves passa por um único método `comparar()`
  que incrementa o contador; nenhuma escapa. Zeramos o contador antes de cada medição.
- **Como vocês sabem que as árvores estão certas?** `TesteArvore` roda dezenas de milhares de operações
  aleatórias (inserir/remover/buscar) comparando com o `TreeMap` do Java e chama `validar()` depois de
  cada uma, que confere ordem das chaves, balanceamento/cores e o tamanho. Também testamos introduzindo
  erros de propósito, e os testes falham.
- **Por que a BST é iterativa e a AVL/Rubro-Negra são recursivas?** Altura da BST pode chegar a 100 mil
  (estouraria a pilha); a das balanceadas é ~20, então a recursão é segura e o código fica mais claro.
- **O que acontece ao remover um nó com dois filhos?** Troca-se pelo **sucessor** (menor chave da
  subárvore direita) e remove-se o nó do sucessor, que tem no máximo um filho.
- **E chave repetida na inserção?** Os dados são substituídos e o tamanho não muda (convenção do grupo).
- **Por que o tempo da AVL/Rubro-Negra no caso aleatório pode ser parecido ou até maior que o da BST?**
  Elas pagam o custo de rebalancear (rotações, recolorações); a BST só ganha dessa vantagem enquanto as
  chaves chegam "bagunçadas". O benefício das balanceadas é a **garantia** de O(log n).
- **Por que escala logarítmica nos gráficos?** A BST ordenada é ordens de magnitude pior; em escala
  linear as outras barras desapareceriam.
- **Limitações do estudo?** Tempo depende da máquina e da JVM (usamos aquecimento e média de várias
  repetições); não medimos memória; chaves inteiras "encaixotadas" (`Integer`); um único dataset.

## 5. Roteiro de estudo (≈ 3 sessões)

1. **BST (30 min).** Leia `BST.java`. Desenhe no papel: insira 50, 30, 70, 20, 40 e depois remova o 30
   (nó com 2 filhos). Rode no visualizador https://www.cs.usfca.edu/~galles/visualization/BST.html
   e confira.
2. **AVL (45 min).** Leia `AVL.java`. No visualizador (AVL Trees), insira 10, 20, 30, 40, 50, 25 e veja
   as 4 rotações. Repita no papel e identifique no código qual trecho faz cada rotação.
3. **Rubro-Negra (60 min).** Leia os comentários de `consertarInsercao` e `consertarRemocao`. No
   visualizador (Red-Black Trees), insira 1…8 e remova alguns. Você não precisa decorar os 4 casos da
   remoção: precisa saber explicar a ideia (preto extra, olhar o irmão) e mostrar onde fica no código.
4. **Resultados (30 min).** Depois de rodar o benchmark, olhe `desempenho_tempo.png`, `altura.png` e
   prepare 2 frases por gráfico: o que se vê e por quê.

## 6. Roteiro da demonstração ao vivo (`Main`)

```
java -cp out arvores.Main data/faixas.csv 20000 embaralhado
```
1. Mostre a tabela inicial: tamanho e **altura** das 3 árvores (BST ~30, AVL ~17, Rubro-Negra ~18).
2. Opção 1: busque um `track_id` e mostre as **comparações** de cada árvore.
3. Opção 2 e 3: insira e remova uma faixa; mostre que a altura se mantém.
4. Opção 5: mostre os percursos pré, em e pós-ordem (o em-ordem sai crescente).
5. Opção 6: valide as estruturas.
6. Feche com o pior caso: `java -cp out arvores.Main data/faixas.csv 20000 ordenado` e compare a altura
   da BST (20.000) com a da AVL (~15).
