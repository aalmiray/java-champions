///usr/bin/env jbang "$0" "$@" ; exit $?

//JAVA 17

//DEPS org.json:json:20250107
//DEPS com.fasterxml.jackson.core:jackson-core:2.16.0
//DEPS com.fasterxml.jackson.core:jackson-databind:2.16.0
//DEPS com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.16.0

import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.json.JSONObject;
import org.json.JSONArray;

/**
 * To be executed with JBang:
 * jbang site.java INPUT_DIR OUTPUT_DIR GEO_API_KEY
 *
 * GEO API key to be generated on https://geocode.maps.co, is free for 5000 request/day, max 1/sec
 *
 * For example:
 * cd resources
 * ./jbang site.java .. ../site/content/ ${{ secrets.GEO_API_TOKEN }}
 */
public class site {
    private static final Map<String, String> STATUS = new TreeMap<>(Map.of(
        "founding-member", "pass:[<i class=\"fa fa-star\" title=\"Founding Member\"></i>]",
        "honorary-member", "pass:[<i class=\"fa fa-medal\" title=\"Honorary Member\"></i>]",
        "alumni", "pass:[<i class=\"fa fa-pause\" title=\"Alumni Member\"></i>]",
        "passed-away", "pass:[<i class=\"fa fa-ribbon\" title=\"Member has passed away\"></i>]",
        "retired", "pass:[<i class=\"fa fa-umbrella-beach\" title=\"Member has retired from the program\"></i>]"
    ));

    private static final Map<String, String> SOCIAL = Map.of(
        "twitter", "pass:[<span class=\"icon\"><i class=\"fab fa-twitter\" title=\"Twitter\"></i></span>]",
        "mastodon", "pass:[<span class=\"icon\"><i class=\"fab fa-mastodon\" title=\"Mastodon\"></i></span>]",
        "linkedin", "pass:[<span class=\"icon\"><i class=\"fab fa-linkedin\" title=\"LinkedIn\"></i></span>]",
        "xing", "pass:[<span class=\"icon\"><i class=\"fab fa-xing\" title=\"Xing\"></i></span>]",
        "github", "pass:[<span class=\"icon\"><i class=\"fab fa-github\" title=\"GitHub\"></i></span>]",
        "bluesky", "pass:[<span class=\"icon\"><i class=\"fa fa-brands fa-bluesky\" title=\"BlueSky\"></i></span>]",
        "website", "pass:[<span class=\"icon\"><i class=\"fa fa-globe\" title=\"Website\"></i></span>]",
        "youtube", "pass:[<span class=\"icon\"><i class=\"fab fa-youtube-square\" title=\"YouTube\"></i></span>]",
        "sessionize", "pass:[<span class=\"icon\"><i class=\"fa fa-bullhorn\" title=\"Sessionize\"></i></span>]",
        "speakerdeck", "pass:[<span class=\"icon\"><i class=\"fab fa-speaker-deck\" title=\"SpeakerDeck\"></i></span>]"
    );

    private static final Map<String, String> COUNTRY = Map.of(
        "nomination", "pass:[<i class=\"fa fa-award\" title=\"Country of nomination\"></i>]",
        "residence", "pass:[<i class=\"fa fa-home\" title=\"Country of residence\"></i>]",
        "citizenship", "pass:[<i class=\"fa fa-passport\" title=\"Country of citizenship\"></i>]",
        "birth", "pass:[<i class=\"fa fa-baby\" title=\"Country of birth\"></i>]"
    );

    public static void main(String... args) throws Exception {
        if (null == args || args.length != 3) {
            System.out.println("❌ Usage: java site.java [YAML DIRECTORY] [OUTPUT DIRECTORY]");
            System.exit(1);
        }

        var inputDirectory = Path.of(args[0]);
        var outputDirectory = Path.of(args[1]);
        var geoApiKey = args[2];
        var fileMembers = inputDirectory.resolve("java-champions.yml");
        var filePodcasts = inputDirectory.resolve("podcasts.yml");

        if (!Files.exists(fileMembers)) {
            System.out.printf("❌ %s/java-champions.yml does not exist%n", inputDirectory.toAbsolutePath());
            System.exit(1);
        }

        if (!Files.exists(filePodcasts)) {
            System.out.printf("❌ %s/podcasts.yml does not exist%n", inputDirectory.toAbsolutePath());
            System.exit(1);
        }

        var mapper = YAMLMapper.builder().build();
        var members = new Members();
        var podcasts = new Podcasts();

        // parse members input data
        try (InputStream in = Files.newInputStream(fileMembers)) {
            members = mapper.readValue(in, Members.class);
        } catch (IOException e) {
            e.printStackTrace();
            System.out.printf("❌ Unexpected error reading %s%n", fileMembers);
            System.exit(1);
        }

        // generate members.adoc
        var membersDoc = new StringBuilder(Files.readString(Path.of("members.adoc.tpl")));
        for (JavaChampion member : members.members) {
            membersDoc.append(member.formatted());
        }

        var outputMembers = outputDirectory.resolve("members.adoc");
        Files.write(outputMembers, membersDoc.toString().getBytes());

        // generate stats.adoc
        var countries = members.members.stream()
            .map(m -> m.country.nomination)
            .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));

        // TODO: sort by country name after sorting by count
        var countriesSb = new StringBuilder();
        countries.entrySet().stream()
            .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
            .forEach(e -> countriesSb.append("        ['")
                .append(e.getKey())
                .append("', ")
                .append(e.getValue())
                .append("],\n"));

        var years = members.members.stream()
            .map(m -> m.year)
            .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));

        var yearsSb = new StringBuilder();
        years.entrySet().stream()
            .sorted(Map.Entry.comparingByKey(Comparator.reverseOrder()))
            .forEach(e -> yearsSb.append("        ['")
                .append(e.getKey())
                .append("', ")
                .append(e.getValue())
                .append("],\n"));

        var statsDoc = Files.readString(Path.of("stats.adoc.tpl"));
        statsDoc = statsDoc.replace("@COUNTRIES@", countriesSb.toString())
            .replace("@COUNTRIES_HEIGHT@", String.valueOf(countries.size() * 30))
            .replace("@YEARS@", yearsSb.toString())
            .replace("@YEARS_HEIGHT@", String.valueOf(years.size() * 30));
        var outputStats = outputDirectory.resolve("stats.adoc");
        Files.write(outputStats, statsDoc.getBytes());

        // generate map.adoc
        var locations = new ArrayList<String>();
        var notFound = new ArrayList<String>();
        var counter = new AtomicInteger(0);
        members.members.forEach(m -> {
            var city = m.city;
            var country = m.country.residence;
            if (country == null || country.isBlank()) {
                country = m.country.nomination;
            }
            if (country != null && !country.isBlank()) {
                var location = getLocation(geoApiKey, country, city);
                if (location.isPresent()) {
                    System.out.println("Location " + counter.incrementAndGet() +
                            " found for " + m.name
                            + ": " + city + ", " + country
                            + " - " + location.get().lat + "/" + location.get().lon);
                    locations.add("{lat: " + location.get().lat
                            + ", lng: " + location.get().lon
                            + ", name: \"" + m.name.replace("\"", "'") + "\""
                            + "}");
                } else {
                    notFound.add(m.name + ": " + city + ", " + country);
                }
            } else {
                notFound.add(m.name + ": " + city + ", " + country);
            }
            if (m.city != null && !m.city.isBlank()) {}
        });

        var mapDoc = Files.readString(Path.of("map.adoc.tpl"));
        mapDoc = mapDoc.replace("@LOCATIONS@", String.join(",\n\t", locations));
        var outputMap = outputDirectory.resolve("map.adoc");
        Files.write(outputMap, mapDoc.getBytes());
        System.out.println("HTML generated for maps");
        Files.write(outputDirectory.resolve("not_found_for_map.txt"), String.join("\n", notFound).getBytes());
        System.out.println("Didn't find location for " + notFound.size() + " champions. List is saved to not_found_for_map.txt");

        // generate fediverse CSV file
        var mastodonCsv = new PrintWriter(Files.newOutputStream(outputDirectory.resolve("resources").resolve("mastodon.csv")));
        mastodonCsv.println("Account address,Show boosts,Notify on new posts,Languages");
        members.members.stream()
            .filter(JavaChampion::hasMastodon)
            .map(JavaChampion::asMastodonCsvEntry)
            .forEach(mastodonCsv::println);
        mastodonCsv.flush();
        mastodonCsv.close();

        // parse podcasts input data
        try (InputStream in = Files.newInputStream(filePodcasts)) {
            podcasts = mapper.readValue(in, Podcasts.class);
        } catch (IOException e) {
            e.printStackTrace();
            System.out.printf("❌ Unexpected error reading %s%n", filePodcasts);
            System.exit(1);
        }

        // generate podcasts.adoc
        var podcastsDoc = new StringBuilder(Files.readString(Path.of("podcasts.adoc.tpl")));
        for (Podcast podcast : podcasts.podcasts) {
            podcastsDoc.append(podcast.formatted());
        }

        var outputPodcasts = outputDirectory.resolve("podcasts.adoc");
        Files.write(outputPodcasts, podcastsDoc.toString().getBytes());
    }

    private static Optional<Location> getLocation(String apiKey, String country, String city) {
        var maxAttempts = 3;
        var attempt = 1;
        while (attempt <= maxAttempts) {
            var location = getLocationFromApi(apiKey, country, city);
            if (location.isPresent()) {
                return location;
            }
            attempt++;
        }
        System.out.println("Couldn't find location for " + city + ", " + country + " after " + maxAttempts + " attempts");
        return Optional.empty();
    }

    private static Optional<Location> getLocationFromApi(String apiKey, String country, String city) {
        var q = (city == null ? "" : city + ", ") + country;
        try {
            Thread.sleep(1000); // GEO API can only be called once per second
            URL url = new URL("https://geocode.maps.co/search"
                    + "?q=" + URLEncoder.encode(q, StandardCharsets.UTF_8)
                    + "&api_key=" + apiKey);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            int responseCode = connection.getResponseCode();
            if (responseCode == 200) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                String line;
                StringBuilder response = new StringBuilder();
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                // Parse the JSON response to extract latitude and longitude
                JSONArray results = new JSONArray(response.toString());
                JSONObject result = results.getJSONObject(0);
                double latitude = result.getDouble("lat");
                double longitude = result.getDouble("lon");
                return Optional.of(new Location(latitude, longitude));
            } else if (responseCode == 400) {
                System.out.println("Error 400 for " + url);
                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                String line;
                StringBuilder response = new StringBuilder();
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                System.out.println(response);
            } else {
                System.out.println("Error: " + responseCode + " for " + q);
            }
        } catch (Exception e) {
            System.out.printf("❌ Unexpected error while getting location for %s: %s%n", q, e.getMessage());
        }
        return Optional.empty();
    }

    static class Location {
        public double lat;
        public double lon;

        public Location(double lat, double lon) {
            this.lat = lat;
            this.lon = lon;
        }
    }

    static class Members {
        public List<JavaChampion> members = new ArrayList<>();
    }

    static class JavaChampion {
        public String name;
        public String year;
        public String avatar;
        public Social social;
        public Country country;
        public String city;
        public List<String> status = new ArrayList<>();

        String formatted() {
            var b = new StringBuilder("|{counter:idx}\n")
                .append("|image:")
                .append(avatar)
                .append("[]");

            if (status != null && !status.isEmpty()) {
                b.append(" +\n");
                STATUS.forEach((k, v) -> {
                    if (status.contains(k)) {
                        b.append(v)
                            .append(" ");
                    }
                });
            }
            b.append("\n")
                .append("|")
                .append(name);

            if (social != null) {
                b.append(social.formatted());
            } else {
                b.append("|\n");
            }

            if (city != null && !city.isBlank()) {
                b.append("|")
                    .append(city)
                    .append("\n");
            } else {
                b.append("|\n");
            }

            b.append(country.formatted())
                .append("|")
                .append(year);

            return b.append("\n\n")
                .toString();
        }

        boolean hasMastodon() {
            return social != null && social.mastodon != null;
        }

        String asMastodonCsvEntry() {
            return social.getMastodonAccount() + ",true,false,";
        }
    }

    static class Country {
        public String nomination;
        public String residence;
        public String citizenship;
        public String birth;

        String formatted() {
            var b = new StringBuilder("|")
                .append(COUNTRY.get("nomination"))
                .append(" ")
                .append(nomination);

            if (residence != null && !residence.isBlank()) {
                b.append(" +\n")
                    .append(COUNTRY.get("residence"))
                    .append(" ")
                    .append(residence);
            }

            if (citizenship != null && !citizenship.isBlank()) {
                b.append(" +\n")
                    .append(COUNTRY.get("citizenship"))
                    .append(" ")
                    .append(citizenship);
            }

            if (birth != null && !birth.isBlank()) {
                b.append(" +\n")
                    .append(COUNTRY.get("birth"))
                    .append(" ")
                    .append(birth);
            }

            return b.append("\n")
                .toString();
        }
    }

    static class Social {
        public String twitter;
        public String mastodon;
        public String bluesky;
        public String linkedin;
        public String github;
        public String website;
        public String youtube;
        public String sessionize;
        public String speakerdeck;
        public String xing;

        String formatted() {
            var b = new StringBuilder("|");

            if (twitter != null && !twitter.isBlank()) {
                b.append("link:")
                    .append(twitter)
                    .append("[")
                    .append(SOCIAL.get("twitter"))
                    .append("] ");
            }

            if (mastodon != null && !mastodon.isBlank()) {
                b.append("link:")
                    .append(mastodon)
                    .append("[")
                    .append(SOCIAL.get("mastodon"))
                    .append("] ");
            }

            if (bluesky != null && !bluesky.isBlank()) {
                b.append("link:")
                    .append(bluesky)
                    .append("[")
                    .append(SOCIAL.get("bluesky"))
                    .append("] ");
            }

            if (linkedin != null && !linkedin.isBlank()) {
                b.append("link:")
                    .append(linkedin)
                    .append("[")
                    .append(SOCIAL.get("linkedin"))
                    .append("] ");
            }

            if (xing != null && !xing.isBlank()) {
                b.append("link:")
                    .append(xing)
                    .append("[")
                    .append(SOCIAL.get("xing"))
                    .append("] ");
            }

            if (github != null && !github.isBlank()) {
                b.append("link:")
                    .append(github)
                    .append("[")
                    .append(SOCIAL.get("github"))
                    .append("] ");
            }

            if (website != null && !website.isBlank()) {
                b.append("link:")
                    .append(website)
                    .append("[")
                    .append(SOCIAL.get("website"))
                    .append("] ");
            }

            if (youtube != null && !youtube.isBlank()) {
                b.append("link:")
                    .append(youtube)
                    .append("[")
                    .append(SOCIAL.get("youtube"))
                    .append("] ");
            }

            if (sessionize != null && !sessionize.isBlank()) {
                b.append("link:")
                    .append(sessionize)
                    .append("[")
                    .append(SOCIAL.get("sessionize"))
                    .append("] ");
            }

            if (speakerdeck != null && !speakerdeck.isBlank()) {
                b.append("link:")
                    .append(speakerdeck)
                    .append("[")
                    .append(SOCIAL.get("speakerdeck"))
                    .append("] ");
            }

            return b.append("\n").toString();
        }

        String getMastodonAccount() {
            var s = mastodon.split("@")[0].substring(8);
            s = s.substring(0, s.length() - 1);
            var n = mastodon.split("@")[1];
            return "@" + n + "@" + s;
        }
    }

    static class Podcasts {
        public List<Podcast> podcasts = new ArrayList<>();
    }

    static class Host {
        public String name;

        String formatted() {
            var b = new StringBuilder()
                    .append(name);

            return b.append("\n")
                    .toString();
        }
    }

    static class Podcast {
        public String title;
        public String url;
        public String language;
        public String logo;
        public Social social;
        public List<Host> hosts = new ArrayList<>();

        String formatted() {
            var b = new StringBuilder("|{counter:idx}\n")
                    .append("|image:")
                    .append(logo)
                    .append("[]");

            b.append("|")
                    .append("link:")
                    .append(url)
                    .append("[")
                    .append(title)
                    .append("]")
                    .append("\n");

            b.append("|")
                    .append(language)
                    .append("\n");

            if (hosts != null && !hosts.isEmpty()) {
                b.append("a|");
                hosts.forEach(h -> b.append("* ").append(h.formatted()).append("\n"));
                b.append("\n");
            } else {
                b.append("|\n");
            }

            if (social != null) {
                b.append(social.formatted());
            } else {
                b.append("|\n");
            }

            return b.append("\n\n")
                    .toString();
        }
    }
}
