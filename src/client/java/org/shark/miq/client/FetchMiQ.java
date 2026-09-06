package org.shark.miq.client;

import net.minecraft.client.Minecraft;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;

public class FetchMiQ {

    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    private static final int MAX_IMAGE_SIZE = 10 * 1024 * 1024; // 10 MiB

    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss_SSS");

    public CompletableFuture<Path> fetchMiQ(String text) {

        String encodedText = URLEncoder.encode(
                text,
                java.nio.charset.StandardCharsets.UTF_8
        );

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(
                        "https://miq.sharkbot.xyz/?text=" + encodedText
                ))
                .timeout(java.time.Duration.ofSeconds(15))
                .header("Accept", "image/png,image/jpeg,image/*")
                .GET()
                .build();

        return HTTP_CLIENT.sendAsync(
                request,
                HttpResponse.BodyHandlers.ofByteArray()
        ).thenApply(response -> {

            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new RuntimeException(
                        "MIQ server returned HTTP " + response.statusCode()
                );
            }

            byte[] data = response.body();

            if (data.length == 0) {
                throw new RuntimeException("Empty image response");
            }

            if (data.length > MAX_IMAGE_SIZE) {
                throw new RuntimeException("Image is too large");
            }

            String contentType = response.headers()
                    .firstValue("Content-Type")
                    .orElse("");

            contentType = contentType.toLowerCase(Locale.ROOT);

            if (!contentType.startsWith("image/")) {
                throw new RuntimeException(
                        "Response is not an image: " + contentType
                );
            }

            BufferedImage image;

            try (ByteArrayInputStream input =
                         new ByteArrayInputStream(data)) {

                image = ImageIO.read(input);
            } catch (IOException e) {
                throw new RuntimeException("Failed to read image", e);
            }

            if (image == null) {
                throw new RuntimeException("Invalid image data");
            }

            long pixels = (long) image.getWidth() * image.getHeight();

            if (pixels > 25_000_000L) {
                throw new RuntimeException("Image dimensions are too large");
            }

            Path mcDir = Minecraft.getInstance()
                    .gameDirectory
                    .toPath();

            Path miqDir = mcDir.resolve("miq");

            try {
                Files.createDirectories(miqDir);

                String fileName = LocalDateTime.now()
                        .format(FORMATTER) + ".png";

                Path target = miqDir.resolve(fileName);

                Path temp = Files.createTempFile(
                        miqDir,
                        ".miq-",
                        ".tmp"
                );

                try {
                    Files.write(
                            temp,
                            data,
                            StandardOpenOption.WRITE,
                            StandardOpenOption.TRUNCATE_EXISTING
                    );

                    Files.move(
                            temp,
                            target,
                            StandardCopyOption.ATOMIC_MOVE,
                            StandardCopyOption.REPLACE_EXISTING
                    );

                    return target;

                } catch (Exception e) {
                    Files.deleteIfExists(temp);
                    throw e;
                }

            } catch (IOException e) {
                throw new RuntimeException(
                        "Failed to save MIQ image",
                        e
                );
            }
        });
    }
}