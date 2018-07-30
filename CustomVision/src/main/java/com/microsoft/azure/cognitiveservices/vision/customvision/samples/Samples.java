/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.customvision.samples;

import static java.util.Arrays.asList;
import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import com.microsoft.azure.cognitiveservices.vision.customvision.training.models.*;
import com.microsoft.azure.cognitiveservices.vision.customvision.training.models.ImageFileCreateBatch;
import com.microsoft.azure.cognitiveservices.vision.customvision.training.models.ImageFileCreateEntry;
import com.microsoft.azure.cognitiveservices.vision.customvision.training.TrainingApi;
import com.microsoft.azure.cognitiveservices.vision.customvision.training.Trainings;
import com.microsoft.azure.cognitiveservices.vision.customvision.training.CustomVisionTrainingManager;
import com.microsoft.azure.cognitiveservices.vision.customvision.training.implementation.TrainingApiImpl;
import com.microsoft.azure.cognitiveservices.vision.customvision.prediction.models.*;
import com.microsoft.azure.cognitiveservices.vision.customvision.prediction.models.ImagePrediction;
import com.microsoft.azure.cognitiveservices.vision.customvision.prediction.models.Prediction;
import com.microsoft.azure.cognitiveservices.vision.customvision.prediction.PredictionEndpoint;
import com.microsoft.azure.cognitiveservices.vision.customvision.prediction.Predictions;
import com.microsoft.azure.cognitiveservices.vision.customvision.prediction.CustomVisionPredictionManager;
import com.microsoft.azure.cognitiveservices.vision.customvision.prediction.implementation.PredictionEndpointImpl;


public class Samples {
    /**
     * Main entry point.
     * @param args the parameters
     */
    public static void runSample(TrainingApi trainer, PredictionEndpoint predictor) {
        try {
            // This demonstrates how to create an image classification project, upload images,
            // train it and make a prediction.
            ImageClassification_Sample(trainer, predictor);

            // This demonstrates how to create an object detection project, upload images,
            // train it and make a prediction.
            ObjectDetection_Sample(trainer, predictor);
        } catch (Exception e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
        }
    }

    public static void ImageClassification_Sample(TrainingApi trainClient, PredictionEndpoint predictor) {
        try {
            Trainings trainer = trainClient.trainings();

            Project project = trainer.createProject()
                                        .withName("Sample Java Project")
                                        .execute();

            // create hemlock tag
            Tag hemlockTag = trainer.createTag()
                                    .withProjectId(project.id())
                                    .withName("Hemlock")
                                    .execute();
            // create cherry tag
            Tag cherryTag = trainer.createTag()
                                    .withProjectId(project.id())
                                    .withName("Japanese Cherry")
                                    .execute();

            System.out.println("Adding images...");
            File[] tFiles = GetResourceImagesByFolder("Hemlock");
            for (final File tFile : tFiles) {
                ImageFileCreateEntry files = new ImageFileCreateEntry()
                                                .withName(tFile.getName())
                                                .withContents(Files.readAllBytes(Paths.get(tFile.getPath())));

                ImageFileCreateBatch batch = new ImageFileCreateBatch()
                                                .withImages(asList(files))
                                                .withTagIds(asList(hemlockTag.id()));

                trainer.createImagesFromFiles(project.id(), batch);
            }

            tFiles = GetResourceImagesByFolder("Japanese Cherry");
            for (final File tFile : tFiles) {
                ImageFileCreateEntry files = new ImageFileCreateEntry()
                                                .withName(tFile.getName())
                                                .withContents(Files.readAllBytes(Paths.get(tFile.getPath())));

                ImageFileCreateBatch batch = new ImageFileCreateBatch()
                                                .withImages(asList(files))
                                                .withTagIds(asList(hemlockTag.id()));

                trainer.createImagesFromFiles(project.id(), batch);
            }

            System.out.println("Training...");
            Iteration iteration = trainer.trainProject(project.id());

            while (iteration.status() == "Training")
            {
                System.out.println("Training Status: "+ iteration.status());
                Thread.sleep(1000);
                iteration = trainer.getIteration(project.id(), iteration.id());
            }

            // use below for url
            // String url = "some url";
            // ImagePrediction results = predictor.predictions().predictImage()
            //                         .withProjectId(project.id())
            //                         .withUrl(url)
            //                         .execute();

            // load test image, use the first as a prediction
            File[] testFiles = GetResourceImagesByFolder("Test");
            byte[] testImage =  Files.readAllBytes(Paths.get(testFiles[0].getPath()));

            // predict
            ImagePrediction results = predictor.predictions().predictImage()
                                            .withProjectId(project.id())
                                            .withImageData(testImage)
                                            .execute();

            for (Prediction prediction: results.predictions())
            {
                System.out.println(String.format("\t%s: %.2f%% at: %.2f%%, %.2f%%, %.2f%%, %.2f%%", prediction.tagName(), prediction.probability() * 100.0f));
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
        }
    }

    public static void ObjectDetection_Sample(TrainingApi trainClient, PredictionEndpoint predictor)
    {
        try {
            Trainings trainer = trainClient.trainings();

            // find the object detection domain to set the project type
            Domain objectDetectionDomain = null;
            List<Domain> domains = trainer.getDomains();
            for (final Domain domain : domains) {
                if (domain.type() == DomainType.OBJECT_DETECTION) {
                    objectDetectionDomain = domain;
                    break;
                }
            }

            // create an object detection project
            Project project = trainer.createProject()
                                    .withName("Sample Java OD Project")
                                    .withDescription("Sample OD Project")
                                    .withDomainId(objectDetectionDomain.id())
                                    .withClassificationType(Classifier.MULTILABEL.toString())
                                    .execute();

            // create fork tag
            Tag forkTag = trainer.createTag()
                                    .withProjectId(project.id())
                                    .withName("fork")
                                    .execute();

            // create scissor tag
            Tag scissorTag = trainer.createTag()
                                    .withProjectId(project.id())
                                    .withName("scissor")
                                    .execute();

            System.out.println("Adding images...");
            File[] tFiles = GetResourceImagesByFolder("fork");
            for (final File tFile : tFiles) {
                ImageFileCreateEntry files = new ImageFileCreateEntry()
                            .withName(tFile.getName())
                            .withContents(Files.readAllBytes(Paths.get(tFile.getPath())));

                ImageFileCreateBatch batch = new ImageFileCreateBatch()
                            .withImages(asList(files))
                            .withTagIds(asList(forkTag.id()));

                trainer.createImagesFromFiles(project.id(), batch);
            }

            tFiles = GetResourceImagesByFolder("scissor");
            for (final File tFile : tFiles) {
                ImageFileCreateEntry files = new ImageFileCreateEntry()
                            .withName(tFile.getName())
                            .withContents(Files.readAllBytes(Paths.get(tFile.getPath())));

                ImageFileCreateBatch batch = new ImageFileCreateBatch()
                            .withImages(asList(files))
                            .withTagIds(asList(scissorTag.id()));

                trainer.createImagesFromFiles(project.id(), batch);
            }

            System.out.println("Training...");
            Iteration iteration = trainer.trainProject(project.id());

            while (iteration.status() == "Training")
            {
                System.out.println("Training Status: "+ iteration.status());
                Thread.sleep(1000);
                iteration = trainer.getIteration(project.id(), iteration.id());
            }

            // use below for url
            // String url = "some url";
            // ImagePrediction results = predictor.predictions().predictImage()
            //                         .withProjectId(project.id())
            //                         .withUrl(url)
            //                         .execute();

            // load test image, use the first as a prediction
            File[] testFiles = GetResourceImagesByFolder("ObjectTest");
            byte[] testImage =  Files.readAllBytes(Paths.get(testFiles[0].getPath()));

            // predict
            ImagePrediction results = predictor.predictions().predictImage()
                                            .withProjectId(project.id())
                                            .withImageData(testImage)
                                            .execute();

            for (Prediction prediction: results.predictions())
            {
                System.out.println(String.format("\t%s: %.2f%% at: %.2f%%, %.2f%%, %.2f%%, %.2f%%",
                    prediction.tagName(),
                    prediction.probability() * 100.0f,
                    prediction.boundingBox().left(),
                    prediction.boundingBox().top(),
                    prediction.boundingBox().width(),
                    prediction.boundingBox().height()
                ));
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
        }
    }

    private static File[] GetResourceImagesByFolder(String folderName)
    {
        URL url = Samples.class.getClassLoader().getResource(folderName);
        String path = url.getPath();
        return new File(path).listFiles();
    }

    /**
     * Main entry point.
     *
     * @param args the parameters
     */
    public static void main(String[] args) {
        try {
            //=============================================================
            // Authenticate

            final String trainingApiKey = System.getenv("AZURE_CUSTOMVISION_TRAINING_API_KEY");;
            final String predictionApiKey = System.getenv("AZURE_CUSTOMVISION_PREDICTION_API_KEY");;

            TrainingApi trainClient = CustomVisionTrainingManager.authenticate(trainingApiKey);
            PredictionEndpoint predictClient = CustomVisionPredictionManager.authenticate(predictionApiKey);

            runSample(trainClient, predictClient);
        } catch (Exception e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
        }
    }
}
