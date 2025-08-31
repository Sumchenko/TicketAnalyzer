package ru.sfedu.guestbook;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

public class Main {
    public static void main(String[] args) {
        try {
            // Загрузка JSON из ресурсов
            String content = loadJsonContent();
            if (content == null) {
                return;
            }

            // Парсинг JSON
            JSONObject jsonObject = new JSONObject(content);
            JSONArray tickets = jsonObject.optJSONArray("tickets");
            if (tickets == null) {
                System.out.println("Массив 'tickets' не найден в JSON.");
                return;
            }

            // Фильтрация билетов только для VVO -> TLV
            List<JSONObject> filteredTickets = filterTickets(tickets, "VVO", "TLV");

            // Расчет минимального времени полета для каждого перевозчика
            Map<String, Long> carrierMinDuration = calculateMinFlightDurations(filteredTickets);

            // Вывод минимального времени полета
            System.out.println("Минимальное время полета между Владивостоком и Тель-Авивом:");
            for (Map.Entry<String, Long> entry : carrierMinDuration.entrySet()) {
                long minutes = entry.getValue();
                System.out.printf("%s: %d ч %d мин%n", entry.getKey(), minutes / 60, minutes % 60);
            }

            // Расчет статистики по ценам
            double[] priceStats = calculatePriceStats(filteredTickets);
            double averagePrice = priceStats[0];
            double median = priceStats[1];
            double difference = averagePrice - median;

            // Вывод разницы между средней ценой и медианой
            System.out.printf("%nРазница между средней ценой (%.2f) и медианой (%.2f): %.2f%n",
                    averagePrice, median, difference);
        } catch (IOException e) {
            System.out.println("Ошибка при чтении файла: " + e.getMessage());
        } catch (ParseException e) {
            System.out.println("Ошибка при парсинге времени: " + e.getMessage());
        }
    }

    // Метод для загрузки содержимого JSON
    private static String loadJsonContent() throws IOException {
        try (InputStream inputStream = Main.class.getClassLoader().getResourceAsStream("tickets.json")) {
            if (inputStream == null) {
                System.out.println("Файл tickets.json не найден в ресурсах!");
                return null;
            }
            String content = new String(inputStream.readAllBytes());
            // Удаление BOM, если присутствует
            if (content.startsWith("\uFEFF")) {
                content = content.substring(1);
            }
            return content;
        }
    }

    // Метод для фильтрации билетов по origin и destination
    private static List<JSONObject> filterTickets(JSONArray tickets, String originFilter, String destFilter) {
        List<JSONObject> filtered = new ArrayList<>();
        for (int i = 0; i < tickets.length(); i++) {
            JSONObject ticket = tickets.optJSONObject(i);
            if (ticket != null) {
                String origin = ticket.optString("origin", null);
                String destination = ticket.optString("destination", null);
                if (origin != null && destination != null &&
                        origin.equals(originFilter) && destination.equals(destFilter)) {
                    filtered.add(ticket);
                }
            }
        }
        return filtered;
    }

    // Метод для расчета минимального времени полета для каждого перевозчика
    private static Map<String, Long> calculateMinFlightDurations(List<JSONObject> tickets) throws ParseException {
        Map<String, Long> carrierMinDuration = new HashMap<>();
        SimpleDateFormat fullFormat = new SimpleDateFormat("dd.MM.yy HH:mm");

        for (JSONObject ticket : tickets) {
            String carrier = ticket.optString("carrier", null);
            String depDate = ticket.optString("departure_date", null);
            String depTime = ticket.optString("departure_time", null);
            String arrDate = ticket.optString("arrival_date", null);
            String arrTime = ticket.optString("arrival_time", null);

            // Проверка на null значения
            if (carrier == null || depDate == null || depTime == null || arrDate == null || arrTime == null) {
                continue; // Пропускаем билет с некорректными данными
            }

            // Парсинг полных дат и времени
            Date departure = fullFormat.parse(depDate + " " + depTime);
            Date arrival = fullFormat.parse(arrDate + " " + arrTime);

            // Вычисление времени полета в минутах
            long flightTime = (arrival.getTime() - departure.getTime()) / (1000 * 60);

            // Обновление минимального времени для перевозчика
            carrierMinDuration.compute(carrier, (key, oldValue) ->
                    oldValue == null ? flightTime : Math.min(oldValue, flightTime));
        }
        return carrierMinDuration;
    }

    // Метод для расчета средней цены и медианы
    private static double[] calculatePriceStats(List<JSONObject> tickets) {
        List<Double> prices = new ArrayList<>();

        for (JSONObject ticket : tickets) {
            if (ticket.has("price") && !ticket.isNull("price")) {
                prices.add(ticket.getDouble("price"));
            }
        }

        if (prices.isEmpty()) {
            return new double[]{0.0, 0.0};
        }

        // Средняя цена
        double averagePrice = prices.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);

        // Сортировка для медианы
        Collections.sort(prices);
        double median;
        int size = prices.size();
        if (size % 2 == 0) {
            median = (prices.get(size / 2 - 1) + prices.get(size / 2)) / 2.0;
        } else {
            median = prices.get(size / 2);
        }

        return new double[]{averagePrice, median};
    }
}