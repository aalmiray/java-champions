///usr/bin/env jbang "$0" "$@" ; exit $?
//JAVA 16
//DEPS com.fasterxml.jackson.core:jackson-core:2.12.3
//DEPS com.fasterxml.jackson.core:jackson-databind:2.12.3

import java.io.*;
import java.net.*;
import java.net.http.*;
import java.nio.file.*;
import java.time.Duration;
import java.time.temporal.ChronoUnit;

import com.fasterxml.jackson.databind.ObjectMapper;

public class avatars {
    private static final long PAUSE = 5000;

    public static void main(String... args) throws Exception {
        if (null == args || args.length != 2) {
            System.out.println("❌ Usage: java avatars.java [TOKEN] [DIRECTORY]");
            System.exit(1);
        }

        var token = args[0];
        var directory = Path.of(args[1]);

        var client = HttpClient.newBuilder()
            .connectTimeout(Duration.of(30_000, ChronoUnit.MILLIS))
            .followRedirects(HttpClient.Redirect.ALWAYS)
            .version(HttpClient.Version.HTTP_2)
            .build();

        var objectMapper = new ObjectMapper();

        Files.list(directory).sorted().forEach(image -> {
            var imageName = image.getFileName().toString();
            var username = imageName.substring(0, imageName.length() - 4);

            try {
                var userProfileUri = new URI("https://api.twitter.com/1.1/users/show.json?screen_name=" + username);

                var request = HttpRequest.newBuilder()
                    .uri(userProfileUri)
                    .header("authorization", "Bearer " + token)
                    .version(HttpClient.Version.HTTP_2)
                    .build();
                var response = client.send(request, HttpResponse.BodyHandlers.ofByteArray()::apply);

                var node = objectMapper.readTree(response.body()).findValue("profile_image_url");
                if (null == node) {
                    System.out.printf("❌ %s%n", username);
                    Thread.sleep(PAUSE);
                    return;
                }

                var profileImageUrl = node.asText();
                if (profileImageUrl.contains("default_profile_images")) {
                    System.out.printf("❌ %s%n", username);
                    Thread.sleep(PAUSE);
                    return;
                }

                var downloaded = downloadImage(profileImageUrl.replace("_normal", "_bigger"), image);
                if (!downloaded) {
                    downloaded = downloadImage(profileImageUrl, image);
                }

                if (downloaded) {
                    System.out.printf("✅ %s%n", username);
                } else {
                    System.out.printf("❌ %s%n", username);
                }

                Thread.sleep(PAUSE);
            } catch (Exception e) {
                System.out.printf("❌ %s -> %s%n", username, e.toString());
            }
        });
    }

    private static boolean downloadImage(String profileImageUrl, Path image) throws Exception {
        var tmpImage = Files.createTempFile("jc", "");

        try (var stream = new URL(profileImageUrl).openStream()) {
            var size = Files.copy(stream, tmpImage, StandardCopyOption.REPLACE_EXISTING);
            if (size > 0L) {
                Files.move(tmpImage, image, StandardCopyOption.REPLACE_EXISTING);
                return true;
            }
        }

        return false;
    }
}
