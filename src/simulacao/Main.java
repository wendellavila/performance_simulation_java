/*
Autor: Wendell Joao Castro de Avila
RA: 2017.1.08.013
*/

package simulacao;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.PriorityQueue;
import java.util.Random;
import java.util.Scanner;

public class Main {
    
    public static Random rand = new Random();
    
    //Retorna um valor contido no intervalo entre (0,1]
    public static double aleatorio() {
        //resultado sera algo entre [0,0.999999...] proximo a 1.0
        double u = rand.nextDouble();
        //limitando entre (0,1]
        return (1.0 - u);
    }

    public static double minimo(double n1, double n2){
        if(n1 < n2)
            return n1;
        return n2;
    }

    //funcao usada para encontrar os tempos de parada para gerar as informacoes parciais
    public static double proximoMultiploDeN(double valorAtual, double n){
        //remover decimais
        n = Math.floor(n);
        return valorAtual - (valorAtual % n) + n;
    }
    
    /**
    * Simulador de um caixa onde clientes chegam e sao atendidos no modelo FIFO.
    * O tempo entre a chegada de clientes, bem como o tempo de atendimento devem 
    * ser gerados de maneira pseudoaleatoria atraves da v.a. exponencial.
    *  
    * Utilizacao ou Ocupacao = fracao de tempo que o caixa permanecera ocupado.
    * Little: E[N] = \lambda * E[w]
    */
    public static void main(String[] args) throws IOException {
        //semente constante
        rand.setSeed(2017108013);
        
        Scanner sc = new Scanner(System.in);

        System.out.println("Informe a quantidade de caixas: ");
        int qtd_caixas = Integer.parseInt(sc.nextLine());
        
        System.out.println("Informe a relação entre a chegada e a capacidade total de atendimento: ");
        double relacao = Double.parseDouble(sc.nextLine());

        System.out.println("Informe o tempo total de simulacao (segundos): ");
        double tempo_simulacao = Double.parseDouble(sc.nextLine());
        
        // 1 / 1 minuto
        double tempo_medio_atendimento = 1.0 / 60.0;
        // 1 / (1 minuto * quantidade de caixas * relacao)
        double tempo_medio_clientes = 1.0 / ((double) qtd_caixas  * relacao * 60.0);
        
        //tempo decorrido da simulacao
        double tempo = 0.0;

        //armazena o tempo de chegada do proximo cliente
        double chegada_cliente = (-1.0 / tempo_medio_clientes) * Math.log(aleatorio());

        //fila ordenada que armazena os tempos em que os clientes
        //que estiverem em atendimento sairao do comercio
        //apos atender um cliente, seu tempo de saida e removido da fila
        //a quantidade de itens em saida_atendimento_caixas
        //diz a quantidade de caixas ocupados, que nao pode exceder qtd_caixas
        PriorityQueue<Double> saida_atendimento_caixas = new PriorityQueue<>();

        double fila = 0.0;
        
        Info en = new Info();
        Info ewEntrada = new Info();
        Info ewSaida = new Info();
        
        //arquivo com valores parciais a serem utilizados para plot de gráficos.
        File file = new File("dados_grafico.csv");
        FileWriter write = new FileWriter(file);
        PrintWriter print = new PrintWriter(write);
        print.println("Tempo,Lambda,En,Ew");

        double intervaloGraficos = 600.0;
        
        //logica da simulacao
        while(tempo <= tempo_simulacao){
            
            double proxima_saida_atendimento = 0.0;
            //System.out.println("===================================");
            //System.out.println("Tempo: " + tempo);
            
            //todos os caixas estao ociosos
            //a simulacao pode avancar no tempo para
            //a chegada do proximo cliente
            if(saida_atendimento_caixas.size() == 0){
                tempo = minimo(chegada_cliente, proximoMultiploDeN(tempo, intervaloGraficos));
            }
            //existem caixas ocupados
            else {
                proxima_saida_atendimento = saida_atendimento_caixas.peek();
                tempo = minimo(minimo(chegada_cliente, proxima_saida_atendimento), proximoMultiploDeN(tempo, intervaloGraficos));
            }

            if(tempo == chegada_cliente){
                //evento de chegada de cliente
                fila++;
                //System.out.println("Chegada de cliente: " + chegada_cliente + ", fila: " + fila);

                //se existir caixa ocioso, pode-se comecar a
                //atender o cliente que acaba de chegar
                if(saida_atendimento_caixas.size() < qtd_caixas){
                    proxima_saida_atendimento = tempo;
                    saida_atendimento_caixas.add(proxima_saida_atendimento);
                }

                //gerar o tempo de chegada do proximo cliente
                chegada_cliente = tempo + (-1.0 / tempo_medio_clientes) * Math.log(aleatorio());

                //calculo do E[N]
                en.somaAreas += en.numeroEventos * (tempo - en.tempoAnterior);
                en.tempoAnterior = tempo;
                en.numeroEventos++;

                //calculo do E[W]
                ewEntrada.somaAreas += ewEntrada.numeroEventos * (tempo - ewEntrada.tempoAnterior);
                ewEntrada.tempoAnterior = tempo;
                ewEntrada.numeroEventos++;
            }
            else if(tempo == proxima_saida_atendimento){
                //evento executado se houver saida de cliente
                //ou ainda se houver chegada de cliente, mas
                //o caixa estiver ocioso.
                //a cabeca da fila nao consiste no cliente em atendimento.
                //o cliente que comeca a ser atendido portanto, sai da fila,
                //e passa a estar ainda no comercio, mas em atendimento no caixa.

                //verifica se ha cliente na fila
                if(fila > 0.0){
                    //atendimento de cliente
                    fila--;
                    double tempo_atendimento = (-1.0 / tempo_medio_atendimento) * Math.log(aleatorio());
                    proxima_saida_atendimento = tempo + tempo_atendimento;
                    saida_atendimento_caixas.add(proxima_saida_atendimento);
                    //System.out.println("Saida de atendimento: " + proxima_saida_atendimento + ", fila: " + fila);
                } else {
                    //chegada de cliente com caixa ocioso
                    saida_atendimento_caixas.remove();
                }

                if(en.tempoAnterior < tempo){
                    //calculo do E[N]
                    en.somaAreas += en.numeroEventos * (tempo - en.tempoAnterior);
                    en.tempoAnterior = tempo;
                    en.numeroEventos--;

                    //calculo do E[W]
                    ewSaida.somaAreas += ewSaida.numeroEventos * (tempo - ewSaida.tempoAnterior);
                    ewSaida.tempoAnterior = tempo;
                    ewSaida.numeroEventos++;
                }
            }
            else {
                //printar informacoes parciais em um arquivo csv
                double enParcial = en.somaAreas / tempo;
                //fazendo o calculo da ultima area dos graficos antes do momento atual
                double ewSaidaParcial = ewSaida.somaAreas + ewSaida.numeroEventos * (tempo - ewSaida.tempoAnterior);
                double ewEntradaParcial = ewEntrada.somaAreas + ewEntrada.numeroEventos * (tempo - ewEntrada.tempoAnterior);
                double ewParcial = (ewEntradaParcial - ewSaidaParcial) / (double) ewEntrada.numeroEventos;
                double lambdaParcial = ewEntrada.numeroEventos / tempo;
                print.println(tempo + "," + lambdaParcial + "," + enParcial + "," + ewParcial);
            }
        }

        //fazendo o calculo da ultima area dos graficos antes do termino da simulacao
        ewSaida.somaAreas += ewSaida.numeroEventos * (tempo - ewSaida.tempoAnterior);
        ewEntrada.somaAreas += ewEntrada.numeroEventos * (tempo - ewEntrada.tempoAnterior); 

        double enF = en.somaAreas / tempo;
        double ew = (ewEntrada.somaAreas - ewSaida.somaAreas) / (double) ewEntrada.numeroEventos;
        double lambda = ewEntrada.numeroEventos / tempo;

        print.close();
        write.close();

        System.out.println("E[N]: " + enF + "\n");
        System.out.println("E[W]: " + ew + "\n");
        //Little --> en = lambda * ew
        //Little --> en - lambda * ew ~ 0.0
        System.out.println("Lambda: "+ lambda + "\n");
        System.out.printf("Validação Little: %2.20f %n \n", (enF - lambda * ew));
    }
}
