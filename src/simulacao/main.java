package simulacao;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Random;
import java.util.Scanner;

public class main {
    
    public static Random rand = new Random();
    
    //Retorna um valor contido no intervalo entre (0,1]
    public static double aleatorio() {
        //resultado sera algo entre [0,0.999999...] proximo a 1.0
        double u = rand.nextDouble();
        //limitando entre (0,1]
        return (1.0 - u);
    }

    public static double minimo(double n1, double n2){
        if (n1 < n2)
            return n1;
        return n2;
    }
    
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

        System.out.println("Informe o tempo medio entre a chegada de clientes (segundos): ");
        double tempo_medio_clientes = 1.0 / Double.parseDouble(sc.nextLine());

        System.out.println("Informe o tempo medio gasto para atender cada cliente (segundos): ");
        double tempo_medio_atendimento = 1.0 / Double.parseDouble(sc.nextLine());

        System.out.println("Informe a quantidade de caixas: ");
        int qtd_caixas = Integer.parseInt(sc.nextLine());

        System.out.println("Informe o tempo total de simulacao (segundos): ");
        double tempo_simulacao = Double.parseDouble(sc.nextLine());
        
        //tempo decorrido da simulacao
        double tempo = 0.0;

        //armazena o tempo de chegada do proximo cliente
        double chegada_cliente = (-1.0 / tempo_medio_clientes) * Math.log(aleatorio());

        //armazena o tempo em que o cliente que estiver em atendimento saira do comercio
        //saida_atendimento == 0.0 indica caixa ocioso
        double saida_atendimento = 0.0;

        double fila = 0.0;

        //somar os tempos de atendimento, para no final calcularmos a ocupacao.
        double soma_atendimentos = 0.0;
        
        Info en = new Info();
        Info ewEntrada = new Info();
        Info ewSaida = new Info();
        
        //arquivo com valores a serem utilizados para plot de gráficos.
        File file = new File("dados_grafico.csv");
        FileWriter write = new FileWriter(file);
        PrintWriter print = new PrintWriter(write);
        print.println("Tempo,Lambda,En,Ew");

        double intervaloGraficos = 600.0;
        
        //logica da simulacao
        while(tempo <= tempo_simulacao){
            //nao existe cliente sendo atendido no momento atual,
            //de modo que a simulacao pode avancar no tempo para
            //a chegada do proximo cliente
            //modulo do tempo (double) por 100 deve ser "igual" a 0

            if(saida_atendimento == 0.0){
                tempo = minimo(chegada_cliente, proximoMultiploDeN(tempo, intervaloGraficos));
            }
            else {
                tempo = minimo(minimo(chegada_cliente, saida_atendimento), proximoMultiploDeN(tempo, intervaloGraficos));
            }

            if(tempo == chegada_cliente){
                //printf("Chegada de cliente: %lF\n", chegada_cliente);
                //evento de chegada de cliente
                fila++;
                //printf("fila: %lF\n", fila);
                //indica que o caixa esta ocioso
                //logo, pode-se comecar a atender
                //o cliente que acaba de chegar
                if(saida_atendimento == 0.0){
                    saida_atendimento = tempo;
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
            else if(tempo == saida_atendimento){
                //evento executado se houver saida de cliente
                //ou ainda se houver chegada de cliente, mas
                //o caixa estiver ocioso.
                //a cabeca da fila nao consiste no cliente em atendimento.
                //o cliente que comeca a ser atendido portanto, sai da fila,
                //e passa a estar ainda no comercio, mas em atendimento no caixa.

                //verifica se ha cliente na fila
                if(fila > 0.0){
                    fila--;

                    double tempo_atendimento = (-1.0 / tempo_medio_atendimento) * Math.log(aleatorio());
                    saida_atendimento = tempo + tempo_atendimento;
                    soma_atendimentos += tempo_atendimento;
                } else {
                    saida_atendimento = 0.0;
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
                //printar informacoes parciais em um arquivo txt
                double enParcial = en.somaAreas / tempo;
                //fazendo o calculo da ultima area dos graficos antes do momento atual
                double ewSaidaParcial = ewSaida.somaAreas + ewSaida.numeroEventos * (tempo - ewSaida.tempoAnterior);
                double ewEntradaParcial = ewEntrada.somaAreas + ewEntrada.numeroEventos * (tempo - ewEntrada.tempoAnterior);
                double ewParcial = (ewEntradaParcial - ewSaidaParcial) / (double) ewEntrada.numeroEventos;
                //remover tempo de ocupacao computado apos o momento atual
                double soma_atendimentos_parcial = soma_atendimentos;
                if (saida_atendimento > tempo)
                    soma_atendimentos_parcial -= (saida_atendimento - tempo);
                double ocupacaoParcial = soma_atendimentos_parcial / tempo * 100;
                double lambdaParcial = ewEntrada.numeroEventos / tempo;
                print.println(tempo + "," + lambdaParcial + "," + enParcial + "," + ewParcial);
            }
        }
        //remover tempo de ocupacao computado apos o termino do tempo de simulacao.
        if (saida_atendimento > tempo)
            soma_atendimentos -= (saida_atendimento - tempo);

        //fazendo o calculo da ultima area dos graficos antes do termino da simulacao
        ewSaida.somaAreas += ewSaida.numeroEventos * (tempo - ewSaida.tempoAnterior);
        ewEntrada.somaAreas += ewEntrada.numeroEventos * (tempo - ewEntrada.tempoAnterior); 

        double enF = en.somaAreas / tempo;
        double ew = (ewEntrada.somaAreas - ewSaida.somaAreas) / (double) ewEntrada.numeroEventos;
        double lambda = ewEntrada.numeroEventos / tempo;

        print.close();
        write.close();

        System.out.println("Ocupacao: " + (soma_atendimentos / tempo * 100) + " %\n");
        System.out.println("E[N]: " + enF + "\n");
        System.out.println("E[W]: " + ew + "\n");
        //Little --> en = lambda * ew
        //Little --> en - lambda * ew ~ 0.0
        System.out.println("Lambda: "+ lambda + "\n");
        System.out.printf("Validação Little: %2.20f %n \n", (enF - lambda * ew));
    }
}
