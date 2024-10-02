package org.example;

import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

class QueueConfig {
    public int servers;
    public int capacity;
    public double minArrival;
    public double maxArrival;
    public double minService;
    public double maxService;

    public QueueConfig() {
    }

    @Override
    public String toString() {
        return "QueueConfig{" +
                "servers=" + servers +
                ", capacity=" + capacity +
                ", minArrival=" + minArrival +
                ", maxArrival=" + maxArrival +
                ", minService=" + minService +
                ", maxService=" + maxService +
                '}';
    }
}

class SimulationConfig {
    public Map<String, Double> arrivals;
    public Map<String, QueueConfig> queues;
    public List<Map<String, Object>> network;
    public List<Double> rndnumbers;
    public int rndnumbersPerSeed;
    public List<Integer> seeds;
}

// Classe principal QueueSimulator
public class QueueSimulator {

    public static void main(String[] args) {
        Yaml yaml = new Yaml();
        try (InputStream inputStream = QueueSimulator.class.getClassLoader().getResourceAsStream("config.yml")) {
            if (inputStream == null) {
                System.out.println("Erro: O arquivo config.yml não foi encontrado.");
                return;
            }

            SimulationConfig config = yaml.loadAs(inputStream, SimulationConfig.class);

            QueueConfig queue1Config = config.queues.get("Q1");
            System.out.println("Fila Q1 - Servidores: " + queue1Config.servers + ", Capacidade: " + queue1Config.capacity);
            System.out.println("Min. Tempo de Serviço: " + queue1Config.minService + ", Max. Tempo de Serviço: " + queue1Config.maxService);

            Simulation simulation = new Simulation(config);
            simulation.runSimulation(100000);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

class Simulation {
    private Map<String, QueueConfig> queues;
    private List<Double> randomNumbers;
    private Random random = new Random();
    private Map<String, QueueResult> queueResults = new HashMap<>();

    public Simulation(SimulationConfig config) {
        this.queues = config.queues;
        this.randomNumbers = config.rndnumbers;

        for (String queueName : queues.keySet()) {
            queueResults.put(queueName, new QueueResult());
        }
    }

    public void runSimulation(int iterations) {
        int totalProcessed = 0;
        int totalLost = 0;
        double globalTime = 0.0;

        for (int i = 0; i < iterations; i++) {
            System.out.println("Iteração " + (i + 1) + ":");

            QueueConfig q1 = queues.get("Q1");
            QueueResult resultQ1 = queueResults.get("Q1");

            double arrivalTime = q1.minArrival + random.nextDouble() * (q1.maxArrival - q1.minArrival);
            globalTime += arrivalTime;

            double serviceTime = q1.minService + random.nextDouble() * (q1.maxService - q1.minService);
            resultQ1.customersProcessed++;
            resultQ1.totalServiceTime += serviceTime;
            System.out.println("Cliente chegou na Fila Q1 no tempo: " + globalTime + ", Tempo de serviço: " + serviceTime);

            double transitionProb = random.nextDouble();
            QueueConfig nextQueue = null;
            QueueResult nextResult = null;
            if (transitionProb <= 0.8) {
                nextQueue = queues.get("Q2");
                nextResult = queueResults.get("Q2");
                System.out.println("Cliente foi para Fila Q2.");
            } else {
                nextQueue = queues.get("Q3");
                nextResult = queueResults.get("Q3");
                System.out.println("Cliente foi para Fila Q3.");
            }

            if (nextQueue != null) {
                double nextServiceTime = nextQueue.minService + random.nextDouble() * (nextQueue.maxService - nextQueue.minService);
                globalTime += nextServiceTime;
                nextResult.customersProcessed++;
                nextResult.totalServiceTime += nextServiceTime;
                System.out.println("Tempo de serviço na próxima fila: " + nextServiceTime + ", Tempo global: " + globalTime);
                totalProcessed++;
            } else {
                totalLost++;
                System.out.println("Cliente perdido devido à capacidade da fila.");
            }
        }

        reportResults(totalProcessed, totalLost, globalTime);
    }

    public void reportResults(int totalProcessed, int totalLost, double globalTime) {
        System.out.println("\n--- Resultados da Simulação ---");
        System.out.println("Clientes processados: " + totalProcessed);
        System.out.println("Clientes perdidos: " + totalLost);
        System.out.println("Tempo global da simulação: " + globalTime + " minutos");

        System.out.println("\n--- Detalhes das Filas ---");

        for (String queueName : queues.keySet()) {
            QueueConfig queueConfig = queues.get(queueName);
            QueueResult queueResult = queueResults.get(queueName);
            queueResult.printResults(queueName, queueConfig.servers, queueConfig.minArrival, queueConfig.maxArrival,
                    queueConfig.minService, queueConfig.maxService, queueConfig.capacity);
        }
    }
}


class QueueResult {
    public int customersProcessed;
    public double totalServiceTime;
    public int maxQueueSizeReached;

    public QueueResult() {
        this.customersProcessed = 0;
        this.totalServiceTime = 0.0;
        this.maxQueueSizeReached = 0;
    }

    public void printResults(String queueName, int servers, double minArrival, double maxArrival, double minService, double maxService, int capacity) {
        System.out.printf("Resultado da %s: G/G/%d/%d, chegadas entre %.1f..%.1f, atendimento entre %.1f..%.1f\n",
                queueName, servers, capacity, minArrival, maxArrival, minService, maxService);
        System.out.printf("Clientes atendidos: %d, Tempo total de atendimento: %.2f, Capacidade máxima atingida: %d\n",
                customersProcessed, totalServiceTime, maxQueueSizeReached);
    }
}


