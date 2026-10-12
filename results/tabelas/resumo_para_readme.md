### Inserção em ordem crescente de track_id (pior caso da BST) — N = 50.000

| Estrutura | Inserção (ms) | Busca existente (ms) | Busca inexistente (ms) | Remoção (ms) | Comparações (inserção) | Altura após inserir | Altura após remover |
|---|---|---|---|---|---|---|---|
| BST | 4.047,7 | 4.215,8 | 7.115,1 | 1.726,9 | 1.249.975.000 | 50.000 | 25.000 |
| AVL | 10,2 | 20,0 | 7,0 | 13,1 | 734.465 | 16 | 16 |
| Rubro-Negra | 9,9 | 19,5 | 8,7 | 11,4 | 1.286.225 | 29 | 17 |

### Inserção em ordem aleatória (caso médio) — N = 50.000

| Estrutura | Inserção (ms) | Busca existente (ms) | Busca inexistente (ms) | Remoção (ms) | Comparações (inserção) | Altura após inserir | Altura após remover |
|---|---|---|---|---|---|---|---|
| BST | 25,6 | 22,4 | 7,0 | 11,7 | 962.667 | 38 | 34 |
| AVL | 28,0 | 18,6 | 7,2 | 14,3 | 720.039 | 18 | 17 |
| Rubro-Negra | 22,7 | 18,9 | 7,0 | 11,9 | 725.785 | 19 | 18 |
