package uj.wmii.pwj.w7.insurance;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static java.lang.Double.parseDouble;
import static java.lang.String.format;
import static java.lang.String.valueOf;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.nio.file.Files.newBufferedWriter;
import static java.nio.file.Path.of;
import static java.util.Locale.US;
import static java.util.stream.Collectors.*;

public class FloridaInsurance {

    private static final String DATA_FILE_ZIP = "FL_insurance.csv.zip";
    private static final String DATA_FILE = "FL_insurance.csv";
    private static final String COUNT_FILE = "count.txt";
    private static final String TIV2012_FILE = "tiv2012.txt";
    private static final String MOST_VALUABLE_FILE = "most_valuable.txt";

    public static void main(String[] args) {

        List<InsuranceRecord> data;

        try {
            data = readToList();
        } catch (IOException e) {
            System.out.println("Error reading data file: " + e.getMessage());
            return;
        }

        createCountFile(data);
        createTiv2012File(data);
        createMostValuableFile(data);
    }

    private static List<InsuranceRecord> readToList() throws IOException {

        try (ZipFile zipFile = new ZipFile(DATA_FILE_ZIP)) {
            ZipEntry entry = zipFile.stream()
                    .filter(e -> e.getName().equals(DATA_FILE))
                    .filter(e -> !e.isDirectory())
                    .findFirst()
                    .orElseThrow(() -> new IOException("Data file not found in the zip archive"));

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(zipFile.getInputStream(entry), UTF_8))) {
                return reader.lines()
                        .skip(1)
                        .map(line -> {
                            String[] fields = line.split(",");
                            double tiv2011 = parseDouble(fields[7]);
                            double tiv2012 = parseDouble(fields[8]);
                            String county = fields[2];
                            return new InsuranceRecord(tiv2011, tiv2012, county);
                        }).toList();
            }
        }
    }

    private static void createCountFile(List<InsuranceRecord> data) {

        Set<String> counties = data.stream()
                .map(record -> record.county)
                .collect(toSet());
        int count = counties.size();

        try (BufferedWriter writer = newBufferedWriter(of(COUNT_FILE))) {
            writer.write(valueOf(count));
        } catch (IOException ex) {
            System.out.println("Error writing to file: " + ex.getMessage());
        }
    }

    private static void createTiv2012File(List<InsuranceRecord> data) {

        double totalTiv2012 = data.stream()
                .mapToDouble(record -> record.tiv2012)
                .sum();

        try (BufferedWriter writer = newBufferedWriter(of(TIV2012_FILE))) {
            writer.write(valueOf(totalTiv2012));
        } catch (IOException ex) {
            System.out.println("Error writing to file: " + ex.getMessage());
        }
    }

    private static void createMostValuableFile(List<InsuranceRecord> data) {

        List<Map.Entry<String, Double>> highestGrowth = data.stream()
                .collect(collectingAndThen(
                        groupingBy(record -> record.county, summingDouble(record -> record.tiv2012 - record.tiv2011)),
                        m -> m.entrySet().stream()
                                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                                .limit(10)
                                .toList()
                ));

        try (BufferedWriter writer = newBufferedWriter(of(MOST_VALUABLE_FILE))) {
            writer.write("country,value");
            writer.newLine();
            for (var value : highestGrowth) {
                String formattedValue = format(US, "%.2f", value.getValue());
                writer.write(value.getKey() + "," + formattedValue);
                writer.newLine();
            }
        } catch (IOException ex) {
            System.out.println("Error writing to file: " + ex.getMessage());
        }
    }
}
