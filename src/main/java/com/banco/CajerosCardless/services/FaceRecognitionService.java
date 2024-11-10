package com.banco.CajerosCardless.services;

import org.springframework.stereotype.Service;

import com.github.sarxos.webcam.Webcam;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.rekognition.RekognitionClient;
import software.amazon.awssdk.services.rekognition.model.*;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;

@Service
public class FaceRecognitionService {

    private static final String ACCESS_KEY = "AKIAYKFQQS6ZUROIR5W5";
    private static final String SECRET_KEY = "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";
    private static final String SOURCE_IMAGE_PATH = "src/main/resources/static/source_face.jpg";

    private final RekognitionClient rekognitionClient;

    public FaceRecognitionService() {
        AwsBasicCredentials awsCreds = AwsBasicCredentials.create(ACCESS_KEY, SECRET_KEY);
        this.rekognitionClient = RekognitionClient.builder()
                .region(Region.US_EAST_2)
                .credentialsProvider(StaticCredentialsProvider.create(awsCreds))
                .build();
    }

    // Cargar la imagen fuente del servidor
    public byte[] loadSourceImage() throws IOException {
        BufferedImage sourceImage = ImageIO.read(new File(SOURCE_IMAGE_PATH));
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(sourceImage, "jpg", baos);
        byte[] imageBytes = baos.toByteArray();
        if (imageBytes.length == 0) {
            throw new IOException("La imagen fuente está vacía o no se pudo cargar correctamente.");
        }
        return imageBytes;
    }

    public byte[] captureImageFromCamera() throws IOException {
        Webcam webcam = Webcam.getDefault();
        webcam.open();
        BufferedImage image = webcam.getImage();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, "jpg", baos);
        byte[] imageBytes = baos.toByteArray();
        webcam.close();
        if (imageBytes.length == 0) {
            throw new IOException("La imagen capturada está vacía.");
        }
        return imageBytes;
    }

    // Comparar la imagen fuente con la imagen capturada
    public boolean compareCapturedImageWithSource() throws IOException {
        byte[] sourceImageBytes = loadSourceImage();
        byte[] capturedImageBytes = captureImageFromCamera();

        // Crear objetos de imagen Rekognition
        Image sourceImage = Image.builder().bytes(SdkBytes.fromByteArray(sourceImageBytes)).build();
        Image capturedImage = Image.builder().bytes(SdkBytes.fromByteArray(capturedImageBytes)).build();

        CompareFacesRequest request = CompareFacesRequest.builder()
                .sourceImage(sourceImage)
                .targetImage(capturedImage)
                .similarityThreshold(80F)
                .build();

        // Ejecutar la comparación de caras
        CompareFacesResponse response = rekognitionClient.compareFaces(request);
        return !response.faceMatches().isEmpty();
    }

    public void close() {
        rekognitionClient.close();
    }
}
