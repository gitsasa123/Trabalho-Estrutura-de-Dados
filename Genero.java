/** Uma linha do genres.csv: genre_id,#tracks,parent,title,top_level */
public record Genero(int id, int numFaixas, int paiId, String titulo) {

    /** Converte uma linha do CSV em Genero. */
    public static Genero deLinhaCsv(String linha) {
        String[] c = linha.split(",", -1);
        return new Genero(
                Integer.parseInt(c[0].trim()),
                Integer.parseInt(c[1].trim()),
                Integer.parseInt(c[2].trim()),
                c[3].trim());
    }

    @Override
    public String toString() {
        return titulo + " (id=" + id + ", faixas=" + numFaixas + ")";
    }
}
