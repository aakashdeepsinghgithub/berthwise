package com.berthwise.io;

import com.berthwise.model.Berth;
import com.berthwise.model.Vessel;
import com.berthwise.model.VesselType;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/** Loads reference data from CSV files with a header row. */
public class FleetCsvLoader {

    /** imo,name,type,loa_m,beam_m,draft_m,gross_tonnage */
    public List<Vessel> loadVessels(Path path) throws IOException {
        BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8);
        List<String> lines = reader.lines().collect(Collectors.toList());

        List<Vessel> vessels = new ArrayList<>();
        for (String line : lines.subList(1, lines.size() - 1)) {
            if (line.isBlank()) {
                continue;
            }
            String[] f = line.split(",");
            vessels.add(new Vessel(
                    f[0].trim(),
                    f[1].trim(),
                    VesselType.valueOf(f[2].trim().toUpperCase(Locale.ROOT)),
                    Double.parseDouble(f[3].trim()),
                    Double.parseDouble(f[4].trim()),
                    Double.parseDouble(f[5].trim()),
                    Integer.parseInt(f[6].trim())));
        }
        return vessels;
    }

    /** id,name,length_m,depth_m,allowed_types (pipe-separated) */
    public List<Berth> loadBerths(Path path) throws IOException {
        List<Berth> berths = new ArrayList<>();
        try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            String header = reader.readLine();
            if (header == null) {
                return berths;
            }
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) {
                    continue;
                }
                String[] f = line.split(",");
                Set<VesselType> types = EnumSet.noneOf(VesselType.class);
                for (String t : f[4].split("\\|")) {
                    types.add(VesselType.valueOf(t.trim().toUpperCase(Locale.ROOT)));
                }
                berths.add(new Berth(f[0].trim(), f[1].trim(),
                        Double.parseDouble(f[2].trim()), Double.parseDouble(f[3].trim()), types));
            }
        }
        return berths;
    }
}
