package ru.sfedu.guestbook;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

public class Main {
    public static void main(String[] args) {
        try {
            // чтение json
            //String content = new String(Files.readAllBytes(Paths.get("tickets.json")));
            String content;
            try (var inputStream = Main.class.getClassLoader().getResourceAsStream("tickets.json")) {
                if (inputStream == null) {
                    System.out.println("Файл tickets.json не найден в ресурсах!");
                    return;
                }
                content = new String(inputStream.readAllBytes());
            }
            // проверка на bom и его удаления
            if (content.startsWith("\uFEFF")) {
                content = content.substring(1);
            }
            JSONObject jsonObject = new JSONObject(content);
            JSONArray tickets = jsonObject.getJSONArray("tickets");

            // map для хранения минимального времени полета для всех перевозчиков
            Map<String, Long> carrierMinDuration = new HashMap<>();
            // список для хранения цен
            List<Double> prices = new ArrayList<>();

            SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm");

            // перебор всех билетов
            for (int i = 0; i < tickets.length(); i++) {
                JSONObject ticket = tickets.getJSONObject(i);
                String origin = ticket.getString("origin");
                String destination = ticket.getString("destination");
                String carrier = ticket.getString("carrier");
                String departureTime = ticket.getString("departure_time");
                String arrivalTime = ticket.getString("arrival_time");
                double price = ticket.getDouble("price");

                // проверка нужного рейса (VVO-TLV)
                if (origin.equals("VVO") && destination.equals("TLV")) {

                    // вычисление времени полета в минутах
                    long flightTime = calculateFlightTime(departureTime, arrivalTime, timeFormat);
                    // обновление минимального времени полета для перевозчика
                    carrierMinDuration.compute(carrier, (key, oldValue) ->
                            oldValue == null ? flightTime : Math.min(oldValue, flightTime));

                    prices.add(price);
                }
            }

            // вывод минимального времени полета для каждого перевозчика
            System.out.println("Минимальное время полета между Владивостоком и Тель-Авивом");
            for (Map.Entry<String, Long> entry : carrierMinDuration.entrySet()) {
                long minutes = entry.getValue();
                System.out.printf("%s: %d ч %d мин%n",
                        entry.getKey(), minutes / 60, minutes % 60);
            }

            // средняя цена билетов
            double averagePrice = prices.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);

            // сортировка цен для нахождения медианы
            Collections.sort(prices);
            double median;
            if (prices.size() % 2 == 0) {
                median = (prices.get(prices.size() / 2 - 1) + prices.get(prices.size() / 2)) / 2.0;
            } else {
                median = prices.get(prices.size() / 2);
            }

            // вычисление разницы мжде средней ценой и медианой
            double difference = averagePrice - median;
            System.out.printf("%nРазница между средней ценой (%.2f) и медианой (%.2f): %.2f%n",
                    averagePrice, median, difference);
        } catch (IOException e) {
            System.out.println("Ошибка при чтении файла: " + e.getMessage());
        } catch (ParseException e) {
            System.out.println("Ошибка при парсинге времени: " + e.getMessage());
        }
    }

    // метод для вычисления времени полета в минутах
    private static long calculateFlightTime(String departure, String arrival, SimpleDateFormat format)
        throws ParseException {
        Date departureDate = format.parse(departure);
        Date arrivalDate = format.parse(arrival);

        long diffMillis = arrivalDate.getTime() - departureDate.getTime();
        // если разница отрицательная, добавляем 24 часа
        if (diffMillis < 0) {
            diffMillis += 24 * 60 * 60 * 1000;
        }
        // миллисекунды в минуты
        return diffMillis / (1000 * 60);
    }
}