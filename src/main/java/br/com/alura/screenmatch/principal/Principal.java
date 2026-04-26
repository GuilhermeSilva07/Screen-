package br.com.alura.screenmatch.principal;

import br.com.alura.screenmatch.model.*;
import br.com.alura.screenmatch.repository.SerieRepository;
import br.com.alura.screenmatch.service.ConsumoApi;
import br.com.alura.screenmatch.service.ConverteDados;
import br.com.alura.screenmatch.service.traducao.ConsultaMyMemory;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

public class Principal {

    private Scanner leitura = new Scanner(System.in);
    private ConsumoApi consumo = new ConsumoApi();
    private ConverteDados conversor = new ConverteDados();
    private final String ENDERECO = "https://www.omdbapi.com/?t=";
    private final String API_KEY = "&apikey=6585022c";

    private List<DadosSerie> dadosSeries = new ArrayList<>();

    private SerieRepository repository;

    private List<Serie> series = new ArrayList<>();

    private Optional<Serie> serieBusca;

    public Principal(SerieRepository repository) {
        this.repository = repository;
    }

    public void exibeMenu() {
        var opcao = -1;
        while(opcao != 0){
            var menu = """
                    1 - Buscar séries
                    2 - Buscar episódios
                    3 - Listar series buscadas
                    4 - Buscar serie por titulo
                    5 - Buscar series por ator
                    6 - Top 5 Series
                    7 - Buscar series por categoria
                    8 - Buscar series por Temporadas e Avaliação
                    9 - Episodios por trecho
                    10 - Top 5 episodios pro serie
                    11 - Busca por data
                    
                    0 - Sair                                
                    """;

            System.out.println(menu);
            opcao = leitura.nextInt();
            leitura.nextLine();

            switch (opcao) {
                case 1:
                    buscarSerieWeb();
                    break;
                case 2:
                    buscarEpisodioPorSerie();
                    break;
                case 3:
                    listarSeriesBuscadas();
                    break;
                case 4:
                    buscarSeriePorTitulo();
                    break;
                case 5:
                    buscarSeriesPorAtor();
                    break;
                case 6:
                    buscarTopSeries();
                    break;
                case 7:
                    buscarSeriesPorCategoria();
                    break;
                case 8:
                    buscarSeriesPorTemporadasEAvaliacao();
                    break;
                case 9:
                    buscarEpisodioPorTrecho();
                    break;
                case 10:
                    topEpisodiosPorSerie();
                    break;
                case 11:
                    buscaPòrData();
                    break;
                case 0:
                    System.out.println("Saindo...");
                    break;
                default:
                    System.out.println("Opção inválida");
            }
        }
    }


    private void buscarSerieWeb() {
        DadosSerie dados = getDadosSerie();
        Serie serie = new Serie(dados);
        //dadosSeries.add(dados);
        repository.save(serie);
        System.out.println(dados);
    }

    private DadosSerie getDadosSerie() {
        System.out.println("Digite o nome da série para busca");
        var nomeSerie = leitura.nextLine();
        var json = consumo.obterDados(ENDERECO + nomeSerie.replace(" ", "+") + API_KEY);
        DadosSerie dados = conversor.obterDados(json, DadosSerie.class);
        return dados;
    }

    private void buscarEpisodioPorSerie(){

        listarSeriesBuscadas();
        System.out.println("Escolha uma serie pelo nome: ");
        var nomeSerie = leitura.nextLine();

        Optional<Serie> serie = repository.findByTituloContainingIgnoreCase(nomeSerie);  //buscando direto do banco

        if(serie.isPresent()){

            var serieEncontrada = serie.get();
            List<DadosTemporada> temporadas = new ArrayList<>();

            for (int i = 1; i <= serieEncontrada.getTotalTemporadas(); i++) {
                var json = consumo.obterDados(ENDERECO + serieEncontrada.getTitulo().replace(" ", "+") + "&season=" + i + API_KEY);
                DadosTemporada dadosTemporada = conversor.obterDados(json, DadosTemporada.class);
                temporadas.add(dadosTemporada);
            }
            temporadas.forEach(System.out::println);

            List<Episodio> episodios = temporadas.stream()
                    .flatMap(d -> d.episodios().stream()
                            .map(e -> new Episodio(d.numero(), e)))
                    .collect(Collectors.toList());
            serieEncontrada.setEpisodios(episodios);
            repository.save(serieEncontrada);

            }else {
                System.out.println("serie não encontrada.");
            }
    }

    private void listarSeriesBuscadas(){
        series = series = repository.findAll();         // <- Buscando diretamente do banco
        series.stream()
                .sorted(Comparator.comparing(Serie::getGenero))
                .forEach(System.out::println);
    }

    private void buscarSeriePorTitulo() {
        System.out.println("Escolha uma serie pelo nome: ");
        var nomeSerie = leitura.nextLine();
        serieBusca = repository.findByTituloContainingIgnoreCase(nomeSerie);

        if (serieBusca.isPresent()){
            System.out.println("Dados da serie: " + serieBusca.get());
        }else {
            System.out.println("Serie não encontrada!");
        }
    }


    private void buscarSeriesPorAtor(){
        System.out.println("Qual o nome para busca?");
        var nomeAtor = leitura.nextLine();
        System.out.println("Avaliaçoes a partir de qual valor? ");
        var avalicao = leitura.nextDouble();
        List<Serie> seriesEncontradas = repository.findByAtoresContainingIgnoreCaseAndAvaliacaoGreaterThanEqual(nomeAtor, avalicao);
        System.out.println("Series em que " + nomeAtor + " teve participação ");
        seriesEncontradas.forEach(s ->
                System.out.println(s.getTitulo() + " avaliação: " + s.getAvaliacao()));
    }

    private void buscarTopSeries(){
        List<Serie> serieTop = repository.findTop5ByOrderByAvaliacaoDesc();
        serieTop.forEach(s ->
                System.out.println(s.getTitulo() + " avaliação: " + s.getAvaliacao()));
    }


    private void buscarSeriesPorCategoria(){
        System.out.println("Deseja buscar series de que categoria/genero? ");
        var nomeGenero = leitura.nextLine();
        Categoria categoria = Categoria.fromPortugues(nomeGenero);
        List<Serie> seriesPorCategoria = repository.findBygenero(categoria);
        System.out.println("Series da categoria " + nomeGenero);
        seriesPorCategoria.forEach(System.out::println);
    }

    private void buscarSeriesPorTemporadasEAvaliacao(){
        System.out.println("Até quantas temporadas ?");
        var totalTemporadas = leitura.nextInt();
        System.out.println("Com avaliação a apartir de qual valor? ");
        var avaliacao = leitura.nextDouble();
        leitura.nextLine();


        List<Serie> series = repository.seriesPorTemporadaEAvaliacao(totalTemporadas, avaliacao);

        series.forEach(s -> System.out.println(s.getTitulo()
                + " | Temporadas: " + s.getTotalTemporadas()
                + " | Avaliação: " + s.getAvaliacao()));
    }

    private void buscarEpisodioPorTrecho(){
        System.out.println("Qual o nome do episodio para busca ?");
        var nomeEpisodio = leitura.nextLine();

        List<Episodio> episodiosEncontrados = repository.episodiosPorTrecho(nomeEpisodio);
        episodiosEncontrados.forEach(e ->
                System.out.printf("Serie: %s Temporada %s - Episodio %s - %s\n",
                        e.getSerie().getTitulo(), e.getTemporada(),
                        e.getNumeroEpisodio(), e.getTitulo()));

    }

    private void topEpisodiosPorSerie() {
        buscarSeriePorTitulo();
        if (serieBusca.isPresent()){
            Serie serie = serieBusca.get();
            List<Episodio> topEpisodios = repository.topEpisodiosPorSerie(serie);
            System.out.println("Total encontrado: " + topEpisodios.size());
            topEpisodios.forEach(e ->
                    System.out.printf("Serie: %s Temporada %s - Episodio %s - %s\n",
                            e.getSerie().getTitulo(), e.getTemporada(),
                            e.getNumeroEpisodio(), e.getTitulo(), e.getAvaliacao()));
        }
    }

    private void buscaPòrData(){
        buscarSeriePorTitulo();
        if(serieBusca.isPresent()){
            Serie serie = serieBusca.get();
            System.out.println("Digite o ano limite de lançamento");
            var anoLancamento = leitura.nextInt();
            leitura.nextLine();

            List<Episodio> episodiosAno = repository.episodiosPorSerieEAno(serie, LocalDate.of(anoLancamento, 1, 1) );
            episodiosAno.forEach(System.out::println);
        }
    }
}