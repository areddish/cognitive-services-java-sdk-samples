/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.customvision.samples;

import static java.util.Arrays.asList;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import com.microsoft.azure.cognitiveservices.vision.customvision.training.models.*;
import com.microsoft.azure.cognitiveservices.vision.customvision.training.TrainingApi;
import com.microsoft.azure.cognitiveservices.vision.customvision.training.implementation.TrainingApiImpl;
import com.microsoft.azure.cognitiveservices.vision.customvision.prediction.models.*;
import com.microsoft.azure.cognitiveservices.vision.customvision.prediction.models.ImagePrediction;
import com.microsoft.azure.cognitiveservices.vision.customvision.prediction.PredictionEndpoint;
import com.microsoft.azure.cognitiveservices.vision.customvision.prediction.implementation.PredictionEndpointImpl;

public class Samples {
    public static String trainingApiKey = null;
    public static String predictionApiKey = null;

    /**
     * Main entry point.
     * @param args the parameters
     */
    public static void main(String[] args) {
        try {
            if (trainingApiKey == null) {
                trainingApiKey = System.getenv("AZURE_CUSTOMVISION_TRAINING_API_KEY");
                if(trainingApiKey == null) {
                    throw new Exception("Azure custom vision samples training api key not found.");
                }
            }
            if (predictionApiKey == null) {
                predictionApiKey = System.getenv("AZURE_CUSTOMVISION_PREDICTION_API_KEY");
                if(predictionApiKey == null) {
                    throw new Exception("Azure custom vision samples prediction api key not found.");
                }
            }

            // This demonstrates how to create an image classification project, upload images,
            // train it and make a prediction.
            ImageClassification_Sample();

            // This demonstrates how to create an object detection project, upload images,
            // train it and make a prediction.
            ObjectDetection_Sample();

        } catch (Exception e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
        }
    }

    public static void ImageClassification_Sample()
    {
        // create a client
        TrainingApi trainer = new TrainingApiImpl()
            .withApiKey(trainingApiKey);

        ProjectModel project = trainer.createProject("Sample Java Project");

        // create hemlock tag
        TagModel hemlockTag = trainer.createTag(project.id(), "Hemlock");
        // create cherry tag
        TagModel cherryTag = trainer.createTag(project.id(), "Japanese Cherry");

        System.out.println("Adding images...");
        File[] tFiles = GetResourceFiles("Hemlock");
        for (final File tFile : tFiles) {
            trainer.createImagesFromData(project.id(), Files.readAllBytes(Paths.get(tFile.getPath())), asList(hemlockTag.id().toString()));
        }
    
        tFiles = GetResourceFiles("Japanese Cherry");
        for (final File tFile : tFiles) {
            trainer.createImagesFromData(project.id(), Files.readAllBytes(Paths.get(tFile.getPath())), asList(hemlockTag.id().toString()));
        }

        System.out.println("Training...");
        IterationModel iteration = trainer.trainProject(project.id());
        while (iteration.status() == "Training")
        {
            System.out.println("Training Status: "+ iteration.status());
            Thread.sleep(1000);
            iteration = trainer.getIteration(project.id(), iteration.id());
        }

        PredictionEndpoint predictor = new PredictionEndpointImpl()
                                        .withApiKey(predictionApiKey);

        // use below for url
        // ImageUrl url = new ImageUrl().withUrl("{url}");
        // ImagePrediction results = predictor.predictImageUrl(project.id(), url);

        // load test image, use the first as a prediction
        File[] testFiles = GetResourceImagesByFolder("Test");
        byte[] testImage =  Files.readAllBytes(Paths.get(testFiles[0].getPath()));
    
        // predict
        ImagePrediction results = predictor.predictImage(project.id(), testImage);
        for (Prediction prediction: results.predictions())
        {
            System.out.println(String.format("\t%s: %.2f%% at: %.2f%%, %.2f%%, %.2f%%, %.2f%%", prediction.tag(), prediction.probability() * 100.0f));
        }
    }

    public static void ObjectDetection_Sample()
    {
        // create a client
        TrainingApi trainer = new TrainingApiImpl()
            .withApiKey(trainingApiKey);

        // find the object detection domain to set the project type
        Domain objectDetectionDomain = null;
        List<Domain> domains = trainer.getDomains();
        for (final Domain domain : domains) {
            if (domain.type == DomainType.OBJECT_DETECTION) {
                objectDetectionDomain = domain;
                break;
            }
        }

        // create an object detection project
        ProjectModel project = trainer.createProject("Sample Java OD Project", "Sample OD Project", objectDetectionDomain.id, Classifier.MULTILABEL);

        // create fork tag
        TagModel forkTag = trainer.createTag(project.id(), "fork");
        // create scissor tag
        TagModel scissorTag = trainer.createTag(project.id(), "scissor");

        System.out.println("Adding images...");
        File[] tFiles = GetResourceFiles("fork");
        for (final File tFile : tFiles) {
            trainer.createImagesFromData(project.id(), Files.readAllBytes(Paths.get(tFile.getPath())), asList(forkTag.id().toString()));
        }
    
        tFiles = GetResourceFiles("scissor");
        for (final File tFile : tFiles) {
            trainer.createImagesFromData(project.id(), Files.readAllBytes(Paths.get(tFile.getPath())), asList(scissorTag.id().toString()));
        }

        System.out.println("Training...");
        IterationModel iteration = trainer.trainProject(project.id());
        while (iteration.status() == "Training")
        {
            System.out.println("Training Status: "+ iteration.status());
            Thread.sleep(1000);
            iteration = trainer.getIteration(project.id(), iteration.id());
        }

        PredictionEndpoint predictor = new PredictionEndpointImpl()
                                        .withApiKey(predictionApiKey);

        // use below for url
        // ImageUrl url = new ImageUrl().withUrl("{url}");
        // ImagePrediction results = predictor.predictImageUrl(project.id(), url);

        // load test image, use the first as a prediction
        File[] testFiles = GetResourceImagesByFolder("ObjectTest");
        byte[] testImage =  Files.readAllBytes(Paths.get(testFiles[0].getPath()));
    
        // predict
        ImagePrediction results = predictor.predictImage(project.id(), testImage);
        for (Prediction prediction: results.predictions())
        {
            System.out.println(String.format("\t%s: %.2f%% at: %.2f%%, %.2f%%, %.2f%%, %.2f%%",
                prediction.tag(),
                prediction.probability() * 100.0f,
                prediction.boundingBox().left(),
                prediction.boundingBox().top(),
                prediction.boundingBox().width(),
                prediction.boundingBox().height()
            ));
        }
    }

    private static byte[] GetResourceImagesByFolder(String folderName)
    {
        URL url = Samples.class.getClassLoader().getResource(folderName);
        String path = url.getPath();
        return new File(path).listFiles();
    }
}
