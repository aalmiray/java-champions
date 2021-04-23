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

        Files.list(directory).forEach(image-> {
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

                String profileImageUrl = objectMapper.readTree(response.body()).findValue("profile_image_url").asText();
                profileImageUrl = profileImageUrl.replace("_normal", "_bigger");

                try (var stream = new URL(profileImageUrl).openStream()) {
                    Files.copy(stream, image, StandardCopyOption.REPLACE_EXISTING);
                    System.out.printf("✅ %s updated successfully%n", username);
                }

                Thread.sleep(1000);
            } catch (Exception e) {
                System.out.printf("❌ %s was not updated: %s%n", username, e.toString());
            }
        });
    }
}
