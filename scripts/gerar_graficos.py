"""
Gera os gráficos e a tabela-resumo a partir dos CSVs que o Benchmark.java grava.

Uso (na pasta do projeto):
    python scripts/gerar_graficos.py                      # usa results/tabelas -> results/graficos
    python scripts/gerar_graficos.py <pasta_tabelas> <pasta_graficos>

Dependências: pandas e matplotlib  (pip install pandas matplotlib)

Saídas:
    results/graficos/desempenho_tempo.png        tempo de cada operação nas 3 árvores
    results/graficos/desempenho_comparacoes.png  nº de comparações de cada operação
    results/graficos/altura.png                  altura x nº de elementos inseridos
    results/tabelas/resumo_para_readme.md        tabelas prontas para colar no README
"""
import sys
from pathlib import Path

import matplotlib

matplotlib.use("Agg")  # só grava arquivos, não abre janela
import matplotlib.pyplot as plt  # noqa: E402
import numpy as np  # noqa: E402
import pandas as pd  # noqa: E402
from matplotlib.patches import Patch  # noqa: E402
from matplotlib.ticker import FuncFormatter, NullFormatter  # noqa: E402

# ----------------------------------------------------------------------------- estilo
SUPERFICIE = "#fcfcfb"   # fundo
TEXTO = "#0b0b0b"
TEXTO_2 = "#52514e"
GRADE = "#e6e5e1"

ARVORES = ["BST", "AVL", "Rubro-Negra"]
# cores validadas para daltonismo; marcador e textura diferentes ajudam quem não distingue as cores
COR = {"BST": "#2a78d6", "AVL": "#eb6834", "Rubro-Negra": "#1baf7a"}
MARCADOR = {"BST": "o", "AVL": "s", "Rubro-Negra": "^"}
TEXTURA = {"BST": "", "AVL": "////", "Rubro-Negra": "xxxx"}

CENARIOS = [
    ("ORDENADO", "Inserção em ordem crescente de track_id (pior caso da BST)"),
    ("EMBARALHADO", "Inserção em ordem aleatória (caso médio)"),
]
OPERACOES = [
    ("INSERCAO", "Inserção"),
    ("BUSCA_EXISTENTE", "Busca\n(chave existente)"),
    ("BUSCA_INEXISTENTE", "Busca\n(chave inexistente)"),
    ("REMOCAO", "Remoção\n(metade das chaves)"),
]

plt.rcParams.update({
    "figure.facecolor": SUPERFICIE, "axes.facecolor": SUPERFICIE, "savefig.facecolor": SUPERFICIE,
    "text.color": TEXTO, "axes.labelcolor": TEXTO_2, "xtick.color": TEXTO_2, "ytick.color": TEXTO_2,
    "axes.edgecolor": GRADE, "axes.spines.top": False, "axes.spines.right": False,
    "axes.grid": True, "axes.axisbelow": True, "grid.color": GRADE, "grid.linewidth": 0.8,
    "hatch.linewidth": 0.8, "font.size": 10,
})


def br(v: float, casas: int = 0) -> str:
    """Formata número no padrão brasileiro: 1.234,5"""
    s = f"{v:,.{casas}f}"
    return s.replace(",", "X").replace(".", ",").replace("X", ".")


def formatar_eixo_log(valor: float, _pos=None) -> str:
    return br(valor, 1 if valor < 1 else 0)


def rotulo_barra(v: float) -> str:
    """Rótulo do gráfico de tempo (ms): 1,2 | 45 | 1.234"""
    if v >= 10:
        return br(v, 0)
    return br(v, 1)


def rotulo_compacto(v: float) -> str:
    """Rótulo do gráfico de comparações, curto para não encostar na barra vizinha: 734 mil | 1,3 mi | 1,25 bi"""
    if v >= 1e9:
        return br(v / 1e9, 2) + " bi"
    if v >= 1e6:
        return br(v / 1e6, 1) + " mi"
    if v >= 1e3:
        return br(v / 1e3, 0) + " mil"
    return br(v, 0)


# ----------------------------------------------------------------------------- gráficos de barras

def grafico_barras(df: pd.DataFrame, coluna: str, erro: str | None, titulo: str, subtitulo: str,
                   rotulo_y: str, arquivo: Path, formato_rotulo=rotulo_barra) -> None:
    fig, eixos = plt.subplots(1, 2, figsize=(14, 5.6), sharey=True)
    largura = 0.27
    x = np.arange(len(OPERACOES))

    todos = df[coluna][df[coluna] > 0]
    y_min, y_max = todos.min() / 3, todos.max() * 8

    for ax, (cenario, nome_cenario) in zip(eixos, CENARIOS):
        sub = df[df["cenario"] == cenario]
        for i, arv in enumerate(ARVORES):
            linhas = [sub[(sub["arvore"] == arv) & (sub["operacao"] == op)].iloc[0] for op, _ in OPERACOES]
            valores = np.array([l[coluna] for l in linhas], dtype=float)
            erros = np.array([l[erro] for l in linhas], dtype=float) if erro else None
            pos = x + (i - 1) * largura
            ax.bar(pos, valores, largura * 0.9, color=COR[arv], edgecolor=SUPERFICIE,
                   hatch=TEXTURA[arv], linewidth=0, zorder=3)
            if erros is not None:
                ax.errorbar(pos, valores, yerr=erros, fmt="none", ecolor=TEXTO_2, elinewidth=0.9,
                            capsize=2.5, zorder=4)
            for xi, v, e in zip(pos, valores, erros if erros is not None else np.zeros_like(valores)):
                ax.annotate(formato_rotulo(v), (xi, v + e), xytext=(0, 3), textcoords="offset points",
                            ha="center", va="bottom", fontsize=7, color=TEXTO_2)
        ax.set_yscale("log")
        ax.set_ylim(y_min, y_max)
        ax.yaxis.set_major_formatter(FuncFormatter(formatar_eixo_log))
        ax.yaxis.set_minor_formatter(NullFormatter())
        ax.set_xticks(x)
        ax.set_xticklabels([nome for _, nome in OPERACOES])
        ax.set_title(nome_cenario, loc="left", fontsize=10.5, color=TEXTO)
        ax.grid(axis="x", visible=False)
        ax.tick_params(length=0)
    eixos[0].set_ylabel(rotulo_y)

    legenda = [Patch(facecolor=COR[a], edgecolor=SUPERFICIE, hatch=TEXTURA[a], label=a) for a in ARVORES]
    fig.legend(handles=legenda, loc="upper right", ncol=3, frameon=False, bbox_to_anchor=(0.99, 0.985))
    fig.text(0.012, 0.965, titulo, fontsize=14, fontweight="bold", va="top", color=TEXTO)
    fig.text(0.012, 0.915, subtitulo, fontsize=9.5, va="top", color=TEXTO_2)
    fig.tight_layout(rect=(0, 0, 1, 0.88))
    fig.savefig(arquivo, dpi=150)
    plt.close(fig)


# ----------------------------------------------------------------------------- gráfico de altura

def rotulos_finais(ax, pontos) -> None:
    """Escreve 'nome: valor' no fim de cada linha, afastando rótulos que ficariam um em cima do outro."""
    para_eixo = ax.transData + ax.transAxes.inverted()
    itens = sorted((para_eixo.transform((x, y))[1], nome, y) for nome, x, y in pontos)
    ocupados = []
    for fy, nome, valor in itens:
        if ocupados and fy - ocupados[-1] < 0.075:
            fy = ocupados[-1] + 0.075
        ocupados.append(fy)
        ax.plot([1.015], [fy], marker=MARCADOR[nome], color=COR[nome], markersize=6,
                transform=ax.transAxes, clip_on=False, linestyle="none")
        ax.text(1.04, fy, f"{nome}: {br(valor)}", transform=ax.transAxes, va="center", fontsize=8.5, color=TEXTO)


def grafico_altura(df: pd.DataFrame, arquivo: Path) -> None:
    fig, eixos = plt.subplots(1, 2, figsize=(14, 5.6))
    for ax, (cenario, nome_cenario) in zip(eixos, CENARIOS):
        sub = df[df["cenario"] == cenario]
        n_max = sub["elementos"].max()
        pontos = []
        for arv in ARVORES:
            s = sub[sub["arvore"] == arv].sort_values("elementos")
            ax.plot(s["elementos"], s["altura"], color=COR[arv], linewidth=2, marker=MARCADOR[arv],
                    markevery=max(1, len(s) // 7), markersize=6.5, markeredgecolor=SUPERFICIE,
                    markeredgewidth=1.2, zorder=3)
            pontos.append((arv, s["elementos"].iloc[-1], s["altura"].iloc[-1]))

        n = np.linspace(1, n_max, 300)
        ax.plot(n, np.log2(n + 1), color=TEXTO_2, linestyle=(0, (4, 3)), linewidth=1.2, zorder=2)

        ax.set_xlim(0, n_max)
        if cenario == "ORDENADO":
            ax.set_yscale("log")
            ax.set_ylim(1, sub["altura"].max() * 1.6)
            ax.yaxis.set_major_formatter(FuncFormatter(formatar_eixo_log))
            ax.yaxis.set_minor_formatter(NullFormatter())
            ax.set_ylabel("Altura da árvore (nº de níveis) — escala log")
        else:
            ax.set_ylim(0, sub["altura"].max() * 1.2)
            ax.set_ylabel("Altura da árvore (nº de níveis)")
        ax.xaxis.set_major_formatter(FuncFormatter(lambda v, _p: br(v)))
        ax.set_xlabel("Elementos inseridos")
        ax.set_title(nome_cenario, loc="left", fontsize=10.5, color=TEXTO)
        ax.tick_params(length=0)
        ax.annotate("log₂(n+1): altura mínima possível", xy=(n_max * 0.5, np.log2(n_max * 0.5 + 1)),
                    xytext=(0, -14), textcoords="offset points", fontsize=8, color=TEXTO_2, ha="center")
        rotulos_finais(ax, pontos)

    legenda = [plt.Line2D([0], [0], color=COR[a], marker=MARCADOR[a], linewidth=2, markersize=6.5,
                          markeredgecolor=SUPERFICIE, label=a) for a in ARVORES]
    legenda.append(plt.Line2D([0], [0], color=TEXTO_2, linestyle=(0, (4, 3)), linewidth=1.2,
                              label="log₂(n+1)"))
    fig.legend(handles=legenda, loc="upper right", ncol=4, frameon=False, bbox_to_anchor=(0.99, 0.985))
    n_total = df["elementos"].max()
    fig.text(0.012, 0.965, "Balanceamento: altura × elementos inseridos", fontsize=14, fontweight="bold",
             va="top", color=TEXTO)
    fig.text(0.012, 0.915, f"Altura medida durante a inserção incremental de até {br(n_total)} faixas do FMA",
             fontsize=9.5, va="top", color=TEXTO_2)
    fig.tight_layout(rect=(0, 0, 1, 0.88))
    fig.subplots_adjust(wspace=0.5, right=0.88)
    fig.savefig(arquivo, dpi=150)
    plt.close(fig)


# ----------------------------------------------------------------------------- tabela-resumo

def escrever_resumo(df: pd.DataFrame, arquivo: Path) -> None:
    linhas = []
    for cenario, nome_cenario in CENARIOS:
        sub = df[df["cenario"] == cenario]
        n = int(sub["n"].iloc[0])
        linhas.append(f"### {nome_cenario} — N = {br(n)}\n")
        linhas.append("| Estrutura | Inserção (ms) | Busca existente (ms) | Busca inexistente (ms) | Remoção (ms) "
                      "| Comparações (inserção) | Altura após inserir | Altura após remover |")
        linhas.append("|---|---|---|---|---|---|---|---|")
        for arv in ARVORES:
            def pega(op, col):
                return sub[(sub["arvore"] == arv) & (sub["operacao"] == op)].iloc[0][col]
            linhas.append(
                f"| {arv} | {br(pega('INSERCAO', 'tempo_ms_medio'), 1)} "
                f"| {br(pega('BUSCA_EXISTENTE', 'tempo_ms_medio'), 1)} "
                f"| {br(pega('BUSCA_INEXISTENTE', 'tempo_ms_medio'), 1)} "
                f"| {br(pega('REMOCAO', 'tempo_ms_medio'), 1)} "
                f"| {br(pega('INSERCAO', 'comparacoes'))} "
                f"| {br(pega('INSERCAO', 'altura'))} "
                f"| {br(pega('REMOCAO', 'altura'))} |")
        linhas.append("")
    arquivo.write_text("\n".join(linhas), encoding="utf-8")


def main() -> None:
    raiz = Path(__file__).resolve().parent.parent / "results"
    pasta_tabelas = Path(sys.argv[1]) if len(sys.argv) > 1 else raiz / "tabelas"
    pasta_graficos = Path(sys.argv[2]) if len(sys.argv) > 2 else raiz / "graficos"
    pasta_graficos.mkdir(parents=True, exist_ok=True)

    arq_desempenho = pasta_tabelas / "desempenho.csv"
    arq_altura = pasta_tabelas / "altura_incremental.csv"
    for a in (arq_desempenho, arq_altura):
        if not a.exists():
            sys.exit(f"Arquivo não encontrado: {a}\nRode antes: java -cp out arvores.Benchmark")

    desempenho = pd.read_csv(arq_desempenho)
    altura = pd.read_csv(arq_altura)
    n = int(desempenho["n"].iloc[0])

    grafico_barras(
        desempenho, "tempo_ms_medio", "tempo_ms_desvio",
        "Desempenho: tempo de cada operação",
        f"N = {br(n)} faixas do FMA · média de várias repetições (barras de erro = desvio padrão) · escala log",
        "Tempo total da operação (ms) — escala log", pasta_graficos / "desempenho_tempo.png")
    grafico_barras(
        desempenho, "comparacoes", None,
        "Desempenho: número de comparações entre chaves",
        f"N = {br(n)} faixas do FMA · total de comparações em cada operação · escala log",
        "Comparações (total) — escala log", pasta_graficos / "desempenho_comparacoes.png",
        formato_rotulo=rotulo_compacto)
    grafico_altura(altura, pasta_graficos / "altura.png")
    escrever_resumo(desempenho, pasta_tabelas / "resumo_para_readme.md")

    print("Gráficos gravados em", pasta_graficos)
    print("Tabela para o README em", pasta_tabelas / "resumo_para_readme.md")


if __name__ == "__main__":
    main()
