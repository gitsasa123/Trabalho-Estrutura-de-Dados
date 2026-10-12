"""
Converte o tracks.csv do Free Music Archive (cabeçalho de várias linhas) em um CSV
simples (data/faixas.csv) que o Java lê.

Uso:
    python scripts/preparar_dados.py data/fma_metadata/tracks.csv data/faixas.csv

Dependência: pandas (pip install pandas)

O que o script faz:
  1. lê o tracks.csv (as colunas vêm agrupadas, ex.: ('track', 'title'));
  2. escolhe as colunas que usamos e dá nomes simples;
  3. descarta faixas sem título, artista, nº de ouvintes ou duração
     (faixas sem gênero principal ficam com o gênero "Desconhecido");
  4. garante que o track_id (a chave das árvores) é único e ordena em ordem crescente.
"""
import sys

import pandas as pd

# nome na saída -> coluna do tracks.csv depois de juntar o cabeçalho com "_"
# Se o script reclamar de coluna faltando, ele mostra as colunas que existem: ajuste aqui.
COLUNAS = {
    "titulo": "track_title",
    "artista": "artist_name",
    "genero": "track_genre_top",
    "ouvintes": "track_listens",
    "duracao": "track_duration",
}


def main(entrada: str, saida: str) -> None:
    df = pd.read_csv(entrada, index_col=0, header=[0, 1], low_memory=False)
    df.columns = ["_".join(c) for c in df.columns]
    print(f"{len(df)} linhas lidas de {entrada}")

    faltando = [c for c in COLUNAS.values() if c not in df.columns]
    if faltando:
        print("Colunas não encontradas:", faltando)
        print("Colunas disponíveis:", list(df.columns))
        sys.exit(1)

    df = df[list(COLUNAS.values())].rename(columns={v: k for k, v in COLUNAS.items()})

    # gênero principal ausente não é motivo para descartar a faixa
    df["genero"] = df["genero"].fillna("Desconhecido")
    antes = len(df)
    df = df.dropna()
    print(f"{antes - len(df)} faixas descartadas por campos vazios")

    df = df[~df.index.duplicated(keep="first")]  # garante chave única

    df["ouvintes"] = df["ouvintes"].astype("int64")
    df["duracao"] = df["duracao"].astype(float).round().astype("int32")
    for col in ("titulo", "artista", "genero"):  # sem quebra de linha dentro do campo
        df[col] = df[col].astype(str).str.replace(r"[\r\n]+", " ", regex=True)

    df.index = df.index.astype("int64")   # track_id como número (senão "10" < "2" na ordenação)
    df = df.sort_index()                  # ordem crescente de track_id
    df.to_csv(saida, index_label="chave", encoding="utf-8")

    print(f"{len(df)} faixas gravadas em {saida}")
    print(f"track_id únicos: {df.index.nunique()} (de {df.index.min()} a {df.index.max()})")
    if len(df) < 10_000:
        print("ATENÇÃO: o enunciado exige no mínimo 10.000 registros!")


if __name__ == "__main__":
    if len(sys.argv) != 3:
        print(__doc__)
        sys.exit(1)
    main(sys.argv[1], sys.argv[2])
